package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import cats.instances.int.given
import org.specs2.mutable.Specification

import data.{Forget, PSVec}
import data.Forget.given
import optics.{Getter, Lens, Optic, Plated}
import optics.Optic.* // get, andThen, cross, foldMap

import generics.plate
import schemes.samples.{Bin, BinF, Rose, RoseF}

/** Behaviour spec for the typed pattern-functor schemes (`cataF` / `anaF` / `hyloF`) and `fLayer`.
  * Companion law/coherence checks live in `SchemesFLawsSpec`.
  *
  * Type-safety note (R3): every `gather`/`alg`/`coalg` below pattern-matches `BinF`'s *named*
  * constructors (`case BinF.BranchF(l, r) => l + r`), with `l`/`r` typed `A` — there is no
  * `kids(0)`/`AnyRef` positional path, so a child-arity mismatch is a compile error, not a runtime
  * `IndexOutOfBounds`.
  */
class SchemesFSpec extends Specification:

  // A small mixed tree: Branch(Leaf 1, Branch(Leaf 2, Leaf 3)) — leaf sum 6, 3 leaves, depth 2.
  private val tree: Bin =
    Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))

  // pure leaf-sum gather (ignores the node S)
  private val sumLeaves: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // ----- cataF (typed fold) -----

  "cataF folds a Bin to a value through F's named constructors" >> {
    val sumG: Getter[Bin, Int] = Schemes.cataF(sumLeaves)
    (sumG.get(tree) == 6) must beTrue
  }

  "cataF is paramorphism-flavored: gather can read the original node S" >> {
    // count Branch nodes by reading the node argument, not the folded children
    val branchCount: (Bin, BinF[Int]) => Int = (node, fa) =>
      val here = node match
        case Bin.Branch(_, _) => 1
        case Bin.Leaf(_)      => 0
      val below = fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => l + r
      here + below
    (Schemes.cataF(branchCount).get(tree) == 2) must beTrue
  }

  "cataF-as-Getter composes onto an outer Getter via andThen" >> {
    val composed: Getter[(String, Bin), Int] =
      Getter[(String, Bin), Bin](_._2).andThen(Schemes.cataF(sumLeaves))
    (composed.get(("x", tree)) == 6) must beTrue
  }

  "a composed Lens focuses a recursive field; the scheme folds it; the lens still writes" >> {
    final case class Inner(label: String, tree: Bin)
    final case class Doc(id: Int, inner: Inner)

    val innerL = Lens[Doc, Inner](_.inner, (d, i) => d.copy(inner = i))
    val treeL = Lens[Inner, Bin](_.tree, (i, t) => i.copy(tree = t))
    val deepTree = innerL.andThen(treeL) // Lens[Doc, Bin] — lens composition

    val doc = Doc(1, Inner("x", tree)) // tree = leaf sum 6
    // read: focus the recursive field with the composed lens, fold it with the scheme.
    // (Schemes are read-only Getters, so we either read at the leaf — `cataF.get(lens.get(doc))` —
    // or wrap the lens read in a Getter to build a reusable composed Getter[Doc, Int].)
    val docLeafSum: Getter[Doc, Int] =
      Getter[Doc, Bin](deepTree.get).andThen(Schemes.cataF(sumLeaves))

    val pruned = deepTree.replace(Bin.Leaf(0))(doc) // write through the same composed lens

    (Schemes.cataF(sumLeaves).get(deepTree.get(doc)) == 6)
      .and(docLeafSum.get(doc) == 6)
      .and(deepTree.get(pruned) == Bin.Leaf(0))
      .and(pruned.inner.label == "x") // the rest of the record is untouched
  }

  // ----- anaF (typed build) -----

  "anaF builds a Bin from a seed, then cataF reads it back" >> {
    // seed n: a left spine of n Branches ending in Leaf(1); right child always Leaf(0).
    val spine: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(n - 1, -1)
    val built: Bin = Schemes.anaF[BinF, Int, Bin](spine).reverseGet(3)
    // seed 3 -> Branch(Branch(Branch(Leaf 1, Leaf 1), Leaf 1), Leaf 1): 4 leaves of 1, depth 3
    (Schemes.cataF(sumLeaves).get(built) == 4).and(
      Schemes
        .cataF[BinF, Bin, Int]((_, fa) =>
          fa match
            case BinF.LeafF(_)      => 0
            case BinF.BranchF(l, _) => 1 + l
        )
        .get(built) == 3
    )
  }

  "anaF.cross(cataF) is the materializing hylo (builds the Bin, then folds)" >> {
    val spine: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(n - 1, -1)
    val refold = Schemes.anaF[BinF, Int, Bin](spine).cross(Schemes.cataF(sumLeaves))
    (refold.get(3) == 4) must beTrue
  }

  // ----- hyloF (fused refold, no intermediate Bin) -----

  "hyloF fuses unfold+fold with no intermediate Bin built" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val leafCount: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 1
        case BinF.BranchF(l, r) => l + r
    // seed 3 -> a right spine of 4 leaves
    (Schemes.hyloF(coalg, leafCount).get(3) == 4) must beTrue
  }

  "hyloF composes further into the pipeline" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val leafCount: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 1
        case BinF.BranchF(l, r) => l + r
    val toStr: Getter[Int, String] =
      Schemes.hyloF(coalg, leafCount).andThen(Getter[Int, String](_.toString))
    (toStr.get(3) == "4") must beTrue
  }

  // ----- edge cases -----

  "cataF / hyloF handle a single leaf (no recursive positions)" >> {
    (Schemes.cataF(sumLeaves).get(Bin.Leaf(7)) == 7).and(
      Schemes
        .hyloF[BinF, Int, Int](
          n => BinF.LeafF(n),
          (_, fa) =>
            fa match
              case BinF.LeafF(n)      => n
              case BinF.BranchF(l, r) => l + r,
        )
        .get(42) == 42
    )
  }

  "cataF handles a one-level Branch(Leaf, Leaf)" >> {
    (Schemes.cataF(sumLeaves).get(Bin.Branch(Bin.Leaf(4), Bin.Leaf(5))) == 9) must beTrue
  }

  // ----- cross-path equivalence to the #23 PSVec cata -----

  "cataF agrees with the #23 Plated-driven cata on the same computation" >> {
    given Plated[Bin] = plate[Bin]
    val psvecSum: (Bin, PSVec[Int]) => Int = (node, kids) =>
      node match
        case Bin.Leaf(n)      => n
        case Bin.Branch(_, _) => kids.toList.sum
    (Schemes.cata(psvecSum).get(tree) == Schemes.cataF(sumLeaves).get(tree))
      .and(Schemes.cataF(sumLeaves).get(tree) == 6)
  }

  // ----- fLayer: the single-layer Forget[F] optic (R1) -----

  "fLayer is a usable Optic[S,S,S,S,Forget[F]]: to/from round-trip one layer" >> {
    val layer: Optic[Bin, Bin, Bin, Bin, Forget[BinF]] = Schemes.fLayer[BinF, Bin]
    val b = Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))
    (layer.from(layer.to(b)) == b) must beTrue
  }

  "fLayer reads its layer's immediate foci via foldMap (Foldable[BinF])" >> {
    val layer = Schemes.fLayer[BinF, Bin]
    // two immediate children of a Branch
    (layer.foldMap[Int](_ => 1)(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))) == 2)
      .and(layer.foldMap[Int](_ => 1)(Bin.Leaf(9)) == 0) // a leaf has no recursive foci
  }

  // ----- stack-safety (R2): 10^6 deep -----
  //
  // cataF and hyloF descend a 10^6-deep spine; anaF additionally materializes an O(n) Bin. The
  // foldLayered machine (the same < 512-on-stack / heap-ArrayDeque hybrid as the PSVec schemes)
  // moves the deep recursion off the JVM call stack onto the heap, so these complete without
  // StackOverflowError where a naive recursion would overflow — in O(depth) space, no Eval chain,
  // so they run in the default test heap (no fork needed, like #23's PSVec 10^6 cases).

  private val Deep = 1_000_000

  "cataF is stack/space-safe folding a 10^6-deep Bin spine" >> {
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < Deep do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    val depth: (Bin, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    (Schemes.cataF(depth).get(b) == Deep) must beTrue
  }

  "hyloF is stack/space-safe at depth 10^6 (no intermediate Bin)" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val depth: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    (Schemes.hyloF(coalg, depth).get(Deep) == Deep) must beTrue
  }

  "anaF is stack/space-safe building a 10^6-deep Bin (the OOM frontier)" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val deep: Bin = Schemes.anaF[BinF, Int, Bin](coalg).reverseGet(Deep)
    val depth: (Bin, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 0
        case BinF.BranchF(l, r) => 1 + math.max(l, r)
    (Schemes.cataF(depth).get(deep) == Deep) must beTrue
  }

  // ----- wide-and-deep: exercise the Traverse[F] sequencing path a binary spine misses -----

  "cataF/hyloF stay safe on a wide-AND-deep RoseF (high fanout + deep)" >> {
    val DeepRose = 100_000
    val Width = 8
    // each level: one deep child (d-1) + Width leaf children (-1 -> empty RoseF)
    val coalg: Int => RoseF[Int] = d =>
      if d <= 0 then RoseF(0, Nil)
      else RoseF(d, (d - 1) :: List.fill(Width)(-1))
    val countNodes: (Int, RoseF[Int]) => Int = (_, fr) => 1 + fr.kids.sum
    // nodes = (DeepRose+1) spine nodes + DeepRose*Width leaves
    val expected = (DeepRose + 1) + DeepRose * Width
    (Schemes.hyloF(coalg, countNodes).get(DeepRose) == expected) must beTrue
  }

  // anaF + cataF over the N-ary RoseF (the hyloF case above never builds/folds a real Rose, so
  // this is the only test exercising the Traverse[RoseF]+Eval sequencing for those two schemes).
  "anaF builds and cataF folds a wide-AND-deep Rose (N-ary Project/Embed)" >> {
    val DeepRose = 20_000
    val Width = 4
    val coalg: Int => RoseF[Int] = d =>
      if d <= 0 then RoseF(0, Nil)
      else RoseF(d, (d - 1) :: List.fill(Width)(-1))
    val countNodes: (Rose, RoseF[Int]) => Int = (_, fr) => 1 + fr.kids.sum
    val built: Rose = Schemes.anaF[RoseF, Int, Rose](coalg).reverseGet(DeepRose)
    val expected = (DeepRose + 1) + DeepRose * Width
    (Schemes.cataF(countNodes).get(built) == expected) must beTrue
  }
