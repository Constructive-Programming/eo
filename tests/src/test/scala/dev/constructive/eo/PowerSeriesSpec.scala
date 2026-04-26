package dev.constructive.eo

import scala.collection.immutable.ArraySeq

import cats.instances.arraySeq.*
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import optics.{Lens, Optic, Traversal}
import optics.Optic.*
import data.PowerSeries
import data.PowerSeries.given

import scala.language.implicitConversions

/** Behaviour-level spec for `PowerSeries`, `Traversal.each`, and the
  * `Composer[Tuple2 → PowerSeries]` bridge.
  *
  * '''2026-04-25 consolidation.''' 12 → 5 named blocks. Pre-image had:
  *
  *   - Three Traversal.each specs (identity / distribute / replace) — collapsed into one.
  *   - Three Composer chain specs (length-1 vector / round-trip / agreement) — collapsed.
  *   - Two empty-array edge cases — collapsed.
  *   - Single-element + 3+ andThen chain regression specs stay separate.
  */
class PowerSeriesSpec extends Specification with ScalaCheck:

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
        i <- Arbitrary.arbitrary[Int]
        n <- Arbitrary.arbitrary[String]
        ps <- Gen.listOfN(3, arbPhone.arbitrary)
      yield Person(i, n, ArraySeq.from(ps))
    )

  private val personPhones =
    Lens[Person, ArraySeq[Phone]](_.phones, (s, b) => s.copy(phones = b))
      .andThen(Traversal.each[ArraySeq, Phone])

  private val phoneIsMobile =
    Lens[Phone, Boolean](_.isMobile, (s, b) => s.copy(isMobile = b))

  private val personAllMobiles =
    personPhones.andThen(phoneIsMobile)

  // covers: leave structure unchanged under modify(identity), distribute a modify
  // across every phone, replace propagates through the whole ArraySeq
  "Traversal.each: identity / distribute modify / replace propagation" >> {
    val identityOk = forAll((p: Person) => personPhones.modify(identity[Phone])(p) == p)

    val distributeOk = forAll { (p: Person) =>
      val toggled = personPhones
        .modify(ph => Phone(!ph.isMobile, ph.number))(p)
      toggled.phones.zip(p.phones).forall { (after, before) =>
        after.isMobile != before.isMobile && after.number == before.number
      }
    }

    val replaceOk = forAll { (p: Person) =>
      val replaced = personAllMobiles.replace(false)(p)
      replaced.phones.forall(ph => ph.isMobile == false)
    }

    identityOk && distributeOk && replaceOk
  }

  // covers: lift a Lens into a PowerSeries optic whose inner Vector has size 1,
  // round-trip modify(identity) through a PowerSeries-lifted Lens,
  // modify-through-morph agrees with modify on the original Lens
  "Tuple2 → PowerSeries composer: length-1 inner vector / identity round-trip / modify agreement" >> {
    val lens = Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    val morphd = lens.morph[PowerSeries]

    val sizeOk = forAll { (p: (Int, String)) => morphd.to(p).vs.length == 1 }
    val identityOk = forAll { (p: (Int, String)) => morphd.modify(identity[Int])(p) == p }
    val agreeOk =
      forAll { (p: (Int, String), f: Int => Int) => morphd.modify(f)(p) == lens.modify(f)(p) }

    sizeOk && identityOk && agreeOk
  }

  // covers: leave the container unchanged under modify(identity) on empty,
  // still leave the container unchanged under a non-identity modify on empty,
  // apply the modify exactly once on a single-element ArraySeq
  "Traversal.each edge cases: empty + single-element ArraySeq" >> {
    val empty = Person(0, "noone", ArraySeq.empty[Phone])
    val emptyIdOk = personPhones.modify(identity[Phone])(empty) == empty
    val emptyConstOk = personPhones.modify((_: Phone) => Phone(true, "x"))(empty) == empty

    val one = Person(1, "solo", ArraySeq(Phone(false, "555-0001")))
    val after = personPhones.modify(ph => Phone(!ph.isMobile, ph.number))(one)
    val singleOk = after.phones.length == 1 &&
      after.phones(0).isMobile == true &&
      after.phones(0).number == "555-0001"

    emptyIdOk && emptyConstOk && singleOk
  }

  // ---- 3+ .andThen chain regression ----------------------------------

  case class Addr2(zip: String)
  case class Ord2(ship: Addr2)
  case class Usr2(orders: List[Ord2])

  private val threeAndThenChain =
    Lens[Usr2, List[Ord2]](_.orders, (u, os) => u.copy(orders = os))
      .andThen(Traversal.each[List, Ord2])
      .andThen(Lens[Ord2, Addr2](_.ship, (o, a) => o.copy(ship = a)))
      .andThen(Lens[Addr2, String](_.zip, (a, z) => a.copy(zip = z)))

  // covers: preserve the order of a 3-element list under modify(identity), preserve
  // the order of every N-element list (N = 0..5) under modify(identity), apply a
  // non-identity modify to every list position
  "3+ andThen chain: order-preserving modify (N=0..5) + non-identity modify positions" >> {
    val identityOk = (0 to 5).forall { n =>
      val items = (0 until n)
        .map(i => Ord2(Addr2(('A' + i).toChar.toString)))
        .toList
      val u = Usr2(items)
      threeAndThenChain.modify(identity[String])(u) == u
    }

    val u = Usr2(List(Ord2(Addr2("a")), Ord2(Addr2("b")), Ord2(Addr2("c"))))
    val out = threeAndThenChain.modify((z: String) => z.toUpperCase)(u)
    val nonIdOk = out.orders.map(_.ship.zip) == List("A", "B", "C")

    identityOk && nonIdOk
  }
