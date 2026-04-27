package dev.constructive.eo

import scala.language.implicitConversions

import cats.instances.int.given
import cats.instances.list.given
import cats.instances.option.given
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

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
import data.{Affine, Forget, Forgetful, MultiFocus, MultiFocusSingleton, PSVec, SetterF}
import data.Forgetful.given
import data.Forget.given
import data.Affine.given
import data.MultiFocus.given
import data.SetterF.given

/** Non-law behavioural coverage for EO's optics: exercises the extension methods (`andThen`,
  * `reverse`, `foldMap`, `modifyA`, `morph`), the Lens/Prism/Traversal alternative constructors,
  * and `Getter`, all of which are never reached from the ported Monocle laws but still belong in
  * any honest description of what the library does.
  *
  * '''2026-04-25 consolidation.''' 61 → 22 named blocks. Pre-image had:
  *
  *   - 3 Lens factory specs (first/second/curried) collapsed into one.
  *   - 4 Review specs collapsed into one (apply/compose/fromIso/fromPrism + ReversedLens alias).
  *   - 5 AffineFold specs collapsed into one (apply/select/fromOptional/fromPrism/Lens-narrow).
  *   - 3 Lens-into-MultiFocus[List] specs (Tuple2/Either/Affine) collapsed via the same parametric
  *     hit/miss assertion shape.
  *   - 3 SetterF lift specs (Either/Affine/PowerSeries → SetterF) collapsed similarly.
  *   - 3 mfFold cardinality specs collapsed.
  *   - 4 Optional.fused-andThen specs (Optional/GetReplaceLens/MendTearPrism/BijectionIso)
  *     collapsed via parametric hit/miss assertion.
  *
  * Each consolidated block lists the specific scenarios it absorbs in a `// covers: ...` comment.
  *
  * '''2026-04-28 MultiFocus migration.''' Every reference to the deleted `AlgLens[F]` and
  * `Kaleidoscope` carriers is rewritten to `MultiFocus[F]`. The `AlgLensSingleton` tag is renamed
  * to `MultiFocusSingleton`. The Kaleidoscope-list `morph[SetterF]` block is now exercised on
  * `MultiFocus[List]` and `MultiFocus[ZipList]` via `multifocus2setter`.
  */
