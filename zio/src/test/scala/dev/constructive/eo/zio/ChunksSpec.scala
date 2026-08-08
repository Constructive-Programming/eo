package dev.constructive.eo
package zio

import _root_.zio.{Chunk, NonEmptyChunk}
import org.specs2.mutable.Specification

import optics.Traversal

class ChunksSpec extends Specification:

  private def bump[S](s: S)(using m: CanModify[S, Int]): S = m.modify(_ + 1)(s)
  private def total[S](s: S)(using f: CanFold[S, Int]): Int = f.foldMap(identity)(s)

  "Chunks.each" should {
    "serve CanModify and CanFold through a client-bound given" >> {
      given Traversal[Chunk[Int], Chunk[Int], Int, Int] = Chunks.each
      (bump(Chunk(1, 2, 3)) === Chunk(2, 3, 4))
        .and(total(Chunk(1, 2, 3)) === 6)
        .and(bump(Chunk.empty[Int]) === Chunk.empty[Int])
    }
    "change the focus type" >> {
      Chunks.each[Int, String].modify(_.toString)(Chunk(1, 2)) === Chunk("1", "2")
    }
  }

  "Chunks.at" should {
    "read and rewrite one index, siblings surviving" >> {
      val at1 = Chunks.at[Int](1)
      (at1.getOption(Chunk(1, 2, 3)) === Some(2))
        .and(at1.replace(9)(Chunk(1, 2, 3)) === Chunk(1, 9, 3))
    }
    "pass writes through on out-of-range" >> {
      (Chunks.at[Int](5).getOption(Chunk(1)) === None)
        .and(Chunks.at[Int](5).replace(9)(Chunk(1)) === Chunk(1))
    }
  }

  "Chunks.eachNonEmpty" should {
    "modify every element, non-emptiness surviving" >> {
      val nec = NonEmptyChunk(1, 2, 3)
      (Chunks.eachNonEmpty[Int, Int].modify(_ * 2)(nec) === NonEmptyChunk(2, 4, 6))
        .and(
          Chunks.eachNonEmpty[Int, String].modify(_.toString)(nec) === NonEmptyChunk(
            "1",
            "2",
            "3",
          )
        )
    }
  }
