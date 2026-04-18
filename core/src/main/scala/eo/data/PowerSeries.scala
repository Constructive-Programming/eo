package eo
package data

import scala.annotation.nowarn
import scala.collection.immutable.ArraySeq

import cats.Applicative
import cats.instances.arraySeq.given
import cats.syntax.functor.*
import cats.syntax.traverse.*

import optics.Optic

/** Carrier for the `PowerSeries`-style `Traversal`: pairs an existential leftover `Snd[A]` with a
  * flat contiguous `ArraySeq[B]` of focused elements.
  *
  * Storage is a flat `ArraySeq[B]` backed by a plain `Array[AnyRef]` — so iteration and slicing
  * are a single `arraycopy` each, and hit the JVM's prefetcher well. The `associateLeft` /
  * `associateRight` paths grow an internal `Array[AnyRef]` in place (see [[ObjArrBuilder]])
  * and publish via `ArraySeq.unsafeWrapArray`, avoiding the buffer→toArray→wrap double-copy
  * path that `ArrayBuffer` takes.
  *
  * No `ClassTag[B]` is required at the API boundary. Every generic `B` inside the optic
  * machinery erases to `Object` on the JVM; the builder stores `Array[AnyRef]` directly and
  * narrows the runtime type back to `ArraySeq[B]` at publish time.
  *
  * See `benchmarks/src/main/scala/eo/bench/PowerSeriesBench.scala` for the runtime profile.
  */
class PowerSeries[A, B](val ps: Tuple2[Snd[A], ArraySeq[B]]) extends AnyVal:
  override def toString(): String = ps.toString()