class OpticsBehaviorSpec extends Specification with ScalaCheck:

  /** Hand-rolled `Forget[F]`-carrier optic on `Int`, parameterised by the classifier shape `F`. */
  private def forgetOpt[F[_]](
      toF: Int => F[Int],
      fromF: F[Int] => Int,
  ): Optic[Int, Int, Int, Int, Forget[F]] =
    new Optic[Int, Int, Int, Int, Forget[F]]:
      type X = Unit
      val to = toF
      val from = fromF

  // ----- Getter --------------------------------------------------------

  "Getter.apply reads the focused value" >> {
    val g = Getter[(Int, String), Int](_._1)
    forAll((s: (Int, String)) => g.get(s) == s._1)
  }

  // ----- Lens alternative constructors ---------------------------------

  // covers: Lens.first exposes the first tuple component, Lens.second exposes the
  // second tuple component, Lens.curried is equivalent to Lens.apply
  "Lens factories: first / second / curried agree with their Lens.apply analogues" >> {
    val l1 = Lens.first[Int, String]
    val l2 = Lens.second[Int, String]
    val curried = Lens.curried[(Int, Int), Int](_._1, a => s => (a, s._2))
    val applied = Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))

    val l1Ok = (l1.get((1, "a")) === 1).and(l1.replace(9)((1, "a")) === ((9, "a")))
    val l2Ok = (l2.get((1, "a")) === "a").and(l2.replace("z")((1, "a")) === ((1, "z")))
    val curriedOk = forAll { (s: (Int, Int), a: Int) =>
      curried.get(s) == applied.get(s) && curried.replace(a)(s) == applied.replace(a)(s)
    }
    l1Ok.and(l2Ok) && curriedOk
  }

  // ----- Lens composition via Optic.andThen ----------------------------

  "Lens composed with Lens reaches nested pair component" >> {
    type S = ((Int, Int), Int)
    val outer: Optic[S, S, (Int, Int), (Int, Int), Tuple2] =
      Lens[S, (Int, Int)](_._1, (s, a) => (a, s._2))
    val inner: Optic[(Int, Int), (Int, Int), Int, Int, Tuple2] =
      Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))
    val nested = outer.andThen(inner)
    (nested.get(((1, 2), 3)) === 1)
      .and(nested.replace(9)(((1, 2), 3)) === (((9, 2), 3)))
      .and(nested.modify(_ + 100)(((1, 2), 3)) === (((101, 2), 3)))
  }

  // ----- Iso.reverse ---------------------------------------------------

  "Iso.reverse swaps the get / reverseGet directions" >> {
    val longIso: Optic[Int, Int, Long, Long, Forgetful] =
      Iso[Int, Int, Long, Long](_.toLong, _.toInt)
    val rev = longIso.reverse
    forAll((n: Int) => rev.get(n.toLong) == n)
  }

  // ----- Prism alternative constructor ---------------------------------

  "Prism.optional builds a Prism from a partial projection" >> {
    val posIntPrism = Prism.optional[Int, Int](
      n => if n >= 0 then Some(n) else None,
      identity,
    )
    (posIntPrism.to(5) === Right(5))
      .and(posIntPrism.to(-1) === Left(-1))
      .and(posIntPrism.reverseGet(7) === 7)
  }

  // ----- Traversal/foldMap/modifyA shorthand ---------------------------

  // covers: Traversal.forEach.foldMap totals the list under Monoid[Int],
  // Traversal.forEach.modifyA short-circuits on None, Lens morphed into an Affine
  // behaves like the original Lens
  "Traversal.forEach: foldMap totals + modifyA short-circuits + morph[Affine] preserves Lens behaviour" >> {
    val t: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
      Traversal.forEach[List, Int, Int]
    val sumOk = forAll((xs: List[Int]) => t.foldMap(identity[Int])(xs) == xs.sum)

    val positivesDoubled: Int => Option[Int] =
      a => if a > 0 then Some(a * 2) else None
    val mAOk =
      (t.modifyA[Option](positivesDoubled)(List(1, 2, 3)) == Some(List(2, 4, 6))) &&
        (t.modifyA[Option](positivesDoubled)(List(1, -2, 3)) == None) &&
        (t.modifyA[Option](positivesDoubled)(Nil) == Some(Nil))

    val l: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens.first[Int, String]
    val asAffine: Optic[(Int, String), (Int, String), Int, Int, Affine] =
      l.morph[Affine]
    val morphOk =
      forAll((pair: (Int, String), a: Int) => asAffine.modify(_ => a)(pair) == l.replace(a)(pair))

    sumOk && morphOk && mAOk
  }

  // ----- Fold ---------------------------------------------------------

  // covers: Fold.apply[List] folds all elements, Fold.select keeps values matching
  // the predicate
  "Fold.apply[List] sums + Fold.select keeps matching predicates" >> {
    val f: Optic[List[Int], Unit, Int, Int, Forget[List]] = Fold[List, Int]
    val sumOk = forAll((xs: List[Int]) => f.foldMap(identity[Int])(xs) == xs.sum)

    val evenFold: Optic[Int, Unit, Int, Int, Forget[Option]] =
      Fold.select[Int](_ % 2 == 0)
    val selectOk = forAll((n: Int) => evenFold.to(n) == (if n % 2 == 0 then Some(n) else None))

    sumOk && selectOk
  }

  // covers: Optional.foldMap returns the focus when matched, empty otherwise
  "Optional.foldMap returns the focus when matched, empty otherwise" >> {
    val evenFstOpt: Optic[(Int, Int), (Int, Int), Int, Int, Affine] =
      Optional[(Int, Int), (Int, Int), Int, Int, Tuple2](
        { case (a, b) => if a % 2 == 0 then Right(a) else Left((a, b)) },
        { case ((_, b), newA) => (newA, b) },
      )
    forAll((a: Int, b: Int) =>
      evenFstOpt.foldMap(identity[Int])((a, b)) == (if a % 2 == 0 then a else 0)
    )
  }

  // ----- Composer morphs from Forgetful --------------------------------

  // covers: Iso.morph[Tuple2] behaves like a Lens, Iso.morph[Either] behaves like a
  // Prism, Iso.morph[Tuple2].morph[Affine] is a valid Affine-shaped optic
  "Iso.morph[Tuple2/Either/Tuple2→Affine] — Forgetful → Tuple2 → Affine chain" >> {
    val doubleIso: Optic[Int, Int, Int, Int, Forgetful] =
      Iso[Int, Int, Int, Int](_ * 2, _ / 2)

    val asLens = doubleIso.morph[Tuple2]
    val tuple2Ok = forAll((n: Int) => asLens.get(n) == n * 2)

    val asPrism = doubleIso.morph[Either]
    val eitherOk =
      (asPrism.to(10) == Right(20)) && (asPrism.reverseGet(20) == 10)

    val asAffine = doubleIso.morph[Tuple2].morph[Affine]
    val affineOk = forAll((n: Int) => asAffine.to(n).affine.toOption.map(_._2) == Some(n * 2))

    tuple2Ok && affineOk && eitherOk
  }

  // ----- Cross-carrier andThen via Morph.bothViaAffine -----------------

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

  // covers: Prism.andThen(Lens) composes via Morph.bothViaAffine, Lens.andThen(Prism)
  // composes via Morph.bothViaAffine (symmetric)
  "Morph.bothViaAffine: Prism→Lens and Lens→Prism both compose via the affine bridge" >> {
    val triSide: Optic[Shape3, Shape3, Int, Int, Affine] = triP.andThen(triSideL)
    val r1 = (triSide.modify(_ + 10)(Shape3.Tri(3)) === Shape3.Tri(13))
      .and(triSide.modify(_ + 10)(Shape3.Sq(5)) === Shape3.Sq(5))

    case class Wrapper(shape: Shape3)
    val wrapperShape = Lens[Wrapper, Shape3](_.shape, (w, s) => w.copy(shape = s))
    val wrappedTri: Optic[Wrapper, Wrapper, Shape3.Tri, Shape3.Tri, Affine] =
      wrapperShape.andThen(triP)
    val r2 =
      (wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Tri(3))) ===
        Wrapper(Shape3.Tri(4))).and(
        wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Sq(5))) ===
          Wrapper(Shape3.Sq(5))
      )
    r1.and(r2)
  }

  // ----- Optional.readOnly behaviour -----------------------------------

  case class AdultPerson(age: Int)

  val adultAge: Optic[AdultPerson, Unit, Int, Int, Affine] =
    Optional.readOnly(p => Option.when(p.age >= 18)(p.age))

  // covers: Optional.readOnly.foldMap folds the hit branch, returns empty on miss,
  // Optional.selectReadOnly keeps values matching the predicate, Optional.readOnly.modifyA
  // lifts a hit-branch read under Applicative[G]
  "Optional.readOnly: foldMap hit/miss + selectReadOnly predicate + modifyA[Option]" >> {
    val foldOk = (adultAge.foldMap(identity[Int])(AdultPerson(20)) === 20)
      .and(adultAge.foldMap(identity[Int])(AdultPerson(15)) === 0)

    val evenAF = Optional.selectReadOnly[Int](_ % 2 == 0)
    val selOk = (evenAF.foldMap(identity[Int])(4) === 4)
      .and(evenAF.foldMap(identity[Int])(3) === 0)
      .and(evenAF.foldMap(identity[Int])(0) === 0)

    val mf: Int => Option[Int] = n => Option.when(n > 0)(n * 10)
    val mAOk = (adultAge.modifyA[Option](mf)(AdultPerson(20)) === Some(()))
      .and(adultAge.modifyA[Option](mf)(AdultPerson(15)) === Some(()))

    foldOk.and(selOk).and(mAOk)
  }

  // ----- AffineFold behaviour ------------------------------------------

  val adultAgeAF: AffineFold[AdultPerson, Int] =
    AffineFold(p => Option.when(p.age >= 18)(p.age))

  // covers: AffineFold.apply hits on Some and misses on None,
  // AffineFold.select keeps values matching the predicate, AffineFold.fromOptional
  // drops write path, AffineFold.fromPrism drops build path,
  // AffineFold.fromOptional narrows a Lens-composed Optional
  "AffineFold: apply / select / fromOptional / fromPrism / Lens-narrow all preserve getOption" >> {
    val applyOk = (adultAgeAF.getOption(AdultPerson(20)) === Some(20))
      .and(adultAgeAF.getOption(AdultPerson(15)) === None)

    val evenAF = AffineFold.select[Int](_ % 2 == 0)
    val selOk = (evenAF.getOption(4) === Some(4)).and(evenAF.getOption(3) === None)

    val ageOpt: Optional[AdultPerson, AdultPerson, Int, Int] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val afFromOpt = AffineFold.fromOptional(ageOpt)
    val fromOptOk = (afFromOpt.getOption(AdultPerson(20)) === Some(20))
      .and(afFromOpt.getOption(AdultPerson(15)) === None)

    val intPrism = Prism.optional[String, Int](s => s.toIntOption, _.toString)
    val afFromPrism = AffineFold.fromPrism(intPrism)
    val fromPrismOk = (afFromPrism.getOption("42") === Some(42))
      .and(afFromPrism.getOption("nope") === None)

    case class Wrapper(inner: AdultPerson)
    val wrapAge = Optional[Wrapper, Wrapper, Int, Int, Affine](
      getOrModify = w => Either.cond(w.inner.age >= 18, w.inner.age, w),
      reverseGet = { case (w, a) => w.copy(inner = AdultPerson(a)) },
    )
    val chained = AffineFold.fromOptional(wrapAge)
    val chainOk = (chained.getOption(Wrapper(AdultPerson(20))) === Some(20))
      .and(chained.getOption(Wrapper(AdultPerson(15))) === None)

    applyOk.and(selOk).and(fromOptOk).and(fromPrismOk).and(chainOk)
  }

  // ----- Review behaviour ----------------------------------------------

  // covers: Review.apply wraps an A => S build function, Reviews compose via
  // direct function composition, Review.fromIso extracts the reverseGet side of a
  // BijectionIso, Review.fromPrism extracts the mend side of a MendTearPrism,
  // ReversedLens(iso) and ReversedPrism(prism) alias the Review factories
  "Review: apply / compose / fromIso / fromPrism + ReversedLens/ReversedPrism aliases" >> {
    val toSome = Review[Option[Int], Int](Some(_))
    val applyOk = (toSome.reverseGet(3) === Some(3)).and(toSome.reverseGet(-1) === Some(-1))

    val stringify = Review[Int, String](_.length)
    val composed =
      Review[Option[Int], String](s => toSome.reverseGet(stringify.reverseGet(s)))
    val composeOk = composed.reverseGet("hello") === Some(5)

    val doubleIsoConcrete: BijectionIso[Int, Int, Int, Int] =
      BijectionIso[Int, Int, Int, Int](_ * 2, _ / 2)
    val fromIsoOk = Review.fromIso(doubleIsoConcrete).reverseGet(10) === 5

    val somePrism = new MendTearPrism[Option[Int], Option[Int], Int, Int](
      tear = {
        case Some(n) => Right(n)
        case None    => Left(None)
      },
      mend = Some(_),
    )
    val fromPrismOk = Review.fromPrism(somePrism).reverseGet(42) === Some(42)

    val rl = ReversedLens(doubleIsoConcrete)
    val rp = ReversedPrism(somePrism)
    val aliasOk = (rl.reverseGet(10) === 5).and(rp.reverseGet(7) === Some(7))

    applyOk.and(composeOk).and(fromIsoOk).and(fromPrismOk).and(aliasOk)
  }

  // ----- Forget[F] `.andThen` via assocForgetMonad ---------------------

  "Forget[Option] optics compose via `.andThen` (algebraic-lens shape under Monad[Option])" >> {
    val outer = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-100)(_ * 2 + 1000),
    )
    val inner = forgetOpt[Option](
      n => Option.when(n < 1000)(n + 1),
      _.fold(-1)(_ + 1),
    )
    val composed = outer.andThen(inner)

    (composed.to(5) === Some(51))
      .and(composed.to(-2) === None)
      .and(composed.to(200) === None)
      .and(composed.from(Some(7)) === 1016)
      .and(composed.from(None) === 998)
  }

  "Forget[Option] injects into MultiFocus[Option] and composes under Monad[Option]" >> {
    val pureForget = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-1)(_ + 7),
    )
    val lifted = summon[Composer[Forget[Option], MultiFocus[Option]]].to(pureForget)
    val r1 = (lifted.modify(_ + 1)(5) === 58).and(lifted.modify(_ + 1)(-2) === -1)

    val inner: Optic[Int, Int, Int, Int, MultiFocus[Option]] =
      new Optic[Int, Int, Int, Int, MultiFocus[Option]]:
        type X = String
        val to: Int => (String, Option[Int]) = n => (s"tag-$n", Option.when(n < 1000)(n + 1))
        val from: ((String, Option[Int])) => Int = {
          case (tag, fb) =>
            fb.fold(tag.length)(_ * 100 + tag.length)
        }
    val composed = lifted.andThen(inner)
    val r2 =
      (composed.modify(identity)(5) === 5113).and(composed.modify(identity)(-2) === -1)
    r1.and(r2)
  }

  // ----- Lens / Prism / Optional → MultiFocus[List] -----------------------

  case class AdultOptCarrier(p: AdultPerson)

  // covers: Tuple2 Lens lifts into MultiFocus[List] and preserves .modify semantics,
  // Either Prism lifts into MultiFocus[List] preserves hit/miss, Affine Optional
  // lifts into MultiFocus[List] preserves hit/miss, Optional andThen MultiFocus[List]
  // classifier composes via affine2multifocus
  "Lens / Prism / Optional → MultiFocus[List]: hit/miss .modify semantics preserved + composer chain" >> {
    val fstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    val tuple2Lifted = summon[Composer[Tuple2, MultiFocus[List]]].to(fstLens)
    val tuple2Ok = (tuple2Lifted.modify(_ + 100)((3, "hi")) === ((103, "hi")))
      .and(tuple2Lifted.modify(_ * 2)((5, "x")) === ((10, "x")))

    val oddP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 1 then Right(n) else Left(n), identity)
    val eitherLifted = summon[Composer[Either, MultiFocus[List]]].to(oddP)
    val eitherOk =
      (eitherLifted.modify(_ * 2)(3) === 6).and(eitherLifted.modify(_ * 2)(4) === 4)

    val adultOpt: Optic[AdultPerson, AdultPerson, Int, Int, Affine] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val affineLifted = summon[Composer[Affine, MultiFocus[List]]].to(adultOpt)
    val affineOk = (affineLifted.modify(_ + 1)(AdultPerson(25)) === AdultPerson(26))
      .and(affineLifted.modify(_ + 1)(AdultPerson(12)) === AdultPerson(12))

    val posOpt: Optic[Int, Int, Int, Int, Affine] =
      Optional[Int, Int, Int, Int, Affine](
        getOrModify = n => Either.cond(n > 0, n, n),
        reverseGet = { case (_, n) => n },
      )
    val candidatesMF = summon[Composer[Forget[List], MultiFocus[List]]].to(
      forgetOpt[List](n => List(n, n * 2), _.sum)
    )
    val composed = posOpt.andThen(candidatesMF)
    val composedOk =
      (composed.modify(_ + 1)(3) === 11).and(composed.modify(_ + 1)(-1) === -1)

    tuple2Ok.and(eitherOk).and(affineOk).and(composedOk)
  }

  // ----- Prism / Optional / Traversal lifted into SetterF --------------

  // covers: Either Prism lifts into SetterF and preserves hit/miss, Affine Optional
  // lifts into SetterF and preserves hit/miss, PowerSeries Traversal lifts into
  // SetterF and applies f to every focus
  "Prism / Optional / Traversal → SetterF: hit/miss + every-focus broadcast" >> {
    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    val eitherLifted: Optic[Int, Int, Int, Int, data.SetterF] =
      summon[Composer[Either, data.SetterF]].to(evenP)
    val eitherOk =
      (eitherLifted.modify(_ + 10)(4) === 14).and(eitherLifted.modify(_ + 10)(5) === 5)

    val adultOpt: Optic[AdultPerson, AdultPerson, Int, Int, Affine] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val affineLifted: Optic[AdultPerson, AdultPerson, Int, Int, data.SetterF] =
      summon[Composer[Affine, data.SetterF]].to(adultOpt)
    val affineOk = (affineLifted.modify(_ + 1)(AdultPerson(25)) === AdultPerson(26))
      .and(affineLifted.modify(_ + 1)(AdultPerson(12)) === AdultPerson(12))

    val each: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
      Traversal.each[List, Int]
    val psLifted: Optic[List[Int], List[Int], Int, Int, data.SetterF] =
      summon[Composer[MultiFocus[PSVec], data.SetterF]].to(each)
    val psOk =
      (psLifted.modify(_ * 10)(List(1, 2, 3)) === List(10, 20, 30))
        .and(psLifted.modify(_ * 10)(Nil) === Nil)

    eitherOk.and(affineOk).and(psOk)
  }

  // ----- MultiFocus[Function1] (absorbed Grate) lifted into SetterF -----

  // covers: MultiFocus.tuple lifts into SetterF and rebroadcasts via the per-slot rebuild,
  // MultiFocus.representable over Representable[Function1[Boolean, *]] lifts identically
  // (pointwise map). The lifted SetterF.modify must agree byte-for-byte with the original
  // MultiFocus's .modify(f) — same Functor[Function1[X0, *]]-broadcast invariant as the deleted
  // ForgetfulFunctor[Grate].
  "MultiFocus.tuple / MultiFocus.representable[Function1] → SetterF: per-slot broadcast under .modify" >> {
    val tupleMF: Optic[(Int, Int, Int), (Int, Int, Int), Int, Int, MultiFocus[Function1[Int, *]]] =
      MultiFocus.tuple[(Int, Int, Int), Int]
    val tupleLifted: Optic[(Int, Int, Int), (Int, Int, Int), Int, Int, data.SetterF] =
      summon[Composer[MultiFocus[Function1[Int, *]], data.SetterF]].to(tupleMF)
    val tupleOk =
      (tupleLifted.modify(_ + 1)((10, 20, 30)) === ((11, 21, 31)))
        .and(tupleLifted.modify(_ * 2)((1, 2, 3)) === ((2, 4, 6)))
        .and(tupleLifted.modify(identity[Int])((7, 8, 9)) === ((7, 8, 9)))

    import cats.instances.function.given
    val fnMF: Optic[Boolean => Int, Boolean => Int, Int, Int, MultiFocus[Function1[Boolean, *]]] =
      MultiFocus.representable[[a] =>> Boolean => a, Int]
    val fnLifted: Optic[Boolean => Int, Boolean => Int, Int, Int, data.SetterF] =
      summon[Composer[MultiFocus[Function1[Boolean, *]], data.SetterF]].to(fnMF)
    val srcFn: Boolean => Int = b => if b then 100 else 200
    val modified: Boolean => Int = fnLifted.modify(_ + 1)(srcFn)
    val fnOk = (modified(true) === 101).and(modified(false) === 201)

    tupleOk.and(fnOk)
  }

  // ----- MultiFocus[F] lifted into SetterF -----------------------------

  // covers: MultiFocus.apply[List] and MultiFocus.apply[ZipList] lift into SetterF and modify
  // element-wise via Functor[F]. The lifted SetterF.modify must agree byte-for-byte with the
  // original MultiFocus's .modify(f) — same Functor-driven element-wise rewrite as
  // ForgetfulFunctor[MultiFocus[F]].
  "MultiFocus.apply[List] / MultiFocus.apply[ZipList] → SetterF: element-wise modify" >> {
    val listMF: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
      MultiFocus.apply[List, Int]
    val listLifted: Optic[List[Int], List[Int], Int, Int, SetterF] =
      summon[Composer[MultiFocus[List], SetterF]].to(listMF)
    val listOk =
      (listLifted.modify(_ + 1)(List(1, 2, 3)) === List(2, 3, 4))
        .and(listLifted.modify(_ * 10)(Nil) === Nil)
        .and(listLifted.modify(identity[Int])(List(7, 8, 9)) === List(7, 8, 9))

    import cats.data.ZipList
    val zipMF: Optic[ZipList[Int], ZipList[Int], Int, Int, MultiFocus[ZipList]] =
      MultiFocus.apply[ZipList, Int]
    val zipLifted: Optic[ZipList[Int], ZipList[Int], Int, Int, SetterF] =
      summon[Composer[MultiFocus[ZipList], SetterF]].to(zipMF)
    val zipOk =
      (zipLifted.modify(_ * 10)(ZipList(List(1, 2, 3))).value === List(10, 20, 30))
        .and(zipLifted.modify(_ + 1)(ZipList(Nil)).value === List.empty[Int])

    listOk.and(zipOk)
  }

  // ----- Cross-carrier composition Lens → MultiFocus classifier --------

  "Lens andThen MultiFocus[List] classifier composes end-to-end" >> {
    case class Row(id: Int, value: Int)
    val valueLens: Optic[Row, Row, Int, Int, Tuple2] =
      Lens[Row, Int](_.value, (r, v) => r.copy(value = v))
    val candidatesForget = forgetOpt[List](n => List(n, n + 10), _.sum)
    val candidatesMF = summon[Composer[Forget[List], MultiFocus[List]]].to(candidatesForget)
    val composed = valueLens.andThen(candidatesMF)
    (composed.modify(_ * 3)(Row(1, 5)) === Row(1, 60))
      .and(composed.modify(identity)(Row(9, 5)) === Row(9, 20))
  }

  // ----- F[A]-focus factories -----------------------------------------

  // covers: MultiFocus.fromLensF rewraps a Lens[S, F[A]] with zero constraints,
  // MultiFocus.fromPrismF lifts a Prism[S, F[A]] with MonoidK[F] only,
  // MultiFocus.fromOptionalF lifts an Optional[S, F[A]] through the Hit branch
  "MultiFocus.fromLensF / fromPrismF / fromOptionalF — F[A]-focus factories" >> {
    case class Table(rows: List[Int])
    val rowsLens: Optic[Table, Table, List[Int], List[Int], Tuple2] =
      Lens[Table, List[Int]](_.rows, (t, rs) => t.copy(rows = rs))
    val listMF = MultiFocus.fromLensF(rowsLens)
    val lensFOk = (listMF.modify(_ + 1)(Table(List(1, 2, 3))) === Table(List(2, 3, 4)))
      .and(listMF.modify(_ * 10)(Table(Nil)) === Table(Nil))

    val p: Optic[Option[List[Int]], Option[List[Int]], List[Int], List[Int], Either] =
      Prism[Option[List[Int]], List[Int]](
        {
          case Some(xs) if xs.nonEmpty => Right(xs)
          case other                   => Left(other)
        },
        Some(_),
      )
    val mf = MultiFocus.fromPrismF(p)
    val prismFOk = (mf.modify(_ * 2)(Some(List(1, 2, 3))) === Some(List(2, 4, 6)))
      .and(mf.modify(_ * 2)(None) === None)
      .and(mf.modify(_ * 2)(Some(Nil)) === Some(Nil))

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
    val optMF = MultiFocus.fromOptionalF(opt)
    val optionalFOk = (optMF.modify(_ + 10)((7, List(1, 2, 3))) === ((7, List(11, 12, 13))))
      .and(optMF.modify(_ + 10)((7, Nil)) === ((7, Nil)))

    lensFOk.and(prismFOk).and(optionalFOk)
  }

  // ----- 3-hop Lens → MultiFocus[List] → Lens chain --------------------

  "Lens → MultiFocus[List] → Lens composes three carriers cleanly" >> {
    case class Wrapper(owner: String, doc: Doc)
    case class Doc(title: String, tags: List[Tag])
    case class Tag(name: String, count: Int)

    val docL: Optic[Wrapper, Wrapper, Doc, Doc, Tuple2] =
      Lens[Wrapper, Doc](_.doc, (w, d) => w.copy(doc = d))
    val tagsL: Optic[Doc, Doc, List[Tag], List[Tag], Tuple2] =
      Lens[Doc, List[Tag]](_.tags, (d, ts) => d.copy(tags = ts))
    val eachTag: Optic[Doc, Doc, Tag, Tag, MultiFocus[List]] =
      MultiFocus.fromLensF(tagsL)
    val countL: Optic[Tag, Tag, Int, Int, Tuple2] =
      Lens[Tag, Int](_.count, (t, c) => t.copy(count = c))

    val everyCount: Optic[Wrapper, Wrapper, Int, Int, MultiFocus[List]] =
      docL.andThen(eachTag).andThen(countL)

    val w = Wrapper(
      owner = "rh",
      doc = Doc(
        title = "notes",
        tags = List(Tag("a", 1), Tag("b", 2), Tag("c", 3)),
      ),
    )

    val r1 = everyCount.modify(_ + 10)(w) === Wrapper(
      owner = "rh",
      doc = Doc(
        title = "notes",
        tags = List(Tag("a", 11), Tag("b", 12), Tag("c", 13)),
      ),
    )
    val wEmpty = Wrapper("rh", Doc("notes", Nil))
    val r2 = everyCount.modify(_ * 100)(wEmpty) === wEmpty
    r1.and(r2)
  }

  // ----- Non-singleton × non-singleton MultiFocus[List] composition ----

  "Two Forget[List] classifiers compose via MultiFocus[List] with non-uniform cardinalities" >> {
    val outerForget = forgetOpt[List](n => List(n, n + 1), _.sum)
    val innerForget = forgetOpt[List](n => List(n * 10, n * 10 + 1, n * 10 + 2), _.product)
    val outerMF = summon[Composer[Forget[List], MultiFocus[List]]].to(outerForget)
    val innerMF = summon[Composer[Forget[List], MultiFocus[List]]].to(innerForget)
    val composed = outerMF.andThen(innerMF)

    (composed.foldMap(identity[Int])(5) === (50 + 51 + 52 + 60 + 61 + 62))
      .and(composed.modify(identity)(5) === (50 * 51 * 52 + 60 * 61 * 62))
      .and(composed.modify(_ + 1)(5) === (51 * 52 * 53 + 61 * 62 * 63))
  }

  // ----- MultiFocusSingleton tag regression ----------------------------

  "MultiFocus carrier tags: only tuple2multifocus mixes in MultiFocusSingleton; everyone else doesn't" >> {
    val fstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    val someP: Optic[Option[Int], Option[Int], Int, Int, Either] =
      Prism[Option[Int], Int](opt => opt.toRight(opt), Some(_))

    val liftedLens = summon[Composer[Tuple2, MultiFocus[List]]].to(fstLens)
    val tagOk1 = liftedLens must beAnInstanceOf[MultiFocusSingleton[?, ?, ?, ?, ?]]

    val liftedPrism = summon[Composer[Either, MultiFocus[List]]].to(someP)
    val tagOk2 = liftedPrism must not(beAnInstanceOf[MultiFocusSingleton[?, ?, ?, ?, ?]])

    val phonesLens: Optic[(String, List[Int]), (String, List[Int]), List[Int], List[Int], Tuple2] =
      Lens[(String, List[Int]), List[Int]](_._2, (s, a) => (s._1, a))
    val fromLens = MultiFocus.fromLensF(phonesLens)
    val tagOk3 = fromLens must not(beAnInstanceOf[MultiFocusSingleton[?, ?, ?, ?, ?]])

    val forgetOptic = forgetOpt[List](n => List(n), _.sum)
    val forgetLifted = summon[Composer[Forget[List], MultiFocus[List]]].to(forgetOptic)
    val tagOk4 = forgetLifted must not(beAnInstanceOf[MultiFocusSingleton[?, ?, ?, ?, ?]])

    tagOk1.and(tagOk2).and(tagOk3).and(tagOk4)
  }

  "Prism.andThen(Prism) via MultiFocus[List] survives inner miss on an outer hit" >> {
    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    val positiveP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n > 0 then Right(n) else Left(n), identity)

    val evenMF = summon[Composer[Either, MultiFocus[List]]].to(evenP)
    val positiveMF = summon[Composer[Either, MultiFocus[List]]].to(positiveP)
    val composed = evenMF.andThen(positiveMF)

    (composed.modify(_ + 10)(4) === 14)
      .and(composed.modify(_ + 10)(-2) === -2)
      .and(composed.modify(_ + 10)(3) === 3)
  }

  "MultiFocusSingleton fast path calls inner.to exactly once per outer element" >> {
    import java.util.concurrent.atomic.AtomicInteger
    val counter = new AtomicInteger(0)
    val baseLens: Optic[Int, Int, Int, Int, Tuple2] =
      Lens[Int, Int](
        get = n => { counter.incrementAndGet(); n * 10 },
        enplace = (_, b) => b / 10,
      )
    val liftedA = summon[Composer[Tuple2, MultiFocus[List]]].to(baseLens)
    val liftedB = summon[Composer[Tuple2, MultiFocus[List]]].to(baseLens)
    val composed = liftedA.andThen(liftedB)

    counter.set(0)
    composed.modify(identity)(5)
    counter.get === 2
  }

  // ----- mfFold cardinality + miss branch coverage ---------------------

  // covers: mfFold fromLensF empty list folds to Monoid.empty, multi-element list
  // sums each element exactly once, fromPrismF miss branch folds to Monoid.empty
  "mfFold cardinality+miss: fromLensF (empty/multi) and fromPrismF (hit/miss) " >> {
    val listLens: Optic[List[Int], List[Int], List[Int], List[Int], Tuple2] =
      Lens[List[Int], List[Int]](identity, (_, b) => b)
    val mf = MultiFocus.fromLensF(listLens)
    val emptyOk = mf.foldMap(identity[Int])(Nil) === 0
    val multiOk = mf.foldMap(identity[Int])(List(2, 3, 5, 7, 11)) === (2 + 3 + 5 + 7 + 11)

    val p: Optic[Option[List[Int]], Option[List[Int]], List[Int], List[Int], Either] =
      Prism[Option[List[Int]], List[Int]](
        { case Some(xs) if xs.nonEmpty => Right(xs); case other => Left(other) },
        Some(_),
      )
    val pa = MultiFocus.fromPrismF(p)
    val missNone = pa.foldMap(identity[Int])(None) === 0
    val missEmpty = pa.foldMap(identity[Int])(Some(Nil)) === 0
    val hit = pa.foldMap(identity[Int])(Some(List(10, 20, 30))) === 60

    emptyOk.and(multiOk).and(missNone).and(missEmpty).and(hit)
  }

  // ---- R11a — Traversal.each × downstream cross-carrier chains --------

  // covers: Traversal.each ∘ Iso, Traversal.each ∘ Optional, Traversal.each ∘ Prism,
  // Traversal.each ∘ Traversal.each, Traversal.each ∘ Lens ∘ Optional
  "Traversal.each ∘ {Iso / Optional / Prism / each / Lens-Optional} downstream chains" >> {
    case class Pair(a: Int, b: Int)
    val pairSwap =
      Iso[Pair, Pair, (Int, Int), (Int, Int)](p => (p.a, p.b), t => Pair(t._1, t._2))
    val isoChain = Traversal.each[List, Pair].andThen(pairSwap)
    val isoOk = isoChain.modify { case (a, b) => (a + 1, b + 10) }(
      List(Pair(1, 2), Pair(3, 4))
    ) === List(Pair(2, 12), Pair(4, 14))

    val positive =
      Optional[Int, Int, Int, Int, Affine](
        getOrModify = n => if n > 0 then Right(n) else Left(n),
        reverseGet = (_, k) => k,
      )
    val optChain = Traversal.each[List, Int].andThen(positive)
    val optOk = (optChain.modify(_ * 2)(List(1, 2, 3)) === List(2, 4, 6))
      .and(optChain.modify(_ * 2)(List(-1, 0, 2, -3, 4)) === List(-1, 0, 4, -3, 8))

    sealed trait Shape
    object Shape:
      case class Circle(r: Int) extends Shape
      case class Square(s: Int) extends Shape
    val circleP =
      Prism[Shape, Shape.Circle](
        { case c: Shape.Circle => Right(c); case other => Left(other) },
        identity,
      )
    val prismChain = Traversal.each[List, Shape].andThen(circleP)
    val shapes: List[Shape] = List(Shape.Circle(1), Shape.Square(2), Shape.Circle(3))
    val prismOk = prismChain.modify(c => Shape.Circle(c.r * 10))(shapes) ===
      List(Shape.Circle(10), Shape.Square(2), Shape.Circle(30))

    val each2 = Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int])
    val each2Ok = each2.modify(_ + 1)(List(List(1, 2), List(3, 4, 5), Nil)) ===
      List(List(2, 3), List(4, 5, 6), Nil)

    case class Owner(phones: List[Phone])
    case class Phone(number: String, active: Option[Boolean])
    val activePhone =
      Optional[Phone, Phone, Boolean, Boolean, Affine](
        getOrModify = p => p.active.toRight(p),
        reverseGet = (p, b) => p.copy(active = Some(b)),
      )
    val ownerPhones =
      Lens[Owner, List[Phone]](_.phones, (o, ps) => o.copy(phones = ps))
    val threeHop =
      ownerPhones.andThen(Traversal.each[List, Phone]).andThen(activePhone)
    val owner = Owner(
      List(Phone("a", Some(true)), Phone("b", None), Phone("c", Some(false)))
    )
    val threeHopOk = threeHop.modify(!_)(owner) === Owner(
      List(Phone("a", Some(false)), Phone("b", None), Phone("c", Some(true)))
    )

    isoOk.and(optOk).and(prismOk).and(each2Ok).and(threeHopOk)
  }

  // ---- R11b — Optional's fused `.andThen` overloads -------------------

  case class Address(street: String, zip: Int)
  case class AddressCarrier(address: Option[Address])

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

  // covers: Optional.andThen(Optional) — fused Affine ∘ Affine,
  // Optional.andThen(GetReplaceLens) — fused Optional + Lens,
  // Optional.andThen(MendTearPrism) — fused Optional + Prism,
  // Optional.andThen(BijectionIso) — fused Optional + Iso
  "Optional.andThen fused overloads: Optional / Lens / Prism / Iso — hit + miss preserved" >> {
    val hit = AddressCarrier(Some(Address("Main St", 12345)))
    val miss = AddressCarrier(None)

    val idAddr: Optional[Address, Address, Address, Address] =
      Optional[Address, Address, Address, Address, Affine](Right(_), (_, a) => a)
    val optChain = addressOpt.andThen(idAddr)
    val optOk =
      (optChain.modify(a => a.copy(street = a.street.toUpperCase))(hit) ===
        AddressCarrier(Some(Address("MAIN ST", 12345))))
        .and(optChain.modify(a => a.copy(street = a.street.toUpperCase))(miss) === miss)

    val lensChain = addressOpt.andThen(streetLens)
    val lensOk = (lensChain.modify(_.toUpperCase)(hit) ===
      AddressCarrier(Some(Address("MAIN ST", 12345)))).and(
      lensChain.modify(_.toUpperCase)(miss) === miss
    )

    case class Tagged(label: String)
    val zipTagPrism: MendTearPrism[Address, Address, Tagged, Tagged] =
      new MendTearPrism[Address, Address, Tagged, Tagged](
        tear = a => if a.zip == 12345 then Right(Tagged(a.street)) else Left(a),
        mend = (t: Tagged) => Address(t.label, 12345),
      )
    val prismChain = addressOpt.andThen(zipTagPrism)
    val missInner = AddressCarrier(Some(Address("Broadway", 99)))
    val prismOk = (prismChain.modify(t => Tagged(t.label.reverse))(hit) ===
      AddressCarrier(Some(Address("tS niaM", 12345))))
      .and(prismChain.modify(t => Tagged(t.label.reverse))(missInner) === missInner)
      .and(prismChain.modify(t => Tagged(t.label.reverse))(miss) === miss)

    val swapIso: BijectionIso[Address, Address, (Int, String), (Int, String)] =
      new BijectionIso[Address, Address, (Int, String), (Int, String)](
        get = a => (a.zip, a.street),
        reverseGet = { case (z, s) => Address(s, z) },
      )
    val isoChain = addressOpt.andThen(swapIso)
    val isoOk = (isoChain.modify { case (z, s) => (z + 1, s + "!") }(hit) ===
      AddressCarrier(Some(Address("Main St!", 12346))))
      .and(isoChain.modify { case (z, s) => (z + 1, s + "!") }(miss) === miss)

    optOk.and(lensOk).and(prismOk).and(isoOk)
  }
