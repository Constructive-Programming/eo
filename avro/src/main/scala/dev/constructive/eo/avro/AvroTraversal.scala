package dev.constructive.eo.avro

import scala.language.dynamics

import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.optics.Traversal
import org.apache.avro.Schema

/** Multi-focus counterpart to [[AvroPrism]], carried on the BINARY WIRE FORM like the prism:
  *
  * {{{
  *   AvroTraversal[A] <: dev.constructive.eo.optics.Traversal[Array[Byte], Array[Byte], A, A]
  *   type X = (Array[Byte], Option[ElementSpans])
  * }}}
  *
  * `to` resolves the prefix to an array-typed field, walks the array's block framing, and locates +
  * slice-decodes the focus inside every element (`AvroBinaryCursor.locateElements`); `from`
  * re-encodes each focus and splices all spans back in one pass. Avro's standard (positive-count)
  * array framing carries no byte lengths, so per-element splices can change size freely; payloads
  * whose array used the negative-count (byte-sized) framing are RE-FRAMED on write — the whole
  * array region is rebuilt as one canonical positive-count block with the focus splices applied
  * (`AvroBinaryCursor.reframeArray`). Same carrier as
  * `dev.constructive.eo.jsoniter.JsoniterTraversal` (`MultiFocus[PSVec]`), so `.foldMap` / `.all` /
  * `.modify` / `.headOption` / same-carrier `.andThen` light up from the standard extension
  * catalogue.
  *
  * Read semantics are "fold the focuses that exist": elements whose focus walk or slice decode
  * refuses keep their bytes but leave the focus set (untouched on write). Whole-walk failure (bad
  * prefix / non-array terminal / truncated bytes) yields no foci and `from` returns the input —
  * silent, like every write-side refusal here (diagnostics live on [[record]]).
  *
  * [[AvroPrism]]'s '''Laws & preconditions''' section applies verbatim: laws hold up to canonical
  * re-encoding of the focused slices, writes need decodable current focuses, and the payload must
  * be encoded under exactly this traversal's reader schema.
  *
  * The record-carried face — the Ior-bearing `modify` / `getAll` / `place` / `transfer` surface
  * over `IndexedRecord | Array[Byte] | String` input — lives behind ONE factory: [[record]]
  * ([[AvroRecordTraversal]]). Drilling (`.field` / `.at` / `.union[Branch]` / `.fields` / Dynamic
  * selection) stays here, the single mechanism; drill first, flip last.
  *
  * The Fields-vs-Leaf split lives entirely inside `focus` (per `AvroFocus`).
  */
