package dev.constructive.eo
package data

import cats.Invariant

import optics.Optic

/** Identity carrier: `Forgetful[X, A] = A`. `X` is phantom. Used by `Iso` and `Getter` where the
  * focus is fully determined and no reassembly information is needed. The `F`-shape sibling
  * [[Forget]] lives in its own file.
  */
type Forgetful[X, A] = A

/** Typeclass instances for [[Forgetful]]. Every operation collapses to plain function application
  * at runtime — zero per-call overhead beyond the user's function.
  */
object Forgetful:

  /** Identity read; unlocks `.get` on Iso / Getter. @group Instances */
  given accessor: Accessor[Forgetful] with

    def get[A]: [X] => Forgetful[X, A] => A =
      [X] => (fa: Forgetful[X, A]) => fa

  /** Identity write; unlocks `.reverseGet` on Iso. @group Instances */
  given reverseAccessor: ReverseAccessor[Forgetful] with

    def reverseGet[A]: [X] => A => Forgetful[X, A] =
      [X] => (a: A) => a

  /** `pure[X, A] = a`; unlocks `.put` on Iso / Getter. @group Instances */
  given applicative: ForgetfulApplicative[Forgetful] with

    def map[X, A, B](fa: Forgetful[X, A], f: A => B): Forgetful[X, B] =
      f(fa)

    def pure[X, A](a: A): Forgetful[X, A] = a

  /** Weakest constraint for Forgetful's `traverse` (`Invariant[G]`). Unlocks `.modifyF` /
    * `.modifyA` on Iso / Getter.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Forgetful, Invariant] with

    def traverse[X, A, B, G[_]: Invariant]: Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  /** Lets `Forgetful`-carrier optics compose; `Z = Nothing` since Forgetful has no leftover.
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
