package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import optics.Getter
import schemes.samples.{Bin, BinF}

/** The fusion seam: `ana(c).cross(cata(a))`.
  *
  *   - Resolution pin: the ascription `: Getter[Seed, A]` compiles only if the FUSED overload on
  *     the concrete `Ana` wins (the generic trait `cross` returns a bare `Optic`, not a
  *     `Getter`) — the matrix-spec-style proof the overload set resolves as designed.
  *   - Fusion law: fused cross == the materializing composition (all algebras, extensional), and
  *     == `hylo` for algebras that read the node only through the seed↔`embed(coalg(seed))`
  *     correspondence (here: a pure algebra typed at both nodes and seeds).
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

  "ana(c).cross(cata(a)) resolves to the FUSED overload (ascription pin)" >> {
    val fused: Getter[Int, Int] = Schemes.ana[BinF, Int, Bin](expand).cross(Schemes.cata(sumAlg))
    fused.get(6) === 6 // six leaves of weight 1
  }

  "fused cross == the materializing composition (node-supplied algebra)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13)
    val ana = Schemes.ana[BinF, Int, Bin](expand)
    val cata = Schemes.cata(sumAlg)
    val fused = ana.cross(cata)
    seeds.map(fused.get) === seeds.map(s => cata.get(ana.reverseGet(s)))
  }

  "fused cross == hylo for a pure algebra (the hylo law under the correspondence)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13)
    val fused = Schemes.ana[BinF, Int, Bin](expand).cross(Schemes.cata(sumAlg))
    seeds.map(fused.get) === seeds.map(Schemes.hylo[BinF, Int, Int](expand, sumAlgSeed).get)
  }

  // Depth bar: stack-safety needs >> the ~10k-frame JVM stack; 200k proves the machine
  // (the 10^6 SPACE bar is carried by the ana/apo sweeps — this suite's fused machine
  // additionally retains the (S, A) pairs, and the suites share one test JVM).
  "fused cross is stack-safe on a 200k-deep spine (single pass)" >> {
    val Deep = 200_000
    def spineCoalg(n: Int): BinF[Int] =
      if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    def leafOrSpine(n: Int): BinF[Int] =
      if n < 0 then BinF.LeafF(0) else spineCoalg(n)
    val depthAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    val fused = Schemes.ana[BinF, Int, Bin](leafOrSpine).cross(Schemes.cata(depthAlg))
    (fused.get(Deep) == Deep) must beTrue
  }
