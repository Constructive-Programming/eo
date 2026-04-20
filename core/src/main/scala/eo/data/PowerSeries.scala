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
  */
final case class PowerSeries[A, B](xo: Snd[A], vs: PSVec[B])

/** Typeclass instances for [[PowerSeries]]. */
object PowerSeries:

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
    * The class is private to `eo` because the type is purely an internal detail of [[assoc]]'s `Z`
    * — no user code ever constructs one.
    */
  final private[eo] class AssocSndZ[Xo, Xi](
      val xo: Snd[Xo],
      val lens: Array[Int],
      val ys: Array[AnyRef],
  )

  /** Internal protocol for PowerSeries-carrier optics that always produce a 0- or 1-element focus
    * vector — i.e. the morphed Lens / Prism / Optional values returned by [[tuple2ps]] /
    * [[either2ps]] / [[affine2ps]]. When an outer PowerSeries optic detects its inner is
    * `PSSingleton`, `assoc.composeTo` / `composeFrom` skip the per-element `PowerSeries` and
    * `PSVec.Single` allocations entirely and (for Prism / Optional) also skip the `Either`/`Option`
    * wrapper the generic `to` path would build.
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
      val lenBuf = new IntArrBuilder(n)
      val ysBuf = new ObjArrBuilder(n)
      val flatBuf = new ObjArrBuilder()
      inner match
        case ps: PSSingleton[A, B, C, D] @unchecked =>
          // Fast path: inner is a morphed Lens / Prism / Optional that
          // always yields 0- or 1-element PSVec. collectTo appends the
          // element's (len, y, focus) directly into the shared builders
          // without per-call PowerSeries / PSVec / Either / Option / Tuple2
          // allocation.
          var i = 0
          while i < n do
            ps.collectTo(va(i), lenBuf, ysBuf, flatBuf)
            i += 1
        case _ =>
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
      val n = lens.length
      val resultBuf = new ObjArrBuilder(n)
      inner match
        case ps: PSSingleton[A, B, C, D] @unchecked =>
          // Fast path mirrors composeTo: feed each element's raw (y, vys,
          // pos, len) into reconstructSingleton instead of allocating a
          // per-element PowerSeries + PSVec slice for inner.from.
          var offset = 0
          var i = 0
          while i < n do
            val len = lens(i)
            val y = ys(i)
            resultBuf.append(ps.reconstructSingleton(y, vys, offset, len).asInstanceOf[AnyRef])
            offset += len
            i += 1
        case _ =>
          var offset = 0
          var i = 0
          while i < n do
            val len = lens(i)
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
      with PSSingleton[S, T, A, B]:
    type X = (1, S)

    val to: S => PowerSeries[X, A] = s => PowerSeries(s, PSVec.singleton[A](lens.get(s)))
    val from: PowerSeries[X, B] => T = ps => lens.enplace(ps.xo, ps.vs.head)

    def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit =
      lenBuf.append(1)
      ysBuf.append(s.asInstanceOf[AnyRef])
      flatBuf.append(lens.get(s).asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      lens.enplace(y.asInstanceOf[S], vys(pos))

  /** Generic Tuple2-carrier → PowerSeries morph for non-[[GetReplaceLens]] lenses (`SimpleLens`,
    * `SplitCombineLens`, hand-rolled `Optic[_, _, _, _, Tuple2]`). Still participates in the
    * `PSSingleton` fast path — it just has to call the generic `o.to(s)` to get the `(xo, a)`
    * tuple.
    */
  final private class GenericTuple2InPS[S, T, A, B](val o: Optic[S, T, A, B, Tuple2])
      extends Optic[S, T, A, B, PowerSeries]
      with PSSingleton[S, T, A, B]:
    type X = (1, o.X)

    val to: S => PowerSeries[X, A] = s =>
      val (xo, a) = o.to(s)
      PowerSeries(xo, PSVec.singleton[A](a))

    val from: PowerSeries[X, B] => T = ps => o.from((ps.xo, ps.vs.head))

    def collectTo(
        s: S,
        lenBuf: IntArrBuilder,
        ysBuf: ObjArrBuilder,
        flatBuf: ObjArrBuilder,
    ): Unit =
      val (xo, a) = o.to(s)
      lenBuf.append(1)
      ysBuf.append(xo.asInstanceOf[AnyRef])
      flatBuf.append(a.asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      o.from((y.asInstanceOf[o.X], vys(pos)))

  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      o match
        case glr: GetReplaceLens[?, ?, ?, ?] =>
          new GetReplaceLensInPS[S, T, A, B](glr.asInstanceOf[GetReplaceLens[S, T, A, B]])
        case _ =>
          new GenericTuple2InPS[S, T, A, B](o)

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
      o.to(s) match
        case Left(x) =>
          lenBuf.append(0)
          ysBuf.append(x.asInstanceOf[AnyRef])
        case Right(a) =>
          lenBuf.append(1)
          ysBuf.append(null)
          flatBuf.append(a.asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      if len == 0 then o.from(Left(y.asInstanceOf[o.X]))
      else o.from(Right(vys(pos)))

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
      o.to(s) match
        case m: Affine.Miss[o.X, A] =>
          lenBuf.append(0)
          ysBuf.append(m.fst.asInstanceOf[AnyRef])
        case h: Affine.Hit[o.X, A] =>
          lenBuf.append(1)
          ysBuf.append(h.snd.asInstanceOf[AnyRef])
          flatBuf.append(h.b.asInstanceOf[AnyRef])

    def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
      if len == 0 then o.from(new Affine.Miss[o.X, B](y.asInstanceOf[Fst[o.X]]))
      else o.from(new Affine.Hit[o.X, B](y.asInstanceOf[Snd[o.X]], vys(pos)))

  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new AffineInPS[S, T, A, B](o)
