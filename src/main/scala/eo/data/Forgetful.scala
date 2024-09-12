package eo
package data

import optics.Optic

import cats.{Applicative, Bifunctor, FlatMap, Functor, Invariant, Traverse}
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

  given assocForget[F[_]: FlatMap, X, Y]: AssociativeFunctor[Forget[F], X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => F[A], A => F[C]) => F[C] =
      case (s, f, g) => f(s).flatMap(g)
    def associateRight[D, B, T]: (F[D], F[D] => B, F[B] => T) => T =
      case (d, g, f) => ???

  given assoc[X, Y]: AssociativeFunctor[Forgetful, X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => A, A => C) => C =
      case (s, f, g) => g(f(s))
    def associateRight[D, B, T]: (D, D => B, B => T) => T =
      case (d, g, f) => f(g(d))

  given tuple2forget[F[_]: Applicative]: Composer[Tuple2, Forget[F]] with
    def to[S, T, A, B](o: Optic[S, T, A, B, [T1, T2] =>> (T1, T2)]): Optic[S, T, A, B, Forget[F]] =
      new Optic[S, T, A, B, Forget[F]]:
        type X = Nothing
        def to: S => F[A] = o.get.andThen(Applicative[F].pure)
        def from: F[B] => T = ???

  given tuple2forgetful: Composer[Tuple2, Forgetful] with
    def to[S, T, A, B](o: Optic[S, T, A, B, [T1, T2] =>> (T1, T2)]): Optic[S, T, A, B, Forgetful] =
      new Optic[S, T, A, B, Forgetful]:
        type X = Nothing
        def to: S => A = o.get
        def from: B => T = ???
