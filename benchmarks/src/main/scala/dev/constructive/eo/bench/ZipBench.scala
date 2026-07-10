package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import optics.{Getter, GetReplaceLens, Lens, Optic}

/** Fanout (`zip`) encoding shoot-out: two same-source lenses combined into one
  * `Optic[S, S, (A, C), (A, C), Tuple2]`.
  *
  * Encodings under test:
  *   - '''D''' (anchor) — fused `GetReplaceLens`: stored gets + sequential `enplace`, no carrier
  *     round-trip.
  *   - '''B''' — ZipFunctor-cell style: anonymous Tuple2-carrier optic, `X = o1.X`; write runs
  *     `o1.from` then re-reads `o2.to` on the intermediate (sequential semantics).
  *   - '''E''' — `alongside` (product-of-sources primitive, lawful) + optic-aware diagonal merge:
  *     write rebuilds BOTH halves, then merges via `o2.replace(o2.get(t2))(t1)`.
  *   - '''cap''' — zip on the capability traits directly (`CanGet & CanModify` intersection), no
  *     carrier at all.
  *
  * Calling paths: direct `.get` / `.modify`, and through a consuming method that demands `CanGet` /
  * `CanModify` evidence (per-call summon from a given optic, per the capability doctrine).
  */
object ZipImpls:

  case class Rec(name: String, age: Int, tag: Long)

  val ageL: GetReplaceLens[Rec, Rec, Int, Int] = Lens[Rec, Int](_.age, (r, a) => r.copy(age = a))

  val tagL: GetReplaceLens[Rec, Rec, Long, Long] =
    Lens[Rec, Long](_.tag, (r, t) => r.copy(tag = t))

  // ---- D: fused GetReplaceLens ------------------------------------------------------------
  def zipD[S, A, C](
      o1: GetReplaceLens[S, S, A, A],
      o2: GetReplaceLens[S, S, C, C],
  ): GetReplaceLens[S, S, (A, C), (A, C)] =
    Lens.pLens(
      s => (o1.get(s), o2.get(s)),
      (s, bd) => o2.enplace(o1.enplace(s, bd._1), bd._2),
    )

  // ---- B: ZipFunctor Tuple2×Tuple2 cell (sequential write, re-read on write path) ----------
  def zipB[S, A, C](
      o1: Optic[S, S, A, A, Tuple2],
      o2: Optic[S, S, C, C, Tuple2],
  ): Optic[S, S, (A, C), (A, C), Tuple2] =
    new Optic[S, S, (A, C), (A, C), Tuple2]:
      type X = o1.X
      def to(s: S): (X, (A, C)) =
        val (x1, a) = o1.to(s)
        (x1, (a, o2.to(s)._2))
      def from(xbd: (X, (A, C))): S =
        val (x1, bd) = xbd
        val t1 = o1.from((x1, bd._1))
        val (x2, _) = o2.to(t1)
        o2.from((x2, bd._2))

  // ---- E: alongside (lawful product primitive) + optic-aware diagonal merge ----------------
  def alongside[S, T, A, B, S2, T2, C, D](
      o1: Optic[S, T, A, B, Tuple2],
      o2: Optic[S2, T2, C, D, Tuple2],
  ): Optic[(S, S2), (T, T2), (A, C), (B, D), Tuple2] =
    new Optic[(S, S2), (T, T2), (A, C), (B, D), Tuple2]:
      type X = (o1.X, o2.X)
      def to(ss: (S, S2)): (X, (A, C)) =
        val (x1, a) = o1.to(ss._1)
        val (x2, c) = o2.to(ss._2)
        ((x1, x2), (a, c))
      def from(xbd: (X, (B, D))): (T, T2) =
        val (x, bd) = xbd
        (o1.from((x._1, bd._1)), o2.from((x._2, bd._2)))

  def zipE[S, A, C](
      o1: Optic[S, S, A, A, Tuple2],
      o2: Optic[S, S, C, C, Tuple2],
  ): Optic[S, S, (A, C), (A, C), Tuple2] =
    val al = alongside(o1, o2)
    new Optic[S, S, (A, C), (A, C), Tuple2]:
      type X = al.X
      def to(s: S): (X, (A, C)) = al.to((s, s))
      def from(xbd: (X, (A, C))): S =
        val (t1, t2) = al.from(xbd)
        // diagonal merge: pull o2's focus out of t2, land it in t1
        o2.from((o2.to(t1)._1, o2.to(t2)._2))

  // ---- cap: zip on the capability traits, no carrier ---------------------------------------
  final class ZipCap[S, A, C](
      m1: CanGet[S, A] & CanModify[S, A],
      m2: CanGet[S, C] & CanModify[S, C],
  ) extends CanGet[S, (A, C)],
        CanModifyP[S, S, (A, C), (A, C)]:
    def get(s: S): (A, C) = (m1.get(s), m2.get(s))

    def modify(f: ((A, C)) => (A, C)): S => S = s =>
      val bd = f((m1.get(s), m2.get(s)))
      m2.replace(bd._2)(m1.replace(bd._1)(s))

  // ---- Z: concrete class, X = S, retained-inline capability members ------------------------
  // `inline def` implementing an abstract member is RETAINED: static call sites splice the
  // body (closure + tuple EA-elided, like B's inline extension path), while evidence-typed
  // callers dispatch to the compiled copy (no derived-closure surcharge, like ZipCap).
  final class Zip2[S, A, C](
      m1: CanGet[S, A] & CanModify[S, A],
      m2: CanGet[S, C] & CanModify[S, C],
  ) extends Optic[S, S, (A, C), (A, C), Tuple2],
        CanGet[S, (A, C)],
        CanModifyP[S, S, (A, C), (A, C)]:
    type X = S
    def to(s: S): (S, (A, C)) = (s, (m1.get(s), m2.get(s)))

    def from(xbd: (S, (A, C))): S =
      val bd = xbd._2
      m2.replace(bd._2)(m1.replace(bd._1)(xbd._1))

    inline def get(s: S): (A, C) = (m1.get(s), m2.get(s))

    inline def modify(f: ((A, C)) => (A, C)): S => S = s =>
      val bd = f((m1.get(s), m2.get(s)))
      m2.replace(bd._2)(m1.replace(bd._1)(s))

  // ---- ZL: Zip2 with concrete GetReplaceLens legs — enplace directly, no replace chain -------
  final class ZipLens[S, A, C](
      l1: GetReplaceLens[S, S, A, A],
      l2: GetReplaceLens[S, S, C, C],
  ) extends Optic[S, S, (A, C), (A, C), Tuple2],
        CanGet[S, (A, C)],
        CanModifyP[S, S, (A, C), (A, C)]:
    type X = S
    def to(s: S): (S, (A, C)) = (s, (l1.get(s), l2.get(s)))

    def from(xbd: (S, (A, C))): S =
      val bd = xbd._2
      l2.enplace(l1.enplace(xbd._1, bd._1), bd._2)

    inline def get(s: S): (A, C) = (l1.get(s), l2.get(s))

    inline def modify(f: ((A, C)) => (A, C)): S => S = s =>
      val bd = f((l1.get(s), l2.get(s)))
      l2.enplace(l1.enplace(s, bd._1), bd._2)

  // ---- fixtures -----------------------------------------------------------------------------
  val rec: Rec = Rec("ada", 42, 7L)
  val bump: ((Int, Long)) => (Int, Long) = p => (p._1 + 1, p._2 + 1L)

  val dZip: GetReplaceLens[Rec, Rec, (Int, Long), (Int, Long)] = zipD(ageL, tagL)
  val bZip: Optic[Rec, Rec, (Int, Long), (Int, Long), Tuple2] = zipB(ageL, tagL)
  val eZip: Optic[Rec, Rec, (Int, Long), (Int, Long), Tuple2] = zipE(ageL, tagL)
  val capZip: ZipCap[Rec, Int, Long] = ZipCap(ageL, tagL)
  val cZip: Getter[Rec, (Int, Long)] = Getter(s => (ageL.get(s), tagL.get(s)))
  val zZip: Zip2[Rec, Int, Long] = Zip2(ageL, tagL)
  val zlZip: ZipLens[Rec, Int, Long] = ZipLens(ageL, tagL)

  // consuming methods per the capability doctrine
  def readPair[T](t: T)(using g: CanGet[T, (Int, Long)]): (Int, Long) = g.get(t)
  def bumpPair[T](t: T)(using m: CanModify[T, (Int, Long)]): T = m.modify(bump)(t)

  object BGivens:
    given Optic[Rec, Rec, (Int, Long), (Int, Long), Tuple2] = bZip

  object EGivens:
    given Optic[Rec, Rec, (Int, Long), (Int, Long), Tuple2] = eZip

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ZipBench extends JmhDefaults:

  import ZipImpls.*

  // ---- baseline: hand-rolled ----------------------------------------------------------------
  @Benchmark def hand_get: (Int, Long) = (rec.age, rec.tag)

  @Benchmark def hand_mod: Rec =
    val bd = bump((rec.age, rec.tag))
    rec.copy(age = bd._1, tag = bd._2)

  // ---- direct calling path ------------------------------------------------------------------
  @Benchmark def d_get: (Int, Long) = dZip.get(rec)
  @Benchmark def b_get: (Int, Long) = bZip.get(rec)
  @Benchmark def e_get: (Int, Long) = eZip.get(rec)
  @Benchmark def cap_get: (Int, Long) = capZip.get(rec)

  @Benchmark def d_mod: Rec = dZip.modify(bump)(rec)
  @Benchmark def b_mod: Rec = bZip.modify(bump)(rec)
  @Benchmark def e_mod: Rec = eZip.modify(bump)(rec)
  @Benchmark def cap_mod: Rec = capZip.modify(bump)(rec)

  // ---- capability-evidence calling path (per-call summon from a given optic) -----------------
  @Benchmark def d_capGet: (Int, Long) = readPair(rec)(using dZip)

  @Benchmark def b_capGet: (Int, Long) =
    import BGivens.given
    readPair(rec)

  @Benchmark def e_capGet: (Int, Long) =
    import EGivens.given
    readPair(rec)

  @Benchmark def cap_capGet: (Int, Long) = readPair(rec)(using capZip)

  // ---- C: read-only Getter fanout -------------------------------------------------------------
  @Benchmark def c_get: (Int, Long) = cZip.get(rec)
  @Benchmark def c_capGet: (Int, Long) = readPair(rec)(using cZip)

  // ---- Z: Zip2 concrete class, retained-inline members ----------------------------------------
  @Benchmark def z_get: (Int, Long) = zZip.get(rec)
  @Benchmark def z_mod: Rec = zZip.modify(bump)(rec)
  @Benchmark def z_capGet: (Int, Long) = readPair(rec)(using zZip)
  @Benchmark def z_capMod: Rec = bumpPair(rec)(using zZip)

  @Benchmark def zl_get: (Int, Long) = zlZip.get(rec)
  @Benchmark def zl_mod: Rec = zlZip.modify(bump)(rec)
  @Benchmark def zl_capGet: (Int, Long) = readPair(rec)(using zlZip)
  @Benchmark def zl_capMod: Rec = bumpPair(rec)(using zlZip)

  @Benchmark def d_capMod: Rec = bumpPair(rec)(using dZip)

  @Benchmark def b_capMod: Rec =
    import BGivens.given
    bumpPair(rec)

  @Benchmark def e_capMod: Rec =
    import EGivens.given
    bumpPair(rec)

  @Benchmark def cap_capMod: Rec = bumpPair(rec)(using capZip)
