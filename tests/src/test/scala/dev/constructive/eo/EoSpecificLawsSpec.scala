package dev.constructive.eo

import cats.instances.list.given
import dev.constructive.eo.accessor.*
import dev.constructive.eo.compose.*
import dev.constructive.eo.forgetful.*
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification

import optics.{Getter, Iso, Lens, Optic, Optional, Prism, Traversal}
import optics.Optic.*
import data.{Affine, Direct, ModifyF, MultiFocus, PSVec}
import laws.eo.{
  ComposeAssociativityLaws,
  IsoComposeLaws,
  LensComposeLaws,
  ModifyAConstLaws,
  ModifyAIdLaws,
  OptionalComposeLaws,
  PrismComposeLaws,
  PutIsReverseGetLaws,
  ReverseInvolutionLaws,
  TransformLaws
}
import laws.eo.discipline.{
  ComposeAssociativityTests,
  IsoComposeTests,
  LensComposeTests,
  ModifyAConstTests,
  ModifyAIdTests,
  OptionalComposeTests,
  PrismComposeTests,
  PutIsReverseGetTests,
  ReverseInvolutionTests,
  TransformTests
}
import laws.typeclass.{ComposerPathIndependenceLaws, ComposerPreservesGetLaws}
import laws.typeclass.discipline.{ComposerPathIndependenceTests, ComposerPreservesGetTests}

/** Binds the EO-specific law traits to concrete optic instances. Where a law is too tangled to
  * abstract over (H1, which depends on the existential X in a Lens's carrier), it's tested inline
  * as a property below.
  */
