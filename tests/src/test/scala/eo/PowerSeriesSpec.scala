package eo

import optics.{Lens, Optic, Traversal}
import optics.Optic.*
import data.{PowerSeries, Vect}
import data.PowerSeries.given

import cats.instances.arraySeq.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scala.collection.immutable.ArraySeq

/** Behaviour-level spec for `PowerSeries`, `Traversal.powerEach`, and
  * the `Composer[Tuple2 → PowerSeries]` bridge. Covers scenarios that
  * the discipline suite (`PowerSeriesLaws` in `eo.laws.data`) does
  * not — specifically, end-to-end `modify` / `replace` semantics on
  * optics that flow through the PowerSeries carrier.
  *
  * Absorbs the prior `Unthreaded.scala` `@main` demonstration into
  * real property assertions so it participates in CI.
  */
class PowerSeriesSpec extends Specification with ScalaCheck:

  // ---- Fixtures: a nested Person/Phone ADT that exercises the same
  //      optic chain as the old Unthreaded example, minus Circe. ----

  case class Phone(isMobile: Boolean, number: String)
  case class Person(id: Int, name: String, phones: ArraySeq[Phone])

  private given arbPhone: Arbitrary[Phone] =
    Arbitrary(
      for
        m <- Arbitrary.arbitrary[Boolean]
        n <- Arbitrary.arbitrary[String]
      yield Phone(m, n)
    )

  private given arbPerson: Arbitrary[Person] =
    Arbitrary(
      for
        i  <- Arbitrary.arbitrary[Int]
        n  <- Arbitrary.arbitrary[String]
        ps <- Gen.listOfN(3, arbPhone.arbitrary)
      yield Person(i, n, ArraySeq.from(ps))
    )

  // ---- Optic chain: Person → phones → each Phone → isMobile -----

  private val personPhones =
    Lens[Person, ArraySeq[Phone]](
      _.phones, (s, b) => s.copy(phones = b),
    )
      .morph[PowerSeries]
      .andThen[Phone, Phone](Traversal.powerEach[ArraySeq, Phone])

  private val phoneIsMobile =
    Lens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b))

  private val personAllMobiles =
    personPhones.andThen(phoneIsMobile.morph[PowerSeries])

  // ---- `powerEach` behaviour ------------------------------------

  "Traversal.powerEach" should {
    "leave structure unchanged under modify(identity)" >> {
      forAll((p: Person) =>
        personPhones.modify(identity[Phone])(p) == p
      )
    }

    "distribute a modify across every phone" >> {
      forAll((p: Person) =>
        val toggled = personPhones
          .modify(ph => Phone(!ph.isMobile, ph.number))(p)
        toggled.phones.zip(p.phones).forall { (after, before) =>
          after.isMobile != before.isMobile && after.number == before.number
        }
      )
    }

    "replace propagates through the whole ArraySeq" >> {
      forAll((p: Person) =>
        val replaced =
          personAllMobiles.replace(false)(p)
        replaced.phones.forall(ph => ph.isMobile == false)
      )
    }
  }

  // ---- Composer chain round-trip on a single-element container --

  "Tuple2 → PowerSeries composer" should {
    "lift a Lens into a PowerSeries optic whose inner Vect has size 1" >> {
      val lens   = Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
      val morphd = lens.morph[PowerSeries]
      forAll((p: (Int, String)) =>
        val vect = morphd.to(p).ps._2
        vect.size == 1
      )
    }

    "round-trip modify(identity) through a PowerSeries-lifted Lens" >> {
      val lens   = Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
      val morphd = lens.morph[PowerSeries]
      forAll((p: (Int, String)) => morphd.modify(identity[Int])(p) == p)
    }

    "modify-through-morph agrees with modify on the original Lens" >> {
      val lens   = Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
      val morphd = lens.morph[PowerSeries]
      forAll((p: (Int, String), f: Int => Int) =>
        morphd.modify(f)(p) == lens.modify(f)(p)
      )
    }
  }

  // ---- Empty and single-element ArraySeq edge cases -------------

  "Traversal.powerEach on an empty ArraySeq" should {
    "leave the container unchanged under modify(identity)" >> {
      val empty = Person(0, "noone", ArraySeq.empty[Phone])
      personPhones.modify(identity[Phone])(empty) == empty
    }

    "still leave the container unchanged under a non-identity modify" >> {
      val empty = Person(0, "noone", ArraySeq.empty[Phone])
      personPhones.modify((_: Phone) => Phone(true, "x"))(empty) == empty
    }
  }

  "Traversal.powerEach on a single-element ArraySeq" should {
    "apply the modify exactly once" >> {
      val one = Person(1, "solo", ArraySeq(Phone(false, "555-0001")))
      val after = personPhones
        .modify(ph => Phone(!ph.isMobile, ph.number))(one)
      after.phones.length == 1 &&
      after.phones(0).isMobile == true &&
      after.phones(0).number == "555-0001"
    }
  }
