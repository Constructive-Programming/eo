package eo

import data.{Affine, Fst, Snd, Forget}

import cats.{Applicative, Functor, Traverse}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import eo.ForgetfulTraverse


/** Traverse the focus of a two-parameter carrier `F[_, _]` under
  * a shape-preserving effectful transform `A => G[B]`.
  *
  * Parameterised by the applicative constraint `C[_[_]]` the
  * traversal requires — some carriers need the full `Applicative`
  * (to build a pure `G[F[X, B]]` on the miss branch), some need
  * only `Functor`, and [[data.SetterF]] needs the weaker
  * `Distributive`.
  *
  * @tparam F the carrier
  * @tparam C constraint that `G` must satisfy
  */
trait ForgetfulTraverse[F[_, _], C[_[_]]]:
  /** Apply `A => G[B]` at the focus and reassemble the carrier. */
  def traverse[X, A, B, G[_]: C]: F[X, A] => (A => G[B]) => G[F[X, B]]

/** Typeclass instances for [[ForgetfulTraverse]]. */
object ForgetfulTraverse:

  /** `Tuple2` traverse that only needs `Functor[G]` — the cheapest
    * path for Lens-carrier optics.
    *
    * @group Instances */
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with
    def traverse[X, A, B, G[_]: Functor]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  /** `Tuple2` traverse under the stricter `Applicative[G]` bound —
    * lets `Optic.modifyA[G]` apply uniformly to Lens / Affine /
    * PowerSeries / Forget[F] carriers.
    *
    * Focus is on the second tuple component; the body only uses
    * `Functor[G]`, but Applicative extends Functor so the same
    * implementation satisfies the stricter bound.
    *
    * @group Instances */
  given tupleFTraverseApplicative: ForgetfulTraverse[Tuple2, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : ((X, A)) => (A => G[B]) => G[(X, B)] =
      fa => f => f(fa._2).map(fa._1 -> _)

  /** `Either` traverse — passes `Left` through via `.pure` and
    * maps `Right` through `f`.
    *
    * @group Instances */
  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]
        : Either[X, A] => (A => G[B]) => G[Either[X, B]] =
      fa => f => fa.fold(_.asLeft[B].pure[G], f.andThen(_.map(_.asRight[X])))

  /** `Affine` traverse — miss branch lifted via `pure`, hit branch
    * run through `f` and rebuilt. Unlocks `.modifyA` on every
    * Optional-carrier optic.
    *
    * @group Instances */
  given affineFTraverse: ForgetfulTraverse[Affine, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.affine.fold(
        l => Affine(l.asLeft[(Snd[X], B)]).pure[G],
        (x, a) => f(a).map(b => Affine((x, b).asRight[Fst[X]]))
      )

  /** `Forget[F]` traverse — delegates to the underlying
    * `Traverse[F]`. The core of `Traversal.each` and `Fold` in
    * their effectful forms.
    *
    * @group Instances */
  given forgetFTraverse[F[_]: Traverse]: ForgetfulTraverse[Forget[F], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: F[A] => (A => G[B]) => G[F[B]] =
      Traverse[F].traverse[G, A, B]
