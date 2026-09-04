package dev.constructive.eo
package zio
package json

import _root_.zio.Chunk
import _root_.zio.json.ast.{Json, JsonCursor}
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

object JsonValuesFixtures:

  private def genLeaf: Gen[Json] = Gen.oneOf(
    Gen.alphaNumStr.map(Json.Str(_)),
    Gen.choose(-1000, 1000).map(i => Json.Num(i)),
    Gen.oneOf(Json.Bool(true), Json.Bool(false), Json.Null),
  )

  /** Objects carry the drilled name ("f") only sometimes, and arrays vary in length, so the miss
    * arms are generated. Keys are otherwise DISTINCT on purpose: zio-json's `Json.Obj.equals` maps
    * the left operand before comparing each right entry, so a duplicate-key object is not equal to
    * itself — an `==`-based law suite would fail on the fixture rather than on the optic. Duplicate
    * keys are pinned behaviourally in JsonOpticsSpec instead.
    */
  private def genTree(depth: Int): Gen[Json] =
    if depth <= 0 then genLeaf
    else
      Gen.frequency(
        3 -> genLeaf,
        2 -> Gen.lzy(
          Gen
            .choose(0, 3)
            .flatMap(n => Gen.listOfN(n, genTree(depth / 2)))
            .map(vs => Json.Arr(Chunk.fromIterable(vs)))
        ),
        2 -> Gen.lzy(
          for
            withF <- Gen.oneOf(true, false)
            n <- Gen.choose(0, 2)
            vs <- Gen.listOfN(n, genTree(depth / 2))
            f <- genTree(depth / 2)
          yield
            val named = vs.zipWithIndex.map((v, i) => (s"k$i", v))
            Json.Obj(Chunk.fromIterable(if withF then ("f" -> f) :: named else named))
        ),
      )

  given Arbitrary[Json] = Arbitrary(Gen.sized(d => genTree(math.min(d, 4))))
  given Cogen[Json] = Cogen[String].contramap(_.toString)

/** Law suites for the [[JsonValues]] kit and the [[JsonCursor]] bridge, all on drilled optics. */
class JsonValuesLawsSpec extends Specification with Discipline:

  import JsonValues.platedJson
  import JsonValuesFixtures.given

  private def eqJson: (Json, Json) => Boolean = _ == _

  checkAll(
    "Optional[Json] via field",
    new OptionalTests[Json, Json]:
      val laws: OptionalLaws[Json, Json] = new OptionalLaws[Json, Json]:
        val optional = JsonValues.field("f")
    .optional,
  )

  checkAll(
    "Seam[Json] via field",
    new SeamTests[Json, Json]:
      val laws: SeamLaws[Json, Json] = new SeamLaws[Json, Json]:
        val optic = JsonValues.field("f")
        val eqv = eqJson
    .seam,
  )

  checkAll(
    "Navigation[Json] via field",
    new NavigationTests[Json, Json]:
      val laws: NavigationLaws[Json, Json] = new NavigationLaws[Json, Json]:
        val navigation = JsonValues.field("f")
        val eqv = eqJson
    .navigation,
  )

  checkAll(
    "Seam[Json] via at",
    new SeamTests[Json, Json]:
      val laws: SeamLaws[Json, Json] = new SeamLaws[Json, Json]:
        val optic = JsonValues.at(0)
        val eqv = eqJson
    .seam,
  )

  checkAll(
    "Navigation[Json] via at",
    new NavigationTests[Json, Json]:
      val laws: NavigationLaws[Json, Json] = new NavigationLaws[Json, Json]:
        val navigation = JsonValues.at(0)
        val eqv = eqJson
    .navigation,
  )

  // The cursor bridge is where the single-pass spine rebuild lives — law-checked on the same
  // generator, so a rebuild that loses siblings or invents a level fails here.
  checkAll(
    "Seam[Json] via JsonCursor",
    new SeamTests[Json, Json]:
      val laws: SeamLaws[Json, Json] = new SeamLaws[Json, Json]:
        val optic = JsonCursor.field("f").optional
        val eqv = eqJson
    .seam,
  )

  checkAll(
    "Navigation[Json] via JsonCursor",
    new NavigationTests[Json, Json]:
      val laws: NavigationLaws[Json, Json] = new NavigationLaws[Json, Json]:
        val navigation = JsonCursor.field("f").optional
        val eqv = eqJson
    .navigation,
  )

  // atField's write can insert and delete, so its put-get covers strictly more than field's.
  checkAll(
    "Optional[Json] via atField",
    new OptionalTests[Json, Option[Json]]:
      val laws: OptionalLaws[Json, Option[Json]] = new OptionalLaws[Json, Option[Json]]:
        val optional = JsonValues.atField("f")
    .optional,
  )

  checkAll(
    "Seam[Json] via atField",
    new SeamTests[Json, Option[Json]]:
      val laws: SeamLaws[Json, Option[Json]] = new SeamLaws[Json, Option[Json]]:
        val optic = JsonValues.atField("f")
        val eqv = eqJson
    .seam,
  )

  checkAll(
    "Navigation[Json] via atField",
    new NavigationTests[Json, Option[Json]]:
      val laws: NavigationLaws[Json, Option[Json]] = new NavigationLaws[Json, Option[Json]]:
        val navigation = JsonValues.atField("f")
        val eqv = eqJson
    .navigation,
  )

  checkAll(
    "Prism[Json, String] via str",
    new PrismTests[Json, String]:
      val laws: PrismLaws[Json, String] = new PrismLaws[Json, String]:
        val prism = JsonValues.str
    .prism,
  )

  checkAll(
    "Plated[Json] (zio-json)",
    new PlatedTests[Json]:
      val laws = new PlatedLaws[Json] {}
    .plated,
  )
