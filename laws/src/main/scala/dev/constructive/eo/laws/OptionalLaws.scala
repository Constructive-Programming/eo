package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Affine

import optics.Optic
import optics.Optic.*

/** Law equations for an `Optional[S, A]` — `Optic[S, S, A, A, Affine]`.
  *
  * Ported from Monocle's `monocle.law.OptionalLaws`. `getOption` is recovered from EO's primitive
  * `to`, whose `Affine` carrier is a Miss/Hit sum — `fold` projects out the focused value when
  * present.
  */
trait OptionalLaws[S, A]:
  def optional: Optic[S, S, A, A, Affine]

  /** Mirror of Monocle's `Optional.getOption`. */
  def getOption(s: S): Option[A] =
    optional.to(s).fold(_ => None, (_, a) => Some(a))

  def modifyIdentity(s: S): Boolean =
    optional.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    optional.modify(g)(optional.modify(f)(s)) == optional.modify(f.andThen(g))(s)

  def consistentGetOptionModifyId(s: S): Boolean =
    getOption(optional.modify(identity[A])(s)) == getOption(s)
