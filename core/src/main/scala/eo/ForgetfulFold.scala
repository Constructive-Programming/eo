package eo

import cats.{Monoid, Foldable}
import data.{Affine, Forget}

/** `foldMap` over the focus of a two-parameter carrier — the
  * mechanism behind `Optic.foldMap` across every optic family.
  * Miss / absent branches contribute `Monoid[M].empty`; hit
  * branches run `f` and fold the result.
  *
  * @tparam F the carrier
  */
trait ForgetfulFold[F[_, _]]:
  /** Combine every focus through `Monoid[M]` after running `f`. */
  def foldMap[X, A, M: Monoid]: (A => M) => F[X, A] => M

/** Typeclass instances for [[ForgetfulFold]]. */
object ForgetfulFold:

  /** `Tuple2` foldMap — runs `f` on the focus, ignores the
    * leftover.
    *
    * @group Instances */
  given tupleFFold: ForgetfulFold[Tuple2] with
    def foldMap[X, A, M: Monoid]: (A => M) => ((X, A)) => M =
      f => fa => f(fa._2)

  /** `Either` foldMap — `Left` contributes `Monoid.empty`, `Right`
    * runs `f`.
    *
    * @group Instances */
  given eitherFFold: ForgetfulFold[Either] with
    def foldMap[X, A, M: Monoid]: (A => M) => Either[X, A] => M =
      f => ea => ea.fold(_ => Monoid[M].empty, f)

  /** `Affine` foldMap — miss branch empty, hit branch runs `f`
    * on the focus.
    *
    * @group Instances */
  given affineFFold: ForgetfulFold[Affine] with
    def foldMap[X, A, M: Monoid]: (A => M) => Affine[X, A] => M =
      f => fa => fa.affine.fold(_ => Monoid[M].empty, p => f(p._2))

  /** `Forget[F]` foldMap — delegates to the underlying
    * `Foldable[F]`. Powers `Fold.apply[F, A]` and
    * `Traversal.each.foldMap`.
    *
    * @group Instances */
  given forgetFFold[F[_]: Foldable]: ForgetfulFold[Forget[F]] with
    def foldMap[X, A, M: Monoid]: (A => M) => F[A] => M =
      f => fa => Foldable[F].foldMap(fa)(f)
