package eo

import cats.{Applicative, Functor}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._

trait ForgetfulTraverse[F[_, _], C[_[_]]] {
  def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]
}

object ForgetfulTraverse {
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with
    def traverse[X, A, B, G[_]: Functor]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
       fa => f => f(fa._2).map(fa._1 -> _)

  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : Either[X, A] => (A => G[B]) => G[Either[X, B]] =
        (fa: Either[X, A]) =>
          (f: A => G[B]) =>
            fa.fold(_.asLeft[B].pure[G], f.andThen(_.map(_.asRight[X])))


}
