package dev.constructive.eo
package kyo
package schema

import _root_.kyo.Chunk
import _root_.kyo.Structure.Value
import dev.constructive.eo.laws.discipline.{
  NavigationTests,
  OptionalTests,
  PlatedTests,
  PrismTests,
  SeamTests
}
import dev.constructive.eo.laws.{NavigationLaws, OptionalLaws, PlatedLaws, PrismLaws, SeamLaws}
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

object StructureValuesFixtures:

  private def genLeaf: Gen[Value] = Gen.oneOf(
    Gen.alphaNumStr.map(Value.Str(_)),
    Gen.choose(-1000L, 1000L).map(Value.Integer(_)),
    Gen.oneOf(true, false).map(Value.Bool(_)),
    Gen.const(Value.Null),
  )

  /** Records carry the drilled name ("f") only sometimes, and sequences vary in length, so the miss
    * arms of `field` / `atField` / `at` / `key` / `variant` are generated — `missWriteIsNoOp` is
    * vacuous otherwise. Names and map keys are otherwise distinct; duplicate-name records are
    * pinned behaviourally in StructureOpticsSpec instead.
    */
  private def genTree(depth: Int): Gen[Value] =
    if depth <= 0 then genLeaf
    else
      Gen.frequency(
        3 -> genLeaf,
        2 -> Gen.lzy(
          Gen
            .choose(0, 3)
            .flatMap(n => Gen.listOfN(n, genTree(depth / 2)))
            .map(vs => Value.Sequence(Chunk.from(vs)))
        ),
        2 -> Gen.lzy(
          for
            withF <- Gen.oneOf(true, false)
            n <- Gen.choose(0, 2)
            vs <- Gen.listOfN(n, genTree(depth / 2))
            f <- genTree(depth / 2)
          yield
            val named = vs.zipWithIndex.map((v, i) => (s"k$i", v))
            Value.Record(Chunk.from(if withF then ("f" -> f) :: named else named))
        ),
        1 -> Gen.lzy(
          Gen
            .choose(0, 2)
            .flatMap(n => Gen.listOfN(n, genTree(depth / 2)))
            .map(vs =>
              Value.MapEntries(
                Chunk.from(vs.zipWithIndex.map((v, i) => (Value.Str(s"k$i"): Value, v)))
              )
            )
        ),
        1 -> Gen.lzy(
          Gen
            .oneOf("hit", "other")
            .flatMap(n => genTree(depth / 2).map(p => Value.VariantCase(n, p)))
        ),
      )

  given Arbitrary[Value] = Arbitrary(Gen.sized(d => genTree(math.min(d, 4))))
  given Cogen[Value] = Cogen[String].contramap(_.toString)

/** Law suites for the [[StructureValues]] kit — the kyo instantiation of the same shared
  * `NavigationLaws` the zio kits run, which is the point of hosting those laws in the published
  * `laws` module: the three untyped-tree kits assert one contract instead of three hand-written
  * approximations of it. Every navigation optic runs DRILLED (the generator surrounds the focus
  * with siblings), per the repo's SeamLaws doctrine.
  */
class StructureValuesLawsSpec extends Specification with Discipline:

  import StructureValues.platedValue
  import StructureValuesFixtures.given

  private def eqv: (Value, Value) => Boolean = _ == _

  checkAll(
    "Optional[Value] via field",
    new OptionalTests[Value, Value]:
      val laws: OptionalLaws[Value, Value] = new OptionalLaws[Value, Value]:
        val optional = StructureValues.field("f")
    .optional,
  )

  checkAll(
    "Seam[Value] via field",
    new SeamTests[Value, Value]:
      val laws: SeamLaws[Value, Value] = new SeamLaws[Value, Value]:
        val optic = StructureValues.field("f")
        val eqv = StructureValuesLawsSpec.this.eqv
    .seam,
  )

  checkAll(
    "Navigation[Value] via field",
    new NavigationTests[Value, Value]:
      val laws: NavigationLaws[Value, Value] = new NavigationLaws[Value, Value]:
        val navigation = StructureValues.field("f")
        val eqv = StructureValuesLawsSpec.this.eqv
    .navigation,
  )

  checkAll(
    "Navigation[Value] via atField (put-get now spans inserts and deletes)",
    new NavigationTests[Value, Option[Value]]:
      val laws: NavigationLaws[Value, Option[Value]] = new NavigationLaws[Value, Option[Value]]:
        val navigation = StructureValues.atField("f")
        val eqv = StructureValuesLawsSpec.this.eqv
    .navigation,
  )

  // NO SeamTests for atField, deliberately — and this is a finding, not an omission.
  //
  // `SeamLaws.seamReplaceOverwrite` is put-put, and it FAILS for an At-style optic over an ORDERED
  // record: `replace(None)` deletes the field, so the following `replace(Some(v))` appends it at the
  // end, whereas the direct `replace(Some(v))` updates it in place. Counterexample the generator
  // found: s = Record((f, …), (k0, …)), a1 = None, a2 = Some(Integer(-510)) — same fields, different
  // ORDER. `seamComposeModify` fails for the same reason when f maps Some -> None and g maps back.
  //
  // Not fixable in the optic: deletion destroys the position that put-put wants restored. The zio
  // kits pass the identical suite only because their equalities ignore field order (a `ListMap` is
  // `Map`-equal, and zio-json's `Json.Obj.equals` maps the left operand) — the BEHAVIOUR there is
  // the same. So the honest statement is: put-get, get-put and the miss contract hold everywhere
  // (checked below); put-put holds only up to field order. See the atField scaladoc.

  checkAll(
    "Seam[Value] via at",
    new SeamTests[Value, Value]:
      val laws: SeamLaws[Value, Value] = new SeamLaws[Value, Value]:
        val optic = StructureValues.at(0)
        val eqv = StructureValuesLawsSpec.this.eqv
    .seam,
  )

  checkAll(
    "Navigation[Value] via key",
    new NavigationTests[Value, Value]:
      val laws: NavigationLaws[Value, Value] = new NavigationLaws[Value, Value]:
        val navigation = StructureValues.key(Value.Str("k0"))
        val eqv = StructureValuesLawsSpec.this.eqv
    .navigation,
  )

  checkAll(
    "Seam[Value] via key",
    new SeamTests[Value, Value]:
      val laws: SeamLaws[Value, Value] = new SeamLaws[Value, Value]:
        val optic = StructureValues.key(Value.Str("k0"))
        val eqv = StructureValuesLawsSpec.this.eqv
    .seam,
  )

  checkAll(
    "Seam[Value] via variant",
    new SeamTests[Value, Value]:
      val laws: SeamLaws[Value, Value] = new SeamLaws[Value, Value]:
        val optic = StructureValues.variant("hit")
        val eqv = StructureValuesLawsSpec.this.eqv
    .seam,
  )

  checkAll(
    "Prism[Value, String] via str",
    new PrismTests[Value, String]:
      val laws: PrismLaws[Value, String] = new PrismLaws[Value, String]:
        val prism = StructureValues.str
    .prism,
  )

  checkAll(
    "Plated[Value]",
    new PlatedTests[Value]:
      val laws = new PlatedLaws[Value] {}
    .plated,
  )
