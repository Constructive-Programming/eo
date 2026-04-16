package eo
package optics

import data._

import cats.{Applicative, Functor, Monoid}
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
trait Optic[S, T, A, B, F[_, _]]:
  self =>
    import Function.const
    type X

    def to: S => F[X, A]
    def from: F[X, B] => T

    def andThenLeft[C](o: Optic[A, Nothing, C, Nothing, F])(using
      laf: LeftAssociativeFunctor[F, self.X, o.X]
    ): Optic[S, Nothing, C, Nothing, F] =
      new Optic:
        type X = laf.Z
        def to: S => F[X, C] = s => laf.associateLeft(s, self.to, o.to)
        def from: F[X, Nothing] => Nothing = ???

    def andThenRight[D](o: Optic[Nothing, B, Nothing, D, F])(using
      af: RightAssociativeFunctor[F, self.X, o.X]
    ): Optic[Nothing, T, Nothing, D, F] =
      new Optic:
        type X = af.Z
        def to: Nothing => F[X, Nothing] = ???
        def from: F[X, D] => T = xd => af.associateRight(xd, o.from, self.from)

    def andThen[C, D](o: Optic[A, B, C, D, F])(using
      af: AssociativeFunctor[F, self.X, o.X]
    ): Optic[S, T, C, D, F] =
      new Optic:
        type X = af.Z
        def to: S => F[X, C] = s => af.associateLeft(s, self.to, o.to)
        def from: F[X, D] => T = xd => af.associateRight(xd, o.from, self.from)

    def morphLeft[G[_, _]](using cf: LeftComposer[F, G]): Optic[S, Nothing, A, Nothing, G] =
      cf.to(self)

    def morphRight[G[_, _]](using cf: RightComposer[F, G]): Optic[Nothing, T, Nothing, B, G] =
      cf.to(self)

    def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
      cf.to(self)

object Optic:
  given outerProfunctor[A, B, F[_, _]]
      : Profunctor[[S, T] =>> Optic[S, T, A, B, F]] with
    def dimap[S, T, R, U](
        o: Optic[S, T, A, B, F]
    )(f: R => S)(g: T => U): Optic[R, U, A, B, F] =
      new Optic[R, U, A, B, F]:
        type X = o.X
        def to: R => F[X, A] = o.to.compose(f)
        def from: F[X, B] => U = o.from.andThen(g)

  given innerProfunctor[S, T, F[_, _]](using
      F: ForgetfulFunctor[F]
  ): Profunctor[[B, A] =>> Optic[S, T, A, B, F]] with
    def dimap[B, A, D, C](
        o: Optic[S, T, A, B, F]
    )(f: D => B)(g: A => C): Optic[S, T, C, D, F] =
      new Optic[S, T, C, D, F]:
        type X = o.X
        def to: S => F[X, C] = s => F.map(o.to(s), g)
        def from: F[X, D] => T = d => o.from(F.map(d, f))

  def id[A]: Optic[A, A, A, A, Forgetful] =
    new Optic[A, A, A, A, Forgetful]:
      type X = Nothing
      def to: A => A = identity
      def from: A => A = identity

  // ---- Generic Optic extensions -------------------------------------

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F])
    inline def get[X](s: S): A = A.get(o.to(s))

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using RA: ReverseAccessor[F])
    inline def reverseGet[X](b: B): T = o.from(RA.reverseGet(b))

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F], RA: ReverseAccessor[F])
    // Intentionally NOT `inline`: the body constructs a fresh `Optic`
    // anonymous class, and `inline` would duplicate that class definition
    // at every call site (E197 warning). Since the body already allocates
    // a new Optic, inlining wouldn't eliminate that allocation -- so we
    // keep the method as a normal `def` and avoid the bytecode bloat.
    def reverse: Optic[B, A, T, S, F] =
      new Optic[B, A, T, S, F]:
        type X = o.X
        def to: B => F[X, T] = (b: B) => RA.reverseGet(o.from(RA.reverseGet(b)))
        def from: F[X, S] => A = (fs: F[X, S]) => A.get(o.to(A.get(fs)))

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F])
    inline def modify[X](f: A => B): S => T =
      s => o.from(FF.map(o.to(s), f))
    inline def replace[X](b: B): S => T =
      s => o.from(FF.map(o.to(s), _ => b))

  extension [S, T, A, B, D, F[_, _]](
    o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F], ev: T => F[o.X, D])
    inline def transform(f: D => B): T => T =
      t => o.from(FF.map(ev(t), f))
    inline def place(b: B): T => T =
      transform(_ => b)
    inline def transfer[C](f: C => B): T => C => T =
      t => c => place(f(c))(t)

  extension [S, T, A, B, F[_, _]](
    o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulApplicative[F])
    inline def put(f: A => B): A => T =
      a => o.from(FF.pure[o.X, B](f(a)))

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Functor])
    inline def modifyF[G[_]](f: A => G[B])(using G: Functor[G]): S => G[T] =
      s => FT.traverse(using G)(o.to(s))(f).map(o.from)

  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Applicative])
    inline def modifyA[G[_]](f: A => G[B])(using G: Applicative[G]): S => G[T] =
      s => FT.traverse(using G)(o.to(s))(f).map(o.from)
    inline def all(s: S): List[F[o.X, A]] =
      FT.traverse(using Applicative[List])(o.to(s))(List(_))

  extension [S, T, A, B, F[_, _]](
    o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFold[F])
    inline def foldMap[M: Monoid](f: A => M): S => M =
      s => FF.foldMap(using Monoid[M])(f)(o.to(s))
