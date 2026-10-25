package dev.constructive.eo

import cats.{Eq, Traverse}
import cats.data.State
import cats.kernel.laws.IsEq
import cats.laws.TraverseLaws
import cats.laws.discipline.TraverseTests
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.mutable.Specification
import org.typelevel.discipline.Laws
import org.typelevel.discipline.specs2.mutable.Discipline

import optics.Traversal
import optics.Optic.*
import data.PSVec

/** Laws for the hand-written `Traverse[PSVec]` — the instance every `Traversal.each(...).modifyA`
  * (and every other `MultiFocus[PSVec]` `.modifyA` / `.all`) runs through.
  *
  * The original bug: `traverse` threaded ONE mutable result array through `G.map2`, so any effect
  * that resumes a continuation more than once handed every resumption the same array and the last
  * writer won — `List(1, 2, 3)` under `a => List(a, -a)` gave 8 copies of `List(-1, -2, -3)`. cats'
  * own `TraverseTests` only admits `CommutativeApplicative` effects (`Option`, `Id`, …), which are
  * single-shot; it caught the bug only through its parallel-composition law (the product of two
  * effects resumes each continuation once per component). So the second rule set re-states cats'
  * `TraverseLaws` with the multi-shot `List` / `Vector` as effects, adds differential checks
  * against cats' `Traverse[List]` / `Traverse[Vector]` (eager `List`, depth-first `LazyList`) and a
  * re-run check (`State`), and the last block checks the optic surface (`Traversal.each`, alone and
  * composed) the same way.
  */
