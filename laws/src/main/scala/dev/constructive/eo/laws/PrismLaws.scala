package dev.constructive.eo
package laws

import optics.Optic
import optics.Optic.*

/** Law equations for a `Prism[S, A]` — `Optic[S, S, A, A, Either]`.
  *
  * Ported from Monocle's `monocle.law.PrismLaws`. The `getOption` convenience mirrors Monocle's
  * member of the same name, recovered from EO's primitive `to: S => Either[S, A]`.
  */
trait PrismLaws[S, A]:
  /** The optic under test. */
  def prism: Optic[S, S, A, A, Either]

  /** Mirror of Monocle's `Prism.getOption`. */
  def getOption(s: S): Option[A] = prism.to(s).toOption

  /** Hit ⇒ `reverseGet(a) == s` where `getOption(s) == Some(a)` — rebuild restores the source. */
  def partialRoundTripOneWay(s: S): Boolean =
    getOption(s) match
      case Some(a) => prism.reverseGet(a) == s
      case None    => true

  /** `getOption(reverseGet(a)) == Some(a)` — a built value always matches. */
  def roundTripOtherWay(a: A): Boolean =
    getOption(prism.reverseGet(a)) == Some(a)

  /** `modify(identity) == identity`. */
  def modifyIdentity(s: S): Boolean =
    prism.modify(identity[A])(s) == s

  /** `modify(g) ∘ modify(f) == modify(f andThen g)`. */
  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    prism.modify(g)(prism.modify(f)(s)) == prism.modify(f.andThen(g))(s)

  /** `getOption` survives a no-op modify: `getOption(modify(identity)(s)) == getOption(s)`. */
  def consistentGetOptionModifyId(s: S): Boolean =
    getOption(prism.modify(identity[A])(s)) == getOption(s)
