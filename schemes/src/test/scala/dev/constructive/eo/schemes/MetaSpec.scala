package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF, Rose, RoseF}
import schemes.zoo.{Attr, Coattr}

/** The metamorphism — fold→unfold, the direction-dual of [[FusionSpec]]'s hylo — and the proof that
  * it is the **honest non-fusion**.
  *
  * `meta` folds an `F`-recursive `S` to a neck value `A`, then unfolds a *different* `G`-recursive
  * `T`. Here `F = BinF`, `G = RoseF`, `T = Rose` — genuinely different functors, which is *why* it
  * cannot deforest: there is no shared functor whose `project ∘ embed` could cancel, so the neck
  * `A` is materialised (the `Meta` existential is `X = A`).
  *
  *   - meta == `ana.reverseGet ∘ cata.get` (it *is* the two-pass composition);
  *   - both passes run — the `BinF` fold calls `project`, the `RoseF` build calls `embed` (contrast
  *     [[FusionSpec]]'s fused hylo, which calls *neither*);
  *   - `metaChrono` is the same at the universal indices (histo→futu), degenerating to `meta`;
  *   - stack-safe to 10⁶ across both passes.
  */
class MetaSpec extends Specification:

  sequential

  // Counting bases, split by side: meta needs Project[BinF, Bin] to fold and Embed[RoseF, Rose] to
  // build — so a non-zero `projects` proves the fold pass ran, a non-zero `embeds` the build pass.
  final private class CountingBin extends Basis[BinF, Bin]:
    var projects = 0

    def project(s: Bin): BinF[Bin] =
      projects += 1
      s match
        case Bin.Leaf(n)      => BinF.LeafF(n)
        case Bin.Branch(l, r) => BinF.BranchF(l, r)

    def embed(fs: BinF[Bin]): Bin = BinF.basis.embed(fs)

  final private class CountingRose extends Basis[RoseF, Rose]:
    var embeds = 0
    def project(r: Rose): RoseF[Rose] = RoseF.basis.project(r)

    def embed(fr: RoseF[Rose]): Rose =
      embeds += 1
      RoseF.basis.embed(fr)

  // mixed tree: Branch(Leaf 1, Branch(Leaf 2, Leaf 3)) — leaf sum 6.
  private val tree: Bin = Bin.Branch(Bin.Leaf(1), Bin.Branch(Bin.Leaf(2), Bin.Leaf(3)))

  // fold (F = BinF): leaf sum → the neck Int.
  private val leafSum: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  // unfold (G = RoseF): neck n → a left Rose spine of n+1 nodes (labels n, n-1, …, 0).
  private val spine: Int => RoseF[Int] = n =>
    if n <= 0 then RoseF(0, Nil) else RoseF(n, List(n - 1))

  // count Rose nodes, to observe the built T.
  private val countRose: RoseF[Int] => Int = fr => 1 + fr.kids.sum

  "meta folds F then unfolds a different G: Bin --leafSum--> Int --spine--> Rose" >> {
    val m = Schemes.meta[BinF, Bin, Int, RoseF, Rose](leafSum, spine)
    val built: Rose = m.get(tree) // neck 6 → spine of 7 nodes
    Schemes.cata[RoseF, Rose, Int](countRose).get(built) === 7
  }

  "meta == ana.reverseGet ∘ cata.get via the cata.meta(ana) seam (it IS the two-pass composition)" >> {
    val cata = Schemes.cata[BinF, Bin, Int](leafSum)
    val ana = Schemes.ana[RoseF, Int, Rose](spine)
    val viaSeam = cata.meta(ana)
    val viaManual: Bin => Rose = s => ana.reverseGet(cata.get(s))
    val trees = List(tree, Bin.Leaf(4), Bin.Branch(Bin.Leaf(5), Bin.Leaf(6)))
    trees.map(viaSeam.get) === trees.map(viaManual)
  }

  "no fusion: meta materialises the neck — BOTH the F-fold's project AND the G-build's embed run" >> {
    val cb = new CountingBin
    val cr = new CountingRose
    val m = Schemes.meta[BinF, Bin, Int, RoseF, Rose](leafSum, spine)(using
      BinF.traverse,
      cb,
      RoseF.traverse,
      cr,
    )
    val _ = m.get(tree) // neck 6 → 7 Rose nodes

    // Contrast FusionSpec's hylo (both zero). Here neither is zero — F ≠ G, the neck is real.
    (cb.projects must be_>(0)).and(cr.embeds must be_>(0))
  }

  // Probe the F =:= G hypothesis: if the type mismatch were the barrier, a SAME-functor meta should
  // fuse (drop project/embed to 0). It doesn't — one Basis[BinF, Bin] serves both sides, the
  // obstruction is gone, yet BOTH counters still fire. The real barrier is the scalar neck: the
  // fold's projects (input side) are never adjacent to the unfold's embeds (output side), so there
  // is no `project ∘ embed` to cancel. F = G removes a *sufficient* witness for no-fusion, not the
  // *cause*.
  "F = G = BinF STILL does not fuse: the scalar neck, not the functor mismatch, is the barrier" >> {
    final class Counting extends Basis[BinF, Bin]:
      var projects = 0
      var embeds = 0
      def project(s: Bin): BinF[Bin] =
        projects += 1
        BinF.basis.project(s)
      def embed(fs: BinF[Bin]): Bin =
        embeds += 1
        BinF.basis.embed(fs)

    val c = new Counting
    // fold Bin --leafSum--> Int, then unfold Int --binSpine--> Bin (a left spine of n Branches).
    val binSpine: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val m = Schemes.meta[BinF, Bin, Int, BinF, Bin](leafSum, binSpine)(using
      BinF.traverse,
      c,
      BinF.traverse,
      c,
    )
    val _ = m.get(tree) // neck 6 → a 6-deep Bin spine

    (c.projects must be_>(0)).and(c.embeds must be_>(0))
  }

  // ----- metaChrono: the same seam at the universal indices (histo → futu) -----

  "metaChrono folds course-of-value (histo) then multi-layer-unfolds (futu)" >> {
    // heads-only histo == leafSum; all-Pure futu == spine — so metaChrono == meta here.
    val histoSum: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l.head + r.head
    val futuSpine: Int => RoseF[Coattr[RoseF, Int]] =
      n => if n <= 0 then RoseF(0, Nil) else RoseF(n, List(Coattr.Pure(n - 1)))

    val viaChrono = Schemes.metaChrono[BinF, Bin, Int, RoseF, Rose](histoSum, futuSpine)
    val viaMeta = Schemes.meta[BinF, Bin, Int, RoseF, Rose](leafSum, spine)
    val trees = List(tree, Bin.Leaf(4), Bin.Branch(Bin.Leaf(5), Bin.Leaf(6)))
    trees.map(viaChrono.get) === trees.map(viaMeta.get)
  }

  "histo.meta(futu) seam == Schemes.metaChrono" >> {
    val histoSum: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l.head + r.head
    val futuSpine: Int => RoseF[Coattr[RoseF, Int]] =
      n => if n <= 0 then RoseF(0, Nil) else RoseF(n, List(Coattr.Pure(n - 1)))
    val viaSeam =
      Schemes.histo[BinF, Bin, Int](histoSum).meta(Schemes.futu[RoseF, Int, Rose](futuSpine))
    val viaCtor = Schemes.metaChrono[BinF, Bin, Int, RoseF, Rose](histoSum, futuSpine)
    val trees = List(tree, Bin.Branch(Bin.Leaf(7), Bin.Leaf(8)))
    trees.map(viaSeam.get) === trees.map(viaCtor.get)
  }

  // ----- stack-safety across both passes: 10^6 -----

  "meta is stack/space-safe: fold a 10^6-deep Bin, unfold a 10^6-node Rose" >> {
    val Deep = 1_000_000
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < Deep do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    val depth: BinF[Int] => Int =
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l, r)
    val m = Schemes.meta[BinF, Bin, Int, RoseF, Rose](depth, spine)
    val built: Rose = m.get(b) // neck = Deep → Rose spine of Deep+1 nodes
    Schemes.cata[RoseF, Rose, Int](countRose).get(built) === Deep + 1
  }
