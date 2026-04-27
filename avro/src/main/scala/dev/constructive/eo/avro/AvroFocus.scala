package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, IndexedRecord}

/** What it means to focus on a value of type `A` somewhere inside an Avro [[IndexedRecord]].
  *
  * Mirrors `dev.constructive.eo.circe.JsonFocus` for the Avro carrier. The four legacy carrier
  * classes — `AvroPrism`, `AvroFieldsPrism`, and (Units 6/7) their traversal siblings — collapse to
  * two real classes by factoring the storage variation along two orthogonal axes:
  *
  *   1. ''Where does the focus live?'' — at a leaf reached by a path (Leaf), or as a NamedTuple
  *      assembled from selected fields under a parent record (Fields).
  *   2. ''Single-focus or multi-focus?'' — applied once at the root (Prism), or applied per-element
  *      after walking a `prefix` to an array (Traversal — Unit 6).
  *
  * Axis 1 lives here as a sealed hierarchy. Axis 2 lives in [[AvroPrism]] (and the future
  * `AvroTraversal`), each of which holds an `AvroFocus[A]` and delegates the per-element focus
  * operations to it.
  *
  * Each focus exposes the same six per-record operations: three Ior-bearing (decoded modify, raw
  * transform, decoded place) for the default surface, and three silent (input pass-through) for the
  * `*Unsafe` escape hatches. Plus two reads (decoded get, decoded silent get) and the abstract
  * Optic-trait `to`-side hook (`navigateRaw` returning `Either[(Throwable, IndexedRecord), A]`).
  */
sealed abstract private[avro] class AvroFocus[A]:

  /** Codec for the focused value `A`. For Leaf focuses this is the user's `AvroCodec[A]`; for
    * Fields focuses this is the codec for the synthesised NamedTuple type.
    */
  private[avro] def codec: AvroCodec[A]

  /** The terminal step of this focus's path — used by failure surfaces to point at the last cursor
    * position the walk attempted. `PathStep.Field("")` for root-level focuses with empty paths.
    */
  protected def terminalStep: PathStep

  /** Decode `cur` and either fold the resulting `A` into a rebuild record (`onHit`), or surface a
    * `DecodeFailed` against the terminal step. Threaded through every Ior-bearing decoded-modify
    * body. Both Leaf and Fields share this exact decode-then-rebuild-or-fail shape.
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

  /** Read variant of [[decodeOrFail]] — decode `cur`; success → `Ior.Right(a)`, failure →
    * `Ior.Left(Chain(DecodeFailed(terminalStep, ...)))`. Used by both Leaf and Fields `readIor`.
    */
  final protected def decodeOrLeft(cur: Any): Ior[Chain[AvroFailure], A] =
    codec.decodeEither(cur) match
      case Right(a) => Ior.Right(a)
      case Left(t)  => Ior.Left(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)))

  // ---- Default Ior-bearing ops ------------------------------------

  /** Walk to the focus, decode, apply `f`, encode, rebuild root. Failures (path miss, decode
    * failure) surface as `Ior.Both(chain, inputRecord)` — the input is preserved as the silent
    * fallback while the diagnostic chain documents what went wrong.
    */
  def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord]

  /** Walk to the focus, apply `f` to the raw [[IndexedRecord]] there, rebuild root. Same failure
    * surface as [[modifyIor]].
    */
  def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord]

  /** Walk to the focus, write the encoded `a`, rebuild root. Same failure surface. */
  def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord]

  /** Walk to the focus, decode. Success = `Ior.Right(a)`; failure = `Ior.Left(chain)` — a read
    * either produces an `A` or it doesn't, so the record is intentionally not carried as a
    * fallback.
    */
  def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A]

  // ---- *Unsafe (silent) ops ---------------------------------------

  /** Silent counterpart to [[modifyIor]] — input pass-through on any failure. */
  def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord

  /** Silent counterpart to [[transformIor]]. */
  def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord

  /** Silent counterpart to [[placeIor]]. */
  def placeImpl(record: IndexedRecord, a: A): IndexedRecord

  /** Silent read — `None` on any failure. */
  def readImpl(record: IndexedRecord): Option[A]

  // ---- Optic-trait `to` side --------------------------------------

  /** Build the `Either[(Throwable, IndexedRecord), A]` shape for the abstract `Optic.to`. The Ior
    * surfaces above bypass this; the generic `Optic` extensions go through it.
    */
  def navigateRaw(record: IndexedRecord): Either[(Throwable, IndexedRecord), A]

end AvroFocus

