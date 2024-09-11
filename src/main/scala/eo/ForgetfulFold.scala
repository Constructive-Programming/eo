package eo

import cats.{Monoid, Foldable}
import data.{Affine, Forget}

trait ForgetfulFold[F[_, _]]:
  def foldMap[X, A, M: Monoid]: (A => M) => F[X, A] => M

object ForgetfulFold:
  given tupleFFold: ForgetfulFold[Tuple2] with
    def foldMap[X, A, M: Monoid]: (A => M) => ((X, A)) => M =
      f => fa => f(fa._2)

  given eitherFFold: ForgetfulFold[Either] with
    def foldMap[X, A, M: Monoid]: (A => M) => Either[X, A] => M =
      f => ea => ea.fold(_ => Monoid[M].empty, f)

  given affineFFold: ForgetfulFold[Affine] with
    def foldMap[X, A, M: Monoid]: (A => M) => Affine[X, A] => M =
      f =>
        case Left(_) => Monoid[M].empty
        case Right((_, a: A)) => f(a)

  given foldFFold[F[_]: Foldable]: ForgetfulFold[Forget[F]] with
    def foldMap[X, A, M: Monoid]: (A => M) => F[A] => M =
      f => fa => Foldable[F].foldMap(fa)(f)
