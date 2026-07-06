package dev.constructive.eo.avro

import scala.language.dynamics

import cats.data.{Chain, Ior}
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema

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
  * re-synthesised). The ROOT object is never materialised — neither as a case class nor as a
  * generic record; a record-SHAPED focus is materialised only as that branch's own
  * [[org.apache.avro.generic.IndexedRecord]] during the slice decode / re-encode. Mirrors
  * [[dev.constructive.eo.jsoniter.JsoniterPrism]] shape-for-shape:
  *
  *   - `Fst[X] = Array[Byte]` — original payload; `Miss` carries it for pass-through (parse
  *     failure, path miss, union-branch mismatch, decode failure, unsupported index step).
  *   - `Snd[X] = (Array[Byte], BinarySpan)` — payload + located span, so `from` can splice.
  *
  * The whole capability-gated extension surface lights up on the bytes — `.getOption`, `.modify`,
  * `.replace`, `.foldMap`, `.andThen`, … Drill with the same macro sugar as ever (`.field(_.x)` /
  * `.fields(...)` / `.union[B]` / `.at(i)` / `.each` / Dynamic field selection).
  *
  * '''Field navigation honours the SCHEMA field name (issue #35).''' `.field(_.x)` (and `.fields`,
  * `selectDynamic`, the traversal siblings) resolve the case-class field `x` to whatever schema
  * field the codec actually emitted for it — under any name transform (kindlings snake / kebab /
  * custom `transformFieldNames`, or a vulcan per-field override map) — by DECLARATION POSITION: the
  * i-th case field maps to the i-th schema field, read back out of the cached schema at
  * construction time (zero per-operation cost). The rare hand-written codec whose schema field
  * ORDER diverges from declaration order needs [[AvroPrism.fieldNamed]]`("schema_name")` to
  * navigate by the explicit schema name instead. Map keys are data, not schema-named fields, and
  * keep their literal key.
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
  *
  * '''Laws & preconditions''' (normative):
  *
  *   - The Optional laws hold '''up to canonical re-encoding of the focused slice''':
  *     `modify(identity)` re-encodes the focus, so a payload whose focused slice used non-canonical
  *     (but spec-legal) encodings — non-minimal varints, byte-sized array blocks — comes back
  *     canonicalised. Byte-for-byte identity holds for payloads from conformant writers
  *     (apache-avro's own encoders included); put-get and put-put hold unconditionally.
  *   - '''Writes require a decodable current focus''': the Affine `to` decodes eagerly, so
  *     `.replace` onto a span whose current value doesn't decode as `A` — or a `.union[B]` focus
  *     sitting on a different runtime branch — is a Miss pass-through. [[graftBytes]] is the
  *     decode-free write (and the only one that can SWITCH union branches).
  *   - '''The payload must be encoded under exactly this prism's reader schema.''' The byte walk
  *     performs no writer/reader schema resolution: structurally drifted payloads Miss silently,
  *     and a same-typed field REORDER between writer and reader is undetectable from the bytes —
  *     the walk reads the wrong field with full confidence. Confluent-framed payloads are handled
  *     by composing [[ConfluentWire.confluent]] (a byte Prism that strips the header, resolves the
  *     writer schema, and fingerprint-gates) BEFORE this optic — `confluent.andThen(thisWalk)`;
  *     past a fingerprint mismatch a mixed-schema topic still needs a resolving decode (the record
  *     face with the right schema per payload).
  *   - Dynamic field sugar is shadowed by real members: an Avro field named like a member of this
  *     class (`record`, `field`, `at`, `union`, `each`, `fields`, …) must be drilled with the
  *     explicit `.field(_.record)` form.
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
        AvroBinaryCursor.decodeSlice(bytes, span, codec) match
          case Right(a) => new Affine.Hit[X, A]((bytes, span), a)
          case Left(_)  => new Affine.Miss[X, A](bytes)

  private def spliceAff(aff: Affine[X, A]): Array[Byte] =
    aff match
      case m: Affine.Miss[X, A] => m.fst
      case h: Affine.Hit[X, A]  =>
        val (src, span) = h.snd
        val encoded = focus match
          // A Fields span addresses the PARENT record — overlay the NT fields by name onto the
          // decoded parent slice (a positional write under the parent schema would swap or drop
          // values; see AvroBinaryCursor.encodeFieldsOverlay).
          case f: AvroFocus.Fields[A] => AvroBinaryCursor.encodeFieldsOverlay(src, span, f, h.b)
          case _: AvroFocus.Leaf[A]   => AvroBinaryCursor.encodeValue(h.b, span, codec)
        encoded match
          // ponytail: silent pass-through on encode failure — from has no failure channel;
          // callers needing diagnostics use the .record Ior surface
          case Left(_)        => src
          case Right(encoded) => AvroBinaryCursor.splice(src, span, encoded)

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

  /** Extend the Leaf path by a field step. Used by [[field]] / `selectDynamic`. `scalaName` is the
    * case-class field name and `declIdx` its declaration index; the actual schema field name (which
    * may differ under a snake/kebab/custom transform or vulcan overrides) is resolved off the
    * cached schema by position — see [[AvroWalk.resolveFieldName]] (issue #35).
    */
  private[avro] def widenPath[B](scalaName: String, declIdx: Int)(using
      codecB: AvroCodec[B]
  ): AvroPrism[B] =
    widenPathStep[B](
      PathStep.Field(
        AvroWalk.resolveFieldName(rootSchemaCached, path, scalaName, declIdx, "AvroPrism.field")
      )
    )

  /** Extend the Leaf path by an EXPLICIT schema field name — the escape hatch for a hand-written
    * codec whose schema field order diverges from case-class declaration order (position resolution
    * would land on the wrong field). Used by [[fieldNamed]].
    */
  private[avro] def widenPathNamed[B](schemaName: String)(using
      codecB: AvroCodec[B]
  ): AvroPrism[B] =
    widenPathStep[B](PathStep.Field(schemaName))

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

  /** Hand off as an `AvroPrism` whose focus is a Fields focus over the selected fields. Used by
    * `.fields`. `scalaNames` / `declIdxs` align 1:1 in selector order; each is resolved to the
    * actual schema field name by position (issue #35) so the parent-record overlay hits under any
    * codec name transform.
    */
  private[avro] def toFieldsPrism[B](
      scalaNames: Array[String],
      declIdxs: Array[Int],
  )(using codecB: AvroCodec[B]): AvroPrism[B] =
    new AvroPrism[B](
      new AvroFocus.Fields[B](path, resolveFieldNames(scalaNames, declIdxs), codecB),
      rootSchemaCached,
    )

  /** Resolve a selector-order batch of `(scalaName, declIdx)` to schema field names against this
    * prism's parent record. Shared by [[toFieldsPrism]].
    */
  private def resolveFieldNames(
      scalaNames: Array[String],
      declIdxs: Array[Int],
  ): Array[String] =
    Array.tabulate(scalaNames.length)(i =>
      AvroWalk.resolveFieldName(
        rootSchemaCached,
        path,
        scalaNames(i),
        declIdxs(i),
        "AvroPrism.fields",
      )
    )

  /** Hand off as an `AvroTraversal[B]` over the iterated element type. Used by `.each`. */
  private[avro] def toTraversal[B](using codecB: AvroCodec[B]): AvroTraversal[B] =
    new AvroTraversal[B](
      path,
      new AvroFocus.Leaf[B](Array.empty[PathStep], codecB),
      rootSchemaCached,
    )

end AvroPrism

object AvroPrism:

  /** Root `AvroPrism[S]`, summoning the schema from the codec (`AvroCodec[S].schema`). The reader
    * schema always matches the codec's — there is no explicit-schema overload, since a reader
    * schema diverging from its codec is a footgun (silent misreads on structural drift).
    */
  def codecPrism[S](using codec: AvroCodec[S]): AvroPrism[S] =
    new AvroPrism[S](new AvroFocus.Leaf[S](Array.empty[PathStep], codec), codec.schema)

  /** `.field(_.x)` — drill via selector lambda. */
  extension [A](o: AvroPrism[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: AvroCodec[B]): AvroPrism[B] =
      ${ AvroPrismMacro.fieldImpl[A, B]('o, 'selector, 'codecB) }

  /** `.fieldNamed[B]("schema_name")` — drill by the EXPLICIT schema field name, bypassing position
    * resolution. The escape hatch (issue #35) for a hand-written codec whose schema field order
    * diverges from case-class declaration order; the common (derived / order-preserving) codecs
    * need `.field(_.x)` instead, which resolves the name for you.
    */
  extension [A](o: AvroPrism[A])

    def fieldNamed[B](schemaName: String)(using codecB: AvroCodec[B]): AvroPrism[B] =
      o.widenPathNamed[B](schemaName)

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
