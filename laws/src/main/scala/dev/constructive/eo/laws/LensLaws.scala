package dev.constructive.eo
package laws

import optics.Optic
import optics.Optic.*

/** Law equations for a `Lens[S, A]` — `Optic[S, S, A, A, Tuple2]`.
  *
  * Ported from Monocle's `monocle.law.LensLaws`. Six equations total: the three classical
  * get/replace round-trips plus modify identity, modify composition, and the consistency bridge
  * between `replace` and `modify`.
  */
trait LensLaws[S, A]:
  def lens: Optic[S, S, A, A, Tuple2]

  def getReplace(s: S): Boolean =
    lens.replace(lens.get(s))(s) == s

  def replaceGet(s: S, a: A): Boolean =
    lens.get(lens.replace(a)(s)) == a

  def replaceIdempotent(s: S, a: A): Boolean =
    lens.replace(a)(lens.replace(a)(s)) == lens.replace(a)(s)

  def modifyIdentity(s: S): Boolean =
    lens.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    lens.modify(g)(lens.modify(f)(s)) == lens.modify(f.andThen(g))(s)

  def consistentReplaceModify(s: S, a: A): Boolean =
    lens.replace(a)(s) == lens.modify(_ => a)(s)
