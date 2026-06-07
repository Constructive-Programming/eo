package dev.constructive.eo
package data

import cats.Invariant

import optics.Optic

/** Identity carrier: `Direct[X, A] = A`. `X` is phantom. Used by `Iso` and `Getter` where the focus
  * is fully determined and no reassembly information is needed — the optic's `to` / `from` are
  * plain functions, so it is the *forgetful functor* (it forgets the leftover `X` entirely). Its
  * instances therefore live under the `Forgetful*` typeclasses ([[ForgetfulFunctor]],
  * [[ForgetfulTraverse]], …). The `F`-shape sibling [[Forget]] lives in its own file.
  */
type Direct[X, A] = A

/** Typeclass instances for [[Direct]]. Every operation collapses to plain function application at
  * runtime — zero per-call overhead beyond the user's function.
  */
object Direct:

  /** Identity read; unlocks `.get` on Iso / Getter. @group Instances */
  given accessor: Accessor[Direct] with

    def get[A]: [X] => Direct[X, A] => A =
      [X] => (fa: Direct[X, A]) => fa

  /** Identity write; unlocks `.reverseGet` on Iso. @group Instances */
  given reverseAccessor: ReverseAccessor[Direct] with

    def reverseGet[A]: [X] => A => Direct[X, A] =
      [X] => (a: A) => a

  /** `pure[X, A] = a`; unlocks `.put` on Iso / Getter. @group Instances */
  given applicative: ForgetfulApplicative[Direct] with

    def map[X, A, B](fa: Direct[X, A], f: A => B): Direct[X, B] =
      f(fa)

    def pure[X, A](a: A): Direct[X, A] = a

  /** Weakest constraint for Direct's `traverse` (`Invariant[G]`). Unlocks `.modifyF` / `.modifyA`
    * on Iso / Getter.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Direct, Invariant] with

    def traverse[X, A, B, G[_]: Invariant]: Direct[X, A] => (A => G[B]) => G[Direct[X, B]] =
      fa => _(fa)

  /** Lets `Direct`-carrier optics compose; `Z = Nothing` since Direct has no leftover.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Direct, Xo, Xi] with
    type Z = Nothing

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Direct] { type X = Xo },
        inner: Optic[A, B, C, D, Direct] { type X = Xi },
    ): C = inner.to(outer.to(s))

    def composeFrom[S, T, A, B, C, D](
        xd: D,
        inner: Optic[A, B, C, D, Direct] { type X = Xi },
        outer: Optic[S, T, A, B, Direct] { type X = Xo },
    ): T = outer.from(inner.from(xd))
