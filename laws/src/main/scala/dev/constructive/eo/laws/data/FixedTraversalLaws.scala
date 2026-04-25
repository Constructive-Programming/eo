package dev.constructive.eo.laws.data

import dev.constructive.eo.ForgetfulFunctor
import dev.constructive.eo.data.FixedTraversal

/** Carrier-level laws for `FixedTraversal[N][X, A]` — a tuple-shaped carrier that places `N` copies
  * of `A` followed by a phantom `X` slot. Only `ForgetfulFunctor[FixedTraversal[N]]` is defined
  * (for `N = 2, 3, 4`); traverse and associative-functor instances are deliberately absent because
  * the fixed-arity carrier does not compose under `andThen`.
  *
  * Stating the laws in a discipline trait keeps downstream derivations honest: if
  * `FixedTraversal[5]` is added later, the same rule set witnesses it.
  */
trait FixedTraversalLaws[N <: Int, X, A]:

  def functorIdentity(fa: FixedTraversal[N][X, A])(using
      FF: ForgetfulFunctor[FixedTraversal[N]]
  ): Boolean =
    FF.map(fa, identity[A]) == fa

  def functorComposition(
      fa: FixedTraversal[N][X, A],
      f: A => A,
      g: A => A,
  )(using FF: ForgetfulFunctor[FixedTraversal[N]]): Boolean =
    FF.map(FF.map(fa, f), g) == FF.map(fa, f.andThen(g))
