package dev.constructive.eo.avro

import scala.language.dynamics
import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}

/** Optic from the Avro BINARY WIRE FORM to a native type `A` — the wire bytes are the default
  * carrier:
  *
  * {{{
  *   AvroPrism[A] <: Optic[Array[Byte], Array[Byte], A, A, Affine]
  *   type X = (Array[Byte], (Array[Byte], BinarySpan))
  * }}}
  *
  * Reads locate the focused field's byte span via [[AvroBinaryCursor]] and decode only that slice;
  * writes re-encode the focus and splice it in place (three `arraycopy`s, union branch index
  * re-synthesised) — no [[org.apache.avro.generic.IndexedRecord]] materialised on either side.
  * Mirrors [[dev.constructive.eo.jsoniter.JsoniterPrism]] shape-for-shape:
  *
  *   - `Fst[X] = Array[Byte]` — original payload; `Miss` carries it for pass-through (parse
  *     failure, path miss, union-branch mismatch, decode failure, unsupported index step).
  *   - `Snd[X] = (Array[Byte], BinarySpan)` — payload + located span, so `from` can splice.
  *
  * The whole capability-gated extension surface lights up on the bytes — `.getOption`, `.modify`,
  * `.replace`, `.foldMap`, `.andThen`, … Drill with the same macro sugar as ever (`.field(_.x)` /
  * `.fields(...)` / `.union[B]` / `.at(i)` / `.each` / Dynamic field selection).
  *
  * Two sibling surfaces, one mechanism each (deliberately NOT duplicated here):
  *
  *   - [[record]] — the [[IndexedRecord]]-carried optic ([[AvroRecordPrism]]) with the Ior-bearing
  *     diagnostic surface (`get` / `modify` / `place` / `transfer` + `*Unsafe`) over
  *     `IndexedRecord | Array[Byte] | String` input. Use it when you hold parsed records or need
  *     accumulated [[AvroFailure]] diagnostics.
  *   - [[sliceBytes]] / [[graftBytes]] — the encoded-fragment surface for hashing / shipping /
  *     splicing a field's raw encoding across payloads without decoding the focus at all.
  *
  * Storage decomposition (Unit 5): an `AvroPrism[A]` holds an [[AvroFocus]] (Leaf vs Fields) and a
  * cached root schema; the byte walk uses the focus's path, the slice decode uses its codec.
  */
final class AvroPrism[A] private[avro] (
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends Optic[Array[Byte], Array[Byte], A, A, Affine],
      Dynamic:

  type X = (Array[Byte], (Array[Byte], AvroBinaryCursor.BinarySpan))

  /** The focus's storage path (`path` for a Leaf focus, `parentPath` for a Fields focus). Used by
    * the byte walk, the `widenPath*` helpers, and tests that introspect the cursor position.
    */
  private[avro] def path: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  /** Codec accessor — the slice decode / splice encode side of the focus. */
  private[avro] def codec: AvroCodec[A] = focus.codec

  // Selectable field sugar — `codecPrism[Person].name` lowers to
  // `codecPrism[Person].field(_.name)` via the macro.

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  // 1-call forwarders; the big bodies live in private methods so the abstract-`def` entry stays
  // a few bytes and inlines at hot use sites (same PrintInlining rationale as JsoniterPrism).
  def to(bytes: Array[Byte]): Affine[X, A] = scan(bytes)
  def from(aff: Affine[X, A]): Array[Byte] = spliceAff(aff)

  private def scan(bytes: Array[Byte]): Affine[X, A] =
    AvroBinaryCursor.locate(bytes, rootSchemaCached, path, strictTerminalUnion = true) match
      case Left(_)     => new Affine.Miss[X, A](bytes)
      case Right(span) =>
        decodeSlice(bytes, span) match
          case Right(a) => new Affine.Hit[X, A]((bytes, span), a)
          case Left(_)  => new Affine.Miss[X, A](bytes)

  private def decodeSlice(
      bytes: Array[Byte],
      span: AvroBinaryCursor.BinarySpan,
  ): Either[Throwable, A] =
    try
      val reader = new GenericDatumReader[Any](span.valueSchema)
      val decoder = DecoderFactory
        .get()
        .binaryDecoder(bytes, span.valueStart, span.end - span.valueStart, null)
      codec.decodeEither(reader.read(null, decoder))
    catch case NonFatal(t) => Left(t)

  private def spliceAff(aff: Affine[X, A]): Array[Byte] =
    aff match
      case m: Affine.Miss[X, A] => m.fst
      case h: Affine.Hit[X, A]  =>
        val (src, span) = h.snd
        encodeValue(h.b, span) match
          // ponytail: silent pass-through on encode failure — from has no failure channel;
          // callers needing diagnostics use the .record Ior surface
          case Left(_)        => src
          case Right(encoded) => AvroBinaryCursor.splice(src, span, encoded)

  private def encodeValue(
      a: A,
      span: AvroBinaryCursor.BinarySpan,
  ): Either[Throwable, Array[Byte]] =
    try
      val out = new ByteArrayOutputStream()
      val writer = new GenericDatumWriter[Any](span.valueSchema)
      val encoder = EncoderFactory.get().binaryEncoder(out, null)
      writer.write(codec.encode(a), encoder)
      encoder.flush()
      Right(out.toByteArray)
    catch case NonFatal(t) => Left(t)

  // ---- Record-carried face -------------------------------------------

  /** The [[IndexedRecord]]-carried counterpart of this prism — same focus, but
    * `Optic[IndexedRecord, IndexedRecord, A, A, Either]` plus the Ior-bearing diagnostic surface.
    * Drill here on the byte prism, then flip at the end:
    *
    * {{{
    *   codecPrism[Person].field(_.name).record.modify(_.toUpperCase)(record)
    * }}}
    */
  def record: AvroRecordPrism[A] = new AvroRecordPrism[A](focus, rootSchemaCached)

  // ---- Byte-span surface (slice / graft) -----------------------------
  //
  // The encoded-FRAGMENT face of the same locate machinery the optic's to/from use: where
  // .getOption / .modify decode and re-encode the focus, these keep it as raw bytes so the
  // fragment can be hashed, passed around, and spliced into another payload with no decode at all.

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
