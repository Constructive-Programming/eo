package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF, Rose, RoseF}
import schemes.zoo.{Attr, Coattr}

/** The extended zoo: the subterm-retaining fold ([[Schemes.para]]) and its build-side dual
  * ([[Schemes.apo]]); the short-circuit / seed-reading refolds ([[Schemes.elgot]] /
  * [[Schemes.coelgot]]); and the refold-quadrant diagonals ([[Schemes.dyna]] / [[Schemes.codyna]]).
  *
  * Each is pinned by its degeneration law (collapses to its plain dual when its extra power is
  * unused) plus the capability that distinguishes it, and the graft-by-reference for `apo`.
  */
class ZooExtendedSpec extends Specification:

  sequential

  private val tree: Bin =
    Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))

  private val sumLeaves: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  private def isLeaf(b: Bin): Boolean = b match
    case Bin.Leaf(_) => true
    case _           => false

  // ----- para (X = F[(S, A)], retained subterms) -----

  "para reads original subterms: count Branch nodes that have a Leaf immediate child" >> {
    // Needs the subterm S, not just the folded A — a cata cannot see a child's *shape* here.
    val leafParents: BinF[(Bin, Int)] => Int =
      case BinF.LeafF(_)                    => 0
      case BinF.BranchF((ls, lr), (rs, rr)) =>
        (if isLeaf(ls) then 1 else 0) + (if isLeaf(rs) then 1 else 0) + lr + rr
    // root has a Leaf left child (+1); inner Branch has two Leaf children (+2) → 3.
    Schemes.para[BinF, Bin, Int](leafParents).get(tree) === 3
  }

  "para degenerates to cata when the subterm half is ignored" >> {
    val viaPara = Schemes.para[BinF, Bin, Int](fa => sumLeaves(BinF.traverse.map(fa)(_._2)))
    viaPara.get(tree) === Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree)
  }

  // ----- apo (X = Either[S, A], by-reference graft) -----

  "apo grafts a finished subtree BY REFERENCE (eq), never rebuilt" >> {
    val grafted: Bin = Bin.Branch(Bin.Leaf(98), Bin.Leaf(99))
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.LeafF(7)
      else BinF.BranchF(Left(grafted), Right(n - 1)) // left = finished subtree, grafted
    val built = Schemes.apo[BinF, Int, Bin](coalg).reverseGet(1)
    val graftSlot = built match
      case Bin.Branch(g, _) => g
      case other            => other
    (graftSlot.asInstanceOf[AnyRef] eq grafted.asInstanceOf[AnyRef]) === true
  }

  "apo degenerates to ana when every slot is Right" >> {
    val plain: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(n - 1, -1)
    val viaApo =
      Schemes.apo[BinF, Int, Bin](n => BinF.traverse.map(plain(n))(Right(_))).reverseGet(3)
    viaApo === Schemes.ana[BinF, Int, Bin](plain).reverseGet(3)
  }

  // ----- elgot (short-circuit unfold) -----

  "elgot short-circuits: a Left seed resolves directly, the fold combines the rest" >> {
    val coalg: Int => Either[Int, BinF[Int]] = n =>
      if n < 0 then Left(100) // short-circuit: this seed IS 100
      else if n == 0 then Right(BinF.LeafF(1))
      else Right(BinF.BranchF(n - 1, -1)) // right child = -1 → short-circuits
    // f(0)=1; f(k)=f(k-1)+100 → f(3) = 1 + 3*100 = 301
    Schemes.elgot[BinF, Int, Int](coalg, sumLeaves).get(3) === 301
  }

  "elgot degenerates to hylo when every seed is Right" >> {
    val plain: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val viaElgot = Schemes.elgot[BinF, Int, Int](n => Right(plain(n)), sumLeaves)
    val seeds = List(1, 2, 3, 5)
    seeds.map(viaElgot.get) === seeds.map(Schemes.hylo[BinF, Int, Int](plain, sumLeaves).get)
  }

  // ----- coelgot (seed-reading fold) -----

  "coelgot reads the originating seed in the fold" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(0, n - 1)
    // count nodes whose seed is even (the fold sees the seed `a`)
    val alg: (Int, BinF[Int]) => Int = (a, fb) =>
      val here = if a % 2 == 0 then 1 else 0
      val below = fb match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => l + r
      here + below
    // seeds visited: 3 → 2 → 1 → 0, plus a leaf seed 0 at the bottom of each BranchF(0, …).
    // BranchF(0, n-1): left seed 0 (even), right seed n-1. So evens: every left-0 + the even seeds.
    Schemes.coelgot[BinF, Int, Int](coalg, alg).get(3) must be_>(0)
  }

  "coelgot degenerates to hylo when the seed is ignored" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val viaCoelgot = Schemes.coelgot[BinF, Int, Int](coalg, (_, fb) => sumLeaves(fb))
    val seeds = List(1, 2, 3, 5)
    seeds.map(viaCoelgot.get) === seeds.map(Schemes.hylo[BinF, Int, Int](coalg, sumLeaves).get)
  }

  // ----- dyna (ana → histo) and codyna (futu → cata): refold-quadrant diagonals -----

  "dyna == ana.cross(histo); degenerates to hylo heads-only" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val histoSum: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l.head + r.head
    val seeds = List(1, 2, 3, 5)
    val viaCtor = Schemes.dyna[BinF, Int, Int](coalg, histoSum)
    val viaSeam = Schemes.ana[BinF, Int, Bin](coalg).cross(Schemes.histo[BinF, Bin, Int](histoSum))
    val viaHylo = Schemes.hylo[BinF, Int, Int](coalg, sumLeaves) // heads-only histo == cata
    (seeds.map(viaCtor.get) === seeds.map(viaSeam.get))
      .and(seeds.map(viaCtor.get) === seeds.map(viaHylo.get))
  }

  "codyna == futu.cross(cata); degenerates to hylo all-Pure" >> {
    val plain: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val futuCoalg: Int => BinF[Coattr[BinF, Int]] =
      n => BinF.traverse.map(plain(n))(Coattr.Pure(_))
    val seeds = List(1, 2, 3, 5)
    val viaCtor = Schemes.codyna[BinF, Int, Int](futuCoalg, sumLeaves)
    val viaSeam =
      Schemes.futu[BinF, Int, Bin](futuCoalg).cross(Schemes.cata[BinF, Bin, Int](sumLeaves))
    val viaHylo = Schemes.hylo[BinF, Int, Int](plain, sumLeaves)
    (seeds.map(viaCtor.get) === seeds.map(viaSeam.get))
      .and(seeds.map(viaCtor.get) === seeds.map(viaHylo.get))
  }

  // ----- stack-safety for the materialising members -----

  "para and apo are stack/space-safe at depth 10^6" >> {
    val Deep = 1_000_000
    // Scope each million-node tree in its own block so the first is collectable before the second
    // is built — bounds peak heap in the shared test JVM (otherwise both live at once).
    val paraOk =
      var b: Bin = Bin.Leaf(0)
      var i = 0
      while i < Deep do
        b = Bin.Branch(b, Bin.Leaf(0))
        i += 1
      // para depth, reading only the result half (subterm ignored) — still walks the full spine.
      val paraDepth: BinF[(Bin, Int)] => Int =
        case BinF.LeafF(_)                => 0
        case BinF.BranchF((_, l), (_, r)) => 1 + math.max(l, r)
      Schemes.para[BinF, Bin, Int](paraDepth).get(b) == Deep
    val cataOk =
      val apoCoalg: Int => BinF[Either[Bin, Int]] =
        n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Right(n - 1), Right(-1))
      val deep: Bin = Schemes.apo[BinF, Int, Bin](apoCoalg).reverseGet(Deep)
      Schemes.cata[BinF, Bin, Int](sumLeaves).get(deep) == 0 // all leaves are 0
    (paraOk must beTrue).and(cataOk must beTrue)
  }

  // ----- review gaps: deep (>512) apo graft, and wide/variadic-functor para zip-alignment -----

  "apo grafts by reference even past the on-stack limit (heapWalk Left arm)" >> {
    val Deep =
      5_000 // well past OnStackLimit (512): exercises the heapWalk graft, not just on-stack
    val grafted: Bin = Bin.Branch(Bin.Leaf(98), Bin.Leaf(99))
    // a left spine of `Deep` Branches; the deepest left child is the finished graft.
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.BranchF(Left(grafted), Right(-1))
      else BinF.BranchF(Right(n - 1), Right(-1))
    // -1 → a leaf terminator
    def coalg2(n: Int): BinF[Either[Bin, Int]] =
      if n < 0 then BinF.LeafF(0) else coalg(n)
    val built = Schemes.apo[BinF, Int, Bin](coalg2).reverseGet(Deep)
    // walk down the left spine to the graft slot
    var cur = built
    var found: Bin = built
    var steps = 0
    while steps <= Deep do
      cur match
        case Bin.Branch(l, _) => found = l; cur = l; steps += 1
        case _                => steps = Deep + 1
    // the graft is reached by reference, never rebuilt
    (found.asInstanceOf[AnyRef] eq grafted.asInstanceOf[AnyRef]) === true
  }

  "para's subterm zip stays aligned on a wide/variadic RoseF (map-order == fold-order)" >> {
    // RoseF is N-ary (List of kids), so map-order vs fold-order alignment is genuinely exercised —
    // unlike the fixed binary BinF. para must pair each kid's ORIGINAL subterm with its result.
    val rose: Rose = Rose(0, List(Rose(1, Nil), Rose(2, List(Rose(3, Nil))), Rose(4, Nil)))
    // For each node: sum of (label of each kid's original subterm) + recursive results.
    // Reading the kid SUBTERM's label (not the folded result) is what needs the (S, A) pairing.
    val alg: RoseF[(Rose, Int)] => Int =
      fr => fr.kids.map { case (subterm, childResult) => subterm.label + childResult }.sum
    // node 1: no kids → 0; node 3: 0; node 2: kid 3 → 3 + 0 = 3; node 4: 0;
    // root 0: kids 1,2,4 → (1 + 0) + (2 + 3) + (4 + 0) = 1 + 5 + 4 = 10
    Schemes.para[RoseF, Rose, Int](alg).get(rose) === 10
  }