final class AvroTraversal[A] private[avro] (
    private[avro] val prefix: Array[PathStep],
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends Traversal[Array[Byte], Array[Byte], A, A],
      Dynamic:

  /** Structural leftover: the original payload plus the located element-span plan (`None` on a
    * whole-walk failure, in which case `from` returns the input).
    */
  type X = (Array[Byte], Option[AvroBinaryCursor.ElementSpans])

  /** The per-element focus path (`path` for a Leaf focus, `parentPath` for a Fields focus). */
  private[avro] def suffix: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  private[avro] def codec: AvroCodec[A] = focus.codec

  // ---- Dynamic field sugar -----------------------------------------

  /** Dynamic field sugar — `basket.items.each.name` lowers to `.field(_.name)` via the macro. An
    * Avro field named like a real member of this class must be drilled with the explicit
    * `.field(_.x)` form.
    */
  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  /** Locate every element's focused span and slice-decode the focuses that exist; whole-walk
    * failure yields no foci (and a `None` plan, so [[from]] passes the input through).
    */
  def to(bytes: Array[Byte]): MultiFocus[PSVec][X, A] = scan(bytes)

  /** Re-encode each focus and splice all spans back in one pass (re-framing the array region when
    * the original framing was byte-sized); any refusal returns the input bytes.
    */
  def from(mf: MultiFocus[PSVec][X, A]): Array[Byte] = spliceFoci(mf)

  private def scan(bytes: Array[Byte]): MultiFocus[PSVec][X, A] =
    AvroBinaryCursor.locateElements(bytes, rootSchemaCached, prefix, suffix) match
      case Left(_)   => MultiFocus((bytes, None), PSVec.empty[A])
      case Right(es) =>
        // Decode each focused span; an element whose decode refuses is DEMOTED to focus-less
        // (its bytes survive writes untouched) so the span↔focus alignment `from` relies on
        // survives.
        val decoded = Vector.newBuilder[A]
        val elements = es
          .elements
          .map { e =>
            e.focus match
              case None       => e
              case Some(span) =>
                AvroBinaryCursor.decodeSlice(bytes, span, codec) match
                  case Right(a) =>
                    decoded += a
                    e
                  case Left(_) => e.copy(focus = None)
          }
        val foci = decoded.result()
        val arr = new Array[AnyRef](foci.length)
        var i = 0
        foci.foreach { a =>
          arr(i) = a.asInstanceOf[AnyRef]
          i += 1
        }
        MultiFocus((bytes, Some(es.copy(elements = elements))), PSVec.unsafeWrap[A](arr))

  private def spliceFoci(mf: MultiFocus[PSVec][X, A]): Array[Byte] =
    val (bytes, planOpt) = mf.context
    val foci = mf.foci
    planOpt match
      case None       => bytes
      case Some(plan) =>
        val active = plan.elements.flatMap(_.focus)
        if active.isEmpty || active.length != foci.length then bytes
        else
          // Encode each focus (leaf: value under the span schema; fields: by-name overlay onto
          // the parent slice). None = encode failure → that element keeps its original bytes.
          val encoded: Vector[Option[Array[Byte]]] =
            active.zipWithIndex.map { (span, i) => encodeFocus(foci(i), bytes, span).toOption }
          if plan.canonicalFraming then
            val reps = active
              .zip(encoded)
              .collect { case (span, Some(enc)) => (span, enc) }
            AvroBinaryCursor.spliceAll(bytes, reps)
          else
            // Byte-sized block framing: in-place splices would falsify the recorded block
            // sizes, so rebuild the whole array region with canonical framing instead.
            AvroBinaryCursor.reframeArray(bytes, plan, encoded)

  private def encodeFocus(
      a: A,
      bytes: Array[Byte],
      span: AvroBinaryCursor.BinarySpan,
  ): Either[Throwable, Array[Byte]] =
    focus match
      case f: AvroFocus.Fields[A] => AvroBinaryCursor.encodeFieldsOverlay(bytes, span, f, a)
      case _: AvroFocus.Leaf[A]   => AvroBinaryCursor.encodeValue(a, span, codec)

  // ---- Record-carried face -------------------------------------------

  /** The `IndexedRecord`-carried counterpart of this traversal — the Ior-bearing diagnostic surface
    * (`modify` / `getAll` / `place` / `transfer` + `*Unsafe`) over
    * `IndexedRecord | Array[Byte] | String` input:
    *
    * {{{
    *   codecPrism[Basket].items.each.name.record.modify(_.toUpperCase)(record)
    * }}}
    */
  def record: AvroRecordTraversal[A] = new AvroRecordTraversal[A](prefix, focus, rootSchemaCached)

  // ---- Path extension (used by field / at / selectDynamic macros) --

  private[avro] def widenSuffix[B](
      scalaName: String,
      declIdx: Int,
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](
      PathStep.Field(
        AvroWalk.fieldNameAt(suffixParentRecord, scalaName, declIdx, "AvroTraversal.field")
      )
    )

  /** Extend the per-element suffix by an EXPLICIT schema field name — the traversal counterpart of
    * [[AvroPrism.widenPathNamed]] (issue #35 escape hatch). Used by [[AvroTraversal.fieldNamed]].
    */
  private[avro] def widenSuffixNamed[B](
      schemaName: String
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Field(schemaName))

  /** The record schema the per-element suffix currently points at: walk the prefix to the array
    * field, descend to its element type, then walk the suffix. The parent for the next `.field` /
    * `.fields` resolution. Throws loudly (never silently) on a schema/path mismatch — issue #35.
    */
  private def suffixParentRecord: Schema =
    val arr = AvroWalk.schemaAt(rootSchemaCached, prefix) match
      case Right(s)  => s
      case Left(err) =>
        throw new IllegalArgumentException(s"AvroTraversal.field: cannot resolve the array — $err.")
    if arr.getType != Schema.Type.ARRAY then
      throw new IllegalArgumentException(
        s"AvroTraversal.field: prefix does not point at an array (found ${arr.getType})."
      )
    else
      AvroWalk.schemaAt(arr.getElementType, suffix) match
        case Right(s)  => s
        case Left(err) =>
          throw new IllegalArgumentException(
            s"AvroTraversal.field: cannot resolve the focus — $err."
          )

  private[avro] def widenSuffixIndex[B](
      i: Int
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Index(i))

  /** Extend the per-element suffix by a union-branch step. Used by `.union[Branch]`. */
  private[avro] def widenSuffixUnion[B](
      branchName: String
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.UnionBranch(branchName))

  private def widenSuffixStep[B](
      step: PathStep
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    val newSuffix = new Array[PathStep](suffix.length + 1)
    System.arraycopy(suffix, 0, newSuffix, 0, suffix.length)
    newSuffix(suffix.length) = step
    new AvroTraversal[B](prefix, new AvroFocus.Leaf[B](newSuffix, codecB), rootSchemaCached)

  /** Hand off the current (prefix, suffix) as a multi-field [[AvroTraversal]] whose focus is an
    * [[AvroFocus.Fields]] enumerating `fieldNames`.
    */
  private[avro] def toFieldsTraversal[B](
      scalaNames: Array[String],
      declIdxs: Array[Int],
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    val parent = suffixParentRecord
    val resolved = Array.tabulate(scalaNames.length)(i =>
      AvroWalk.fieldNameAt(parent, scalaNames(i), declIdxs(i), "AvroTraversal.fields")
    )
    new AvroTraversal[B](
      prefix,
      new AvroFocus.Fields[B](suffix, resolved, codecB),
      rootSchemaCached,
    )

end AvroTraversal

object AvroTraversal:

  /** `.field(_.x)` sugar — extend the suffix by a named field. */
  extension [A](t: AvroTraversal[A])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: AvroCodec[B]): AvroTraversal[B] =
      ${ AvroPrismMacro.fieldTraversalImpl[A, B]('t, 'selector, 'codecB) }

  /** `.fieldNamed[B]("schema_name")` — drill by EXPLICIT schema field name (issue #35 escape
    * hatch), the traversal counterpart of [[AvroPrism.fieldNamed]].
    */
  extension [A](t: AvroTraversal[A])

    def fieldNamed[B](schemaName: String)(using codecB: AvroCodec[B]): AvroTraversal[B] =
      t.widenSuffixNamed[B](schemaName)

  /** `.at(i)` sugar — extend the suffix by an array index. */
  extension [A](t: AvroTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ AvroPrismMacro.atTraversalImpl[A]('t, 'i) }

  /** `.union[Branch]` — narrow the per-element focus to one union alternative. */
  extension [A](t: AvroTraversal[A])

    transparent inline def union[Branch]: Any =
      ${ AvroPrismMacro.unionTraversalImpl[A, Branch]('t) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple per element. */
  extension [A](t: AvroTraversal[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ AvroPrismMacro.fieldsTraversalImpl[A]('t, 'selectors) }

end AvroTraversal
