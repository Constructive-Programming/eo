package dev.constructive.eo

import cats.instances.list.given
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification

import optics.{Iso, Lens, Optic, Optional, Prism, Traversal}
import optics.Optic.*
import data.{Affine, Forgetful, MultiFocus, PSVec, SetterF}
import data.Forgetful.given
import data.Affine.given
import data.MultiFocus.given
import data.SetterF.given
import laws.eo.{
  IsoComposeLaws,
  LensComposeLaws,
  ModifyAConstLaws,
  ModifyAIdLaws,
  OptionalComposeLaws,
  PrismComposeLaws,
  PutIsReverseGetLaws,
  ReverseInvolutionLaws,
  TransformLaws,
}
import laws.eo.discipline.{
  IsoComposeTests,
  LensComposeTests,
  ModifyAConstTests,
  ModifyAIdTests,
  OptionalComposeTests,
  PrismComposeTests,
  PutIsReverseGetTests,
  ReverseInvolutionTests,
  TransformTests,
}
import laws.typeclass.{ComposerPathIndependenceLaws, ComposerPreservesGetLaws}
import laws.typeclass.discipline.{ComposerPathIndependenceTests, ComposerPreservesGetTests}

/** Binds the EO-specific law traits to concrete optic instances. Where a law is too tangled to
  * abstract over (H1, which depends on the existential X in a Lens's carrier), it's tested inline
  * as a property below.
  */
