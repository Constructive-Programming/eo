package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import data.Forget
import optics.{Getter, Optic}
import optics.Optic.* // get, readOnly, reverseGet, foldMap, andThen

import schemes.samples.{Bin, BinF, Rose, RoseF}

/** Behaviour spec for the node-blind recursion-scheme spine (`cata` / `ana` / `hylo`) and `fLayer`.
  *
  * Type-safety note: every `alg`/`coalg` pattern-matches `F`'s *named* constructors (`case
  * BinF.BranchF(l, r) => l + r`), with `l`/`r` typed `A` — there is no `kids(0)`/`AnyRef`
  * positional path, so a child-arity mismatch is a compile error, not a runtime `IndexOutOfBounds`.
  *
  * `cata` is **node-blind** here (`alg: F[A] => A`): the algebra never sees the source node, only
  * its folded children. A node-reading fold is a paramorphism — a follow-up scheme, not one of the
  * three.
  */
class SchemesSpec extends Specification:

  // Deep examples: one-at-a-time to bound peak heap (shared test JVM).
  sequential

  // A small mixed tree: Branch(Leaf 1, Branch(Leaf 2, Leaf 3)) — leaf sum 6, 3 leaves, depth 2.
  private val tree: Bin =
    Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))

  // node-blind leaf-sum algebra (sees only the folded children, never the Bin)
  private val sumLeaves: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  private val depthAlg: BinF[Int] => Int =
    case BinF.LeafF(_)      => 0
    case BinF.BranchF(l, r) => 1 + math.max(l, r)

  // ----- cata (typed node-blind fold) -----

  "cata folds a Bin to a value through F's named constructors" >> {
    (Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree) == 6) must beTrue
  }

  "cata handles a single leaf (no recursive positions)" >> {
    (Schemes.cata[BinF, Bin, Int](sumLeaves).get(Bin.Leaf(7)) == 7) must beTrue
  }

  "cata handles a one-level Branch(Leaf, Leaf)" >> {
    (Schemes.cata[BinF, Bin, Int](sumLeaves).get(Bin.Branch(Bin.Leaf(4), Bin.Leaf(5))) == 9) must
      beTrue
  }

  "cata-as-read composes onto an outer Getter via andThen (.readOnly bridges the carrier)" >> {
    val composed: Getter[(String, Bin), Int] =
      Getter[(String, Bin), Bin](_._2).andThen(Schemes.cata[BinF, Bin, Int](sumLeaves).readOnly)
    (composed.get(("x", tree)) == 6) must beTrue
  }

  // ----- ana (typed build) -----

  "ana builds a Bin from a seed, then cata reads it back" >> {
    // seed n: a left spine of n Branches ending in Leaf(1); right child always Leaf(0).
    val spine: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(n - 1, -1)
    val built: Bin = Schemes.ana[BinF, Int, Bin](spine).reverseGet(3)
    // seed 3 -> 4 leaves of weight 1 → sum 4, depth 3
    (Schemes.cata[BinF, Bin, Int](sumLeaves).get(built) == 4)
      .and(Schemes.cata[BinF, Bin, Int](depthAlg).get(built) == 3)
  }

  // ----- hylo (fused refold, no intermediate Bin) -----

  "hylo fuses unfold+fold with no intermediate Bin built" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    // seed 3 -> a right spine of 4 leaves
    (Schemes.hylo[BinF, Int, Int](coalg, sumLeaves).get(3) == 4) must beTrue
  }

  "hylo composes further into the pipeline" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)
    val toStr: Getter[Int, String] =
      Schemes
        .hylo[BinF, Int, Int](coalg, sumLeaves)
        .readOnly
        .andThen(Getter[Int, String](_.toString))
    (toStr.get(3) == "4") must beTrue
  }

  // ----- fLayer: the single-layer Forget[F] optic -----

  "fLayer is a usable Optic[S,S,S,S,Forget[F]]: to/from round-trip one layer" >> {
    val layer: Optic[Bin, Bin, Bin, Bin, Forget[BinF]] = Schemes.fLayer[BinF, Bin]
    val b = Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))
    (layer.from(layer.to(b)) == b) must beTrue
  }

  "fLayer reads its layer's immediate foci via foldMap (Foldable[BinF])" >> {
    val layer = Schemes.fLayer[BinF, Bin]
    (layer.foldMap[Int](_ => 1)(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))) == 2)
      .and(layer.foldMap[Int](_ => 1)(Bin.Leaf(9)) == 0) // a leaf has no recursive foci
  }

  // ----- stack-safety: 10^6 deep -----
  //
  // The foldLayered machine (the < 512-on-stack / heap-ArrayDeque hybrid) moves the deep recursion
  // off the JVM call stack, so these complete without StackOverflowError where naive recursion
  // overflows — in O(depth) space (no Eval chain), so they run in the default test heap.

  private val Deep = 1_000_000

  "cata is stack/space-safe folding a 10^6-deep Bin spine" >> {
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < Deep do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    (Schemes.cata[BinF, Bin, Int](depthAlg).get(b) == Deep) must beTrue
  }

  "hylo is stack/space-safe at depth 10^6 (no intermediate Bin)" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    (Schemes.hylo[BinF, Int, Int](coalg, depthAlg).get(Deep) == Deep) must beTrue
  }

  "ana is stack/space-safe building a 10^6-deep Bin (the OOM frontier)" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val deep: Bin = Schemes.ana[BinF, Int, Bin](coalg).reverseGet(Deep)
    (Schemes.cata[BinF, Bin, Int](depthAlg).get(deep) == Deep) must beTrue
  }

  // ----- wide-and-deep: exercise the Traverse[F] sequencing a binary spine misses -----

  "hylo stays safe on a wide-AND-deep RoseF (high fanout + deep)" >> {
    val DeepRose = 100_000
    val Width = 8
    val coalg: Int => RoseF[Int] =
      d => if d <= 0 then RoseF(0, Nil) else RoseF(d, (d - 1) :: List.fill(Width)(-1))
    val countNodes: RoseF[Int] => Int = fr => 1 + fr.kids.sum
    val expected = (DeepRose + 1) + DeepRose * Width
    (Schemes.hylo[RoseF, Int, Int](coalg, countNodes).get(DeepRose) == expected) must beTrue
  }

  "ana builds and cata folds a wide-AND-deep Rose (N-ary Project/Embed)" >> {
    val DeepRose = 20_000
    val Width = 4
    val coalg: Int => RoseF[Int] =
      d => if d <= 0 then RoseF(0, Nil) else RoseF(d, (d - 1) :: List.fill(Width)(-1))
    val countNodes: RoseF[Int] => Int = fr => 1 + fr.kids.sum
    val built: Rose = Schemes.ana[RoseF, Int, Rose](coalg).reverseGet(DeepRose)
    val expected = (DeepRose + 1) + DeepRose * Width
    (Schemes.cata[RoseF, Rose, Int](countNodes).get(built) == expected) must beTrue
  }
