package dev.constructive.eo

import org.specs2.mutable.Specification

import data.Affine
import data.Affine.{Hit, Miss}
import optics.Optic

/** Behaviour checks for [[Affine]] worn on its **build seam** — the graft-finality equations the
  * recursion-scheme zoo's decoration citizens must satisfy, stated against a toy citizen here (the
  * named `apo`/`futu` decorations in `cats-eo-schemes` state them per value).
  *
  * The toy citizen pins the existential the way every concrete decoration does: `X = (W, F[W])`
  * with `Fst[X] = W` (the `Miss` payload is a finished result — `Graft.done`) and `Snd[X] = F[W]`
  * (the one-layer leftover context carried by `Hit` — `Graft.step`).
  */
class AffineBuildSeamSpec extends Specification:

  private type TX = (Int, List[Int])

  // Toy full citizen: W = Int, F = List. Negative values are "already finished"
  // (Miss/done); non-negative ones keep going, carrying one layer of context (Hit/step).
  private val toy: Optic[Int, Int, Int, Int, Affine] { type X = TX } =
    new Optic[Int, Int, Int, Int, Affine]:
      type X = TX
      def to(w: Int): Affine[X, Int] =
        if w < 0 then new Miss[X, Int](w)
        else new Hit[X, Int](List(w), w)
      def from(xb: Affine[X, Int]): Int = xb match
        case d: Miss[X, Int] => d.fst
        case s: Hit[X, Int]  => s.b

  "Miss.widenB is allocation-free (reference-equal result)" in {
    val d = new Miss[TX, Int](5)
    (d.widenB[String].asInstanceOf[AnyRef] eq d.asInstanceOf[AnyRef]) === true
  }

  "a full Affine build-seam citizen" should {

    "treat Miss as final: from(Miss(w)) == w" in {
      (toy.from(new Miss[TX, Int](-7)) === -7).and(toy.from(new Miss[TX, Int](42)) === 42)
    }

    "round-trip the Hit arm: from(to(w)) == w" in {
      List(0, 1, 17, 4096).map(w => toy.from(toy.to(w))) === List(0, 1, 17, 4096)
    }

    "round-trip the Miss arm: from(to(w)) == w on finished inputs" in {
      List(-1, -100).map(w => toy.from(toy.to(w))) === List(-1, -100)
    }
  }

  "Affine.assoc — the composition-matrix row, exercised through the build seam" should {

    // A second citizen whose Miss fires on an *even* focus, so toy.andThen(innerToy) reaches all
    // three composed arms: outer Miss (w<0), Hit∘Hit (w≥0 odd), Hit∘Miss (w≥0 even).
    val innerToy: Optic[Int, Int, Int, Int, Affine] { type X = TX } =
      new Optic[Int, Int, Int, Int, Affine]:
        type X = TX
        def to(w: Int): Affine[X, Int] =
          if w % 2 == 0 then new Miss[X, Int](w) else new Hit[X, Int](List(w), w)
        def from(xb: Affine[X, Int]): Int = xb match
          case d: Miss[X, Int] => d.fst
          case s: Hit[X, Int]  => s.b

    val composed = toy.andThen(innerToy)

    "affine.andThen(affine) type-checks and round-trips across all three arms" in {
      // w<0 → outer Miss; w≥0 odd → Hit∘Hit; w≥0 even → Hit∘Miss.
      List(-5, 1, 3, 4, 16, 17).map(w => composed.from(composed.to(w))) ===
        List(-5, 1, 3, 4, 16, 17)
    }

    "outer Miss short-circuits the composition (Left arm of Z)" in {
      composed.from(composed.to(-9)) === -9
    }
  }

  "Affine cross-carrier bridges" should {

    "Composer[Tuple2, Affine] lifts a Lens-shaped optic to always-Hit, round-tripping" in {
      val tupleOptic: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
        new Optic[(Int, String), (Int, String), Int, Int, Tuple2]:
          type X = String
          def to(s: (Int, String)): (X, Int) = (s._2, s._1)
          def from(p: (X, Int)): (Int, String) = (p._2, p._1)
      val af = tupleOptic.morph[Affine]
      af.from(af.to((7, "x"))) === ((7, "x"))
    }

    "Composer[Either, Affine] maps Right→Hit and Left→Miss, round-tripping both arms" in {
      val eitherOptic: Optic[Option[Int], Option[Int], Int, Int, Either] =
        new Optic[Option[Int], Option[Int], Int, Int, Either]:
          type X = Unit
          def to(s: Option[Int]): Either[X, Int] = s match
            case Some(n) => Right(n)
            case None    => Left(())
          def from(xb: Either[X, Int]): Option[Int] = xb match
            case Right(n) => Some(n)
            case Left(_)  => None
      val af = eitherOptic.morph[Affine]
      (af.from(af.to(Some(5))) === Some(5)).and(af.from(af.to(None)) === None)
    }
  }
