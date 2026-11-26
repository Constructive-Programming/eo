package dev.constructive.eo.avro
package laws

import scala.language.implicitConversions

import dev.constructive.eo.avro.vulcan.DivergentCodecs.encodeBytes
import dev.constructive.eo.avro.vulcan.Ev
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
  *   - a NEGATIVE control on `Ev`, the RESIDUAL b2 shape from `ResolutionResidualSpec`: the vulcan
  *     field list renames AND reorders, so the resolver abstains to position and
  *     `.field(_.occurredAt)` reads `seq_no`. That optic is intrinsically lawful ([[SeamLaws]]
  *     passes on it) and fails [[TargetingLaws]] — the whole point of the ruleset.
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

  // ---- negative: RESIDUAL b2, lawful but mis-targeted ----------------------

  "RESIDUAL b2 — the mis-targeted field optic is intrinsically lawful yet fails TargetingLaws" >> {
    // {seq_no, event_ts} vs {occurredAt, seqNo}: no name survives (`occurredAt` has no match, so
    // the all-or-nothing name rung abstains) and the field list is reversed, so position decides
    // and is wrong. `.field(_.occurredAt)` reads the sequence number.
    val misAimed: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      codecPrism[Ev].field(_.occurredAt)

    val bytes = encodeBytes(Ev(1700000000000L, 7L))

    // Intrinsic laws hold — that is why no optic law family could see this.
    val seam = new SeamLaws[Array[Byte], Long]:
      val optic = misAimed
      val eqv = bytesEq
    val intrinsicallyLawful =
      seam.seamModifyIdentity(bytes) &&
        seam.seamComposeModify(bytes, _ + 1, _ + 1) &&
        seam.seamReplaceOverwrite(bytes, 5L, 6L)

    val targeting = new TargetingLaws[Array[Byte], Long, Affine]:
      val optic = misAimed
      def read(bytes: Array[Byte]) = misAimed.getOption(bytes)
      val refGet = (bytes: Array[Byte]) => decode[Ev](bytes).map(_.occurredAt)
      val refReplace = (s: Array[Byte], occurredAt: Long) =>
        decode[Ev](s) match
          case Some(e) => encodeBytes(e.copy(occurredAt = occurredAt))
          case None    => s
      val eqv = bytesEq

    intrinsicallyLawful must beTrue
    (misAimed.getOption(bytes) must beSome(7L))
      .and(targeting.readTargets(bytes) must beFalse)
      .and(targeting.writeTargets(bytes, 42L) must beFalse)
      .and(targeting.modifyTargets(bytes, _ + 1) must beFalse)
  }
