package dev.constructive.eo
package zio
package schema

import scala.collection.immutable.ListMap

import _root_.zio.Chunk
import _root_.zio.schema.{DynamicValue, StandardType, TypeId}
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

object DynamicValuesFixtures:

  private def genLeaf: Gen[DynamicValue] = Gen.oneOf(
    Gen.alphaNumStr.map(s => DynamicValue.Primitive(s, StandardType.StringType)),
    Gen.choose(-1000, 1000).map(i => DynamicValue.Primitive(i, StandardType.IntType)),
    Gen.oneOf(true, false).map(b => DynamicValue.Primitive(b, StandardType.BoolType)),
  )

  /** Records deliberately mix in AND omit the drilled field name ("f"), and sequences vary in
    * length, so the miss arms of `field` / `at` / `key` / `variant` are actually generated —
    * `missWriteIsNoOp` is vacuous otherwise.
    */
  private def genTree(depth: Int): Gen[DynamicValue] =
    if depth <= 0 then genLeaf
    else
      Gen.frequency(
        3 -> genLeaf,
        2 -> Gen.lzy(
          Gen
            .choose(0, 3)
            .flatMap(n => Gen.listOfN(n, genTree(depth / 2)))
            .map(vs => DynamicValue.Sequence(Chunk.fromIterable(vs)))
        ),
        2 -> Gen.lzy(
          for
            withF <- Gen.oneOf(true, false)
            n <- Gen.choose(0, 2)
            vs <- Gen.listOfN(n, genTree(depth / 2))
            f <- genTree(depth / 2)
          yield
            val named = vs.zipWithIndex.map((v, i) => (s"k$i", v))
            val fields = if withF then ("f" -> f) :: named else named
            DynamicValue.Record(TypeId.Structural, ListMap.from(fields))
        ),
        1 -> Gen.lzy(
          Gen
            .choose(0, 2)
            .flatMap(n => Gen.listOfN(n, genTree(depth / 2)))
            // Distinct keys: duplicate-key dictionaries are pinned behaviourally in
            // SchemaOpticsSpec, and `Dictionary` order-sensitivity makes them a poor law fixture.
            .map(vs =>
              DynamicValue.Dictionary(
                Chunk.fromIterable(vs.zipWithIndex.map { (v, i) =>
                  (DynamicValue.Primitive(s"k$i", StandardType.StringType), v)
                })
              )
            )
        ),
        1 -> Gen.lzy(
          Gen
            .oneOf("hit", "other")
            .flatMap(n =>
              genTree(depth / 2).map(p => DynamicValue.Enumeration(TypeId.Structural, (n, p)))
            )
        ),
        1 -> Gen.lzy(genTree(depth / 2).map(v => DynamicValue.SetValue(Set(v)))),
      )

  given Arbitrary[DynamicValue] = Arbitrary(Gen.sized(d => genTree(math.min(d, 4))))
  given Cogen[DynamicValue] = Cogen[String].contramap(_.toString)

/** Law suites for the [[DynamicValues]] kit. Every navigation optic is run '''drilled''' — the
  * generator surrounds the focus with siblings — which is the shape the repo's `SeamLaws` doctrine
  * requires: a carrier that rebuilds the focus standalone passes a full-cover fixture and fails
  * here.
  */
