package dev.constructive.eo.avro

import scala.annotation.tailrec
import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
import dev.constructive.eo.widenRight
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, IndexedRecord}

/** Focus on a value of type `A` somewhere inside an Avro [[IndexedRecord]]. Mirrors
  * `circe.JsonFocus` for the Avro carrier. Storage decomposition along two axes:
  *
  *   1. Where the focus lives — at a leaf reached by a path (Leaf), or as a NamedTuple assembled
  *      from selected fields under a parent record (Fields).
  *   2. Single- vs multi-focus — applied once at the root ([[AvroPrism]]) or per-element after a
  *      prefix walk ([[AvroTraversal]]).
  *
  * Each focus exposes six per-record operations (three Ior-bearing default + three silent) plus two
  * reads and two Optic-trait `to`-side hooks.
  */
sealed abstract private[avro] class AvroFocus[A]:

  /** Codec for `A`. For Leaf this is the user's `AvroCodec[A]`; for Fields it's the synthesised
    * NamedTuple codec.
    */
  private[avro] def codec: AvroCodec[A]

  /** Terminal path step — used by failure surfaces. `PathStep.Field("")` for root-level focuses. */
  protected def terminalStep: PathStep

  /** Decode `cur` and fold success into `onHit`, or surface a `DecodeFailed`. Shared between Leaf
    * and Fields.
    */
  final protected def decodeOrFail(
      cur: Any,
      record: IndexedRecord,
  )(onHit: A => IndexedRecord): Ior[Chain[AvroFailure], IndexedRecord] =
    codec.decodeEither(cur) match
      case Right(a) =>
        try Ior.Right(onHit(a))
        catch
          case NonFatal(t) =>
            Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)
      case Left(t) =>
        Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

  /** Read counterpart to [[decodeOrFail]] — decode → `Ior.Right(a)` or `Ior.Left(chain)`. */
  final protected def decodeOrLeft(cur: Any): Ior[Chain[AvroFailure], A] =
    codec.decodeEither(cur) match
      case Right(a) => Ior.Right(a)
      case Left(t)  => Ior.Left(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)))

  // Default Ior-bearing ops — failures surface as `Ior.Both(chain, inputRecord)` (input preserved
  // as silent fallback). `readIor` returns `Ior.Left(chain)` on failure (a read produces an `A` or
  // it doesn't, no fallback).

  def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord]

  def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord]

  def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord]
  def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A]

  // *Unsafe (silent) ops — input pass-through on any failure (read returns None).
  def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord
  def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord
  def placeImpl(record: IndexedRecord, a: A): IndexedRecord
  def readImpl(record: IndexedRecord): Option[A]

  /** Navigate + decode the focus in ONE walk, and capture a writer that places a new focus back
    * WITHOUT walking again — the writer closes over the walk this call already did (parents for a
    * Leaf, resolved parent + walk for a Fields). Backs [[AvroRecordPrism]]'s `Affine` `to`/`from`
    * seam so a generic/composed `modify` is a single tree walk, mirroring how the byte face
    * captures a `BinarySpan`.
    *
    * `Left` on navigate/decode failure. The returned writer catches encode/rebuild failures and
    * returns the original `record` unchanged — the Optic `from` has no failure channel; diagnostics
    * live on the Ior member surface.
    */
  def navigateForWrite(record: IndexedRecord): Either[AvroFailure, (A, A => IndexedRecord)]

end AvroFocus

