package eo.laws.typeclass

import eo.ForgetfulTraverse

import cats.{Applicative, Id}

/** Law equations for any `ForgetfulTraverse[F, Applicative]` — the identity law stated at `Id`:
  *
  * * `traverse[Id](pure) == pure ∘ traverse[Id]` — which collapses to `traverse[Id](fa)(a => a:
  * Id[A]) == fa` because `Id[X] = X`.
  *
  * The full `ForgetfulTraverse` family in core has two flavours: `[_ <: Applicative]` (Affine,
  * Forget[F], PowerSeries, Forgetful at Invariant) and `[_ <: Distributive]` (SetterF). We expose
  * the `[Applicative]` flavour here because `Id`-identity is the widely applicable anchor law; the
  * Distributive variant collapses to a tautology at `Id`, so witnessing it adds no signal.
  *
  * The naturality and sequential-composition laws (the other two classical Traverse laws) are
  * deferred to 0.1.1 — they require an applicative-transformation fixture, which in turn needs a
  * second concrete `Applicative` beyond `Id`. Out of scope for this plan.
  */
trait ForgetfulTraverseLaws[F[_, _], X, A]:

  def traverseIdentity(fa: F[X, A])(using
      FT: ForgetfulTraverse[F, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](using Applicative[Id])(fa)(a => a: Id[A]) == fa
