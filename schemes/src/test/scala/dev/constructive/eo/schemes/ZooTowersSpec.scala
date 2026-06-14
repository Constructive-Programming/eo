package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import cats.arrow.FunctionK
import cats.~>
import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF}

/** Behaviour + degeneration spec for the schemes that complete the two index towers and the
  * natural-transformation axis:
  *
  *   - the comonad-tower rung [[Schemes.zygo]] (`X = F[(B, A)]`) and its generalisation
  *     [[Schemes.mutu]] (`X = F[(A, B)]`);
  *   - the build-side duals [[Schemes.cozygo]] (`X = Either[B, A]`) and [[Schemes.comutu]] (`X =
  *     Either[A, B]`);
  *   - the layer-transforming pair [[Schemes.prepro]] / [[Schemes.postpro]] (`η : F ~> F`).
  *
  * Each is anchored by the law that proves the generalisation correct — it collapses to a known
  * scheme when its extra power is unused — plus a behaviour case that the base scheme could not
  * express, plus stack-safety (10⁶ for the `O(n)` schemes; a heap-machine-crossing depth for the
  * `O(n · depth)` pre/postpro).
  */
class ZooTowersSpec extends Specification:

  sequential

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  private val sumLeaves: BinF[Int] => Int =
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r

  private val depthAlg: BinF[Int] => Int =
    case BinF.LeafF(_)      => 0
    case BinF.BranchF(l, r) => 1 + math.max(l, r)

  private def deepSpine(n: Int): Bin =
    var b: Bin = Bin.Leaf(0)
    var i = 0
    while i < n do
      b = Bin.Branch(b, Bin.Leaf(0))
      i += 1
    b

  // ----- zygo (X = F[(B, A)], the store comonad over an auxiliary carrier) -----

  private val sizeAux: BinF[Int] => Int =
    case BinF.LeafF(_)      => 1
    case BinF.BranchF(l, r) => 1 + l + r

  "B-blind zygo degenerates to cata" >> {
    val viaZygo = Schemes
      .zygo[BinF, Bin, Int, Int](sizeAux)(layer => sumLeaves(BinF.traverse.map(layer)(_._2)))
      .get(tree)
    viaZygo === Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree)
  }

  "zygo at B = S with aux = embed is exactly para" >> {
    def countNodes(b: Bin): Int = b match
      case Bin.Leaf(_)      => 1
      case Bin.Branch(l, r) => 1 + countNodes(l) + countNodes(r)
    val alg: BinF[(Bin, Int)] => Int =
      case BinF.LeafF(n)                          => n
      case BinF.BranchF((lSub, lRes), (_, rRes)) => lRes + rRes + countNodes(lSub)
    val viaZygo = Schemes.zygo[BinF, Bin, Int, Bin](BinF.basis.embed)(alg).get(tree)
    viaZygo === Schemes.para[BinF, Bin, Int](alg).get(tree)
  }

  "zygo reads its auxiliary result: leaf-sum plus each branch's left-subtree size" >> {
    val alg: BinF[(Int, Int)] => Int =
      case BinF.LeafF(n)                            => n
      case BinF.BranchF((sizeL, resL), (_, resR)) => resL + resR + sizeL
    // inner branches: 1+2+1=4 and 3+4+1=8; root: 4+8+sizeL(3) = 15.
    Schemes.zygo[BinF, Bin, Int, Int](sizeAux)(alg).get(tree) === 15
  }

  "zygo is stack-safe folding a 10^6-deep spine" >> {
    val deep = deepSpine(1_000_000)
    val depthMain: BinF[(Int, Int)] => Int =
      case BinF.LeafF(_)                  => 0
      case BinF.BranchF((_, l), (_, r)) => 1 + math.max(l, r)
    Schemes.zygo[BinF, Bin, Int, Int](sizeAux)(depthMain).get(deep) === 1_000_000
  }

  // ----- mutu (X = F[(A, B)], mutual recursion) -----

  "A-blind mutu degenerates to zygo (modulo the tuple flip)" >> {
    // mutu with algB ignoring the A half == zygo(algB-as-aux)(algA).
    val algB: BinF[(Int, Int)] => Int = // = sizeAux on the B half
      case BinF.LeafF(_)                          => 1
      case BinF.BranchF((_, b1), (_, b2)) => 1 + b1 + b2
    val algA: BinF[(Int, Int)] => Int =
      case BinF.LeafF(n)                            => n
      case BinF.BranchF((a1, b1), (a2, _)) => a1 + a2 + b1
    val viaMutu = Schemes.mutu[BinF, Bin, Int, Int](algA, algB).get(tree)
    val viaZygo = Schemes
      .zygo[BinF, Bin, Int, Int](sizeAux) {
        case BinF.LeafF(n)                            => n
        case BinF.BranchF((sizeL, resL), (_, resR)) => resL + resR + sizeL
      }
      .get(tree)
    viaMutu === viaZygo and (viaMutu === 15)
  }

  "mutu is genuinely mutual: each algebra reads the other's half" >> {
    // A = signed sum where the sign of a branch flips when its B (node count) is even.
    val count: BinF[(Int, Int)] => Int =
      case BinF.LeafF(_)                  => 1
      case BinF.BranchF((_, b1), (_, b2)) => 1 + b1 + b2
    val signed: BinF[(Int, Int)] => Int =
      case BinF.LeafF(n)                  => n
      case BinF.BranchF((a1, b1), (a2, _)) =>
        if (b1 % 2 == 0) a2 - a1 else a1 + a2
    // inner branches have count 3 (odd): 1+2=3 and 3+4=7; root left-count 3 (odd): 3+7 = 10.
    Schemes.mutu[BinF, Bin, Int, Int](signed, count).get(tree) === 10
  }

  // ----- cozygo / g-apo (X = Either[B, A], build-side dual of zygo) -----

  private val anaCoalg: Int => BinF[Int] =
    n => if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  "all-Right cozygo degenerates to ana" >> {
    val viaCozygo = Schemes
      .cozygo[BinF, Int, Int, Bin](_ => BinF.LeafF(0))(n =>
        BinF.traverse.map(anaCoalg(n))(Right(_))
      )
      .reverseGet(6)
    viaCozygo === Schemes.ana[BinF, Int, Bin](anaCoalg).reverseGet(6)
  }

  "cozygo unfolds its Left slots through the auxiliary coalgebra" >> {
    val aux: Int => BinF[Int] =
      b => if b <= 0 then BinF.LeafF(0) else BinF.BranchF(b - 1, b - 1)
    val coalg: Int => BinF[Either[Int, Int]] =
      a => if a <= 0 then BinF.LeafF(99) else BinF.BranchF(Left(1), Right(a - 1))
    val built = Schemes.cozygo[BinF, Int, Int, Bin](aux)(coalg).reverseGet(1)
    built === Bin.Branch(Bin.Branch(Bin.Leaf(0), Bin.Leaf(0)), Bin.Leaf(99))
  }

  "cozygo is stack-safe building a 10^6-deep spine (all-Right path)" >> {
    val coalg: Int => BinF[Either[Int, Int]] =
      n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Right(n - 1), Right(-1))
    val built = Schemes.cozygo[BinF, Int, Int, Bin](_ => BinF.LeafF(0))(coalg).reverseGet(1_000_000)
    Schemes.cata[BinF, Bin, Int](depthAlg).get(built) === 1_000_000
  }

  // ----- comutu (X = Either[A, B], build-side dual of mutu) -----

  "single-coalgebra comutu degenerates to ana" >> {
    val viaComutu = Schemes
      .comutu[BinF, Int, Int, Bin](
        a => BinF.traverse.map(anaCoalg(a))(Left(_)),
        (b: Int) => BinF.LeafF(b),
      )
      .reverseGet(6)
    viaComutu === Schemes.ana[BinF, Int, Bin](anaCoalg).reverseGet(6)
  }

  "comutu alternates between its two coalgebras" >> {
    val coalgA: Int => BinF[Either[Int, Int]] =
      a => if a <= 0 then BinF.LeafF(0) else BinF.BranchF(Left(a - 1), Right(a))
    val coalgB: Int => BinF[Either[Int, Int]] = b => BinF.LeafF(b)
    val built = Schemes.comutu[BinF, Int, Int, Bin](coalgA, coalgB).reverseGet(1)
    built === Bin.Branch(Bin.Leaf(0), Bin.Leaf(1))
  }

  // ----- prepro / postpro (η : F ~> F, the layer-transforming axis) -----

  private val incLeaf: BinF ~> BinF = new (BinF ~> BinF):
    def apply[A](fa: BinF[A]): BinF[A] = fa match
      case BinF.LeafF(n)          => BinF.LeafF(n + 1)
      case b @ BinF.BranchF(_, _) => b

  "prepro with η = id is exactly cata" >> {
    Schemes.prepro[BinF, Bin, Int](FunctionK.id[BinF])(sumLeaves).get(tree) ===
      Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree)
  }

  "prepro applies η once per level: each leaf gains its depth" >> {
    // leaves sit at depth 2, so η (which +1's a leaf) fires twice on each: sum 10 + 4*2 = 18.
    Schemes.prepro[BinF, Bin, Int](incLeaf)(sumLeaves).get(tree) === 18
  }

  "prepro is stack-safe across the heap-machine boundary (O(n·depth), so not 10^6)" >> {
    val deep = deepSpine(2_000)
    Schemes.prepro[BinF, Bin, Int](FunctionK.id[BinF])(sumLeaves).get(deep) === 0
  }

  "postpro with η = id is exactly ana" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, n - 1)
    Schemes.postpro[BinF, Int, Bin](FunctionK.id[BinF])(coalg).reverseGet(2) ===
      Schemes.ana[BinF, Int, Bin](coalg).reverseGet(2)
  }

  "postpro applies η once per level on the build side: each leaf gains its depth" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, n - 1)
    // seed 2 builds a depth-2 perfect tree of 4 zero-leaves; η lifts each by its depth (2): sum 8.
    val built = Schemes.postpro[BinF, Int, Bin](incLeaf)(coalg).reverseGet(2)
    Schemes.cata[BinF, Bin, Int](sumLeaves).get(built) === 8
  }

  "postpro is stack-safe across the heap-machine boundary (O(n·depth), so not 10^6)" >> {
    val coalg: Int => BinF[Int] = n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1)
    val built = Schemes.postpro[BinF, Int, Bin](FunctionK.id[BinF])(coalg).reverseGet(2_000)
    Schemes.cata[BinF, Bin, Int](depthAlg).get(built) === 2_000
  }
