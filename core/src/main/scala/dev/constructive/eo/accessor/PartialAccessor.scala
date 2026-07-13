package dev.constructive.eo
package accessor

import kyo.Maybe.{Absent, Present}
import kyo.{Maybe, Result}

import data.Affine

trait PartialAccessor[F[_, _]]:

  def getOption[X, A](fa: F[X, A]): Maybe[A]

object PartialAccessor:

  given resultAccessor: PartialAccessor[Result] with
    def getOption[X, A](fa: Result[X, A]): Maybe[A] = fa.toMaybe

  given affineAccessor: PartialAccessor[Affine] with
    def getOption[X, A](fa: Affine[X, A]): Maybe[A] = fa.fold(_ => Absent, (_, h) => Present(h))
