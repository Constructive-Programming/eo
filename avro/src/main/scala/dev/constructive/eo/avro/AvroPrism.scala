package dev.constructive.eo.avro

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord

/** Specialised Prism from [[org.apache.avro.generic.IndexedRecord]] to a native type `A`. Mirrors
  * `dev.constructive.eo.circe.JsonPrism` for the Avro carrier:
  *
  * {{{
  *   AvroPrism[A] <: Optic[IndexedRecord, IndexedRecord, A, A, Either]
  *   type X = (AvroFailure, IndexedRecord)
  * }}}
  *
  * Two call-surface tiers (via [[AvroOpticOps]]):
  *
  *   - Default Ior-bearing: `modify` / `get` etc. accumulate `Chain[AvroFailure]` on failure;
  *     partial success surfaces as `Ior.Both(chain, inputRecord)`.
  *   - `*Unsafe`: silent pass-through hot path.
  *
  * Triple-input shape `IndexedRecord | Array[Byte] | String` (where `String` is the Avro JSON wire
  * format). The prism caches the root schema in [[rootSchemaCached]] for byte-input decoding.
  *
  * Storage decomposition (Unit 5): an `AvroPrism[A]` holds an [[AvroFocus]] (Leaf vs Fields) and a
  * cached root schema; per-record hooks delegate to the focus.
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

  // Selectable field sugar — `codecPrism[Person].name` lowers to
  // `codecPrism[Person].field(_.name)` via the macro.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  def to(record: IndexedRecord): Either[(AvroFailure, IndexedRecord), A] =
    focus.navigateRaw(record) match
      case Left(failure) => Left((failure, record))
      case Right(any)    =>
        focus.decodeFrom(any) match
          case Right(a) => Right(a)
          case Left(t)  =>
            Left((AvroFailure.DecodeFailed(AvroWalk.terminalOf(path), t), record))

  def from(e: Either[(AvroFailure, IndexedRecord), A]): IndexedRecord =
    e match
      case Left((_, record)) => record
      case Right(a)          => reverseGet(a)

  // ---- Read surface --------------------------------------------------

  /** Decode the focused value, threading parse failures (for `Array[Byte]` input) and walk failures
    * through the Ior channel.
    */
  def get(input: IndexedRecord | Array[Byte] | String): Ior[Chain[AvroFailure], A] =
    AvroFailure.parseInputIor(input, rootSchemaCached).flatMap(focus.readIor)

  /** Encode `a` standalone, returning the codec's [[IndexedRecord]] payload (or a synthesised empty
    * record when the encoded value isn't record-shaped). Counterpart to `JsonPrism.reverseGet`.
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

  // ---- Byte-span surface (slice / graft) -----------------------------
  //
  // Locates the focused field's encoded byte span directly in the binary payload (no
  // IndexedRecord materialised — see AvroBinaryCursor) so the encoded fragment can be hashed,
  // passed around, and spliced into another payload without a decode/re-encode round-trip.

  /** Slice the focused field's encoded value bytes out of a binary payload.
    *
    * The returned [[AvroFragment]] carries the value bytes (union branch index STRIPPED when the
    * prism focuses a union branch), the resolved field / branch schema, and the branch ordinal. The
    * runtime branch must match the prism's focused branch — a payload sitting on a different branch
    * surfaces [[AvroFailure.UnionResolutionFailed]].
    *
    * Array-index steps are unsupported ([[AvroFailure.UnsupportedSpanStep]]); for a `.fields(...)`
    * prism the span addressed is the PARENT record enclosing the selected fields (the selected
    * fields themselves are not contiguous).
    */
  def sliceBytes(bytes: Array[Byte]): Ior[Chain[AvroFailure], AvroFragment] =
    AvroBinaryCursor.locate(bytes, rootSchemaCached, path, strictTerminalUnion = true) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(span)   =>
        Ior.Right(
          AvroFragment(
            java.util.Arrays.copyOfRange(bytes, span.valueStart, span.end),
            span.valueSchema,
            span.branchOrdinal,
          )
        )

  /** Silent counterpart to [[sliceBytes]] — `None` on any failure. */
  def sliceBytesUnsafe(bytes: Array[Byte]): Option[AvroFragment] =
    AvroBinaryCursor
      .locate(bytes, rootSchemaCached, path, strictTerminalUnion = true)
      .toOption
      .map(span =>
        AvroFragment(
          java.util.Arrays.copyOfRange(bytes, span.valueStart, span.end),
          span.valueSchema,
          span.branchOrdinal,
        )
      )

  /** Graft an encoded fragment into a binary payload at the focused field — splice bytes in place
    * of the field's current encoding, decode-free: prefix copy + (when the prism focuses a union
    * branch: the zigzag-encoded index of the prism's focused branch, re-synthesised from the path —
    * the payload's current branch is discarded, so grafting can SWITCH branches) + the fragment
    * bytes + suffix copy.
    *
    * '''NO SCHEMA VALIDATION.''' The fragment bytes are spliced verbatim — this method does NOT
    * check that `fragment` is a valid encoding of the focused field's schema. A fragment encoded
    * under a different (or drifted) schema produces a payload that is silently corrupt until
    * something decodes it. The caller owns schema-fingerprint checks (compare the writer schema /
    * Confluent schema id of the fragment's source against the receiving field's schema) BEFORE
    * grafting.
    */
  def graftBytes(
      bytes: Array[Byte],
      fragment: Array[Byte],
  ): Ior[Chain[AvroFailure], Array[Byte]] =
    AvroBinaryCursor.locate(bytes, rootSchemaCached, path, strictTerminalUnion = false) match
      case Left(failure) => Ior.Left(Chain.one(failure))
      case Right(span)   => Ior.Right(AvroBinaryCursor.splice(bytes, span, fragment))

  /** Silent counterpart to [[graftBytes]] — input bytes pass through unchanged on any failure. Same
    * NO-SCHEMA-VALIDATION caveat as [[graftBytes]].
    */
  def graftBytesUnsafe(bytes: Array[Byte], fragment: Array[Byte]): Array[Byte] =
    AvroBinaryCursor.locate(bytes, rootSchemaCached, path, strictTerminalUnion = false) match
      case Left(_)     => bytes
      case Right(span) => AvroBinaryCursor.splice(bytes, span, fragment)

  /** Re-carrier this prism onto the raw binary payload: the same focus `A`, but with `S = T =
    * Array[Byte]` — the second of the two optics an `AvroPrism` gives you (the prism itself reads /
    * writes through [[IndexedRecord]]; this one reads / writes the wire bytes directly). See
    * [[AvroBytesPrism]] for carrier shape and semantics.
    */
  def bytes: Optic[Array[Byte], Array[Byte], A, A, Affine] =
    AvroBytesPrism(this)

  // ---- Path widening (used by macro extensions) ---------------------

  /** Extend the Leaf path by a field step. Used by [[field]] / `selectDynamic`. */
  private[avro] def widenPath[B](step: String)(using codecB: AvroCodec[B]): AvroPrism[B] =
    widenPathStep[B](PathStep.Field(step))

  /** Extend by an array-index step. Used by [[at]]. */
  private[avro] def widenPathIndex[B](i: Int)(using codecB: AvroCodec[B]): AvroPrism[B] =
    widenPathStep[B](PathStep.Index(i))

  /** Extend by a union-branch step. Used by `.union[Branch]`. */
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

  /** Hand off as an `AvroPrism` whose focus is a Fields focus over `fieldNames`. Used by `.fields`.
    */
  private[avro] def toFieldsPrism[B](
      fieldNames: Array[String]
  )(using codecB: AvroCodec[B]): AvroPrism[B] =
    new AvroPrism[B](new AvroFocus.Fields[B](path, fieldNames, codecB), rootSchemaCached)

  /** Hand off as an `AvroTraversal[B]` over the iterated element type. Used by `.each`. */
  private[avro] def toTraversal[B](using codecB: AvroCodec[B]): AvroTraversal[B] =
    new AvroTraversal[B](
      path,
      new AvroFocus.Leaf[B](Array.empty[PathStep], codecB),
      rootSchemaCached,
    )

