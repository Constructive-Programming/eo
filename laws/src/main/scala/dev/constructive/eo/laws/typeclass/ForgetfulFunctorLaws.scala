package dev.constructive.eo.laws.typeclass

import dev.constructive.eo.ForgetfulFunctor

/** Law equations for any `ForgetfulFunctor[F]` instance — the usual two functor laws, stated in
  * carrier-carrier form:
  *
  * * `map(id) == id` * `map(g) ∘ map(f) == map(f andThen g)`
  *
  * Holds for every carrier EO uses: `Tuple2`, `Either`, `Affine`, `SetterF`, `Forgetful`,
  * `Forget[F]`, `MultiFocus[F]`. The law trait is parameterised so downstream adding a new
  * carrier can witness its `ForgetfulFunctor` instance here.
  *
  * Equality is structural — if the carrier wraps a function (as `SetterF` does),
  * discipline-checking this law requires an extensional comparison. See
  * [[dev.constructive.eo.laws.data.SetterFLaws]] for that carrier-specific phrasing.
  */
trait ForgetfulFunctorLaws[F[_, _], X, A]:

  def functorIdentity(fa: F[X, A])(using FF: ForgetfulFunctor[F]): Boolean =
    FF.map(fa, identity[A]) == fa

  def functorComposition(
      fa: F[X, A],
      f: A => A,
      g: A => A,
  )(using FF: ForgetfulFunctor[F]): Boolean =
    FF.map(FF.map(fa, f), g) == FF.map(fa, f.andThen(g))
