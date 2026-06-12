package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic
import schemes.samples.{Bin, BinF}

/** Decoration laws — the per-value equations of the [[Gather]]/[[Scatter]] vocabulary, plus the
  * behaviour-identity of the re-derived `cata`/`ana` (the identity fast path must agree with the
  * generic decoration route, proven by running a *fresh* user-written id decoration through the
  * generic route and comparing).
  */
class GatherScatterLawsSpec extends Specification:

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

  private val sumAlg: (Bin, BinF[Int]) => Int = (_, fa) =>
    fa match
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l + r

  // ----- demoted decorations as LAW FIXTURES ---------------------------------
  // para's gather and apo's scatter have no shipped values (their generic routes
  // are deliberately inferior: re-embed / distApo) — their decoration semantics
  // live HERE, pinning the native Schemes.para / Schemes.apo engines.

  private def paraGather[F[_]: cats.Functor, S, A](using E: Embed[F, S]): Gather[F, (S, A), A] =
    new Optic[Unit, (S, A), Unit, A, BiAffine]:
      type X = (Unit, F[(S, A)])
      def to(u: Unit): BiAffine[X, Unit] =
        throw new UnsupportedOperationException("gather-only")
      def from(xb: BiAffine[X, A]): (S, A) = xb match
        case s: Step[X, A] => (E.embed(cats.Functor[F].map(s.snd)(_._1)), s.b)
        case _: Done[X, A] => throw new UnsupportedOperationException("fold-side Done")

  private def apoScatter[F[_]: cats.Functor, S, A](using
      P: Project[F, S]
  ): Scatter[F, Either[S, A], A] =
    new Optic[Either[S, A], Either[S, A], A, A, BiAffine]:
      type X = (F[Either[S, A]], Unit)
      def to(w: Either[S, A]): BiAffine[X, A] = w match
        case Right(a) => new Step[X, A]((), a)
        case Left(s)  => new Done[X, A](cats.Functor[F].map(P.project(s))(Left(_)))
      def from(xb: BiAffine[X, A]): Either[S, A] = xb match
        case s: Step[X, A] => Right(s.b)
        case _: Done[X, A] => throw new UnsupportedOperationException("unit on Step only")

  // ----- gather-side equations ----------------------------------------------

  "Gather.histo gather == the Attr constructor" >> {
    val layer: BinF[Attr[BinF, Int]] =
      BinF.BranchF(Attr(1, BinF.LeafF(1)), Attr(2, BinF.LeafF(2)))
    Gather
      .histo[BinF, Int]
      .from(
        new Step[(Unit, BinF[Attr[BinF, Int]]), Int](layer, 3)
      ) === Attr(3, layer)
  }

  "the para gather fixture == (re-embedded subterm, result)" >> {
    val layer: BinF[(Bin, Int)] =
      BinF.BranchF((Bin.Leaf(1), 1), (Bin.Leaf(2), 2))
    paraGather[BinF, Bin, Int]
      .from(
        new Step[(Unit, BinF[(Bin, Int)]), Int](layer, 3)
      ) === (Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), 3)
  }

  "Gather.cata gather == keep the result, discard the layer" >> {
    Gather
      .cata[BinF, Int]
      .from(
        new Step[(Unit, BinF[Int]), Int](BinF.BranchF(1, 2), 9)
      ) === 9
  }

  "Gather.cata is identity-stable across instantiations (the fast-path dispatch key)" >> {
    (Gather.cata[BinF, Int].asInstanceOf[AnyRef] eq
      Gather.cata[[x] =>> Option[x], String].asInstanceOf[AnyRef]) === true
  }

  "Gather.cata's vestigial read side throws" >> {
    val thrown =
      try { val _ = Gather.cata[BinF, Int].to(()); false }
      catch case _: UnsupportedOperationException => true
    thrown === true
  }

  // ----- scatter-side equations ---------------------------------------------

  "Scatter.ana scatters every value as a Step (no Done arm)" >> {
    Scatter.ana[BinF, Int].to(7) === new Step[(BinF[Int], Unit), Int]((), 7)
  }

  "Scatter.ana satisfies the unit law: to(from(Step((), a))) == Step((), a)" >> {
    val d = Scatter.ana[BinF, Int]
    d.to(d.from(new Step[(BinF[Int], Unit), Int]((), 5))) ===
      new Step[(BinF[Int], Unit), Int]((), 5)
  }

  "Scatter.futu scatters Pure as Step and Roll as Done(layer)" >> {
    val d = Scatter.futu[BinF, Int]
    val layer: BinF[Coattr[BinF, Int]] = BinF.BranchF(Coattr.Pure(1), Coattr.Pure(2))
    (d.to(Coattr.Pure(4)) === new Step[(BinF[Coattr[BinF, Int]], Unit), Int]((), 4))
      .and(d.to(Coattr.Roll(layer)) === new Done[(BinF[Coattr[BinF, Int]], Unit), Int](layer))
  }

  "Scatter.futu's unit is Coattr.Pure" >> {
    val d = Scatter.futu[BinF, Int]
    d.from(new Step[(BinF[Coattr[BinF, Int]], Unit), Int]((), 4)) === Coattr.Pure(4)
  }

  "the apo scatter fixture (distApo) scatters Right as Step and Left as the projected layer" >> {
    val d = apoScatter[BinF, Bin, Int]
    val grafted = Bin.Branch(Bin.Leaf(1), Bin.Leaf(2))
    (d.to(Right(7)) === new Step[(BinF[Either[Bin, Int]], Unit), Int]((), 7))
      .and(
        d.to(Left(grafted)) === new Done[(BinF[Either[Bin, Int]], Unit), Int](
          BinF.BranchF(Left(Bin.Leaf(1)), Left(Bin.Leaf(2)))
        )
      )
  }

  "the apo scatter fixture's unit is Right" >> {
    val d = apoScatter[BinF, Bin, Int]
    d.from(new Step[(BinF[Either[Bin, Int]], Unit), Int]((), 7)) === Right(7)
  }

  // ----- re-derivation behaviour identity ------------------------------------

  // A FRESH user-written id gather — structurally Gather.cata but a distinct value,
  // so the generic driver cannot take the identity fast path.
  private val freshIdGather: Gather[BinF, Int, Int] =
    new Optic[Unit, Int, Unit, Int, BiAffine]:
      type X = (Unit, BinF[Int])
      def to(u: Unit): BiAffine[X, Unit] = throw new UnsupportedOperationException("vestigial")
      def from(xb: BiAffine[X, Int]): Int = xb match
        case s: Step[X, Int] => s.b
        case _: Done[X, Int] => throw new UnsupportedOperationException("fold-side Done")

  private val freshIdScatter: Scatter[BinF, Int, Int] =
    new Optic[Int, Int, Int, Int, BiAffine]:
      type X = (BinF[Int], Unit)
      def to(w: Int): BiAffine[X, Int] = new Step[X, Int]((), w)
      def from(xb: BiAffine[X, Int]): Int = xb match
        case s: Step[X, Int] => s.b
        case _: Done[X, Int] => throw new UnsupportedOperationException("unit on Step only")

  "the generic decoration route agrees with the identity fast path on cata" >> {
    Schemes.cata[BinF, Bin, Int, Int](freshIdGather)(sumAlg).get(tree) ===
      Schemes.cata[BinF, Bin, Int](sumAlg).get(tree)
  }

  "the generic decoration route agrees with the identity fast path on ana" >> {
    def expand(n: Int): BinF[Int] =
      if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)
    Schemes.ana[BinF, Int, Int, Bin](freshIdScatter)(expand).reverseGet(5) ===
      Schemes.ana[BinF, Int, Bin](expand).reverseGet(5)
  }

  "histo through Gather.histo: heads-only course-of-value == cata" >> {
    val viaHisto = Schemes
      .cata[BinF, Bin, Attr[BinF, Int], Int](Gather.histo[BinF, Int]) { (s, layer) =>
        sumAlg(s, BinF.traverse.map(layer)(_.head))
      }
      .get(tree)
    viaHisto === Schemes.cata[BinF, Bin, Int](sumAlg).get(tree)
  }

  "futu through Scatter.futu: a two-layer-per-step coalgebra builds the right tree" >> {
    // Each step on seed n > 1 emits TWO layers at once: a branch whose left side is
    // a prebuilt leaf layer (Roll) and whose right side keeps unfolding (Pure).
    def coalg(n: Int): BinF[Coattr[BinF, Int]] =
      if n <= 1 then BinF.LeafF(1)
      else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
    val built = Schemes
      .ana[BinF, Int, Coattr[BinF, Int], Bin](Scatter.futu[BinF, Int])(coalg)
      .reverseGet(3)
    built === Bin.Branch(Bin.Leaf(3), Bin.Branch(Bin.Leaf(2), Bin.Leaf(1)))
  }

  // ----- native routes == generic decoration routes (the perf-win pins) --------

  "native histo == the generic route at Gather.histo" >> {
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
    Schemes.histo[BinF, Bin, Int](alg).get(tree) ===
      Schemes.cata[BinF, Bin, Attr[BinF, Int], Int](Gather.histo[BinF, Int])(alg).get(tree)
  }

  "native futu == the generic route at Scatter.futu" >> {
    def coalg(n: Int): BinF[Coattr[BinF, Int]] =
      if n <= 1 then BinF.LeafF(1)
      else BinF.BranchF(Coattr.Roll(BinF.LeafF(n)), Coattr.Pure(n - 1))
    Schemes.futu[BinF, Int, Bin](coalg).reverseGet(4) ===
      Schemes.ana[BinF, Int, Coattr[BinF, Int], Bin](Scatter.futu[BinF, Int])(coalg).reverseGet(4)
  }

  // ----- generic-route end-to-end pins --------------------------------------

  "native para == the generic route at the para gather fixture" >> {
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
    Schemes.para[BinF, Bin, Int](alg).get(tree) ===
      Schemes.cata[BinF, Bin, (Bin, Int), Int](paraGather[BinF, Bin, Int])(alg).get(tree)
  }

  "native apo == the generic route at the apo scatter fixture (distApo)" >> {
    // Coalg with one graft: at n <= 0 emit a leaf; otherwise graft Bin.Leaf(n) as left child.
    // The native apo places the graft by reference; the generic route (distApo via project)
    // rebuilds it — structural equality (==) holds, reference identity (eq) only for native.
    def coalg(n: Int): BinF[Either[Bin, Int]] =
      if n <= 0 then BinF.LeafF(0)
      else BinF.BranchF(Left(Bin.Leaf(n)), Right(n - 1))
    val nativeResult = Schemes.apo[BinF, Int, Bin](coalg).reverseGet(2)
    val genericResult =
      Schemes.ana[BinF, Int, Either[Bin, Int], Bin](apoScatter[BinF, Bin, Int])(coalg).reverseGet(2)
    // Both produce the same tree by value; use == not eq (generic route REBUILDS the graft).
    nativeResult === genericResult
  }
