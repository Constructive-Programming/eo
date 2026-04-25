package dev.constructive.eo
package laws
package discipline

import org.scalacheck.{Arbitrary, Cogen}
import org.scalacheck.Prop.forAll
import org.typelevel.discipline.Laws

/** Discipline `RuleSet` for [[KaleidoscopeLaws]]. The `F` path-type flows in from the `laws` field
  * — each concrete `checkAll` wiring pins it to `List`, `ZipList`, `Const[M, *]`, etc.
  *
  * K3 (`collect via reflect`) needs `S =:= F[A]` evidence to reach the focus — the generic
  * `Kaleidoscope.apply[F, A]` factory witnesses exactly this equality. A `Cogen[F[A]]` is required
  * to generate `F[A] => A` aggregator functions; callers pass it explicitly per instance because
  * generic `Cogen[F[A]]` derivation isn't available for `ZipList` / `Const` out of the box.
  */
abstract class KaleidoscopeTests[S, A, F[_]] extends Laws:
  def laws: KaleidoscopeLaws[S, A, F]

  def kaleidoscope(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      Cogen[F[A]],
      S =:= F[A],
  ): RuleSet =
    new SimpleRuleSet(
      "Kaleidoscope",
      "modify identity" -> forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify" ->
        forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "collect via reflect" ->
        forAll((s: S, agg: F[A] => A) => laws.collectViaReflect(s, agg)),
    )
