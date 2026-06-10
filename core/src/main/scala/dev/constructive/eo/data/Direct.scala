package dev.constructive.eo
package data

import cats.Invariant

import accessor.*
import forgetful.*
import compose.*
import optics.Optic

/** Identity carrier: `Direct[X, A] = A` (opaque). `X` is phantom. Used by `Iso` and `Getter` where
  * the focus is fully determined and no reassembly information is needed — the optic's `to` /
  * `from` are plain functions, so it is the *forgetful functor* (it forgets the leftover `X`
  * entirely). Its instances therefore live under the `Forgetful*` typeclasses
  * ([[ForgetfulFunctor]], [[ForgetfulTraverse]], …). The `F`-shape sibling [[Forget]] lives in its
  * own file.
  *
  * `opaque` (not a transparent alias) so it is a *distinct* type for implicit search: `object
  * Direct` is its companion, so `Accessor[Direct]`, `AssociativeFunctor[Direct, …]`, etc. resolve
  * via companion scope with no import, and the compiler never dealiases `Direct[X, A]` to `A`
  * (which would lose those givens). It still erases to `A`, so [[Direct.apply]] (wrap) / [[value]]
  * (unwrap) are identity at runtime.
  */
opaque type Direct[X, A] = A

/** Typeclass instances for [[Direct]], plus the `wrap` / `unwrap` boundary. Every operation
  * collapses to plain function application at runtime — zero per-call overhead beyond the user's
  * function.
  */
object Direct:

  /** Wrap a plain `A` into the `Direct` carrier. Identity at runtime (the opaque type erases to
    * `A`); needed only so construction sites outside this object satisfy the `Direct[X, A]` type.
    */
  transparent inline def apply[X, A](a: A): Direct[X, A] = a

  /** Unwrap the carried `A`. Identity at runtime. */
  extension [X, A](d: Direct[X, A]) transparent inline def value: A = d

  /** Identity read; unlocks `.get` on Iso / Getter. @group Instances */
  given accessor: Accessor[Direct] with

    def get[X, A](fa: Direct[X, A]): A = fa

  /** Identity write; unlocks `.reverseGet` on Iso. @group Instances */
  given reverseAccessor: ReverseAccessor[Direct] with

    def reverseGet[X, A](a: A): Direct[X, A] = a

  /** Single-focus fold — applies `f` to the one focus. Unlocks `.foldMap` on Direct-carrier optics
    * and lets `ReadCompose`'s many-fold tier pair a Direct side with a multi-focus side (`getter ∘
    * traversal`, `traversal ∘ getter`, …).
    *
    * @group Instances
    */
  given fold: ForgetfulFold[Direct] with

    def foldMap[X, A, M: cats.Monoid](f: A => M, fa: Direct[X, A]): M = f(fa)

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

    def traverse[X, A, B, G[_]: Invariant](fa: Direct[X, A], f: A => G[B]): G[Direct[X, B]] =
      f(fa)

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
