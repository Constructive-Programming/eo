package dev.constructive.eo
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.bench.fixture.SchemesFixtures.given
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.schemes.Schemes
import dev.constructive.eo.schemes.proto.{Proto, Scheme}
import Scheme.given

/** Prototype validation: does `ana.cross(cata)` on the `Scheme` carrier fuse to `hylo` cost?
  *
  *   - `protoFusedCross` — `ana.cross(cata)` with a node-BLIND `cata` (X = Nothing): should rebuild
  *     the single-pass hylo machine — NO intermediate `Bin` — so ≈ `eoHyloRef` (≈361k B/op).
  *   - `protoParaCross` — `ana.cross(para)` with a node-READING `para` (X = S): the overload picks
  *     the materialising branch (build the `Bin`, then fold) — ≈885k B/op.
  *   - `protoManual` — `cata.get(ana.reverseGet(...))`, the hand-written materialisation — ≈885k.
  *
  * The X-resolution (Nothing vs S) is what selects fused vs materialising — same `cross` spelling.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ProtoFusionBench extends JmhDefaults:
  import SchemesFixtures.*

  final val Depth = 12 // 2^12 = 4096 leaves, 8191 nodes — same workload as SchemesBench

  // node-BLIND fold (true catamorphism) — fusable
  private val pureSum: BinF[Int] => Int = {
    case BinF.LeafF(v)    => v
    case BinF.NodeF(l, r) => l + r
  }

  // Prebuilt optics (construction not measured).
  val protoCata = Proto.cata[BinF, Bin, Int](pureSum)
  val protoPara = Proto.para[BinF, Bin, Int](eoTypedSum) // node-READING (X = S)
  val protoAna = Proto.ana[BinF, Int, Bin](eoTypedCoalg)

  val protoFusedG = protoAna.cross(protoCata) // X_cata = Nothing → fused
  val protoParaG = protoAna.cross(protoPara) //  X_para = S       → materialising

  // Reference: the existing fused hylo (node-blind), the bar to hit.
  val eoHyloRefG = Schemes.hylo[BinF, Int, Int](eoTypedCoalg, (_, fa) => pureSum(fa))

  @Benchmark def protoFusedCross: Int = protoFusedG.get(Depth)
  @Benchmark def protoParaCross: Int = protoParaG.get(Depth)
  @Benchmark def protoManual: Int = protoCata.get(protoAna.reverseGet(Depth))
  @Benchmark def eoHyloRef: Int = eoHyloRefG.get(Depth)
