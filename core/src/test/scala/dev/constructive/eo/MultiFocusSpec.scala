package dev.constructive.eo

import scala.language.implicitConversions

import cats.data.Chain
import cats.instances.list.given
import cats.instances.option.given
import cats.instances.vector.given
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.{Forgetful, MultiFocus, SetterF}
import data.MultiFocus.given
import data.MultiFocus.{collectList, collectMap}
import optics.*
import optics.Optic.*

// Top-level ADTs — placed outside the class so the macro-friendly construction patterns work.
private case class Phone(isMobile: Boolean, number: String)
private case class Person(name: String, phones: List[Phone])

/** Spike-scope spec for the unified `MultiFocus[F]` carrier — pins down the load-bearing slice the
  * spike claims to deliver: `.modify` (Functor[F]), `.modifyA` (Traverse[F]), same-carrier
  * `.andThen` (Lens → MultiFocus → Lens with singleton fast-path), the Iso bridge
  * (`forgetful2multifocus`), and the SetterF widening (`multifocus2setter`). Plus the .collect
  * universal in its two derivations.
  */
class MultiFocusSpec extends Specification with ScalaCheck:

  // ----- Q3 probe: same-carrier .andThen, Lens → MultiFocus[List] → Lens ----
  // covers: tuple2multifocus singleton fast-path, mfAssoc same-F andThen.
  "Lens → MultiFocus[List] → Lens — singleton-classifier fast path .modify" >> {
    val phonesL: Optic[Person, Person, List[Phone], List[Phone], Tuple2] =
      Lens(_.phones, (s: Person, b: List[Phone]) => s.copy(phones = b))
    val isMobileL: Optic[Phone, Phone, Boolean, Boolean, Tuple2] =
      Lens(_.isMobile, (s: Phone, b: Boolean) => s.copy(isMobile = b))

    val chain: Optic[Person, Person, Boolean, Boolean, MultiFocus[List]] =
      MultiFocus.fromLensF(phonesL).andThen(isMobileL)

    val person0 = Person(
      "Alice",
      List(Phone(true, "n0"), Phone(false, "n1"), Phone(true, "n2")),
    )
    val flipped = chain.modify(!_)(person0)
    flipped.phones.map(_.isMobile) == List(false, true, false)
  }

  // ----- Q1 probe: collectMap (Functor-broadcast) and collectList (List-singleton) -----
  // covers: collectMap on a length-preserving F (Functor.map default), collectList for
  // the v1 cartesian-Reflector[List] semantics.
  "MultiFocus.apply[List] — collectMap (broadcast) and collectList (singleton)" >> {
    val k: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
      MultiFocus.apply[List, Int]

    // collectMap: length-preserving — sum broadcast back across the input.
    val sumBroadcast = k.collectMap[Int](_.sum)
    val mapOk = sumBroadcast(List(1, 2, 3)) == List(6, 6, 6)

    // collectList: cartesian-style singleton, matches the v1 Reflector[List] semantics.
    val sumSingle = k.collectList(_.sum)
    val singleOk = sumSingle(List(1, 2, 3)) == List(6)

    mapOk && singleOk
  }

  // ----- Functor + Traverse capability ----------------------------------
  // covers: ForgetfulFunctor[MultiFocus[F]] (.modify), ForgetfulTraverse[MultiFocus[F]]
  // (.modifyA, .all) on multiple F-instances.
  "MultiFocus.apply[F, A].modify / .modifyA across F in {List, Option, Vector, Chain}" >> {
    val kList = MultiFocus.apply[List, Int]
    val kOpt = MultiFocus.apply[Option, Int]
    val kVec = MultiFocus.apply[Vector, Int]
    val kChain = MultiFocus.apply[Chain, Int]

    val modList = forAll { (xs: List[Int]) => kList.modify(_ + 1)(xs) == xs.map(_ + 1) }
    val modOpt = forAll { (x: Option[Int]) => kOpt.modify(_ + 1)(x) == x.map(_ + 1) }
    val modVec = forAll { (xs: Vector[Int]) => kVec.modify(_ + 1)(xs) == xs.map(_ + 1) }
    val modChain = forAll { (xs: List[Int]) =>
      val ch = Chain.fromSeq(xs)
      kChain.modify(_ + 1)(ch) == Chain.fromSeq(xs.map(_ + 1))
    }

    // .modifyA — Traverse[F] path with Applicative[Option] effect.
    val modA = forAll { (xs: List[Int]) =>
      val res: Option[List[Int]] = kList.modifyA[Option](i => if i >= 0 then Some(i) else None)(xs)
      if xs.forall(_ >= 0) then res == Some(xs)
      else res == None
    }

    modList && modOpt && modVec && modChain && modA
  }

  // ----- Composer bridges: Forgetful → MultiFocus[List] + MultiFocus[List] → SetterF ----
  //
  // 2026-04-29 consolidation: 2 same-shape composer-bridge tests → 1 composite.

  // covers: Composer[Forgetful, MultiFocus[List]] (forgetful2multifocus) round-trips an
  //   Iso's .modify through the bridge — 5 → 6 (forward) → 12 (×2) → 11 (back);
  //   Composer[MultiFocus[F], SetterF] (multifocus2setter) widens MultiFocus[List]'s
  //   element-wise modify to SetterF and preserves the modify byte-for-byte
  "Composer bridges: Forgetful → MultiFocus[List] (Iso round-trip) + MultiFocus[List] → SetterF" >> {
    val iso: Optic[Int, Int, Int, Int, Forgetful] =
      Iso[Int, Int, Int, Int](_ + 1, (b: Int) => b - 1)
    val asMF: Optic[Int, Int, Int, Int, MultiFocus[List]] =
      summon[Composer[Forgetful, MultiFocus[List]]].to(iso)
    val isoOk = asMF.modify(_ * 2)(5) == 11

    val k: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
      MultiFocus.apply[List, Int]
    val asSetter: Optic[List[Int], List[Int], Int, Int, SetterF] =
      summon[Composer[MultiFocus[List], SetterF]].to(k)
    val setterOk = asSetter.modify(_ * 10)(List(1, 2, 3)) == List(10, 20, 30)

    isoOk && setterOk
  }
