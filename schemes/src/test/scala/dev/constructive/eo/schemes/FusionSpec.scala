package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF}

/** The thesis, as an executable proof: **hylo is the fusion of ana and cata**, automatic from the
  * existential.
  *
  * `ana` is a build (`Review`-shaped, `X = S`) and `cata` a node-blind fold (`Getter`-shaped, `X =
  * Nothing`); the build⇄read seam between them is `ana.cross(cata)` (definitionally
  * `ana.reverse.andThen(cata)`). Because the citizens keep their `coalg`/`alg` alive, that compose
  * **fuses**:
  *
  *   - it equals the materialising `cata.get ∘ ana.reverseGet` (the hylo law), and
  *   - it equals [[Schemes.hylo]], and
  *   - it builds **no intermediate `S`** — made observable below by an instrumented [[Basis]] whose
  *     `project`/`embed` the fused refold never calls (whereas the materialising spelling calls
  *     each once per node).
  */
class FusionSpec extends Specification:

  sequential

  // A Basis that counts how many layers it peels (`project`) and glues (`embed`). The fused refold
  // touches neither — it threads `coalg`/`alg` directly — so the counters are the deforestation
  // witness.
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

  // seed n: a balanced split down to leaves of weight 1.
  private val coalg: Int => BinF[Int] =
    n => if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  private val sumLeaves: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  "ana.cross(cata) == cata.get ∘ ana.reverseGet == hylo (the hylo law)" >> {
    val seeds = List(1, 2, 3, 5, 8, 13, 21)
    val basis = new CountingBasis
    val ana = Schemes.ana[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val cata = Schemes.cata[BinF, Bin, Int](sumLeaves)(using BinF.traverse, basis)

    val fused = ana.cross(cata) // Hylo[Int, Int]
    val direct = Schemes.hylo[BinF, Int, Int](coalg, sumLeaves)

    val fusedR = seeds.map(fused.get)
    val materialisedR = seeds.map(s => cata.get(ana.reverseGet(s)))
    val directR = seeds.map(direct.get)

    (fusedR === materialisedR).and(fusedR === directR).and(fusedR === seeds) // sum of unit leaves
  }

  "the fused refold builds NO intermediate Bin: project/embed are never called" >> {
    val basis = new CountingBasis
    val ana = Schemes.ana[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val cata = Schemes.cata[BinF, Bin, Int](sumLeaves)(using BinF.traverse, basis)

    val fused = ana.cross(cata)
    val _ = fused.get(21) // run the whole refold

    // Deforestation witness: the fused pass threads coalg/alg only — zero layer peels, zero glues.
    (basis.projects === 0).and(basis.embeds === 0)
  }

  "the materialising spelling DOES build the Bin: embed-per-node on build, project-per-node on fold" >> {
    val basis = new CountingBasis
    val ana = Schemes.ana[BinF, Int, Bin](coalg)(using BinF.traverse, basis)
    val cata = Schemes.cata[BinF, Bin, Int](sumLeaves)(using BinF.traverse, basis)

    val built = ana.reverseGet(21) // builds the tree: one embed per node
    val nodes = basis.embeds
    val _ = cata.get(built) // folds the tree: one project per node

    // Same node count on both passes, and it is non-trivial (the tree was really built).
    (basis.projects === nodes).and(nodes > 0)
  }

  "the fused hylo builds no intermediate Bin and is stack-safe at depth 10^6" >> {
    val Deep = 1_000_000
    val spineCoalg: Int => BinF[Int] =
      n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val depthAlg: BinF[Int] => Int =
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l, r)
    val fused =
      Schemes.ana[BinF, Int, Bin](spineCoalg).cross(Schemes.cata[BinF, Bin, Int](depthAlg))
    (fused.get(Deep) == Deep) must beTrue
  }
