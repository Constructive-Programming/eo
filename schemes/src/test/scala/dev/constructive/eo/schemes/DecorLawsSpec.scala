package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic
import schemes.samples.{Bin, BinF}

/** Decoration laws — the per-value equations of the [[Decor]] vocabulary, plus the
  * behaviour-identity of the re-derived `cataF`/`anaF` (the identity fast path must agree with the
  * generic decoration route, proven by running a *fresh* user-written id decoration through the
  * generic route and comparing).
  */
class DecorLawsSpec extends Specification:

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // ----- gather-side equations ----------------------------------------------

  "Decor.histo gather == the Attr constructor" >> {
    val layer: BinF[Attr[BinF, Int]] =
      BinF.BranchF(Attr(1, BinF.LeafF(1)), Attr(2, BinF.LeafF(2)))
    Decor
      .histo[BinF, Int]
      .from(
        new Step[(Unit, BinF[Attr[BinF, Int]]), Int](layer, 3)
      ) === Attr(3, layer)
  }

  "Decor.para gather == (re-embedded subterm, result)" >> {
    val layer: BinF[(Bin, Int)] =
      BinF.BranchF((Bin.Leaf(1), 1), (Bin.Leaf(2), 2))
    Decor
      .para[BinF, Bin, Int]
      .from(
        new Step[(Unit, BinF[(Bin, Int)]), Int](layer, 3)
      ) === (Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), 3)
  }

  "Decor.cata gather == keep the result, discard the layer" >> {
    Decor
      .cata[BinF, Int]
      .from(
        new Step[(Unit, BinF[Int]), Int](BinF.BranchF(1, 2), 9)
      ) === 9
  }

  "Decor.cata is identity-stable across instantiations (the fast-path dispatch key)" >> {
    (Decor.cata[BinF, Int].asInstanceOf[AnyRef] eq
      Decor.cata[[x] =>> Option[x], String].asInstanceOf[AnyRef]) === true
  }

  "Decor.cata's vestigial read side throws" >> {
    val thrown =
      try { val _ = Decor.cata[BinF, Int].to(()); false }
      catch case _: UnsupportedOperationException => true
    thrown === true
  }

  // ----- scatter-side equations ---------------------------------------------

  "Decor.ana scatters every value as a Step (no Done arm)" >> {
    Decor.ana[BinF, Int].to(7) === new Step[(BinF[Int], Unit), Int]((), 7)
  }

  "Decor.ana satisfies the unit law: to(from(Step((), a))) == Step((), a)" >> {
    val d = Decor.ana[BinF, Int]
    d.to(d.from(new Step[(BinF[Int], Unit), Int]((), 5))) ===
      new Step[(BinF[Int], Unit), Int]((), 5)
  }

  "Decor.futu scatters Pure as Step and Roll as Done(layer)" >> {
    val d = Decor.futu[BinF, Int]
    val layer: BinF[Coattr[BinF, Int]] = BinF.BranchF(Coattr.Pure(1), Coattr.Pure(2))
    (d.to(Coattr.Pure(4)) === new Step[(BinF[Coattr[BinF, Int]], Unit), Int]((), 4))
      .and(d.to(Coattr.Roll(layer)) === new Done[(BinF[Coattr[BinF, Int]], Unit), Int](layer))
  }

  "Decor.futu's unit is Coattr.Pure" >> {
    val d = Decor.futu[BinF, Int]
    d.from(new Step[(BinF[Coattr[BinF, Int]], Unit), Int]((), 4)) === Coattr.Pure(4)
  }

  "Decor.apo (generic route) scatters Right as Step and Left as the projected layer" >> {
    val d = Decor.apo[BinF, Bin, Int]
    val grafted = Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))
    (d.to(Right(7)) === new Step[(BinF[Either[Bin, Int]], Unit), Int]((), 7))
      .and(
        d.to(Left(grafted)) === new Done[(BinF[Either[Bin, Int]], Unit), Int](
          BinF.BranchF(Left(Bin.Leaf(1)), Left(Bin.Leaf(2)))
        )
      )
  }

  "Decor.apo's unit is Right" >> {
    val d = Decor.apo[BinF, Bin, Int]
    d.from(new Step[(BinF[Either[Bin, Int]], Unit), Int]((), 7)) === Right(7)
  }

  // ----- re-derivation behaviour identity ------------------------------------

  // A FRESH user-written id gather — structurally Decor.cata but a distinct value,
  // so the generic driver cannot take the identity fast path.
  private val freshIdGather: DecorGather[BinF, Int, Int] =
    new Optic[Unit, Int, Unit, Int, BiAffine]:
      type X = (Unit, BinF[Int])
      def to(u: Unit): BiAffine[X, Unit] = throw new UnsupportedOperationException("vestigial")
      def from(xb: BiAffine[X, Int]): Int = xb match
        case s: Step[X, Int] => s.b
        case _: Done[X, Int] => throw new UnsupportedOperationException("fold-side Done")

  private val freshIdScatter: DecorScatter[BinF, Int, Int] =
    new Optic[Int, Int, Int, Int, BiAffine]:
      type X = (BinF[Int], Unit)
      def to(w: Int): BiAffine[X, Int] = new Step[X, Int]((), w)
      def from(xb: BiAffine[X, Int]): Int = xb match
        case s: Step[X, Int] => s.b
        case _: Done[X, Int] => throw new UnsupportedOperationException("unit on Step only")

  "the generic decoration route agrees with the identity fast path on cataF" >> {
    Schemes.cataF[BinF, Bin, Int, Int](freshIdGather)(sumAlg).get(tree) ===
      Schemes.cataF[BinF, Bin, Int](sumAlg).get(tree)
  }

  "the generic decoration route agrees with the identity fast path on anaF" >> {
    def expand(n: Int): BinF[Int] =
      if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    Schemes.anaF[BinF, Int, Int, Bin](freshIdScatter)(expand).reverseGet(5) ===
      Schemes.anaF[BinF, Int, Bin](expand).reverseGet(5)
  }

  "histo through Decor.histo: heads-only course-of-value == cata" >> {
    val viaHisto = Schemes
      .cataF[BinF, Bin, Attr[BinF, Int], Int](Decor.histo[BinF, Int]) { (s, layer) =>
        sumAlg(s, BinF.traverse.map(layer)(_.head))
      }
      .get(tree)
    viaHisto === Schemes.cataF[BinF, Bin, Int](sumAlg).get(tree)
  }

  "futu through Decor.futu: a two-layer-per-step coalgebra builds the right tree" >> {
    // Each step on seed n > 1 emits TWO layers at once: a branch whose left side is
    // a prebuilt leaf layer (Roll) and whose right side keeps unfolding (Pure).
    def coalg(n: Int): BinF[Coattr[BinF, Int]] =
      if n <= 1 then BinF.LeafF(1)
      else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
    val built = Schemes
      .anaF[BinF, Int, Coattr[BinF, Int], Bin](Decor.futu[BinF, Int])(coalg)
      .reverseGet(3)
    built === Bin.Branch(Bin.Leaf(3), Bin.Branch(Bin.Leaf(2), Bin.Leaf(1)))
  }

  // ----- native routes == generic decoration routes (the perf-win pins) --------

  "native histoF == the generic route at Decor.histo" >> {
    val alg: (Bin, BinF[Attr[BinF, Int]]) => Int = (s, layer) =>
      sumAlg(
        s,
        BinF
          .traverse
          .map(layer)(a =>
            a.head + a.tail.match {
              case BinF.LeafF(_) => 0; case BinF.BranchF(l, r) => l.head + r.head
            }
          ),
      )
    Schemes.histoF[BinF, Bin, Int](alg).get(tree) ===
      Schemes.cataF[BinF, Bin, Attr[BinF, Int], Int](Decor.histo[BinF, Int])(alg).get(tree)
  }

  "native futuF == the generic route at Decor.futu" >> {
    def coalg(n: Int): BinF[Coattr[BinF, Int]] =
      if n <= 1 then BinF.LeafF(1)
      else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
    Schemes.futuF[BinF, Int, Bin](coalg).reverseGet(4) ===
      Schemes.anaF[BinF, Int, Coattr[BinF, Int], Bin](Decor.futu[BinF, Int])(coalg).reverseGet(4)
  }

  // ----- generic-route end-to-end pins --------------------------------------

  "native paraF == the generic route at Decor.para" >> {
    // Algebra that uses BOTH the paired subterm and the result:
    // for a branch, sum child results and add 1 for each child subterm that is a Leaf.
    // tree = Branch(Branch(Leaf(1), Leaf(2)), Leaf(3))
    //   Leaf(1) → 1; Leaf(2) → 2
    //   Branch(Leaf(1), Leaf(2)) → 1+2 + 1 (Leaf(1)) + 1 (Leaf(2)) = 5
    //   Leaf(3) → 3
    //   Branch(Branch(L1,L2), Leaf(3)) → 5+3 + 0 (left is Branch) + 1 (Leaf(3)) = 9
    val alg: (Bin, BinF[(Bin, Int)]) => Int = (_, layer) =>
      layer match
        case BinF.LeafF(n)                    => n
        case BinF.BranchF((ls, la), (rs, ra)) =>
          la + ra + (if ls.isInstanceOf[Bin.Leaf] then 1 else 0) +
            (if rs.isInstanceOf[Bin.Leaf] then 1 else 0)
    Schemes.paraF[BinF, Bin, Int](alg).get(tree) ===
      Schemes.cataF[BinF, Bin, (Bin, Int), Int](Decor.para[BinF, Bin, Int])(alg).get(tree)
  }

  "native apoF == the generic route at Decor.apo (distApo)" >> {
    // Coalg with one graft: at n <= 0 emit a leaf; otherwise graft Bin.Leaf(n) as left child.
    // The native apoF places the graft by reference; the generic route (distApo via project)
    // rebuilds it — structural equality (==) holds, reference identity (eq) only for native.
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.LeafF(0)
      else BinF.BranchF(Left(Bin.Leaf(n)), Right(n - 1))
    val nativeResult = Schemes.apoF[BinF, Int, Bin](coalg).reverseGet(2)
    val genericResult =
      Schemes.anaF[BinF, Int, Either[Bin, Int], Bin](Decor.apo[BinF, Bin, Int])(coalg).reverseGet(2)
    // Both produce the same tree by value; use == not eq (generic route REBUILDS the graft).
    nativeResult === genericResult
  }
