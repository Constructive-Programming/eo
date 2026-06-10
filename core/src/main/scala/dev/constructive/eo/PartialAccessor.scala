package dev.constructive.eo
package data

trait PartialAccessor[F[_, _]]:

  def getOption[X, A](fa: F[X, A]): Option[A]

object PartialAccessor:

  given eitherAccessor: PartialAccessor[Either] with
    def getOption[X, A](fa: Either[X, A]) = fa.toOption

  given affineAccessor: PartialAccessor[Affine] with
    def getOption[X, A](fa: Affine[X, A]): Option[A] = fa.fold(_ => None, (_, h) => Some(h))
