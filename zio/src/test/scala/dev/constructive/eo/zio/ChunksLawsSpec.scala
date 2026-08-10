package dev.constructive.eo
package zio

import _root_.zio.{Chunk, NonEmptyChunk}
import cats.Functor
import cats.laws.discipline.TraverseTests
import dev.constructive.eo.laws.TraversalLaws
import dev.constructive.eo.laws.discipline.TraversalTests
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Laws for the Chunk element optics AND for the hand-written cats `Traverse` adapters they stand
  * on. The adapter laws matter as much as the optic laws: `TraverseTraversal` collects foci in fold
  * order and reassembles them through `Functor.map`, so an adapter whose `map` and `traverse` visit
  * elements in different orders would silently permute written foci — and this is exactly where the
  * `map` override (added for the write-path allocation win) could have introduced such a skew.
  */
class ChunksLawsSpec extends Specification with Discipline:

  given [A: Arbitrary]: Arbitrary[Chunk[A]] =
    Arbitrary(Gen.listOf(Arbitrary.arbitrary[A]).map(Chunk.fromIterable))

  given [A: Arbitrary]: Arbitrary[NonEmptyChunk[A]] =
    Arbitrary(
      for
        h <- Arbitrary.arbitrary[A]
        t <- Gen.listOf(Arbitrary.arbitrary[A])
      yield NonEmptyChunk.fromIterable(h, t)
    )

  given [A: Cogen]: Cogen[Chunk[A]] = Cogen[List[A]].contramap(_.toList)
  given [A: Cogen]: Cogen[NonEmptyChunk[A]] = Cogen[List[A]].contramap(_.toChunk.toList)

  given Functor[Chunk] = Chunks.chunkTraverse
  given Functor[NonEmptyChunk] = Chunks.nonEmptyChunkTraverse

  // Chunk equality is element-wise and representation-independent, so `==` is a lawful Eq.
  given [A]: cats.Eq[Chunk[A]] = cats.Eq.fromUniversalEquals
  given [A]: cats.Eq[NonEmptyChunk[A]] = cats.Eq.fromUniversalEquals

  checkAll(
    "Traverse[Chunk] (the adapter behind Chunks.each)",
    TraverseTests[Chunk](using Chunks.chunkTraverse)
      .traverse[Int, Int, Int, Int, Option, Option],
  )

  checkAll(
    "Traverse[NonEmptyChunk]",
    TraverseTests[NonEmptyChunk](using Chunks.nonEmptyChunkTraverse)
      .traverse[Int, Int, Int, Int, Option, Option],
  )

  checkAll(
    "Traversal[Chunk, Int] via Chunks.each",
    new TraversalTests[Chunk, Int]:
      val laws: TraversalLaws[Chunk, Int] = new TraversalLaws[Chunk, Int]:
        val traversal = Chunks.each[Int, Int]
    .traversal,
  )

  checkAll(
    "Traversal[NonEmptyChunk, Int] via Chunks.eachNonEmpty",
    new TraversalTests[NonEmptyChunk, Int]:
      val laws: TraversalLaws[NonEmptyChunk, Int] = new TraversalLaws[NonEmptyChunk, Int]:
        val traversal = Chunks.eachNonEmpty[Int, Int]
    .traversal,
  )
