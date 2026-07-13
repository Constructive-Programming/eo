package dev.constructive.eo
package compose

import kernel.Monoid
import forgetful.ForgetfulFold
import accessor.{Accessor, PartialAccessor}
import optics.{ForgetFold, Getter, Optic, PickFold}

trait ReadCompose[F[_, _], G[_, _]]:
  type Out[_, _]

  def compose[S, T, A, B, V, C, D](
      outer: Optic[S, T, A, B, F],
      inner: Optic[A, V, C, D, G],
  ): Out[S, C]

object ReadCompose extends LowPriorityReadCompose:

  given totalTotal[F[_, _], G[_, _]](using
      AO: Accessor[F],
      AI: Accessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = Getter[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): Getter[S, C] =
      Getter(s => AI.get(inner.to(AO.get(outer.to(s)))))

  given totalPartial[F[_, _], G[_, _]](using
      AO: Accessor[F],
      PI: PartialAccessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PI.getOption(inner.to(AO.get(outer.to(s)))))

  given partialTotal[F[_, _], G[_, _]](using
      PO: PartialAccessor[F],
      AI: Accessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PO.getOption(outer.to(s)).map(a => AI.get(inner.to(a))))

  given partialPartial[F[_, _], G[_, _]](using
      PO: PartialAccessor[F],
      PI: PartialAccessor[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = PickFold[S, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): PickFold[S, C] =
      PickFold(s => PO.getOption(outer.to(s)).flatMap(a => PI.getOption(inner.to(a))))

trait LowPriorityReadCompose:

  given foldFold[F[_, _], G[_, _]](using
      FF: ForgetfulFold[F],
      FG: ForgetfulFold[G],
  ): ReadCompose[F, G] with
    type Out[S, C] = ForgetFold[S, List, C]

    def compose[S, T, A, B, V, C, D](
        outer: Optic[S, T, A, B, F],
        inner: Optic[A, V, C, D, G],
    ): ForgetFold[S, List, C] =
      new ForgetFold[S, List, C](s =>
        FF.foldMap(
          (a: A) => FG.foldMap((c: C) => c :: Nil, inner.to(a))(using Monoid[List[C]]),
          outer.to(s),
        )(using Monoid[List[C]])
      )