/** Typeclass instances for [[PowerSeries]]. */
object PowerSeries:

  def unapply[A, B](ps: PowerSeries[A, B]): Tuple2[Snd[A], ArraySeq[B]] =
    ps.ps

  /** Minimal grow-on-demand `Array[AnyRef]` builder. Hand-rolled rather than using
    * `ArrayBuffer` so the final freeze is a zero-copy `unsafeWrapArray` on the builder's
    * own array (with a truncation copy only when the buffer was over-allocated). Doubles
    * capacity on overflow; inlines the inner-loop arithmetic the JMH hot path benefits from.
    */
  private final class ObjArrBuilder(initialCapacity: Int = 16):
    private var arr: Array[AnyRef] = new Array[AnyRef](math.max(initialCapacity, 1))
    private var len: Int           = 0

    def size: Int = len

    def append(x: AnyRef): Unit =
      if len == arr.length then grow(len + 1)
      arr(len) = x
      len += 1

    def appendAllFromArraySeq[A](src: ArraySeq[A]): Unit =
      val n = src.length
      if len + n > arr.length then grow(len + n)
      var i = 0
      while i < n do
        arr(len + i) = src(i).asInstanceOf[AnyRef]
        i += 1
      len += n

    private def grow(minCap: Int): Unit =
      var newCap = arr.length * 2
      while newCap < minCap do newCap *= 2
      val newArr = new Array[AnyRef](newCap)
      System.arraycopy(arr, 0, newArr, 0, len)
      arr = newArr

    /** Return the accumulated array exactly-sized. No-copy when the builder filled its
      * internal array exactly; one arraycopy to truncate when it didn't. */
    def freezeAs[A]: ArraySeq[A] =
      val finalArr =
        if len == arr.length then arr
        else
          val trimmed = new Array[AnyRef](len)
          System.arraycopy(arr, 0, trimmed, 0, len)
          trimmed
      ArraySeq.unsafeWrapArray(finalArr).asInstanceOf[ArraySeq[A]]

  given map: ForgetfulFunctor[PowerSeries] with
    def map[X, A, B](psa: PowerSeries[X, A], f: A => B): PowerSeries[X, B] =
      val src = psa.ps._2
      val n   = src.length
      val arr = new Array[AnyRef](n)
      var i   = 0
      while i < n do
        arr(i) = f(src(i)).asInstanceOf[AnyRef]
        i += 1
      PowerSeries(psa.ps._1 -> ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[B]])

  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]
        : PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      psa => f => psa.ps._2.traverse(f).map(vb => PowerSeries(psa.ps._1 -> vb))

  given assoc[X, Y]: AssociativeFunctor[PowerSeries, X, Y] with
    type SndZ = (Snd[X], ArraySeq[(Int, Snd[Y])])
    type Z    = (Int, SndZ)

    def associateLeft[S, A, C]
        : (S, S => PowerSeries[X, A], A => PowerSeries[Y, C]) => PowerSeries[Z, C] =
      (s, f, g) =>
        val (x, va)    = f(s).ps
        val indexBuf   = new ObjArrBuilder(va.length)
        val flatBuf    = new ObjArrBuilder()
        var i          = 0
        while i < va.length do
          val (y, vy) = g(va(i)).ps
          indexBuf.append((vy.length, y).asInstanceOf[AnyRef])
          flatBuf.appendAllFromArraySeq(vy)
          i += 1
        PowerSeries(
          (x, indexBuf.freezeAs[(Int, Snd[Y])]) -> flatBuf.freezeAs[C]
        )

    def associateRight[D, B, T]
        : (PowerSeries[Z, D], PowerSeries[Y, D] => B, PowerSeries[X, B] => T) => T =
      (ps, g, f) =>
        val ((x, ys), vys) = ps.ps
        val resultBuf      = new ObjArrBuilder(ys.length)
        var offset         = 0
        var i              = 0
        while i < ys.length do
          val (len, y) = ys(i)
          val chunk    = vys.slice(offset, offset + len)
          resultBuf.append(g(PowerSeries(y -> chunk)).asInstanceOf[AnyRef])
          offset      += len
          i           += 1
        f(PowerSeries(x -> resultBuf.freezeAs[B]))

  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, o.X)

        def to: S => PowerSeries[X, A] =
          s =>
            val (xo, a) = o.to(s)
            val arr     = new Array[AnyRef](1)
            arr(0)      = a.asInstanceOf[AnyRef]
            PowerSeries(xo -> ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])

        def from: PowerSeries[X, B] => T =
          ps => o.from(ps.ps._1 -> ps.ps._2.head)

  given either2ps: Composer[Either, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Option[o.X])

        def to: S => PowerSeries[X, A] =
          s =>
            o.to(s)
              .fold(
                x => PowerSeries(Some(x) -> emptyArraySeq[A]),
                a =>
                  val arr = new Array[AnyRef](1)
                  arr(0)  = a.asInstanceOf[AnyRef]
                  PowerSeries(None -> ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])
                ,
              )

        @nowarn("msg=cannot be checked at runtime")
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Some(x), _) => o.from(Left(x))
          case PowerSeries(_, vec)     => o.from(Right(vec.head))

  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Either[Fst[o.X], Snd[o.X]])

        def to: S => PowerSeries[X, A] =
          s =>
            o.to(s)
              .affine
              .fold(
                x0     => PowerSeries(Left(x0) -> emptyArraySeq[A]),
                (x1, b) =>
                  val arr = new Array[AnyRef](1)
                  arr(0)  = b.asInstanceOf[AnyRef]
                  PowerSeries(Right(x1) -> ArraySeq.unsafeWrapArray(arr).asInstanceOf[ArraySeq[A]])
                ,
              )

        @nowarn("msg=cannot be checked at runtime")
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Left(fx), _)    => o.from(Affine.ofLeft(fx))
          case PowerSeries(Right(sx), vec) => o.from(Affine.ofRight(sx -> vec.head))

  /** Shared zero-length `ArraySeq` singleton cast to whatever element type the call site asks
    * for. Empty arrays are type-oblivious — an `Array[AnyRef]` of length 0 is interchangeable
    * with `Array[A]` of length 0 for any reference `A`.
    */
  private val emptyObjArr: Array[AnyRef]    = new Array[AnyRef](0)
  private inline def emptyArraySeq[A]: ArraySeq[A] =
    ArraySeq.unsafeWrapArray(emptyObjArr).asInstanceOf[ArraySeq[A]]
