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
  /** The optic under test. */
  def optional: Optic[S, S, A, A, Affine]

  /** Mirror of Monocle's `Optional.getOption`. */
  def getOption(s: S): Option[A] =
    optional.to(s).fold(_ => None, (_, a) => Some(a))

  /** `modify(identity) == identity`. */
  def modifyIdentity(s: S): Boolean =
    optional.modify(identity[A])(s) == s

  /** `modify(g) ∘ modify(f) == modify(f andThen g)`. */
  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    optional.modify(g)(optional.modify(f)(s)) == optional.modify(f.andThen(g))(s)

  /** `getOption` survives a no-op modify: `getOption(modify(identity)(s)) == getOption(s)`. */
  def consistentGetOptionModifyId(s: S): Boolean =
    getOption(optional.modify(identity[A])(s)) == getOption(s)
