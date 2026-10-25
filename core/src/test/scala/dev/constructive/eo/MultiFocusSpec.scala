package dev.constructive.eo

import scala.language.implicitConversions

import cats.instances.list.given
import dev.constructive.eo.compose.*
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.{Direct, ModifyF, MultiFocus}
import data.MultiFocus.{collectList, collectMap}
import optics.*
import optics.Optic.*

// Top-level ADTs — placed outside the class so the macro-friendly construction patterns work.
private case class Phone(isMobile: Boolean, number: String)
private case class Person(name: String, phones: List[Phone])

/** Spike-scope spec for the unified `MultiFocus[F]` carrier — pins down the load-bearing slice the
  * spike claims to deliver: same-carrier `.andThen` (Lens → MultiFocus → Lens with singleton
  * fast-path), the Iso bridge (`forgetful2multifocus`), and the ModifyF widening
  * (`multifocus2modify`). Plus the .collect universal in its two derivations. `.modify` /
  * `.modifyA` / `.all` across F are law-checked in `tests/` (`ForgetfulFunctor` /
  * `ForgetfulTraverse` per carrier in OpticsLawsSpec, D2 `ModifyAListTests` and E2 `allIsFoci` in
  * EoSpecificLawsSpec).
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

  // ----- Composer bridges: Direct → MultiFocus[List] + MultiFocus[List] → ModifyF ----
  //
  // 2026-04-29 consolidation: 2 same-shape composer-bridge tests → 1 composite.

  // covers: Composer[Direct, MultiFocus[List]] (forgetful2multifocus) round-trips an
  //   Iso's .modify through the bridge — 5 → 6 (forward) → 12 (×2) → 11 (back);
  //   Composer[MultiFocus[F], ModifyF] (multifocus2modify) widens MultiFocus[List]'s
  //   element-wise modify to ModifyF and preserves the modify byte-for-byte
  "Composer bridges: Direct → MultiFocus[List] (Iso round-trip) + MultiFocus[List] → ModifyF" >> {
    val iso: Optic[Int, Int, Int, Int, Direct] =
      Iso[Int, Int, Int, Int](_ + 1, (b: Int) => b - 1)
    val asMF: Optic[Int, Int, Int, Int, MultiFocus[List]] =
      summon[Composer[Direct, MultiFocus[List]]].to(iso)
    val isoOk = asMF.modify(_ * 2)(5) == 11

    val k: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
      MultiFocus.apply[List, Int]
    val asModify: Optic[List[Int], List[Int], Int, Int, ModifyF] =
      summon[Composer[MultiFocus[List], ModifyF]].to(k)
    val modifyOk = asModify.modify(_ * 10)(List(1, 2, 3)) == List(10, 20, 30)

    isoOk && modifyOk
  }
