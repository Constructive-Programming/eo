package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[ZipLaws]] — certifies that a specific `l1.zip(l2)` pair focuses
  * disjoint parts and therefore forms a lawful lens.
  */
abstract class ZipTests[S, A, C] extends Laws:
  def laws: ZipLaws[S, A, C]

  def zip(using Arbitrary[S], Arbitrary[A], Arbitrary[C]): RuleSet =
    new SimpleRuleSet(
      "Zip",
      "get consistent with legs" -> forAll((s: S) => laws.getConsistent(s)),
      "get-replace" -> forAll((s: S) => laws.getReplace(s)),
      "replace-get" -> forAll((s: S, ac: (A, C)) => laws.replaceGet(s, ac)),
      "writes commute" -> forAll((s: S, a: A, c: C) => laws.writesCommute(s, a, c)),
      "read1 stable under write2" -> forAll((s: S, c: C) => laws.read1StableUnder2(s, c)),
      "read2 stable under write1" -> forAll((s: S, a: A) => laws.read2StableUnder1(s, a)),
    )
