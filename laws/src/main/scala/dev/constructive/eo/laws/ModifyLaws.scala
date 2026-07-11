package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.ModifyF

import optics.Optic
import optics.Optic.*

/** Law equations for a `Modify[S, A]` — `Optic[S, S, A, A, ModifyF]`.
  *
  * Ported from Monocle's `monocle.law.SetterLaws`. Modify optics have no `get`, so the four laws
  * are modify-only: identity, composition, replace idempotence, and the consistency of `replace`
  * with `modify(const a)`.
  */
trait ModifyLaws[S, A]:
  /** The optic under test. */
  def modify: Optic[S, S, A, A, ModifyF]

  /** `modify(identity) == identity`. */
  def modifyIdentity(s: S): Boolean =
    modify.modify(identity[A])(s) == s

  /** `modify(g) ∘ modify(f) == modify(f andThen g)`. */
  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    modify.modify(g)(modify.modify(f)(s)) == modify.modify(f.andThen(g))(s)

  /** put-put: `replace(a) ∘ replace(a) == replace(a)`. */
  def replaceIdempotent(s: S, a: A): Boolean =
    modify.replace(a)(modify.replace(a)(s)) == modify.replace(a)(s)

  /** `replace(a) == modify(_ => a)`. */
  def consistentReplaceModify(s: S, a: A): Boolean =
    modify.replace(a)(s) == modify.modify(_ => a)(s)
