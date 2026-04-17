package eo

import data.{Affine, Fst, Snd, Forget}

import cats.{Applicative, Functor, Traverse}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import eo.ForgetfulTraverse


trait ForgetfulTraverse[F[_, _], C[_[_]]]:
  def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]

object ForgetfulTraverse:
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with
    def traverse[X, A, B, G[_]: Functor]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  // Focus is on the second tuple component; the body only uses
  // `Functor[G]`, but Applicative extends Functor so the same
  // implementation satisfies the stricter bound. Having this
  // instance lets `Optic.modifyA[G]` apply to Tuple2-carrier optics
  // (lenses), matching the ergonomics of Affine / PowerSeries /
  // Forget[F] carriers that already ship `[Applicative]` instances.
  given tupleFTraverseApplicative: ForgetfulTraverse[Tuple2, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : Either[X, A] => (A => G[B]) => G[Either[X, B]] =
      fa => f => fa.fold(_.asLeft[B].pure[G], f.andThen(_.map(_.asRight[X])))

  given affineFTraverse: ForgetfulTraverse[Affine, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.affine.fold(
        l => Affine(l.asLeft[(Snd[X], B)]).pure[G],
        (x, a) => f(a).map(b => Affine((x, b).asRight[Fst[X]]))
      )

  given forgetFTraverse[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: F[A] => (A => G[B]) => G[F[B]] =
      Traverse[F].traverse[G, A, B]
