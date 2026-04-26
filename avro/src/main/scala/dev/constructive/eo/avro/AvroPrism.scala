package dev.constructive.eo.avro

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import vulcan.{AvroError, Codec}

/** A specialised Prism from [[org.apache.avro.generic.IndexedRecord]] to a native type `A`.
  *
  * Mirrors `dev.constructive.eo.circe.JsonPrism` for the Avro carrier:
  *
  * {{{
  *   AvroPrism[A] <: Optic[IndexedRecord, IndexedRecord, A, A, Either]
  *   type X = (vulcan.AvroError, IndexedRecord)
  * }}}
  *
  * Two call-surface tiers (parallel to JsonPrism, supplied via [[AvroOpticOps]]):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` return
  *     `(IndexedRecord | Array[Byte]) => Ior[Chain[AvroFailure], IndexedRecord]`; `get` returns
  *     `Ior[Chain[AvroFailure], A]`. Failures (path miss, non-record / non-array parent,
  *     out-of-range index, decode failure, union mismatch) accumulate into `Chain[AvroFailure]`.
  *     Partial success returns `Ior.Both(chain, inputRecord)`.
  *   - '''`*Unsafe` (silent).''' `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` /
  *     `transferUnsafe` / `getOptionUnsafe` ship the silent pass-through hot path: input pass-
  *     through on the modify family, `Option[A]` on the read family.
  *
  * Failure diagnostics on the abstract [[Optic]] surface: the inherited `to` returns
  * `Left((AvroError, IndexedRecord))` so callers routing through the generic cats-eo extensions can
  * both diagnose (`AvroError.message`) and recover the input record (the second element of the
  * pair).
  *
  * '''D6 / OQ-avro-5 (plan).''' The prism caches the root schema in [[rootSchema]] (read off the
  * user-supplied [[Codec.schema]] at construction time, OR accepted explicitly via the
  * `codecPrism[S](schema)` overload). The schema is needed to decode `Array[Byte]` inputs at the
  * dual-input boundary; pinning it here means callers don't have to thread it through every
  * `.modify` / `.get` call.
  *
  * '''Per OQ-avro-3:''' the dual-input shape is `IndexedRecord | Array[Byte]` only at v0.1.0; the
  * `String` (Avro JSON wire format) overload is deferred to Unit 10 / v0.2.x.
  *
  * '''Per OQ-avro-7:''' [[AvroOpticOps]] is a deliberate copy-paste of `JsonOpticOps`; a future
  * `core.OpticOps[Carrier, Failure, A]` generalisation lands when the third cursor module appears.
  */
