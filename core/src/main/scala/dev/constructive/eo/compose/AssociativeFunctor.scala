package dev.constructive.eo
package compose

import kyo.Result

import optics.Optic

trait AssociativeFunctor[F[_, _], Xo, Xi]:
  type Z

  def composeTo[S, T, A, B, C, D](
      s: S,
      outer: Optic[S, T, A, B, F] { type X = Xo },
      inner: Optic[A, B, C, D, F] { type X = Xi },
  ): F[Z, C]

  def composeFrom[S, T, A, B, C, D](
      xd: F[Z, D],
      inner: Optic[A, B, C, D, F] { type X = Xi },
      outer: Optic[S, T, A, B, F] { type X = Xo },
  ): T

object AssociativeFunctor:

  given tupleAssocF[Xo, Xi]: AssociativeFunctor[Tuple2, Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Tuple2] { type X = Xo },
        inner: Optic[A, B, C, D, Tuple2] { type X = Xi },
    ): (Z, C) =
      val (x, a) = outer.to(s)
      val (y, c) = inner.to(a)
      (x -> y, c)

    def composeFrom[S, T, A, B, C, D](
        xd: (Z, D),
        inner: Optic[A, B, C, D, Tuple2] { type X = Xi },
        outer: Optic[S, T, A, B, Tuple2] { type X = Xo },
    ): T =
      val ((x, y), d) = xd
      outer.from(x, inner.from(y, d))

  given resultAssocF[Xo, Xi]: AssociativeFunctor[Result, Xo, Xi] with
    type Z = Result[Xo, Xi]

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Result] { type X = Xo },
        inner: Optic[A, B, C, D, Result] { type X = Xi },
    ): Result[Z, C] =
      outer
        .to(s)
        .foldError(
          a => inner.to(a).mapFailure(xi => Result.succeed[Xo, Xi](xi)),
          // the outer Error[Xo] IS a Result[Xo, Xi] (success-free), so it can sit in the
          // failure slot unchanged — mirror of the cats `Left(Left(x))` shape without rewrap
          err => Result.fail[Z, C](err),
        )

    def composeFrom[S, T, A, B, C, D](
        xd: Result[Z, D],
        inner: Optic[A, B, C, D, Result] { type X = Xi },
        outer: Optic[S, T, A, B, Result] { type X = Xo },
    ): T =
      xd.fold(
        d => outer.from(Result.succeed(inner.from(Result.succeed(d)))),
        z =>
          z.foldError(
            xi => outer.from(Result.succeed(inner.from(Result.fail(xi)))),
            errXo => outer.from(errXo),
          ),
        thr => throw thr,
      )
