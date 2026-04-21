package eo

import optics.{
  BijectionIso,
  Fold,
  Getter,
  Iso,
  Lens,
  MendTearPrism,
  Optic,
  Optional,
  Prism,
  ReversedLens,
  ReversedPrism,
  Review,
  Traversal,
}
import optics.Optic.*
import data.{Affine, Forget, Forgetful}
import data.Forgetful.given
import data.Affine.given

import cats.instances.int.given
import cats.instances.list.given
import cats.instances.option.given

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Non-law behavioural coverage for EO's optics: exercises the extension methods (`andThen`,
  * `reverse`, `foldMap`, `modifyA`, `morph`), the Lens/Prism/Traversal alternative constructors,
  * and `Getter`, all of which are never reached from the ported Monocle laws but still belong in
  * any honest description of what the library does.
  */
class OpticsBehaviorSpec extends Specification with ScalaCheck:

  // ----- Getter.apply ----------------------------------------------

  "Getter.apply reads the focused value" >> {
    val g = Getter[(Int, String), Int](_._1)
    forAll((s: (Int, String)) => g.get(s) == s._1)
  }

  // ----- Lens alternative constructors -----------------------------

  "Lens.first exposes the first tuple component" >> {
    val l = Lens.first[Int, String]
    l.get((1, "a")) === 1
    l.replace(9)((1, "a")) === (9, "a")
  }

  "Lens.second exposes the second tuple component" >> {
    val l = Lens.second[Int, String]
    l.get((1, "a")) === "a"
    l.replace("z")((1, "a")) === (1, "z")
  }

  "Lens.curried is equivalent to Lens.apply" >> {
    val curried =
      Lens.curried[(Int, Int), Int](_._1, a => s => (a, s._2))
    val applied =
      Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))
    forAll((s: (Int, Int), a: Int) =>
      curried.get(s) == applied.get(s) &&
        curried.replace(a)(s) == applied.replace(a)(s)
    )
  }

  // ----- Lens composition via Optic.andThen ------------------------
  //
  // Exercises AssociativeFunctor[Tuple2, X, Y] through andThen.

  "Lens composed with Lens reaches nested pair component" >> {
    type S = ((Int, Int), Int)
    val outer: Optic[S, S, (Int, Int), (Int, Int), Tuple2] =
      Lens[S, (Int, Int)](_._1, (s, a) => (a, s._2))
    val inner: Optic[(Int, Int), (Int, Int), Int, Int, Tuple2] =
      Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))
    val nested = outer.andThen(inner)
    nested.get(((1, 2), 3)) === 1
    nested.replace(9)(((1, 2), 3)) === ((9, 2), 3)
    nested.modify(_ + 100)(((1, 2), 3)) === ((101, 2), 3)
  }

  // ----- Iso.reverse -----------------------------------------------

  "Iso.reverse swaps the get / reverseGet directions" >> {
    val longIso: Optic[Int, Int, Long, Long, Forgetful] =
      Iso[Int, Int, Long, Long](_.toLong, _.toInt)
    val rev = longIso.reverse
    forAll((n: Int) =>
      // reverse.get undoes longIso.get within Int's range
      rev.get(n.toLong) == n
    )
  }

  // ----- Prism alternative constructors ----------------------------

  "Prism.optional builds a Prism from a partial projection" >> {
    val posIntPrism = Prism.optional[Int, Int](
      n => if n >= 0 then Some(n) else None,
      identity,
    )
    posIntPrism.to(5) === Right(5)
    posIntPrism.to(-1) === Left(-1)
    posIntPrism.reverseGet(7) === 7
  }

  // ----- foldMap exercised on Traversal.forEach --------------------
  //
  // Hits ForgetfulFold.foldFFold[List] and Optic.foldMap.

  "Traversal.forEach.foldMap totals the list under Monoid[Int]" >> {
    val t: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
      Traversal.forEach[List, Int, Int]
    forAll((xs: List[Int]) => t.foldMap(identity[Int])(xs) == xs.sum)
  }

  // ----- modifyA with the Option applicative -----------------------
  //
  // Hits ForgetfulTraverse.traverse2[List] for Forget[List] and
  // demonstrates that failure short-circuits the whole traversal.

  "Traversal.forEach.modifyA short-circuits on None" >> {
    val t: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
      Traversal.forEach[List, Int, Int]
    val positivesDoubled: Int => Option[Int] =
      a => if a > 0 then Some(a * 2) else None

    t.modifyA[Option](positivesDoubled)(List(1, 2, 3)) === Some(List(2, 4, 6))
    t.modifyA[Option](positivesDoubled)(List(1, -2, 3)) === None
    t.modifyA[Option](positivesDoubled)(Nil) === Some(Nil)
  }

  // ----- .morph coerces a Tuple2 carrier (Lens) to an Affine --------
  //
  // Hits Composer.tuple2affine in data.Affine.

  "Lens morphed into an Affine behaves like the original Lens" >> {
    val l: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens.first[Int, String]
    val asAffine: Optic[(Int, String), (Int, String), Int, Int, Affine] =
      l.morph[Affine]

    forAll((pair: (Int, String), a: Int) => asAffine.modify(_ => a)(pair) == l.replace(a)(pair))
  }

  // ----- Fold.apply for a Foldable ---------------------------------

  "Fold.apply[List] folds all elements" >> {
    val f: Optic[List[Int], Unit, Int, Int, Forget[List]] =
      Fold[List, Int]
    forAll((xs: List[Int]) => f.foldMap(identity[Int])(xs) == xs.sum)
  }

  // ----- Optional.foldMap exercises the Affine ForgetfulFold --------
  //
  // ForgetfulFold.affineFFold was cold — no test called foldMap on an
  // Optional. A partial Optional (focus is the first Int only when it's
  // even) hits both branches of the Affine foldMap: Right yields f(a),
  // Left yields Monoid[M].empty.

  "Optional.foldMap returns the focus when matched, empty otherwise" >> {
    val evenFstOpt: Optic[(Int, Int), (Int, Int), Int, Int, Affine] =
      Optional[(Int, Int), (Int, Int), Int, Int, Tuple2](
        { case (a, b) => if a % 2 == 0 then Right(a) else Left((a, b)) },
        { case ((_, b), newA) => (newA, b) },
      )
    forAll((a: Int, b: Int) =>
      evenFstOpt.foldMap(identity[Int])((a, b)) ==
        (if a % 2 == 0 then a else 0)
    )
  }

  // ----- Fold.select predicate semantics ---------------------------
  //
  // Pins the `Option(_).filter(p)` in Fold.select so stryker detects a
  // mutation to `filterNot` here (the whole project has exactly one
  // stryker-reachable runtime expression, and this is it).

  "Fold.select keeps values matching the predicate, drops the rest" >> {
    val evenFold: Optic[Int, Unit, Int, Int, Forget[Option]] =
      Fold.select[Int](_ % 2 == 0)
    forAll((n: Int) => evenFold.to(n) == (if n % 2 == 0 then Some(n) else None))
  }

  // ----- Composer morphs from Forgetful -----------------------------
  //
  // Iso's carrier is Forgetful; the three `morph` targets exercise
  // `forgetful2tuple`, `forgetful2either`, and `chain` (Composer.scala),
  // which are otherwise unreachable from the ported laws.

  val doubleIso: Optic[Int, Int, Int, Int, Forgetful] =
    Iso[Int, Int, Int, Int](_ * 2, _ / 2)

  "Iso.morph[Tuple2] behaves like a Lens" >> {
    val asLens = doubleIso.morph[Tuple2]
    forAll((n: Int) => asLens.get(n) == n * 2)
  }

  "Iso.morph[Either] behaves like a Prism" >> {
    val asPrism = doubleIso.morph[Either]
    // Forgetful → Either always sends to Right
    asPrism.to(10) === Right(20)
    asPrism.reverseGet(20) === 10
  }

  // Chaining morphs (Forgetful -> Tuple2 -> Affine) exercises
  // Composer.chain for its middle step.
  "Iso.morph[Tuple2].morph[Affine] is a valid Affine-shaped optic" >> {
    val asAffine = doubleIso.morph[Tuple2].morph[Affine]
    forAll((n: Int) => asAffine.to(n).affine.toOption.map(_._2) == Some(n * 2))
  }

  // ----- Cross-carrier andThen via Morph ------------------------------
  //
  // Pins the three Morph instances that rewire `.andThen` across
  // differing carriers. `leftToRight` and `rightToLeft` are covered
  // indirectly through the Lens→Optional and Lens→Traversal paths
  // elsewhere; this block adds explicit coverage plus a regression
  // for `bothViaAffine`, the low-priority fallback that lifts two
  // optics with no direct bridge (e.g. `Either` + `Tuple2`) into a
  // shared `Affine`.

  private enum Shape3:
    case Tri(side: Int)
    case Sq(edge: Int)

  private val triP: Optic[Shape3, Shape3, Shape3.Tri, Shape3.Tri, Either] =
    Prism[Shape3, Shape3.Tri](
      {
        case t: Shape3.Tri => Right(t)
        case other         => Left(other)
      },
      identity,
    )

  private val triSideL: Optic[Shape3.Tri, Shape3.Tri, Int, Int, Tuple2] =
    Lens[Shape3.Tri, Int](_.side, (t, s) => t.copy(side = s))

  "Prism.andThen(Lens) composes via Morph.bothViaAffine" >> {
    // Carrier pair (Either, Tuple2) has no direct Composer either way;
    // bothViaAffine picks Affine as the common target.
    val triSide: Optic[Shape3, Shape3, Int, Int, Affine] =
      triP.andThen(triSideL)

    triSide.modify(_ + 10)(Shape3.Tri(3)) === Shape3.Tri(13)
    triSide.modify(_ + 10)(Shape3.Sq(5)) === Shape3.Sq(5)
  }

  "Lens.andThen(Prism) composes via Morph.bothViaAffine (symmetric)" >> {
    case class Wrapper(shape: Shape3)
    val wrapperShape =
      Lens[Wrapper, Shape3](_.shape, (w, s) => w.copy(shape = s))
    val wrappedTri: Optic[Wrapper, Wrapper, Shape3.Tri, Shape3.Tri, Affine] =
      wrapperShape.andThen(triP)

    wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Tri(3))) ===
      Wrapper(Shape3.Tri(4))
    wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Sq(5))) ===
      Wrapper(Shape3.Sq(5))
  }

  // ----- Optional.readOnly behaviour --------------------------------
  //
  // Read-only Optional constructed with no write-back — T = Unit so
  // .modify / .replace are statically forbidden, only .foldMap and
  // direct .to / PowerSeries-style access remain. Exercises the
  // Affine carrier's ForgetfulFold path for the hit/miss branches.

  case class AdultPerson(age: Int)

  val adultAge: Optic[AdultPerson, Unit, Int, Int, Affine] =
    Optional.readOnly(p => Option.when(p.age >= 18)(p.age))

  "Optional.readOnly.foldMap folds the hit branch, returns empty on miss" >> {
    import cats.instances.int.given
    adultAge.foldMap(identity[Int])(AdultPerson(20)) === 20
    adultAge.foldMap(identity[Int])(AdultPerson(15)) === 0
  }

  "Optional.selectReadOnly keeps values matching the predicate" >> {
    import cats.instances.int.given
    val evenAF = Optional.selectReadOnly[Int](_ % 2 == 0)
    evenAF.foldMap(identity[Int])(4) === 4
    evenAF.foldMap(identity[Int])(3) === 0
    evenAF.foldMap(identity[Int])(0) === 0
  }

  "Optional.readOnly.modifyA lifts a hit-branch read under Applicative[G]" >> {
    import cats.instances.option.given
    val mf: Int => Option[Int] = n => Option.when(n > 0)(n * 10)
    adultAge.modifyA[Option](mf)(AdultPerson(20)) === Some(())
    adultAge.modifyA[Option](mf)(AdultPerson(15)) === Some(()) // miss → pure(empty)
  }

  // ----- Review behaviour ------------------------------------------

  "Review.apply wraps an A => S build function" >> {
    val toSome = Review[Option[Int], Int](Some(_))
    toSome.reverseGet(3) === Some(3)
    toSome.reverseGet(-1) === Some(-1)
  }

  "Reviews compose via direct function composition" >> {
    val toSome = Review[Option[Int], Int](Some(_))
    val stringify = Review[Int, String](_.length)
    val composed =
      Review[Option[Int], String](s => toSome.reverseGet(stringify.reverseGet(s)))
    composed.reverseGet("hello") === Some(5)
  }

  "Review.fromIso extracts the reverseGet side of a BijectionIso" >> {
    val doubleIsoConcrete: BijectionIso[Int, Int, Int, Int] =
      BijectionIso[Int, Int, Int, Int](_ * 2, _ / 2)
    val rev = Review.fromIso(doubleIsoConcrete)
    rev.reverseGet(10) === 5
  }

  "Review.fromPrism extracts the mend side of a MendTearPrism" >> {
    val somePrism = new MendTearPrism[Option[Int], Option[Int], Int, Int](
      tear = {
        case Some(n) => Right(n)
        case None    => Left(None)
      },
      mend = Some(_),
    )
    val rev = Review.fromPrism(somePrism)
    rev.reverseGet(42) === Some(42)
  }

  "ReversedLens(iso) and ReversedPrism(prism) alias the Review factories" >> {
    val doubleIsoConcrete: BijectionIso[Int, Int, Int, Int] =
      BijectionIso[Int, Int, Int, Int](_ * 2, _ / 2)
    val fromIso = ReversedLens(doubleIsoConcrete)
    fromIso.reverseGet(10) === 5

    val somePrism = new MendTearPrism[Option[Int], Option[Int], Int, Int](
      tear = {
        case Some(n) => Right(n)
        case None    => Left(None)
      },
      mend = Some(_),
    )
    val fromPrism = ReversedPrism(somePrism)
    fromPrism.reverseGet(7) === Some(7)
  }
