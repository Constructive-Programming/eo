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

/** Capability ladder for [[Forget]]. Each typeclass on `F` unlocks a matching optic-level operation
  * via a dedicated given instance:
  *
  * {{{
  *   F: Functor      →  ForgetfulFunctor[Forget[F]]              →  .modify / .replace
  *   F: Foldable     →  ForgetfulFold[Forget[F]]                 →  .foldMap
  *   F: Traverse     →  ForgetfulTraverse[Forget[F], Applicative] →  .modifyA, .all
  *   F: Applicative  →  ForgetfulApplicative[Forget[F]]          →  .put
  *   F: Monad        →  AssociativeFunctor[Forget[F], _, _]      →  same-carrier .andThen
  *                                                                  (algebraic-lens composition:
  *                                                                   `flatMap` on push, `pure`
  *                                                                   on pull)
  * }}}
  *
  * `Forget[F]`'s `X` is phantom (`Forget[F][X, A] = F[A]`), which is exactly what `Traversal` /
  * `Fold` want — those families never need outer-structural context on the `from` side. For the
  * richer multi-focus family, where a `Lens` / `Prism` / `Optional` must be bridged into a
  * classifier-shape carrier and the outer's leftover `X` has to survive the round-trip, the pair
  * carrier [[MultiFocus]] (`[X, A] =>> (X, F[A])`) is the right home; `Forget[F]` injects trivially
  * into it via `Composer[Forget[F], MultiFocus[F]]`.
  *
  * Instances that don't target `Forget[F]` specifically (for example `AssociativeFunctor[Forgetful,
  * _, _]` used by `Iso` / `Getter`) live in [[Forgetful]].
  *
  * '''2026-04-26 dedup.''' The two `AssociativeFunctor[Forget[F], _, _]` instances —
  * Monad-via-`pure-then-from` and FlatMap+Comonad-via-`coflatMap-from` — used to be 14-line near-
  * twin instance bodies that only differed in the last line of `composeFrom`. They now share a
  * single `Forget.assocFor[F]` builder that takes the pull-side `redistribute: F[D] => F[B]` as a
  * parameter. The `assocForgetMonad` / `assocForgetComonad` givens reduce to `assocFor[F](pure)` /
  * `assocFor[F](_.coflatMap(_))` plumbing, the priority ordering is preserved, and the wider "users
  * can opt into the Comonad pull explicitly" contract is unchanged.
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
    * inner's `from` through the supplied [[ForgetPull]] strategy, hands the resulting `F[B]` to the
    * outer's `from`.
    *
    * Both `assocForgetMonad` (Monad pull) and `assocForgetComonad` (Comonad pull) reduce to
    * `assocFor[F]` plus a strategy. The duplicated 14-line bodies that used to live on each
    * instance are gone — only the per-strategy plumbing differs.
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

  /** `AssociativeFunctor[Forget[F], Xo, Xi]` for any `F: Monad` — algebraic-lens composition of two
    * classifier-style optics `S -> F[A], F[B] -> T`:
    *
    *   - push side: `outer.to(s).flatMap(inner.to)` threads the F-context through the inner
    *     classifier, collecting `F[C]` candidates.
    *   - pull side: `outer.from(F.pure(inner.from(xd)))` — the inner collapses `F[D]` back to a
    *     single `B`, which is re-lifted via `pure` so the outer's `F[B] => T` can rebuild.
    *
    * Higher priority than the [[LowPriorityForgetInstances.assocForgetComonad]] `FlatMap + Comonad`
    * variant so that whenever an `F` is a full `Monad` this composition wins.
    *
    * @group Instances
    */
  given assocForgetMonad[F[_]: Monad, Xo, Xi]: AssociativeFunctor[Forget[F], Xo, Xi] =
    assocFor[F, Xo, Xi](ForgetPull.monadicPull[F])

/** Lower-priority instance drawer for [[Forget]]. Holds the older `FlatMap + Comonad` associative
  * functor, which composes via `coflatMap` — genuinely different semantics from the `Monad`-based
  * classifier composition in the main object, and kept at lower priority so that whenever an `F` is
  * a full `Monad` the algebraic-lens composition wins implicit resolution. Users who want the
  * Comonad-based parallel-fold semantics explicitly can still summon this instance by hand.
  */
trait LowPriorityForgetInstances:

  /** `AssociativeFunctor[Forget[F], Xo, Xi]` for any `F` with both `FlatMap` (push side) and
    * `Comonad` (pull side). Composes via `coflatMap` on the `from` direction — distributes the
    * inner's `from` across the context. Predates the algebraic-lens framing.
    *
    * @group Instances
    */
  given assocForgetComonad[F[_]: FlatMap: Comonad, Xo, Xi]: AssociativeFunctor[Forget[F], Xo, Xi] =
    Forget.assocFor[F, Xo, Xi](
      new Forget.ForgetPull[F]:

        def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B] =
          // Comonad pull: distribute fromInner across each "shape" of the F[D] via
          // coflatMap. The classical Comonad.coflatMap takes (W[A], W[A] => B) ⇒ W[B];
          // the inner's `from: F[D] => B` is exactly that shape after the partial
          // application. Predates the algebraic-lens framing but kept for backwards
          // compatibility — see the `LowPriorityForgetInstances` doc.
          xd.coflatMap(fromInner)
    )
