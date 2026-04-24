package eo

import optics.{
  AffineFold,
  BijectionIso,
  Fold,
  GetReplaceLens,
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
import data.{Affine, AlgLens, AlgLensSingleton, Forget, Forgetful, PowerSeries}
import data.Forgetful.given
import data.Forget.given
import data.Affine.given
import data.AlgLens.given

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

  /** Hand-rolled `Forget[F]`-carrier optic on `Int`, parameterised by the classifier shape `F` so
    * the AlgLens / Forget-composition specs below can build `Forget[List]` / `Forget[Option]` /
    * `Forget[Either[E, *]]` classifiers without re-typing the carrier boilerplate per call site.
    */
  private def forgetOpt[F[_]](
      toF: Int => F[Int],
      fromF: F[Int] => Int,
  ): Optic[Int, Int, Int, Int, Forget[F]] =
    new Optic[Int, Int, Int, Int, Forget[F]]:
      type X = Unit
      val to = toF
      val from = fromF

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
    val mf: Int => Option[Int] = n => Option.when(n > 0)(n * 10)
    adultAge.modifyA[Option](mf)(AdultPerson(20)) === Some(())
    adultAge.modifyA[Option](mf)(AdultPerson(15)) === Some(()) // miss → pure(empty)
  }

  // ----- AffineFold behaviour --------------------------------------
  //
  // AffineFold is the user-facing name for the read-only 0-or-1 focus
  // shape. The constructor is specialised to X = (Unit, Unit) so the
  // Hit branch doesn't carry the unused S; the observable contract is
  // still .getOption / .foldMap / .modifyA.

  val adultAgeAF: AffineFold[AdultPerson, Int] =
    AffineFold(p => Option.when(p.age >= 18)(p.age))

  "AffineFold.apply hits on Some and misses on None" >> {
    adultAgeAF.getOption(AdultPerson(20)) === Some(20)
    adultAgeAF.getOption(AdultPerson(15)) === None
  }

  "AffineFold.select keeps values matching the predicate" >> {
    val evenAF = AffineFold.select[Int](_ % 2 == 0)
    evenAF.getOption(4) === Some(4)
    evenAF.getOption(3) === None
  }

  "AffineFold.fromOptional drops write path, preserves getOption" >> {
    val ageOpt: Optional[AdultPerson, AdultPerson, Int, Int] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (p, a) => AdultPerson(a) },
      )
    val af = AffineFold.fromOptional(ageOpt)
    af.getOption(AdultPerson(20)) === Some(20)
    af.getOption(AdultPerson(15)) === None
  }

  "AffineFold.fromPrism drops build path, preserves getOption" >> {
    val intPrism = Prism.optional[String, Int](s => s.toIntOption, _.toString)
    val af = AffineFold.fromPrism(intPrism)
    af.getOption("42") === Some(42)
    af.getOption("nope") === None
  }

  // Direct Lens.andThen(AffineFold) is not well-typed — AffineFold's
  // T = Unit mismatches the outer Lens's B slot in the Optic composition
  // law. The intended route to a composed read-only focus is to build a
  // full Optional through the Lens chain and narrow the result via
  // AffineFold.fromOptional.
  "AffineFold.fromOptional narrows a Lens-composed Optional" >> {
    case class Wrapper(inner: AdultPerson)
    val wrapAge = Optional[Wrapper, Wrapper, Int, Int, Affine](
      getOrModify = w => Either.cond(w.inner.age >= 18, w.inner.age, w),
      reverseGet = { case (w, a) => w.copy(inner = AdultPerson(a)) },
    )
    val chained = AffineFold.fromOptional(wrapAge)
    chained.getOption(Wrapper(AdultPerson(20))) === Some(20)
    chained.getOption(Wrapper(AdultPerson(15))) === None
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

  // ----- Forget[F] `.andThen` via assocForgetMonad -----------------
  //
  // Two Forget[Option]-carrier optics (classifier-style: S -> Option[A],
  // Option[B] -> T) compose end-to-end under algebraic-lens composition:
  // push side flatMaps through inner.to, pull side lifts the inner's
  // collapse back via pure before handing to outer.from.

  "Forget[Option] optics compose via `.andThen` (algebraic-lens shape under Monad[Option])" >> {
    // `from: Option[Int] => Int` uses `.fold(default)(onHit)` — default on miss,
    // on-Some otherwise — to produce a sentinel-free Int the spec can assert.
    val outer = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-100)(_ * 2 + 1000),
    )
    val inner = forgetOpt[Option](
      n => Option.when(n < 1000)(n + 1),
      _.fold(-1)(_ + 1),
    )

    val composed = outer.andThen(inner)

    // Push — outer.to(5)=Some(50), flatMap(inner.to)=Some(51).
    composed.to(5) === Some(51)
    // Outer miss bubbles through flatMap.
    composed.to(-2) === None
    // Inner miss after outer hit: outer.to(200)=Some(2000), 2000 fails inner.
    composed.to(200) === None

    // Pull: outer.from(pure(inner.from(Some(7)))).
    //   inner.from(Some(7))=8, pure(8)=Some(8), outer.from(Some(8))=1016.
    composed.from(Some(7)) === 1016
    // Miss-pull: inner.from(None)=-1, pure(-1)=Some(-1), outer.from(Some(-1))=998.
    composed.from(None) === 998
  }

  // ----- AlgLens[F] composition + Forget→AlgLens injection ---------
  //
  // A Forget[Option]-carrier classifier lifts into AlgLens[Option] via the
  // trivial X=Unit injection, then composes with another AlgLens[Option]
  // optic under Monad[Option]. The Z = (Xo, F[Xi]) shape threads each
  // inner-leftover alongside the outer's.

  "Forget[Option] injects into AlgLens[Option] and composes under Monad[Option]" >> {
    val pureForget = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-1)(_ + 7),
    )

    // Inject into AlgLens[Option] via the Forget→AlgLens composer. The
    // resulting optic is verified structurally by composing against a
    // second AlgLens[Option] optic and observing end-to-end semantics.
    val lifted = summon[Composer[Forget[Option], AlgLens[Option]]].to(pureForget)

    // `.modify` exercises the ForgetfulFunctor[AlgLens[Option]] path end
    // to end: outer.to(5)=Some(50), map f through, then outer.from with
    // the rewritten fb. For fb=Some(51), pureForget.from(Some(51)) = 58.
    lifted.modify(_ + 1)(5) === 58
    // Outer-miss case: to produces None; map f is still None; from sees
    // None and returns the forget's miss sentinel (-1).
    lifted.modify(_ + 1)(-2) === -1

    // Compose the lifted outer with a non-trivial inner AlgLens[Option]
    // optic (X = String). End-to-end behaviour proves both the Z = (Xo,
    // F[Xi]) threading and the Forget→AlgLens injection.
    val inner: Optic[Int, Int, Int, Int, AlgLens[Option]] =
      new Optic[Int, Int, Int, Int, AlgLens[Option]]:
        type X = String
        val to: Int => (String, Option[Int]) = n => (s"tag-$n", Option.when(n < 1000)(n + 1))
        val from: ((String, Option[Int])) => Int = {
          case (tag, fb) =>
            fb.fold(tag.length)(_ * 100 + tag.length)
        }

    val composed = lifted.andThen(inner)

    // End-to-end modify: lifted.to(5) = Some(50), inner.to(50) =
    // ("tag-50", Some(51)); flattened fc = Some(51), mapped by f=_+0 is
    // still Some(51). Rebuild: inner.from("tag-50", Some(51)) = 51*100+6
    // = 5106. Then outer (pureForget) .from(Some(5106)) = 5113.
    composed.modify(identity)(5) === 5113

    // Outer-miss: lifted.to(-2) = None, fxi = None, fc = None. Rebuild:
    // outer.from(None) = -1 directly.
    composed.modify(identity)(-2) === -1
  }

  // ----- Lens → AlgLens[F] bridge ----------------------------------
  //
  // A Tuple2-carrier Lens lifts into AlgLens[F] via the `Applicative[F]
  // + Foldable[F]` composer: to wraps the focus in `F.pure`, from
  // collapses the classifier to its last B via `Foldable`. End-to-end
  // `.modify` should behave identically to the original Lens (since the
  // singleton classifier round-trips trivially).

  "Tuple2 Lens lifts into AlgLens[List] and preserves .modify semantics" >> {
    val fstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))

    val lifted = summon[Composer[Tuple2, AlgLens[List]]].to(fstLens)

    lifted.modify(_ + 100)((3, "hi")) === ((103, "hi"))
    lifted.modify(_ * 2)((5, "x")) === ((10, "x"))
  }

  "Either Prism lifts into AlgLens[List] and preserves hit/miss .modify semantics" >> {
    val oddP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 1 then Right(n) else Left(n), identity)

    val lifted = summon[Composer[Either, AlgLens[List]]].to(oddP)

    // Hit: 3 is odd → classifier is List(3), modify doubles to List(6),
    //      from(Right(6)) = 6.
    lifted.modify(_ * 2)(3) === 6
    // Miss: 4 is even → classifier is Nil, fold yields Left-tag path,
    //      from(Left(4)) = 4.
    lifted.modify(_ * 2)(4) === 4
  }

  // ----- Cross-carrier composition Lens andThen AlgLens classifier -
  //
  // This is the headline use case: a plain Lens composed with an
  // AlgLens[F]-carrier classifier via `.andThen`. The implicit Morph
  // picks up `Composer[Tuple2, AlgLens[F]]` to lift the Lens, then
  // `AssociativeFunctor[AlgLens[F], _, _]` under Monad[F] does the
  // joint composition.

  "Lens andThen AlgLens[List] classifier composes end-to-end" >> {
    case class Row(id: Int, value: Int)
    val valueLens: Optic[Row, Row, Int, Int, Tuple2] =
      Lens[Row, Int](_.value, (r, v) => r.copy(value = v))

    // A toy "enumerate candidates" classifier on Int expressed as a
    // Forget[List]-carrier optic, then injected into AlgLens[List].
    val candidatesForget = forgetOpt[List](n => List(n, n + 10), _.sum)

    val candidatesAlg = summon[Composer[Forget[List], AlgLens[List]]].to(candidatesForget)

    // Cross-carrier composition: `.andThen` sees differing carriers
    // (Tuple2 vs AlgLens[List]) and summons Morph, which picks the
    // Tuple2 → AlgLens bridge.
    val composed = valueLens.andThen(candidatesAlg)

    // `.modify` multiplies each candidate by 3: classifier emits
    // List(5, 15) for value=5, the map produces List(15, 45), sum = 60.
    // Then back through the Lens: Row(1, 60).
    composed.modify(_ * 3)(Row(1, 5)) === Row(1, 60)

    // Identity mod: List(5, 15) summed = 20 back through the Lens.
    composed.modify(identity)(Row(9, 5)) === Row(9, 20)
  }

  // ----- F[A]-focus factories --------------------------------------
  //
  // Complementary to the Composer bridges: when the outer optic's focus
  // is already `F[A]`, the adapter is a pure rewrap — no Foldable
  // needed (and for Tuple2 no Applicative either) because we don't
  // synthesise an F or collapse one.

  "AlgLens.fromLensF rewraps a Lens[S, F[A]] with zero constraints" >> {
    case class Table(rows: List[Int])
    val rowsLens: Optic[Table, Table, List[Int], List[Int], Tuple2] =
      Lens[Table, List[Int]](_.rows, (t, rs) => t.copy(rows = rs))

    val listAlg: Optic[Table, Table, Int, Int, AlgLens[List]] =
      AlgLens.fromLensF(rowsLens)

    // Focus is the inner Int; .modify walks the list.
    listAlg.modify(_ + 1)(Table(List(1, 2, 3))) === Table(List(2, 3, 4))
    // Empty list: map preserves emptiness; lens rebuilds Table with [].
    listAlg.modify(_ * 10)(Table(Nil)) === Table(Nil)
  }

  "AlgLens.fromPrismF lifts a Prism[S, F[A]] with Alternative[F] only" >> {
    // A Prism on Option[List[Int]] that fires on Some(nonEmpty).
    val p: Optic[Option[List[Int]], Option[List[Int]], List[Int], List[Int], Either] =
      Prism[Option[List[Int]], List[Int]](
        {
          case Some(xs) if xs.nonEmpty => Right(xs)
          case other                   => Left(other)
        },
        Some(_),
      )

    val alg: Optic[Option[List[Int]], Option[List[Int]], Int, Int, AlgLens[List]] =
      AlgLens.fromPrismF(p)

    // Hit: list is passed through, .modify maps each element.
    alg.modify(_ * 2)(Some(List(1, 2, 3))) === Some(List(2, 4, 6))
    // Miss (None): Miss branch preserves the source.
    alg.modify(_ * 2)(None) === None
    // Miss (Some(Nil)): Miss branch returns source unchanged.
    alg.modify(_ * 2)(Some(Nil)) === Some(Nil)
  }

  "AlgLens.fromOptionalF lifts an Optional[S, F[A]] through the Hit branch" >> {
    // An Optional on (Int, List[Int]) that fires only on non-empty lists.
    val opt: Optic[
      (Int, List[Int]),
      (Int, List[Int]),
      List[Int],
      List[Int],
      Affine,
    ] =
      Optional[(Int, List[Int]), (Int, List[Int]), List[Int], List[Int], Tuple2](
        {
          case (_, xs) if xs.nonEmpty => Right(xs)
          case other                  => Left(other)
        },
        { case ((k, _), xs) => (k, xs) },
      )

    val alg: Optic[(Int, List[Int]), (Int, List[Int]), Int, Int, AlgLens[List]] =
      AlgLens.fromOptionalF(opt)

    alg.modify(_ + 10)((7, List(1, 2, 3))) === ((7, List(11, 12, 13)))
    alg.modify(_ + 10)((7, Nil)) === ((7, Nil))
  }

  // ----- AlgLens[List] sandwiched between two Lenses ---------------
  //
  // Mirrors the PowerSeries `everyZip` example (CrudRoundtripSpec) but
  // with an algebraic-lens middle section instead of a traversal.
  // Three hops, all via cross-carrier `.andThen`:
  //
  //   Lens[Wrapper, Doc]                 -- outer, Tuple2 carrier
  //     .andThen(AlgLens.fromLensF(...)) -- middle, AlgLens[List]
  //     .andThen(Lens[Tag, Int])         -- inner, Tuple2 carrier
  //
  // A single `.modify(_ + 1)` then bumps every tag's count in one pass.

  "Lens → AlgLens[List] → Lens composes three carriers cleanly" >> {
    case class Wrapper(owner: String, doc: Doc)
    case class Doc(title: String, tags: List[Tag])
    case class Tag(name: String, count: Int)

    val docL: Optic[Wrapper, Wrapper, Doc, Doc, Tuple2] =
      Lens[Wrapper, Doc](_.doc, (w, d) => w.copy(doc = d))

    val tagsL: Optic[Doc, Doc, List[Tag], List[Tag], Tuple2] =
      Lens[Doc, List[Tag]](_.tags, (d, ts) => d.copy(tags = ts))

    val eachTag: Optic[Doc, Doc, Tag, Tag, AlgLens[List]] =
      AlgLens.fromLensF(tagsL)

    val countL: Optic[Tag, Tag, Int, Int, Tuple2] =
      Lens[Tag, Int](_.count, (t, c) => t.copy(count = c))

    val everyCount: Optic[Wrapper, Wrapper, Int, Int, AlgLens[List]] =
      docL.andThen(eachTag).andThen(countL)

    val w = Wrapper(
      owner = "rh",
      doc = Doc(
        title = "notes",
        tags = List(Tag("a", 1), Tag("b", 2), Tag("c", 3)),
      ),
    )

    everyCount.modify(_ + 10)(w) === Wrapper(
      owner = "rh",
      doc = Doc(
        title = "notes",
        tags = List(Tag("a", 11), Tag("b", 12), Tag("c", 13)),
      ),
    )

    // Empty list: map preserves emptiness; the lenses rebuild around [].
    val wEmpty = Wrapper("rh", Doc("notes", Nil))
    everyCount.modify(_ * 100)(wEmpty) === wEmpty
  }

  // ----- Non-singleton × non-singleton AlgLens[List] composition ----
  //
  // Pins down the general chunking algebra in `assocAlgMonad`: the outer
  // produces k candidates per input, the inner produces m candidates per
  // call, and `composeFrom` must route each inner's `F[D]` slice back to
  // the right `xi`. This case doesn't exercise the Singleton fast path —
  // both optics are Forget[List] classifiers, neither mixes in
  // AlgLens.Singleton, so the general path with per-xi sizing runs.

  "Two Forget[List] classifiers compose via AlgLens[List] with non-uniform cardinalities" >> {
    // Outer: Int → List(n, n+1) — two candidates per input.
    val outerForget = forgetOpt[List](n => List(n, n + 1), _.sum)

    // Inner: Int → List(n*10, n*10+1, n*10+2) — THREE candidates per input.
    val innerForget = forgetOpt[List](n => List(n * 10, n * 10 + 1, n * 10 + 2), _.product)

    val outerAlg = summon[Composer[Forget[List], AlgLens[List]]].to(outerForget)
    val innerAlg = summon[Composer[Forget[List], AlgLens[List]]].to(innerForget)
    val composed = outerAlg.andThen(innerAlg)

    // Push: outer.to(5) = [5, 6]; inner.to(5) = [50, 51, 52]; inner.to(6) = [60, 61, 62].
    //       Flattened classifier: [50, 51, 52, 60, 61, 62].
    //       `.foldMap(identity)` invokes `algFold[List]` (the ForgetfulFold instance), which
    //       walks the `F[C]` side of the AlgLens pair and combines under `Monoid[Int]` — the
    //       assertion pins the classifier's sum.
    composed.foldMap(identity[Int])(5) === (50 + 51 + 52 + 60 + 61 + 62)

    // Pull via .modify(identity): each inner.from([50,51,52]) = 132600, inner.from([60,61,62]) = 226920.
    // Then outer.from([132600, 226920]) = 359520.
    composed.modify(identity)(5) === (50 * 51 * 52 + 60 * 61 * 62)

    // Pull via .modify(_+1): shifts every classifier element by 1 before the per-chunk
    //   inner.from. inner.from([51,52,53]) = 51*52*53 = 140556.
    //   inner.from([61,62,63]) = 61*62*63 = 238266. Sum = 378822.
    composed.modify(_ + 1)(5) === (51 * 52 * 53 + 61 * 62 * 63)
  }

  // ----- AlgLensSingleton tag regression guard ---------------------
  //
  // Only the `tuple2alg` bridge mixes in `AlgLensSingleton` so
  // `assocAlgMonad` routes Lens-inner compositions through the singleton
  // fast path. Silently dropping `with AlgLensSingleton` in a future
  // refactor would not break any semantic test — the general path is a
  // correct fallback — but would regress perf by 3-5×. These assertions
  // pin the tag onto the adapter output that earns it, and verify the
  // bridges / factories that should *not* be tagged aren't.

  "AlgLens carrier tags: only tuple2alg mixes in AlgLensSingleton; everyone else doesn't" >> {
    val fstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    val someP: Optic[Option[Int], Option[Int], Int, Int, Either] =
      Prism[Option[Int], Int](opt => opt.toRight(opt), Some(_))

    // Only tuple2alg (Lens → AlgLens) earns the singleton tag — Lens always hits.
    val liftedLens = summon[Composer[Tuple2, AlgLens[List]]].to(fstLens)
    liftedLens must beAnInstanceOf[AlgLensSingleton[?, ?, ?, ?, ?]]

    // either2alg does NOT — Prism.miss produces F.empty (cardinality 0), which would break
    // the singleton fast path's "≥ 1 element" invariant.
    val liftedPrism = summon[Composer[Either, AlgLens[List]]].to(someP)
    liftedPrism must not(beAnInstanceOf[AlgLensSingleton[?, ?, ?, ?, ?]])

    // F[A]-focus factories don't — their classifier carries native F-cardinality.
    val phonesLens: Optic[(String, List[Int]), (String, List[Int]), List[Int], List[Int], Tuple2] =
      Lens[(String, List[Int]), List[Int]](_._2, (s, a) => (s._1, a))
    val fromLens = AlgLens.fromLensF(phonesLens)
    fromLens must not(beAnInstanceOf[AlgLensSingleton[?, ?, ?, ?, ?]])

    // forget2alg is the Forget → AlgLens injection — carries Forget's native cardinality,
    // not singleton.
    val forgetOptic = forgetOpt[List](n => List(n), _.sum)
    val forgetLifted = summon[Composer[Forget[List], AlgLens[List]]].to(forgetOptic)
    forgetLifted must not(beAnInstanceOf[AlgLensSingleton[?, ?, ?, ?, ?]])
  }

  // Regression: Prism.andThen composing two AlgLens-lifted Prisms must tolerate miss
  // on EITHER side (outer or inner), not just the outer. The critical scenario is
  // outer hit (non-empty classifier) but inner miss on that element — the singleton
  // fast path would then try to `reduceLeftToOption(F.empty[A]).get` and throw.
  "Prism.andThen(Prism) via AlgLens[List] survives inner miss on an outer hit" >> {
    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    // `positiveP` hits on n > 0, misses on n ≤ 0.
    val positiveP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n > 0 then Right(n) else Left(n), identity)

    val evenAlg = summon[Composer[Either, AlgLens[List]]].to(evenP)
    val positiveAlg = summon[Composer[Either, AlgLens[List]]].to(positiveP)

    // Outer hit + inner hit: 4 even AND positive → classifier fires end-to-end.
    val composed = evenAlg.andThen(positiveAlg)
    composed.modify(_ + 10)(4) === 14

    // Outer hit + inner miss: -2 is even (outer hit), not positive (inner miss).
    // Outer produces [-2], inner.to(-2) produces (Left(-2), Nil) — EMPTY classifier.
    // The singleton fast path must handle this without throwing.
    composed.modify(_ + 10)(-2) === -2

    // Outer miss: 3 is odd → outer classifier empty, inner never runs, source unchanged.
    composed.modify(_ + 10)(3) === 3
  }

  // Regression: the singleton fast path must call inner.to exactly once per outer
  // element. Prior to finding #1 (third-pass review) the fast path called inner.to
  // twice per element (once for ._1, once for ._2). A probe `AtomicInteger` wraps
  // a Lens-carrier inner and counts invocations of its `to`.
  "AlgLensSingleton fast path calls inner.to exactly once per outer element" >> {
    import java.util.concurrent.atomic.AtomicInteger
    val counter = new AtomicInteger(0)
    val baseLens: Optic[Int, Int, Int, Int, Tuple2] =
      Lens[Int, Int](
        get = n => { counter.incrementAndGet(); n * 10 },
        enplace = (_, b) => b / 10,
      )

    // Lift twice via tuple2alg → two singleton-tagged AlgLens optics, compose them.
    val liftedA = summon[Composer[Tuple2, AlgLens[List]]].to(baseLens)
    val liftedB = summon[Composer[Tuple2, AlgLens[List]]].to(baseLens)
    val composed = liftedA.andThen(liftedB)

    // Single composeTo: outer runs once (to get the 1-element fa), inner runs once
    // on that element — total 2 `to` calls for a single-step composition.
    counter.set(0)
    composed.modify(identity)(5)
    // outer baseLens.get + inner baseLens.get = 2 calls. If the fast path had the
    // pre-fix two-pass shape (inner.to(a)._1 then inner.to(a)._2 separately) this
    // would be 3 (1 outer + 2 inner).
    counter.get === 2
  }

  // ----- algFold coverage across cardinalities and miss branches ----
  //
  // `algFold[F]` delegates to `Foldable[F].foldMap` on the F[A] side of the
  // AlgLens pair and ignores the structural leftover X. Prior specs only
  // exercised one cardinality-6 path through a Forget→AlgLens compose; the
  // cases below pin the empty case, a genuinely-multi case, and the miss
  // branch of a Prism-lifted AlgLens so a broken `algFold` wouldn't slip
  // through silently.
  "algFold: fromLensF on an empty List folds to Monoid.empty" >> {
    val listLens: Optic[List[Int], List[Int], List[Int], List[Int], Tuple2] =
      Lens[List[Int], List[Int]](identity, (_, b) => b)
    val alg = AlgLens.fromLensF(listLens)
    alg.foldMap(identity[Int])(Nil) === 0
  }

  "algFold: fromLensF on a multi-element list sums each element exactly once" >> {
    val listLens: Optic[List[Int], List[Int], List[Int], List[Int], Tuple2] =
      Lens[List[Int], List[Int]](identity, (_, b) => b)
    val alg = AlgLens.fromLensF(listLens)
    alg.foldMap(identity[Int])(List(2, 3, 5, 7, 11)) === (2 + 3 + 5 + 7 + 11)
  }

  "algFold: fromPrismF miss branch folds to Monoid.empty" >> {
    val p: Optic[Option[List[Int]], Option[List[Int]], List[Int], List[Int], Either] =
      Prism[Option[List[Int]], List[Int]](
        { case Some(xs) if xs.nonEmpty => Right(xs); case other => Left(other) },
        Some(_),
      )
    val alg = AlgLens.fromPrismF(p)
    // Miss (None): F.empty[Int] on the focus side, sum = 0.
    alg.foldMap(identity[Int])(None) === 0
    // Miss (Some(Nil)): falls to miss branch, sum = 0.
    alg.foldMap(identity[Int])(Some(Nil)) === 0
    // Hit: folds the underlying list.
    alg.foldMap(identity[Int])(Some(List(10, 20, 30))) === 60
  }

  // ---- R11a — Traversal.each × downstream cross-carrier chains ----
  //
  // Closes the composition-gap top-3: the single most-common real
  // chain shape (traverse-then-drill) has zero behaviour coverage
  // pre-Unit-16. Each spec chains Traversal.each (PowerSeries carrier)
  // with an Iso / Optional / Prism / nested-each inner and checks the
  // result against a hand-rolled map. Exercises Composer[Tuple2, PowerSeries],
  // Composer[Either, PowerSeries], Composer[Affine, PowerSeries] fast
  // paths that previously only got reached through the benchmarks.

  "Traversal.each ∘ Iso uppercases every swap-focused element" >> {
    // Resolves via the direct `Composer[Forgetful, PowerSeries]` given
    // (added in Unit 16) — no explicit `.morph[Tuple2]` workaround needed.
    case class Pair(a: Int, b: Int)
    val pairSwap =
      Iso[Pair, Pair, (Int, Int), (Int, Int)](p => (p.a, p.b), t => Pair(t._1, t._2))
    val chain = Traversal.each[List, Pair].andThen(pairSwap)
    val out = chain.modify { case (a, b) => (a + 1, b + 10) }(
      List(Pair(1, 2), Pair(3, 4))
    )
    out === List(Pair(2, 12), Pair(4, 14))
  }

  "Traversal.each ∘ Optional skips miss branches, updates hits" >> {
    val positive =
      Optional[Int, Int, Int, Int, Affine](
        getOrModify = n => if n > 0 then Right(n) else Left(n),
        reverseGet = (_, k) => k,
      )
    val chain = Traversal.each[List, Int].andThen(positive)
    // All-hit input doubles every element:
    chain.modify(_ * 2)(List(1, 2, 3)) === List(2, 4, 6)
    // Mixed: only positives get doubled.
    chain.modify(_ * 2)(List(-1, 0, 2, -3, 4)) === List(-1, 0, 4, -3, 8)
  }

  "Traversal.each ∘ Prism matches selected elements" >> {
    sealed trait Shape
    object Shape:
      case class Circle(r: Int) extends Shape
      case class Square(s: Int) extends Shape
    val circleP =
      Prism[Shape, Shape.Circle](
        {
          case c: Shape.Circle => Right(c)
          case other           => Left(other)
        },
        identity,
      )
    val chain = Traversal.each[List, Shape].andThen(circleP)
    val shapes: List[Shape] = List(Shape.Circle(1), Shape.Square(2), Shape.Circle(3))
    chain.modify(c => Shape.Circle(c.r * 10))(shapes) ===
      List(Shape.Circle(10), Shape.Square(2), Shape.Circle(30))
  }

  "Traversal.each ∘ Traversal.each traverses a matrix uniformly" >> {
    val each2 = Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int])
    each2.modify(_ + 1)(List(List(1, 2), List(3, 4, 5), Nil)) ===
      List(List(2, 3), List(4, 5, 6), Nil)
  }

  "Traversal.each ∘ Lens ∘ Optional — three-hop realistic chain" >> {
    case class Owner(phones: List[Phone])
    case class Phone(number: String, active: Option[Boolean])
    val activePhone =
      Optional[Phone, Phone, Boolean, Boolean, Affine](
        getOrModify = p => p.active.toRight(p),
        reverseGet = (p, b) => p.copy(active = Some(b)),
      )
    // Lens[Owner, List[Phone]] then .each then Optional — covers the
    // standard "every phone's active flag, when present" shape.
    val ownerPhones =
      Lens[Owner, List[Phone]](_.phones, (o, ps) => o.copy(phones = ps))
    val chain =
      ownerPhones.andThen(Traversal.each[List, Phone]).andThen(activePhone)

    val owner = Owner(
      List(Phone("a", Some(true)), Phone("b", None), Phone("c", Some(false)))
    )
    chain.modify(!_)(owner) === Owner(
      List(Phone("a", Some(false)), Phone("b", None), Phone("c", Some(true)))
    )
  }

  // ---- R11b — Optional's fused `.andThen` overloads ---------------
  //
  // Four fused overloads exist on `Optional` (optics/Optional.scala
  // lines 123-188). Each one is an optimisation over the generic
  // cross-carrier `.andThen` but previously had no behaviour spec —
  // which also drove Optional.scala's 21.92% line coverage. Each spec
  // below calls the fused overload on a realistic fixture and asserts
  // both `.modify` behaviour and `.getOption` read-side behaviour.

  case class Address(street: String, zip: Int)
  case class AddressCarrier(address: Option[Address])

  // Shared fixtures across the R11b block.
  private val addressOpt: Optional[AddressCarrier, AddressCarrier, Address, Address] =
    Optional[AddressCarrier, AddressCarrier, Address, Address, Affine](
      getOrModify = c => c.address.toRight(c),
      reverseGet = (c, a) => c.copy(address = Some(a)),
    )

  private val streetLens: GetReplaceLens[Address, Address, String, String] =
    new GetReplaceLens[Address, Address, String, String](
      get = _.street,
      enplace = (a, s) => a.copy(street = s),
    )

  "Optional.andThen(Optional) — fused Affine ∘ Affine" >> {
    // AddressCarrier → Address (Optional) → (identity Optional on Address).
    // The inner is a trivial full-hit Optional, so the chain reduces to
    // "just apply the outer's modify where present".
    val idAddr: Optional[Address, Address, Address, Address] =
      Optional[Address, Address, Address, Address, Affine](
        Right(_),
        (_, a) => a,
      )
    val chained: Optional[AddressCarrier, AddressCarrier, Address, Address] =
      addressOpt.andThen(idAddr)

    val hit = AddressCarrier(Some(Address("Main St", 12345)))
    val miss = AddressCarrier(None)

    chained.modify(a => a.copy(street = a.street.toUpperCase))(hit) ===
      AddressCarrier(Some(Address("MAIN ST", 12345)))
    chained.modify(a => a.copy(street = a.street.toUpperCase))(miss) === miss
  }

  "Optional.andThen(GetReplaceLens) — fused Optional + Lens" >> {
    val chained: Optional[AddressCarrier, AddressCarrier, String, String] =
      addressOpt.andThen(streetLens)
    val hit = AddressCarrier(Some(Address("Main St", 12345)))
    val miss = AddressCarrier(None)
    chained.modify(_.toUpperCase)(hit) ===
      AddressCarrier(Some(Address("MAIN ST", 12345)))
    chained.modify(_.toUpperCase)(miss) === miss
  }

  "Optional.andThen(MendTearPrism) — fused Optional + Prism" >> {
    // Focus: AddressCarrier.address (Optional), then Prism that only
    // picks zip=12345 addresses, returning the street.
    case class Tagged(label: String)
    val zipTagPrism: MendTearPrism[Address, Address, Tagged, Tagged] =
      new MendTearPrism[Address, Address, Tagged, Tagged](
        tear = a => if a.zip == 12345 then Right(Tagged(a.street)) else Left(a),
        mend = (t: Tagged) => Address(t.label, 12345),
      )
    val chained: Optional[AddressCarrier, AddressCarrier, Tagged, Tagged] =
      addressOpt.andThen(zipTagPrism)

    val hit = AddressCarrier(Some(Address("Main St", 12345)))
    val missInner = AddressCarrier(Some(Address("Broadway", 99)))
    val missOuter = AddressCarrier(None)

    chained.modify(t => Tagged(t.label.reverse))(hit) ===
      AddressCarrier(Some(Address("tS niaM", 12345)))
    chained.modify(t => Tagged(t.label.reverse))(missInner) === missInner
    chained.modify(t => Tagged(t.label.reverse))(missOuter) === missOuter
  }

  "Optional.andThen(BijectionIso) — fused Optional + Iso" >> {
    // (zip, street) iso to (street, zip), then Optional into the carrier.
    val swapIso: BijectionIso[Address, Address, (Int, String), (Int, String)] =
      new BijectionIso[Address, Address, (Int, String), (Int, String)](
        get = a => (a.zip, a.street),
        reverseGet = { case (z, s) => Address(s, z) },
      )
    val chained: Optional[AddressCarrier, AddressCarrier, (Int, String), (Int, String)] =
      addressOpt.andThen(swapIso)

    val hit = AddressCarrier(Some(Address("Main St", 12345)))
    val miss = AddressCarrier(None)

    chained.modify { case (z, s) => (z + 1, s + "!") }(hit) ===
      AddressCarrier(Some(Address("Main St!", 12346)))
    chained.modify { case (z, s) => (z + 1, s + "!") }(miss) === miss
  }