class PSVecTraverseLawsSpec extends Specification with Discipline:

  // Small vectors on purpose: a multi-shot effect's result size is the PRODUCT of the per-element
  // effect sizes, so `n` elements under a 0..3-wide effect is up to 4^n branches (and the
  // sequential-composition law nests two such effects).
  private val smallInts: Gen[List[Int]] =
    Gen.choose(0, 5).flatMap(Gen.listOfN(_, Gen.choose(-9, 9)))

  // Every PSVec variant: Empty, Single, a dense Slice, and an OFFSET Slice (a view into a larger
  // backing array) — the last is what `mfAssocPSVec.composeFrom` hands inner traversals.
  private def psvecOf[A](as: Gen[List[A]], pad: Gen[A]): Gen[PSVec[A]] =
    for
      xs <- as
      pre <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, pad))
      post <- Gen.choose(0, 2).flatMap(Gen.listOfN(_, pad))
    yield PSVec.from(pre ++ xs ++ post).slice(pre.length, pre.length + xs.length)

  private given arbPSVec[A: Arbitrary]: Arbitrary[PSVec[A]] =
    Arbitrary(
      psvecOf(
        Gen.choose(0, 5).flatMap(Gen.listOfN(_, Arbitrary.arbitrary[A])),
        Arbitrary.arbitrary[A],
      )
    )

  // PSVec equality is value-based across variants, so `==` is a lawful Eq.
  private given eqPSVec[A]: Eq[PSVec[A]] = Eq.fromUniversalEquals

  // ----- cats' discipline rule set (commutative effects only) --------------

  checkAll(
    "Traverse[PSVec] (cats TraverseTests)",
    TraverseTests[PSVec].traverse[Int, Int, Int, Int, Option, Option],
  )

  // ----- the same laws under multi-shot effects ----------------------------

  // Bounded-width effects: each focus fans out to 0..3 branches (0 exercises the empty-product
  // short-circuit, 1 the single-shot path, 2..3 the multi-shot path the bug lived in).
  private def width[F[_]](mk: List[Int] => F[Int]): Gen[Int => F[Int]] =
    Gen.function1(Gen.choose(0, 3).flatMap(Gen.listOfN(_, Gen.choose(-9, 9))).map(mk))

  private val listF: Gen[Int => List[Int]] = width(identity)
  private val vectorF: Gen[Int => Vector[Int]] = width(_.toVector)
  private val vecs: Gen[PSVec[Int]] = psvecOf(smallInts, Gen.choose(-9, 9))

  private def holds[A](e: IsEq[A]): Prop =
    Prop(e.lhs == e.rhs) :| s"lhs = ${e.lhs}\nrhs = ${e.rhs}"

  private object multiShot extends Laws:
    private val L = TraverseLaws[PSVec]

    def rules: RuleSet = new DefaultRuleSet(
      "traverse under multi-shot and re-run effects",
      None,
      "traverse identity (List)" -> forAll(vecs)(fa =>
        // `traverseIdentity` is stated over `Id`; the multi-shot analogue is that `pure`
        // continuations (one branch each) rebuild the input exactly.
        holds(IsEq(Traverse[PSVec].traverse(fa)(a => List(a)), List(fa)))
      ),
      "traverse sequential composition (List then Vector)" -> forAll(vecs, listF, vectorF)(
        (fa, f, g) => holds(L.traverseSequentialComposition[Int, Int, Int, List, Vector](fa, f, g))
      ),
      "traverse parallel composition (List, Vector)" -> forAll(vecs, listF, vectorF)((fa, f, g) =>
        holds(L.traverseParallelComposition[Int, Int, List, Vector](fa, f, g))
      ),
      "traverseTap (List)" -> forAll(vecs, listF)((fa, f) =>
        holds(L.traverseTap[Int, Int, List](fa, f))
      ),
      "agrees with Traverse[List] (G = List)" -> forAll(vecs, listF)((fa, f) =>
        holds(
          IsEq(
            Traverse[PSVec].traverse(fa)(f).map(_.toList),
            Traverse[List].traverse(fa.toList)(f),
          )
        )
      ),
      // LazyList forces branches depth-first, so a later branch at index `i` meets a buffer whose
      // first branch has ALREADY run on past `i` — the frozen-prefix copy, not the in-step one.
      "agrees with Traverse[List] (G = LazyList, depth-first)" -> forAll(vecs, listF)((fa, f) =>
        holds(
          IsEq(
            Traverse[PSVec].traverse(fa)(a => f(a).to(LazyList)).map(_.toList).toList,
            Traverse[List].traverse(fa.toList)(f),
          )
        )
      ),
      // Re-running one `G` value re-runs its continuations: an earlier run's result must survive
      // a later run unchanged (a buffer seeded once, outside the effect, would be overwritten).
      "re-running the effect leaves earlier results intact (G = State)" -> forAll(vecs)(fa =>
        val prog = Traverse[PSVec].traverse(fa)(a => State((s: Int) => (s + 1, a + s)))
        val first = prog.runA(0).value
        val snapshot = first.toList
        val second = prog.runA(100).value
        holds(
          IsEq(
            (first.toList, second.toList),
            (snapshot, fa.toList.zipWithIndex.map((a, i) => a + 100 + i)),
          )
        )
      ),
      "agrees with Traverse[Vector] (G = Vector)" -> forAll(vecs, vectorF)((fa, f) =>
        holds(
          IsEq(
            Traverse[PSVec].traverse(fa)(f).map(_.iterator.to(Vector)),
            Traverse[Vector].traverse(fa.iterator.to(Vector))(f),
          )
        )
      ),
    )

  checkAll("Traverse[PSVec]", multiShot.rules)

  // ----- the optic surface: modifyA through Traversal.each -----------------

  private val nested: Gen[List[List[Int]]] =
    Gen
      .choose(0, 3)
      .flatMap(Gen.listOfN(_, Gen.choose(0, 2).flatMap(Gen.listOfN(_, Gen.choose(-9, 9)))))

  private object eachModifyA extends Laws:
    private val listEach = Traversal.each[List, Int]
    private val vectorEach = Traversal.each[Vector, Int]
    private val nestedEach = Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int])

    def rules: RuleSet = new DefaultRuleSet(
      "Traversal.each(...).modifyA agrees with cats' traverse",
      None,
      "each[List].modifyA[List]" -> forAll(smallInts, listF)((xs, f) =>
        holds(IsEq(listEach.modifyA[List](f)(xs), Traverse[List].traverse(xs)(f)))
      ),
      "each[Vector].modifyA[Vector]" -> forAll(smallInts, vectorF)((xs, f) =>
        val v = xs.toVector
        holds(IsEq(vectorEach.modifyA[Vector](f)(v), Traverse[Vector].traverse(v)(f)))
      ),
      // `each ∘ each` reassembles every branch through `mfAssocPSVec.composeFrom`'s offset slices.
      "(each ∘ each).modifyA[List]" -> forAll(nested, listF)((xss, f) =>
        holds(
          IsEq(
            nestedEach.modifyA[List](f)(xss),
            Traverse[List].traverse(xss)(Traverse[List].traverse(_)(f)),
          )
        )
      ),
    )

  checkAll("Traversal.each", eachModifyA.rules)

  "each[List].modifyA[List] returns every combination, not one shared result" >> {
    // The reported case: 2^3 DISTINCT sign patterns, in cats' order.
    val got = Traversal.each[List, Int].modifyA[List](a => List(a, -a))(List(1, 2, 3))
    (got === Traverse[List].traverse(List(1, 2, 3))(a => List(a, -a)))
      .and(got.distinct.size === 8)
  }
