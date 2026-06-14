package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF}
import schemes.zoo.{Attr, Coattr}

/** Behaviour + degeneration spec for the universal-index schemes: [[Schemes.histo]] (`X = Attr`, the
  * cofree comonad) and [[Schemes.futu]] (`X = Coattr`, the free monad).
  *
  *   - Degeneration: each collapses to its trivial-index dual when its decoration is unused —
  *     heads-only `histo == cata`, all-`Pure` `futu == ana`.
  *   - Real index: a course-of-value fold that reads grandchildren (unreachable by single-pass
  *     `cata`); a multi-layer unfold that `Roll`s a whole layer in one step (unreachable by `ana`).
  *   - Stack-safety to 10⁶ per scheme.
  */
class ZooSpec extends Specification:

  sequential

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  private val sumLeaves: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  private val depthAlg: BinF[Int] => Int =
    case BinF.LeafF(_)      => 0
    case BinF.BranchF(l, r) => 1 + math.max(l, r)

  // ----- histo (X = Attr[F, A], cofree) -----

  "heads-only histo degenerates to cata" >> {
    val viaHisto = Schemes
      .histo[BinF, Bin, Int](layer => sumLeaves(BinF.traverse.map(layer)(_.head)))
      .get(tree)
    viaHisto === Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree)
  }

  "histo reads real history: a leaf-sum that also reaches its grandchildren" >> {
    // Unreachable by a single-pass cata: each branch adds its grandchildren's heads again,
    // through the retained Attr history (course-of-value).
    val cov = Schemes
      .histo[BinF, Bin, Int] {
        case BinF.LeafF(n)      => n
        case BinF.BranchF(l, r) =>
          def grand(attr: Attr[BinF, Int]): Int = attr.tail match
            case BinF.LeafF(_)        => 0
            case BinF.BranchF(gl, gr) => gl.head + gr.head
          l.head + r.head + grand(l) + grand(r)
      }
      .get(tree)
    // inner branches: 1+2=3 and 3+4=7 (their leaf children have no grandchildren);
    // root: heads 3+7 plus grandchildren-through-history (1+2)+(3+4) = 20.
    cov === 20
  }

  "histo is stack/space-safe folding a 10^6-deep Bin spine" >> {
    val Deep = 1_000_000
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < Deep do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    val histoDepth: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l.head, r.head)
    (Schemes.histo[BinF, Bin, Int](histoDepth).get(b) == Deep) must beTrue
  }

  // ----- futu (X = Coattr[F, A], free) -----

  "single-layer (all-Pure) futu degenerates to ana" >> {
    val expand: Int => BinF[Int] = n => if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    val viaFutu = Schemes
      .futu[BinF, Int, Bin](n => BinF.traverse.map(expand(n))(Coattr.Pure(_)))
      .reverseGet(6)
    viaFutu === Schemes.ana[BinF, Int, Bin](expand).reverseGet(6)
  }

  "futu emits multiple layers per step: Roll unrolls a whole Branch with no coalgebra call" >> {
    val coalg: Int => BinF[Coattr[BinF, Int]] = n =>
      if n <= 0 then BinF.LeafF(n)
      else
        BinF.BranchF(
          Coattr.Roll(BinF.BranchF(Coattr.Pure(0), Coattr.Pure(0))), // two layers in one step
          Coattr.Pure(n - 1),
        )
    val built = Schemes.futu[BinF, Int, Bin](coalg).reverseGet(1)
    built === Bin.Branch(Bin.Branch(Bin.Leaf(0), Bin.Leaf(0)), Bin.Leaf(0))
  }

  "futu is stack/space-safe building a 10^6-deep Bin (folded back for the check)" >> {
    val Deep = 1_000_000
    val coalg: Int => BinF[Coattr[BinF, Int]] = n =>
      if n <= 0 then BinF.LeafF(0)
      else BinF.BranchF(Coattr.Pure(n - 1), Coattr.Pure(-1))
    val built: Bin = Schemes.futu[BinF, Int, Bin](coalg).reverseGet(Deep)
    (Schemes.cata[BinF, Bin, Int](depthAlg).get(built) == Deep) must beTrue
  }
