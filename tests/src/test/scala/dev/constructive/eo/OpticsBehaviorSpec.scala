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
  Review,
  Setter,
  Traversal,
}
import optics.Optic.*
import data.{Affine, Forget, Direct, MultiFocus, MultiFocusSingleton, PSVec, SetterF}

/** Non-law behavioural coverage for EO's optics: exercises the extension methods (`andThen`,
  * `reverse`, `foldMap`, `modifyA`, `morph`), the Lens/Prism/Traversal alternative constructors,
  * and `Getter`, all of which are never reached from the ported Monocle laws but still belong in
  * any honest description of what the library does.
  *
  * '''2026-04-25 consolidation.''' 61 → 22 named blocks. Pre-image had:
  *
  *   - 3 Lens factory specs (first/second/curried) collapsed into one.
  *   - Review specs collapsed into one (apply + compose-as-Optic via andThen).
  *   - 5 AffineFold specs collapsed into one (apply/select + read-only views via
  *     `AffineFold(o.getOption)`).
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
      def to(s: Int): Forget[F][X, Int] = Forget(toF(s))
      def from(b: Forget[F][X, Int]): Int = fromF(b.value)

  // ----- Basic-factory smoke tests -------------------------------------
  //
  // 2026-04-29 consolidation: 5 simple factory blocks → 1 composite. Each factory's
  // public surface is exercised in this single block; downstream tests below pin down
  // the more interesting carrier-cross-over behaviour.

  // covers: Getter.apply reads the focused value (forAll over pair),
  //   Lens.first exposes first component, Lens.second exposes second component,
  //   Lens.curried agrees with Lens.apply on get + replace,
  //   Lens.andThen reaches a nested pair component (get / replace / modify),
  //   Iso.reverse swaps the get/reverseGet directions (forAll),
  //   Prism.optional builds a Prism from a partial projection (hit / miss / reverseGet)
  "Basic factory smoke: Getter / Lens.{first,second,curried,andThen} / Iso.reverse / Prism.optional" >> {
    val getter = Getter[(Int, String), Int](_._1)
    val getterOk = forAll((s: (Int, String)) => getter.get(s) == s._1)

    // Getter ∘ Getter composes through the ordinary `andThen` (B = T = Unit, so
    // the inner T lines up with the outer B and Direct's composeFrom is Unit=>Unit).
    val composedGetter =
      Getter[((Int, String), Boolean), (Int, String)](_._1)
        .andThen(Getter[(Int, String), Int](_._1))
    val composeOk = forAll((s: ((Int, String), Boolean)) => composedGetter.get(s) == s._1._1)

    val l1 = Lens.first[Int, String]
    val l2 = Lens.second[Int, String]
    val curried = Lens.curried[(Int, Int), Int](_._1, a => s => (a, s._2))
    val applied = Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))

    val l1Ok = (l1.get((1, "a")) === 1).and(l1.replace(9)((1, "a")) === ((9, "a")))
    val l2Ok = (l2.get((1, "a")) === "a").and(l2.replace("z")((1, "a")) === ((1, "z")))
    val curriedOk = forAll { (s: (Int, Int), a: Int) =>
      curried.get(s) == applied.get(s) && curried.replace(a)(s) == applied.replace(a)(s)
    }

    type S = ((Int, Int), Int)
    val outer: Optic[S, S, (Int, Int), (Int, Int), Tuple2] =
      Lens[S, (Int, Int)](_._1, (s, a) => (a, s._2))
    val inner: Optic[(Int, Int), (Int, Int), Int, Int, Tuple2] =
      Lens[(Int, Int), Int](_._1, (s, a) => (a, s._2))
    val nested = outer.andThen(inner)
    val nestedOk = (nested.get(((1, 2), 3)) === 1)
      .and(nested.replace(9)(((1, 2), 3)) === (((9, 2), 3)))
      .and(nested.modify(_ + 100)(((1, 2), 3)) === (((101, 2), 3)))

    val longIso: Optic[Int, Int, Long, Long, Direct] =
      Iso[Int, Int, Long, Long](_.toLong, _.toInt)
    val isoRevOk = forAll((n: Int) => longIso.reverse.get(n.toLong) == n)

    val posIntPrism = Prism.optional[Int, Int](
      n => if n >= 0 then Some(n) else None,
      identity,
    )
    val prismOk = (posIntPrism.to(5) === Right(5))
      .and(posIntPrism.to(-1) === Left(-1))
      .and(posIntPrism.reverseGet(7) === 7)

    getterOk && composeOk && l1Ok.and(l2Ok) && curriedOk && nestedOk && isoRevOk && prismOk
  }

  // ----- Traversal/foldMap/modifyA shorthand --------------------------

  // covers: Traversal.each.foldMap totals the list under Monoid[Int],
  // Traversal.each.modifyA short-circuits on None, Lens morphed into an Affine
  // behaves like the original Lens
  "Traversal.each: foldMap totals + modifyA short-circuits + morph[Affine] preserves Lens behaviour" >> {
    val t: Optic[List[Int], List[Int], Int, Int, MultiFocus[PSVec]] =
      Traversal.each[List, Int]
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

  // covers: Fold.apply[List] folds all elements (forAll over List[Int]),
  //   Fold.select keeps values matching the predicate (predicate-gated Forget[Option] carrier),
  //   Optional.foldMap returns the focus when matched, empty otherwise (Affine carrier hit/miss)
  "Fold.apply[List] / Fold.select / Optional.foldMap: predicate-gated + Monoid-empty on miss" >> {
    val f: Optic[List[Int], Unit, Int, Unit, Forget[List]] = Fold[List, Int]
    val sumOk = forAll((xs: List[Int]) => f.foldMap(identity[Int])(xs) == xs.sum)

    val evenFold: Optic[Int, Unit, Int, Unit, Forget[Option]] = Fold.select[Int](_ % 2 == 0)
    val selectOk =
      forAll((n: Int) => evenFold.to(n).value == (if n % 2 == 0 then Some(n) else None))

    val evenFstOpt: Optic[(Int, Int), (Int, Int), Int, Int, Affine] =
      Optional[(Int, Int), (Int, Int), Int, Int, Tuple2](
        { case (a, b) => if a % 2 == 0 then Right(a) else Left((a, b)) },
        { case ((_, b), newA) => (newA, b) },
      )
    val foldMapOk = forAll((a: Int, b: Int) =>
      evenFstOpt.foldMap(identity[Int])((a, b)) == (if a % 2 == 0 then a else 0)
    )

    sumOk && selectOk && foldMapOk
  }

  // ----- Composer morphs from Direct --------------------------------

  // ----- Iso.morph + cross-carrier andThen via Morph.bothViaAffine ---------------
  //
  // 2026-04-29 consolidation: 2 morph-themed tests → 1 composite. Both exercise the
  // `morph[…]` machinery + the affine bridge for cross-carrier composition.

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

  // covers: Iso.morph[Tuple2] behaves like a Lens (forAll over Int),
  //   Iso.morph[Either] behaves like a Prism (.to / .reverseGet),
  //   Iso.morph[Tuple2].morph[Affine] is a valid Affine-shaped optic;
  //   Morph.bothViaAffine — Prism.andThen(Lens) composes via the affine bridge (hit + miss),
  //   Lens.andThen(Prism) composes symmetrically via the affine bridge (hit + miss)
  "Iso.morph[Tuple2/Either/Affine] + Morph.bothViaAffine: Prism↔Lens cross-carrier composition" >> {
    val doubleIso: Optic[Int, Int, Int, Int, Direct] =
      Iso[Int, Int, Int, Int](_ * 2, _ / 2)

    val asLens = doubleIso.morph[Tuple2]
    val tuple2Ok = forAll((n: Int) => asLens.get(n) == n * 2)

    val asPrism = doubleIso.morph[Either]
    val eitherOk = (asPrism.to(10) == Right(20)) && (asPrism.reverseGet(20) == 10)

    val asAffine = doubleIso.morph[Tuple2].morph[Affine]
    val affineOk = forAll((n: Int) => asAffine.to(n).affine.toOption.map(_._2) == Some(n * 2))

    val triSide: Optic[Shape3, Shape3, Int, Int, Affine] = triP.andThen(triSideL)
    val r1 = (triSide.modify(_ + 10)(Shape3.Tri(3)) == Shape3.Tri(13)) &&
      (triSide.modify(_ + 10)(Shape3.Sq(5)) == Shape3.Sq(5))

    case class Wrapper(shape: Shape3)
    val wrapperShape = Lens[Wrapper, Shape3](_.shape, (w, s) => w.copy(shape = s))
    val wrappedTri: Optic[Wrapper, Wrapper, Shape3.Tri, Shape3.Tri, Affine] =
      wrapperShape.andThen(triP)
    val r2 =
      (wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Tri(3))) ==
        Wrapper(Shape3.Tri(4))) &&
        (wrappedTri.modify(t => Shape3.Tri(t.side + 1))(Wrapper(Shape3.Sq(5))) ==
          Wrapper(Shape3.Sq(5)))

    // getOption lights up on the bare Either-carrier optic (not just the concrete Prism):
    // hit unwraps the Right branch, miss returns None.
    val getOptionOk =
      (triP.getOption(Shape3.Tri(3)) == Some(Shape3.Tri(3))) &&
        (triP.getOption(Shape3.Sq(5)) == None)

    tuple2Ok && affineOk && eitherOk && r1 && r2 && getOptionOk
  }

  // ----- Optional.readOnly behaviour -----------------------------------

  case class AdultPerson(age: Int)

  val adultAge: Optic[AdultPerson, Unit, Int, Unit, Affine] =
    Optional.readOnly(p => Option.when(p.age >= 18)(p.age))

  val adultAgeAF: AffineFold[AdultPerson, Int] =
    AffineFold(p => Option.when(p.age >= 18)(p.age))

  // covers: Optional.readOnly.foldMap folds the hit branch, returns empty on miss,
  //   Optional.selectReadOnly keeps values matching the predicate (4 even / 3 odd / 0 zero),
  //   Optional.readOnly.modifyA lifts a hit-branch read under Applicative[Option];
  //   AffineFold.apply hits on Some and misses on None,
  //   AffineFold.select keeps values matching the predicate,
  //   a writable optic's read-only view is `AffineFold(optic.getOption)` — for an Optional
  //   (drops the write path), an Either-carrier Prism (drops the build path), and a
  //   Lens-composed Optional (Wrapper.inner.age). No bespoke fromOptional/fromPrism needed.
  "Optional.readOnly + AffineFold: predicate-gated AdultPerson hit/miss across all factories" >> {
    val foldOk = (adultAge.foldMap(identity[Int])(AdultPerson(20)) === 20)
      .and(adultAge.foldMap(identity[Int])(AdultPerson(15)) === 0)

    val evenSelectAF = Optional.selectReadOnly[Int](_ % 2 == 0)
    val selOk = (evenSelectAF.foldMap(identity[Int])(4) === 4)
      .and(evenSelectAF.foldMap(identity[Int])(3) === 0)
      .and(evenSelectAF.foldMap(identity[Int])(0) === 0)

    // AffineFold is honestly one-way (B = Unit), so modifyA visits the focus producing G[Unit].
    val mfo: Int => Option[Unit] = n => Option.when(n > 0)(())
    val mAOk = (adultAge.modifyA[Option](mfo)(AdultPerson(20)) === Some(()))
      .and(adultAge.modifyA[Option](mfo)(AdultPerson(15)) === Some(()))

    val applyOk = (adultAgeAF.getOption(AdultPerson(20)) === Some(20))
      .and(adultAgeAF.getOption(AdultPerson(15)) === None)

    val evenAF = AffineFold.select[Int](_ % 2 == 0)
    val afSelOk = (evenAF.getOption(4) === Some(4)).and(evenAF.getOption(3) === None)

    val ageOpt: Optional[AdultPerson, AdultPerson, Int, Int] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val afFromOpt = AffineFold((p: AdultPerson) => ageOpt.getOption(p))
    val fromOptOk = (afFromOpt.getOption(AdultPerson(20)) === Some(20))
      .and(afFromOpt.getOption(AdultPerson(15)) === None)

    val intPrism = Prism.optional[String, Int](s => s.toIntOption, _.toString)
    val afFromPrism = AffineFold((s: String) => intPrism.getOption(s))
    val fromPrismOk = (afFromPrism.getOption("42") === Some(42))
      .and(afFromPrism.getOption("nope") === None)

    case class Wrapper(inner: AdultPerson)
    val wrapAge = Optional[Wrapper, Wrapper, Int, Int, Affine](
      getOrModify = w => Either.cond(w.inner.age >= 18, w.inner.age, w),
      reverseGet = { case (w, a) => w.copy(inner = AdultPerson(a)) },
    )
    val chained = AffineFold((w: Wrapper) => wrapAge.getOption(w))
    val chainOk = (chained.getOption(Wrapper(AdultPerson(20))) === Some(20))
      .and(chained.getOption(Wrapper(AdultPerson(15))) === None)

    foldOk
      .and(selOk)
      .and(mAOk)
      .and(applyOk)
      .and(afSelOk)
      .and(fromOptOk)
      .and(fromPrismOk)
      .and(chainOk)
  }

  // covers: the GENERAL read-only collapse — composing ANY optic with a read-only Getter projects
  //   it to its read-only counterpart (write side forgotten, T = B = Unit), routed through the
  //   Accessor / Morph machinery (not per-class fused overloads): a total reader (Lens / Iso,
  //   `Accessor[F]`) yields a Getter; a partial one (Optional / AffineFold / Prism) composes its
  //   `readOnly` projection through Morph-to-Affine and yields an AffineFold. Plus the write-side
  //   dual: any writable optic .andThen(Setter) morphs through `Composer[·, SetterF]` and stays a
  //   Setter. Not Affine-only and not per-class — one rule per side.
  "any optic .andThen(Getter) projects to read-only (Getter / AffineFold); .andThen(Setter) stays a Setter" >> {
    val toStr = Getter[Int, String](_.toString)

    // total readers -> Getter
    val fstLens = Lens[(Int, String), Int](_._1, (t, n) => (n, t._2))
    val lensThen: Getter[(Int, String), String] = fstLens.andThen(toStr)
    val lensOk = lensThen.get((7, "x")) === "7"

    val idIso = Iso[Int, Int, Int, Int](identity, identity)
    val isoThen: Getter[Int, String] = idIso.andThen(toStr)
    val isoOk = isoThen.get(9) === "9"

    // partial readers -> AffineFold
    val ageOpt: Optional[AdultPerson, AdultPerson, Int, Int] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val optThen: AffineFold[AdultPerson, String] = ageOpt.andThen(toStr)
    val optOk = (optThen.getOption(AdultPerson(20)) === Some("20"))
      .and(optThen.getOption(AdultPerson(15)) === None)

    val afThen: AffineFold[AdultPerson, String] = adultAgeAF.andThen(toStr)
    val afOk = (afThen.getOption(AdultPerson(18)) === Some("18"))
      .and(afThen.getOption(AdultPerson(10)) === None)

    val intP = Prism.optional[String, Int](s => s.toIntOption, _.toString)
    val prismThen: AffineFold[String, String] = intP.andThen(toStr)
    val prismOk =
      (prismThen.getOption("42") === Some("42")).and(prismThen.getOption("nope") === None)

    // write-side dual: any writable optic .andThen(Setter) -> Setter
    val plusSetter = Setter[Int, Int, Int, Int](f => n => f(n))
    val lensThenSet: Setter[(Int, String), (Int, String), Int, Int] =
      fstLens.andThen(plusSetter)
    val setLensOk = lensThenSet.modify(_ + 1)((7, "x")) === ((8, "x"))

    val ageOptW = Optional[AdultPerson, AdultPerson, Int, Int, Affine](
      getOrModify = p => Either.cond(p.age >= 18, p.age, p),
      reverseGet = { case (_, a) => AdultPerson(a) },
    )
    val optThenSet = ageOptW.andThen(plusSetter)
    val setOptOk = (optThenSet.modify(_ + 1)(AdultPerson(20)) === AdultPerson(21))
      .and(optThenSet.modify(_ + 1)(AdultPerson(15)) === AdultPerson(15)) // miss: unchanged

    lensOk
      .and(isoOk)
      .and(optOk)
      .and(afOk)
      .and(prismOk)
      .and(setLensOk)
      .and(setOptOk)
  }

  // ----- Review behaviour ----------------------------------------------

  // covers: Review.apply wraps an A => S build function; Review is a proper Optic
  // (the mirror of Getter), so two Reviews compose via the fused `andThen`. (There are
  // no cross-optic `fromIso`/`fromPrism` factories — an Iso/Prism already is a build
  // direction; use `Review(iso.reverseGet)` / `Review(prism.mend)` directly.)
  "Review: apply + compose (as an Optic, via andThen)" >> {
    val toSome = Review[Option[Int], Int](Some(_))
    val applyOk = (toSome.reverseGet(3) === Some(3)).and(toSome.reverseGet(-1) === Some(-1))

    val stringify = Review[Int, String](_.length)
    // build String -> Int -> Option[Int] through the fused andThen.
    val composed = toSome.andThen(stringify)
    val composeOk = composed.reverseGet("hello") === Some(5)

    applyOk.and(composeOk)
  }

  // covers: Optic.cross = self.reverse.andThen(that), structure-preserving. Review[Int,String]
  // (builds Int from String) crossed with a getter on Int gives an Optic[String,Unit,Int,Unit];
  // its `.get` reads getter.get(review.reverseGet(s)). This is the build->read seam `hylo` rides.
  "Optic.cross: Review (build) crossed with Getter reads via the composite .get" >> {
    val lengthR = Review[Int, String](_.length)
    val plusOne = Getter[Int, Int](_ + 1)
    val crossed = lengthR.cross(plusOne)
    (crossed.get("abc") === 4).and(crossed.get("hello world") === 12)
  }

  // covers: cross preserves STRUCTURE — Iso x Iso stays an Iso, so the result has BOTH a forward
  // .get AND a build .reverseGet (it does not collapse to a one-way getter). Iso's read focus A
  // survives as the composite's write-back focus.
  "Optic.cross: Iso crossed with Iso stays a full Iso (has .get and .reverseGet)" >> {
    val iso1 = Iso[Int, Int, Int, Int](_ + 1, _ - 1) // reverseGet: b => b - 1
    val iso2 = Iso[Int, Int, Int, Int](_ * 2, _ / 2) // get: a => a * 2
    val composed =
      iso1.cross(iso2) // Optic[Int, Int, Int, Int, Direct] = iso1.reverse.andThen(iso2)
    (composed.get(3) === 4) // (3-1)*2
      .and(composed.get(11) === 20) // (11-1)*2
      .and(composed.reverseGet(8) === 5) // build survives: (8/2)+1
      .and(composed.get(composed.reverseGet(8)) === 8) // round-trips
  }

  // covers: cross is genuinely CROSS-CARRIER — a Review (Direct, builds) crossed with an
  // AffineFold (Affine, partial read) bridges via Morph[Direct, Affine], so the composite is a
  // partial optic: .getOption, Some on hit / None on miss.
  "Optic.cross: cross-carrier (Review[Direct] × AffineFold[Affine]) yields a partial getOption" >> {
    val lengthR = Review[Int, String](_.length) // builds Int from String
    val bigOnly = AffineFold.select[Int](_ > 3) // keeps the focus only when > 3
    val crossed =
      lengthR.cross(bigOnly) // Optic[String,Unit,Int,Unit,Affine] — cross-carrier
    (crossed.getOption("hello") === Some(5)) // length 5 > 3
      .and(crossed.getOption("ab") === None) // length 2, filtered out
  }

  // covers: read-many falls out of cross — crossing a Review (Direct, builds) with a Fold
  // (Forget) bridges via the Composer[Direct, Forget] (sound now that Fold is honestly one-way),
  // so the composite is a Fold over everything built. .foldMap, no separate crossFold.
  "Optic.cross: Review crossed with a Fold (cross-carrier) yields a read-many Fold" >> {
    val buildList = Review[List[Int], Int](n => (1 to n).toList) // reverseGet n => [1..n]
    val sumAll = buildList.cross(Fold[List, Int]) // Optic[Int,Unit,Int,Unit,Forget[List]]
    (sumAll.foldMap[Int](identity)(3) === 6) // 1+2+3
      .and(sumAll.foldMap[Int](identity)(4) === 10) // 1+2+3+4
  }

  // ----- Forget[F] `.andThen` via assocForgetMonad ---------------------
  //
  // 2026-04-29 consolidation: 2 Forget[Option] tests → 1 composite. Both witnessed the
  // same Composer[Forget[Option], …] code path; collapsed to one composite block.

  // covers: Forget[Option] same-carrier .andThen routes through assocForgetMonad
  //   (composed.to / composed.from sweep across Some / None / overflow inputs),
  //   Forget[Option] lifts into MultiFocus[Option] via Composer and modifies under
  //   Monad[Option], cross-carrier composed MultiFocus[Option] preserves inner.X
  //   via the existential threading
  "Forget[Option] same-carrier andThen + injected into MultiFocus[Option] (Monad-driven)" >> {
    val outer = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-100)(_ * 2 + 1000),
    )
    val inner = forgetOpt[Option](
      n => Option.when(n < 1000)(n + 1),
      _.fold(-1)(_ + 1),
    )
    val sameCarrier = outer.andThen(inner)
    val sameOk = (sameCarrier.to(5).value === Some(51))
      .and(sameCarrier.to(-2).value === None)
      .and(sameCarrier.to(200).value === None)
      .and(sameCarrier.from(Forget(Some(7))) === 1016)
      .and(sameCarrier.from(Forget(None)) === 998)

    val pureForget = forgetOpt[Option](
      n => Option.when(n > 0)(n * 10),
      _.fold(-1)(_ + 7),
    )
    val lifted = summon[Composer[Forget[Option], MultiFocus[Option]]].to(pureForget)
    val liftedOk = (lifted.modify(_ + 1)(5) === 58).and(lifted.modify(_ + 1)(-2) === -1)

    val inner2: Optic[Int, Int, Int, Int, MultiFocus[Option]] =
      new Optic[Int, Int, Int, Int, MultiFocus[Option]]:
        type X = String
        def to(n: Int): MultiFocus[Option][X, Int] =
          MultiFocus(s"tag-$n", Option.when(n < 1000)(n + 1))
        def from(pair: MultiFocus[Option][X, Int]): Int =
          pair.foci.fold(pair.context.length)(_ * 100 + pair.context.length)
    val composed = lifted.andThen(inner2)
    val composedOk =
      (composed.modify(identity)(5) === 5113).and(composed.modify(identity)(-2) === -1)

    sameOk.and(liftedOk).and(composedOk)
  }

  // ----- Lens / Prism / Optional → MultiFocus[List] / SetterF cross-carrier ----------------
  //
  // 2026-04-29 consolidation: dropped a standalone case class `AdultOptCarrier` (was unused).

  case class AdultOptCarrier(p: AdultPerson)

  // ----- Prism / Optional / Traversal / MultiFocus.tuple / MultiFocus.representable → SetterF
  //
  // 2026-04-29 consolidation: 2 SetterF-themed blocks → 1 composite. Both witness the
  // canonical "lift into SetterF + .modify byte-for-byte agrees with the source carrier".

  // covers: Lens(Tuple2) lifts into MultiFocus[List] preserving .modify semantics,
  //   Either Prism lifts into MultiFocus[List] preserves hit/miss,
  //   Affine Optional lifts into MultiFocus[List] preserves hit/miss,
  //   Optional.andThen(Forget[List]→MultiFocus[List]) classifier composes via affine2multifocus;
  //   Either Prism lifts into SetterF and preserves hit/miss,
  //   Affine Optional lifts into SetterF and preserves hit/miss,
  //   PowerSeries (MultiFocus[PSVec]) Traversal lifts into SetterF and applies f to every focus,
  //   MultiFocus.tuple lifts into SetterF and rebroadcasts via per-slot rebuild,
  //   MultiFocus.representable over Representable[Function1[Boolean, *]] lifts identically
  "Lens/Prism/Optional → MultiFocus[List] + → SetterF: cross-carrier lifts (one composite block)" >> {
    // ---- → MultiFocus[List] half (absorbed standalone test) ----
    val fstLens: Optic[(Int, String), (Int, String), Int, Int, Tuple2] =
      Lens[(Int, String), Int](_._1, (s, a) => (a, s._2))
    val tuple2LiftedMF = summon[Composer[Tuple2, MultiFocus[List]]].to(fstLens)
    val tuple2MFOk = (tuple2LiftedMF.modify(_ + 100)((3, "hi")) === ((103, "hi")))
      .and(tuple2LiftedMF.modify(_ * 2)((5, "x")) === ((10, "x")))

    val oddP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 1 then Right(n) else Left(n), identity)
    val eitherLiftedMF = summon[Composer[Either, MultiFocus[List]]].to(oddP)
    val eitherMFOk =
      (eitherLiftedMF.modify(_ * 2)(3) === 6).and(eitherLiftedMF.modify(_ * 2)(4) === 4)

    val adultOpt: Optic[AdultPerson, AdultPerson, Int, Int, Affine] =
      Optional[AdultPerson, AdultPerson, Int, Int, Affine](
        getOrModify = p => Either.cond(p.age >= 18, p.age, p),
        reverseGet = { case (_, a) => AdultPerson(a) },
      )
    val affineLiftedMF = summon[Composer[Affine, MultiFocus[List]]].to(adultOpt)
    val affineMFOk = (affineLiftedMF.modify(_ + 1)(AdultPerson(25)) === AdultPerson(26))
      .and(affineLiftedMF.modify(_ + 1)(AdultPerson(12)) === AdultPerson(12))

    val posOpt: Optic[Int, Int, Int, Int, Affine] =
      Optional[Int, Int, Int, Int, Affine](
        getOrModify = n => Either.cond(n > 0, n, n),
        reverseGet = { case (_, n) => n },
      )
    val candidatesMF = summon[Composer[Forget[List], MultiFocus[List]]].to(
      forgetOpt[List](n => List(n, n * 2), _.sum)
    )
    val composedMF = posOpt.andThen(candidatesMF)
    val composedMFOk =
      (composedMF.modify(_ + 1)(3) === 11).and(composedMF.modify(_ + 1)(-1) === -1)

    // ---- → SetterF half ----
    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    val eitherLifted: Optic[Int, Int, Int, Int, data.SetterF] =
      summon[Composer[Either, data.SetterF]].to(evenP)
    val eitherOk =
      (eitherLifted.modify(_ + 10)(4) === 14).and(eitherLifted.modify(_ + 10)(5) === 5)

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

    tuple2MFOk
      .and(eitherMFOk)
      .and(affineMFOk)
      .and(composedMFOk)
      .and(eitherOk)
      .and(affineOk)
      .and(psOk)
      .and(tupleOk)
      .and(fnOk)
  }

  // ----- MultiFocus[F] lifted into SetterF + Foldable-aggregated escape ----------------
  //
  // 2026-04-29 consolidation: 2 MultiFocus[F]→SetterF / read-only-escape blocks → 1.

  // covers: MultiFocus[F] read-only escape via .foldMap (sum / count / empty) — the gap-#2
  //   closure shipped two ways: (a) extension methods (.foldMap / .headOption / .length / .exists)
  //   on every `ForgetfulFold[F]`-bearing carrier; (b) `multifocus2forget[F]: Composer[MultiFocus[F],
  //   Forget[F]]` — the explicit carrier morph, defensive about the bidirectional pair with
  //   `forget2multifocus` (works when the user routes via `summon[Composer[..]].to(o)` rather
  //   than `.andThen` to avoid the Morph-resolution ambiguity).
  //   Plus: MultiFocus.apply[List] / MultiFocus.apply[ZipList] → SetterF element-wise modify.
  "MultiFocus[F] read-only escape (foldMap, → Forget[F]) + List/ZipList → SetterF" >> {
    val listMF: Optic[List[Int], List[Int], Int, Int, MultiFocus[List]] =
      MultiFocus.apply[List, Int]

    val sumOk = listMF.foldMap(identity[Int])(List(1, 2, 3, 4)) === 10
    val sizeOk = listMF.foldMap(_ => 1)(List(1, 2, 3)) === 3
    val emptyOk = listMF.foldMap(identity[Int])(Nil) === 0

    // multifocus2forget Composer — explicit morph from MultiFocus[List] → Forget[List].
    val asFold: Optic[List[Int], List[Int], Int, Int, Forget[List]] =
      summon[Composer[MultiFocus[List], Forget[List]]].to(listMF)
    val foldReadOk = asFold.to(List(7, 8, 9)) === List(7, 8, 9)

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

    sumOk.and(sizeOk).and(emptyOk).and(foldReadOk).and(listOk).and(zipOk)
  }

  // ----- SetterF same-carrier composition (gap #4) ---------------------
  //
  // SetterF can't ship `AssociativeFunctor[SetterF]` because the deferred-modify
  // semantic doesn't fit composeTo/composeFrom. Instead, `SetterF.scala` ships
  // a SetterF-specific `.andThen` extension that composes via direct function
  // composition. Scala 3 picks the more-specific extension over the carrier-
  // generic `Optic.andThen[F[_, _]]` whenever both sides are concretely SetterF.

  // covers: Lens lifted to SetterF .andThen Lens lifted to SetterF — composed
  //   .modify agrees with sequential modify through both;
  //   Lens-into-SetterF .andThen Prism-into-SetterF — composed hit modifies
  //   the inner focus; composed miss leaves the source unchanged;
  //   Lens-into-SetterF .andThen Optional-into-SetterF — same hit/miss shape
  //   under the Affine carrier
  "SetterF same-carrier composition: setter.andThen(setter)" >> {
    case class Inner(value: Int)
    case class Outer(inner: Inner, tag: String)

    val outerLens: Optic[Outer, Outer, Inner, Inner, Tuple2] =
      Lens[Outer, Inner](_.inner, (s, i) => s.copy(inner = i))
    val innerLens: Optic[Inner, Inner, Int, Int, Tuple2] =
      Lens[Inner, Int](_.value, (s, v) => s.copy(value = v))

    val outerSf: Optic[Outer, Outer, Inner, Inner, SetterF] =
      summon[Composer[Tuple2, SetterF]].to(outerLens)
    val innerSf: Optic[Inner, Inner, Int, Int, SetterF] =
      summon[Composer[Tuple2, SetterF]].to(innerLens)

    val composed: Optic[Outer, Outer, Int, Int, SetterF] = outerSf.andThen(innerSf)

    val src = Outer(Inner(10), "tag")
    val lensLensOk =
      (composed.modify(_ + 1)(src) === Outer(Inner(11), "tag"))
        .and(composed.modify(_ * 2)(src) === Outer(Inner(20), "tag"))
        .and(composed.modify(identity[Int])(src) === src)

    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    val evenSf: Optic[Int, Int, Int, Int, SetterF] =
      summon[Composer[Either, SetterF]].to(evenP)

    val outerThenEven: Optic[Outer, Outer, Int, Int, SetterF] =
      outerSf.andThen(innerSf).andThen(evenSf)
    val lensPrismHit = outerThenEven.modify(_ + 100)(Outer(Inner(4), "x"))
    val lensPrismMiss = outerThenEven.modify(_ + 100)(Outer(Inner(5), "x"))
    val lensPrismOk =
      (lensPrismHit === Outer(Inner(104), "x"))
        .and(lensPrismMiss === Outer(Inner(5), "x"))

    val posOpt: Optic[Int, Int, Int, Int, Affine] =
      Optional[Int, Int, Int, Int, Affine](
        getOrModify = n => Either.cond(n > 0, n, n),
        reverseGet = { case (_, n) => n },
      )
    val posSf: Optic[Int, Int, Int, Int, SetterF] =
      summon[Composer[Affine, SetterF]].to(posOpt)

    val outerThenPos: Optic[Outer, Outer, Int, Int, SetterF] =
      outerSf.andThen(innerSf).andThen(posSf)
    val lensOptHit = outerThenPos.modify(_ * 10)(Outer(Inner(3), "y"))
    val lensOptMiss = outerThenPos.modify(_ * 10)(Outer(Inner(-3), "y"))
    val lensOptOk =
      (lensOptHit === Outer(Inner(30), "y"))
        .and(lensOptMiss === Outer(Inner(-3), "y"))

    lensLensOk.and(lensPrismOk).and(lensOptOk)
  }

  // ----- same-F Fold composition via the trait andThen -----------------
  //
  // Now that Fold is honestly one-way (B = Unit), Fold × Fold composes through the ordinary trait
  // `Optic.andThen` (the seam unifies at Unit/Unit, driven by assocForgetMonad's flatMap) — no
  // bespoke Forget extension. covers: outer Fold[List] andThen inner Fold[List] flatMaps.
  "Fold[List].andThen(Fold[List]) via trait andThen flatMaps the two folds" >> {
    val triplet: Optic[Int, Unit, Int, Unit, Forget[List]] =
      new Optic[Int, Unit, Int, Unit, Forget[List]]:
        type X = Unit
        def to(n: Int): Forget[List][Unit, Int] = Forget(List(n - 1, n, n + 1))
        def from(u: Forget[List][Unit, Unit]): Unit = ()

    val pair: Optic[Int, Unit, Int, Unit, Forget[List]] =
      new Optic[Int, Unit, Int, Unit, Forget[List]]:
        type X = Unit
        def to(n: Int): Forget[List][Unit, Int] = Forget(List(n, n + 1))
        def from(u: Forget[List][Unit, Unit]): Unit = ()

    val composed = triplet.andThen(pair)
    // to(5) = List(4,5,6).flatMap(n => List(n, n+1)) = List(4,5,5,6,6,7)
    composed.to(5) === List(4, 5, 5, 6, 6, 7)
  }

  // ----- F[A]-focus factories -----------------------------------------

  // covers: MultiFocus.fromLensF rewraps a Lens[S, F[A]] with zero constraints
  //   (Table.rows modify per-element, empty list short-circuits),
  //   MultiFocus.fromPrismF lifts a Prism[S, F[A]] with MonoidK[F] only (Some/None/empty branches),
  //   MultiFocus.fromOptionalF lifts an Optional[S, F[A]] through the Hit branch;
  //   mfFold cardinality+miss — fromLensF empty list folds to Monoid.empty,
  //   fromLensF multi-element list sums each element exactly once,
  //   fromPrismF None / Some(Nil) miss branches fold to Monoid.empty,
  //   fromPrismF Some(non-empty) hit branch sums correctly
  "MultiFocus.fromLensF / fromPrismF / fromOptionalF — F[A]-focus factories + mfFold cardinality" >> {
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

    // ---- mfFold cardinality + miss (absorbed standalone test) ----
    val listLens: Optic[List[Int], List[Int], List[Int], List[Int], Tuple2] =
      Lens[List[Int], List[Int]](identity, (_, b) => b)
    val mfFromLens = MultiFocus.fromLensF(listLens)
    val foldEmptyOk = mfFromLens.foldMap(identity[Int])(Nil) === 0
    val foldMultiOk = mfFromLens.foldMap(identity[Int])(List(2, 3, 5, 7, 11)) ===
      (2 + 3 + 5 + 7 + 11)
    val foldMissNone = mf.foldMap(identity[Int])(None) === 0
    val foldMissEmpty = mf.foldMap(identity[Int])(Some(Nil)) === 0
    val foldHit = mf.foldMap(identity[Int])(Some(List(10, 20, 30))) === 60

    lensFOk
      .and(prismFOk)
      .and(optionalFOk)
      .and(foldEmptyOk)
      .and(foldMultiOk)
      .and(foldMissNone)
      .and(foldMissEmpty)
      .and(foldHit)
  }

  // ----- Cross-carrier composition: Lens → MultiFocus[List] chains -----------------
  //
  // 2026-04-29 consolidation: dropped the standalone "Lens andThen MultiFocus[List] classifier"
  // 2-hop test — its Lens(Row.value).andThen(forget2multifocus) coverage is a strict subset of
  // the 3-hop chain below (which threads three carriers Lens → MultiFocus[List] → Lens).

  // covers: Lens(Wrapper.doc) → MultiFocus.fromLensF(Doc.tags) → Lens(Tag.count) chains three
  //   carriers cleanly, multi-element rebuild preserved, empty list short-circuits to id;
  //   Lens.andThen(MultiFocus.classifier) propagates modify-multiplier under non-uniform
  //   element counts (subsumes the 2-hop "Lens andThen MultiFocus[List] classifier" test);
  //   Two Forget[List] classifiers (different non-uniform cardinalities — outer = 2 elements,
  //   inner = 3 elements) compose via MultiFocus[List] (non-singleton × non-singleton),
  //   foldMap aggregates across the cartesian product, modify(identity) preserves the per-branch
  //   product, modify(f) applies through both layers
  "Lens → MultiFocus[List] → Lens composes three carriers cleanly + non-singleton×non-singleton" >> {
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

    // ---- Non-singleton × non-singleton MultiFocus[List] composition ----
    val outerForget = forgetOpt[List](n => List(n, n + 1), _.sum)
    val innerForget = forgetOpt[List](n => List(n * 10, n * 10 + 1, n * 10 + 2), _.product)
    val outerMF = summon[Composer[Forget[List], MultiFocus[List]]].to(outerForget)
    val innerMF = summon[Composer[Forget[List], MultiFocus[List]]].to(innerForget)
    val composedNN = outerMF.andThen(innerMF)
    val nonUniformOk = (composedNN.foldMap(identity[Int])(5) === (50 + 51 + 52 + 60 + 61 + 62))
      .and(composedNN.modify(identity)(5) === (50 * 51 * 52 + 60 * 61 * 62))
      .and(composedNN.modify(_ + 1)(5) === (51 * 52 * 53 + 61 * 62 * 63))

    r1.and(r2).and(nonUniformOk)
  }

  // ----- MultiFocusSingleton tag regression + Prism→Prism miss-survival + counter ---
  //
  // 2026-04-29 consolidation: 3 singleton-related blocks → 1 composite.

  // covers: tuple2multifocus mixes in MultiFocusSingleton; either2multifocus, fromLensF,
  //   forget2multifocus do NOT mix it in;
  //   Prism.andThen(Prism) via MultiFocus[List] survives inner miss on an outer hit;
  //   MultiFocusSingleton fast-path calls inner.to exactly once per outer element
  "MultiFocus singleton: tag matrix + Prism∘Prism miss-survival + fast-path counter" >> {
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

    val evenP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n % 2 == 0 then Right(n) else Left(n), identity)
    val positiveP: Optic[Int, Int, Int, Int, Either] =
      Prism[Int, Int](n => if n > 0 then Right(n) else Left(n), identity)
    val evenMF = summon[Composer[Either, MultiFocus[List]]].to(evenP)
    val positiveMF = summon[Composer[Either, MultiFocus[List]]].to(positiveP)
    val composed = evenMF.andThen(positiveMF)
    val missSurviveOk = (composed.modify(_ + 10)(4) === 14)
      .and(composed.modify(_ + 10)(-2) === -2)
      .and(composed.modify(_ + 10)(3) === 3)

    import java.util.concurrent.atomic.AtomicInteger
    val counter = new AtomicInteger(0)
    val baseLens: Optic[Int, Int, Int, Int, Tuple2] =
      Lens[Int, Int](
        get = n => { counter.incrementAndGet(); n * 10 },
        enplace = (_, b) => b / 10,
      )
    val liftedA = summon[Composer[Tuple2, MultiFocus[List]]].to(baseLens)
    val liftedB = summon[Composer[Tuple2, MultiFocus[List]]].to(baseLens)
    val counterChain = liftedA.andThen(liftedB)
    counter.set(0)
    counterChain.modify(identity)(5)
    val counterOk = counter.get === 2

    tagOk1.and(tagOk2).and(tagOk3).and(tagOk4).and(missSurviveOk).and(counterOk)
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

  // ----- ForgetfulFold extension methods: headOption / length / exists -----
  //
  // covers: .headOption (first focus), .length (focus count), .exists (predicate
  // over foci) on three carrier shapes — Lens (Tuple2 = always 1 focus), Prism
  // (Either = 0 or 1 focus), Traversal (MultiFocus[PSVec] = 0..n foci) — and the
  // Fold-carrier read shape (Forget[List]).
  "ForgetfulFold extensions: headOption / length / exists across Lens / Prism / Traversal / Fold" >> {
    val ageL: Optic[(String, Int), (String, Int), Int, Int, Tuple2] =
      Lens(_._2, (s, a) => (s._1, a))
    val lensOk = (ageL.headOption(("Alice", 30)) === Some(30))
      .and(ageL.length(("Alice", 30)) === 1)
      .and(ageL.exists((_: Int) > 18)(("Alice", 30)) === true)
      .and(ageL.exists((_: Int) > 100)(("Alice", 30)) === false)

    val posIntPrism: Optic[Int, Int, Int, Int, Either] =
      Prism.optional[Int, Int](n => if n >= 0 then Some(n) else None, identity)
    val prismOk = (posIntPrism.headOption(5) === Some(5))
      .and(posIntPrism.headOption(-1) === None)
      .and(posIntPrism.length(5) === 1)
      .and(posIntPrism.length(-1) === 0)
      .and(posIntPrism.exists((n: Int) => n > 3)(5) === true)
      .and(posIntPrism.exists((n: Int) => n > 3)(-1) === false)

    val each = Traversal.each[List, Int]
    val travOk = (each.headOption(List(7, 8, 9)) === Some(7))
      .and(each.headOption(List.empty[Int]) === None)
      .and(each.length(List(1, 2, 3, 4, 5)) === 5)
      .and(each.length(List.empty[Int]) === 0)
      .and(each.exists((n: Int) => n > 3)(List(1, 2, 3, 4)) === true)
      .and(each.exists((n: Int) => n > 99)(List(1, 2, 3, 4)) === false)

    val listFold = Fold[List, Int]
    val foldOk = (listFold.headOption(List(10, 20, 30)) === Some(10))
      .and(listFold.length(List(10, 20, 30)) === 3)
      .and(listFold.exists((n: Int) => n == 20)(List(10, 20, 30)) === true)

    lensOk.and(prismOk).and(travOk).and(foldOk)
  }
