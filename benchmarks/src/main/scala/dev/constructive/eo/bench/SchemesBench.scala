package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import higherkindness.droste.data.Fix
import higherkindness.droste.scheme

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.bench.fixture.SchemesFixtures.given
import dev.constructive.eo.schemes.Schemes

/** Recursion schemes — `cata` / `ana` / `hylo` — four ways, on the same workload:
  *
  *   - **eoF** — the typed pattern-functor path (`cata`/`ana`/`hylo` over `BinF` via a `Basis` +
  *     `Traverse[BinF]`) on the stack-safe `foldLayered` heap machine. (The untyped `PSVec` path
  *     was removed once the typed path subsumed it.)
  *   - **droste** — the pattern-functor + `Fix[BinF]` encoding (`scheme.cata/ana/hylo`). NB
  *     droste's *basic* schemes are stack-*unsafe* (naive recursion); `eoF` delivers the
  *     stack-safety they lack, so the comparison is not apples-to-apples.
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
  // typed pattern-functor path (Eval trampoline over Traverse[BinF])
  val eoCataG = Schemes.cata(eoTypedSum) // Getter[Bin, Int]
  val eoHyloG = Schemes.hylo(eoTypedCoalg, eoTypedHyloAlg) // Getter[Int, Int]
  val eoAnaG = Schemes.ana[BinF, Int, Bin](eoTypedCoalg) // Getter[Int, Bin]

  val drosteCataF: Fix[BinF] => Int = scheme.cata(drosteSum)
  val drosteHyloF: Int => Int = scheme.hylo(drosteSum, drosteBuild)
  val drosteAnaF: Int => Fix[BinF] = scheme.ana(drosteBuild)

  // ----- cata: fold a prebuilt tree to its leaf-sum --------------------------
  @Benchmark def eoCata: Int = eoCataG.get(eoTree)
  @Benchmark def drosteCata: Int = drosteCataF(fixTree)
  @Benchmark def handCata: Int = handSum(eoTree)

  // ----- hylo: build + fold from a seed, fused (no intermediate tree) --------
  @Benchmark def eoHylo: Int = eoHyloG.get(Depth)
  @Benchmark def drosteHylo: Int = drosteHyloF(Depth)
  @Benchmark def handHylo: Int = SchemesFixtures.handHylo(Depth)

  // ----- ana: build the tree from a seed (materializing) ---------------------
  @Benchmark def eoAna: Bin = eoAnaG.get(Depth)
  @Benchmark def drosteAna: Fix[BinF] = drosteAnaF(Depth)
  @Benchmark def handAna: Bin = handBuild(Depth)

  // ----- the zoo: para / apo / histo / futu (eo native routes vs droste.zoo) --

  val eoParaG = Schemes.para[BinF, Bin, Int](eoParaAlg)
  val drosteParaFn: Fix[BinF] => Int = scheme.zoo.para(drosteParaAlg)
  val eoApoG = Schemes.apo[BinF, Int, Bin](eoApoCoalg)
  val drosteApoFn: Int => Fix[BinF] = scheme.zoo.apo(drosteApoCoalg)
  val eoHistoG = Schemes.histo[BinF, Bin, Int](eoHistoAlg)
  val drosteHistoFn: Fix[BinF] => Int = scheme.zoo.histo(drosteHistoAlg)
  val eoFutuG = Schemes.futu[BinF, Int, Bin](eoFutuCoalg)
  val drosteFutuFn: Int => Fix[BinF] = scheme.zoo.futu(drosteFutuCoalg)

  @Benchmark def eoPara: Int = eoParaG.get(eoTree)
  @Benchmark def drostePara: Int = drosteParaFn(fixTree)
  @Benchmark def eoApo: Bin = eoApoG.get(Depth)
  @Benchmark def drosteApo: Fix[BinF] = drosteApoFn(Depth)
  @Benchmark def eoHisto: Int = eoHistoG.get(eoTree)
  @Benchmark def drosteHisto: Int = drosteHistoFn(fixTree)
  @Benchmark def eoFutu: Bin = eoFutuG.get(Depth)
  @Benchmark def drosteFutu: Fix[BinF] = drosteFutuFn(Depth)

  // ----- apo with ONE BIG GRAFT. VERIFIED (the D6 check): droste's zoo.apo
  // ALSO grafts O(1) here — its R is the fixed point, so Left(fix) embeds by
  // reference. The honest claim is therefore PARITY on the native routes (both
  // ~ns-flat regardless of graft size), with eo adding the law-shaped eq
  // guarantee; the O(graft) re-walk contrast applies to the GENERIC distApo
  // route (distApo, a law fixture only), not to droste.zoo.apo.

  val eoApoGraftG = Schemes.apo[BinF, Int, Bin] { d =>
    if d == 0 then BinF.NodeF(Left(eoTree), Right(-1)) else BinF.LeafF(1)
  }

  val drosteApoGraftFn: Int => Fix[BinF] = scheme
    .zoo
    .apo(
      higherkindness.droste.RCoalgebra { (d: Int) =>
        if d == 0 then BinF.NodeF(Left(fixTree), Right(-1)) else BinF.LeafF(1)
      }
    )

  @Benchmark def eoApoGraft: Bin = eoApoGraftG.get(0)
  @Benchmark def drosteApoGraft: Fix[BinF] = drosteApoGraftFn(0)

  // ----- materializing refold: andThen-spelling vs manual (both build the Bin) --
  // Post directional-flip, `ana.andThen(cata)` IS the materializing hylo (builds the
  // Bin, then folds) — the two spellings should allocate identically. The *fused*
  // (no-intermediate-Bin) contrast is `eoHylo` above (~half the B/op).

  val eoRefoldAndThenG = Schemes.ana[BinF, Int, Bin](eoTypedCoalg).andThen(Schemes.cata(eoTypedSum))

  @Benchmark def eoRefoldAndThen: Int = eoRefoldAndThenG.get(Depth)
  @Benchmark def eoRefoldManual: Int = eoCataG.get(eoAnaG.get(Depth))

  // ----- generic decoration route (user-written Gather, no identity fast path) --

  val eoCataGenericG = Schemes.cata[BinF, Bin, Int, Int](userIdGather)(eoTypedSum)

  @Benchmark def eoCataGenericRoute: Int = eoCataGenericG.get(eoTree)

  // ----- the M path at Id: the tailRecM-lifted machine's per-event floor ------

  val eoHyloMRunner =
    Schemes.hyloM[cats.Id, BinF, Int, Int](d => eoTypedCoalg(d), (s, fa) => eoTypedHyloAlg(s, fa))

  @Benchmark def eoHyloM: Int = eoHyloMRunner.run(Depth)
