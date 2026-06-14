package dev.constructive.eo

import org.specs2.mutable.Specification

import data.BiAffine
import data.BiAffine.{Done, Step}
import optics.Optic

/** Behaviour checks for the [[BiAffine]] carrier worn by an optic — the graft-finality equations a
  * full `Gather`/`Scatter` citizen must satisfy, stated against a toy citizen here (the named
  * `Gather`/`Scatter` values in `cats-eo-schemes` state them per value).
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

  "Done.widenB is allocation-free (reference-equal result)" in {
    val d = new Done[TX, Int](5)
    (d.widenB[String].asInstanceOf[AnyRef] eq d.asInstanceOf[AnyRef]) === true
  }

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

  "BiAffine.assoc — the composition-matrix row" should {

    // A second citizen whose Done fires on an *even* focus, so toy.andThen(innerToy) reaches all
    // three composed arms: outer Done (w<0), Step∘Step (w≥0 odd), Step∘Done (w≥0 even).
    val innerToy: Optic[Int, Int, Int, Int, BiAffine] { type X = TX } =
      new Optic[Int, Int, Int, Int, BiAffine]:
        type X = TX
        def to(w: Int): BiAffine[X, Int] =
          if w % 2 == 0 then new Done[X, Int](w) else new Step[X, Int](List(w), w)
        def from(xb: BiAffine[X, Int]): Int = xb match
          case d: Done[X, Int] => d.fst
          case s: Step[X, Int] => s.b

    val composed = toy.andThen(innerToy)

    "biaffine.andThen(biaffine) type-checks and round-trips across all three arms" in {
      // w<0 → outer Done; w≥0 odd → Step∘Step; w≥0 even → Step∘Done.
      List(-5, 1, 3, 4, 16, 17).map(w => composed.from(composed.to(w))) ===
        List(-5, 1, 3, 4, 16, 17)
    }

    "outer Done short-circuits the composition (Left arm of Z)" in {
      composed.from(composed.to(-9)) === -9
    }
  }
