package dev.constructive.eo

import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.{Applicative, Functor}

import data.Affine

/** Traverse the focus of `F[_, _]` under an effectful `A => G[B]`. Parameterised by the applicative
  * constraint `C[_[_]]` — `Applicative` for carriers with miss branches, `Functor` for Tuple2,
  * `Distributive` for SetterF.
  *
  * @tparam F
  *   the carrier
  * @tparam C
  *   constraint on `G`
  */
trait ForgetfulTraverse[F[_, _], C[_[_]]]:
  def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]

/** Typeclass instances for [[ForgetfulTraverse]]. */
object ForgetfulTraverse:

  /** `Tuple2` under `Functor[G]` — cheapest Lens path. @group Instances */
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with

    def traverse[X, A, B, G[_]: Functor]: ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  /** `Tuple2` under `Applicative[G]` — same body, stricter bound (Applicative ext Functor) so
    * `Optic.modifyA[G]` applies uniformly across carriers.
    *
    * @group Instances
    */
  given tupleFTraverseApplicative: ForgetfulTraverse[Tuple2, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  /** `Either` — `Left` passes through via `.pure`. @group Instances */
  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Either[X, A] => (A => G[B]) => G[Either[X, B]] =
      fa =>
        f =>
          fa.fold(
            x => x.asLeft[B].pure[G],
            a => f(a).map(_.asRight[X]),
          )

  /** `Affine` — miss via `pure`, hit through `f`. @group Instances */
  given affineFTraverse: ForgetfulTraverse[Affine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.aTraverse(f)
