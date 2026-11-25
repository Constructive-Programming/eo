package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.laws.TargetingLaws
import dev.constructive.eo.laws.discipline.TargetingTests
import java.util.Arrays
import org.scalacheck.{Arbitrary, Cogen, Gen}

/** Issue #99 on the jsoniter byte carrier: `.field(_.name)` checked against the CODEC's own
  * accessor.
  *
  * The reference is written without the prism: `readFromArray[TargetUser]` (the whole-document
  * decode the caller would otherwise do) reads the field, and decode → `copy` → `writeToArray`
  * writes it. Nothing in that reference knows the JSONPath the prism resolved, which is exactly
  * what lets it see a resolution mistake — every intrinsic law suite (`SeamTests` included) passes
  * on an optic aimed at the wrong key, because such an optic is lawful onto that key.
  *
  * Sources are canonical `writeToArray` encodings, so the whole-document re-encode the reference
  * performs is byte-identical to the prism's in-place splice.
  */
class JsoniterTargetingSuite extends munit.DisciplineSuite:

  /** The leaf codec `.field(_.name)` needs at the call site — jsoniter derives none implicitly. */
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def encode(u: TargetUser): Array[Byte] = writeToArray(u)

  private val arbUserBytes: Arbitrary[Array[Byte]] = Arbitrary(
    for
      name <- Gen.alphaStr.map(_.take(8)).suchThat(_.nonEmpty)
      age <- Gen.choose(0, 120)
    yield encode(TargetUser(name, age))
  )

  checkAll(
    "Jsoniter targeting — JsoniterPrism[TargetUser].field(_.name) targets the codec's own name",
    new TargetingTests[Array[Byte], String, Affine]:
      val laws = new TargetingLaws[Array[Byte], String, Affine]:
        val optic = JsoniterPrism[TargetUser].field(_.name)
        def read(s: Array[Byte]) = optic.getOption(s)
        val refGet = (s: Array[Byte]) => Some(readFromArray[TargetUser](s).name)
        val refReplace = (s: Array[Byte], name: String) =>
          writeToArray(readFromArray[TargetUser](s).copy(name = name))
        val eqv = (a: Array[Byte], b: Array[Byte]) => Arrays.equals(a, b)
    .targeting(using arbUserBytes, summon[Arbitrary[String]], summon[Cogen[String]]),
  )

/** Fixture for [[JsoniterTargetingSuite]] — top level so `JsonCodecMaker.make` derives a codec for
  * a plain product type.
  */
final case class TargetUser(name: String, age: Int)

object TargetUser:
  given JsonValueCodec[TargetUser] = JsonCodecMaker.make