final class AvroPrism[A] private[avro] (
    private[avro] val path: Array[PathStep],
    private[avro] val codec: Codec[A],
    private[avro] val rootSchemaCached: Schema,
    private[avro] val leafSchema: Schema,
) extends Optic[IndexedRecord, IndexedRecord, A, A, Either],
      AvroOpticOps[A],
      Dynamic:

  type X = (AvroError, IndexedRecord)

  override protected def rootSchema: Schema = rootSchemaCached

  // ---- Selectable field sugar ---------------------------------------
  //
  // `codecPrism[Person].name` compiles to
  // `codecPrism[Person].field(_.name)` — no explicit selector lambdas,
  // no extension-method noise. The macro looks the field up on `A`'s
  // case-class schema at compile time, summons its Codec, and delegates
  // to `widenPath`.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  def to: IndexedRecord => Either[(AvroError, IndexedRecord), A] = record =>
    AvroWalk.walkPath(record, path) match
      case Left(_) =>
        // Path-walk failures don't have a vulcan.AvroError to surface; synthesise one.
        Left((AvroError(s"path missing in ${path.mkString("/")}"), record))
      case Right((cur, _)) =>
        codec.decode(cur, leafSchema) match
          case Right(a) => Right(a)
          case Left(e)  => Left((e, record))

  def from: Either[(AvroError, IndexedRecord), A] => IndexedRecord = {
    case Left((_, record)) => record
    case Right(a)          =>
      // No prior record context — best effort: encode `a` standalone.
      // The encoder produces an `Any` payload; callers should typically
      // route through `place(a)` instead, which has a record context.
      codec.encode(a) match
        case Right(any: IndexedRecord) => any
        case _                         =>
          // The encoded value isn't itself an IndexedRecord (e.g. a primitive).
          // We can't manufacture a root record here; surface a sentinel
          // empty record built from the cached root schema.
          new org.apache.avro.generic.GenericData.Record(rootSchemaCached)
  }

  // ---- Read surface --------------------------------------------------

  /** Decode the focused value, threading parse failures (for `Array[Byte]` input) and walk failures
    * through the Ior channel.
    */
  def get(input: IndexedRecord | Array[Byte]): Ior[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(readIor)

  /** Construct a record-shaped value from `a` by encoding through the codec. Counterpart to
    * `JsonPrism.reverseGet` — encodes `a` standalone, returning the codec's [[IndexedRecord]]
    * payload (or a synthesised empty record on encode failure).
    */
  def reverseGet(a: A): IndexedRecord =
    codec.encode(a) match
      case Right(any: IndexedRecord) => any
      case _                         =>
        new org.apache.avro.generic.GenericData.Record(rootSchemaCached)

  /** Silent counterpart to [[get]] — `None` on any failure. */
  inline def getOptionUnsafe(input: IndexedRecord | Array[Byte]): Option[A] =
    readImpl(AvroFailure.parseInputUnsafe(input, rootSchemaCached))

  // ---- Per-record Ior-bearing hooks ---------------------------------

  override protected def modifyIor(
      record: IndexedRecord,
      f: A => A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    AvroWalk.walkPath(record, path) match
      case Left(failure)         => Ior.Both(Chain.one(failure), record)
      case Right((cur, parents)) =>
        codec.decode(cur, leafSchema) match
          case Left(e) =>
            Ior.Both(
              Chain.one(AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), e)),
              record,
            )
          case Right(a) =>
            codec.encode(f(a)) match
              case Right(encoded) =>
                Ior.Right(
                  AvroWalk
                    .rebuildPath(parents, path, encoded)
                    .asInstanceOf[IndexedRecord]
                )
              case Left(e) =>
                Ior.Both(
                  Chain.one(AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), e)),
                  record,
                )

  override protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    AvroWalk.walkPath(record, path) match
      case Left(failure)         => Ior.Both(Chain.one(failure), record)
      case Right((cur, parents)) =>
        cur match
          case asRecord: IndexedRecord =>
            Ior.Right(
              AvroWalk
                .rebuildPath(parents, path, f(asRecord))
                .asInstanceOf[IndexedRecord]
            )
          case _ =>
            Ior.Both(Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(path))), record)

  override protected def placeIor(
      record: IndexedRecord,
      a: A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    if path.length == 0 then
      codec.encode(a) match
        case Right(asRec: IndexedRecord) => Ior.Right(asRec)
        case Right(_)                    =>
          // Not a record at the root — the root schema isn't a record type.
          Ior.Both(
            Chain.one(AvroFailure.NotARecord(AvroWalk.terminalOf(path))),
            record,
          )
        case Left(e) =>
          Ior.Both(Chain.one(AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), e)), record)
    else
      AvroWalk.walkPath(record, path) match
        case Left(failure)       => Ior.Both(Chain.one(failure), record)
        case Right((_, parents)) =>
          codec.encode(a) match
            case Right(encoded) =>
              Ior.Right(
                AvroWalk
                  .rebuildPath(parents, path, encoded)
                  .asInstanceOf[IndexedRecord]
              )
            case Left(e) =>
              Ior.Both(Chain.one(AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), e)), record)

  // ---- Per-record silent (*Unsafe) hooks ----------------------------

  override protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
    AvroWalk.walkPath(record, path) match
      case Left(_)               => record
      case Right((cur, parents)) =>
        codec.decode(cur, leafSchema) match
          case Left(_)  => record
          case Right(a) =>
            codec.encode(f(a)) match
              case Left(_)        => record
              case Right(encoded) =>
                AvroWalk
                  .rebuildPath(parents, path, encoded)
                  .asInstanceOf[IndexedRecord]

  override protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord =
    AvroWalk.walkPath(record, path) match
      case Left(_)               => record
      case Right((cur, parents)) =>
        cur match
          case asRecord: IndexedRecord =>
            AvroWalk
              .rebuildPath(parents, path, f(asRecord))
              .asInstanceOf[IndexedRecord]
          case _ => record

  override protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
    if path.length == 0 then
      codec.encode(a) match
        case Right(asRec: IndexedRecord) => asRec
        case _                           => record
    else
      AvroWalk.walkPath(record, path) match
        case Left(_)             => record
        case Right((_, parents)) =>
          codec.encode(a) match
            case Right(encoded) =>
              AvroWalk
                .rebuildPath(parents, path, encoded)
                .asInstanceOf[IndexedRecord]
            case Left(_) => record

  // ---- Read hooks ----------------------------------------------------

  private def readIor(record: IndexedRecord): Ior[Chain[AvroFailure], A] =
    AvroWalk.walkPath(record, path) match
      case Left(failure)   => Ior.Left(Chain.one(failure))
      case Right((cur, _)) =>
        codec.decode(cur, leafSchema) match
          case Right(a) => Ior.Right(a)
          case Left(e)  =>
            Ior.Left(Chain.one(AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), e)))

  private def readImpl(record: IndexedRecord): Option[A] =
    AvroWalk.walkPath(record, path).toOption.flatMap { s =>
      codec.decode(s._1, leafSchema).toOption
    }

  // ---- Path widening (used by macro extensions) ---------------------

  /** Extend the path by a field step and swap to a narrower codec. Used by [[field]] /
    * `selectDynamic`.
    *
    * The new prism's `leafSchema` is computed from the parent's `leafSchema` by looking up the
    * named field's schema. If the parent's leaf isn't a record (or the field doesn't exist), we
    * fall back to the child codec's own schema — at walk-time the runtime walker will surface
    * `PathMissing` / `NotARecord` if the schema-driven step doesn't actually exist in the record's
    * runtime shape.
    */
  private[avro] def widenPath[B](step: String)(using codecB: Codec[B]): AvroPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = PathStep.Field(step)

    val nextLeafSchema: Schema =
      if leafSchema.getType == Schema.Type.RECORD then
        val f = leafSchema.getField(step)
        if f != null then f.schema else codecB.schema.toOption.getOrElse(leafSchema)
      else codecB.schema.toOption.getOrElse(leafSchema)

    new AvroPrism[B](newPath, codecB, rootSchemaCached, nextLeafSchema)

  /** Extend the path by an array-index step. Used by [[at]]. */
  private[avro] def widenPathIndex[B](i: Int)(using codecB: Codec[B]): AvroPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = PathStep.Index(i)

    val nextLeafSchema: Schema =
      if leafSchema.getType == Schema.Type.ARRAY then leafSchema.getElementType
      else codecB.schema.toOption.getOrElse(leafSchema)

    new AvroPrism[B](newPath, codecB, rootSchemaCached, nextLeafSchema)

  /** Extend the path by a union-branch step. Used by `.union[Branch]` macro (Unit 8 ships the macro
    * entry; the helper is in place for early adopters / hand-written paths).
    */
  private[avro] def widenPathUnion[B](
      branchName: String
  )(using codecB: Codec[B]): AvroPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = PathStep.UnionBranch(branchName)

    val nextLeafSchema: Schema =
      codecB.schema.toOption.getOrElse(leafSchema)

    new AvroPrism[B](newPath, codecB, rootSchemaCached, nextLeafSchema)

