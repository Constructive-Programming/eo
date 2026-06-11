package dev.constructive.eo
package forgetful

import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.{Applicative, Functor}

/** Traverse the focus of `F[_, _]` under an effectful `A => G[B]`. Parameterised by the applicative
  * constraint `C[_[_]]` — `Applicative` for carriers with miss branches, `Functor` for Tuple2,
  * `Distributive` for ModifyF.
  *
  * @tparam F
  *   the carrier
  * @tparam C
  *   constraint on `G`
  */
trait ForgetfulTraverse[F[_, _], C[_[_]]]:
  def traverse[X, A, B, G[_]: C](fa: F[X, A], f: A => G[B]): G[F[X, B]]

/** Typeclass instances for [[ForgetfulTraverse]]. */
object ForgetfulTraverse:

  /** `Tuple2` under `Functor[G]` — cheapest Lens path. @group Instances */
  given tupleFTraverse: ForgetfulTraverse[Tuple2, Functor] with

    def traverse[X, A, B, G[_]: Functor](fa: (X, A), f: A => G[B]): G[(X, B)] =
      f(fa._2).map(fa._1 -> _)

  /** `Tuple2` under `Applicative[G]` — same body, stricter bound (Applicative ext Functor) so
    * `Optic.modifyA[G]` applies uniformly across carriers.
    *
    * @group Instances
    */
  given tupleFTraverseApplicative: ForgetfulTraverse[Tuple2, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: (X, A), f: A => G[B]): G[(X, B)] =
      f(fa._2).map(fa._1 -> _)

  /** `Either` — `Left` passes through via `.pure`. @group Instances */
  given eitherFTraverse: ForgetfulTraverse[Either, Applicative] with

    def traverse[X, A, B, G[_]: Applicative](fa: Either[X, A], f: A => G[B]): G[Either[X, B]] =
      fa.fold(
        x => x.asLeft[B].pure[G],
        a => f(a).map(_.asRight[X]),
      )

  // `ForgetfulTraverse[Affine]` is NOT here — it is carrier-owned (`Affine.traverse`), matching
  // Direct / ModifyF / MultiFocus / Forget. Only the stdlib carriers (Tuple2, Either) live in this
  // companion, since their own companions can't be extended. A duplicate here would shadow-tie the
  // carrier-side instance and force every call site to disambiguate via `import data.Affine.given`.