end AvroPrism

object AvroPrism:

  /** Root `AvroPrism[S]`, summoning the schema from the codec (`AvroCodec[S].schema`). */
  def codecPrism[S](using codec: AvroCodec[S]): AvroPrism[S] =
    new AvroPrism[S](new AvroFocus.Leaf[S](Array.empty[PathStep], codec), codec.schema)

  /** Root `AvroPrism[S]` with an explicit reader schema (overrides `codec.schema`). Use when
    * loading from `.avsc` / Schema Registry.
    */
  def codecPrism[S](schema: Schema)(using codec: AvroCodec[S]): AvroPrism[S] =
    new AvroPrism[S](new AvroFocus.Leaf[S](Array.empty[PathStep], codec), schema)

  /** `.field(_.x)` — drill via selector lambda. */
  extension [A](o: AvroPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: AvroCodec[B]): AvroPrism[B] =
      ${ AvroPrismMacro.fieldImpl[A, B]('o, 'selector, 'codecB) }

  /** `.at(i)` — drill into the i-th array element / map entry. */
  extension [A](o: AvroPrism[A])

    transparent inline def at(i: Int): Any =
      ${ AvroPrismMacro.atImpl[A]('o, 'i) }

  /** `.union[Branch]` — drill into a union alternative by branch type. */
  extension [A](o: AvroPrism[A])

    transparent inline def union[Branch]: Any =
      ${ AvroPrismMacro.unionImpl[A, Branch]('o) }

  /** `.each` — split into an `AvroTraversal` over the iterated array. */
  extension [A](o: AvroPrism[A])

    transparent inline def each: Any =
      ${ AvroPrismMacro.eachImpl[A]('o) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple over selected fields. */
  extension [A](o: AvroPrism[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ AvroPrismMacro.fieldsImpl[A]('o, 'selectors) }

end AvroPrism
