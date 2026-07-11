package dev.constructive.eo
package data

import kernel.{Invariant, Monoid}
import accessor.*
import forgetful.*
import compose.*
import optics.Optic

opaque type Direct[X, A] = A

object Direct:

  transparent inline def apply[X, A](a: A): Direct[X, A] = a

  extension [X, A](d: Direct[X, A]) transparent inline def value: A = d

  given accessor: Accessor[Direct] with

    def get[X, A](fa: Direct[X, A]): A = fa

  given reverseAccessor: ReverseAccessor[Direct] with

    def reverseGet[X, A](a: A): Direct[X, A] = a

  given fold: ForgetfulFold[Direct] with

    def foldMap[X, A, M: Monoid](f: A => M, fa: Direct[X, A]): M = f(fa)

  given applicative: ForgetfulApplicative[Direct] with

    def map[X, A, B](fa: Direct[X, A], f: A => B): Direct[X, B] =
      f(fa)

    def pure[X, A](a: A): Direct[X, A] = a

  given traverse: ForgetfulTraverse[Direct, Invariant] with

    def traverse[X, A, B, G[_]: Invariant](fa: Direct[X, A], f: A => G[B]): G[Direct[X, B]] =
      f(fa)

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
