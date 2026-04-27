package dev.constructive.eo
package data

import cats.{Applicative, Eval, Foldable, Functor, Traverse}

/** cats typeclass instances for [[PSVec]] — required so `MultiFocus[PSVec]` can absorb the
  * PowerSeries carrier.
  *
  * `PSVec` is array-backed and immutable; `Functor.map` allocates a fresh `Array[AnyRef]` exactly
  * once and wraps via `PSVec.unsafeWrap`. `Foldable` walks via `apply(i)` (the `Slice` variant's
  * indexed access is O(1)). `Traverse.traverse` builds an applicative chain over the focus vector
  * identical in shape to `PowerSeries.traverse`.
  *
  * Lives in a separate file to keep `PSVec.scala` free of cats imports — the carrier itself is
  * data-only.
  */
private[eo] object PSVecInstances:

  given functor: Functor[PSVec] with
    def map[A, B](fa: PSVec[A])(f: A => B): PSVec[B] =
      val n = fa.length
      if n == 0 then PSVec.empty[B]
      else
        val arr = new Array[AnyRef](n)
        var i = 0
        while i < n do
          arr(i) = f(fa(i)).asInstanceOf[AnyRef]
          i += 1
        PSVec.unsafeWrap[B](arr)

  given foldable: Foldable[PSVec] with

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      var acc = b
      val n = fa.length
      var i = 0
      while i < n do
        acc = f(acc, fa(i))
        i += 1
      acc

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] =
      val n = fa.length
      def loop(i: Int): Eval[B] =
        if i >= n then lb
        else f(fa(i), Eval.defer(loop(i + 1)))
      Eval.defer(loop(0))

    override def size[A](fa: PSVec[A]): Long = fa.length.toLong

  given traverse: Traverse[PSVec] with

    def traverse[G[_]: Applicative, A, B](fa: PSVec[A])(f: A => G[B]): G[PSVec[B]] =
      val G = Applicative[G]
      val n = fa.length
      if n == 0 then G.pure(PSVec.empty[B])
      else
        var acc: G[Array[AnyRef]] = G.pure(new Array[AnyRef](n))
        var i = 0
        while i < n do
          val idx = i
          val gb = f(fa(idx))
          acc = G.map2(acc, gb) { (a, b) =>
            a(idx) = b.asInstanceOf[AnyRef]
            a
          }
          i += 1
        G.map(acc)(arr => PSVec.unsafeWrap[B](arr))

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      foldable.foldLeft(fa, b)(f)

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] = foldable.foldRight(fa, lb)(f)
