package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import data.Affine
import optics.Optic
import optics.Optic.* // reverseGet
import schemes.samples.{Bin, BinF}

/** apo re-carriered onto [[data.Affine]]: its per-slot residual is now [[Schemes.apoScatter]], a
  * composable `Affine`-carried scatter optic (`Left → Miss`, the O(1) graft; `Right → Hit`, keep
  * unfolding), and apo's engine constructs + consumes that decision through it. Pins the scatter's
  * `Miss`/`Hit` semantics, that it composes via `Affine.assoc`, and that the scheme still builds
  * (graft intact) after the re-carriering.
  */
class ApoScatterSpec extends Specification:

  private val sc = Schemes.apoScatter[Bin, Int]

  "apoScatter maps Left → Miss (carrying the grafted subtree)" >> {
    sc.to(Left(Bin.Leaf(7))).fold(s => s, (_, _) => Bin.Leaf(-1)) === Bin.Leaf(7)
  }

  "apoScatter maps Right → Hit (carrying the keep-going focus)" >> {
    sc.to(Right(9)).fold(_ => -1, (_, b) => b) === 9
  }

  // A second Affine optic on the focus Int — Miss on negatives — to compose under apoScatter.
  private val innerToy: Optic[Int, Unit, Int, Unit, Affine] { type X = (Int, Unit) } =
    new Optic[Int, Unit, Int, Unit, Affine]:
      type X = (Int, Unit)
      def to(n: Int): Affine[X, Int] =
        if n < 0 then new Affine.Miss[X](n) else new Affine.Hit[X, Int]((), n)
      def from(b: Affine[X, Unit]): Unit = ()

  private val composed = sc.andThen(innerToy)

  "apoScatter composes via Affine.assoc — Hit∘Hit threads the focus" >> {
    composed.to(Right(5)).fold(_ => -1, (_, b) => b) === 5
  }

  "apoScatter composes — outer Miss (graft) short-circuits the composition" >> {
    composed.to(Left(Bin.Leaf(0))).fold(_ => -1, (_, b) => b) === -1
  }

  "apoScatter composes — inner Miss short-circuits the keep-going arm" >> {
    composed.to(Right(-3)).fold(_ => -1, (_, b) => b) === -1
  }

  "the re-carriered apo still builds: Miss grafts, Hit unfolds" >> {
    val coalg: Int => BinF[Either[Bin, Int]] =
      n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Left(Bin.Leaf(99)), Right(n - 1))
    Schemes.apo[BinF, Int, Bin](coalg).reverseGet(2) ===
      Bin.Branch(Bin.Leaf(99), Bin.Branch(Bin.Leaf(99), Bin.Leaf(0)))
  }
