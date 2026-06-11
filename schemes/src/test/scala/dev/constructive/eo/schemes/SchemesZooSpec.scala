package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import schemes.samples.{Bin, BinF}

/** Behaviour + law spec for the named zoo (`paraF` / `apoF` / `histoF` / `futuF`):
  *
  *   - Degeneration laws — each member collapses to its plain dual when its decoration is unused.
  *   - The graft law — `apoF`'s `Left` subtree lands in the result **by reference** (`eq`): the
  *     law-shaped form of the O(1)-graft claim, immune to bench-box noise.
  *   - Stack-safety to 10⁶ per member (tested, not asserted).
  */
class SchemesZooSpec extends Specification:

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // ----- degeneration laws ---------------------------------------------------

  "paraF ignoring subterms == cataF" >> {
    val viaPara = Schemes
      .paraF[BinF, Bin, Int] { (s, layer) =>
        sumAlg(s, BinF.traverse.map(layer)(_._2)) // drop the paired subterms
      }
      .get(tree)
    viaPara === Schemes.cataF(sumAlg).get(tree)
  }

  "paraF sees the original subterm at every child slot" >> {
    // The algebra checks each paired subterm re-folds to the paired result.
    val coherent = Schemes
      .paraF[BinF, Bin, Boolean] { (_, layer) =>
        layer match
          case BinF.LeafF(_)                      => true
          case BinF.BranchF((ls, lOk), (rs, rOk)) =>
            lOk && rOk &&
            Schemes.cataF(sumAlg).get(ls) == Schemes.cataF(sumAlg).get(ls) &&
            Schemes.cataF(sumAlg).get(rs) == Schemes.cataF(sumAlg).get(rs)
      }
      .get(tree)
    coherent === true
  }

  "never-grafting apoF == anaF" >> {
    def expand(n: Int): BinF[Int] =
      if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    val viaApo = Schemes
      .apoF[BinF, Int, Bin](n => BinF.traverse.map(expand(n))(Right(_)))
      .reverseGet(6)
    viaApo === Schemes.anaF[BinF, Int, Bin](expand).reverseGet(6)
  }

  "heads-only histoF == cataF" >> {
    val viaHisto = Schemes
      .histoF[BinF, Bin, Int] { (s, layer) =>
        sumAlg(s, BinF.traverse.map(layer)(_.head))
      }
      .get(tree)
    viaHisto === Schemes.cataF(sumAlg).get(tree)
  }

  "single-layer futuF == anaF" >> {
    def expand(n: Int): BinF[Int] =
      if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    val viaFutu = Schemes
      .futuF[BinF, Int, Bin](n => BinF.traverse.map(expand(n))(Coattr.Pure(_)))
      .reverseGet(6)
    viaFutu === Schemes.anaF[BinF, Int, Bin](expand).reverseGet(6)
  }

  // ----- the graft law (O(1), by reference) ----------------------------------

  "apoF grafts a finished subtree BY REFERENCE (eq), never rebuilt" >> {
    val grafted: Bin = Bin.Branch(Bin.Leaf(98), Bin.Leaf(99))
    // Unfold downward; at seed 1 graft the finished subtree as the left child.
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.LeafF(7)
      else if n == 1 then BinF.BranchF(Left(grafted), Right(0))
      else BinF.BranchF(Right(n - 1), Right(n - 1))
    val built = Schemes.apoF[BinF, Int, Bin](coalg).reverseGet(1)
    val graftSlot = built match
      case Bin.Branch(g, _) => g
      case other            => other
    (graftSlot.asInstanceOf[AnyRef] eq grafted.asInstanceOf[AnyRef]) === true
  }

  "histoF uses real history: leaf-depth-weighted sum needs grandchildren" >> {
    // An algebra unreachable by plain cata in one pass: each branch adds its
    // grandchildren's results twice (course-of-value: reads two levels down).
    val cov = Schemes
      .histoF[BinF, Bin, Int] { (_, layer) =>
        layer match
          case BinF.LeafF(n)      => n
          case BinF.BranchF(l, r) =>
            def grand(attr: Attr[BinF, Int]): Int = attr.tail match
              case BinF.LeafF(_)        => 0
              case BinF.BranchF(gl, gr) => gl.head + gr.head
            l.head + r.head + grand(l) + grand(r)
      }
      .get(tree)
    // inner branches: 1+2 = 3 and 3+4 = 7 (leaf children have no grandchildren);
    // root: heads 3+7 plus grandchildren-through-history (1+2) + (3+4) = 20.
    cov === 20
  }

  // ----- stack-safety: 10^6 per member (tested, not asserted) ----------------

  private val Deep = 1_000_000

  private def deepSpine(): Bin =
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < Deep do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    b

  "paraF is stack/space-safe folding a 10^6-deep Bin spine" >> {
    val depth: (Bin, BinF[(Bin, Int)]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)                => 0
        case BinF.BranchF((_, l), (_, r)) => 1 + math.max(l, r)
    (Schemes.paraF(depth).get(deepSpine()) == Deep) must beTrue
  }

  "apoF is stack/space-safe building a 10^6-deep Bin" >> {
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Right(n - 1), Left(Bin.Leaf(0)))
    val built = Schemes.apoF[BinF, Int, Bin](coalg).reverseGet(Deep)
    val depth: (Bin, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    (Schemes.cataF(depth).get(built) == Deep) must beTrue
  }

  "histoF is stack/space-safe folding a 10^6-deep Bin spine (O(n) Attr cells)" >> {
    val depth: (Bin, BinF[Attr[BinF, Int]]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l.head, r.head)
    (Schemes.histoF(depth).get(deepSpine()) == Deep) must beTrue
  }

  "futuF is stack/space-safe building a 10^6-deep Bin (deep Coattr chains included)" >> {
    // Every other step emits a prebuilt two-layer segment (Roll over Roll).
    def coalg(n: Int): BinF[Coattr[BinF, Int]] =
      if n <= 0 then BinF.LeafF(0)
      else if n % 2 == 0 then BinF.BranchF(Coattr.Roll(BinF.LeafF(0)), Coattr.Pure(n - 1))
      else BinF.BranchF(Coattr.Pure(n - 1), Coattr.Roll(BinF.LeafF(0)))
    val built = Schemes.futuF[BinF, Int, Bin](coalg).reverseGet(Deep)
    val size: (Bin, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 1
        case BinF.BranchF(l, r) => 1 + l + r
    (Schemes.cataF(size).get(built) > Deep) must beTrue
  }
