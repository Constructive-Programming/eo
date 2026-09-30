package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import optics.Optic.* // get
import schemes.samples.{Bin, BinF}

/** Pins [[Schemes.para]]'s *route*, not just its result: the retained subterms must come off the
  * layer the machine already peeled, so a para fold peels each node **exactly once**.
  *
  * The alternative — recovering the subterms by re-`project`ing each node (or materializing
  * each node's children into a `List`) — peels every node twice and allocates an extra layer
  * per node on top of the `List`; that is the regression this spec exists to catch (on the
  * 8 191-node benchmark fixture it costs ~2x para's allocation, past droste's `zoo.para`).
  */
class ParaRouteSpec extends Specification:

  sequential

  // 7 nodes: 4 leaves + 3 branches.
  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  /** Counts layer peels; a fold never calls `embed`. */
  final private class CountingBasis extends Basis[BinF, Bin]:
    var projects = 0

    def project(s: Bin): BinF[Bin] =
      projects += 1
      s match
        case Bin.Leaf(n)      => BinF.LeafF(n)
        case Bin.Branch(l, r) => BinF.BranchF(l, r)

    def embed(fs: BinF[Bin]): Bin =
      fs match
        case BinF.LeafF(n)      => Bin.Leaf(n)
        case BinF.BranchF(l, r) => Bin.Branch(l, r)

  private val subtermSum: BinF[(Bin, Int)] => Int =
    case BinF.LeafF(n)                => n
    case BinF.BranchF((_, l), (_, r)) => l + r

  "para peels each node exactly once: subterms come off the machine already-peeled layer" >> {
    val basis = new CountingBasis
    val sum = Schemes.para[BinF, Bin, Int](subtermSum)(using BinF.traverse, basis).get(tree)

    (sum === 10).and(basis.projects === 7) // 7 nodes folded => 7 peels: no per-node re-project
  }

  "para reads the original subterms, not just the children results" >> {
    // Only a subterm-retaining fold can compute this: each branch adds its LEFT child leaf
    // weight when that child is itself a leaf — a plain cata sees results only.
    val leftLeafWeight: BinF[(Bin, Int)] => Int =
      case BinF.LeafF(n)                 => n
      case BinF.BranchF((ls, l), (_, r)) =>
        l + r + (ls match { case Bin.Leaf(w) => w; case _ => 0 })

    // Branch(Branch(1, 2), Branch(3, 4)): the two inner branches are the left-leaf cases =>
    // (1 + 2 + 1) + (3 + 4 + 3) = 14.
    Schemes.para[BinF, Bin, Int](leftLeafWeight).get(tree) === 14
  }
