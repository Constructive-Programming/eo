package dev.constructive.eo
package zio

import _root_.zio.{Chunk, NonEmptyChunk}
import cats.{Applicative, Eval, Foldable, Traverse}

import optics.{Optional, Traversal}

/** Element optics for zio's `Chunk` / `NonEmptyChunk` — the collection legs every other seam in
  * this module stands on (`DynamicValue.Sequence`, zio-json arrays, `BinaryCodec` payloads all
  * speak Chunk).
  *
  * zio ships no cats instances (the orphan `Traverse[Chunk]` given belongs to zio-interop-cats), so
  * these route [[optics.Traversal.pEach]] through private `Traverse` adapters — the adapter stays
  * an implementation detail of the constructor, exactly the constructor-not-given doctrine.
  */
object Chunks:

  /** Traversal over every element of a `Chunk` — polymorphic in the focus. */
  def each[A, B]: Traversal[Chunk[A], Chunk[B], A, B] =
    Traversal.pEach[Chunk, A, B](using chunkTraverse)

  /** Traversal over every element of a `NonEmptyChunk`. Size-preserving by construction
    * (element-wise `modify` / broadcast `replace`), so non-emptiness survives every write.
    */
  def eachNonEmpty[A, B]: Traversal[NonEmptyChunk[A], NonEmptyChunk[B], A, B] =
    Traversal.pEach[NonEmptyChunk, A, B](using nonEmptyChunkTraverse)

  /** Optional onto index `i` — siblings survive writes, out-of-range misses pass through. */
  def at[A](i: Int): Optional[Chunk[A], Chunk[A], A, A] =
    Optional[Chunk[A], Chunk[A], A, A](
      c => if c.isDefinedAt(i) then Right(c(i)) else Left(c),
      (c, a) => if c.isDefinedAt(i) then c.updated(i, a) else c,
    )

  private val chunkTraverse: Traverse[Chunk] = new Traverse[Chunk]:
    def traverse[G[_]: Applicative, A, B](fa: Chunk[A])(f: A => G[B]): G[Chunk[B]] =
      fa.foldLeft(Applicative[G].pure(Chunk.empty[B])) { (acc, a) =>
        Applicative[G].map2(acc, f(a))(_ :+ _)
      }
    def foldLeft[A, B](fa: Chunk[A], b: B)(f: (B, A) => B): B = fa.foldLeft(b)(f)
    def foldRight[A, B](fa: Chunk[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      Foldable.iterateRight(fa, lb)(f)

  private val nonEmptyChunkTraverse: Traverse[NonEmptyChunk] = new Traverse[NonEmptyChunk]:
    def traverse[G[_]: Applicative, A, B](fa: NonEmptyChunk[A])(f: A => G[B]): G[NonEmptyChunk[B]] =
      val head = Applicative[G].map(f(fa.head))(NonEmptyChunk.single)
      fa.tail.foldLeft(head)((acc, a) => Applicative[G].map2(acc, f(a))(_ :+ _))
    def foldLeft[A, B](fa: NonEmptyChunk[A], b: B)(f: (B, A) => B): B = fa.toChunk.foldLeft(b)(f)
    def foldRight[A, B](fa: NonEmptyChunk[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      Foldable.iterateRight(fa.toChunk, lb)(f)
