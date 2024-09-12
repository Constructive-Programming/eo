package eo

import cats.syntax.either._

trait LeftAssociativeFunctor[F[_, _], X, Y]:
  type Z
  def associateLeft[S, A, C]: (S, S => F[X, A], A => F[Y, C]) => F[Z, C]

trait RightAssociativeFunctor[F[_, _], X, Y]:
  type Z
  def associateRight[D, B, T]: (F[Z, D], F[Y, D] => B, F[X, B] => T) => T

trait AssociativeFunctor[F[_, _], X, Y] extends LeftAssociativeFunctor[F, X, Y], RightAssociativeFunctor[F, X, Y]

object AssociativeFunctor:

  given tupleAssocF[X, Y]: AssociativeFunctor[Tuple2, X, Y] with
    type Z = (X, Y)

    def associateLeft[S, A, C]: (S, S => (X, A), A => (Y, C)) => (Z, C) =
      case (s, f, g) =>
        val (x, a) = f(s)
        val (y, c) = g(a)
        (x -> y, c)

    def associateRight[D, B, T]: ((Z, D), ((Y, D)) => B, ((X, B)) => T) => T =
      case (((x, y), d), g, f) => f(x, g(y, d))

  given eitherAssocF[X, Y]: AssociativeFunctor[Either, X, Y] with
    type Z = Either[X, Y]

    def associateLeft[S, A, C]: (S, S => Either[X, A], A => Either[Y, C]) => Either[Z, C] =
      case (s, f, g) =>
        f(s).fold(_.asLeft[Y].asLeft[C], g(_).leftMap(_.asRight[X]))

    def associateRight[D, B, T]: (Either[Z, D], Either[Y, D] => B, Either[X, B] => T) => T =
      case (Right(d), f, g)       => g(Right(f(Right(d))))
      case (Left(Right(y)), f, g) => g(Right(f(Left(y))))
      case (Left(Left(x)), _, g)  => g(Left(x))
