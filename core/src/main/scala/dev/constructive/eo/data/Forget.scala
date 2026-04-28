package dev.constructive.eo
package data

import cats.syntax.coflatMap.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Applicative, Bifunctor, Comonad, FlatMap, Foldable, Functor, Monad, Traverse}

import optics.Optic

/** Adapt a `F[_]` container to the two-parameter carrier shape by wrapping it under the phantom
  * `X`. Equivalent to the classic Haskell `newtype Forget r a b = Forget (a -> r)` construction but
  * applied to a type constructor `F`: `Forget[F][X, A] = F[A]`, ignoring `X` completely.
  *
  * Used by [[dev.constructive.eo.optics.Fold]] and the multi-focus family
  * ([[dev.constructive.eo.data.MultiFocus]]) as a uniform "F-shape carrier" whose optic-level
  * capabilities scale with the typeclasses `F` itself admits.
  */
type Forget[F[_]] = [X, A] =>> Forgetful[X, F[A]]

/** Capability ladder for [[Forget]]. Each typeclass on `F` unlocks a matching optic operation:
  *
  * {{{
  *   F: Functor      →  ForgetfulFunctor[Forget[F]]              →  .modify / .replace
  *   F: Foldable     →  ForgetfulFold[Forget[F]]                 →  .foldMap
  *   F: Traverse     →  ForgetfulTraverse[Forget[F], Applicative] →  .modifyA, .all
  *   F: Applicative  →  ForgetfulApplicative[Forget[F]]          →  .put
  *   F: Monad        →  AssociativeFunctor[Forget[F], _, _]      →  same-carrier .andThen
  *                                                                  (algebraic-lens shape)
  * }}}
  *
  * `Forget[F]`'s `X` is phantom — Traversal / Fold need no outer-structural context on `from`. For
  * cases where the outer's leftover must survive, use the pair carrier [[MultiFocus]]; `Forget[F]`
  * injects trivially into it via `Composer[Forget[F], MultiFocus[F]]`. Forgetful-targeting
  * instances live in [[Forgetful]].
  */
object Forget extends LowPriorityForgetInstances:

  /** Strategy for "redistribute the inner `from` across the F-context" on the pull side. The Monad
    * form ignores the `F[D]` and lifts a single `inner.from(xd)` via `pure`; the Comonad form
    * `coflatMap`s `inner.from` over the existing F[D]. Other lawful strategies (e.g.
    * `Distributive[F].cosequence` over an inner `F[F[B]]`) could be added as further instances.
    */
  trait ForgetPull[F[_]]:

    /** Given an `F[D]` and a `D => F[B]` (the inner's `from` post-composed with `pure` to lift
      * scalar results when needed), produce an `F[B]` to feed the outer's `from`.
      */
    def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B]

  object ForgetPull:

    /** Monad-based pull — the algebraic-lens semantics. `inner.from(xd)` collapses the F[D] to a
      * single B, which is re-lifted via `pure` so the outer's F[B]-shaped `from` can rebuild.
      */
    given monadicPull[F[_]: Applicative]: ForgetPull[F] with

      def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B] =
        Applicative[F].pure(fromInner(xd))

  /** Shared `AssociativeFunctor[Forget[F], Xo, Xi]` body — push uses `flatMap`, pull threads the
    * inner's `from` through the supplied [[ForgetPull]] strategy. Both `assocForgetMonad` (Monad
    * pull) and `assocForgetComonad` (Comonad pull) reduce to `assocFor[F]` plus a strategy.
    */
  private[data] def assocFor[F[_]: FlatMap, Xo, Xi](
      pull: ForgetPull[F]
  ): AssociativeFunctor[Forget[F], Xo, Xi] =
    new AssociativeFunctor[Forget[F], Xo, Xi]:
      type Z = Nothing

      def composeTo[S, T, A, B, C, D](
          s: S,
          outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
          inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
      ): F[C] = outer.to(s).flatMap(inner.to)

      def composeFrom[S, T, A, B, C, D](
          xd: F[D],
          inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
          outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
      ): T = outer.from(pull.redistribute(xd)(inner.from))

  /** `Bifunctor[Forget[F]]` via the underlying `Functor[F]`. Left parameter is phantom; `bimap`
    * routes only through the right-side `F`.
    *
    * @group Instances
    */
  given bifunctor[F[_]: Functor]: Bifunctor[Forget[F]] with

    def bimap[A, B, C, D](fab: Forget[F][A, B])(f: A => C, g: B => D): Forget[F][C, D] =
      fab.map(g)

  /** Direct `ForgetfulFunctor` — calls `Functor[F].map` without routing through `Bifunctor`.
    *
    * @group Instances
    */
  given forgetFFunctor[F[_]](using F: Functor[F]): ForgetfulFunctor[Forget[F]] with
    def map[X, A, B](fa: F[A], f: A => B): F[B] = F.map(fa)(f)

  /** `ForgetfulApplicative[Forget[F]]` via any `Applicative[F]`. Unlocks `Optic.put` on
    * Forget-carrier optics.
    *
    * @group Instances
    */
  given forgetFApplicative[F[_]: Applicative]: ForgetfulApplicative[Forget[F]] with
    def map[X, A, B](fa: F[A], f: A => B): F[B] = Applicative[F].map(fa)(f)
    def pure[X, A](a: A): F[A] = Applicative[F].pure(a)

  /** `ForgetfulFold[Forget[F]]` — delegates to the underlying `Foldable[F]`. Powers
    * `Fold.apply[F, A]`.
    *
    * @group Instances
    */
  given forgetFFold[F[_]: Foldable]: ForgetfulFold[Forget[F]] with

    def foldMap[X, A, M: cats.Monoid]: (A => M) => F[A] => M =
      f => fa => Foldable[F].foldMap(fa)(f)

  /** `ForgetfulTraverse[Forget[F], Applicative]` — lifts `Traverse[F]` into the two-parameter
    * carrier shape. Core of `Fold` in its effectful form.
    *
    * @group Instances
    */
  given forgetFTraverse[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: F[A] => (A => G[B]) => G[F[B]] =
      Traverse[F].traverse[G, A, B]

  /** Algebraic-lens composition for `F: Monad`. Push: `outer.to(s).flatMap(inner.to)`. Pull: the
    * inner's `from` collapses `F[D]` to `B`, re-lifted via `pure` for the outer's `F[B] => T`.
    * Higher priority than [[LowPriorityForgetInstances.assocForgetComonad]] so Monad wins.
    *
    * @group Instances
    */
  given assocForgetMonad[F[_]: Monad, Xo, Xi]: AssociativeFunctor[Forget[F], Xo, Xi] =
    assocFor[F, Xo, Xi](ForgetPull.monadicPull[F])

/** Lower-priority instance drawer — holds the `FlatMap + Comonad` `AssociativeFunctor` which
  * composes via `coflatMap` (parallel-fold semantics, genuinely different from the Monad-based
  * algebraic-lens composition in the main object). Kept at lower priority so Monad wins when both
  * apply.
  */
trait LowPriorityForgetInstances:

  /** Comonad-pull composition for `F: FlatMap + Comonad`. Composes via `coflatMap` on `from`.
    *
    * @group Instances
    */
  given assocForgetComonad[F[_]: FlatMap: Comonad, Xo, Xi]: AssociativeFunctor[Forget[F], Xo, Xi] =
    Forget.assocFor[F, Xo, Xi](
      new Forget.ForgetPull[F]:

        def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B] =
          xd.coflatMap(fromInner)
    )
