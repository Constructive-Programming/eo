package dev.constructive.eo
package schemes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import cats.Eval
import org.specs2.mutable.Specification

import schemes.samples.{Bin, BinF}
import zoo.*

/** Concurrency spec for the typed recursion-scheme engines.
  *
  * Motivation: [[Machines]] documents a thread-safety model — every machine allocates its own
  * mutable state per invocation, and the only shared values ([[Machines.NoChildren]],
  * [[Machines.AscendToken]]) are immutable by construction. These tests exercise that claim with N
  * concurrent tasks racing on shared prebuilt fixtures.
  *
  * Design: 16 `Future` tasks launched in parallel, each running a MIX of schemes over shared
  * prebuilt optics and a shared input tree. Every task asserts its result equals the expected
  * value. No sleeps; `Await` with `Duration.Inf` gives a deterministic pass/fail.
  */
class SchemesConcurrencySpec extends Specification:

  private val N = 16

  // Shared fixtures — prebuilt once, used from all N concurrent tasks.
  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  // leaf sum of `tree` = 10
  private val ExpectedSum = 10

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // Prebuilt shared optics (shared across all tasks — the thread-safety claim under test).
  private val sharedCata = Schemes.cata(sumAlg)

  private val sharedPara = Schemes.para[BinF, Bin, Int] { (_, layer) =>
    layer match
      case BinF.LeafF(n)                => n
      case BinF.BranchF((_, l), (_, r)) => l + r
  }

  private val sharedHisto = Schemes.histo[BinF, Bin, Int] { (_, layer) =>
    layer match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l.head + r.head
  }

  // Per-task seed for ana/hylo/futu (varies per task to exercise different input paths).
  private def expand(n: Int): BinF[Int] =
    if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  "N=16 concurrent tasks running a mix of cata/hylo/ana/para/histo/futu/cataM on shared fixtures all return correct results" >> {
    val tasks: Seq[Future[Boolean]] = (1 to N).map { taskId =>
      Future {
        // (a) cata leaf-sum on the shared Bin tree
        val cataOk = sharedCata.get(tree) == ExpectedSum

        // (b) hylo from a per-task seed — independent fold, no shared state
        val seed = taskId % 5 // seeds 0–4
        val hyloResult = Schemes
          .hylo[BinF, Int, Int](
            expand,
            (_, fa) =>
              fa match
                case BinF.LeafF(n)      => n
                case BinF.BranchF(l, r) => l + r,
          )
          .get(seed)
        val expectedHylo =
          Schemes.cata(sumAlg).get(Schemes.ana[BinF, Int, Bin](expand).get(seed))
        val hyloOk = hyloResult == expectedHylo

        // (c) ana builds a per-task Bin from a per-task seed, cata folds it back
        val built: Bin = Schemes.ana[BinF, Int, Bin](expand).get(seed)
        val anaOk = Schemes.cata(sumAlg).get(built) == expectedHylo

        // (d) para on the shared tree ignoring subterms == cata
        val paraOk = sharedPara.get(tree) == ExpectedSum

        // (e) histo on the shared tree heads-only == cata
        val histoOk = sharedHisto.get(tree) == ExpectedSum

        // (f) futu single-layer-per-step == ana
        val futuCoalg: Int => BinF[Coattr[BinF, Int]] = n =>
          if n <= 1 then BinF.LeafF(1)
          else BinF.BranchF(Coattr.Pure(n / 2), Coattr.Pure(n - n / 2))
        val futuBuilt: Bin = Schemes.futu[BinF, Int, Bin](futuCoalg).get(seed)
        val futuOk = Schemes.cata(sumAlg).get(futuBuilt) == expectedHylo

        // (g) cataM[Eval].run forced per task
        val cataMResult =
          Schemes
            .cataM[Eval, BinF, Bin, Int]((s, fa) => Eval.now(sumAlg(s, fa)))
            .run(tree)
            .value
        val cataMOk = cataMResult == ExpectedSum

        cataOk && hyloOk && anaOk && paraOk && histoOk && futuOk && cataMOk
      }
    }

    val results = Await.result(Future.sequence(tasks), Duration.Inf)
    results.forall(identity) must beTrue
  }