private[avro] object AvroFocus:

  /** Single-leaf focus — reaches `A` via `path` and reads / writes through the user-supplied
    * [[AvroCodec]]. Powers root-level [[AvroPrism]]s and per-element steps of `AvroTraversal[A]`.
    */
  final class Leaf[A] private[avro] (
      private[avro] val path: Array[PathStep],
      private[avro] val codec: AvroCodec[A],
  ) extends AvroFocus[A]:

    protected def terminalStep: PathStep = AvroWalk.terminalOf(path)

    def navigateForWrite(
        record: IndexedRecord
    ): Either[AvroFailure, (A, A => IndexedRecord)] =
      if path.length == 0 then
        codec.decodeEither(record) match
          case Left(t)  => Left(AvroFailure.DecodeFailed(terminalStep, t))
          case Right(a) =>
            // Root full-cover: the writer IS the old reverseGet (encode standalone).
            val writer: A => IndexedRecord = b =>
              try
                codec.encode(b) match
                  case asRec: IndexedRecord => asRec
                  case _                    => record
              catch case NonFatal(_) => record
            Right((a, writer))
      else
        AvroWalk.walkPathArr(record, path) match
          case l @ Left(_)   => l.widenRight
          case Right(walked) =>
            codec.decodeEither(walked.cur) match
              case Left(t)  => Left(AvroFailure.DecodeFailed(terminalStep, t))
              case Right(a) =>
                val writer: A => IndexedRecord = b =>
                  try
                    AvroWalk
                      .rebuildRecordArr(walked.parents, walked.parentsLen, path, codec.encode(b))
                  catch case NonFatal(_) => record
                Right((a, writer))

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      // One walk: reuse navigateForWrite's walk+decode+writer (the Optic seam's own machinery) so
      // the silent modify can't drift from it. The captured writer already re-encodes, rebuilds,
      // and falls back to `record` on a NonFatal encode/rebuild failure.
      navigateForWrite(record) match
        case Right((a, writer)) => writer(f(a))
        case Left(_)            => record

    def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord =
      AvroWalk.walkPathArr(record, path) match
        case Left(_)       => record
        case Right(walked) =>
          walked.cur match
            case asRecord: IndexedRecord =>
              AvroWalk
                .rebuildRecordArr(walked.parents, walked.parentsLen, path, f(asRecord))
            case _ => record

    def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
      if path.length == 0 then
        try
          codec.encode(a) match
            case asRec: IndexedRecord => asRec
            case _                    => record
        catch case NonFatal(_) => record
      else
        AvroWalk.walkPathArr(record, path) match
          case Left(_)       => record
          case Right(walked) =>
            try
              val encoded = codec.encode(a)
              AvroWalk
                .rebuildRecordArr(walked.parents, walked.parentsLen, path, encoded)
            catch case NonFatal(_) => record

    def readImpl(record: IndexedRecord): Option[A] =
      AvroWalk.walkPathArr(record, path).toOption.flatMap(s => codec.decodeEither(s.cur).toOption)

    def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
      AvroWalk.walkPathArr(record, path) match
        case Left(failure) => Ior.Both(Chain.one(failure), record)
        case Right(walked) =>
          decodeOrFail(walked.cur, record)(a =>
            AvroWalk
              .rebuildRecordArr(walked.parents, walked.parentsLen, path, codec.encode(f(a)))
          )

    def transformIor(
        record: IndexedRecord,
        f: IndexedRecord => IndexedRecord,
    ): Ior[Chain[AvroFailure], IndexedRecord] =
      AvroWalk.walkPathArr(record, path) match
        case Left(failure) => Ior.Both(Chain.one(failure), record)
        case Right(walked) =>
          walked.cur match
            case asRecord: IndexedRecord =>
              Ior.Right(
                AvroWalk
                  .rebuildRecordArr(walked.parents, walked.parentsLen, path, f(asRecord))
              )
            case _ =>
              Ior.Both(Chain.one(AvroFailure.NotARecord(terminalStep)), record)

    def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord] =
      if path.length == 0 then
        try
          codec.encode(a) match
            case asRec: IndexedRecord => Ior.Right(asRec)
            case _                    =>
              Ior.Both(Chain.one(AvroFailure.NotARecord(terminalStep)), record)
        catch
          case NonFatal(t) =>
            Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)
      else
        AvroWalk.walkPathArr(record, path) match
          case Left(failure) => Ior.Both(Chain.one(failure), record)
          case Right(walked) =>
            try
              val encoded = codec.encode(a)
              Ior.Right(
                AvroWalk
                  .rebuildRecordArr(walked.parents, walked.parentsLen, path, encoded)
              )
            catch
              case NonFatal(t) =>
                Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

    def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A] =
      AvroWalk.walkPathArr(record, path) match
        case Left(failure) => Ior.Left(Chain.one(failure))
        case Right(walked) => decodeOrLeft(walked.cur)

  end Leaf

  /** Multi-field focus — reaches a parent record via `parentPath`, then assembles a NamedTuple `A`
    * from `fieldNames`. All-or-nothing: a partial read accumulates one `PathMissing` per missing
    * field and the modify family leaves the input unchanged at that location.
    *
    * `fieldNames` are the resolved PARENT-record schema names, in selector order (issue #35). The
    * NT side is addressed by POSITION (selector index → NT schema field position), never by name,
    * so the parent and NT namespaces are free to differ — a snake_case parent with a
    * kindlings-identity NT codec, a vulcan-override parent, etc. — without a name collision or an
    * NPE.
    */
  final class Fields[A] private[avro] (
      private[avro] val parentPath: Array[PathStep],
      private[avro] val fieldNames: Array[String],
      private[avro] val codec: AvroCodec[A],
  ) extends AvroFocus[A]:

    protected def terminalStep: PathStep = AvroWalk.terminalOf(parentPath)

    /** NT's schema. Addressed positionally (selector index i → `ntSchema.getFields.get(i)`); its
      * field NAMES need not match the parent's (issue #35).
      */
    private val ntSchema: Schema = codec.schema

    def navigateForWrite(
        record: IndexedRecord
    ): Either[AvroFailure, (A, A => IndexedRecord)] =
      walkParent(record) match
        case l @ Left(_)             => l.widenRight
        case Right((parent, walked)) =>
          readFields(parent) match
            case Left(chain) =>
              Left(chain.headOption.getOrElse(AvroFailure.PathMissing(terminalStep)))
            case Right(sub) =>
              codec.decodeEither(sub) match
                case Left(t)  => Left(AvroFailure.DecodeFailed(terminalStep, t))
                case Right(a) =>
                  // Writer overlays the NT fields BY NAME onto the resolved parent (writeFields)
                  // and rebuilds through the captured walk — one walk total.
                  val writer: A => IndexedRecord = b =>
                    try rebuild(writeFields(parent, b), walked)
                    catch case NonFatal(_) => record
                  Right((a, writer))

    /** Atomic read — succeeds only when ALL selected fields are present; missing fields accumulate
      * as `PathMissing`.
      */
    private def readFields(parent: IndexedRecord): Either[Chain[AvroFailure], IndexedRecord] =
      val parentSchema = parent.getSchema
      val sub = new GenericData.Record(ntSchema)
      @tailrec def loop(i: Int, chain: Chain[AvroFailure]): Chain[AvroFailure] =
        if i >= fieldNames.length then chain
        else
          val name = fieldNames(i)
          val parentField = parentSchema.getField(name)
          val next =
            if parentField == null then chain :+ AvroFailure.PathMissing(PathStep.Field(name))
            else
              // NT side by position i (selector order), never by name — see the class doc.
              sub.put(ntSchema.getFields.get(i).pos, parent.get(parentField.pos))
              chain
          loop(i + 1, next)
      val chain = loop(0, Chain.empty[AvroFailure])
      if chain.isEmpty then Right(sub) else Left(chain)

    /** Project selected fields out of `parent` for transform-shape ops; missing → `null`.
      * Non-atomic counterpart to [[readFields]].
      */
    private def buildSubRecord(parent: IndexedRecord): IndexedRecord =
      val parentSchema = parent.getSchema
      val sub = new GenericData.Record(ntSchema)
      @tailrec def loop(i: Int): Unit =
        if i < fieldNames.length then
          val parentField = parentSchema.getField(fieldNames(i))
          val value: Any = if parentField == null then null else parent.get(parentField.pos)
          // NT side by position i (selector order), never by name — see the class doc.
          sub.put(ntSchema.getFields.get(i).pos, value)
          loop(i + 1)
      loop(0)
      sub

    /** Overlay the encoder's output for `a` onto `parent`; encoders that omit a selected field
      * leave the parent's entry untouched.
      */
    private[avro] def writeFields(parent: IndexedRecord, a: A): IndexedRecord =
      codec.encode(a) match
        case encoded: IndexedRecord => overlayFields(parent, encoded)
        case _                      =>
          // Encoder produced a non-record (shouldn't happen for an NT under a record schema).
          parent

    /** Overlay a foreign sub-record (transform's result, same NT shape) onto `parent`. Reads the
      * sub-record by POSITION (selector order), keys updates by the PARENT schema name.
      */
    private def overlayFields(parent: IndexedRecord, newSub: IndexedRecord): IndexedRecord =
      val newFields = newSub.getSchema.getFields
      val updates: Map[String, Any] =
        fieldNames
          .indices
          .iterator
          .map(i => fieldNames(i) -> newSub.get(newFields.get(i).pos))
          .toMap
      AvroWalk.replaceRecordFields(parent, updates)

    /** Walk the parent path; projects the terminal as `IndexedRecord`. Returns the array-shaped
      * `WalkRes` so [[rebuild]] re-uses parents without materialising a Vector.
      */
    private def walkParent(
        record: IndexedRecord
    ): Either[AvroFailure, (IndexedRecord, AvroWalk.WalkRes)] =
      AvroWalk.walkPathArr(record, parentPath).flatMap { walked =>
        walked.cur match
          case rec: IndexedRecord => Right((rec, walked))
          case _                  => Left(AvroFailure.NotARecord(terminalStep))
      }

    private def rebuild(newParent: IndexedRecord, walked: AvroWalk.WalkRes): IndexedRecord =
      AvroWalk
        .rebuildRecordArr(walked.parents, walked.parentsLen, parentPath, newParent)

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      // One walk: delegate to navigateForWrite (see the Leaf note). NB the *Ior* modify stays
      // separate — it must surface readFields' accumulated PathMissing Chain, which
      // navigateForWrite collapses to a single failure.
      navigateForWrite(record) match
        case Right((a, writer)) => writer(f(a))
        case Left(_)            => record

    def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord =
      walkParent(record) match
        case Left(_)                 => record
        case Right((parent, walked)) =>
          try
            val newSub = f(buildSubRecord(parent))
            rebuild(overlayFields(parent, newSub), walked)
          catch case NonFatal(_) => record

    def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
      walkParent(record) match
        case Left(_)                 => record
        case Right((parent, walked)) =>
          try rebuild(writeFields(parent, a), walked)
          catch case NonFatal(_) => record

    def readImpl(record: IndexedRecord): Option[A] =
      walkParent(record).toOption.flatMap {
        case (parent, _) =>
          readFields(parent).toOption.flatMap(sub => codec.decodeEither(sub).toOption)
      }

    def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)           => Ior.Both(Chain.one(failure), record)
        case Right((parent, walked)) =>
          readFields(parent) match
            case Left(chain) => Ior.Both(chain, record)
            case Right(sub)  =>
              decodeOrFail(sub, record)(a => rebuild(writeFields(parent, f(a)), walked))

    def transformIor(
        record: IndexedRecord,
        f: IndexedRecord => IndexedRecord,
    ): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)           => Ior.Both(Chain.one(failure), record)
        case Right((parent, walked)) =>
          try
            val newSub = f(buildSubRecord(parent))
            Ior.Right(rebuild(overlayFields(parent, newSub), walked))
          catch
            case NonFatal(t) =>
              Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

    def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)           => Ior.Both(Chain.one(failure), record)
        case Right((parent, walked)) =>
          try Ior.Right(rebuild(writeFields(parent, a), walked))
          catch
            case NonFatal(t) =>
              Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

    def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A] =
      walkParent(record) match
        case Left(failure)      => Ior.Left(Chain.one(failure))
        case Right((parent, _)) =>
          readFields(parent) match
            case Left(chain) => Ior.Left(chain)
            case Right(sub)  => decodeOrLeft(sub)

  end Fields

end AvroFocus