class EoSpecificLawsSpec extends Specification with CheckAllHelpers:

  // ----- Concrete optics used by several checkAlls ---------------

  val doubleIso: Optic[Int, Int, Int, Int, Forgetful] =
    Iso[Int, Int, Int, Int](_ * 2, _ / 2)

  val firstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
    Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

  val listTraversal: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
    Traversal.each[List, Int]

  // =============== A1/I1 — morph preserves modify ==================

  // covers: Morph from Tuple2 → SetterF on a Lens
  checkAllMorphPreservesModifyFor[(Int, String), Int, Tuple2, SetterF](
    "Lens.morph[SetterF] preserves modify (I1)",
    firstLens,
    firstLens.morph[SetterF],
  )

  // covers: Morph from Tuple2 → Affine on a Lens
  checkAllMorphPreservesModifyFor[(Int, String), Int, Tuple2, Affine](
    "Lens.morph[Affine] preserves modify",
    firstLens,
    firstLens.morph[Affine],
  )

  // covers: Morph from Forgetful → Tuple2 on an Iso (preserves-get arm)
  checkAllMorphPreservesGetFor[Int, Int, Forgetful, Tuple2](
    "Iso.morph[Tuple2] preserves get",
    doubleIso,
    doubleIso.morph[Tuple2],
  )

  // covers: Morph from MultiFocus[Function1[Int, *]] → SetterF on a tuple-shaped MultiFocus
  // (the absorbed Grate). The `multifocus2setter` Composer widens any MultiFocus-carrier optic to
  // the Setter API. The MorphLaws.A1 check pins down that the lifted SetterF's `.modify(f)(s)`
  // produces the same `T` as the original MultiFocus's `.modify(f)(s)` — the whole structural
  // soundness of the bridge in one law.
  val tuple2MultiFocusFnForMorph
      : Optic[(Int, Int), (Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
    MultiFocus.tuple[(Int, Int), Int]

  checkAllMorphPreservesModifyFor[(Int, Int), Int, MultiFocus[Function1[Int, *]], SetterF](
    "MultiFocus.tuple[(Int,Int)].morph[SetterF] preserves modify (I1)",
    tuple2MultiFocusFnForMorph,
    tuple2MultiFocusFnForMorph.morph[SetterF],
  )

  // covers: Morph from MultiFocus[List] → SetterF on a List-shaped MultiFocus.
  //
  // The `Composer[MultiFocus[F], SetterF]` (`multifocus2setter`, MultiFocus.scala) widens any
  // MultiFocus-carrier optic to the Setter API. The MorphLaws.A1 check pins down that the lifted
  // SetterF's `.modify(f)(s)` produces the same `T` as the original MultiFocus's `.modify(f)(s)`
  // via `mfFunctor` — the whole structural soundness of the bridge in one law. List is the
  // canonical multi-focus instance; the law covers the path `o.to(s) → mfFunctor.map(_, f) →
  // o.from(_)` end-to-end.
  val listMultiFocusForMorph: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
    MultiFocus.apply[List, Int]

  checkAllMorphPreservesModifyFor[List[Int], Int, MultiFocus[List], SetterF](
    "MultiFocus.apply[List,Int].morph[SetterF] preserves modify (I1)",
    listMultiFocusForMorph,
    listMultiFocusForMorph.morph[SetterF],
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
  // Forgetful → Tuple2 → Tuple2 with a locally-declared identity
  // composer at the second hop. Degenerate because Tuple2 is the
  // only non-Forgetful carrier with Accessor right now, but the
  // trait is ready to accept more witnesses.

  checkAll(
    "doubleIso: Forgetful → Tuple2 → Tuple2 chain preserves get",
    new ComposerPreservesGetTests[Int, Int, Forgetful, Tuple2, Tuple2]:

      val laws =
        new ComposerPreservesGetLaws[Int, Int, Forgetful, Tuple2, Tuple2]:
          val optic = doubleIso
          val fToG = Composer.forgetful2tuple

          val gToH = new Composer[Tuple2, Tuple2]:

            def to[S, T, A, B](
                o: Optic[S, T, A, B, Tuple2]
            ): Optic[S, T, A, B, Tuple2] = o

          given accessorF: data.Accessor[Forgetful] =
            Forgetful.accessor

          given accessorH: data.Accessor[Tuple2] =
            data.Accessor.tupleAccessor
    .composerPreservesGet,
  )

  // =============== C3 + C4 — Iso ∘ Iso =============================

  checkAll(
    "negate ∘ bitwise-not: Iso[Int,Int] ∘ Iso[Int,Int]",
    new IsoComposeTests[Int, Int, Int]:

      val laws = new IsoComposeLaws[Int, Int, Int]:

        val outer: Optic[Int, Int, Int, Int, Forgetful] =
          Iso[Int, Int, Int, Int](-_, -_)

        val inner: Optic[Int, Int, Int, Int, Forgetful] =
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
    Optional[(Int, Option[Int]), (Int, Option[Int]), Option[Int], Option[Int], Tuple2](
      { case (_, opt) => Right(opt) },
      { case ((i, _), newOpt) => (i, newOpt) },
    )

  val innerAffineOptional: Optic[Option[Int], Option[Int], Int, Int, Affine] { type X <: Tuple } =
    Optional[Option[Int], Option[Int], Int, Int, Tuple2](
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
  // With `Traversal.{two,three,four}` carrying `MultiFocus[Function1[Int, *]]`
  // (post-FixedTraversal-fold), the fixed-arity traversals inherit `.modify`
  // and `.replace` via the shared `mfFunctor[Function1[Int, *]]` instance.
  // Behaviour smoke-test pins the semantics down and lights up the
  // previously-unreached `from` clauses of those constructors.

  "Traversal.two modifies both components and preserves structure" >> {
    val t = Traversal.two[(Int, Int), (Int, Int), Int, Int](
      _._1,
      _._2,
      (a, b) => (a, b),
    )
    forAll((p: (Int, Int)) => t.modify(_ + 100)(p) == (p._1 + 100, p._2 + 100))
  }

  "Traversal.three modifies all three components" >> {
    val t = Traversal.three[(Int, Int, Int), (Int, Int, Int), Int, Int](
      _._1,
      _._2,
      _._3,
      (a, b, c) => (a, b, c),
    )
    forAll((p: (Int, Int, Int)) => t.modify(_ + 1)(p) == (p._1 + 1, p._2 + 1, p._3 + 1))
  }
