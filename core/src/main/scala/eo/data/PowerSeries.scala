package eo
package data

import scala.collection.immutable.ArraySeq

import cats.Applicative
import cats.instances.arraySeq.given
import cats.syntax.functor.*
import cats.syntax.traverse.*

import optics.Optic

/** Carrier for the `PowerSeries`-style `Traversal`: pairs an existential leftover `xo: Snd[A]` with
  * a flat contiguous `ArraySeq[B]` of focused elements.
  *
  * Fields are stored directly (not wrapped in a `Tuple2`) and this class is not `AnyVal` — the
  * Optic trait's generic return slots force every `PowerSeries[A, B]` to reify anyway, so the
  * `AnyVal` wrapper bought nothing and the inner `Tuple2` cost one heap allocation per creation.
  * Two direct fields halves the carrier's per-creation allocation footprint.
  *
  * Storage is a flat `ArraySeq[B]` backed by a plain `Array[AnyRef]` — iteration and slicing are a
  * single `arraycopy` each, and hit the JVM's prefetcher well. The `assoc`'s `composeTo` /
  * `composeFrom` grow internal `Array[AnyRef]` buffers in place (see [[ObjArrBuilder]]) and
  * publish via `ArraySeq.unsafeWrapArray`, avoiding the buffer→toArray→wrap double-copy path
  * that `ArrayBuffer` takes.
  *
  * No `ClassTag[B]` is required at the API boundary. Every generic `B` inside the optic machinery
  * erases to `Object` on the JVM; the builder stores `Array[AnyRef]` directly and narrows the
  * runtime type back to `ArraySeq[B]` at publish time.
  *
  * See `benchmarks/src/main/scala/eo/bench/PowerSeriesBench.scala` for the runtime profile.
  */
final case class PowerSeries[A, B](xo: Snd[A], vs: ArraySeq[B])

