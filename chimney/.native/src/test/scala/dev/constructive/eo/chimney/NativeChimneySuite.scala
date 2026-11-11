// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo
package chimney

import io.scalaland.chimney.{partial, PartialTransformer, Transformer}
import org.scalacheck.{Arbitrary, Cogen, Gen}

import generics.lens
import laws.{GetterLaws, IsoLaws, PrismLaws}
import laws.discipline.{GetterTests, IsoTests, PrismTests}

// Top-level fixtures (macro-built `new T(...)` has no outer accessor); trimmed from `samples`.
final case class NPerson(name: String, age: Int)
final case class NPersonRow(age: Int, name: String)

final case class NCents private (value: Long)

object NCents:
  def from(value: Long): Option[NCents] = Option.when(value >= 0)(new NCents(value))
  given Transformer[NCents, Long] = _.value

  given PartialTransformer[Long, NCents] =
    PartialTransformer(l => partial.Result.fromOptionOrString(from(l), s"negative amount: $l"))

final case class NAccount(owner: String, balance: NCents)
final case class NAccountDto(owner: String, balance: Long)

/** Scala Native twin of the core of `ChimneyLawsSpec` (specs2 has no Native artifact). */
class NativeChimneySuite extends munit.DisciplineSuite:

  given Arbitrary[NPerson] = Arbitrary(Gen.resultOf(NPerson.apply))
  given Arbitrary[NPersonRow] = Arbitrary(Gen.resultOf(NPersonRow.apply))
  given Cogen[NPersonRow] = Cogen[(Int, String)].contramap(r => (r.age, r.name))

  // Negative balances reach the prism too, so the miss side of every law is exercised.
  given Arbitrary[NAccountDto] =
    Arbitrary(Gen.resultOf(NAccountDto.apply)(using summon, Arbitrary(Gen.choose(-50L, 50L))))

  given Arbitrary[NAccount] = Arbitrary(
    for
      owner <- Arbitrary.arbitrary[String]
      cents <- Gen.choose(0L, 1000000L)
    yield NAccount(owner, NCents.from(cents).get)
  )

  given Cogen[NAccount] = Cogen[(String, Long)].contramap(a => (a.owner, a.balance.value))

  checkAll(
    "Chimney.iso[NPerson, NPersonRow]",
    new IsoTests[NPerson, NPersonRow]:
      val laws = new IsoLaws[NPerson, NPersonRow]:
        val iso = Chimney.iso[NPerson, NPersonRow]
    .iso,
  )

  checkAll(
    "Chimney.prism[NAccount, NAccountDto]",
    new PrismTests[NAccountDto, NAccount]:
      val laws = new PrismLaws[NAccountDto, NAccount]:
        val prism = Chimney.prism[NAccount, NAccountDto]
    .prism,
  )

  checkAll(
    "Chimney.getter[NPerson, NPersonRow] ∘ lens(_.age)",
    new GetterTests[NPerson, Int]:
      val laws = new GetterLaws[NPerson, Int]:
        val getter = Chimney.getter[NPerson, NPersonRow].andThen(lens[NPersonRow](_.age))
        val reference = (p: NPerson) => p.age
    .getter,
  )

  test("resultFold keeps chimney's errors on a miss") {
    val decode = Chimney.resultFold[NAccountDto, NAccount]
    assertEquals(decode.getOption(NAccountDto("a", 5L)).map(_.owner), Some("a"))
    assertEquals(decode.getOption(NAccountDto("b", -5L)), None)
    assert(decode.either(NAccountDto("b", -5L)).isLeft)
  }
// CPD-ON
