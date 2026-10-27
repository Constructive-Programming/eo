package dev.constructive.eo
package laws

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

object PlatedLaws:

  extension [S](laws: PlatedLaws[S])

    /** Second CartesianStore coalgebra law (`fmap plate ∘ plate == duplicate ∘ plate`, O'Connor
      * 2011 §3.2): modifying the immediate children twice in sequence equals one pass with the
      * composed function. Catches a `rebuild` that reads stale child fields off `parent` instead of
      * the supplied children vector, or permutes / drops modified children on a second pass — bugs
      * that [[PlatedLaws.plateModifyIdentity]] alone cannot detect.
      */
    def plateComposeModify(s: S, f: S => S, g: S => S): Boolean =
      import laws.P
      P.plate.modify(g)(P.plate.modify(f)(s)) == P.plate.modify(f.andThen(g))(s)

    /** `transformM` instantiated at `cats.Id` agrees with pure `transform` (O'Connor 2011 §5.1:
      * `mapFamily` is `mapFamilyM` at `Id`).
      */
    def transformMIdConsistent(s: S, f: S => S): Boolean =
      import laws.P
      Plated.transformM[cats.Id, S](f)(s) == Plated.transform(f)(s)