/** Typeclass instances for [[PowerSeries]]. */
object PowerSeries:

  /** Legacy convenience — accept a `(Snd[A], ArraySeq[B])` tuple. Kept for call sites that spell
    * the pair out explicitly; prefer the synthesised case-class `apply(xo, vs)` on the hot path.
    */
  def apply[A, B](ps: (Snd[A], ArraySeq[B])): PowerSeries[A, B] = PowerSeries(ps._1, ps._2)

  given map: ForgetfulFunctor[PowerSeries] with
    def map[X, A, B](psa: PowerSeries[X, A], f: A => B): PowerSeries[X, B] =
      val src = psa.vs
      val n   = src.length
      val arr = new Array[AnyRef](n)
      var i   = 0
      while i < n do
        arr(i) = f(src(i)).asInstanceOf[AnyRef]
        i += 1
      PowerSeries(psa.xo, ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[B]])

  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]
        : PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      psa => f => psa.vs.traverse(f).map(vb => PowerSeries(psa.xo, vb))

  /** Composed existential leftover for `PowerSeries`-carrier `andThen`. Stores the outer
    * leftover, a primitive `Array[Int]` of per-outer focus counts, and an `Array[AnyRef]` of
    * per-outer inner leftovers — parallel arrays rather than a Tuple2-of-two arrays, so each
    * per-element entry pays one primitive-int write and one reference write instead of
    * allocating an intermediate `(Int, Snd[Xi])` Tuple2.
    *
    * The class is private to `eo` because the type is purely an internal detail of
    * [[assoc]]'s `Z` — no user code ever constructs one.
    */
  private[eo] final class AssocSndZ[Xo, Xi](
      val xo:     Snd[Xo],
      val lens:   Array[Int],
      val ys:     Array[AnyRef],
  )

  given assoc[Xo, Xi]: AssociativeFunctor[PowerSeries, Xo, Xi] with
    type SndZ = AssocSndZ[Xo, Xi]
    type Z    = (Int, SndZ)

    def composeTo[S, T, A, B, C, D](
        s:     S,
        outer: Optic[S, T, A, B, PowerSeries] { type X = Xo },
        inner: Optic[A, B, C, D, PowerSeries] { type X = Xi },
    ): PowerSeries[Z, C] =
      val outerPS = outer.to(s)
      val xo      = outerPS.xo
      val va      = outerPS.vs
      val n       = va.length
      val lenBuf  = new IntArrBuilder(n)
      val ysBuf   = new ObjArrBuilder(n)
      val flatBuf = new ObjArrBuilder()
      var i       = 0
      while i < n do
        val innerPS = inner.to(va(i))
        val vy      = innerPS.vs
        lenBuf.append(vy.length)
        ysBuf.append(innerPS.xo.asInstanceOf[AnyRef])
        flatBuf.appendAllFromArraySeq(vy)
        i += 1
      val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
      PowerSeries(sndZ, flatBuf.freezeAs[C])

    def composeFrom[S, T, A, B, C, D](
        xd:    PowerSeries[Z, D],
        inner: Optic[A, B, C, D, PowerSeries] { type X = Xi },
        outer: Optic[S, T, A, B, PowerSeries] { type X = Xo },
    ): T =
      val sndZ      = xd.xo
      val vys       = xd.vs
      val lens      = sndZ.lens
      val ys        = sndZ.ys
      val n         = lens.length
      val resultBuf = new ObjArrBuilder(n)
      var offset    = 0
      var i         = 0
      while i < n do
        val len     = lens(i)
        val y       = ys(i).asInstanceOf[Snd[Xi]]
        val chunk   = vys.slice(offset, offset + len)
        resultBuf.append(inner.from(PowerSeries(y, chunk)).asInstanceOf[AnyRef])
        offset     += len
        i          += 1
      outer.from(PowerSeries(sndZ.xo, resultBuf.freezeAs[B]))

  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, o.X)

        val to: S => PowerSeries[X, A] = s =>
          val (xo, a) = o.to(s)
          val arr     = new Array[AnyRef](1)
          arr(0)      = a.asInstanceOf[AnyRef]
          PowerSeries(xo, ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])

        val from: PowerSeries[X, B] => T = ps =>
          o.from((ps.xo, ps.vs.head))

  given either2ps: Composer[Either, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Option[o.X])

        val to: S => PowerSeries[X, A] = s =>
          o.to(s) match
            case Left(x) => PowerSeries(Some(x), emptyArraySeq[A])
            case Right(a) =>
              val arr = new Array[AnyRef](1)
              arr(0)  = a.asInstanceOf[AnyRef]
              PowerSeries(None, ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])

        val from: PowerSeries[X, B] => T = ps =>
          ps.xo.asInstanceOf[Option[o.X]] match
            case Some(x) => o.from(Left(x))
            case None    => o.from(Right(ps.vs.head))

  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Either[Fst[o.X], Snd[o.X]])

        val to: S => PowerSeries[X, A] = s =>
          o.to(s).affine match
            case Left(x0) => PowerSeries(Left(x0), emptyArraySeq[A])
            case Right((x1, b)) =>
              val arr = new Array[AnyRef](1)
              arr(0)  = b.asInstanceOf[AnyRef]
              PowerSeries(Right(x1), ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])

        val from: PowerSeries[X, B] => T = ps =>
          ps.xo.asInstanceOf[Either[Fst[o.X], Snd[o.X]]] match
            case Left(fx)  => o.from(Affine.ofLeft(fx))
            case Right(sx) => o.from(Affine.ofRight(sx -> ps.vs.head))

  /** Shared zero-length `ArraySeq` singleton cast to whatever element type the call site asks
    * for. Empty arrays are type-oblivious — an `Array[AnyRef]` of length 0 is interchangeable
    * with `Array[A]` of length 0 for any reference `A`.
    */
  private val emptyObjArr: Array[AnyRef]    = new Array[AnyRef](0)
  private inline def emptyArraySeq[A]: ArraySeq[A] =
    ArraySeq.unsafeWrapArray(emptyObjArr).asInstanceOf[ArraySeq[A]]
