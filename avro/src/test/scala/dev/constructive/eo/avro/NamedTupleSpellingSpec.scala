package dev.constructive.eo.avro

import scala.compiletime.testing.typeCheckErrors

import dev.constructive.eo.generics.lens
import dev.constructive.eo.optics.SimpleLens
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification

/** Regression spec for issue #96 — the '''spelling''' of the `NamedTuple` types the eo-generics
  * macros synthesise.
  *
  * `MacroSelectors.fieldsSelectorNT` (behind `AvroPrism.fields` / `AvroTraversal.fields` /
  * `JsonPrism.fields`) and `LensMacro.namedTupleTypeOf` (behind `lens[S](_.a, _.b)`'s focus and
  * every macro-lens complement) both build `NamedTuple[Names, Values]`. Two spellings of `Values`
  * are `=:=` to the compiler —
  *
  *   - `String *: Int *: EmptyTuple` (a `*:` cons chain), and
  *   - `(String, Int)` (i.e. `scala.Tuple2[String, Int]`)
  *
  * — but only the second is constructible by hearth's `SyntheticNamedTupleConstructor` below arity
  * 23, where it emits `new <Values-primary-constructor>(args…)`. For the cons spelling that is
  * `new *:(a, b)`, and `*:` declares no value parameters, so any third-party derivation that
  * '''builds''' the NamedTuple (kindlings' `AvroDecoderHandleAsNamedTupleRule`) blows up with
  * `wrong number of arguments at inlining … expected: 0, found: N` plus a secondary
  * `a reference to method decode_…$macro$N was used outside the scope where it was defined`. That
  * `n < 23` branch is unchanged in hearth 0.4.2 — only arity '''>= 23''' gained a `Tuple.fromArray`
  * path — so the TupleN spelling stays mandatory at 2..22, permanently.
  *
  * Every other in-repo `.fields` spec hand-declares its own `AvroCodec[NamedTuple[…]]` given
  * (spelled `TupleN`, because that is what a human writes), which short-circuits kindlings' NT rule
  * — which is exactly why CI never saw this. '''This file deliberately declares no NamedTuple given
  * at all''': every `.fields` / `lens` row below drives the auto-derivation path a user with no
  * hand-written given gets.
  *
  * It also carries two '''differently-shaped''' `.fields` expansions in one file (arity 2 and arity
  * 3), which refutes the earlier "a second differently-shaped `.fields` expansion in one file trips
  * a macro hoisting bug" hypothesis: given availability is the sole discriminator.
  */
