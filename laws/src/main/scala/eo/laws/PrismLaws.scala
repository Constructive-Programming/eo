package eo
package laws

import optics.Optic
import optics.Optic.*
// Imports shared givens that the Either-carrier's ForgetfulFunctor
// instance depends on (used by `prism.modify`).
import _root_.eo.data.Forgetful.given

/** Law equations for a `Prism[S, A]` — `Optic[S, S, A, A, Either]`.
  *
  * Ported from Monocle's `monocle.law.PrismLaws`. The `getOption` convenience mirrors Monocle's
  * member of the same name, recovered from EO's primitive `to: S => Either[S, A]`.
  */
trait PrismLaws[S, A]:
  def prism: Optic[S, S, A, A, Either]

  /** Mirror of Monocle's `Prism.getOption`. */
  def getOption(s: S): Option[A] = prism.to(s).toOption

  def partialRoundTripOneWay(s: S): Boolean =
    getOption(s) match
      case Some(a) => prism.reverseGet(a) == s
      case None    => true

  def roundTripOtherWay(a: A): Boolean =
    getOption(prism.reverseGet(a)) == Some(a)

  def modifyIdentity(s: S): Boolean =
    prism.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    prism.modify(g)(prism.modify(f)(s)) == prism.modify(f.andThen(g))(s)

  def consistentGetOptionModifyId(s: S): Boolean =
    // Monocle: optional.getOption(prism.modify(identity)(s)) === optional.getOption(s)
    getOption(prism.modify(identity[A])(s)) == getOption(s)