class EoSpecificLawsSpec extends Specification with CheckAllHelpers:

  // ----- Concrete optics used by several checkAlls ---------------

  val doubleIso: Optic[Int, Int, Int, Int, Direct] =
    Iso[Int, Int, Int, Int](_ * 2, _ / 2)

  val firstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
    Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

  val listTraversal: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
    Traversal.each[List, Int]

  // =============== A1/I1 — morph preserves modify ==================

  // covers: Morph from Tuple2 → ModifyF on a Lens
  checkAllMorphPreservesModifyFor[(Int, String), Int, Tuple2, ModifyF](
    "Lens.morph[ModifyF] preserves modify (I1)",
    firstLens,
    firstLens.morph[ModifyF],
  )

  // covers: Morph from Tuple2 → Affine on a Lens
  checkAllMorphPreservesModifyFor[(Int, String), Int, Tuple2, Affine](
    "Lens.morph[Affine] preserves modify",
    firstLens,
    firstLens.morph[Affine],
  )

  // covers: Morph from Direct → Tuple2 on an Iso (preserves-get arm)
  checkAllMorphPreservesGetFor[Int, Int, Direct, Tuple2](
    "Iso.morph[Tuple2] preserves get",
    doubleIso,
    doubleIso.morph[Tuple2],
  )

  // covers: Morph from MultiFocus[Function1[Int, *]] → ModifyF on a tuple-shaped MultiFocus
  // (the absorbed Grate). The `multifocus2modify` Composer widens any MultiFocus-carrier optic to
  // the Modify API. The MorphLaws.A1 check pins down that the lifted ModifyF's `.modify(f)(s)`
  // produces the same `T` as the original MultiFocus's `.modify(f)(s)` — the whole structural
  // soundness of the bridge in one law.
  val tuple2MultiFocusFnForMorph
      : Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
    MultiFocus.tuple[(Int, Int), Int]

  checkAllMorphPreservesModifyFor[(Int, Int), Int, MultiFocus[Function1[Int, *]], ModifyF](
    "MultiFocus.tuple[(Int,Int)].morph[ModifyF] preserves modify (I1)",
    tuple2MultiFocusFnForMorph,
    tuple2MultiFocusFnForMorph.morph[ModifyF],
  )

  // covers: Morph from MultiFocus[List] → ModifyF on a List-shaped MultiFocus.
  //
  // The `Composer[MultiFocus[F], ModifyF]` (`multifocus2modify`, MultiFocus.scala) widens any
  // MultiFocus-carrier optic to the Modify API. The MorphLaws.A1 check pins down that the lifted
  // ModifyF's `.modify(f)(s)` produces the same `T` as the original MultiFocus's `.modify(f)(s)`
  // via `mfFunctor` — the whole structural soundness of the bridge in one law. List is the
  // canonical multi-focus instance; the law covers the path `o.to(s) → mfFunctor.map(_, f) →
  // o.from(_)` end-to-end.
  val listMultiFocusForMorph: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
    MultiFocus.apply[List, Int]

  checkAllMorphPreservesModifyFor[List[Int], Int, MultiFocus[List], ModifyF](
    "MultiFocus.apply[List,Int].morph[ModifyF] preserves modify (I1)",
    listMultiFocusForMorph,
    listMultiFocusForMorph.morph[ModifyF],
  )

  // =============== B1 — Iso reverse involution =====================

  checkAll(
    "Iso reverse is involutive",
    new ReverseInvolutionTests[Int, Int]:

      val laws = new ReverseInvolutionLaws[Int, Int]:
        val iso = doubleIso
    .reverseInvolution,
  )

  // =============== C1/C2 — Lens ∘ Lens composition =================

  checkAll(
    "Lens ∘ Lens composes get / replace correctly",
    new LensComposeTests[((Int, Int), Int), (Int, Int), Int]:

      val laws = new LensComposeLaws[((Int, Int), Int), (Int, Int), Int]:

        val outer =
          Lens[((Int, Int), Int), (Int, Int)](
            _._1,
            (s, a) => (a, s._2),
          )

        val inner =
          Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))
    .lensCompose,
  )

  // =============== D1 — modifyA at Id ≡ modify =====================

  checkAll(
    "Traversal.each modifyA[Id] ≡ modify",
    new ModifyAIdTests[List[Int], Int, MultiFocus[PSVec]]:

      val laws = new ModifyAIdLaws[List[Int], Int, MultiFocus[PSVec]]:
        val optic = listTraversal
    .modifyAId,
  )

  // =============== D3 — modifyA at Const ≡ foldMap =================

  checkAll(
    "Traversal.each modifyA[Const[Int,*]].getConst ≡ foldMap",
    new ModifyAConstTests[List[Int], Int, MultiFocus[PSVec]]:

      val laws = new ModifyAConstLaws[List[Int], Int, MultiFocus[PSVec]]:
        val optic = listTraversal
    .modifyAConst,
  )

  // =============== E1 — foldMap Monoid homomorphism ================

  // covers: foldMap homomorphism on Traversal (MultiFocus[PSVec] carrier)
  checkAllFoldMapHomomorphismFor[List[Int], Int, MultiFocus[PSVec]](
    "Traversal.each.foldMap is a Monoid[Int] homomorphism",
    listTraversal,
  )

  // =============== H3 — put ≡ reverseGet ∘ pure ====================

  checkAll(
    "Iso.put ≡ reverseGet ∘ pure",
    new PutIsReverseGetTests[Int, Int]:

      val laws = new PutIsReverseGetLaws[Int, Int]:
        val iso = doubleIso
    .putIsReverseGet,
  )

  // =============== H1 + H2 + H4 — transform / place / transfer laws

  checkAll(
    "Lens.second: transform identity, transfer=curried place, place=transform const",
    new TransformTests[(Int, Boolean), Boolean, Int]:

      val laws = new TransformLaws[(Int, Boolean), Boolean, Int]:

        val optic: Optic[
          (Int, Boolean),
          (Int, Boolean),
          Boolean,
          Boolean,
          Tuple2,
        ] { type X = Int } = Lens.second[Int, Boolean]

        given transformEv: (((Int, Boolean)) => (Int, Boolean)) =
          identity
    .transform,
  )

  // =============== C1 — Composer path independence ================

  checkAll(
    "doubleIso: chain through Tuple2 ≡ chain through Either",
    new ComposerPathIndependenceTests[Int, Int]:

      val laws = new ComposerPathIndependenceLaws[Int, Int]:
        val iso = doubleIso
    .composerPathIndependence,
  )

  // =============== C2 — Composer preserves get ===================
  //
  // Direct → Tuple2 → Tuple2 with a locally-declared identity
  // composer at the second hop. Degenerate because Tuple2 is the
  // only non-Direct carrier with Accessor right now, but the
  // trait is ready to accept more witnesses.

  checkAll(
    "doubleIso: Direct → Tuple2 → Tuple2 chain preserves get",
    new ComposerPreservesGetTests[Int, Int, Direct, Tuple2, Tuple2]:

      val laws =
        new ComposerPreservesGetLaws[Int, Int, Direct, Tuple2, Tuple2]:
          val optic = doubleIso
          val fToG = Composer.direct2tuple

          val gToH = new Composer[Tuple2, Tuple2]:

            def to[S, T, A, B](
                o: Optic[S, T, A, B, Tuple2]
            ): Optic[S, T, A, B, Tuple2] = o

          given accessorF: accessor.Accessor[Direct] =
            Direct.accessor

          given accessorH: accessor.Accessor[Tuple2] =
            accessor.Accessor.tupleAccessor
    .composerPreservesGet,
  )

  // =============== C3 + C4 — Iso ∘ Iso =============================

  checkAll(
    "negate ∘ bitwise-not: Iso[Int,Int] ∘ Iso[Int,Int]",
    new IsoComposeTests[Int, Int, Int]:

      val laws = new IsoComposeLaws[Int, Int, Int]:

        val outer: Optic[Int, Int, Int, Int, Direct] =
          Iso[Int, Int, Int, Int](-_, -_)

        val inner: Optic[Int, Int, Int, Int, Direct] =
          Iso[Int, Int, Int, Int](~_, ~_)
    .isoCompose,
  )

  // =============== C5 + Prism ∘ Prism getOption / reverseGet =======
  //
  // Two Some-prisms unpack Option[Option[Int]] down to Int.

  val outerSomePrism
      : Optic[Option[Option[Int]], Option[Option[Int]], Option[Int], Option[Int], Either] =
    Prism[Option[Option[Int]], Option[Int]](
      opt => opt.toRight(opt),
      Some(_),
    )

  val innerSomePrism: Optic[Option[Int], Option[Int], Int, Int, Either] =
    Prism[Option[Int], Int](opt => opt.toRight(opt), Some(_))

  checkAll(
    "Some ∘ Some: Prism[Option[Option[Int]], Int] via composition",
    new PrismComposeTests[Option[Option[Int]], Option[Int], Int]:

      val laws = new PrismComposeLaws[Option[Option[Int]], Option[Int], Int]:
        val outer = outerSomePrism
        val inner = innerSomePrism
    .prismCompose,
  )

  // =============== Optional ∘ Optional =============================
  //
  // Two Option-field Optionals nest to give a Some ∘ Some style
  // Optional on (Int, Option[Int]).

  val outerAffineOptional
      : Optic[(Int, Option[Int]), (Int, Option[Int]), Option[Int], Option[Int], Affine] {
        type X <: Tuple
      } =
    Optional[(Int, Option[Int]), (Int, Option[Int]), Option[Int], Option[Int]](
      { case (_, opt) => Right(opt) },
      { case ((i, _), newOpt) => (i, newOpt) },
    )

  val innerAffineOptional: Optic[Option[Int], Option[Int], Int, Int, Affine] { type X <: Tuple } =
    Optional[Option[Int], Option[Int], Int, Int](
      opt => opt.toRight(opt),
      { case (_, a) => Some(a) },
    )

  checkAll(
    "Optional ∘ Optional: (Int, Option[Int]) → Int",
    new OptionalComposeTests[(Int, Option[Int]), Option[Int], Int]:

      val laws =
        new OptionalComposeLaws[(Int, Option[Int]), Option[Int], Int]:
          val outer = outerAffineOptional
          val inner = innerAffineOptional
    .optionalCompose,
  )

  // =============== Traversal.two: modify now works =================
  //
  // `Traversal.{two,three,four}` carry `MultiFocus[PSVec]` (arity tabulated at
  // construction), so the fixed-arity traversals inherit `.modify` and
  // `.replace` via the shared PSVec carrier instances. Behaviour smoke-test
  // pins the semantics down and lights up the previously-unreached `from`
  // clauses of those constructors. Cross-family composition lives in
  // `FixedArityTraversalSpec`.

  // covers: Traversal.two modifies both components and preserves structure,
  //   Traversal.three modifies all three components
  "Traversal.two / Traversal.three: pointwise modify across MultiFocus[PSVec]" >> {
    val t2 = Traversal.two[(Int, Int), (Int, Int), Int, Int](
      _._1,
      _._2,
      (a, b) => (a, b),
    )
    val t3 = Traversal.three[(Int, Int, Int), (Int, Int, Int), Int, Int](
      _._1,
      _._2,
      _._3,
      (a, b, c) => (a, b, c),
    )
    forAll { (p2: (Int, Int), p3: (Int, Int, Int)) =>
      val a2 = t2.modify(_ + 100)(p2) == (p2._1 + 100, p2._2 + 100)
      val a3 = t3.modify(_ + 1)(p3) == (p3._1 + 1, p3._2 + 1, p3._3 + 1)
      a2 && a3
    }
  }

  // =============== andThen associativity (behavioural) ============
  //
  // ((a ∘ b) ∘ c) and (a ∘ (b ∘ c)) modify (and, on a total-read carrier, get) identically. Both
  // bracketings are materialised here; the law trait can't call `.andThen` itself. Registered over a
  // three-deep Lens chain (Tuple2 carrier — has `Accessor`, so `get` associativity rides too).

  val assocA: Optic[
    (((Int, Int), Int), String),
    (((Int, Int), Int), String),
    ((Int, Int), Int),
    ((Int, Int), Int),
    Tuple2,
  ] =
    Lens[(((Int, Int), Int), String), ((Int, Int), Int)](_._1, (s, x) => (x, s._2))

  val assocB: Optic[((Int, Int), Int), ((Int, Int), Int), (Int, Int), (Int, Int), Tuple2] =
    Lens[((Int, Int), Int), (Int, Int)](_._1, (s, x) => (x, s._2))

  val assocC: Optic[(Int, Int), (Int, Int), Int, Int, Tuple2] =
    Lens[(Int, Int), Int](_._1, (s, x) => (x, s._2))

  checkAll(
    "Lens chain: andThen is associative (modify + get)",
    new ComposeAssociativityTests[(((Int, Int), Int), String), Int, Tuple2]:
      val laws = new ComposeAssociativityLaws[(((Int, Int), Int), String), Int, Tuple2]:
        val leftNested = assocA.andThen(assocB).andThen(assocC)
        val rightNested = assocA.andThen(assocB.andThen(assocC))
        val functor = summon[ForgetfulFunctor[Tuple2]]
    .associativeComposeWithGet,
  )

  // =============== Capability-keyed compose-coherence across families ============
  //
  // The four capability laws each state ONE composition equation (get / getOption / foldMap /
  // reverseGet distributes through `andThen`). Registered here at a representative spread of
  // cross-family cells — the cells the OpticsBehaviorSpec composites exercise only ad-hoc — so the
  // coherence equation is pinned as a discipline law rather than by hand.

  // ---- ComposedGet (total read): Lens ∘ Getter, Iso ∘ Getter ----

  val toStrGetter: Optic[Int, Unit, String, Unit, Direct] = Getter[Int, String](_.toString)

  val lensThenGetter: Getter[(Int, String), String] = firstLens.andThen(toStrGetter)

  // covers: Lens ∘ Getter get-coherence (firstLens reads Int, then toString) — the read-only
  //   collapse the "any optic .andThen(Getter)" OpticsBehaviorSpec block exercises ad-hoc
  checkAllComposedGetFor[(Int, String), Int, String]("Lens ∘ Getter — composed get")(
    getCap(firstLens),
    getCap(toStrGetter),
    getCap(lensThenGetter),
  )

  val isoThenGetter: Getter[Int, String] = doubleIso.andThen(toStrGetter)

  // covers: Iso ∘ Getter get-coherence
  checkAllComposedGetFor[Int, Int, String]("Iso ∘ Getter — composed get")(
    getCap(doubleIso),
    getCap(toStrGetter),
    getCap(isoThenGetter),
  )

  // ---- ComposedFoldMap: Prism ∘ Lens, Lens ∘ Prism ----

  val someTupleP: Optic[
    Option[(Int, String)],
    Option[(Int, String)],
    (Int, String),
    (Int, String),
    Either,
  ] =
    Prism[Option[(Int, String)], (Int, String)](opt => opt.toRight(opt), Some(_))

  val prismLensComposed: Optic[Option[(Int, String)], Option[(Int, String)], Int, Int, Affine] =
    someTupleP.andThen(firstLens)

  // covers: Prism ∘ Lens foldMap-coherence — the "Prism∘Lens" cross-family cell (triP∘triSideL in
  //   OpticsBehaviorSpec) as a monoidal-fold distribution law
  checkAllComposedFoldMapFor[Option[(Int, String)], (Int, String), Int](
    "Prism ∘ Lens — composed foldMap"
  )(
    foldCap(someTupleP),
    foldCap(firstLens),
    foldCap(prismLensComposed),
  )

  val optFieldLens
      : Optic[(Option[Int], String), (Option[Int], String), Option[Int], Option[Int], Tuple2] =
    Lens[(Option[Int], String), Option[Int]](_._1, (s, a) => (a, s._2))

  val someIntP: Optic[Option[Int], Option[Int], Int, Int, Either] =
    Prism[Option[Int], Int](opt => opt.toRight(opt), Some(_))

  val lensPrismComposed: Optic[(Option[Int], String), (Option[Int], String), Int, Int, Affine] =
    optFieldLens.andThen(someIntP)

  // covers: Lens ∘ Prism foldMap-coherence (wrapperShape∘triP shape in OpticsBehaviorSpec)
  checkAllComposedFoldMapFor[(Option[Int], String), Option[Int], Int](
    "Lens ∘ Prism — composed foldMap"
  )(
    foldCap(optFieldLens),
    foldCap(someIntP),
    foldCap(lensPrismComposed),
  )

  // ---- ComposedGetOption (partial read): Optional ∘ Prism, Prism ∘ Optional ----

  val headOptional
      : Optic[(Int, Option[Int]), (Int, Option[Int]), Option[Int], Option[Int], Affine] {
        type X <: Tuple
      } =
    Optional[(Int, Option[Int]), (Int, Option[Int]), Option[Int], Option[Int]](
      { case (_, opt) => Right(opt) },
      { case ((i, _), newOpt) => (i, newOpt) },
    )

  val optionalPrismComposed: Optic[(Int, Option[Int]), (Int, Option[Int]), Int, Int, Affine] =
    headOptional.andThen(someIntP)

  // covers: Optional ∘ Prism getOption-coherence (the Kleisli-over-Option law across carriers)
  checkAllComposedGetOptionFor[(Int, Option[Int]), Option[Int], Int](
    "Optional ∘ Prism — composed getOption"
  )(
    getOptionCap(headOptional),
    getOptionCap(someIntP),
    getOptionCap(optionalPrismComposed),
  )

  val someWrapP: Optic[
    Option[(Int, Option[Int])],
    Option[(Int, Option[Int])],
    (Int, Option[Int]),
    (Int, Option[Int]),
    Either,
  ] =
    Prism[Option[(Int, Option[Int])], (Int, Option[Int])](opt => opt.toRight(opt), Some(_))

  val prismOptionalComposed: Optic[Option[(Int, Option[Int])], Option[(Int, Option[Int])], Option[
    Int
  ], Option[Int], Affine] =
    someWrapP.andThen(headOptional)

  // covers: Prism ∘ Optional getOption-coherence
  checkAllComposedGetOptionFor[Option[(Int, Option[Int])], (Int, Option[Int]), Option[Int]](
    "Prism ∘ Optional — composed getOption"
  )(
    getOptionCap(someWrapP),
    getOptionCap(headOptional),
    getOptionCap(prismOptionalComposed),
  )

  // ---- ComposedReverseGet (build): Iso ∘ Prism, Prism ∘ Iso ----

  val evenIntP: Optic[Int, Int, Int, Int, Either] =
    Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)

  val isoPrismComposed: Optic[Int, Int, Int, Int, Either] = doubleIso.andThen(evenIntP)

  // covers: Iso ∘ Prism reverseGet-coherence (build distributes through the chain)
  checkAllComposedReverseGetFor[Int, Int, Int]("Iso ∘ Prism — composed reverseGet")(
    reverseGetCap(doubleIso),
    reverseGetCap(evenIntP),
    reverseGetCap(isoPrismComposed),
  )

  val prismIsoComposed: Optic[Int, Int, Int, Int, Either] = evenIntP.andThen(doubleIso)

  // covers: Prism ∘ Iso reverseGet-coherence
  checkAllComposedReverseGetFor[Int, Int, Int]("Prism ∘ Iso — composed reverseGet")(
    reverseGetCap(evenIntP),
    reverseGetCap(doubleIso),
    reverseGetCap(prismIsoComposed),
  )