private[avro] object AvroFocus:

  /** Single-leaf focus — reaches an `A` via the `path` walk and reads / writes it through the
    * user-supplied [[AvroCodec]]. Powers root-level [[AvroPrism]]s and (in Unit 6+) per-element
    * steps of `AvroTraversal[A]` when the user did `.each.<field>` style chains.
    */
  final class Leaf[A] private[avro] (
      private[avro] val path: Array[PathStep],
      private[avro] val codec: AvroCodec[A],
  ) extends AvroFocus[A]:

    protected def terminalStep: PathStep = AvroWalk.terminalOf(path)

    def navigateRaw(record: IndexedRecord): Either[(Throwable, IndexedRecord), A] =
      AvroWalk.walkPath(record, path) match
        case Left(_) =>
          Left((new RuntimeException(s"path missing in ${path.mkString("/")}"), record))
        case Right((cur, _)) =>
          codec.decodeEither(cur) match
            case Right(a) => Right(a)
            case Left(t)  => Left((t, record))

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      AvroWalk.walkPath(record, path) match
        case Left(_)               => record
        case Right((cur, parents)) =>
          codec.decodeEither(cur) match
            case Left(_)  => record
            case Right(a) =>
              try
                val encoded = codec.encode(f(a))
                AvroWalk.rebuildPath(parents, path, encoded).asInstanceOf[IndexedRecord]
              catch case NonFatal(_) => record

    def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord =
      AvroWalk.walkPath(record, path) match
        case Left(_)               => record
        case Right((cur, parents)) =>
          cur match
            case asRecord: IndexedRecord =>
              AvroWalk.rebuildPath(parents, path, f(asRecord)).asInstanceOf[IndexedRecord]
            case _ => record

    def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
      if path.length == 0 then
        try
          codec.encode(a) match
            case asRec: IndexedRecord => asRec
            case _                    => record
        catch case NonFatal(_) => record
      else
        AvroWalk.walkPath(record, path) match
          case Left(_)             => record
          case Right((_, parents)) =>
            try
              val encoded = codec.encode(a)
              AvroWalk.rebuildPath(parents, path, encoded).asInstanceOf[IndexedRecord]
            catch case NonFatal(_) => record

    def readImpl(record: IndexedRecord): Option[A] =
      AvroWalk.walkPath(record, path).toOption.flatMap(s => codec.decodeEither(s._1).toOption)

    def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
      AvroWalk.walkPath(record, path) match
        case Left(failure)         => Ior.Both(Chain.one(failure), record)
        case Right((cur, parents)) =>
          decodeOrFail(cur, record)(a =>
            AvroWalk.rebuildPath(parents, path, codec.encode(f(a))).asInstanceOf[IndexedRecord]
          )

    def transformIor(
        record: IndexedRecord,
        f: IndexedRecord => IndexedRecord,
    ): Ior[Chain[AvroFailure], IndexedRecord] =
      AvroWalk.walkPath(record, path) match
        case Left(failure)         => Ior.Both(Chain.one(failure), record)
        case Right((cur, parents)) =>
          cur match
            case asRecord: IndexedRecord =>
              Ior.Right(
                AvroWalk.rebuildPath(parents, path, f(asRecord)).asInstanceOf[IndexedRecord]
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
        AvroWalk.walkPath(record, path) match
          case Left(failure)       => Ior.Both(Chain.one(failure), record)
          case Right((_, parents)) =>
            try
              val encoded = codec.encode(a)
              Ior.Right(AvroWalk.rebuildPath(parents, path, encoded).asInstanceOf[IndexedRecord])
            catch
              case NonFatal(t) =>
                Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

    def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A] =
      AvroWalk.walkPath(record, path) match
        case Left(failure)   => Ior.Left(Chain.one(failure))
        case Right((cur, _)) => decodeOrLeft(cur)

  end Leaf

  /** Multi-field focus — reaches a parent [[IndexedRecord]] via `parentPath`, then assembles a
    * NamedTuple `A` by reading the selected `fieldNames` from it. Powers the multi-field variants
    * (formerly `AvroFieldsPrism` — now a type alias).
    *
    * Per-element atomicity: the NamedTuple is all-or-nothing, so partial reads (some selected
    * fields missing) cannot synthesise an `A` — the chain accumulates one `AvroFailure.PathMissing`
    * per missing field, and the modify family preserves the input unchanged at that location.
    */
  final class Fields[A] private[avro] (
      private[avro] val parentPath: Array[PathStep],
      private[avro] val fieldNames: Array[String],
      private[avro] val codec: AvroCodec[A],
  ) extends AvroFocus[A]:

    protected def terminalStep: PathStep = AvroWalk.terminalOf(parentPath)

    /** The NamedTuple's own schema, read off the codec once at construction. The sub-record built
      * for atomic reads / overlays is always allocated under this schema; kindlings derives the NT
      * with field names matching the case-class field names, so the NT's positional layout aligns
      * with the parent record by name.
      */
    private val ntSchema: Schema = codec.schema

    def navigateRaw(record: IndexedRecord): Either[(Throwable, IndexedRecord), A] =
      AvroWalk.walkPath(record, parentPath) match
        case Left(_) =>
          Left((new RuntimeException(s"path missing in ${parentPath.mkString("/")}"), record))
        case Right((cur, _)) =>
          cur match
            case parent: IndexedRecord =>
              readFields(parent) match
                case Right(sub) =>
                  codec.decodeEither(sub) match
                    case Right(a) => Right(a)
                    case Left(t)  => Left((t, record))
                case Left(_) =>
                  Left((new RuntimeException("missing fields"), record))
            case _ =>
              Left((new RuntimeException("parent is not a record"), record))

    /** Atomic read — succeeds with the assembled sub-record only when ALL selected fields are
      * present. Missing fields accumulate as `PathMissing` failures.
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
          val ntField = ntSchema.getField(name)
          // NT schema is derived from the case class' field set; `ntField` is non-null for any
          // selector the macro accepted.
          sub.put(ntField.pos, parent.get(parentField.pos))
        i += 1
      if chain.isEmpty then Right(sub) else Left(chain)

    /** Project the selected fields out of `parent` for transform-shape ops, missing fields default
      * to `null`. Used whenever transform-shape ops need a synthesis of "the focus sub-record"
      * without insisting on atomicity.
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

    /** Overlay the encoder's output for a focus value `a` onto `parent`. Encoders that omit a
      * selected field leave the parent's existing entry untouched.
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
          // The encoder produced a non-record value (shouldn't happen for a NamedTuple under a
          // record schema, but be defensive — leave parent untouched).
          parent

    /** Overlay a foreign sub-record (the result of a `transform`'s user function) onto `parent`. */
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

    /** Walk the parent path and project the terminal as an [[IndexedRecord]]. */
    private def walkParent(
        record: IndexedRecord
    ): Either[AvroFailure, (IndexedRecord, Vector[AnyRef])] =
      AvroWalk.walkPath(record, parentPath).flatMap {
        case (cur, parents) =>
          cur match
            case rec: IndexedRecord => Right((rec, parents))
            case _                  => Left(AvroFailure.NotARecord(terminalStep))
      }

    private def rebuild(newParent: IndexedRecord, parents: Vector[AnyRef]): IndexedRecord =
      AvroWalk.rebuildPath(parents, parentPath, newParent).asInstanceOf[IndexedRecord]

    def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
      walkParent(record) match
        case Left(_)                  => record
        case Right((parent, parents)) =>
          readFields(parent) match
            case Left(_)    => record
            case Right(sub) =>
              codec.decodeEither(sub) match
                case Left(_)  => record
                case Right(a) =>
                  try rebuild(writeFields(parent, f(a)), parents)
                  catch case NonFatal(_) => record

    def transformImpl(record: IndexedRecord, f: IndexedRecord => IndexedRecord): IndexedRecord =
      walkParent(record) match
        case Left(_)                  => record
        case Right((parent, parents)) =>
          try
            val newSub = f(buildSubRecord(parent))
            rebuild(overlayFields(parent, newSub), parents)
          catch case NonFatal(_) => record

    def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
      walkParent(record) match
        case Left(_)                  => record
        case Right((parent, parents)) =>
          try rebuild(writeFields(parent, a), parents)
          catch case NonFatal(_) => record

    def readImpl(record: IndexedRecord): Option[A] =
      walkParent(record).toOption.flatMap {
        case (parent, _) =>
          readFields(parent).toOption.flatMap(sub => codec.decodeEither(sub).toOption)
      }

    def modifyIor(record: IndexedRecord, f: A => A): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)            => Ior.Both(Chain.one(failure), record)
        case Right((parent, parents)) =>
          readFields(parent) match
            case Left(chain) => Ior.Both(chain, record)
            case Right(sub)  =>
              decodeOrFail(sub, record)(a => rebuild(writeFields(parent, f(a)), parents))

    def transformIor(
        record: IndexedRecord,
        f: IndexedRecord => IndexedRecord,
    ): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)            => Ior.Both(Chain.one(failure), record)
        case Right((parent, parents)) =>
          try
            val newSub = f(buildSubRecord(parent))
            Ior.Right(rebuild(overlayFields(parent, newSub), parents))
          catch
            case NonFatal(t) =>
              Ior.Both(Chain.one(AvroFailure.DecodeFailed(terminalStep, t)), record)

    def placeIor(record: IndexedRecord, a: A): Ior[Chain[AvroFailure], IndexedRecord] =
      walkParent(record) match
        case Left(failure)            => Ior.Both(Chain.one(failure), record)
        case Right((parent, parents)) =>
          try Ior.Right(rebuild(writeFields(parent, a), parents))
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
