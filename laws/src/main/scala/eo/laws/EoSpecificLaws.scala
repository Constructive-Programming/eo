package eo
package laws

import optics.Optic
import optics.Optic.*
import data.{Affine, Forgetful, Forget, SetterF}
import data.Forgetful.given
import data.Affine.given
import data.SetterF.given

import cats.{Applicative, Eval, Id, Monoid, Traverse, Functor}
import cats.data.Const

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Laws that have no direct Monocle analogue because they pin down EO's
  * own machinery: carrier coercion (`morph`), Iso's `reverse`, Lens ∘
  * Lens via `Optic.andThen`, the `modifyA` / `modifyF` / `foldMap` /
  * `put` / `place` extension-method family, and the `Composer.chain`
  * derivation.
  *
  * Organised by carrier affinity so each trait's type constraints stay
  * local.
  */
object EoSpecificLaws:

  // ================= A1–A2: morph preserves optic semantics ========

  trait MorphLaws[S, A, F[_, _], G[_, _]]:
    def optic: Optic[S, S, A, A, F]
    def morphed: Optic[S, S, A, A, G]

    /** A1 — morph preserves modify. */
    def morphPreservesModify(s: S, f: A => A)(using
        ForgetfulFunctor[F], ForgetfulFunctor[G]
    ): Boolean =
      morphed.modify(f)(s) == optic.modify(f)(s)

    /** A2 — morph preserves get (when both carriers have Accessor). */
    def morphPreservesGet(s: S)(using
        eo.data.Accessor[F], eo.data.Accessor[G]
    ): Boolean =
      morphed.get(s) == optic.get(s)

  abstract class MorphTests[S, A, F[_, _], G[_, _]] extends Laws:
    def laws: MorphLaws[S, A, F, G]

    def morphPreservesModify(using
        Arbitrary[S], Arbitrary[A], Cogen[A],
        ForgetfulFunctor[F], ForgetfulFunctor[G]
    ): RuleSet =
      new SimpleRuleSet(
        "Morph preserves modify",
        "modify equivalence" ->
          forAll((s: S, f: A => A) => laws.morphPreservesModify(s, f)),
      )

    def morphPreservesGet(using
        Arbitrary[S], Arbitrary[A],
        eo.data.Accessor[F], eo.data.Accessor[G]
    ): RuleSet =
      new SimpleRuleSet(
        "Morph preserves get",
        "get equivalence" ->
          forAll((s: S) => laws.morphPreservesGet(s)),
      )

  // ================= B1: Iso reverse involution ====================

  trait ReverseInvolutionLaws[S, A]:
    def iso: Optic[S, S, A, A, Forgetful]

    def reverseInvolutionGet(s: S): Boolean =
      iso.reverse.reverse.get(s) == iso.get(s)

    def reverseInvolutionReverseGet(a: A): Boolean =
      iso.reverse.reverse.reverseGet(a) == iso.reverseGet(a)

  abstract class ReverseInvolutionTests[S, A] extends Laws:
    def laws: ReverseInvolutionLaws[S, A]

    def reverseInvolution(using
        Arbitrary[S], Arbitrary[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Iso reverse is involutive",
        "reverse.reverse.get == get" ->
          forAll((s: S) => laws.reverseInvolutionGet(s)),
        "reverse.reverse.reverseGet == reverseGet" ->
          forAll((a: A) => laws.reverseInvolutionReverseGet(a)),
      )

  // ================= C1–C2: Lens ∘ Lens composition ================

  trait LensComposeLaws[S, A, B]:
    def outer: Optic[S, S, A, A, Tuple2]
    def inner: Optic[A, A, B, B, Tuple2]

    // outer.andThen(inner) is Optic[S, S, B, B, Tuple2]

    def composedGet(s: S): Boolean =
      outer.andThen(inner).get(s) == inner.get(outer.get(s))

    def composedReplace(s: S, b: B): Boolean =
      outer.andThen(inner).replace(b)(s) ==
        outer.replace(inner.replace(b)(outer.get(s)))(s)

  abstract class LensComposeTests[S, A, B] extends Laws:
    def laws: LensComposeLaws[S, A, B]

    def lensCompose(using
        Arbitrary[S], Arbitrary[B]
    ): RuleSet =
      new SimpleRuleSet(
        "Lens ∘ Lens",
        "composed get" ->
          forAll((s: S) => laws.composedGet(s)),
        "composed replace" ->
          forAll((s: S, b: B) => laws.composedReplace(s, b)),
      )

  // ================= D1: modifyA at Identity ≡ modify ==============

  trait ModifyAIdLaws[S, A, F[_, _]]:
    def optic: Optic[S, S, A, A, F]

    def modifyAIdIsModify(s: S, f: A => A)(using
        ForgetfulFunctor[F],
        ForgetfulTraverse[F, Applicative],
    ): Boolean =
      val viaModifyA: Id[S] = optic.modifyA[Id](a => (f(a): Id[A]))(s)
      val viaModify: S      = optic.modify(f)(s)
      viaModifyA == viaModify

  abstract class ModifyAIdTests[S, A, F[_, _]] extends Laws:
    def laws: ModifyAIdLaws[S, A, F]

    def modifyAId(using
        Arbitrary[S], Arbitrary[A], Cogen[A],
        ForgetfulFunctor[F],
        ForgetfulTraverse[F, Applicative],
    ): RuleSet =
      new SimpleRuleSet(
        "modifyA at Id ≡ modify",
        "modifyA[Id] == modify" ->
          forAll((s: S, f: A => A) => laws.modifyAIdIsModify(s, f)),
      )

  // ================= D3: modifyA at Const[M,*] ≡ foldMap ===========

  trait ModifyAConstLaws[S, A, F[_, _]]:
    def optic: Optic[S, S, A, A, F]

    def modifyAConstIsFoldMap(s: S, f: A => Int)(using
        ForgetfulFold[F],
        ForgetfulTraverse[F, Applicative],
    ): Boolean =
      // Const[Int, _] has Applicative iff Monoid[Int] exists (additive)
      type ConstInt[X] = Const[Int, X]
      val viaModifyA: Int =
        optic.modifyA[ConstInt](a => Const(f(a)))(s).getConst
      val viaFoldMap: Int = optic.foldMap(f)(s)
      viaModifyA == viaFoldMap

  abstract class ModifyAConstTests[S, A, F[_, _]] extends Laws:
    def laws: ModifyAConstLaws[S, A, F]

    def modifyAConst(using
        Arbitrary[S], Arbitrary[A], Cogen[A],
        ForgetfulFold[F],
        ForgetfulTraverse[F, Applicative],
    ): RuleSet =
      new SimpleRuleSet(
        "modifyA at Const[M,*] ≡ foldMap",
        "modifyA[Const[Int,*]].getConst == foldMap" ->
          forAll((s: S, f: A => Int) => laws.modifyAConstIsFoldMap(s, f)),
      )

  // ================= E1: foldMap is a Monoid homomorphism ==========

  trait FoldMapHomomorphismLaws[S, A, F[_, _]]:
    def optic: Optic[S, S, A, A, F]

    def foldMapHomomorphism(s: S, f: A => Int, g: A => Int)(using
        ForgetfulFold[F]
    ): Boolean =
      // Uses additive Monoid[Int]
      optic.foldMap(a => f(a) + g(a))(s) ==
        optic.foldMap(f)(s) + optic.foldMap(g)(s)

    def foldMapEmpty(s: S)(using ForgetfulFold[F]): Boolean =
      optic.foldMap[Int](_ => 0)(s) == 0

  abstract class FoldMapHomomorphismTests[S, A, F[_, _]] extends Laws:
    def laws: FoldMapHomomorphismLaws[S, A, F]

    def foldMapHomomorphism(using
        Arbitrary[S], Arbitrary[A], Cogen[A],
        ForgetfulFold[F],
    ): RuleSet =
      new SimpleRuleSet(
        "foldMap is a Monoid homomorphism",
        "foldMap(f ⊕ g) == foldMap(f) ⊕ foldMap(g)" ->
          forAll((s: S, f: A => Int, g: A => Int) =>
            laws.foldMapHomomorphism(s, f, g)
          ),
        "foldMap(_ => mempty) == mempty" ->
          forAll((s: S) => laws.foldMapEmpty(s)),
      )

  // H1 (place = transform const) is deliberately NOT a reusable law
  // here: the `transform` / `place` extensions need an implicit
  // `T => F[X, D]` whose type depends on the optic's existential `X`,
  // and abstracting over that would require a `val lens` with
  // per-instance identity givens — more plumbing than payoff. The
  // matching EoSpecificLawsSpec in cats-eo-tests tests H1 directly on
  // a concrete Lens.

  // ================= H3: put ≡ reverseGet ∘ pure ===================

  trait PutIsReverseGetLaws[S, A]:
    def iso: Optic[S, S, A, A, Forgetful]

    def putIsReverseGetCompose(a: A, f: A => A): Boolean =
      iso.put(f)(a) == iso.reverseGet(f(a))

  abstract class PutIsReverseGetTests[S, A] extends Laws:
    def laws: PutIsReverseGetLaws[S, A]

    def putIsReverseGet(using
        Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Optic.put ≡ reverseGet ∘ pure",
        "put f a == reverseGet (f a)" ->
          forAll((a: A, f: A => A) => laws.putIsReverseGetCompose(a, f)),
      )
