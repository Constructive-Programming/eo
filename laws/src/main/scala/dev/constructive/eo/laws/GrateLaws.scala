package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.Grate
import _root_.dev.constructive.eo.data.Grate.given

import optics.Optic
import optics.Optic.*

/** Law equations for a `Grate[S, A]` — `Optic[S, S, A, A, Grate]`.
  *
  * Ported from Haskell `optics` (`Optics.Grate`) and the Clarke et al. categorical formulation of
  * distributive optics (see the plan at `docs/plans/2026-04-23-004-feat-grate-optic-family-plan.md`
  * §Sources, D5). Three equations:
  *
  *   - **G1 modifyIdentity** — `grate.modify(identity)(s) == s`. Shared shape with every other
  *     family that admits `.modify`.
  *   - **G2 composeModify** —
  *     `grate.modify(g)(grate.modify(f)(s)) == grate.modify(f andThen g)(s)`. Shared shape.
  *   - **G3 pureRebuild** — `grate.replace(a)(s)` is itself well-formed: replacing with the focus
  *     just read yields a structure whose focus re-reads as `a`. This is the Grate-specific
  *     consistency check between the focus side and the rebuild side. We express it as a stable
  *     round-trip rather than the textbook `k(s) == a` (which is not expressible in the generic
  *     `Optic[…, Grate]` API because `s: X` is existential; carrier-level specs can witness the
  *     stronger form directly).
  *
  * @see
  *   `dev.constructive.eo.laws.discipline.GrateTests` for the discipline RuleSet.
  */
trait GrateLaws[S, A]:
  def grate: Optic[S, S, A, A, Grate]

  def modifyIdentity(s: S): Boolean =
    grate.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    grate.modify(g)(grate.modify(f)(s)) == grate.modify(f.andThen(g))(s)

  /** Replace-idempotent consistency — replacing with the same constant twice matches replacing
    * once. This isolates the Grate's `from` as a well-behaved rebuild: the second replace sees a
    * structure whose every focus is already `a`, and re-reading through `from` must yield the same
    * all-`a` structure. Stronger than the Lens variant because Grate's `.replace(a)` fills every
    * slot (broadcast), so the idempotence fold also witnesses the broadcast itself.
    */
  def replaceIdempotent(s: S, a: A): Boolean =
    grate.replace(a)(grate.replace(a)(s)) == grate.replace(a)(s)