class DynamicValuesLawsSpec extends Specification with Discipline:

  import DynamicValues.platedDynamicValue
  import DynamicValuesFixtures.given

  private def eqDv: (DynamicValue, DynamicValue) => Boolean = _ == _

  // ---- field("f"): drilled into records that may or may not have the field ----

  checkAll(
    "Optional[DynamicValue] via field",
    new OptionalTests[DynamicValue, DynamicValue]:
      val laws: OptionalLaws[DynamicValue, DynamicValue] =
        new OptionalLaws[DynamicValue, DynamicValue]:
          val optional = DynamicValues.field("f")
    .optional,
  )

  checkAll(
    "Seam[DynamicValue] via field",
    new SeamTests[DynamicValue, DynamicValue]:
      val laws: SeamLaws[DynamicValue, DynamicValue] = new SeamLaws[DynamicValue, DynamicValue]:
        val optic = DynamicValues.field("f")
        val eqv = eqDv
    .seam,
  )

  checkAll(
    "Navigation[DynamicValue] via field",
    new NavigationTests[DynamicValue, DynamicValue]:
      val laws: NavigationLaws[DynamicValue, DynamicValue] =
        new NavigationLaws[DynamicValue, DynamicValue]:
          val navigation = DynamicValues.field("f")
          val eqv = eqDv
    .navigation,
  )

  // ---- at(0) / key(k0) / variant("hit"): the same three tiers, condensed to the ones that
  // exercise a different write arm (sequence index, dictionary key, sum tag) ----

  checkAll(
    "Seam[DynamicValue] via at",
    new SeamTests[DynamicValue, DynamicValue]:
      val laws: SeamLaws[DynamicValue, DynamicValue] = new SeamLaws[DynamicValue, DynamicValue]:
        val optic = DynamicValues.at(0)
        val eqv = eqDv
    .seam,
  )

  checkAll(
    "Navigation[DynamicValue] via at",
    new NavigationTests[DynamicValue, DynamicValue]:
      val laws: NavigationLaws[DynamicValue, DynamicValue] =
        new NavigationLaws[DynamicValue, DynamicValue]:
          val navigation = DynamicValues.at(0)
          val eqv = eqDv
    .navigation,
  )

  checkAll(
    "Seam[DynamicValue] via key",
    new SeamTests[DynamicValue, DynamicValue]:
      val laws: SeamLaws[DynamicValue, DynamicValue] = new SeamLaws[DynamicValue, DynamicValue]:
        val optic = DynamicValues.key(DynamicValue.Primitive("k0", StandardType.StringType))
        val eqv = eqDv
    .seam,
  )

  checkAll(
    "Navigation[DynamicValue] via key",
    new NavigationTests[DynamicValue, DynamicValue]:
      val laws: NavigationLaws[DynamicValue, DynamicValue] =
        new NavigationLaws[DynamicValue, DynamicValue]:
          val navigation = DynamicValues.key(
            DynamicValue.Primitive("k0", StandardType.StringType)
          )
          val eqv = eqDv
    .navigation,
  )

  checkAll(
    "Seam[DynamicValue] via variant",
    new SeamTests[DynamicValue, DynamicValue]:
      val laws: SeamLaws[DynamicValue, DynamicValue] = new SeamLaws[DynamicValue, DynamicValue]:
        val optic = DynamicValues.variant("hit")
        val eqv = eqDv
    .seam,
  )

  checkAll(
    "Navigation[DynamicValue] via variant",
    new NavigationTests[DynamicValue, DynamicValue]:
      val laws: NavigationLaws[DynamicValue, DynamicValue] =
        new NavigationLaws[DynamicValue, DynamicValue]:
          val navigation = DynamicValues.variant("hit")
          val eqv = eqDv
    .navigation,
  )

  // ---- constructor prisms ----

  checkAll(
    "Prism[DynamicValue, String] via str",
    new PrismTests[DynamicValue, String]:
      val laws: PrismLaws[DynamicValue, String] = new PrismLaws[DynamicValue, String]:
        val prism = DynamicValues.str
    .prism,
  )

  checkAll(
    "Prism[DynamicValue, Int] via int",
    new PrismTests[DynamicValue, Int]:
      val laws: PrismLaws[DynamicValue, Int] = new PrismLaws[DynamicValue, Int]:
        val prism = DynamicValues.int
    .prism,
  )

  // ---- the whole-tree self-traversal, over every node shape the generator emits (records,
  // sequences, sets, dictionaries, enumerations) — the arms the behavioural spec can only sample ----

  checkAll(
    "Plated[DynamicValue]",
    new PlatedTests[DynamicValue]:
      val laws = new PlatedLaws[DynamicValue] {}
    .plated,
  )
