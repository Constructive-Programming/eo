package dev.constructive.eo

import org.specs2.mutable.Specification

import optics.{BijectionIso, GetReplaceLens, MendTearPrism, Optional, PickMendPrism}
import data.Affine

/** Dedicated coverage for the 21 fused `.andThen` overloads on the concrete optic subclasses
  * (`BijectionIso`, `MendTearPrism`, `PickMendPrism`, `GetReplaceLens`, `Optional`). Each fused
  * overload bypasses the generic `AssociativeFunctor` / `Composer` path when both sides are known
  * concrete-typed at the call site; they're a silent runtime-performance feature.
  *
  * Unit 16 covered the four `Optional.andThen(_)` fused paths as part of the composition-gap top-3
  * closures. This spec closes the remaining 17 across the three other concrete classes. Each
  * scenario calls the fused path on a realistic 3-field fixture and asserts the observed modify
  * behaviour (for Lens/Iso/Optional-returning fused paths) or the Prism hit/miss split (for
  * Prism-returning fused paths).
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

  // ---- BijectionIso.andThen(_) — 4 fused overloads -----------------

  "BijectionIso.andThen(BijectionIso) fuses two isos" >> {
    val chained = wrapperIso.andThen(innerIso)
    chained.get(Wrapper(Inner(5, "x"))) === ((5, "x"))
    chained.reverseGet((10, "y")) === Wrapper(Inner(10, "y"))
  }

  "BijectionIso.andThen(GetReplaceLens) produces a GetReplaceLens" >> {
    val chained: GetReplaceLens[Wrapper, Wrapper, Int, Int] =
      wrapperIso.andThen(innerLens)
    chained.get(Wrapper(Inner(7, "t"))) === 7
    chained.enplace(Wrapper(Inner(7, "t")), 11) === Wrapper(Inner(11, "t"))
  }

  "BijectionIso.andThen(MendTearPrism) produces a MendTearPrism" >> {
    val intIso: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ * 3, _ / 3)
    val chained: MendTearPrism[Int, Int, Int, Int] =
      intIso.andThen(evenMtPrism)
    // 4 * 3 = 12 (even after iso swap? Actually Iso.to(4) = 12, then prism.tear(12) = Right(6).
    chained.tear(4) === Right(6)
    // 3 * 3 = 9, odd → Left(9 / 3 = 3 via iso.reverseGet on the unchanged value? Let's trace:
    // intIso.get(3) = 9; evenMtPrism.tear(9) = Left(9); intIso.reverseGet(9) = 3.
    chained.tear(3) === Left(3)
    chained.mend(4) === intIso.reverseGet(evenMtPrism.mend(4)) // 4*2 = 8, then 8/3 = 2
  }

  "BijectionIso.andThen(Optional) produces an Optional" >> {
    val chained: Optional[Wrapper, Wrapper, Int, Int] =
      wrapperIso.andThen(adultOpt)
    chained.getOrModify(Wrapper(Inner(20, "a"))) === Right(20)
    chained.getOrModify(Wrapper(Inner(15, "m"))) === Left(Wrapper(Inner(15, "m")))
    chained.reverseGet(Wrapper(Inner(20, "a")), 99) === Wrapper(Inner(99, "a"))
  }

  // ---- MendTearPrism.andThen(_) — 5 fused overloads ----------------

  "MendTearPrism.andThen(MendTearPrism) fuses two prisms" >> {
    // Outer: even → half. Inner: same shape.
    val chained: MendTearPrism[Int, Int, Int, Int] =
      evenMtPrism.andThen(evenMtPrism)
    // 12 → 6 → 3 (3 is odd, so inner tears Left). Result: Left(outer.mend(Left's inner)) = Left(6*2=12)?
    // Let's trace: outer.tear(12) = Right(6). inner.tear(6) = Right(3). So chained.tear(12) = Right(3).
    chained.tear(12) === Right(3)
    // 10 → 5 → odd → Left(5). Outer receives Left(5), rewraps via outer.mend(5) = 10.
    chained.tear(10) === Left(10)
    // 7 → odd → Left(7). Outer never calls inner; returns Left(7).
    chained.tear(7) === Left(7)
    // mend: inner.mend(4) = 8, outer.mend(8) = 16.
    chained.mend(4) === 16
  }

  "MendTearPrism.andThen(BijectionIso) produces a MendTearPrism" >> {
    val intIso: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ + 100, _ - 100)
    val chained: MendTearPrism[Int, Int, Int, Int] = evenMtPrism.andThen(intIso)
    // 12 → 6 (even, prism hit) → iso.get(6) = 106. So chained.tear(12) = Right(106).
    chained.tear(12) === Right(106)
    // 7 → odd → Left(7).
    chained.tear(7) === Left(7)
    // mend: iso.reverseGet(106) = 6, prism.mend(6) = 12.
    chained.mend(106) === 12
  }

  "MendTearPrism.andThen(PickMendPrism) fuses when A = B" >> {
    val chained: MendTearPrism[Int, Int, Int, Int] =
      evenMtPrism.andThen(evenPmPrism)
    // Outer: 12 → 6 (even, hit). Inner: 6 → 3 (even, hit). Chained: Right(3).
    chained.tear(12) === Right(3)
    // Outer: 10 → 5 (hit). Inner: 5 → odd → None. Chained miss-via-inner:
    // Left(outer.mend(outer's focus 5)) = Left(10).
    chained.tear(10) === Left(10)
    // Outer: 7 → odd → Left(7).
    chained.tear(7) === Left(7)
  }

  // Both MendTearPrism.andThen(GetReplaceLens) and andThen(Optional) start from the
  // same "prism over Inner that hits when n >= 0" outer — factor it once so the two
  // scenarios below only express the per-fusion assertion shape.
  private val nonNegInnerPrism: MendTearPrism[Inner, Inner, Inner, Inner] =
    new MendTearPrism[Inner, Inner, Inner, Inner](
      tear = i => if i.n >= 0 then Right(i) else Left(i),
      mend = identity,
    )

  "MendTearPrism.andThen(GetReplaceLens) produces an Optional" >> {
    val chained: Optional[Inner, Inner, Int, Int] =
      nonNegInnerPrism.andThen(innerLens)
    chained.getOrModify(Inner(5, "x")) === Right(5)
    chained.getOrModify(Inner(-3, "y")) === Left(Inner(-3, "y"))
    chained.reverseGet(Inner(5, "x"), 99) === Inner(99, "x")
  }

  "MendTearPrism.andThen(Optional) produces an Optional" >> {
    val chained: Optional[Inner, Inner, Int, Int] =
      nonNegInnerPrism.andThen(adultOpt)
    chained.getOrModify(Inner(20, "a")) === Right(20)
    chained.getOrModify(Inner(-5, "neg")) === Left(Inner(-5, "neg")) // outer miss
    chained.getOrModify(Inner(15, "m")) === Left(Inner(15, "m")) // inner miss
  }

  // ---- PickMendPrism.andThen(_) — 3 fused overloads ----------------

  "PickMendPrism.andThen(PickMendPrism) fuses two" >> {
    val chained: PickMendPrism[Int, Int, Int] =
      evenPmPrism.andThen(evenPmPrism)
    // 16 → 8 (even, hit) → 4 (even, hit). chained: Some(4).
    chained.pick(16) === Some(4)
    // 10 → 5 (even, hit) → None (5 is odd). chained: None (outer hits, inner misses).
    chained.pick(10) === None
    // 3 → odd → None (outer miss).
    chained.pick(3) === None
    // mend: inner.mend(4) = 8, outer.mend(8) = 16.
    chained.mend(4) === 16
  }

  "PickMendPrism.andThen(MendTearPrism) requires A = B" >> {
    val chained: MendTearPrism[Int, Int, Int, Int] =
      evenPmPrism.andThen(evenMtPrism)
    chained.tear(16) === Right(4) // 16 → 8 → 4, chain hit
    chained.tear(10) === Left(10) // 10 → 5 → odd → inner miss, outer.mend(5) = 10
    chained.tear(3) === Left(3) // outer miss
    chained.mend(4) === 16
  }

  "PickMendPrism.andThen(BijectionIso) produces a PickMendPrism" >> {
    val intIso: BijectionIso[Int, Int, Int, Int] =
      new BijectionIso[Int, Int, Int, Int](_ * 10, _ / 10)
    val chained: PickMendPrism[Int, Int, Int] = evenPmPrism.andThen(intIso)
    chained.pick(16) === Some(80) // 16/2=8, iso 8*10=80
    chained.pick(7) === None
    chained.mend(80) === 16 // iso 80/10=8, outer 8*2=16
  }

  // ---- GetReplaceLens.andThen(_) — 5 fused overloads ---------------

  "GetReplaceLens.andThen(GetReplaceLens) fuses two lenses" >> {
    val outerLens: GetReplaceLens[Wrapper, Wrapper, Inner, Inner] =
      new GetReplaceLens[Wrapper, Wrapper, Inner, Inner](_.inner, (w, i) => w.copy(inner = i))
    val chained: GetReplaceLens[Wrapper, Wrapper, Int, Int] =
      outerLens.andThen(innerLens)
    chained.get(Wrapper(Inner(7, "x"))) === 7
    chained.enplace(Wrapper(Inner(7, "x")), 11) === Wrapper(Inner(11, "x"))
    chained.modify(_ + 1)(Wrapper(Inner(7, "x"))) === Wrapper(Inner(8, "x"))
  }

  "GetReplaceLens.andThen(BijectionIso) produces a GetReplaceLens" >> {
    val outerLens: GetReplaceLens[Wrapper, Wrapper, Inner, Inner] =
      new GetReplaceLens[Wrapper, Wrapper, Inner, Inner](_.inner, (w, i) => w.copy(inner = i))
    val chained: GetReplaceLens[Wrapper, Wrapper, (Int, String), (Int, String)] =
      outerLens.andThen(innerIso)
    chained.get(Wrapper(Inner(5, "a"))) === ((5, "a"))
    chained.enplace(Wrapper(Inner(5, "a")), (9, "z")) === Wrapper(Inner(9, "z"))
  }

  "GetReplaceLens.andThen(MendTearPrism) produces an Optional" >> {
    val outerLens: GetReplaceLens[Wrapper, Wrapper, Inner, Inner] =
      new GetReplaceLens[Wrapper, Wrapper, Inner, Inner](_.inner, (w, i) => w.copy(inner = i))
    val innerPrism: MendTearPrism[Inner, Inner, Int, Int] =
      new MendTearPrism[Inner, Inner, Int, Int](
        tear = i => if i.n >= 0 then Right(i.n) else Left(i),
        mend = n => Inner(n, "from-mend"),
      )
    val chained: Optional[Wrapper, Wrapper, Int, Int] =
      outerLens.andThen(innerPrism)
    chained.getOrModify(Wrapper(Inner(5, "ok"))) === Right(5)
    chained.getOrModify(Wrapper(Inner(-1, "neg"))) === Left(Wrapper(Inner(-1, "neg")))
  }

  "GetReplaceLens.andThen(PickMendPrism) produces an Optional (A = B)" >> {
    val outerLens: GetReplaceLens[Inner, Inner, Int, Int] = innerLens
    val chained: Optional[Inner, Inner, Int, Int] =
      outerLens.andThen(evenPmPrism)
    chained.getOrModify(Inner(16, "x")) === Right(8) // 16 even → hit
    chained.getOrModify(Inner(7, "y")) === Left(Inner(7, "y")) // 7 odd → miss
  }

  "GetReplaceLens.andThen(Optional) produces an Optional" >> {
    val outerLens: GetReplaceLens[Wrapper, Wrapper, Inner, Inner] =
      new GetReplaceLens[Wrapper, Wrapper, Inner, Inner](_.inner, (w, i) => w.copy(inner = i))
    val chained: Optional[Wrapper, Wrapper, Int, Int] =
      outerLens.andThen(adultOpt)
    chained.getOrModify(Wrapper(Inner(30, "a"))) === Right(30)
    chained.getOrModify(Wrapper(Inner(15, "m"))) === Left(Wrapper(Inner(15, "m")))
    chained.reverseGet(Wrapper(Inner(30, "a")), 99) === Wrapper(Inner(99, "a"))
  }
