package dev.constructive.eo.avro

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** A specialised Prism from [[org.apache.avro.generic.IndexedRecord]] to a native type `A`.
  *
  * Mirrors `dev.constructive.eo.circe.JsonPrism` for the Avro carrier:
  *
  * {{{
  *   AvroPrism[A] <: Optic[IndexedRecord, IndexedRecord, A, A, Either]
  *   type X = (AvroFailure, IndexedRecord)
  * }}}
  *
  * '''2026-04-27 (Unit 5).''' The four legacy carriers (`AvroPrism`, `AvroFieldsPrism`, and Units
  * 6/7 traversal siblings) collapse to two real classes by factoring the storage variation along
  * two orthogonal axes — see [[AvroFocus]] for the design note. An `AvroPrism[A]` now holds an
  * `AvroFocus[A]` and a cached root schema; the per-record hooks delegate straight to the focus.
  *
  * Two call-surface tiers (parallel to JsonPrism, supplied via [[AvroOpticOps]]):
  *
  *   - '''Default (Ior-bearing).''' `modify` / `transform` / `place` / `transfer` return
  *     `(IndexedRecord | Array[Byte] | String) => Ior[Chain[AvroFailure], IndexedRecord]`; `get`
  *     returns `Ior[Chain[AvroFailure], A]`. Failures (path miss, non-record / non-array parent,
  *     out-of-range index, decode failure, union mismatch) accumulate into `Chain[AvroFailure]`.
  *     Partial success returns `Ior.Both(chain, inputRecord)`.
  *   - '''`*Unsafe` (silent).''' `modifyUnsafe` / `transformUnsafe` / `placeUnsafe` /
  *     `transferUnsafe` / `getOptionUnsafe` ship the silent pass-through hot path: input pass-
  *     through on the modify family, `Option[A]` on the read family.
  *
  * Failure diagnostics on the abstract [[Optic]] surface: the inherited `to` returns
  * `Left((AvroFailure, IndexedRecord))` — the same shape `JsonPrism` ships as
  * `(DecodingFailure, HCursor)`: a structured failure paired with the recoverable input. Walk
  * misses surface the relevant `AvroFailure.PathMissing` / `NotARecord` / etc. cases; decode
  * failures surface `AvroFailure.DecodeFailed(step, cause)` carrying kindlings' underlying
  * Throwable on the `cause` side.
  *
  * '''D6 / OQ-avro-5 (plan).''' The prism caches the root schema in [[rootSchema]] (read off the
  * user-supplied `AvroCodec[Root]` at construction time, OR accepted explicitly via the
  * `codecPrism[S](schema)` overload). The schema is needed to decode `Array[Byte]` inputs at the
  * dual-input boundary; pinning it here means callers don't have to thread it through every
  * `.modify` / `.get` call.
  *
  * '''Per OQ-avro-3 (Unit 10).''' The triple-input shape is `IndexedRecord | Array[Byte] | String`
  * — `String` is the Avro JSON wire format, parsed via apache-avro's `JsonDecoder`. The original
  * plan deferred `String` to v0.2; Unit 10 lifted it into v0.1.0 because the parser shape mirrors
  * the binary path one-for-one.
  *
  * '''Per OQ-avro-7:''' [[AvroOpticOps]] is a deliberate copy-paste of `JsonOpticOps`; a future
  * `core.OpticOps[Carrier, Failure, A]` generalisation lands when the third cursor module appears.
  */
