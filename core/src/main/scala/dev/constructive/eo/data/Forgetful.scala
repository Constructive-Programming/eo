package dev.constructive.eo
package data

import optics.Optic

import cats.Invariant

/** The identity carrier: `Forgetful[X, A] = A`.
  *
  * Carries no leftover — the `X` parameter is structurally ignored. Used by `Iso` and `Getter`,
  * where the optic's observation fully determines the focus and no reassembly information is
  * needed.
  *
  * The `F`-shape sibling [[Forget]] lives in its own file; instances that scale with the
  * typeclasses `F` admits (Functor / Foldable / Traverse / Applicative / Monad / Alternative)
  * belong there.
  */
type Forgetful[X, A] = A

/** Typeclass instances for [[Forgetful]].
  *
  * Every instance collapses to a plain function application at runtime — `Forgetful` forgets the
  * existential, so operations like `.modify` / `.get` / `.reverseGet` on Iso- / Getter-carrier
  * optics have zero per-call overhead beyond the user's function.
  */
object Forgetful:

  /** Reads the focus directly out of a `Forgetful[X, A]` — identity since `Forgetful[X, A] = A`.
    * Unlocks `.get` on Iso and Getter.
    *
    * @group Instances
    */
  given accessor: Accessor[Forgetful] with

    def get[A]: [X] => Forgetful[X, A] => A =
      [X] => (fa: Forgetful[X, A]) => fa

  /** Writes a fresh focus into a `Forgetful` — identity, for the same reason. Unlocks `.reverseGet`
    * on Iso.
    *
    * @group Instances
    */
  given reverseAccessor: ReverseAccessor[Forgetful] with

    def reverseGet[A]: [X] => A => Forgetful[X, A] =
      [X] => (a: A) => a

  /** `ForgetfulApplicative` — with `pure[X, A] = a`. Unlocks `.put` on Iso / Getter.
    *
    * @group Instances
    */
  given applicative: ForgetfulApplicative[Forgetful] with

    def map[X, A, B](fa: Forgetful[X, A], f: A => B): Forgetful[X, B] =
      f(fa)

    def pure[X, A](a: A): Forgetful[X, A] = a

  /** `ForgetfulTraverse[Forgetful, Invariant]` — the weakest applicative constraint that supports
    * Forgetful's `traverse`. Unlocks `.modifyF` / `.modifyA` on Iso and Getter carriers.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Forgetful, Invariant] with

    def traverse[X, A, B, G[_]: Invariant]: Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  /** `AssociativeFunctor[Forgetful, Xo, Xi]` — lets any two `Forgetful`-carrier optics compose via
    * `Optic.andThen`. Carrier-erasing: the result type `Z = Nothing` since `Forgetful` has no
    * leftover.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Forgetful, Xo, Xi] with
    type Z = Nothing

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Forgetful] { type X = Xo },
        inner: Optic[A, B, C, D, Forgetful] { type X = Xi },
    ): C = inner.to(outer.to(s))

    def composeFrom[S, T, A, B, C, D](
        xd: D,
        inner: Optic[A, B, C, D, Forgetful] { type X = Xi },
        outer: Optic[S, T, A, B, Forgetful] { type X = Xo },
    ): T = outer.from(inner.from(xd))
