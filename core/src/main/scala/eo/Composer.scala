package eo

import optics.Optic

trait Composer[F[_, _], G[_, _]]:
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]

object Composer:

  import data.Forgetful

  given chain[F[_, _], G[_, _], H[_, _]](using
      f2g: Composer[F, G],
      g2h: Composer[G, H],
  ): Composer[F, H] with
    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, H] = g2h.to(f2g.to(o))

  given forgetful2tuple: Composer[Forgetful, Tuple2] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Tuple2] =
      new Optic[S, T, A, B, Tuple2]:
        type X = Unit
        def to: S => (X, A) = o.to.andThen(() -> _)
        def from: ((X, B)) => T = o.from.compose(_._2)

  given forgetful2either: Composer[Forgetful, Either] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, Either] =
      new Optic[S, T, A, B, Either]:
        type X = Nothing
        def to: S => Either[X, A] = o.to.andThen(Right(_))
        def from: Either[X, B] => T = o.from.compose(_.getOrElse(???))
