package eo
package laws

import optics.Optic
import optics.Optic.*
import _root_.eo.data.SetterF
import _root_.eo.data.SetterF.given

/** Law equations for a `Setter[S, A]` — `Optic[S, S, A, A, SetterF]`.
  *
  * Ported from Monocle's `monocle.law.SetterLaws`. Setters have no
  * `get`, so the four laws are modify-only: identity, composition,
  * replace idempotence, and the consistency of `replace` with
  * `modify(const a)`.
  */
trait SetterLaws[S, A]:
  def setter: Optic[S, S, A, A, SetterF]

  def modifyIdentity(s: S): Boolean =
    setter.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    setter.modify(g)(setter.modify(f)(s)) == setter.modify(f andThen g)(s)

  def replaceIdempotent(s: S, a: A): Boolean =
    setter.replace(a)(setter.replace(a)(s)) == setter.replace(a)(s)

  def consistentReplaceModify(s: S, a: A): Boolean =
    setter.replace(a)(s) == setter.modify(_ => a)(s)
