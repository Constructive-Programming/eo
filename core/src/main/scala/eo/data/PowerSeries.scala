package eo
package data

import cats.Applicative

import optics.{GetReplaceLens, Optic}

/** Carrier for the `PowerSeries`-style `Traversal`: pairs an existential leftover `xo: Snd[A]` with
  * a flat focus vector `vs: PSVec[B]`.
  *
  * Focus storage is a [[PSVec]] — an `Array[AnyRef]` plus an `(offset, length)` view — so that
  * `assoc.composeFrom` can hand each inner reassembly a zero-copy slice of the underlying flat
  * array. No `ClassTag[B]` is required at the API boundary: every generic `B` inside the optic
  * machinery erases to `Object`, so `PSVec` stores `Array[AnyRef]` and narrows back to `B` at read
  * time.
  *
  * See `benchmarks/src/main/scala/eo/bench/PowerSeriesBench.scala` for the runtime profile.
  *
  * @tparam A
  *   existential leftover tuple — the `Snd[A]` match type reduces to the second tuple element when
  *   `A` is a concrete `Tuple2` and stays inert otherwise.
  * @tparam B
  *   focus element type — stored erased-to-`AnyRef` inside the underlying [[PSVec]].
  */
final case class PowerSeries[A, B](xo: Snd[A], vs: PSVec[B])

