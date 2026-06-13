package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import optics.Getter
import schemes.samples.{Bin, BinF}

/** The hylo law as composition: `ana.andThen(cata) == hylo`.
  *
  * Both `ana` and `cata` are forward `Getter`s (`Getter[Seed, S]` and `Getter[S, A]`), so they
  * compose at the focus seam with the core fused `Getter.andThen` — no `cross`, no clone classes.
  *   - `ana.andThen(cata)` is the **materialising** hylo: it builds the full `S`, then folds it.
  *   - `Schemes.hylo(coalg, alg)` is the **fused** hylo: one pass, no intermediate `S`.
  *   - They agree on every seed (the hylo law) for a pure algebra; for a node-reading para algebra
  *     only under the seed↔`embed(coalg(seed))` correspondence.
  */
class FusionSpec extends Specification:

  // Deep examples: one-at-a-time to bound peak heap (shared test JVM).
  sequential

  private def expand(n: Int): BinF[Int] =
    if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // Pure algebra, seed-typed for hylo (node argument ignored on both sides).
  private val sumAlgSeed: (Int, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  "ana.andThen(cata) composes via the core fused Getter.andThen (no cross)" >> {
    val hylo: Getter[Int, Int] = Schemes.ana[BinF, Int, Bin](expand).andThen(Schemes.cata(sumAlg))
    hylo.get(6) === 6 // six leaves of weight 1
  }

  "ana.andThen(cata) == cata.get ∘ ana.get (the materialising composition)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13)
    val ana = Schemes.ana[BinF, Int, Bin](expand)
    val cata = Schemes.cata(sumAlg)
    val composed = ana.andThen(cata)
    seeds.map(composed.get) === seeds.map(s => cata.get(ana.get(s)))
  }

  "ana.andThen(cata) == hylo for a pure algebra (the hylo law)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13)
    val composed = Schemes.ana[BinF, Int, Bin](expand).andThen(Schemes.cata(sumAlg))
    seeds.map(composed.get) === seeds.map(Schemes.hylo[BinF, Int, Int](expand, sumAlgSeed).get)
  }

  "the fused hylo builds no intermediate Bin and is stack-safe at depth 10^6" >> {
    val Deep = 1_000_000
    def spine(n: Int): BinF[Int] =
      if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    def leafOrSpine(n: Int): BinF[Int] = if n < 0 then BinF.LeafF(0) else spine(n)
    val depthAlg: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    (Schemes.hylo[BinF, Int, Int](leafOrSpine, depthAlg).get(Deep) == Deep) must beTrue
  }
