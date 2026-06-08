package dev.constructive.eo
package circe

import dev.constructive.eo.laws.PlatedLaws
import dev.constructive.eo.laws.discipline.PlatedTests
import dev.constructive.eo.optics.Plated
import io.circe.Json
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

object JsonPlatedFixtures:

  private def genJson(depth: Int): Gen[Json] =
    val leaf = Gen.oneOf(
      Gen.alphaNumStr.map(Json.fromString),
      Gen.choose(-1000, 1000).map(Json.fromInt),
      Gen.oneOf(Json.True, Json.False, Json.Null),
    )
    if depth <= 0 then leaf
    else
      Gen.frequency(
        3 -> leaf,
        1 -> Gen.lzy(
          Gen.choose(0, 3).flatMap(n => Gen.listOfN(n, genJson(depth / 2)).map(Json.fromValues))
        ),
        // Distinct keys (k0, k1, …) so rebuild can't collapse duplicates.
        1 -> Gen.lzy(
          Gen
            .choose(0, 3)
            .flatMap(n => Gen.listOfN(n, genJson(depth / 2)))
            .map(vs => Json.obj(vs.zipWithIndex.map((v, i) => s"k$i" -> v)*))
        ),
      )

  given Arbitrary[Json] = Arbitrary(Gen.sized(d => genJson(math.min(d, 4))))

/** `Plated[io.circe.Json]` — the universal JSON-tree self-traversal. Discipline laws on generated
  * documents, plus the headline use case: rewrite every node, recursively.
  */
class JsonPlatedSpec extends Specification with Discipline:

  import JsonPlatedFixtures.given

  checkAll(
    "Plated[Json]",
    new PlatedTests[Json]:
      val laws = new PlatedLaws[Json] {}
    .plated,
  )

  "transform uppercases every string anywhere in the document" >> {
    val doc = Json.obj(
      "a" -> Json.fromString("x"),
      "b" -> Json.arr(Json.fromString("y"), Json.fromInt(3)),
      "c" -> Json.obj("d" -> Json.fromString("z")),
    )
    val upper: Json => Json = j => j.asString.fold(j)(s => Json.fromString(s.toUpperCase))
    Plated.transform(upper)(doc) == Json.obj(
      "a" -> Json.fromString("X"),
      "b" -> Json.arr(Json.fromString("Y"), Json.fromInt(3)),
      "c" -> Json.obj("d" -> Json.fromString("Z")),
    )
  }