/** Typeclass instances for [[PowerSeries]]. */
object PowerSeries:

  /** `ForgetfulFunctor[PowerSeries]` — maps each focus element through `f` into a freshly allocated
    * `Array[AnyRef]` and re-wraps as a [[PSVec]]. Unlocks `.modify` / `.replace` on every
    * PowerSeries-carrier optic.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[PowerSeries] with

    def map[X, A, B](psa: PowerSeries[X, A], f: A => B): PowerSeries[X, B] =
      val src = psa.vs
      val n = src.length
      val arr = new Array[AnyRef](n)
      var i = 0
      while i < n do
        arr(i) = f(src(i)).asInstanceOf[AnyRef]
        i += 1
      PowerSeries(psa.xo, PSVec.unsafeWrap[B](arr))

  /** `ForgetfulTraverse[PowerSeries, Applicative]` — sequences an effectful `A => G[B]` through the
    * focus vector by applicative combination, producing `G[PowerSeries[X, B]]`. Unlocks `.modifyA`
    * / `.all` on every PowerSeries-carrier optic.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]
        : PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      psa =>
        f =>
          val src = psa.vs
          val n = src.length
          val G = Applicative[G]
          if n == 0 then G.pure(PowerSeries(psa.xo, PSVec.empty[B]))
          else
            // Build a G[Array[AnyRef]] incrementally with applicative combination
            // — equivalent to sequence, without materialising an intermediate
            // List or ArraySeq.
            var acc: G[Array[AnyRef]] = G.pure(new Array[AnyRef](n))
            var i = 0
            while i < n do
              val idx = i
              val gb = f(src(idx))
              acc = G.map2(acc, gb) { (a, b) =>
                a(idx) = b.asInstanceOf[AnyRef]
                a
              }
              i += 1
            G.map(acc)(arr => PowerSeries(psa.xo, PSVec.unsafeWrap[B](arr)))

  /** Composed existential leftover for `PowerSeries`-carrier `andThen`. Stores the outer leftover,
    * a primitive `Array[Int]` of per-outer focus counts, and an `Array[AnyRef]` of per-outer inner
    * leftovers — parallel arrays rather than a Tuple2-of-two arrays, so each per-element entry pays
    * one primitive-int write and one reference write instead of allocating an intermediate
    * `(Int, Snd[Xi])` Tuple2.
    *
    * When the inner optic is a [[PSSingletonAlwaysHit]] (a morphed Lens — every call yields exactly
    * one focus), `lens` is left `null`: every per-element length is implicitly 1, so composeFrom
    * can use `i` as both the focus-vector offset and the ys index without consulting the array.
    * Saves `4 + 4*n` bytes per always-hit compose level.
    *
    * The class is private to `eo` because the type is purely an internal detail of [[assoc]]'s `Z`
    * — no user code ever constructs one.
    */
  final private[eo] class AssocSndZ[Xo, Xi](
      val xo: Snd[Xo],
      val lens: Array[Int] | Null,
      val ys: Array[AnyRef],
  )

  /** Internal protocol for PowerSeries-carrier optics that always produce a 0- or 1-element focus
    * vector — the morphed Lens / Prism / Optional values returned by [[tuple2ps]] / [[either2ps]] /
    * [[affine2ps]]. When `assoc.composeTo` / `composeFrom` detect a `PSSingleton` inner, they skip
    * the per-element `PowerSeries` + `PSVec.Single` allocations and (for Prism / Optional) also
    * skip the `Either` / `Option` wrapper the generic `to` path would build.
    *
    * The stricter refinement [[PSSingletonAlwaysHit]] covers the "every call hits" case (Lens
    * morphs), letting `assoc` skip the `Array[Int]` `lens` array entirely.
    *
    * Private to `eo` — user code never sees this trait or its instances.
    */
  private[eo] trait PSSingleton[S, T, A, B]:

    /** Emit this optic's contribution for one outer-focus value `s` directly into the assoc
      * builders. No per-call `PowerSeries` / `PSVec` / `Either` / `Option` / `Tuple2` allocation.
      *
      * @param lenBuf
      *   receives 0 (miss) or 1 (hit) for this element
      * @param ysBuf
      *   receives the element's leftover (`Fst[o.X]` / `Snd[o.X]` / `o.X` / `S`) as AnyRef
      * @param flatBuf
      *   receives the focus `A` as AnyRef on the hit branch; no entry on miss
      */
    def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit

    /** Reconstruct `T` for one outer-focus value using the raw buffer contents.
      *
      * @param y
      *   the AnyRef stashed into `ysBuf` at [[collectTo]] time
      * @param vys
      *   shared flat focus vector after `inner.modify` ran
      * @param pos
      *   absolute index into `vys` where this element's focus lives; meaningful only when `len ==
      *   1`
      * @param len
      *   0 (miss) or 1 (hit) — mirrors the slot that was pushed into `lenBuf`
      */
    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T

  /** Refinement of [[PSSingleton]] for optics that always produce **exactly one** focus per outer
    * element (a morphed Lens — no miss branch, no multi-focus). Lets `composeTo` skip the
    * per-element `lens` array entirely (every slot would be 1) and lets `composeFrom` address `ys`
    * and `vys` with the same index `i` without walking a running `offset`.
    *
    * Prism / Optional morphs don't qualify: they may miss, producing a 0-length focus contribution.
    *
    * Implementers only need to define the two always-hit methods — the base [[PSSingleton]]
    * contract is filled in automatically by delegation, so the generic maybe-hit path in
    * `composeTo` / `composeFrom` still works if the type-match ever routes through it.
    *
    * Private to `eo` — user code never sees this trait.
    */
  private[eo] trait PSSingletonAlwaysHit[S, T, A, B] extends PSSingleton[S, T, A, B]:

    /** Emit the one-focus contribution for `s` directly into the shared builders. `ysBuf` and
      * `flatBuf` stay 1:1 aligned across calls.
      */
    def collectAlwaysHit(s: S, ysBuf: ObjArrBuilder, flatBuf: ObjArrBuilder): Unit

    /** Reassemble `T` from the i-th `(y, focus)` pair. Skips the
      * [[PSSingleton.reconstructSingleton]] `pos` / `len` plumbing since always-hit implies
      * `pos == i, len == 1`.
      */
    def reconstructAlwaysHit(y: AnyRef, focus: B): T

    final def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit =
      lenBuf.append(1)
      collectAlwaysHit(s, ysBuf, flatBuf)

    final def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      reconstructAlwaysHit(y, vys(pos))

  /** `AssociativeFunctor[PowerSeries, Xo, Xi]` — same-carrier `.andThen` for two
    * PowerSeries-carrier optics. Specialises on [[PSSingletonAlwaysHit]] (morphed Lens) and
    * [[PSSingleton]] (morphed Prism / Optional) inner optics for zero-intermediate-allocation fast
    * paths; falls back to the generic per-element path otherwise.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[PowerSeries, Xo, Xi] with
    type SndZ = AssocSndZ[Xo, Xi]
    type Z = (Int, SndZ)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, PowerSeries] { type X = Xo },
        inner: Optic[A, B, C, D, PowerSeries] { type X = Xi },
    ): PowerSeries[Z, C] =
      val outerPS = outer.to(s)
      val xo = outerPS.xo
      val va = outerPS.vs
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      inner match
        case ah: PSSingletonAlwaysHit[A, B, C, D] @unchecked =>
          // Always-hit fast path: every call produces exactly one focus, so
          // `lenBuf` would be filled with 1s. Skip it entirely (AssocSndZ.lens
          // stays null) and pre-size flatBuf — we know the exact total is `n`.
          val flatBuf = new ObjArrBuilder(n)
          var i = 0
          while i < n do
            ah.collectAlwaysHit(va(i), ysBuf, flatBuf)
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, null, ysBuf.freezeArr)
          PowerSeries(sndZ, flatBuf.freezeAsPSVec[C])
        case ps: PSSingleton[A, B, C, D] @unchecked =>
          // Maybe-hit fast path (Prism / Optional morphs). We still need lenBuf
          // because some elements may miss — flatBuf.length < n is possible.
          // All three builders are pre-sized to `n`: lenBuf and ysBuf fill
          // exactly, flatBuf overshoots on misses (trimmed on freeze, but no
          // intermediate grow allocations).
          val lenBuf = new IntArrBuilder(n)
          val flatBuf = new ObjArrBuilder(n)
          var i = 0
          while i < n do
            ps.collectTo(va(i), lenBuf, ysBuf, flatBuf)
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
          PowerSeries(sndZ, flatBuf.freezeAsPSVec[C])
        case _ =>
          val lenBuf = new IntArrBuilder(n)
          val flatBuf = new ObjArrBuilder()
          var i = 0
          while i < n do
            val innerPS = inner.to(va(i))
            val vy = innerPS.vs
            lenBuf.append(vy.length)
            ysBuf.append(innerPS.xo.asInstanceOf[AnyRef])
            flatBuf.appendAllFromPSVec(vy)
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
          PowerSeries(sndZ, flatBuf.freezeAsPSVec[C])

    def composeFrom[S, T, A, B, C, D](
        xd: PowerSeries[Z, D],
        inner: Optic[A, B, C, D, PowerSeries] { type X = Xi },
        outer: Optic[S, T, A, B, PowerSeries] { type X = Xo },
    ): T =
      val sndZ = xd.xo
      val vys = xd.vs
      val lens = sndZ.lens
      val ys = sndZ.ys
      val resultBuf = new ObjArrBuilder(ys.length)
      inner match
        case ah: PSSingletonAlwaysHit[A, B, C, D] @unchecked =>
          // Always-hit fast path (lens == null, every element hits exactly once):
          // vys index == ys index == i, no running offset needed.
          val n = ys.length
          var i = 0
          while i < n do
            resultBuf.append(ah.reconstructAlwaysHit(ys(i), vys(i)).asInstanceOf[AnyRef])
            i += 1
        case ps: PSSingleton[A, B, C, D] @unchecked =>
          // Maybe-hit fast path — still consult lens for 0/1 length per element.
          val lensArr = lens.nn
          val n = lensArr.length
          var offset = 0
          var i = 0
          while i < n do
            val len = lensArr(i)
            resultBuf.append(ps.reconstructSingleton(ys(i), vys, offset, len).asInstanceOf[AnyRef])
            offset += len
            i += 1
        case _ =>
          val lensArr = lens.nn
          val n = lensArr.length
          var offset = 0
          var i = 0
          while i < n do
            val len = lensArr(i)
            val y = ys(i).asInstanceOf[Snd[Xi]]
            val chunk = vys.slice(offset, offset + len)
            resultBuf.append(inner.from(PowerSeries(y, chunk)).asInstanceOf[AnyRef])
            offset += len
            i += 1
      outer.from(PowerSeries(sndZ.xo, resultBuf.freezeAsPSVec[B]))

  /** PS-carrier view of a [[GetReplaceLens]]. Specialised over the generic Tuple2 morph: stores the
    * lens's `get` / `enplace` directly and skips the intermediate `(s, get(s))` Tuple2 that the
    * generic `Tuple2InPS` would build on every access.
    */
  final private class GetReplaceLensInPS[S, T, A, B](lens: GetReplaceLens[S, T, A, B])
      extends Optic[S, T, A, B, PowerSeries]
      with PSSingletonAlwaysHit[S, T, A, B]:
    type X = (1, S)

    val to: S => PowerSeries[X, A] = s => PowerSeries(s, PSVec.singleton[A](lens.get(s)))
    val from: PowerSeries[X, B] => T = ps => lens.enplace(ps.xo, ps.vs.head)

    def collectAlwaysHit(s: S, ysBuf: ObjArrBuilder, flatBuf: ObjArrBuilder): Unit =
      // `unsafeAppend` skips the grow-check — composeTo pre-sizes both builders
      // to `n` (the exact total for the always-hit case).
      ysBuf.unsafeAppend(s.asInstanceOf[AnyRef])
      flatBuf.unsafeAppend(lens.get(s).asInstanceOf[AnyRef])

    def reconstructAlwaysHit(y: AnyRef, focus: B): T =
      lens.enplace(y.asInstanceOf[S], focus)

  /** Generic Tuple2-carrier → PowerSeries morph for non-[[GetReplaceLens]] lenses (`SimpleLens`,
    * `SplitCombineLens`, hand-rolled `Optic[_, _, _, _, Tuple2]`). Still participates in the
    * `PSSingleton` fast path — it just has to call the generic `o.to(s)` to get the `(xo, a)`
    * tuple.
    */
  final private class GenericTuple2InPS[S, T, A, B](val o: Optic[S, T, A, B, Tuple2])
      extends Optic[S, T, A, B, PowerSeries]
      with PSSingletonAlwaysHit[S, T, A, B]:
    type X = (1, o.X)

    val to: S => PowerSeries[X, A] = s =>
      val (xo, a) = o.to(s)
      PowerSeries(xo, PSVec.singleton[A](a))

    val from: PowerSeries[X, B] => T = ps => o.from((ps.xo, ps.vs.head))

    def collectAlwaysHit(s: S, ysBuf: ObjArrBuilder, flatBuf: ObjArrBuilder): Unit =
      val (xo, a) = o.to(s)
      ysBuf.unsafeAppend(xo.asInstanceOf[AnyRef])
      flatBuf.unsafeAppend(a.asInstanceOf[AnyRef])

    def reconstructAlwaysHit(y: AnyRef, focus: B): T =
      o.from((y.asInstanceOf[o.X], focus))

  /** `Composer[Tuple2, PowerSeries]` — lifts a Lens-carrier optic into PowerSeries so
    * `lens.andThen(traversal)` type-checks. Specialises on [[optics.GetReplaceLens]] for the fast
    * path (`GetReplaceLensInPS`); falls back to the generic Tuple2 wrapper otherwise.
    *
    * @group Instances
    */
  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      o match
        case glr: GetReplaceLens[?, ?, ?, ?] =>
          new GetReplaceLensInPS[S, T, A, B](glr.asInstanceOf[GetReplaceLens[S, T, A, B]])
        case _ =>
          new GenericTuple2InPS[S, T, A, B](o)

  // Iso → PowerSeries composition is handled by the low-priority `Morph.viaTuple2` fallback
  // (see `eo.LowPriorityMorphInstances`). No direct `Composer[Forgetful, PowerSeries]` is
  // shipped here — the fallback fires cleanly once `Composer.chain` is at low priority,
  // producing the same `Forgetful → Tuple2 → PowerSeries` path as a direct given would have.

  /** PS-carrier view of an `Optic[_, _, _, _, Either]`. The generic `to` path would stuff the
    * Prism's miss / hit into a `PowerSeries` wrapping an `Option[o.X]` existential. The
    * `PSSingleton` fast path skips that outer Option wrapper entirely: len=0 signals miss (ysBuf
    * holds `o.X` directly), len=1 signals hit (ysBuf irrelevant, focus in flatBuf).
    */
  final private class EitherInPS[S, T, A, B](val o: Optic[S, T, A, B, Either])
      extends Optic[S, T, A, B, PowerSeries]
      with PSSingleton[S, T, A, B]:
    type X = (1, Option[o.X])

    val to: S => PowerSeries[X, A] = s =>
      o.to(s) match
        case Left(x)  => PowerSeries(Some(x), PSVec.empty[A])
        case Right(a) => PowerSeries(None, PSVec.singleton[A](a))

    val from: PowerSeries[X, B] => T = ps =>
      ps.xo.asInstanceOf[Option[o.X]] match
        case Some(x) => o.from(Left(x))
        case None    => o.from(Right(ps.vs.head))

    def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit =
      // `unsafeAppend` — all three builders are pre-sized to `n` in
      // composeTo (lenBuf / ysBuf fill exactly, flatBuf overshoots on misses
      // and trims on freeze).
      o.to(s) match
        case Left(x) =>
          lenBuf.unsafeAppend(0)
          ysBuf.unsafeAppend(x.asInstanceOf[AnyRef])
        case Right(a) =>
          lenBuf.unsafeAppend(1)
          // ysBuf slot for this index is irrelevant on hit (reconstructSingleton
          // reads from flatBuf when len==1), but we must push *something* to
          // keep ysBuf 1:1 aligned with lenBuf. `null.asInstanceOf[AnyRef]`
          // rather than `null` so this compiles under `-Yexplicit-nulls`.
          ysBuf.unsafeAppend(null.asInstanceOf[AnyRef])
          flatBuf.unsafeAppend(a.asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      if len == 0 then o.from(Left(y.asInstanceOf[o.X]))
      else o.from(Right(vys(pos)))

  /** `Composer[Either, PowerSeries]` — lifts a Prism-carrier optic into PowerSeries so
    * `prism.andThen(traversal)` type-checks. Uses the [[PSSingleton]] fast path to skip the
    * intermediate `Option[o.X]` wrapper the generic `to` would allocate.
    *
    * @group Instances
    */
  given either2ps: Composer[Either, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new EitherInPS[S, T, A, B](o)

  /** PS-carrier view of an `Optic[_, _, _, _, Affine]`. The generic `to` path wraps the Affine's
    * Miss/Hit leftover into an `Either[Fst[o.X], Snd[o.X]]` that `PowerSeries.xo` then carries. The
    * `PSSingleton` fast path skips that Either entirely: len=0 stashes `Fst[o.X]` directly in
    * ysBuf, len=1 stashes `Snd[o.X]` directly.
    */
  final private class AffineInPS[S, T, A, B](val o: Optic[S, T, A, B, Affine])
      extends Optic[S, T, A, B, PowerSeries]
      with PSSingleton[S, T, A, B]:
    type X = (1, Either[Fst[o.X], Snd[o.X]])

    val to: S => PowerSeries[X, A] = s =>
      o.to(s) match
        case m: Affine.Miss[o.X, A] => PowerSeries(Left(m.fst), PSVec.empty[A])
        case h: Affine.Hit[o.X, A]  => PowerSeries(Right(h.snd), PSVec.singleton[A](h.b))

    val from: PowerSeries[X, B] => T = ps =>
      ps.xo.asInstanceOf[Either[Fst[o.X], Snd[o.X]]] match
        case Left(fx)  => o.from(new Affine.Miss[o.X, B](fx))
        case Right(sx) => o.from(new Affine.Hit[o.X, B](sx, ps.vs.head))

    def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit =
      // `unsafeAppend` — same pre-sized-builder contract as [[EitherInPS]].
      o.to(s) match
        case m: Affine.Miss[o.X, A] =>
          lenBuf.unsafeAppend(0)
          ysBuf.unsafeAppend(m.fst.asInstanceOf[AnyRef])
        case h: Affine.Hit[o.X, A] =>
          lenBuf.unsafeAppend(1)
          ysBuf.unsafeAppend(h.snd.asInstanceOf[AnyRef])
          flatBuf.unsafeAppend(h.b.asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      if len == 0 then o.from(new Affine.Miss[o.X, B](y.asInstanceOf[Fst[o.X]]))
      else o.from(new Affine.Hit[o.X, B](y.asInstanceOf[Snd[o.X]], vys(pos)))

  /** `Composer[Affine, PowerSeries]` — lifts an Optional-carrier optic into PowerSeries so
    * `optional.andThen(traversal)` type-checks. Uses the [[PSSingleton]] fast path to skip the
    * intermediate `Either[Fst[o.X], Snd[o.X]]` wrapper the generic `to` would allocate.
    *
    * @group Instances
    */
  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new AffineInPS[S, T, A, B](o)