class NamedTupleSpellingSpec extends Specification:

  import NamedTupleSpellingSpec.*

  // ---- .fields with NO NamedTuple given in scope (issue #96) -------------

  // covers: `codecPrism[A].fields(_.a, _.b)` compiles with no user-written AvroCodec[NamedTuple]
  //   in scope (the macro's synthesised NT must be TupleN-spelled for kindlings' NT decoder rule
  //   to be able to CONSTRUCT it),
  //   a two-sibling replace writes BOTH selected slots,
  //   the un-selected sibling comes through reference-identical (positional overlay, no
  //   re-encode of the whole parent)
  "`.fields` with no hand-written NamedTuple codec: compiles, writes both slots, leaves the sibling identical" >> {
    val record = skuRecord(Sku("AB-1", "widget", "shelf 4"))
    val L = codecPrism[Sku].fields(_.code, _.label).record

    val out = L.placeUnsafe((code = "CD-2", label = "gadget"))(record)

    val codePos = skuSchema.getField("code").pos
    val labelPos = skuSchema.getField("label").pos
    val notePos = skuSchema.getField("note").pos

    (out.get(codePos).toString === "CD-2")
      .and(out.get(labelPos).toString === "gadget")
      .and(out.get(notePos).toString === "shelf 4")
      .and((out.get(notePos) eq record.get(notePos)) === true)
  }

  // covers: a SECOND, differently-shaped `.fields` expansion in the same file (arity 3, full
  //   cover) also compiles with no given in scope — the old "one shape per file" hypothesis was
  //   about given availability, not about expansion hoisting
  "`.fields` at arity 3, second differently-shaped expansion in the same file: still given-free" >> {
    val record = skuRecord(Sku("AB-1", "widget", "shelf 4"))
    val L = codecPrism[Sku].fields(_.code, _.label, _.note).record

    val out = L.placeUnsafe((code = "CD-2", label = "gadget", note = "shelf 9"))(record)

    (out.get(skuSchema.getField("code").pos).toString === "CD-2")
      .and(out.get(skuSchema.getField("label").pos).toString === "gadget")
      .and(out.get(skuSchema.getField("note").pos).toString === "shelf 9")
  }

  // covers: the read side of the given-free NT — `get` decodes through kindlings' NT decoder,
  //   which is the rule that could not construct the cons-spelled tuple
  "`.fields` with no hand-written NamedTuple codec: the read side decodes the NamedTuple" >> {
    val record = skuRecord(Sku("AB-1", "widget", "shelf 4"))
    val L = codecPrism[Sku].fields(_.code, _.label).record

    L.getOptionUnsafe(record) match
      case Some(nt) => (nt.code === "AB-1").and(nt.label === "widget")
      case None     =>
        org.specs2.execute.Failure("expected Some(namedTuple)"): org.specs2.execute.Result
  }

  // covers: the `TupleN` spelling the macros now emit is the SAME TYPE as the `*:` cons spelling
  //   they used to emit — so an already-published user given
  //   (`given AvroCodec[NamedTuple[("a","b"), (A, B)]] = AvroCodec.derived`) keeps matching the
  //   macro's synthesised focus, in both directions. The fix is a spelling change, not a type
  //   change; nothing downstream has to be rewritten.
  "TupleN and `*:` NamedTuple spellings are the same type (user givens keep matching)" >> {
    type Cons =
      NamedTuple.NamedTuple["code" *: "label" *: EmptyTuple, String *: String *: EmptyTuple]
    type Flat = NamedTuple.NamedTuple[("code", "label"), (String, String)]

    val flat: Flat = (code = "AB-1", label = "widget")
    val cons: Cons = flat // same type — no conversion, no ascription gymnastics
    val back: Flat = cons

    (back.code === "AB-1").and(back.label === "widget")
  }

  // ---- the eo-generics lens complement (LensMacro.namedTupleTypeOf) ------

  // covers: `lens[S](_.field)`'s NamedTuple COMPLEMENT is spelled so a third-party derivation can
  //   construct it — the same hazard as `.fields`, in the other macro. The complement is really
  //   built here (summoned codec, encode, decode), not merely spelled.
  "macro-lens complement NamedTuple: a third-party derivation can construct it" >> {
    val sku = Sku("AB-1", "widget", "shelf 4")
    val l = lens[Sku](_.code)
    val complement = l.split(sku)._1

    complementRoundTrip(l, sku) match
      case Right(back) => (back === complement).and(complement.label === "widget")
      case Left(t)     =>
        org.specs2.execute.Failure(s"complement round-trip failed: $t"): org.specs2.execute.Result
  }

  // covers: the multi-selector partial-cover lens builds BOTH halves through namedTupleTypeOf —
  //   focus (selector order) and complement (declaration order); the complement is the one a
  //   derivation has to construct
  "macro-lens multi-selector complement: constructible too" >> {
    val sku = Sku("AB-1", "widget", "shelf 4")
    val l = lens[Sku](_.code, _.label)

    complementRoundTrip(l, sku) match
      case Right(back) => back.note === "shelf 4"
      case Left(t)     =>
        org.specs2.execute.Failure(s"complement round-trip failed: $t"): org.specs2.execute.Result
  }

  // ---- above the old arity ceiling (was a pinned failure) ---------------

  // covers: at 23+ selectors there IS no `TupleN` spelling — `(A, …, A)` with 23 components IS
  //   the `*:` cons chain — so the TupleN spelling above cannot reach that far. This used to be
  //   a pinned FAILURE (hearth 0.4.0 emitted `new *:(args…)` and `*:` takes no value params).
  //   hearth 0.4.2's `SyntheticNamedTupleConstructor` routes arity >= 23 through
  //   `Tuple.fromArray` instead, so the row is now the positive assertion its own note asked for.
  //   Still given-free, like every other row here — this is the auto-derivation path.
  //   The wide end of the surface (mixed field types, un-selected siblings, reference identity)
  //   is covered separately by `WideFieldsArityCeilingSpec`.
  "`.fields` at arity 23: compiles and round-trips on hearth 0.4.2 (ceiling broken)" >> {
    val record = wide23Record
    val L = codecPrism[Wide23]
      .fields(
        _.f01,
        _.f02,
        _.f03,
        _.f04,
        _.f05,
        _.f06,
        _.f07,
        _.f08,
        _.f09,
        _.f10,
        _.f11,
        _.f12,
        _.f13,
        _.f14,
        _.f15,
        _.f16,
        _.f17,
        _.f18,
        _.f19,
        _.f20,
        _.f21,
        _.f22,
        _.f23,
      )
      .record

    L.getOptionUnsafe(record) match
      case Some(nt) =>
        // `Tuple.fromArray` builds a TupleXXL, NOT a TupleN — the branch that fixed this.
        (nt.asInstanceOf[AnyRef].getClass.getName === "scala.runtime.TupleXXL")
          .and(nt.f01 === "a01")
          .and(nt.f23 === "a23")
      case None =>
        org.specs2.execute.Failure("expected Some(namedTuple)"): org.specs2.execute.Result
  }

  // covers: hearth 0.4.2's fix is scoped to arity >= 23 ONLY — its `n < 23` branch still emits
  //   `new <Values-primary-constructor>(args…)`, which for a cons chain is the value-parameterless
  //   `new *:(a, b)`. So `MacroSelectors.tupleTypeOf`'s "TupleN at <= 22, cons fold above" split is
  //   a PERMANENT requirement of the hearth contract, not a workaround the bump makes redundant:
  //   collapsing it to a uniform cons fold would re-break every `.fields` call of arity 2..22.
  //   This row is the executable proof, so a future simplification pass fails loudly.
  "a cons-spelled NamedTuple below arity 23 is still unconstructible (the TupleN branch is permanent)" >> {
    val errs = typeCheckErrors("""
        import hearth.kindlings.avroderivation.AvroDecoder
        type ConsNT =
          NamedTuple.NamedTuple["a" *: "b" *: EmptyTuple, String *: String *: EmptyTuple]
        val d: AvroDecoder[ConsNT] = AvroDecoder.derived
      """)

    (errs.nonEmpty === true)
      .and(errs.exists(_.message.contains("*:")) === true)
  }

  // ---- helpers ----------------------------------------------------------

  /** Summon an `AvroCodec` for a macro-lens complement and round-trip the complement value through
    * it. Generic in `C` on purpose: `C` is only ever instantiated by the macro's own synthesised
    * `NamedTuple` `TypeRepr`, so the implicit search — and hence kindlings' NamedTuple derivation —
    * runs against exactly the spelling `LensMacro` emitted.
    */
  private def complementRoundTrip[S, A, C](l: SimpleLens[S, A, C], s: S)(using
      c: AvroCodec[C]
  ): Either[Throwable, C] =
    c.decodeEither(c.encode(l.split(s)._1))

