package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
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

  /** Walk to the value ready for decode (Leaf: leaf value; Fields: assembled atomic-read record).
    * Counterpart to `JsonFocus.navigateCursor`, but returning the raw value (Avro has no cursor
    * abstraction). Decode step is the shared [[decodeFrom]] default below.
    */
  def navigateRaw(record: IndexedRecord): Either[AvroFailure, Any]

  /** Decode an Avro-shaped runtime value to `A`. Shared one-line default. */
  final def decodeFrom(any: Any): Either[Throwable, A] = codec.decodeEither(any)

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

    def navigateRaw(record: IndexedRecord): Either[AvroFailure, Any] =
      AvroWalk.walkPathArr(record, path).map(_.cur)

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      AvroWalk.walkPathArr(record, path) match
        case Left(_)       => record
        case Right(walked) =>
          codec.decodeEither(walked.cur) match
            case Left(_)  => record
            case Right(a) =>
              try
                val encoded = codec.encode(f(a))
                AvroWalk
                  .rebuildPathArr(walked.parents, walked.parentsLen, path, encoded)
                  .asInstanceOf[IndexedRecord]
              catch case NonFatal(_) => record

    def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord =
      AvroWalk.walkPathArr(record, path) match
        case Left(_)       => record
        case Right(walked) =>
          walked.cur match
            case asRecord: IndexedRecord =>
              AvroWalk
                .rebuildPathArr(walked.parents, walked.parentsLen, path, f(asRecord))
                .asInstanceOf[IndexedRecord]
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
                .rebuildPathArr(walked.parents, walked.parentsLen, path, encoded)
                .asInstanceOf[IndexedRecord]
            catch case NonFatal(_) => record

    def readImpl(record: IndexedRecord): Option[A] =
      AvroWalk.walkPathArr(record, path).toOption.flatMap(s => codec.decodeEither(s.cur).toOption)

    def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
      AvroWalk.walkPathArr(record, path) match
        case Left(failure) => Ior.Both(Chain.one(failure), record)
        case Right(walked) =>
          decodeOrFail(walked.cur, record)(a =>
            AvroWalk
              .rebuildPathArr(walked.parents, walked.parentsLen, path, codec.encode(f(a)))
              .asInstanceOf[IndexedRecord]
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
                  .rebuildPathArr(walked.parents, walked.parentsLen, path, f(asRecord))
                  .asInstanceOf[IndexedRecord]
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
                  .rebuildPathArr(walked.parents, walked.parentsLen, path, encoded)
                  .asInstanceOf[IndexedRecord]
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
    */
  final class Fields[A] private[avro] (
      private[avro] val parentPath: Array[PathStep],
      private[avro] val fieldNames: Array[String],
      private[avro] val codec: AvroCodec[A],
  ) extends AvroFocus[A]:

    protected def terminalStep: PathStep = AvroWalk.terminalOf(parentPath)

    /** NT's schema (kindlings derives field names matching the case class so positional layout
      * aligns with the parent record by name).
      */
    private val ntSchema: Schema = codec.schema

    def navigateRaw(record: IndexedRecord): Either[AvroFailure, Any] =
      AvroWalk.walkPath(record, parentPath).flatMap {
        case (cur, _) =>
          cur match
            case parent: IndexedRecord =>
              // navigateRaw can only carry one failure; surface readFields' first PathMissing.
              readFields(parent)
                .left
                .map(_.headOption.getOrElse(AvroFailure.PathMissing(terminalStep)))
            case _ => Left(AvroFailure.NotARecord(terminalStep))
      }

    /** Atomic read — succeeds only when ALL selected fields are present; missing fields accumulate
      * as `PathMissing`.
      */
    private def readFields(parent: IndexedRecord): Either[Chain[AvroFailure], IndexedRecord] =
      val parentSchema = parent.getSchema
      val sub = new GenericData.Record(ntSchema)
      var chain = Chain.empty[AvroFailure]
      var i = 0
      while i < fieldNames.length do
        val name = fieldNames(i)
        val parentField = parentSchema.getField(name)
        if parentField == null then chain = chain :+ AvroFailure.PathMissing(PathStep.Field(name))
        else
          // ntField is non-null for any selector the macro accepted.
          val ntField = ntSchema.getField(name)
          sub.put(ntField.pos, parent.get(parentField.pos))
        i += 1
      if chain.isEmpty then Right(sub) else Left(chain)

    /** Project selected fields out of `parent` for transform-shape ops; missing → `null`.
      * Non-atomic counterpart to [[readFields]].
      */
    private def buildSubRecord(parent: IndexedRecord): IndexedRecord =
      val parentSchema = parent.getSchema
      val sub = new GenericData.Record(ntSchema)
      var i = 0
      while i < fieldNames.length do
        val name = fieldNames(i)
        val parentField = parentSchema.getField(name)
        val ntField = ntSchema.getField(name)
        val value: Any = if parentField == null then null else parent.get(parentField.pos)
        if ntField != null then sub.put(ntField.pos, value)
        i += 1
      sub

    /** Overlay the encoder's output for `a` onto `parent`; encoders that omit a selected field
      * leave the parent's entry untouched.
      */
    private def writeFields(parent: IndexedRecord, a: A): IndexedRecord =
      codec.encode(a) match
        case encoded: IndexedRecord =>
          val updates: Map[String, Any] =
            val encodedSchema = encoded.getSchema
            fieldNames
              .iterator
              .flatMap { name =>
                val ef = encodedSchema.getField(name)
                if ef == null then Iterator.empty else Iterator.single(name -> encoded.get(ef.pos))
              }
              .toMap
          AvroWalk.replaceRecordFields(parent, updates)
        case _ =>
          // Encoder produced a non-record (shouldn't happen for an NT under a record schema).
          parent

    /** Overlay a foreign sub-record (transform's result) onto `parent`. */
    private def overlayFields(parent: IndexedRecord, newSub: IndexedRecord): IndexedRecord =
      val newSchema = newSub.getSchema
      val updates: Map[String, Any] =
        fieldNames
          .iterator
          .flatMap { name =>
            val nf = newSchema.getField(name)
            if nf == null then Iterator.empty else Iterator.single(name -> newSub.get(nf.pos))
          }
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
        .rebuildPathArr(walked.parents, walked.parentsLen, parentPath, newParent)
        .asInstanceOf[IndexedRecord]

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      walkParent(record) match
        case Left(_)                 => record
        case Right((parent, walked)) =>
          readFields(parent) match
            case Left(_)    => record
            case Right(sub) =>
              codec.decodeEither(sub) match
                case Left(_)  => record
                case Right(a) =>
                  try rebuild(writeFields(parent, f(a)), walked)
                  catch case NonFatal(_) => record

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
