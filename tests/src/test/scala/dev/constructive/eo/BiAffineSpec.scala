package dev.constructive.eo

import org.specs2.mutable.Specification

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic

/** Behaviour checks for the [[BiAffine]] carrier worn by an optic — the graft-finality equations a
  * full `Decor` citizen must satisfy, stated against a toy citizen here (the named `Decor` values
  * in `cats-eo-schemes` state them per value).
  *
  * The toy citizen pins the existential the way every concrete decoration does: `X = (W, F[W])`
  * with `Fst[X] = W` (the `Done` payload is a finished result) and `Snd[X] = F[W]` (the one-F-layer
  * leftover context carried by `Step`).
  */
class BiAffineSpec extends Specification:

  private type TX = (Int, List[Int])

  // Toy full citizen: W = Int, F = List. Negative values are "already finished"
  // (Done); non-negative ones keep going, carrying one layer of context.
  private val toy: Optic[Int, Int, Int, Int, BiAffine] { type X = TX } =
    new Optic[Int, Int, Int, Int, BiAffine]:
      type X = TX
      def to(w: Int): BiAffine[X, Int] =
        if w < 0 then new Done[X, Int](w)
        else new Step[X, Int](List(w), w)
      def from(xb: BiAffine[X, Int]): Int = xb match
        case d: Done[X, Int] => d.fst
        case s: Step[X, Int] => s.b

  "a full BiAffine citizen" should {

    "treat Done as final: from(Done(w)) == w" in {
      (toy.from(new Done[TX, Int](-7)) === -7).and(toy.from(new Done[TX, Int](42)) === 42)
    }

    "round-trip the Step arm: from(to(w)) == w" in {
      List(0, 1, 17, 4096).map(w => toy.from(toy.to(w))) === List(0, 1, 17, 4096)
    }

    "round-trip the Done arm: from(to(w)) == w on finished inputs" in {
      List(-1, -100).map(w => toy.from(toy.to(w))) === List(-1, -100)
    }
  }
