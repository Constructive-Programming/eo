package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.MultiFocus.given
import _root_.dev.constructive.eo.optics.Plated

import optics.Optic.*

/** Law equations for a [[dev.constructive.eo.optics.Plated]] instance.
  *
  * These are the unconditional checks — they hold for *any* `S` and validate the three moving parts
  * at once: the self-traversal `plate` (round-trips), `transform` (identity-preserving), and
  * `universe` / `children` (mutually consistent). Running them on a macro-derived `plate[S]` is the
  * primary correctness signal for the derivation; `rewrite` needs a terminating rule and so is
  * exercised behaviourally instead of here.
  *
  * `equals` is used for the comparisons, so `S` must have structural equality (every case class /
  * enum does; `io.circe.Json` does too).
  */
trait PlatedLaws[S](using val P: Plated[S]):

  /** The self-traversal round-trips: writing each child back unchanged is the identity. This is the
    * law that catches a broken derivation (wrong child set, mis-ordered rebuild).
    */
  def plateModifyIdentity(s: S): Boolean =
    P.plate.modify(identity[S])(s) == s

  /** A no-op rewrite over the whole tree is the identity. */
  def transformIdentity(s: S): Boolean =
    Plated.transform(identity[S])(s) == s

  /** `universe` is `s` followed by the pre-order `universe` of each immediate child — ties the
    * stack-safe worklist implementation to `children`.
    */
  def universeDecomposition(s: S): Boolean =
    Plated.universe(s) == (s :: Plated.children(s).flatMap(Plated.universe(_)))

  /** The number of immediate children equals the focus count the self-traversal reports. */
  def childrenLengthMatchesPlateLength(s: S): Boolean =
    Plated.children(s).length == P.plate.length(s)
