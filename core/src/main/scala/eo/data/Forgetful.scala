package eo
package data

import optics.Optic

import cats.{Applicative, Bifunctor, Comonad, FlatMap, Functor, Invariant, Traverse}
import cats.syntax.comonad._
import cats.syntax.coflatMap._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._

/** The identity carrier: `Forgetful[X, A] = A`.
  *
  * Carries no leftover — the `X` parameter is structurally ignored. Used by `Iso` and `Getter`,
  * where the optic's observation fully determines the focus and no reassembly information is
  * needed.
  */
type Forgetful[X, A] = A

/** Adapt a `F[_]` container to the two-parameter carrier shape by wrapping it under `Forgetful`.
  * Equivalent to the classic Haskell `newtype Forget r a b = Forget (a -> r)` construction but
  * applied to a functor `F`. Used by `Fold` and `Traversal.each` to walk every element of an
  * `F[A]`.
  */
type Forget[F[_]] = [X, A] =>> Forgetful[X, F[A]]

/** Typeclass instances for [[Forgetful]] and [[Forget]].
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

  /** `Bifunctor[Forget[F]]` via the underlying `Functor[F]`. The left parameter is phantom, so
    * bimap leaves it untouched and maps only through the right-side `F[_]`.
    *
    * @group Instances
    */
  given bifunctor[F[_]: Functor]: Bifunctor[Forget[F]] with

    def bimap[A, B, C, D](fab: Forget[F][A, B])(f: A => C, g: B => D): Forget[F][C, D] =
      fab.map(g)

  /** `ForgetfulTraverse[Forgetful, Invariant]` — the weakest applicative constraint that supports
    * Forgetful's `traverse`. Unlocks `.modifyF` / `.modifyA` on Iso and Getter carriers.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Forgetful, Invariant] with

    def traverse[X, A, B, G[_]: Invariant]: Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  /** `ForgetfulTraverse[Forget[F], Applicative]` — lifts the underlying `Traverse[F]` into the
    * two-parameter carrier shape. This is what makes `Fold` and `Traversal.each` work.
    *
    * @group Instances
    */
  given traverse2[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Forget[F][X, A] => (A => G[B]) => G[Forget[F][X, B]] =
      fa => f => fa.traverse(f)

  /** `AssociativeFunctor[Forgetful, Xo, Xi]` — lets any two `Forgetful`-carrier optics compose via
    * `Optic.andThen`. Carrier-erasing: the result type `Z = Nothing` since `Forgetful` has no
    * leftover.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Forgetful, Xo, Xi] with
    type Z = Nothing

    def composeTo[S, T, A, B, C, D](
        s:     S,
        outer: Optic[S, T, A, B, Forgetful] { type X = Xo },
        inner: Optic[A, B, C, D, Forgetful] { type X = Xi },
    ): C = inner.to(outer.to(s))

    def composeFrom[S, T, A, B, C, D](
        xd:    D,
        inner: Optic[A, B, C, D, Forgetful] { type X = Xi },
        outer: Optic[S, T, A, B, Forgetful] { type X = Xo },
    ): T = outer.from(inner.from(xd))

  /** `AssociativeFunctor[Forget[F], Xo, Xi]` for any `F` with both `FlatMap` (push side) and
    * `Comonad` (pull side). Powers `traversal.andThen(traversal)` style composition over a
    * shared container carrier.
    *
    * @group Instances
    */
  given assocForget[F[_]: FlatMap: Comonad, Xo, Xi]: AssociativeFunctor[Forget[F], Xo, Xi] with
    type Z = Nothing

    def composeTo[S, T, A, B, C, D](
        s:     S,
        outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
        inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
    ): F[C] = outer.to(s).flatMap(inner.to)

    def composeFrom[S, T, A, B, C, D](
        xd:    F[D],
        inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
        outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
    ): T = outer.from(xd.coflatMap(inner.from))
