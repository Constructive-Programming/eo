package eo

import optics.{Iso, Lens, Optic, Optional, Prism, Traversal}
import optics.Optic.*
import data.{Affine, FixedTraversal, Forget, Forgetful, SetterF}
import data.Forgetful.given
import data.Affine.given
import data.SetterF.given
import data.FixedTraversal.given
import laws.eo.{
  ChainAccessorLaws,
  ChainPathIndependenceLaws,
  FoldMapHomomorphismLaws,
  ForgetAllModifyLaws,
  IsoComposeLaws,
  LensComposeLaws,
  ModifyAConstLaws,
  ModifyAIdLaws,
  MorphLaws,
  OptionalComposeLaws,
  PrismComposeLaws,
  PutIsReverseGetLaws,
  ReverseInvolutionLaws,
  TransformLaws,
  TraverseAllLaws,
}
import laws.eo.discipline.{
  ChainAccessorTests,
  ChainPathIndependenceTests,
  FoldMapHomomorphismTests,
  ForgetAllModifyTests,
  IsoComposeTests,
  LensComposeTests,
  ModifyAConstTests,
  ModifyAIdTests,
  MorphTests,
  OptionalComposeTests,
  PrismComposeTests,
  PutIsReverseGetTests,
  ReverseInvolutionTests,
  TransformTests,
  TraverseAllTests,
}

import cats.instances.int.given
import cats.instances.list.given

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Binds the EO-specific law traits to concrete optic instances. Where a law is too tangled to
  * abstract over (H1, which depends on the existential X in a Lens's carrier), it's tested inline
  * as a property below.
  */
class EoSpecificLawsSpec extends Specification with Discipline:

  // ----- Concrete optics used by several checkAlls ---------------

  val doubleIso: Optic[Int, Int, Int, Int, Forgetful] =
    Iso[Int, Int, Int, Int](_ * 2, _ / 2)

  val firstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
    Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

  val listTraversal: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
    Traversal.each[List, Int, Int]

  // =============== A1/I1 — morph preserves modify ==================

  checkAll(
    "Lens.morph[SetterF] preserves modify (I1)",
    new MorphTests[(Int, String), Int, Tuple2, SetterF]:

      val laws = new MorphLaws[(Int, String), Int, Tuple2, SetterF]:
        val optic = firstLens
        val morphed = firstLens.morph[SetterF]
    .morphPreservesModify,
  )

  checkAll(
    "Lens.morph[Affine] preserves modify",
    new MorphTests[(Int, String), Int, Tuple2, Affine]:

      val laws = new MorphLaws[(Int, String), Int, Tuple2, Affine]:
        val optic = firstLens
        val morphed = firstLens.morph[Affine]
    .morphPreservesModify,
  )

  checkAll(
    "Iso.morph[Tuple2] preserves get",
    new MorphTests[Int, Int, Forgetful, Tuple2]:

      val laws = new MorphLaws[Int, Int, Forgetful, Tuple2]:
        val optic = doubleIso
        val morphed = doubleIso.morph[Tuple2]
    .morphPreservesGet,
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
    new ModifyAIdTests[List[Int], Int, Forget[List]]:

      val laws = new ModifyAIdLaws[List[Int], Int, Forget[List]]:
        val optic = listTraversal
    .modifyAId,
  )

  // =============== D3 — modifyA at Const ≡ foldMap =================

  checkAll(
    "Traversal.each modifyA[Const[Int,*]].getConst ≡ foldMap",
    new ModifyAConstTests[List[Int], Int, Forget[List]]:

      val laws = new ModifyAConstLaws[List[Int], Int, Forget[List]]:
        val optic = listTraversal
    .modifyAConst,
  )

  // =============== E1 — foldMap Monoid homomorphism ================

  checkAll(
    "Traversal.each.foldMap is a Monoid[Int] homomorphism",
    new FoldMapHomomorphismTests[List[Int], Int, Forget[List]]:

      val laws =
        new FoldMapHomomorphismLaws[List[Int], Int, Forget[List]]:
          val optic = listTraversal
    .foldMapHomomorphism,
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

  // =============== F1 — Composer.chain path independence ===========

  checkAll(
    "doubleIso: chain through Tuple2 ≡ chain through Either",
    new ChainPathIndependenceTests[Int, Int]:

      val laws = new ChainPathIndependenceLaws[Int, Int]:
        val iso = doubleIso
    .chainPathIndependence,
  )

  // =============== F2 — Composer.chain preserves get ===============
  //
  // Forgetful → Tuple2 → Tuple2 with a locally-declared identity
  // composer at the second hop. Degenerate because Tuple2 is the
  // only non-Forgetful carrier with Accessor right now, but the
  // trait is ready to accept more witnesses.

  checkAll(
    "doubleIso: Forgetful → Tuple2 → Tuple2 chain preserves get",
    new ChainAccessorTests[Int, Int, Forgetful, Tuple2, Tuple2]:

      val laws =
        new ChainAccessorLaws[Int, Int, Forgetful, Tuple2, Tuple2]:
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
    .chainAccessor,
  )

  // =============== G1 + G2 — Optic.all on Forget[T] ================

  checkAll(
    "Traversal.each[List, Int]: all has length 1 and head == input",
    new TraverseAllTests[List, Int]:

      val laws = new TraverseAllLaws[List, Int]:
        val traversal = listTraversal
    .traverseAll,
  )

  // =============== G3 — all-then-map ≡ modify on Forget[T] =========

  checkAll(
    "Traversal.each[List, Int]: T.map(all(s).head)(f) ≡ modify(f)(s)",
    new ForgetAllModifyTests[List, Int]:

      val laws = new ForgetAllModifyLaws[List, Int]:
        val traversal = listTraversal
    .allMap,
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
  // With `ForgetfulFunctor[FixedTraversal[2]]` in core, the two/three/
  // four-arity traversals can finally `modify` and `replace`. Behavior
  // smoke-test nails down the semantics and lights up the previously-
  // unreached `from` clauses of those constructors.

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
