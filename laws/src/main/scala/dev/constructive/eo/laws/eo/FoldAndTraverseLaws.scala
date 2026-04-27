package dev.constructive.eo
package laws
package eo

import optics.Optic
import optics.Optic.*

// Fold / Traverse family — the laws below pin down how EO's
// `foldMap` and `all` interact with the carriers that carry
// Traverse / Traversable structure.
//
//   E1 — `foldMap` is a Monoid homomorphism.

/** E1 — `optic.foldMap` is a Monoid homomorphism on the target monoid (tested at `Int` with
  * additive `Monoid[Int]`, which is enough to witness the law).
  */
trait FoldMapHomomorphismLaws[S, A, F[_, _]]:
  def optic: Optic[S, S, A, A, F]

  def foldMapHomomorphism(s: S, f: A => Int, g: A => Int)(using
      ForgetfulFold[F]
  ): Boolean =
    optic.foldMap(a => f(a) + g(a))(s) ==
      optic.foldMap(f)(s) + optic.foldMap(g)(s)

  def foldMapEmpty(s: S)(using ForgetfulFold[F]): Boolean =
    optic.foldMap[Int](_ => 0)(s) == 0
