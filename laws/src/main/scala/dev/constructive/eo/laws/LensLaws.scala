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
  /** The optic under test. */
  def lens: Optic[S, S, A, A, Tuple2]

  /** get-put: `replace(get(s))(s) == s` — writing back what was read is a no-op. */
  def getReplace(s: S): Boolean =
    lens.replace(lens.get(s))(s) == s

  /** put-get: `get(replace(a)(s)) == a` — a read observes the last write. */
  def replaceGet(s: S, a: A): Boolean =
    lens.get(lens.replace(a)(s)) == a

  /** put-put: `replace(a) ∘ replace(a) == replace(a)` — replacing twice equals replacing once. */
  def replaceIdempotent(s: S, a: A): Boolean =
    lens.replace(a)(lens.replace(a)(s)) == lens.replace(a)(s)

  /** `modify(identity) == identity`. */
  def modifyIdentity(s: S): Boolean =
    lens.modify(identity[A])(s) == s

  /** `modify(g) ∘ modify(f) == modify(f andThen g)`. */
  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    lens.modify(g)(lens.modify(f)(s)) == lens.modify(f.andThen(g))(s)

  /** `replace(a) == modify(_ => a)`. */
  def consistentReplaceModify(s: S, a: A): Boolean =
    lens.replace(a)(s) == lens.modify(_ => a)(s)
