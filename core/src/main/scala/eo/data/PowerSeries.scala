package eo
package data

import cats.Applicative

import optics.Optic

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

  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, o.X)

        val to: S => PowerSeries[X, A] = s =>
          val (xo, a) = o.to(s)
          PowerSeries(xo, PSVec.singleton[A](a))

        val from: PowerSeries[X, B] => T = ps => o.from((ps.xo, ps.vs.head))

  given either2ps: Composer[Either, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Option[o.X])

        val to: S => PowerSeries[X, A] = s =>
          o.to(s) match
            case Left(x)  => PowerSeries(Some(x), PSVec.empty[A])
            case Right(a) => PowerSeries(None, PSVec.singleton[A](a))

        val from: PowerSeries[X, B] => T = ps =>
          ps.xo.asInstanceOf[Option[o.X]] match
            case Some(x) => o.from(Left(x))
            case None    => o.from(Right(ps.vs.head))

  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Either[Fst[o.X], Snd[o.X]])

        val to: S => PowerSeries[X, A] = s =>
          o.to(s) match
            case m: Affine.Miss[o.X, A] => PowerSeries(Left(m.fst), PSVec.empty[A])
            case h: Affine.Hit[o.X, A]  => PowerSeries(Right(h.snd), PSVec.singleton[A](h.b))

        val from: PowerSeries[X, B] => T = ps =>
          ps.xo.asInstanceOf[Either[Fst[o.X], Snd[o.X]]] match
            case Left(fx)  => o.from(new Affine.Miss[o.X, B](fx))
            case Right(sx) => o.from(new Affine.Hit[o.X, B](sx, ps.vs.head))
