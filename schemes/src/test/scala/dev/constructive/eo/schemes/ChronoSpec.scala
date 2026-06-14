package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF}
import schemes.zoo.{Attr, Coattr}

/** The chronomorphism, and its **fuse efficiency**: `chrono` is `hylo` at the universal indices —
  * `futu.cross(histo)` (build through the free monad [[Coattr]], fold through the cofree comonad
  * [[Attr]]) — and like `hylo` it fuses, building **no intermediate `S`**.
  *
  *   - chrono law: the fused `futu.cross(histo)` equals the materialising `histo.get ∘
  *     futu.reverseGet`, and equals [[Schemes.chrono]].
  *   - fuse efficiency: an instrumented [[Basis]] witnesses that the fused refold calls
  *     `project`/`embed` **zero** times (whereas the materialising spelling calls each once per
  *     node), and the fused refold is stack-safe at 10⁶ — no `S` to overflow.
  *   - degeneration: all-`Pure` coalg + heads-only algebra collapses chrono to [[Schemes.hylo]].
  */
class ChronoSpec extends Specification:

  sequential

  // Counts layer peels (`project`) and glues (`embed`). The fused chrono touches neither.
  final private class CountingBasis extends Basis[BinF, Bin]:
    var projects = 0
    var embeds = 0

    def project(s: Bin): BinF[Bin] =
      projects += 1
      s match
        case Bin.Leaf(n)      => BinF.LeafF(n)
        case Bin.Branch(l, r) => BinF.BranchF(l, r)

    def embed(fs: BinF[Bin]): Bin =
      embeds += 1
      fs match
        case BinF.LeafF(n)      => Bin.Leaf(n)
        case BinF.BranchF(l, r) => Bin.Branch(l, r)

  // futu coalgebra (all-Pure here): a balanced split down to unit leaves.
  private val coalg: Int => BinF[Coattr[BinF, Int]] = n =>
    if n <= 1 then BinF.LeafF(1)
    else BinF.BranchF(Coattr.Pure(n / 2), Coattr.Pure(n - n / 2))

  // histo algebra (heads-only here): leaf sum. Reads each child's Attr head.
  private val sumAlg: BinF[Attr[BinF, Int]] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l.head + r.head

  "futu.cross(histo) == histo.get ∘ futu.reverseGet == Schemes.chrono (the chrono law)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13, 21)
    val basis = new CountingBasis
    val futu = Schemes.futu[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val histo = Schemes.histo[BinF, Bin, Int](sumAlg)(using BinF.traverse, basis)

    val fused = futu.cross(histo) // Hylo[Int, Int]
    val direct = Schemes.chrono[BinF, Int, Int](coalg, sumAlg)

    val fusedR = seeds.map(fused.get)
    val materialisedR = seeds.map(s => histo.get(futu.reverseGet(s)))
    val directR = seeds.map(direct.get)

    (fusedR === materialisedR).and(fusedR === directR).and(fusedR === seeds) // sum of unit leaves
  }

  "fuse efficiency: the fused chrono builds NO intermediate Bin (project/embed never called)" >> {
    val basis = new CountingBasis
    val futu = Schemes.futu[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val histo = Schemes.histo[BinF, Bin, Int](sumAlg)(using BinF.traverse, basis)

    val _ = futu.cross(histo).get(21) // run the whole refold

    // Deforestation witness: the fused pass threads Coattr/Attr only — zero peels, zero glues.
    (basis.projects === 0).and(basis.embeds === 0)
  }

  "contrast: the materialising spelling DOES build the Bin (embed-per-node, then project-per-node)" >> {
    val basis = new CountingBasis
    val futu = Schemes.futu[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val histo = Schemes.histo[BinF, Bin, Int](sumAlg)(using BinF.traverse, basis)

    val built = futu.reverseGet(21) // builds the tree: one embed per node
    val nodes = basis.embeds
    val _ = histo.get(built) // folds the tree: one project per node

    (basis.projects === nodes).and(nodes > 0)
  }

  "chrono degenerates to hylo (all-Pure coalg + heads-only algebra)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13, 21)
    // plain hylo over the same shape: coalg' : Int => BinF[Int], alg' : BinF[Int] => Int
    val plainCoalg: Int => BinF[Int] =
      n => if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    val plainAlg: BinF[Int] => Int =
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r
    val viaChrono = Schemes.chrono[BinF, Int, Int](coalg, sumAlg)
    val viaHylo = Schemes.hylo[BinF, Int, Int](plainCoalg, plainAlg)
    seeds.map(viaChrono.get) === seeds.map(viaHylo.get)
  }

  "the fused chrono builds no intermediate Bin and is stack-safe at depth 10^6" >> {
    val Deep = 1_000_000
    // all-Pure deep spine; -1 terminates a branch with a leaf.
    val spineCoalg: Int => BinF[Coattr[BinF, Int]] = n =>
      if n <= 0 then BinF.LeafF(0)
      else BinF.BranchF(Coattr.Pure(n - 1), Coattr.Pure(-1))
    val depthAlg: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l.head, r.head)
    (Schemes.chrono[BinF, Int, Int](spineCoalg, depthAlg).get(Deep) == Deep) must beTrue
  }
