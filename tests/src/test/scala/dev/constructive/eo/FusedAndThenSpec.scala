package dev.constructive.eo

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.{BijectionIso, GetReplaceLens, MendTearPrism, Optional, PickMendPrism}
import data.Affine

/** Dedicated coverage for the 21 fused `.andThen` overloads on the concrete optic subclasses
  * (`BijectionIso`, `MendTearPrism`, `PickMendPrism`, `GetReplaceLens`, `Optional`).
  *
  * '''2026-04-25 consolidation.''' 17 → 5 named blocks, one per outer-class group. Each block
  * exercises every fused overload of that outer (eg BijectionIso.andThen(BijectionIso/GRL/MTP/Opt)
  * collapsed into one BijectionIso group). Per-overload semantics asserted in a single composite
  * Result.
  */
class FusedAndThenSpec extends Specification:

  // ---- Shared fixtures ---------------------------------------------

  case class Wrapper(inner: Inner)
  case class Inner(n: Int, tag: String)

  private val wrapperIso: BijectionIso[Wrapper, Wrapper, Inner, Inner] =
    new BijectionIso[Wrapper, Wrapper, Inner, Inner](_.inner, Wrapper(_))

  private val innerIso: BijectionIso[Inner, Inner, (Int, String), (Int, String)] =
    new BijectionIso[Inner, Inner, (Int, String), (Int, String)](
      i => (i.n, i.tag),
      t => Inner(t._1, t._2),
    )

  private val innerLens: GetReplaceLens[Inner, Inner, Int, Int] =
    new GetReplaceLens[Inner, Inner, Int, Int](_.n, (i, n2) => i.copy(n = n2))

  private val evenMtPrism: MendTearPrism[Int, Int, Int, Int] =
    new MendTearPrism[Int, Int, Int, Int](
      tear = n => if n % 2 == 0 then Right(n / 2) else Left(n),
      mend = _ * 2,
    )

  private val evenPmPrism: PickMendPrism[Int, Int, Int] =
    new PickMendPrism[Int, Int, Int](
      pick = n => if n % 2 == 0 then Some(n / 2) else None,
      mend = _ * 2,
    )

  private val adultOpt: Optional[Inner, Inner, Int, Int] =
    Optional[Inner, Inner, Int, Int, Affine](
      getOrModify = i => if i.n >= 18 then Right(i.n) else Left(i),
      reverseGet = (i, n2) => i.copy(n = n2),
    )

  // covers: BijectionIso.andThen(BijectionIso) fuses two isos,
  // BijectionIso.andThen(GetReplaceLens) produces a GetReplaceLens,
  // BijectionIso.andThen(MendTearPrism) produces a MendTearPrism,
  // BijectionIso.andThen(Optional) produces an Optional
  "BijectionIso.andThen — 4 fused overloads (Iso/Lens/Prism/Optional)" >> {
    val isoChain = wrapperIso.andThen(innerIso)
    val isoOk = (isoChain.get(Wrapper(Inner(5, "x"))) === ((5, "x")))
      .and(isoChain.reverseGet((10, "y")) === Wrapper(Inner(10, "y")))

    val lensChain: GetReplaceLens[Wrapper, Wrapper, Int, Int] = wrapperIso.andThen(innerLens)
    val lensOk = (lensChain.get(Wrapper(Inner(7, "t"))) === 7)
      .and(lensChain.enplace(Wrapper(Inner(7, "t")), 11) === Wrapper(Inner(11, "t")))

    val intIso: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ * 3, _ / 3)
    val prismChain: MendTearPrism[Int, Int, Int, Int] = intIso.andThen(evenMtPrism)
    val prismOk = (prismChain.tear(4) === Right(6))
      .and(prismChain.tear(3) === Left(3))
      .and(prismChain.mend(4) === intIso.reverseGet(evenMtPrism.mend(4)))

    val optChain: Optional[Wrapper, Wrapper, Int, Int] = wrapperIso.andThen(adultOpt)
    val optOk = (optChain.getOrModify(Wrapper(Inner(20, "a"))) === Right(20))
      .and(optChain.getOrModify(Wrapper(Inner(15, "m"))) === Left(Wrapper(Inner(15, "m"))))
      .and(optChain.reverseGet(Wrapper(Inner(20, "a")), 99) === Wrapper(Inner(99, "a")))

    isoOk.and(lensOk).and(prismOk).and(optOk)
  }

  // covers: MendTearPrism.andThen(MendTearPrism) fuses two prisms,
  // MendTearPrism.andThen(BijectionIso) produces a MendTearPrism,
  // MendTearPrism.andThen(PickMendPrism) fuses when A = B,
  // MendTearPrism.andThen(GetReplaceLens) produces an Optional,
  // MendTearPrism.andThen(Optional) produces an Optional
  "MendTearPrism.andThen — 5 fused overloads (Prism/Iso/PMPrism/Lens/Optional)" >> {
    val mtChain: MendTearPrism[Int, Int, Int, Int] = evenMtPrism.andThen(evenMtPrism)
    val mtOk = (mtChain.tear(12) === Right(3))
      .and(mtChain.tear(10) === Left(10))
      .and(mtChain.tear(7) === Left(7))
      .and(mtChain.mend(4) === 16)

    val intIso: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ + 100, _ - 100)
    val isoChain: MendTearPrism[Int, Int, Int, Int] = evenMtPrism.andThen(intIso)
    val isoOk = (isoChain.tear(12) === Right(106))
      .and(isoChain.tear(7) === Left(7))
      .and(isoChain.mend(106) === 12)

    val pmChain: MendTearPrism[Int, Int, Int, Int] = evenMtPrism.andThen(evenPmPrism)
    val pmOk = (pmChain.tear(12) === Right(3))
      .and(pmChain.tear(10) === Left(10))
      .and(pmChain.tear(7) === Left(7))

    val nonNegInnerPrism: MendTearPrism[Inner, Inner, Inner, Inner] =
      new MendTearPrism[Inner, Inner, Inner, Inner](
        tear = i => if i.n >= 0 then Right(i) else Left(i),
        mend = identity,
      )
    val lensChain: Optional[Inner, Inner, Int, Int] = nonNegInnerPrism.andThen(innerLens)
    val lensOk = (lensChain.getOrModify(Inner(5, "x")) === Right(5))
      .and(lensChain.getOrModify(Inner(-3, "y")) === Left(Inner(-3, "y")))
      .and(lensChain.reverseGet(Inner(5, "x"), 99) === Inner(99, "x"))

    val optChain: Optional[Inner, Inner, Int, Int] = nonNegInnerPrism.andThen(adultOpt)
    val optOk = (optChain.getOrModify(Inner(20, "a")) === Right(20))
      .and(optChain.getOrModify(Inner(-5, "neg")) === Left(Inner(-5, "neg")))
      .and(optChain.getOrModify(Inner(15, "m")) === Left(Inner(15, "m")))

    mtOk.and(isoOk).and(pmOk).and(lensOk).and(optOk)
  }

  // covers: PickMendPrism.andThen(PickMendPrism) fuses two PMPrisms (pick + mend matrix),
  //   PickMendPrism.andThen(MendTearPrism) requires A = B and produces a MTPrism,
  //   PickMendPrism.andThen(BijectionIso) produces a PMPrism (pick / mend through iso);
  //   GetReplaceLens.andThen(GetReplaceLens) fuses two lenses (get / enplace / modify),
  //   GetReplaceLens.andThen(BijectionIso) produces a GRL,
  //   GetReplaceLens.andThen(MendTearPrism) produces an Optional (hit / miss),
  //   GetReplaceLens.andThen(PickMendPrism) produces an Optional (A = B requirement),
  //   GetReplaceLens.andThen(Optional) produces an Optional (hit / miss / reverseGet)
  "PickMendPrism + GetReplaceLens .andThen — 8 fused overloads" >> {
    // ---- PickMendPrism group ----
    val pmChain: PickMendPrism[Int, Int, Int] = evenPmPrism.andThen(evenPmPrism)
    val pmOk = (pmChain.pick(16) === Some(4))
      .and(pmChain.pick(10) === None)
      .and(pmChain.pick(3) === None)
      .and(pmChain.mend(4) === 16)

    val pmMtChain: MendTearPrism[Int, Int, Int, Int] = evenPmPrism.andThen(evenMtPrism)
    val pmMtOk = (pmMtChain.tear(16) === Right(4))
      .and(pmMtChain.tear(10) === Left(10))
      .and(pmMtChain.tear(3) === Left(3))
      .and(pmMtChain.mend(4) === 16)

    val intIsoForPm: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ * 10, _ / 10)
    val pmIsoChain: PickMendPrism[Int, Int, Int] = evenPmPrism.andThen(intIsoForPm)
    val pmIsoOk = (pmIsoChain.pick(16) === Some(80))
      .and(pmIsoChain.pick(7) === None)
      .and(pmIsoChain.mend(80) === 16)

    // ---- GetReplaceLens group ----
    val outerLens: GetReplaceLens[Wrapper, Wrapper, Inner, Inner] =
      new GetReplaceLens[Wrapper, Wrapper, Inner, Inner](_.inner, (w, i) => w.copy(inner = i))

    val lensChain: GetReplaceLens[Wrapper, Wrapper, Int, Int] = outerLens.andThen(innerLens)
    val lensOk = (lensChain.get(Wrapper(Inner(7, "x"))) === 7)
      .and(lensChain.enplace(Wrapper(Inner(7, "x")), 11) === Wrapper(Inner(11, "x")))
      .and(lensChain.modify(_ + 1)(Wrapper(Inner(7, "x"))) === Wrapper(Inner(8, "x")))

    val isoChain: GetReplaceLens[Wrapper, Wrapper, (Int, String), (Int, String)] =
      outerLens.andThen(innerIso)
    val isoOk = (isoChain.get(Wrapper(Inner(5, "a"))) === ((5, "a")))
      .and(isoChain.enplace(Wrapper(Inner(5, "a")), (9, "z")) === Wrapper(Inner(9, "z")))

    val innerPrism: MendTearPrism[Inner, Inner, Int, Int] =
      new MendTearPrism[Inner, Inner, Int, Int](
        tear = i => if i.n >= 0 then Right(i.n) else Left(i),
        mend = n => Inner(n, "from-mend"),
      )
    val mtChain: Optional[Wrapper, Wrapper, Int, Int] = outerLens.andThen(innerPrism)
    val mtOk = (mtChain.getOrModify(Wrapper(Inner(5, "ok"))) === Right(5))
      .and(mtChain.getOrModify(Wrapper(Inner(-1, "neg"))) === Left(Wrapper(Inner(-1, "neg"))))

    val pmInnerLens: GetReplaceLens[Inner, Inner, Int, Int] = innerLens
    val pmGrlChain: Optional[Inner, Inner, Int, Int] = pmInnerLens.andThen(evenPmPrism)
    val pmGrlOk = (pmGrlChain.getOrModify(Inner(16, "x")) === Right(8))
      .and(pmGrlChain.getOrModify(Inner(7, "y")) === Left(Inner(7, "y")))

    val optChain: Optional[Wrapper, Wrapper, Int, Int] = outerLens.andThen(adultOpt)
    val optOk = (optChain.getOrModify(Wrapper(Inner(30, "a"))) === Right(30))
      .and(optChain.getOrModify(Wrapper(Inner(15, "m"))) === Left(Wrapper(Inner(15, "m"))))
      .and(optChain.reverseGet(Wrapper(Inner(30, "a")), 99) === Wrapper(Inner(99, "a")))

    pmOk.and(pmMtOk).and(pmIsoOk)
      .and(lensOk).and(isoOk).and(mtOk).and(pmGrlOk).and(optOk)
  }
