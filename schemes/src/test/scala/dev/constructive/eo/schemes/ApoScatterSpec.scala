package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import data.BiAffine
import optics.Optic
import optics.Optic.* // reverseGet

import schemes.samples.{Bin, BinF}

/** apo re-carriered onto [[data.BiAffine]]: its per-slot residual is now [[Schemes.apoScatter]], a
  * composable `BiAffine`-carried scatter optic (`Left → Done`, the O(1) graft; `Right → Step`, keep
  * unfolding), and apo's engine constructs + consumes that decision through it. Pins the scatter's
  * `Done`/`Step` semantics, that it composes via `BiAffine.assoc`, and that the scheme still builds
  * (graft intact) after the re-carriering. */
class ApoScatterSpec extends Specification:

  private val sc = Schemes.apoScatter[Bin, Int]

  "apoScatter maps Left → Done (carrying the grafted subtree)" >> {
    sc.to(Left(Bin.Leaf(7))).fold(s => s, (_, _) => Bin.Leaf(-1)) === Bin.Leaf(7)
  }

  "apoScatter maps Right → Step (carrying the keep-going focus)" >> {
    sc.to(Right(9)).fold(_ => -1, (_, b) => b) === 9
  }

  // A second BiAffine optic on the focus Int — Done on negatives — to compose under apoScatter.
  private val innerToy: Optic[Int, Unit, Int, Unit, BiAffine] { type X = (Int, Unit) } =
    new Optic[Int, Unit, Int, Unit, BiAffine]:
      type X = (Int, Unit)
      def to(n: Int): BiAffine[X, Int] =
        if n < 0 then new BiAffine.Done[X, Int](n) else new BiAffine.Step[X, Int]((), n)
      def from(b: BiAffine[X, Unit]): Unit = ()

  private val composed = sc.andThen(innerToy)

  "apoScatter composes via BiAffine.assoc — Step∘Step threads the focus" >> {
    composed.to(Right(5)).fold(_ => -1, (_, b) => b) === 5
  }

  "apoScatter composes — outer Done (graft) short-circuits the composition" >> {
    composed.to(Left(Bin.Leaf(0))).fold(_ => -1, (_, b) => b) === -1
  }

  "apoScatter composes — inner Done short-circuits the keep-going arm" >> {
    composed.to(Right(-3)).fold(_ => -1, (_, b) => b) === -1
  }

  "the re-carriered apo still builds: Done grafts, Step unfolds" >> {
    val coalg: Int => BinF[Either[Bin, Int]] =
      n => if n <= 0 then BinF.LeafF(0) else BinF.BranchF(Left(Bin.Leaf(99)), Right(n - 1))
    Schemes.apo[BinF, Int, Bin](coalg).reverseGet(2) ===
      Bin.Branch(Bin.Leaf(99), Bin.Branch(Bin.Leaf(99), Bin.Leaf(0)))
  }
