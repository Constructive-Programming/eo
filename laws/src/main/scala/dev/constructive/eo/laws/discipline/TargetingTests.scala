package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen, Prop}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for the read tier of [[ReadTargeting]] — the extrinsic read agreement,
  * usable on its own for a read-only optic (`Getter`, `PickFold`, `Fold`). Register it beside the
  * intrinsic suite for the same optic, on a generator that produces both hits and misses of the
  * REFERENCE.
  */
abstract class ReadTargetingTests[S, A] extends Laws:
  /** Laws under test. */
  def laws: ReadTargeting[S, A]

  /** The "ReadTargeting" rule set. Needs no `Cogen[A]` — the prop is value-only. */
  def readTargeting(using Arbitrary[S]): RuleSet =
    new SimpleRuleSet(
      "ReadTargeting",
      "read targets the reference slot" ->
        Prop.forAll((s: S) => laws.readTargets(s)),
    )

/** Discipline `RuleSet` for [[TargetingLaws]] — read, write and modify agreement with an extrinsic
  * reference accessor. This is the rule set that fails for a lawful-but-mis-targeted optic (one
  * that passes `LensTests` / `OptionalTests` while reading and writing the wrong slot).
  *
  * Register it beside the intrinsic suite for the same optic, on a generator that produces real
  * data — values that all miss the reference make the read law vacuous.
  */
abstract class TargetingTests[S, A, F[_, _]] extends ReadTargetingTests[S, A]:
  /** Laws under test. */
  def laws: TargetingLaws[S, A, F]

  /** The "Targeting" rule set: the read tier plus the write and modify agreements. */
  def targeting(using
      arbS: Arbitrary[S],
      arbA: Arbitrary[A],
      cogenA: Cogen[A],
  ): RuleSet =
    new DefaultRuleSet(
      name = "Targeting",
      parent = Some(readTargeting),
      "write targets the reference slot" ->
        Prop.forAll((s: S, a: A) => laws.writeTargets(s, a)),
      "modify targets the reference slot" ->
        Prop.forAll((s: S, f: A => A) => laws.modifyTargets(s, f)),
    )
