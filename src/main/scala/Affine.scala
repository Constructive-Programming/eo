package eo

import cats.syntax.bifunctor._
import cats.syntax.either._

type Affine[A, B] = A match
  case (a1, a2) => Either[a1, (a2, B)]

object Affine {
  given assoc[X0, X1, X <: Tuple2[X0, X1], Y0, Y1, Y <: Tuple2[Y0, Y1]]
      : AssociativeFunctor[Affine, X, Y] with
    type Z = (Either[X0, (X1, Y0)], (X1, Y1))

    def associateLeft[S, A, C]
        : (S, S => Affine[X, A], A => Affine[Y, C]) => Affine[Z, C] =
      case (s, f, g) =>
        f(s).fold(
          _.asLeft[(X1, Y0)].asLeft[((X1, Y1), C)],
          { case (x1, a) =>
            g(a).bimap(c => (x1 -> c).asRight[X0], p => (x1, p._1) -> p._2)
          }
        )

    def associateRight[D, B, T]
        : (Affine[Z, D], Affine[Y, D] => B, Affine[X, B] => T) => T =
      case (Right(((x1, y1), d)), f, g)  => g(Right(x1 -> f(Right(y1 -> d))))
      case (Left(Right((x1, y0))), f, g) => g(Right(x1 -> f(Left(y0))))
      case (Left(Left(x0)), _, g)        => g(Left(x0))

}
