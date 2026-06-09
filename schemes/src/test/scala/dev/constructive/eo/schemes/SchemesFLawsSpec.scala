package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import optics.Optic.* // get, cross

import schemes.samples.{Bin, BinF, Rose, RoseF}

/** Law/coherence checks for the typed pattern-functor schemes.
  *
  *   - '''Project/Embed coherence''' — the hand-written `S`↔`F` correspondence is not
  *     compiler-checked, so its two round-trip laws are property-tested here.
  *   - '''Hylo law (pure flavor)''' — `hyloF == anaF.cross(cataF)` holds *generically* only when
  *     the algebra ignores its node argument (a pure `F[A] => A` fold). Tested via `forAll`.
  *   - '''Hylo law (para flavor)''' — for a node-reading algebra, `hyloF` threads the *seed* while
  *     the materializing `cataF` threads the rebuilt `S`, so the two coincide only under the
  *     seed↔`embed(coalg(seed))` correspondence. Verified at specific points, NOT via `forAll`.
  */
class SchemesFLawsSpec extends Specification with ScalaCheck:

  // bounded-depth Bin generator (keeps trees small for the property runs)
  private def genBin(depth: Int): Gen[Bin] =
    if depth <= 0 then Gen.choose(0, 20).map(Bin.Leaf(_))
    else
      Gen.frequency(
        1 -> Gen.choose(0, 20).map(Bin.Leaf(_)),
        2 -> Gen.zip(genBin(depth - 1), genBin(depth - 1)).map((l, r) => Bin.Branch(l, r)),
      )

  private given Arbitrary[Bin] = Arbitrary(genBin(5))

  // one layer of BinF over arbitrary Bin children
  private given Arbitrary[BinF[Bin]] = Arbitrary(
    Gen.oneOf(
      Gen.choose(0, 20).map(BinF.LeafF(_)),
      Gen.zip(genBin(4), genBin(4)).map((l, r) => BinF.BranchF(l, r)),
    )
  )

  // ----- Project/Embed coherence -----

  "embed(project(s)) == s (round-trip through one S layer)" >> prop { (s: Bin) =>
    val basis = summon[Basis[BinF, Bin]]
    (basis.embed(basis.project(s)) == s) must beTrue
  }

  "project(embed(fs)) == fs (round-trip through one F layer)" >> prop { (fs: BinF[Bin]) =>
    val basis = summon[Basis[BinF, Bin]]
    (basis.project(basis.embed(fs)) == fs) must beTrue
  }

  // same coherence laws for the N-ary RoseF/Rose basis (a swapped label/kids mapping would escape
  // the node-count behaviour tests, so the correspondence is pinned here)
  private def genRose(depth: Int): Gen[Rose] =
    for
      label <- Gen.choose(0, 20)
      kids <-
        if depth <= 0 then Gen.const(List.empty[Rose])
        else Gen.choose(0, 3).flatMap(n => Gen.listOfN(n, genRose(depth - 1)))
    yield Rose(label, kids)

  private given Arbitrary[Rose] = Arbitrary(genRose(3))

  private given Arbitrary[RoseF[Rose]] = Arbitrary(
    for
      label <- Gen.choose(0, 20)
      n <- Gen.choose(0, 3)
      kids <- Gen.listOfN(n, genRose(2))
    yield RoseF(label, kids)
  )

  "embed(project(r)) == r for the N-ary RoseF/Rose basis" >> prop { (r: Rose) =>
    val basis = summon[Basis[RoseF, Rose]]
    (basis.embed(basis.project(r)) == r) must beTrue
  }

  "project(embed(fr)) == fr for the N-ary RoseF/Rose basis" >> prop { (fr: RoseF[Rose]) =>
    val basis = summon[Basis[RoseF, Rose]]
    (basis.project(basis.embed(fr)) == fr) must beTrue
  }

  // ----- hylo law, pure flavor (generic forAll) -----

  // seed n -> a right spine of (n+1) leaves of value 1
  private val coalg: Int => BinF[Int] = n =>
    if n <= 0 then BinF.LeafF(1) else BinF.BranchF(0, n - 1)

  // pure algebra: ignores the node argument (so it is shape-agnostic to seed-vs-S)
  private val pureSum: BinF[Int] => Int = {
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r
  }

  "hyloF == anaF.cross(cataF) for a PURE algebra (the hylo law)" >> {
    forAll(Gen.choose(0, 12)) { (seed: Int) =>
      val fused = Schemes.hyloF[BinF, Int, Int](coalg, (_, fa) => pureSum(fa)).get(seed)
      val materializing =
        Schemes
          .anaF[BinF, Int, Bin](coalg)
          .cross(Schemes.cataF[BinF, Bin, Int]((_, fa) => pureSum(fa)))
          .get(seed)
      fused == materializing
    }
  }

  // ----- hylo law, para flavor (point tests, not forAll) -----
  //
  // A node-reading algebra: hyloF sees the Int seed at each layer; the materializing path sees the
  // rebuilt Bin. They are NOT equal in general — verified here only that fused matches itself and a
  // hand-computed value, documenting why forAll does not apply.

  "para-flavored hyloF computes the expected value at specific seeds" >> {
    // alg reads neither node meaningfully here but is typed para; leaf-count over the spine
    val leafCount: (Int, BinF[Int]) => Int = (_, fa) =>
      fa match
        case BinF.LeafF(_)      => 1
        case BinF.BranchF(l, r) => l + r
    val h = Schemes.hyloF(coalg, leafCount)
    (h.get(0) == 1).and(h.get(3) == 4).and(h.get(7) == 8)
  }
