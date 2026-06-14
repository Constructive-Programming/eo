package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import cats.instances.option.*
import cats.{Eval, Id}
import org.specs2.mutable.Specification

import optics.Optic.* // get, reverseGet

import schemes.samples.{Bin, BinF}
import schemes.zoo.{Attr, Coattr}

/** Behaviour spec for the monadic (`*M`) scheme family — [[Schemes.cataM]] / `paraM` / `histoM` /
  * `anaM` / `apoM` / `futuM` / `hyloM` / `chronoM`, all riding [[Machines.foldLayeredM]].
  *
  * Two anchors per scheme:
  *
  *   - '''Agreement at `M = Id`''' — the effectful scheme with the identity monad reproduces its
  *     pure twin exactly (the cross-architecture pin: pure engine vs `Monad`-lifted engine agree).
  *   - '''Real effect''' — threading `Option` short-circuits the whole fold/build to `None` when
  *     any node aborts, which the pure scheme cannot express.
  *
  * Plus stack-safety: the `M` engine frames every node on a heap `ArrayDeque` and loops through
  * `tailRecM`, so a lawful stack-safe `M` (`Eval`) folds/builds 10⁶-deep with no native-stack use.
  */
class SchemesMSpec extends Specification:

  sequential

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Branch(Bin.Leaf(3), Bin.Leaf(4)))

  private val withNeg: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(-2)), Bin.Leaf(3))

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

  // ----- cataM -----

  "cataM at M = Id reproduces cata" >> {
    val viaM: Id[Int] = Schemes.cataM[Id, BinF, Bin, Int](sumLeaves).get(tree)
    viaM === Schemes.cata[BinF, Bin, Int](sumLeaves).get(tree)
  }

  "cataM threads Option, short-circuiting the whole fold when a node aborts" >> {
    val alg: BinF[Int] => Option[Int] =
      case BinF.LeafF(n)      => if n < 0 then None else Some(n)
      case BinF.BranchF(l, r) => Some(l + r)
    Schemes.cataM[Option, BinF, Bin, Int](alg).get(tree) === Some(10) and
      (Schemes.cataM[Option, BinF, Bin, Int](alg).get(withNeg) === None)
  }

  // ----- paraM / histoM (the comonad-tower indices, M-lifted) -----

  "paraM at M = Id reproduces para" >> {
    val alg: BinF[(Bin, Int)] => Int =
      case BinF.LeafF(n)                  => n
      case BinF.BranchF((_, l), (_, r)) => l + r
    val viaM: Id[Int] = Schemes.paraM[Id, BinF, Bin, Int](alg).get(tree)
    viaM === Schemes.para[BinF, Bin, Int](alg).get(tree)
  }

  "histoM at M = Id reproduces histo" >> {
    val alg: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(n)      => n
      case BinF.BranchF(l, r) => l.head + r.head
    val viaM: Id[Int] = Schemes.histoM[Id, BinF, Bin, Int](alg).get(tree)
    viaM === Schemes.histo[BinF, Bin, Int](alg).get(tree)
  }

  // ----- anaM / apoM / futuM -----

  private val anaCoalg: Int => BinF[Int] =
    n => if n <= 1 then BinF.LeafF(1) else BinF.BranchF(n / 2, n - n / 2)

  "anaM at M = Id reproduces ana" >> {
    val viaM: Id[Bin] = Schemes.anaM[Id, BinF, Int, Bin](anaCoalg).reverseGet(6)
    viaM === Schemes.ana[BinF, Int, Bin](anaCoalg).reverseGet(6)
  }

  "anaM threads Option, short-circuiting the whole build when a seed aborts" >> {
    val coalg: Int => Option[BinF[Int]] =
      n => if n < 0 then None else Some(anaCoalg(n))
    Schemes.anaM[Option, BinF, Int, Bin](coalg).reverseGet(6) ===
      Some(Schemes.ana[BinF, Int, Bin](anaCoalg).reverseGet(6)) and
      (Schemes.anaM[Option, BinF, Int, Bin](_ => None).reverseGet(6) === None)
  }

  "apoM at M = Id reproduces apo (Left grafts a finished subtree)" >> {
    val coalg: Int => BinF[Either[Bin, Int]] =
      a => if a <= 0 then BinF.LeafF(0) else BinF.BranchF(Left(Bin.Leaf(99)), Right(a - 1))
    val viaM: Id[Bin] = Schemes.apoM[Id, BinF, Int, Bin](coalg).reverseGet(2)
    viaM === Schemes.apo[BinF, Int, Bin](coalg).reverseGet(2)
  }

  "futuM at M = Id reproduces futu (Roll unrolls a prebuilt layer)" >> {
    val coalg: Int => BinF[Coattr[BinF, Int]] = n =>
      if n <= 0 then BinF.LeafF(n)
      else
        BinF.BranchF(
          Coattr.Roll(BinF.BranchF(Coattr.Pure(0), Coattr.Pure(0))),
          Coattr.Pure(n - 1),
        )
    val viaM: Id[Bin] = Schemes.futuM[Id, BinF, Int, Bin](coalg).reverseGet(1)
    viaM === Schemes.futu[BinF, Int, Bin](coalg).reverseGet(1)
  }

  // ----- hyloM / chronoM (fused) -----

  "hyloM at M = Id reproduces hylo, and Option short-circuits the fused refold" >> {
    val viaM: Id[Int] = Schemes.hyloM[Id, BinF, Int, Int](anaCoalg, sumLeaves).get(6)
    val idOk = viaM === Schemes.hylo[BinF, Int, Int](anaCoalg, sumLeaves).get(6)
    val coalgOpt: Int => Option[BinF[Int]] = n => Some(anaCoalg(n))
    val algOpt: BinF[Int] => Option[Int] =
      case BinF.LeafF(n)      => if n < 0 then None else Some(n)
      case BinF.BranchF(l, r) => Some(l + r)
    idOk and (Schemes.hyloM[Option, BinF, Int, Int](coalgOpt, algOpt).get(6) must beSome)
  }

  "chronoM at M = Id reproduces chrono" >> {
    val coalg: Int => BinF[Coattr[BinF, Int]] =
      n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Coattr.Pure(n - 1), Coattr.Pure(-1))
    val alg: BinF[Attr[BinF, Int]] => Int =
      case BinF.LeafF(_)      => 0
      case BinF.BranchF(l, r) => 1 + math.max(l.head, r.head)
    val viaM: Id[Int] = Schemes.chronoM[Id, BinF, Int, Int](coalg, alg).get(8)
    viaM === Schemes.chrono[BinF, Int, Int](coalg, alg).get(8)
  }

  // ----- stack-safety of the M engine (Eval, 10^6) -----

  "cataM is stack-safe folding a 10^6-deep spine through Eval" >> {
    val deep = deepSpine(1_000_000)
    Schemes.cataM[Eval, BinF, Bin, Int](layer => Eval.now(depthAlg(layer))).get(deep).value ===
      1_000_000
  }

  "anaM is stack-safe building a 10^6-deep spine through Eval (folded back to check)" >> {
    val coalg: Int => Eval[BinF[Int]] =
      n => Eval.now(if n <= 0 then BinF.LeafF(0) else BinF.BranchF(n - 1, -1))
    val built: Bin = Schemes.anaM[Eval, BinF, Int, Bin](coalg).reverseGet(1_000_000).value
    Schemes.cata[BinF, Bin, Int](depthAlg).get(built) === 1_000_000
  }
