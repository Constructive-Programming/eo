package eo
package data

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.bifunctor._
import cats.syntax.either._
import cats.syntax.functor._

type Affine[A, B] = A match
  case (a1, a2) => Either[a1, (a2, B)]

object Affine:

  given map: ForgetfulFunctor[Affine] with

    def map[X, A, B]: Affine[X, A] => (A => B) => Affine[X, B] =
      (fa: Affine[X, A]) =>
        (f: A => B) =>
          fa match {
            case l: Left[_, _] => l.asInstanceOf[Affine[X, B]]
            case Right((x1, a)) =>
              (x1, f(a.asInstanceOf[A])).asRight.asInstanceOf[Affine[X, B]]
          }

  given traverse: ForgetfulTraverse[Affine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      (fa: Affine[X, A]) =>
        (f: A => G[B]) =>
          fa match {
            case l: Left[_, _] => l.asInstanceOf[Affine[X, B]].pure[G]
            case Right((x1, a)) =>
              f(a.asInstanceOf[A]).map(b => (x1, b).asRight.asInstanceOf[Affine[X, B]])
          }

  given assoc[X0, X1, X <: Tuple2[X0, X1], Y0, Y1, Y <: Tuple2[Y0, Y1]]
      : AssociativeFunctor[Affine, X, Y] with
    type Z = (Either[X0, (X1, Y0)], (X1, Y1))

    def associateLeft[S, A, C]: (S, S => Affine[X, A], A => Affine[Y, C]) => Affine[Z, C] =
      case (s, f, g) =>
        f(s).fold(
          _.asLeft[(X1, Y0)].asLeft[((X1, Y1), C)],
          {
            case (x1, a) =>
              g(a).bimap(c => (x1 -> c).asRight[X0], p => (x1, p._1) -> p._2)
          },
        )

    def associateRight[D, B, T]: (Affine[Z, D], Affine[Y, D] => B, Affine[X, B] => T) => T =
      case (Right(((x1, y1), d)), f, g)  => g(Right(x1 -> f(Right(y1 -> d))))
      case (Left(Right((x1, y0))), f, g) => g(Right(x1 -> f(Left(y0))))
      case (Left(Left(x0)), _, g)        => g(Left(x0))


  given tuple2affine: Composer[Tuple2, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (T, o.X)
        def to: S => Affine[X, A] = s => Right(o.to(s))

        def from: Affine[X, B] => T = {
          case Left(t)  => t
          case Right(p) => o.from(p)
        }

  given either2affine: Composer[Either, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (o.X, S)
        def to: S => Affine[X, A] = s => o.to(s).map(s -> _)
        def from: Affine[X, B] => T = xb => o.from(xb.map(_._2))
