package dev.constructive.eo.avro

import scala.language.dynamics

import dev.constructive.eo.data.{MultiFocus, PSVec}
import dev.constructive.eo.optics.Optic
import org.apache.avro.Schema

/** Multi-focus counterpart to [[AvroPrism]], carried on the BINARY WIRE FORM like the prism:
  *
  * {{{
  *   AvroTraversal[A] <: Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]]
  *   type X = (Array[Byte], Vector[BinarySpan])
  * }}}
  *
  * `to` resolves the prefix to an array-typed field, walks the array's block framing, and locates +
  * slice-decodes the focus inside every element ([[AvroBinaryCursor.locateElements]]); `from`
  * re-encodes each focus and splices all spans back in one pass. Avro's standard (positive-count)
  * array framing carries no byte lengths, so per-element splices can change size freely — the write
  * path needs no re-framing. Same carrier as [[dev.constructive.eo.jsoniter.JsoniterTraversal]]
  * (`MultiFocus[PSVec]`), so `.foldMap` / `.all` / `.modify` / `.headOption` / same-carrier
  * `.andThen` light up from the standard extension catalogue — but unlike the jsoniter one, this
  * traversal is WRITE-capable.
  *
  * Read semantics are "fold the focuses that exist": elements whose focus walk or slice decode
  * refuses are dropped from the focus set (and stay untouched on write). Two pass-through cases on
  * the write side, both silent (diagnostics live on [[record]]):
  *
  *   - whole-walk failure (bad prefix / non-array terminal / truncated bytes) → no foci, `from`
  *     returns the input;
  *   - a payload whose array uses the negative-count (byte-sized) block framing — splicing would
  *     leave the recorded block sizes stale, so writes pass through unchanged.
  *
  * The record-carried face — the Ior-bearing `modify` / `getAll` / `place` / `transfer` surface
  * over `IndexedRecord | Array[Byte] | String` input — lives behind ONE factory: [[record]]
  * ([[AvroRecordTraversal]]). Drilling (`.field` / `.at` / `.fields` / Dynamic selection) stays
  * here, the single mechanism; drill first, flip last.
  *
  * The Fields-vs-Leaf split lives entirely inside `focus` (per [[AvroFocus]]); the compatibility
  * alias [[AvroFieldsTraversal]] points back here.
  */
final class AvroTraversal[A] private[avro] (
    private[avro] val prefix: Array[PathStep],
    private[avro] val focus: AvroFocus[A],
    private[avro] val rootSchemaCached: Schema,
) extends Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]],
      Dynamic:

  type X = (Array[Byte], Vector[AvroBinaryCursor.BinarySpan])

  /** The per-element focus path (`path` for a Leaf focus, `parentPath` for a Fields focus). */
  private[avro] def suffix: Array[PathStep] = focus match
    case l: AvroFocus.Leaf[A]   => l.path
    case f: AvroFocus.Fields[A] => f.parentPath

  private[avro] def codec: AvroCodec[A] = focus.codec

  // ---- Dynamic field sugar -----------------------------------------

  transparent inline def selectDynamic(inline name: String): Any =
    ${ AvroPrismMacro.selectFieldTraversalImpl[A]('{ this }, 'name) }

  // ---- Abstract Optic members ---------------------------------------

  def to(bytes: Array[Byte]): MultiFocus[PSVec][X, A] = scan(bytes)
  def from(mf: MultiFocus[PSVec][X, A]): Array[Byte] = spliceFoci(mf)

  private def scan(bytes: Array[Byte]): MultiFocus[PSVec][X, A] =
    AvroBinaryCursor.locateElements(bytes, rootSchemaCached, prefix, suffix) match
      case Left(_)   => MultiFocus((bytes, Vector.empty), PSVec.empty[A])
      case Right(es) =>
        // Decode each focused span; a span whose decode refuses is dropped TOGETHER with its
        // focus so the span↔focus alignment `from` relies on survives.
        val kept = es
          .spans
          .flatMap(span =>
            AvroBinaryCursor.decodeSlice(bytes, span, codec).toOption.map(a => (span, a))
          )
        val arr = new Array[AnyRef](kept.length)
        var i = 0
        kept.foreach { (_, a) =>
          arr(i) = a.asInstanceOf[AnyRef]
          i += 1
        }
        // Non-writable framing (negative-count blocks): keep the foci readable but store no
        // spans, so `from` falls into the pass-through arm below.
        // ponytail: re-frame negative-count blocks on write if such payloads ever matter
        val spans = if es.writable then kept.map(_._1) else Vector.empty
        MultiFocus((bytes, spans), PSVec.unsafeWrap[A](arr))

  private def spliceFoci(mf: MultiFocus[PSVec][X, A]): Array[Byte] =
    val (bytes, spans) = mf.context
    val foci = mf.foci
    if spans.isEmpty || spans.length != foci.length then bytes
    else
      val reps = Vector.newBuilder[(AvroBinaryCursor.BinarySpan, Array[Byte])]
      var i = 0
      while i < spans.length do
        AvroBinaryCursor.encodeValue(foci(i), spans(i), codec) match
          case Right(enc) => reps += ((spans(i), enc))
          // silent per-element pass-through on encode failure — that element keeps its bytes
          case Left(_) => ()
        i += 1
      AvroBinaryCursor.spliceAll(bytes, reps.result())

  // ---- Record-carried face -------------------------------------------

  /** The [[org.apache.avro.generic.IndexedRecord]]-carried counterpart of this traversal — the
    * Ior-bearing diagnostic surface (`modify` / `getAll` / `place` / `transfer` + `*Unsafe`) over
    * `IndexedRecord | Array[Byte] | String` input:
    *
    * {{{
    *   codecPrism[Basket].items.each.name.record.modify(_.toUpperCase)(record)
    * }}}
    */
  def record: AvroRecordTraversal[A] = new AvroRecordTraversal[A](prefix, focus, rootSchemaCached)

  // ---- Path extension (used by field / at / selectDynamic macros) --

  private[avro] def widenSuffix[B](
      step: String
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Field(step))

  private[avro] def widenSuffixIndex[B](
      i: Int
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    widenSuffixStep[B](PathStep.Index(i))

  private def widenSuffixStep[B](
      step: PathStep
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    val newSuffix = new Array[PathStep](suffix.length + 1)
    System.arraycopy(suffix, 0, newSuffix, 0, suffix.length)
    newSuffix(suffix.length) = step
    new AvroTraversal[B](prefix, new AvroFocus.Leaf[B](newSuffix, codecB), rootSchemaCached)

  /** Hand off the current (prefix, suffix) as a multi-field [[AvroTraversal]] whose focus is an
    * [[AvroFocus.Fields]] enumerating `fieldNames`. Historically returned `AvroFieldsTraversal[B]`;
    * the alias is preserved.
    */
  private[avro] def toFieldsTraversal[B](
      fieldNames: Array[String]
  )(using codecB: AvroCodec[B]): AvroTraversal[B] =
    new AvroTraversal[B](
      prefix,
      new AvroFocus.Fields[B](suffix, fieldNames, codecB),
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

  /** `.at(i)` sugar — extend the suffix by an array index. */
  extension [A](t: AvroTraversal[A])

    transparent inline def at(i: Int): Any =
      ${ AvroPrismMacro.atTraversalImpl[A]('t, 'i) }

  /** `.fields(_.a, _.b, ...)` — focus a NamedTuple per element. */
  extension [A](t: AvroTraversal[A])

    transparent inline def fields(inline selectors: (A => Any)*): Any =
      ${ AvroPrismMacro.fieldsTraversalImpl[A]('t, 'selectors) }

end AvroTraversal
