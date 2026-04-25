package dev.constructive.eo

import cats.Monoid
import data.Affine

/** `foldMap` over the focus of a two-parameter carrier — the mechanism behind `Optic.foldMap`
  * across every optic family. Miss / absent branches contribute `Monoid[M].empty`; hit branches run
  * `f` and fold the result.
  *
  * @tparam F
  *   the carrier
  */
trait ForgetfulFold[F[_, _]]:
  /** Combine every focus through `Monoid[M]` after running `f`.
    *
    * @tparam X
    *   existential leftover (unused by the fold — `Monoid.empty` on miss, `f(focus)` on hit)
    * @tparam A
    *   focus being folded
    * @tparam M
    *   monoid the focus is folded into
    */
  def foldMap[X, A, M: Monoid]: (A => M) => F[X, A] => M

/** Typeclass instances for [[ForgetfulFold]]. */
object ForgetfulFold:

  /** `Tuple2` foldMap — runs `f` on the focus, ignores the leftover.
    *
    * @group Instances
    */
  given tupleFFold: ForgetfulFold[Tuple2] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, A)) => M =
      f => fa => f(fa._2)

  /** `Either` foldMap — `Left` contributes `Monoid.empty`, `Right` runs `f`.
    *
    * @group Instances
    */
  given eitherFFold: ForgetfulFold[Either] with

    def foldMap[X, A, M: Monoid]: (A => M) => Either[X, A] => M =
      f => ea => ea.fold(_ => Monoid[M].empty, f)

  /** `Affine` foldMap — miss branch empty, hit branch runs `f` on the focus.
    *
    * @group Instances
    */
  given affineFFold: ForgetfulFold[Affine] with

    def foldMap[X, A, M: Monoid]: (A => M) => Affine[X, A] => M =
      f =>
        fa =>
          fa match
            case _: Affine.Miss[X, A] => Monoid[M].empty
            case h: Affine.Hit[X, A]  => f(h.b)
