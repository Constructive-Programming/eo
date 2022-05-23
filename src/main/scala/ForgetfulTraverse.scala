package eo

import cats.{Applicative, Functor, Traverse}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._

trait ForgetfulTraverse[F[_, _], C[_[_]]] extends ForgetfulFunctor[F] {
  def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]
}

object ForgetfulTraverse {
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with
    def traverse[X, A, B, G[_]: Functor]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
       fa => f => f(fa._2).map(fa._1 -> _)
    def map[X, A, B]: ((X, A)) => (A => B) => (X, B) =
       fa => f => fa._1 -> f(fa._2)

  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : Either[X, A] => (A => G[B]) => G[Either[X, B]] =
        (fa: Either[X, A]) =>
          (f: A => G[B]) =>
            fa.fold(_.asLeft[B].pure[G], f.andThen(_.map(_.asRight[X])))
    def map[X, A, B]: Either[X, A] => (A => B) => Either[X, B] =
      fa => f => fa.map(f)

  given affineFTraverse: ForgetfulTraverse[Affine, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
        (fa: Affine[X, A]) =>
          (f: A => G[B]) =>
            fa match {
              case l: Left[_, _] => l.asInstanceOf[Affine[X, B]].pure[G]
              case Right((x1, a)) =>
                f(a.asInstanceOf[A]).map(b =>
                  (x1, b).asRight.asInstanceOf[Affine[X, B]]
                )
          }
    def map[X, A, B]: Affine[X, A] => (A => B) => Affine[X, B] =
        (fa: Affine[X, A]) =>
          (f: A => B) =>
            fa match {
              case l: Left[_, _] => l.asInstanceOf[Affine[X, B]]
              case Right((x1, a)) =>
                (x1, f(a.asInstanceOf[A])).asRight.asInstanceOf[Affine[X, B]]
          }

}
