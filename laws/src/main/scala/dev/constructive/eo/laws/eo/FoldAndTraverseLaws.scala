package dev.constructive.eo
package laws
package eo

import dev.constructive.eo.forgetful.*

import optics.Optic
import optics.Optic.*

// Fold family — the laws below pin down EO's `foldMap` and `all` on
// every carrier with a `ForgetfulFold` instance (Lens, Prism, Optional,
// Traversal, Fold, …).
//
//   E1 — `foldMap` is a Monoid homomorphism.
//   E2 — `all` is `foldMap(List(_))`: every focus, in traversal order
//        (the raw-optic spelling of `CanFold.foci`).

/** E1 — `optic.foldMap` is a Monoid homomorphism on the target monoid (tested at `Int` with
  * additive `Monoid[Int]`, which is enough to witness the law); E2 — `optic.all` is the fold into
  * the free monoid, `foldMap(List(_))`.
  */
trait FoldMapHomomorphismLaws[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]

  /** `foldMap(f ⊕ g) == foldMap(f) ⊕ foldMap(g)` — tested at additive `Int`. */
  def foldMapHomomorphism(s: S, f: A => Int, g: A => Int)(using
      ForgetfulFold[F]
  ): Boolean =
    optic.foldMap(a => f(a) + g(a))(s) ==
      optic.foldMap(f)(s) + optic.foldMap(g)(s)

  /** `foldMap(const(mempty)) == mempty`. */
  def foldMapEmpty(s: S)(using ForgetfulFold[F]): Boolean =
    optic.foldMap[Int](_ => 0)(s) == 0

  /** E2 — `all(s) == foldMap(List(_))(s)`. */
  def allIsFoci(s: S)(using ForgetfulFold[F]): Boolean =
    optic.all(s) == optic.foldMap(List(_))(s)
