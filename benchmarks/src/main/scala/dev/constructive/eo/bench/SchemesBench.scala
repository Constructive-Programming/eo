package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import higherkindness.droste.data.Fix
import higherkindness.droste.scheme

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.bench.fixture.SchemesFixtures.given
import dev.constructive.eo.bench.fixture.PlatedTrees.eoBin // given Plated[Bin]
import dev.constructive.eo.schemes.Schemes

/** Recursion schemes — `cata` / `ana` / `hylo` — four ways, on the same workload:
  *
  *   - **eo** — schemes as optics over the *native* `Bin` (`cata` driven by `Plated[Bin]`, `ana` a
  *     `Review`, fused `hylo` a `Getter`), all on one stack-safe `PSVec` heap machine.
  *   - **eoF** — the *typed* pattern-functor path (`cataF`/`anaF`/`hyloF` over `BinF` via a `Basis`
  *     + `Traverse[BinF]`), a `cats.Eval` trampoline. This row quantifies the typed path's
  *     allocation against droste's basic schemes — the U6 measurement that informs the
  *     Eval-vs-explicit-heap-machine driver decision.
  *   - **droste** — the pattern-functor + `Fix[BinF]` encoding (`scheme.cata/ana/hylo`). NB droste's
  *     *basic* schemes are stack-*unsafe* (naive recursion); `eoF` delivers the stack-safety they
  *     lack, so the comparison is not apples-to-apples.
  *   - **hand** — plain recursion on `Bin`, the baseline you'd write without either library.
  *
  * Workload is a perfect binary tree of `2^Depth` `Leaf(1)`s (Depth = 12 ⇒ 4096 leaves, 8191
  * nodes). `cata` folds a prebuilt tree to its leaf-sum; `ana` builds the tree from a seed; `hylo`
  * builds-and-folds in one pass (eo/droste fuse — no intermediate tree; hand is just recursion).
  *
  * JMH caveats apply (see the repo's bench docs): trustworthy comparison needs a quiet box, and the
  * heap-allocation (`-prof gc`, B/op) signal is steadier than ns/op here.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class SchemesBench extends JmhDefaults:
  import SchemesFixtures.*

  final val Depth = 12 // 2^12 = 4096 leaves, 8191 nodes

  // Prebuilt trees for the cata benches (construction not measured).
  val eoTree: Bin = balancedBin(Depth)
  val fixTree: Fix[BinF] = balancedFix(Depth)

  // Prebuilt scheme optics / functions (construction not measured).
  val eoCataG = Schemes.cata(eoSum) // Getter[Bin, Int]
  val eoHyloG = Schemes.hylo(eoExpand, eoHyloAlg) // Getter[Int, Int]
  val eoAnaR = Schemes.ana(eoAnaCoalg) // Review[Bin, Int]

  // typed pattern-functor path (Eval trampoline over Traverse[BinF])
  val eoCataFG = Schemes.cataF(eoTypedSum) // Getter[Bin, Int]
  val eoHyloFG = Schemes.hyloF(eoTypedCoalg, eoTypedHyloAlg) // Getter[Int, Int]
  val eoAnaFR = Schemes.anaF[BinF, Int, Bin](eoTypedCoalg) // Review[Bin, Int]

  val drosteCataF: Fix[BinF] => Int = scheme.cata(drosteSum)
  val drosteHyloF: Int => Int = scheme.hylo(drosteSum, drosteBuild)
  val drosteAnaF: Int => Fix[BinF] = scheme.ana(drosteBuild)

  // ----- cata: fold a prebuilt tree to its leaf-sum --------------------------
  @Benchmark def eoCata: Int = eoCataG.get(eoTree)
  @Benchmark def eoCataF: Int = eoCataFG.get(eoTree)
  @Benchmark def drosteCata: Int = drosteCataF(fixTree)
  @Benchmark def handCata: Int = handSum(eoTree)

  // ----- hylo: build + fold from a seed, fused (no intermediate tree) --------
  @Benchmark def eoHylo: Int = eoHyloG.get(Depth)
  @Benchmark def eoHyloF: Int = eoHyloFG.get(Depth)
  @Benchmark def drosteHylo: Int = drosteHyloF(Depth)
  @Benchmark def handHylo: Int = SchemesFixtures.handHylo(Depth)

  // ----- ana: build the tree from a seed (materializing) ---------------------
  @Benchmark def eoAna: Bin = eoAnaR.reverseGet(Depth)
  @Benchmark def eoAnaF: Bin = eoAnaFR.reverseGet(Depth)
  @Benchmark def drosteAna: Fix[BinF] = drosteAnaF(Depth)
  @Benchmark def handAna: Bin = handBuild(Depth)
