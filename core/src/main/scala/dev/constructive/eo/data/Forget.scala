package dev.constructive.eo
package data

import kernel.{Applicative, FlatMap, Foldable, Functor, Monoid, Traverse}
import forgetful.*
import compose.*
import optics.Optic

// `[X, A] =>> F[A]` — the `X` is phantom. The uncurried [[ForgetK]] is `opaque` for the same
// reason [[Direct]] is: a transparent alias dealiases to a bare type lambda, which has no
// companion in implicit scope, so `ForgetfulFold[Forget[F]]`, `AssociativeFunctor[Forget[F], …]`,
// etc. were invisible without an explicit `import data.Forget.given`. With the opaque anchor
// inside, dealiasing `Forget[F][X, A]` stops at `ForgetK[F, X, A]`, whose companion (where the
// instances live) *is* an implicit-scope anchor — no import needed. (`Forget` itself must stay a
// plain alias: an opaque type cannot have the curried `[F[_]] => [X, A] =>> …` shape.) Within this
// file the opaque is transparent; outside, [[ForgetK.apply]] / [[ForgetK.value]] are the
// (runtime-identity) boundary.
opaque type ForgetK[F[_], X, A] = F[A]

type Forget[F[_]] = [X, A] =>> ForgetK[F, X, A]

object ForgetK:

  transparent inline def apply[F[_], X, A](fa: F[A]): ForgetK[F, X, A] = fa

  extension [F[_], X, A](self: ForgetK[F, X, A]) transparent inline def value: F[A] = self

  trait ForgetPull[F[_]]:

    def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B]

  object ForgetPull:

    given monadicPull[F[_]: Applicative]: ForgetPull[F] with

      def redistribute[D, B](xd: F[D])(fromInner: F[D] => B): F[B] =
        Applicative[F].pure(fromInner(xd))

  private[data] def assocFor[F[_], Xo, Xi](
      FM: FlatMap[F],
      pull: ForgetPull[F],
  ): AssociativeFunctor[Forget[F], Xo, Xi] =
    new AssociativeFunctor[Forget[F], Xo, Xi]:
      type Z = Nothing

      def composeTo[S, T, A, B, C, D](
          s: S,
          outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
          inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
      ): F[C] = FM.flatMap(outer.to(s))(inner.to)

      def composeFrom[S, T, A, B, C, D](
          xd: F[D],
          inner: Optic[A, B, C, D, Forget[F]] { type X = Xi },
          outer: Optic[S, T, A, B, Forget[F]] { type X = Xo },
      ): T = outer.from(pull.redistribute(xd)(inner.from))

  given forgetFFunctor[F[_]](using F: Functor[F]): ForgetfulFunctor[Forget[F]] with
    def map[X, A, B](fa: Forget[F][X, A], f: A => B): Forget[F][X, B] = F.map(fa)(f)

  given forgetFApplicative[F[_]: Applicative]: ForgetfulApplicative[Forget[F]] with
    def map[X, A, B](fa: Forget[F][X, A], f: A => B): Forget[F][X, B] = Applicative[F].map(fa)(f)
    def pure[X, A](a: A): Forget[F][X, A] = Applicative[F].pure(a)

  given forgetFFold[F[_]: Foldable]: ForgetfulFold[Forget[F]] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: Forget[F][X, A]): M =
      Foldable[F].foldMap(fa)(f)

  given forgetFTraverse[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative](
        fa: Forget[F][X, A],
        f: A => G[B],
    ): G[Forget[F][X, B]] =
      Traverse[F].traverse(fa)(f)

  given assocForgetMonad[F[_]: Applicative: FlatMap, Xo, Xi]
      : AssociativeFunctor[Forget[F], Xo, Xi] =
    assocFor[F, Xo, Xi](FlatMap[F], ForgetPull.monadicPull[F])

object Forget:
  export ForgetK.{given, *}
