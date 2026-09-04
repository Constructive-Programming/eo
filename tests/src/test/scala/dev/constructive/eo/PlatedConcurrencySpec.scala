package dev.constructive.eo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import dev.constructive.eo.optics.Plated
import org.specs2.mutable.Specification

import PlatedFixtures.given

/** Concurrency spec for [[Plated]] operations.
  *
  * [[Plated.transform]], [[Plated.universe]], and [[Plated.rewrite]] build no shared mutable state
  * across invocations — each call to the underlying machine is independent. These tests exercise
  * that claim with N=16 concurrent tasks running `transform`, `universe`, and `rewrite` over a
  * shared prebuilt structure, asserting all results correct and deterministic.
  */
class PlatedConcurrencySpec extends Specification:

  private val N = 16

  // Shared prebuilt tree — used from all N concurrent tasks.
  private val sharedBin: Bin =
    Bin.Node(Bin.Node(Bin.Leaf(1), Bin.Leaf(2)), Bin.Node(Bin.Leaf(3), Bin.Leaf(4)))

  // Expected results for each operation on sharedBin.
  // universe count: 7 nodes total (4 leaves + 3 nodes, root included).
  private val ExpectedUniverseSize = 7

  // After incrementing every leaf by 1 the leaf sum becomes 1+1 + 2+1 + 3+1 + 4+1 = 14.
  private val incLeaf: Bin => Bin = {
    case Bin.Leaf(v) => Bin.Leaf(v + 1)
    case node        => node
  }

  private def leafSum(b: Bin): Int = b match
    case Bin.Leaf(v)    => v
    case Bin.Node(l, r) => leafSum(l) + leafSum(r)

  private val ExpectedTransformedSum = 14

  // rewrite: fold adjacent Node(Leaf, Leaf) → single Leaf sum.
  // sharedBin → Node(Leaf(3), Leaf(7)) → Leaf(10).
  private val foldAdjacent: Bin => Option[Bin] = {
    case Bin.Node(Bin.Leaf(a), Bin.Leaf(b)) => Some(Bin.Leaf(a + b))
    case _                                  => None
  }

  "N=16 concurrent Plated.transform + universe + rewrite on a shared structure all return correct results" >> {
    val tasks: Seq[Future[Boolean]] = (1 to N).map { _ =>
      Future {
        // (a) transform: increment every leaf
        val transformed = Plated.transform(incLeaf)(sharedBin)
        val transformOk = leafSum(transformed) == ExpectedTransformedSum

        // (b) universe: count all nodes
        val universeOk = Plated.universe(sharedBin).length == ExpectedUniverseSize

        // (c) rewrite: fold adjacent leaves to a fixpoint
        val rewrote = Plated.rewrite(foldAdjacent)(sharedBin)
        val rewriteOk = rewrote == Bin.Leaf(10)

        transformOk && universeOk && rewriteOk
      }
    }

    val results = Await.result(Future.sequence(tasks), Duration.Inf)
    results.forall(identity) must beTrue
  }
