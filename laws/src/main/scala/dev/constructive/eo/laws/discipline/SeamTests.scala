package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[SeamLaws]] — the write-seam round-trips (`modify(identity)`,
  * `compose modify`, `replace overwrite`) that must hold when an optic's generic `.modify` /
  * `.replace` is exercised on a DRILLED / partial-cover optic. Instantiate one per carrier with a
  * drilled optic and a structural `eqv`; the buggy sibling-dropping carriers fail
  * `seam modify identity` / `seam replace overwrite`.
  */
abstract class SeamTests[S, A] extends Laws:
  /** Laws under test. */
  def laws: SeamLaws[S, A]

  /** The "Seam" rule set. */
  def seam(using Arbitrary[S], Arbitrary[A], Cogen[A]): RuleSet =
    new SimpleRuleSet(
      "Seam",
      "seam modify identity (context preserved)" ->
        forAll((s: S) => laws.seamModifyIdentity(s)),
      "seam compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.seamComposeModify(s, f, g)),
      "seam replace overwrite (context preserved)" ->
        forAll((s: S, a1: A, a2: A) => laws.seamReplaceOverwrite(s, a1, a2)),
    )
