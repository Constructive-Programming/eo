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

  // ================= H2 + H4: transform / place / transfer =========
  //
  // The `transform` extension requires an implicit `ev: T => F[o.X, D]`
  // whose type depends on the optic's existential `X`. That forced us
  // to test H1 inline rather than as a reusable law, but H2 (transfer
  // ≡ curried place) and H4 (transform identity) are cleanly
  // expressible once we:
  //
  //   * declare `optic` as a `val` so `optic.X` is a stable path-
  //     dependent type, and
  //   * leave the `given transformEv` abstract so each concrete
  //     subclass supplies the S => (optic.X, A) evidence (identity for
  //     Lens.second, swap for Lens.first, …).

  trait TransformLaws[S, A, X0]:
    /** The lens-shaped optic under test, with its existential `X`
      * surfaced as the explicit `X0` parameter so the `ev` given can
      * refer to it without path-dependent shenanigans. */
    val optic: Optic[S, S, A, A, Tuple2] { type X = X0 }

    /** Evidence that `S` can be viewed as `(X0, A)`. Concrete subclass
      * supplies this — `identity` for `Lens.second`, `_.swap` for
      * `Lens.first`, etc. */
    given transformEv: (S => (X0, A))

    /** H4 — transform identity is a no-op. */
    def transformIdentity(t: S): Boolean =
      optic.transform(identity[A])(t) == t

    /** H2 — `transfer(f)(t)(c)` equals `place(f(c))(t)`. */
    def transferIsCurriedPlace(t: S, c: A, f: A => A): Boolean =
      optic.transfer(f)(t)(c) == optic.place(f(c))(t)

    /** H1 — `place(a)` equals `transform(_ => a)`. */
    def placeIsTransformConst(t: S, a: A): Boolean =
      optic.place(a)(t) == optic.transform(_ => a)(t)

  abstract class TransformTests[S, A, X0] extends Laws:
    def laws: TransformLaws[S, A, X0]

    def transform(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Lens transform / place / transfer",
        "transform(identity) == identity" ->
          forAll((t: S) => laws.transformIdentity(t)),
        "transfer(f)(t)(c) == place(f(c))(t)" ->
          forAll((t: S, c: A, f: A => A) =>
            laws.transferIsCurriedPlace(t, c, f)
          ),
        "place(a) == transform(_ => a)" ->
          forAll((t: S, a: A) => laws.placeIsTransformConst(t, a)),
      )

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

  // ================= F1: Composer.chain path independence ==========
  //
  // From Forgetful there are two direct composers (→ Tuple2, → Either)
  // and from each of those a direct composer to Affine. So an Iso can
  // land in Affine via two distinct chains. They should be modify-
  // equivalent — otherwise `Composer.chain` would be making the
  // caller's choice of intermediary observable.

  trait ChainPathIndependenceLaws[S, A]:
    def iso: Optic[S, S, A, A, Forgetful]

    // Using the composers explicitly sidesteps the implicit-resolution
    // ambiguity that `iso.morph[Affine]` would hit (Scala can't pick
    // between Tuple2 and Either for the intermediate G).
    private def viaTuple2: Optic[S, S, A, A, Affine] =
      Affine.tuple2affine.to(Composer.forgetful2tuple.to(iso))

    private def viaEither: Optic[S, S, A, A, Affine] =
      Affine.either2affine.to(Composer.forgetful2either.to(iso))

    def chainPathIndependence(s: S, f: A => A): Boolean =
      viaTuple2.modify(f)(s) == viaEither.modify(f)(s)

  abstract class ChainPathIndependenceTests[S, A] extends Laws:
    def laws: ChainPathIndependenceLaws[S, A]

    def chainPathIndependence(using
        Arbitrary[S], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Composer.chain path independence",
        "Forgetful→Tuple2→Affine ≡ Forgetful→Either→Affine on modify" ->
          forAll((s: S, f: A => A) =>
            laws.chainPathIndependence(s, f)
          ),
      )

  // ================= F2: Composer.chain preserves Accessor =========
  //
  // A 2-hop chain `F → G → H` preserves `get` whenever all three
  // carriers have an Accessor. Currently core only ships Accessor
  // instances for Forgetful and Tuple2, so the only non-degenerate
  // witness is Forgetful → Tuple2 → Tuple2 with an identity composer
  // at the second hop — testable with a locally-declared identity
  // Composer in the spec. The trait itself is fully generic and will
  // gain more witnesses as new Accessor instances are added.

  trait ChainAccessorLaws[S, A, F[_, _], G[_, _], H[_, _]]:
    def optic: Optic[S, S, A, A, F]
    def fToG: eo.Composer[F, G]
    def gToH: eo.Composer[G, H]

    // The `get` extension on both ends needs an Accessor in scope.
    // Middle-step Accessor isn't required by the law statement but
    // is useful for sanity-checking intermediates if anyone extends
    // this spec.
    given accessorF: eo.data.Accessor[F]
    given accessorH: eo.data.Accessor[H]

    def chainPreservesGet(s: S): Boolean =
      gToH.to(fToG.to(optic)).get(s) == optic.get(s)

  abstract class ChainAccessorTests[S, A, F[_, _], G[_, _], H[_, _]]
      extends Laws:
    def laws: ChainAccessorLaws[S, A, F, G, H]

    def chainAccessor(using Arbitrary[S]): RuleSet =
      new SimpleRuleSet(
        "Composer.chain preserves get",
        "chain(F→G→H).get(s) == optic.get(s)" ->
          forAll((s: S) => laws.chainPreservesGet(s)),
      )

  // ================= G1, G2: Optic.all on Forget[T] ================
  //
  // `Optic.all` uses `traverse(List(_))` under the hood, which with
  // List's cartesian applicative yields a one-element list wrapping
  // the whole container — not a per-element enumeration. These laws
  // pin that behaviour down so later changes can't silently break it.

  trait TraverseAllLaws[T[_], A](using val FT: Traverse[T]):
    def traversal: Optic[T[A], T[A], A, A, Forget[T]]

    /** G1 — `all` always has exactly one element. */
    def allHasLengthOne(s: T[A]): Boolean =
      traversal.all(s).length == 1

    /** G2 — that single element is the original container. */
    def allHeadIsInput(s: T[A]): Boolean =
      traversal.all(s).head == s

  abstract class TraverseAllTests[T[_]: Traverse, A] extends Laws:
    def laws: TraverseAllLaws[T, A]

    def traverseAll(using Arbitrary[T[A]]): RuleSet =
      new SimpleRuleSet(
        "Optic.all on Forget[T]",
        "all(s).length == 1" ->
          forAll((s: T[A]) => laws.allHasLengthOne(s)),
        "all(s).head == s" ->
          forAll((s: T[A]) => laws.allHeadIsInput(s)),
      )

  // ================= G3: all-then-map ≡ modify on Forget[T] ========
  //
  // For a `Forget[T]`-carrier optic, `all(s)` is `List(s)` (a single-
  // element list holding the whole container — see TraverseAllLaws).
  // Because `Forget[T][X, A]` type-reduces to `T[A]`, that head is
  // already an ordinary `T[A]` that `Functor[T].map` can walk. The
  // law says mapping the head agrees with `modify`, tying `all`'s
  // reading of the structure to `modify`'s rewriting of it.

  trait ForgetAllModifyLaws[T[_], A](using val T: Traverse[T]):
    def traversal: Optic[T[A], T[A], A, A, Forget[T]]

    def allMapEqualsModify(s: T[A], f: A => A): Boolean =
      val head: T[A] = traversal.all(s).head
      T.map(head)(f) == traversal.modify(f)(s)

  abstract class ForgetAllModifyTests[T[_]: Traverse, A] extends Laws:
    def laws: ForgetAllModifyLaws[T, A]

    def allMap(using
        Arbitrary[T[A]], Arbitrary[A], Cogen[A]
    ): RuleSet =
      new SimpleRuleSet(
        "Forget[T]: all-then-map ≡ modify",
        "T.map(all(s).head)(f) == modify(f)(s)" ->
          forAll((s: T[A], f: A => A) => laws.allMapEqualsModify(s, f)),
      )