end NamedTupleSpellingSpec

object NamedTupleSpellingSpec:

  /** Three String fields: two to select, one to watch survive reference-identical. */
  case class Sku(code: String, label: String, note: String)

  object Sku:

    given AvroEncoder[Sku] = AvroEncoder.derived
    given AvroDecoder[Sku] = AvroDecoder.derived
    given AvroSchemaFor[Sku] = AvroSchemaFor.derived

  lazy val skuSchema: org.apache.avro.Schema = summon[AvroCodec[Sku]].schema

  def skuRecord(s: Sku): GenericRecord =
    summon[AvroCodec[Sku]].encode(s).asInstanceOf[GenericRecord]

  /** 23 fields — one past the last arity that has a `TupleN` spelling, so `.fields` over all of
    * them is spelled as a `*:` cons chain and built by hearth's `Tuple.fromArray` branch.
    */
  case class Wide23(
      f01: String,
      f02: String,
      f03: String,
      f04: String,
      f05: String,
      f06: String,
      f07: String,
      f08: String,
      f09: String,
      f10: String,
      f11: String,
      f12: String,
      f13: String,
      f14: String,
      f15: String,
      f16: String,
      f17: String,
      f18: String,
      f19: String,
      f20: String,
      f21: String,
      f22: String,
      f23: String,
  )

  object Wide23:

    given AvroEncoder[Wide23] = AvroEncoder.derived
    given AvroDecoder[Wide23] = AvroDecoder.derived
    given AvroSchemaFor[Wide23] = AvroSchemaFor.derived

  lazy val wide23Record: GenericRecord =
    summon[AvroCodec[Wide23]]
      .encode(
        Wide23(
          "a01",
          "a02",
          "a03",
          "a04",
          "a05",
          "a06",
          "a07",
          "a08",
          "a09",
          "a10",
          "a11",
          "a12",
          "a13",
          "a14",
          "a15",
          "a16",
          "a17",
          "a18",
          "a19",
          "a20",
          "a21",
          "a22",
          "a23",
        )
      )
      .asInstanceOf[GenericRecord]

  // Deliberately NO `given AvroCodec[NamedTuple[…]]` anywhere in this file — see the class
  // scaladoc. Adding one would silently restore the pre-fix pass.

end NamedTupleSpellingSpec