end AvroPrism

object AvroPrism:

  /** Construct a root-level `AvroPrism[S]`, summoning the schema from the codec.
    *
    * Per OQ-avro-5: the schema is read off `Codec[S].schema`; if the codec's schema fails to
    * resolve, this throws (the codec's own `validate` step would have failed earlier in any
    * realistic build).
    */
  def codecPrism[S](using codec: Codec[S]): AvroPrism[S] =
    val schema = codec.schema match
      case Right(s) => s
      case Left(e)  => sys.error(s"Codec[${codec}] has no resolvable schema: $e")
    new AvroPrism[S](Array.empty[PathStep], codec, schema, schema)

  /** Construct a root-level `AvroPrism[S]` with an explicit reader schema. Use when the schema is
    * loaded at runtime from an `.avsc` file or a Schema Registry rather than derived from the codec
    * — the user-supplied schema overrides whatever `codec.schema` would produce.
    */
  def codecPrism[S](schema: Schema)(using codec: Codec[S]): AvroPrism[S] =
    new AvroPrism[S](Array.empty[PathStep], codec, schema, schema)

  /** `.field(_.x)` — drill into a named field via a selector lambda. The macro extracts the field
    * name at compile time, summons the inner [[Codec]], and emits `widenPath`.
    */
  extension [A](o: AvroPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: Codec[B]): AvroPrism[B] =
      ${ AvroPrismMacro.fieldImpl[A, B]('o, 'selector, 'codecB) }

  /** `.at(i)` — drill into the i-th element of an array (or the value for the i-th map entry).
    * Requires the parent focus `A` to be a Scala collection.
    */
  extension [A](o: AvroPrism[A])

    transparent inline def at(i: Int): Any =
      ${ AvroPrismMacro.atImpl[A]('o, 'i) }

  /** `.union[Branch]` — drill into a union alternative by branch type. Skeleton entry; Unit 8 lands
    * the full macro implementation including schema-side branch-name resolution.
    */
  extension [A](o: AvroPrism[A])

    transparent inline def union[Branch]: Any =
      ${ AvroPrismMacro.unionImpl[A, Branch]('o) }

end AvroPrism
