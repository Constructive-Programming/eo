package dev.constructive.eo
package laws

import cats.Monoid
import optics.AffineFold
import optics.Optic.*

/** Law equations for an `AffineFold[S, A]` — a read-only 0-or-1 focus optic whose carrier is
  * `Affine` and whose write side is pinned to `Unit`.
  *
  * AffineFold's observable surface is two operations — `getOption` and `foldMap` — and the laws
  * below pin down their agreement:
  *
  *   1. `getOptionConsistent`: `getOption` and `foldMap` agree on the single hit. Phrased via
  *      `foldMap(List(_))`: the full fold collects every focus into a list; for AffineFold that
  *      list has zero or one element, whose head is exactly the `getOption` result.
  *   2. `missIsEmpty`: when `getOption(s)` is `None`, `foldMap(f)(s)` must equal `Monoid.empty`
  *      under every monoid `M`. Tested at `Int` (additive) for scalacheck coverage.
  *   3. `hitIsSingleton`: when `getOption(s)` is `Some(a)`, `foldMap(f)(s)` must equal `f(a)` under
  *      every monoid. Tested at `Int` as above.
  *
  * Laws (2) and (3) are the two "branches" of an invariant that's trivially true by the `Affine`
  * carrier's `ForgetfulFold` instance — they're nonetheless worth stating because the AffineFold's
  * value is exactly that a *user-supplied* partial matcher fits the miss/hit contract. A
  * constructor that returned the wrong branch would fail law (2) or (3) for the wronged input.
  *
  * Ported from Monocle's `monocle.law.AffineFoldLaws` (which states
  * `getAll.headOption === getOption` directly); our phrasing goes through the generic `foldMap`
  * extension to stay within the existential-optic surface.
  */
trait AffineFoldLaws[S, A]:
  def af: AffineFold[S, A]

  /** `getOption` agrees with the `foldMap`-list head. */
  def getOptionConsistent(s: S): Boolean =
    af.getOption(s) == af.foldMap(a => List(a))(s).headOption

  /** Miss ⇒ `foldMap` collapses to the monoid identity under any `Monoid[Int]`-valued `f`. */
  def missIsEmpty(s: S, f: A => Int): Boolean =
    if af.getOption(s).isEmpty then af.foldMap(f)(s) == Monoid[Int].empty
    else true

  /** Hit ⇒ `foldMap(f)(s) == f(a)` where `a = getOption(s).get`, under any `Monoid[Int]`-valued
    * `f`.
    */
  def hitIsSingleton(s: S, f: A => Int): Boolean =
    af.getOption(s) match
      case Some(a) => af.foldMap(f)(s) == f(a)
      case None    => true
