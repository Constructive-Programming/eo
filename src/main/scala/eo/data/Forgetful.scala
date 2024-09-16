package eo
package data

import optics.Optic

import cats.{Applicative, Bifunctor, Comonad, FlatMap, Functor, Invariant, Traverse}
import cats.syntax.comonad._
import cats.syntax.coflatMap._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.traverse._

type Forgetful[X, A] = A

type Forget[F[_]] = [X, A] =>> Forgetful[X, F[A]]

object Forgetful:

  given accesor: Accessor[Forgetful] with
    def get[A]: [X] => Forgetful[X, A] => A =
      [X] => (fa: Forgetful[X, A]) => fa

  given revaccesor: ReverseAccessor[Forgetful] with
    def reverseGet[A]: [X] => A => Forgetful[X, A] =
      [X] => (a: A) => a

  given map: ForgetfulFunctor[Forgetful] with
    def map[X, A, B]: Forgetful[X, A] => (A => B) => Forgetful[X, B] =
      a => f => f(a)

  given bifunctor[F[_]: Functor]: Bifunctor[Forget[F]] with
    def bimap[A, B, C, D](fab: Forget[F][A, B])(f: A => C, g: B => D): Forget[F][C, D] =
      fab.map(g)

  given traverse: ForgetfulTraverse[Forgetful, Invariant] with
    def traverse[X, A, B, G[_]: Invariant]
        : Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  given traverse2[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: Forget[F][X, A] => (A => G[B]) => G[Forget[F][X, B]] =
      fa => f => fa.traverse(f)

  given assoc[X, Y]: AssociativeFunctor[Forgetful, X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => A, A => C) => C =
      (s, f, g) => g(f(s))
    def associateRight[D, B, T]: (D, D => B, B => T) => T =
      (d, g, f) => f(g(d))

  given leftAssocForget[F[_]: FlatMap, X, Y]: LeftAssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => F[A], A => F[C]) => F[C] =
      (s, f, g) => f(s).flatMap(g)

  given rightAssocForget[F[_]: Comonad, X, Y]: RightAssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateRight[D, B, T]: (F[D], F[D] => B, F[B] => T) => T =
      (d, g, f) => f(d.coflatMap(g))

  given assocForget[F[_]: FlatMap: Comonad, X, Y]: AssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => F[A], A => F[C]) => F[C] =
      (s, f, g) => f(s).flatMap(g)
    def associateRight[D, B, T]: (F[D], F[D] => B, F[B] => T) => T =
      (d, g, f) => f(d.coflatMap(g))
