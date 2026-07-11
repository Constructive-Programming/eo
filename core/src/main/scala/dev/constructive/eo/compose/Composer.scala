package dev.constructive.eo
package compose

import kyo.Result

import kernel.{Applicative, Foldable}
import optics.Optic

trait Composer[F[_, _], G[_, _]]:
  def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G]

object Composer extends LowPriorityComposerInstances:

  import data.{Direct, Forget, MultiFocusK}

  given direct2tuple: Composer[Direct, Tuple2] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Tuple2] =
      new Optic[S, T, A, B, Tuple2]:
        type X = Unit
        def to(s: S): (X, A) = ((), o.to(s).value)
        def from(pair: (X, B)): T = o.from(Direct(pair._2))

  given direct2result: Composer[Direct, Result] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Result] =
      new Optic[S, T, A, B, Result]:
        type X = Nothing
        def to(s: S): Result[X, A] = Result.succeed(o.to(s).value)
        def from(e: Result[X, B]): T =
          // `X = Nothing` makes the failure branch uninhabited; the `Nothing` value conforms
          // to `T`, keeping the fold total and compiler-verified — no `???` needed.
          e.foldOrThrow(b => o.from(Direct(b)), (x: Nothing) => x)

  given direct2forget[F[_]](using
      F: Applicative[F],
      FF: Foldable[F],
  ): Composer[Direct, Forget[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, Forget[F]] =
      new Optic[S, T, A, B, Forget[F]]:
        type X = Nothing
        def to(s: S): Forget[F][X, A] = Forget(F.pure(o.to(s).value))
        def from(fb: Forget[F][X, B]): T =
          o.from(Direct(MultiFocusK.pickSingletonOrThrow[F, B](fb.value, "Direct")))

trait LowPriorityComposerInstances:

  given chainViaTuple2[F[_, _], G[_, _]](using
      f2tuple: Composer[F, Tuple2],
      tuple2g: Composer[Tuple2, G],
  ): Composer[F, G] with

    def to[S, T, A, B](o: Optic[S, T, A, B, F]): Optic[S, T, A, B, G] =
      tuple2g.to(f2tuple.to(o))
