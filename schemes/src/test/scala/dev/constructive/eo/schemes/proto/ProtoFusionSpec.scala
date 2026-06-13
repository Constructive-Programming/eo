package dev.constructive.eo
package schemes
package proto

import org.specs2.mutable.Specification

import optics.Optic.*
import schemes.samples.{Bin, BinF}
import Scheme.given

/** Correctness pin for the fusion prototype: the fused `ana.cross(cata)` must agree with the
  * materialising spellings (the hylo law for a node-blind algebra), and stay stack-safe.
  */
class ProtoFusionSpec extends Specification:

  sequential

  private val coalg: Int => BinF[Int] = n =>
    if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  // node-BLIND fold (fusable)
  private val pureSum: BinF[Int] => Int = {
    case BinF.LeafF(v)      => v
    case BinF.BranchF(l, r) => l + r
  }

  // node-READING fold (para; equals pureSum here but typed to need the node)
  private val nodeSum: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(v)      => v
      case BinF.BranchF(l, r) => l + r

  private val cata = Proto.cata[BinF, Bin, Int](pureSum)
  private val para = Proto.para[BinF, Bin, Int](nodeSum)
  private val ana = Proto.ana[BinF, Int, Bin](coalg)

  "fused ana.cross(cata) == manual cata.get(ana.reverseGet) == para.cross materialised" >> {
    val seeds = List(1, 2, 3, 5, 8, 13, 21)
    val fused = ana.cross(cata) // X = Nothing → fused
    val viaPara = ana.cross(para) // X = S       → materialising
    val fusedR = seeds.map(fused.get)
    val manualR = seeds.map(s => cata.get(ana.reverseGet(s)))
    val paraR = seeds.map(viaPara.get)
    (fusedR === manualR).and(fusedR === paraR)
  }

  "the fused cross is stack-safe at depth 10^6 (no intermediate Bin to overflow)" >> {
    val Deep = 1_000_000
    def spine(n: Int): BinF[Int] =
      if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    def leafOrSpine(n: Int): BinF[Int] = if n < 0 then BinF.LeafF(0) else spine(n)
    val depthAlg: BinF[Int] => Int = {
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l, r)
    }
    val fused = Proto.ana[BinF, Int, Bin](leafOrSpine).cross(Proto.cata[BinF, Bin, Int](depthAlg))
    (fused.get(Deep) == Deep) must beTrue
  }
