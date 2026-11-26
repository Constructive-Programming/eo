package dev.constructive.eo.avro
package laws

import scala.language.implicitConversions

import dev.constructive.eo.avro.vulcan.Comp
import dev.constructive.eo.avro.vulcan.DivergentCodecs.encodeBytes
import dev.constructive.eo.avro.vulcan.given
import dev.constructive.eo.data.Affine
import dev.constructive.eo.laws.discipline.TargetingTests
import dev.constructive.eo.laws.{SeamLaws, TargetingLaws}
import dev.constructive.eo.optics.Optic
import java.util.Arrays
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

import AvroSpecFixtures.Person

/** Issue #99 on the avro wire carrier: `.field(_.x)` checked against the CODEC's own accessor.
  *
  * The reference here is written without the prism: `AvroCodec.decodeValue[A]` (the carrier's
  * whole-record decode, driven by the codec's field list) reads the case field, and the same decode
  * → `copy` → encode sequence writes it. Nothing in that reference knows about
  * `AvroWalk.fieldNameAt`, the name rung or the positional rung — which is exactly what makes it
  * able to see a resolution mistake.
  *
  * Two halves:
  *
  *   - a POSITIVE discipline registration on a kindlings-derived (1:1) fixture, where both rungs
  *     agree and the optic really does target the codec's slot;
  *   - a NEGATIVE control on `Comp`, the RESIDUAL b6 shape from `ResolutionResidualSpec`: the codec
  *     never stores `b`, so the probe abstains and position aims `.field(_.b)` at the `x_digest`
  *     column. That optic is intrinsically lawful ([[SeamLaws]] passes on it) and fails
  *     [[TargetingLaws]] — the whole point of the ruleset. (It was RESIDUAL b2 on `Ev` until the
  *     differential probe, #100, closed that one.)
  *
  * `Array[Byte]` equality is not structural, so `Arrays.equals` is injected as the law's `eqv`.
  */
class AvroTargetingSpec extends Specification with Discipline with ScalaCheck:

  private def decode[A](bytes: Array[Byte])(using AvroCodec[A]): Option[A] =
    AvroCodec.decodeValue(bytes).toOption

  private val bytesEq: (Array[Byte], Array[Byte]) => Boolean = Arrays.equals

  /** Canonical Person encodings — every generated source both hits the reference and re-encodes to
    * itself, so the write half of the law is not vacuous.
    */
  private val arbPersonBytes: Arbitrary[Array[Byte]] = Arbitrary(
    for
      name <- Gen.alphaStr.map(_.take(8)).suchThat(_.nonEmpty)
      age <- Gen.choose(0, 120)
    yield encodeBytes(Person(name, age))
  )

  // ---- positive: a 1:1 codec, both rungs agree ------------------------------

  checkAll(
    "Avro targeting — codecPrism[Person].field(_.name) targets the codec's own name",
    new TargetingTests[Array[Byte], String, Affine]:
      val laws = new TargetingLaws[Array[Byte], String, Affine]:
        val optic = codecPrism[Person].field(_.name)
        def read(s: Array[Byte]) = optic.getOption(s)
        val refGet = (s: Array[Byte]) => decode[Person](s).map(_.name)
        val refReplace = (s: Array[Byte], name: String) =>
          decode[Person](s) match
            case Some(p) => encodeBytes(p.copy(name = name))
            case None    => s
        val eqv = bytesEq
    .targeting(using arbPersonBytes, summon[Arbitrary[String]], summon[Cogen[String]]),
  )

  // ---- negative: RESIDUAL b6, lawful but mis-targeted ----------------------

  "RESIDUAL b6 — the mis-targeted field optic is intrinsically lawful yet fails TargetingLaws" >> {
    // {x_a, x_digest, x_c}: the codec never stores `b` (it writes `a + "/" + b` into `x_digest`),
    // so the probe has no column to find and abstains, and position lands `.field(_.b)` on the
    // digest. The optic reads and writes that slot consistently, which is all the intrinsic laws
    // can check.
    val misAimed: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      codecPrism[Comp].field(_.b)

    val bytes = encodeBytes(Comp("A0", "B0", "C0"))

    // Intrinsic laws hold — that is why no optic law family could see this.
    val seam = new SeamLaws[Array[Byte], String]:
      val optic = misAimed
      val eqv = bytesEq
    val intrinsicallyLawful =
      seam.seamModifyIdentity(bytes) &&
        seam.seamComposeModify(bytes, _ + "1", _ + "2") &&
        seam.seamReplaceOverwrite(bytes, "X", "Y")

    val targeting = new TargetingLaws[Array[Byte], String, Affine]:
      val optic = misAimed
      def read(bytes: Array[Byte]) = misAimed.getOption(bytes)
      val refGet = (bytes: Array[Byte]) => decode[Comp](bytes).map(_.b)
      val refReplace = (s: Array[Byte], b: String) =>
        decode[Comp](s) match
          case Some(c) => encodeBytes(c.copy(b = b))
          case None    => s
      val eqv = bytesEq

    intrinsicallyLawful must beTrue
    (misAimed.getOption(bytes) must beSome("A0/B0"))
      .and(targeting.readTargets(bytes) must beFalse)
      .and(targeting.writeTargets(bytes, "B1") must beFalse)
      .and(targeting.modifyTargets(bytes, _ + "!") must beFalse)
  }
