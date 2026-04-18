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
  * Carries no leftover — the `X` parameter is structurally ignored.
  * Used by `Iso` and `Getter`, where the optic's observation fully
  * determines the focus and no reassembly information is needed.
  */
type Forgetful[X, A] = A

/** Adapt a `F[_]` container to the two-parameter carrier shape by
  * wrapping it under `Forgetful`. Equivalent to the classic
  * Haskell `newtype Forget r a b = Forget (a -> r)` construction
  * but applied to a functor `F`. Used by `Fold` and
  * `Traversal.each` to walk every element of an `F[A]`.
  */
type Forget[F[_]] = [X, A] =>> Forgetful[X, F[A]]

/** Typeclass instances for [[Forgetful]] and [[Forget]].
  *
  * Every instance collapses to a plain function application at
  * runtime — `Forgetful` forgets the existential, so operations
  * like `.modify` / `.get` / `.reverseGet` on Iso- / Getter-carrier
  * optics have zero per-call overhead beyond the user's function.
  */
object Forgetful:

  /** Reads the focus directly out of a `Forgetful[X, A]` — identity
    * since `Forgetful[X, A] = A`. Unlocks `.get` on Iso and Getter.
    *
    * @group Instances */
  given accessor: Accessor[Forgetful] with
    def get[A]: [X] => Forgetful[X, A] => A =
      [X] => (fa: Forgetful[X, A]) => fa

  /** Writes a fresh focus into a `Forgetful` — identity, for the
    * same reason. Unlocks `.reverseGet` on Iso.
    *
    * @group Instances */
  given reverseAccessor: ReverseAccessor[Forgetful] with
    def reverseGet[A]: [X] => A => Forgetful[X, A] =
      [X] => (a: A) => a

  /** `ForgetfulApplicative` — with `pure[X, A] = a`. Unlocks `.put`
    * on Iso / Getter.
    *
    * @group Instances */
  given applicative: ForgetfulApplicative[Forgetful] with
    def map[X, A, B](fa: Forgetful[X, A], f: A => B): Forgetful[X, B] =
      f(fa)
    def pure[X, A](a: A): Forgetful[X, A] = a

  /** `Bifunctor[Forget[F]]` via the underlying `Functor[F]`. The
    * left parameter is phantom, so bimap leaves it untouched and
    * maps only through the right-side `F[_]`.
    *
    * @group Instances */
  given bifunctor[F[_]: Functor]: Bifunctor[Forget[F]] with
    def bimap[A, B, C, D](fab: Forget[F][A, B])(f: A => C, g: B => D): Forget[F][C, D] =
      fab.map(g)

  /** `ForgetfulTraverse[Forgetful, Invariant]` — the weakest
    * applicative constraint that supports Forgetful's `traverse`.
    * Unlocks `.modifyF` / `.modifyA` on Iso and Getter carriers.
    *
    * @group Instances */
  given traverse: ForgetfulTraverse[Forgetful, Invariant] with
    def traverse[X, A, B, G[_]: Invariant]
        : Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  /** `ForgetfulTraverse[Forget[F], Applicative]` — lifts the
    * underlying `Traverse[F]` into the two-parameter carrier
    * shape. This is what makes `Fold` and `Traversal.each` work.
    *
    * @group Instances */
  given traverse2[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: Forget[F][X, A] => (A => G[B]) => G[Forget[F][X, B]] =
      fa => f => fa.traverse(f)

  /** `AssociativeFunctor[Forgetful, X, Y]` — lets any two
    * `Forgetful`-carrier optics compose via `Optic.andThen`.
    *
    * @group Instances */
  given assoc[X, Y]: AssociativeFunctor[Forgetful, X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => A, A => C) => C =
      (s, f, g) => g(f(s))
    def associateRight[D, B, T]: (D, D => B, B => T) => T =
      (d, g, f) => f(g(d))

  /** `LeftAssociativeFunctor[Forget[F], X, Y]` for any `F` with
    * `FlatMap` — needed by `Optic.andThenLeft` on Fold chains.
    *
    * @group Instances */
  given leftAssocForget[F[_]: FlatMap, X, Y]: LeftAssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => F[A], A => F[C]) => F[C] =
      (s, f, g) => f(s).flatMap(g)

  /** `RightAssociativeFunctor[Forget[F], X, Y]` for any `F` with
    * `Comonad` — the dual of [[leftAssocForget]].
    *
    * @group Instances */
  given rightAssocForget[F[_]: Comonad, X, Y]: RightAssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateRight[D, B, T]: (F[D], F[D] => B, F[B] => T) => T =
      (d, g, f) => f(d.coflatMap(g))

  /** Full `AssociativeFunctor[Forget[F], X, Y]` — sums the left
    * and right variants above. Requires both `FlatMap` and
    * `Comonad` on `F`.
    *
    * @group Instances */
  given assocForget[F[_]: FlatMap: Comonad, X, Y]: AssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => F[A], A => F[C]) => F[C] =
      (s, f, g) => f(s).flatMap(g)
    def associateRight[D, B, T]: (F[D], F[D] => B, F[B] => T) => T =
      (d, g, f) => f(d.coflatMap(g))