final class AvroPrism[A] private[avro] (
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends Optic[IndexedRecord, IndexedRecord, A, A, Either],
      AvroOpticOps[A],
      Dynamic:

  type X = (AvroFailure, IndexedRecord)

  override protected def rootSchema: Schema = rootSchemaCached

  /** The focus's storage path (`path` for a Leaf focus, `parentPath` for a Fields focus). Used by
    * the `widenPath*` helpers (which only fire on Leaf focuses) and by tests that introspect the
    * cursor position.
    */
  private[avro] def path: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  /** Codec accessor — kept for the `widenPath` paths and for backwards compatibility with code that
    * read this field off the prism directly.
    */
  private[avro] def codec: AvroCodec[A] = focus.codec

  // ---- Selectable field sugar ---------------------------------------
  //
  // `codecPrism[Person].name` compiles to
  // `codecPrism[Person].field(_.name)` — no explicit selector lambdas,
  // no extension-method noise. The macro looks the field up on `A`'s
  // case-class schema at compile time, summons its AvroCodec, and
  // delegates to `widenPath`.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  def to: IndexedRecord => Either[(AvroFailure, IndexedRecord), A] = record =>
    focus.navigateRaw(record) match
      case Left(failure) => Left((failure, record))
      case Right(any)    =>
        focus.decodeFrom(any) match
          case Right(a) => Right(a)
          case Left(t)  =>
            Left((AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), t), record))

  def from: Either[(AvroFailure, IndexedRecord), A] => IndexedRecord = {
    case Left((_, record)) => record
    case Right(a)          => reverseGet(a)
  }

  // ---- Read surface --------------------------------------------------

  /** Decode the focused value, threading parse failures (for `Array[Byte]` input) and walk failures
    * through the Ior channel.
    */
  def get(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(focus.readIor)

  /** Construct a record-shaped value from `a` by encoding through the codec. Counterpart to
    * `JsonPrism.reverseGet` — encodes `a` standalone, returning the codec's [[IndexedRecord]]
    * payload (or a synthesised empty record when the encoded value isn't record-shaped).
    */
  def reverseGet(a: A): IndexedRecord =
    focus.codec.encode(a) match
      case any: IndexedRecord => any
      case _                  =>
        new org.apache.avro.generic.GenericData.Record(rootSchemaCached)

  /** Silent counterpart to [[get]] — `None` on any failure. */
  inline def getOptionUnsafe(input: IndexedRecord | Array[Byte] | String): Option[A] =
    focus.readImpl(AvroFailure.parseInputUnsafe(input, rootSchemaCached))

  // ---- Per-record Ior-bearing hooks (delegate to focus) -------------

  override protected def modifyIor(
      record: IndexedRecord,
      f: A => A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.modifyIor(record, f)

  override protected def transformIor(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.transformIor(record, f)

  override protected def placeIor(
      record: IndexedRecord,
      a: A,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    focus.placeIor(record, a)

  // ---- Per-record silent (*Unsafe) hooks (delegate to focus) --------

  override protected def modifyImpl(record: IndexedRecord, f: A => A): IndexedRecord =
    focus.modifyImpl(record, f)

  override protected def transformImpl(
      record: IndexedRecord,
      f: IndexedRecord => IndexedRecord,
  ): IndexedRecord =
    focus.transformImpl(record, f)

  override protected def placeImpl(record: IndexedRecord, a: A): IndexedRecord =
    focus.placeImpl(record, a)

  // ---- Path widening (used by macro extensions) ---------------------

  /** Extend the leaf focus's path by a field step and swap to a narrower codec. Only valid when the
    * current focus is a Leaf (the `field` macro never composes Fields focuses with further
    * `.field`). Used by [[field]] / `selectDynamic`.
    */
  private[avro] def widenPath[B](step: String)(using codecB: AvroCodec[B]): AvroPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend the leaf focus's path by an array-index step. Used by [[at]]. */
  private[avro] def widenPathIndex[B](i: Int)(using codecB: AvroCodec[B]): AvroPrism[B] =
    widenPathStep[B](PathStep.Index(i))

  /** Extend the leaf focus's path by a union-branch step. Used by `.union[Branch]` macro (Unit 8
    * ships the macro entry; the helper is in place for early adopters / hand-written paths).
    */
  private[avro] def widenPathUnion[B](
      branchName: String
  )(using codecB: AvroCodec[B]): AvroPrism[B] =
    widenPathStep[B](PathStep.UnionBranch(branchName))

  private def widenPathStep[B](
      step: PathStep
  )(using codecB: AvroCodec[B]): AvroPrism[B] =
    val newPath = new Array[PathStep](path.length + 1)
    System.arraycopy(path, 0, newPath, 0, path.length)
    newPath(path.length) = step
    new AvroPrism[B](new AvroFocus.Leaf[B](newPath, codecB), rootSchemaCached)

  /** Hand off the current path as a [[AvroPrism]] whose focus is a Fields focus enumerating
    * `fieldNames` under that parent. Used by the `.fields` macro.
    */
  private[avro] def toFieldsPrism[B](
      fieldNames: Array[String]
  )(using codecB: AvroCodec[B]): AvroPrism[B] =
    new AvroPrism[B](new AvroFocus.Fields[B](path, fieldNames, codecB), rootSchemaCached)

  /** Hand off the current path as an [[AvroTraversal]] prefix; the new focus is a Leaf focus over
    * the iterated element type `B`. Used by the `.each` macro. The prism's cached root schema
    * threads straight through — the traversal needs it for `Array[Byte]` parsing at the dual-input
    * boundary.
    */
  private[avro] def toTraversal[B](using codecB: AvroCodec[B]): AvroTraversal[B] =
    new AvroTraversal[B](
      path,
      new AvroFocus.Leaf[B](Array.empty[PathStep], codecB),
      rootSchemaCached,
    )

end AvroPrism

object AvroPrism:

  /** Construct a root-level `AvroPrism[S]`, summoning the schema from the codec.
    *
    * Per OQ-avro-5: the schema is read off `AvroCodec[S].schema`. Kindlings' `AvroSchemaFor[A]`
    * always resolves a schema for any derivable type, so this never throws on the happy path.
    */
  def codecPrism[S](using codec: AvroCodec[S]): AvroPrism[S] =
    new AvroPrism[S](new AvroFocus.Leaf[S](Array.empty[PathStep], codec), codec.schema)

  /** Construct a root-level `AvroPrism[S]` with an explicit reader schema. Use when the schema is
    * loaded at runtime from an `.avsc` file or a Schema Registry rather than derived from the codec
    * — the user-supplied schema overrides whatever `codec.schema` would produce.
    */
  def codecPrism[S](schema: Schema)(using codec: AvroCodec[S]): AvroPrism[S] =
    new AvroPrism[S](new AvroFocus.Leaf[S](Array.empty[PathStep], codec), schema)

  /** `.field(_.x)` — drill into a named field via a selector lambda. The macro extracts the field
    * name at compile time, summons the inner [[AvroCodec]], and emits `widenPath`.
    */
  extension [A](o: AvroPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: AvroCodec[B]): AvroPrism[B] =
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

  /** `.each` — split into an [[AvroTraversal]] over the iterated array's elements. */
  extension [A](o: AvroPrism[A])

    transparent inline def each: Any =
      ${ AvroPrismMacro.eachImpl[A]('o) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple over selected fields. Returns an `AvroPrism[NT]`
    * whose focus is an [[AvroFocus.Fields]] (with the [[AvroFieldsPrism]] alias still pointing at
    * the same shape).
    */
  extension [A](o: AvroPrism[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ AvroPrismMacro.fieldsImpl[A]('o, 'selectors) }

end AvroPrism
