package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[NavigationLaws]] — the two equations every untyped-tree navigation
  * kit promises and none of the stock suites check: put-get on a hit, and write-is-a-no-op on a
  * miss. Instantiate per kit alongside `OptionalTests` (read tier) and `SeamTests` (drilled write
  * seam), with a generator that produces BOTH hits and misses.
  */
abstract class NavigationTests[S, A] extends Laws:
  /** Laws under test. */
  def laws: NavigationLaws[S, A]

  /** The "Navigation" rule set. Needs no `Cogen[A]` — both props are value-only. */
  def navigation(using Arbitrary[S], Arbitrary[A]): RuleSet =
    new SimpleRuleSet(
      "Navigation",
      "put-get (hit writes are readable)" ->
        forAll((s: S, a: A) => laws.putGet(s, a)),
      "miss write is a no-op (no node invented)" ->
        forAll((s: S, a: A) => laws.missWriteIsNoOp(s, a)),
    )
