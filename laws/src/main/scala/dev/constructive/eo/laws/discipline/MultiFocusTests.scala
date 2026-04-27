package dev.constructive.eo
package laws
package discipline

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Cogen}
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[MultiFocusLaws]]. The classifier `F[_]` flows in from the `laws`
  * field — each concrete `checkAll` wiring pins it to `List`, `Option`, `ZipList`, `Const[M, *]`,
  * etc.
  *
  * MF3 (`collect via map`) needs `S =:= F[A]` evidence to reach the focus — the generic
  * `MultiFocus.apply[F, A]` factory witnesses exactly this equality. A `Cogen[F[A]]` is required to
  * generate `F[A] => A` aggregator functions; callers pass it explicitly per instance because
  * generic `Cogen[F[A]]` derivation isn't available for `ZipList` / `Const` out of the box.
  */
abstract class MultiFocusTests[S, A, F[_]] extends Laws:
  def laws: MultiFocusLaws[S, A, F]

  def multiFocus(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      Cogen[F[A]],
      S =:= F[A],
  ): RuleSet =
    new SimpleRuleSet(
      "MultiFocus",
      OpticLawProps.modifyIdentity[S](laws.modifyIdentity),
      OpticLawProps.composeModify[S, A](laws.composeModify),
      "collect via map" ->
        forAll((s: S, agg: F[A] => A) => laws.collectViaMap(s, agg)),
    )
