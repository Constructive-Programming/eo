package dev.constructive.eo
package laws
package discipline
package internal

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Internal parent `RuleSet` for the partial-read law families — `Optional` and `Prism`. Both
  * pin down `modifyIdentity`, `composeModify`, and `consistentGetOptionModifyId` (the partial-read
  * round-trip "getOption survives a no-op modify"). Leaves add only the props specific to them
  * — Prism contributes the two reverse-roundtrip props on top.
  *
  * '''Path B (2026-04-25)''': removes the duplicate three-prop body each leaf used to spell out.
  *
  * Visibility: `private[discipline]`. Leaves: `OptionalTests`, `PrismTests`.
  */
private[discipline] abstract class PartialReadLawsTests[S, A] extends Laws:

  protected def modifyIdentityFn: S => Boolean
  protected def composeModifyFn: (S, A => A, A => A) => Boolean
  protected def consistentGetOptionModifyIdFn: S => Boolean

  /** Three-prop parent ruleSet shared by `OptionalTests` and `PrismTests`. */
  def partialReadTier(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "partialReadTier",
      OpticLawProps.modifyIdentity[S](modifyIdentityFn),
      OpticLawProps.composeModify[S, A](composeModifyFn),
      "consistent getOption / modify-id" ->
        forAll((s: S) => consistentGetOptionModifyIdFn(s)),
    )
