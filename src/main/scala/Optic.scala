package eo

import cats.{Applicative, Functor}
import cats.arrow.Profunctor
import cats.syntax.functor._

/** This is an 'existential' implementation of profunctor optics.
  The idea is that instead of using forall, we use exists.
  e.g
  `type Optic s t a b = forall p. Profunctor p => p a b -> p s t`
  we do
  `type Optic s t a b = exists c => (s -> (cXa), cXb -> t)`
  which are clearly encoded in the `to` and `from` methods bellow.
  */
trait Optic[S, T, A, B, F[_, _]] { self =>
  type X

  def to: S => F[X, A]
  def from: F[X, B] => T

  def andThen[C, D](o: Optic[A, B, C, D, F])(using
      af: AssociativeFunctor[F, self.X, o.X]
  ): Optic[S, T, C, D, F] = new Optic {
    type X = af.Z
    def to: S => F[X, C] = s => af.associateLeft(s, self.to, o.to)
    def from: F[X, D] => T = xd => af.associateRight(xd, o.from, self.from)
  }

  def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
    cf.to(self)

}

object Optic {
  given outerProfunctor[A, B, F[_, _]]
      : Profunctor[[S, T] =>> Optic[S, T, A, B, F]] with
    def dimap[S, T, R, U](
        o: Optic[S, T, A, B, F]
    )(f: R => S)(g: T => U): Optic[R, U, A, B, F] = new Optic[R, U, A, B, F] {
      type X = o.X
      def to: R => F[X, A] = o.to.compose(f)
      def from: F[X, B] => U = o.from.andThen(g)
    }

  given innerProfunctor[S, T, F[_, _]](using
      F: ForgetfulFunctor[F]
  ): Profunctor[[B, A] =>> Optic[S, T, A, B, F]] with
    def dimap[B, A, D, C](
        o: Optic[S, T, A, B, F]
    )(f: D => B)(g: A => C): Optic[S, T, C, D, F] = new Optic[S, T, C, D, F] {
      type X = o.X
      def to: S => F[X, C] = o.to.andThen(F.map(_)(g))
      def from: F[X, D] => T = o.from.compose(F.map(_)(f))
    }

  def id[A]: Optic[A, A, A, A, Forgetful] = new Optic[A, A, A, A, Forgetful] {
    type X = Nothing
    def to: A => A = identity
    def from: A => A = identity
  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F]) {
    def get[X](s: S): A = A.get(o.to(s))
  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using RA: ReverseAccessor[F]) {
    def reverseGet[X](b: B): T = o.from(RA.reverseGet(b))
  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F], RA: ReverseAccessor[F]) {
    def reverse = new Optic[B, A, T, S, F] {
      type X = o.X
      def to: B => F[X, T] = (b: B) => RA.reverseGet(o.from(RA.reverseGet(b)))

      def from: F[X, S] => A = (fs: F[X, S]) => A.get(o.to(A.get(fs)))
    }
  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F]) {
    def modify[X](f: A => B): S => T =
      o.to.andThen(FF.map(_)(f)).andThen(o.from)

    def replace[X](b: B): S => T =
      modify[X](_ => b)

  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Functor]) {
    def modifyF[G[_]](f: A => G[B])(using G: Functor[G]): S => G[T] =
      o.to.andThen(FT.traverse(G)(_)(f)).andThen(_.map(o.from))
  }

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Applicative]) {
    def modifyA[G[_]](f: A => G[B])(using G: Applicative[G]): S => G[T] =
      o.to.andThen(FT.traverse(G)(_)(f)).andThen(_.map(o.from))

    def all(s: S): List[F[o.X, A]] =
      FT.traverse(Applicative[List])(o.to(s))(List(_))
  }

}
