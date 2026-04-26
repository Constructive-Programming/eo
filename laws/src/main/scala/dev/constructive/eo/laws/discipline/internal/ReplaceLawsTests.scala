package dev.constructive.eo
package laws
package discipline
package internal

import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Internal parent `RuleSet` for the modify-tier law families — `Setter`, `Lens`, and `Traversal`.
  * Each of those law sets shares the four "modify-tier" props (modify identity, compose modify,
  * replace idempotent, consistent replace-modify); the leaf Tests classes pass a parent built here
  * to their `RuleSet.parents` and only have to spell out the props that are unique to themselves.
  *
  * '''Path B (2026-04-25)''': replaces the per-leaf duplication of those four `forAll` lines with
  * a single inheritance hop. Discipline aggregates parent props automatically, so the user-facing
  * prop names ("modify identity", "compose modify", …) and check predicates are unchanged.
  *
  * Visibility: `private[discipline]`. The leaf Tests classes (`SetterTests`, `LensTests`,
  * `TraversalTests`) are the only intended consumers — downstream users wire to those.
  */
private[discipline] abstract class ReplaceLawsTests[S, A] extends Laws:

  /** Per-family law projection — `SetterLaws`, `LensLaws`, and `TraversalLaws` all expose the
    * four modify-tier methods this parent depends on. We keep the projection structural-free
    * with explicit function values so the parent doesn't need to know which leaf trait it sees.
    */
  protected def modifyIdentityFn: S => Boolean
  protected def composeModifyFn: (S, A => A, A => A) => Boolean
  protected def replaceIdempotentFn: (S, A) => Boolean
  protected def consistentReplaceModifyFn: (S, A) => Boolean

  /** Shared four-prop ruleSet. Leaf Tests classes thread this into their own `RuleSet.parents`. */
  def replaceTier(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "replaceTier",
      OpticLawProps.modifyIdentity[S](modifyIdentityFn),
      OpticLawProps.composeModify[S, A](composeModifyFn),
      OpticLawProps.replaceIdempotent[S, A](replaceIdempotentFn),
      OpticLawProps.consistentReplaceModify[S, A](consistentReplaceModifyFn),
    )
