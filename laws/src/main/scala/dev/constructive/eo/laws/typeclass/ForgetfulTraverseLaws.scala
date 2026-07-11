package dev.constructive.eo.laws.typeclass

import cats.{Applicative, Id}
import dev.constructive.eo.forgetful.ForgetfulTraverse

/** Law equations for any `ForgetfulTraverse[F, Applicative]` — the identity law stated at `Id`:
  *
  *   - `traverse[Id](fa)(a => a) == fa` — the classical `traverse(pure) == pure ∘ _` identity,
  *     collapsed through `Id[X] = X`.
  *
  * The full `ForgetfulTraverse` family in core has two flavours: `[_ <: Applicative]` (Affine,
  * Forget[F], PowerSeries, Direct at Invariant) and `[_ <: Distributive]` (ModifyF). We expose the
  * `[Applicative]` flavour here because `Id`-identity is the widely applicable anchor law; the
  * Distributive variant collapses to a tautology at `Id`, so witnessing it adds no signal.
  *
  * The naturality and sequential-composition laws (the other two classical Traverse laws) are not
  * stated here — they require an applicative-transformation fixture, which in turn needs a second
  * concrete `Applicative` beyond `Id`.
  */
trait ForgetfulTraverseLaws[F[_, _], X, A]:

  /** `traverse[Id](fa)(a => a) == fa`. */
  def traverseIdentity(fa: F[X, A])(using
      FT: ForgetfulTraverse[F, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](fa, a => a: Id[A])(using Applicative[Id]) == fa
