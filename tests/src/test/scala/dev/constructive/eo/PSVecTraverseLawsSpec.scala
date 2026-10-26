package dev.constructive.eo

import cats.data.State
import cats.kernel.laws.IsEq
import cats.laws.discipline.TraverseTests
import cats.{Eq, Traverse}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.mutable.Specification
import org.typelevel.discipline.Laws

import optics.Traversal
import optics.Optic.*
import data.{MultiFocus, PSVec}
import laws.eo.ModifyAListLaws
import laws.eo.discipline.ModifyAListTests

/** Laws for the hand-written `Traverse[PSVec]` — the instance every `Traversal.each(...).modifyA`
  * (and every other `MultiFocus[PSVec]` `.modifyA`) runs through.
  *
  * The original bug: `traverse` threaded ONE mutable result array through `G.map2`, so any effect
  * that resumes a continuation more than once handed every resumption the same array and the last
  * writer won — `List(1, 2, 3)` under `a => List(a, -a)` gave 8 copies of `List(-1, -2, -3)`. cats'
  * own `TraverseTests` only admits `CommutativeApplicative` effects (`Option`, `Id`, …), which are
  * single-shot; it caught the bug only through its parallel-composition law. The multi-shot laws
  * now live in the shared kit (`ForgetfulTraverseTests.forgetfulTraverseMultiShot`, D2
  * `ModifyAListTests`); this spec runs them over every PSVec variant and adds what is
  * PSVec-specific: the depth-first (`LazyList`) and re-run (`State`) effects that reach the
  * buffer's copy paths.
  */
class PSVecTraverseLawsSpec extends Specification with CheckAllHelpers:

  // Small vectors on purpose: a multi-shot effect's result size is the PRODUCT of the per-element
  // effect sizes, so `n` elements under a 0..3-wide effect is up to 4^n branches.
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

  private val vecs: Gen[PSVec[Int]] = psvecOf(smallInts, Gen.choose(-9, 9))

  // ----- cats' discipline rule set (commutative effects only) --------------

  checkAll(
    "Traverse[PSVec] (cats TraverseTests)",
    TraverseTests[PSVec].traverse[Int, Int, Int, Int, Option, Option],
  )

  // ----- eo's ForgetfulTraverse laws over every PSVec variant ---------------

  // The generic multi-shot laws (Nested / Tuple2K composition, agreement with Traverse[List]) are
  // registered for MultiFocus[PSVec] in OpticsLawsSpec too, but on a dense 3-slot Slice only.
  private given arbMultiFocusPSVec: Arbitrary[MultiFocus[PSVec][Int, Int]] =
    Arbitrary(Arbitrary.arbitrary[Int].flatMap(x => vecs.map(MultiFocus(x, _))))

  checkAllForgetfulTraverseFor[MultiFocus[PSVec], Int, Int](
    "ForgetfulTraverse[MultiFocus[PSVec]] on Empty / Single / Slice / offset Slice"
  )

  // `each ∘ each` reassembles every branch through `mfAssocPSVec.composeFrom`'s offset slices.
  locally {
    given Arbitrary[List[List[Int]]] = Arbitrary(
      Gen
        .choose(0, 3)
        .flatMap(Gen.listOfN(_, Gen.choose(0, 2).flatMap(Gen.listOfN(_, Gen.choose(-9, 9)))))
    )
    checkAll(
      "(each ∘ each) modifyA[List] ≡ cartesian traverse",
      new ModifyAListTests[List[List[Int]], Int, MultiFocus[PSVec]]:
        val laws = new ModifyAListLaws[List[List[Int]], Int, MultiFocus[PSVec]]:
          val optic = Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int])
      .modifyAList,
    )
    checkAllFoldMapHomomorphismFor[List[List[Int]], Int, MultiFocus[PSVec]](
      "(each ∘ each) foldMap homomorphism; all ≡ foldMap(List(_))",
      Traversal.each[List, List[Int]].andThen(Traversal.each[List, Int]),
    )
  }

  // ----- PSVec-specific: depth-first and re-run effects ----------------------

  // Each focus fans out to 0..3 branches.
  private val listF: Gen[Int => List[Int]] =
    Gen.function1(Gen.choose(0, 3).flatMap(Gen.listOfN(_, Gen.choose(-9, 9))))

  private def holds[A](e: IsEq[A]): Prop =
    Prop(e.lhs == e.rhs) :| s"lhs = ${e.lhs}\nrhs = ${e.rhs}"

  private object effects extends Laws:

    def rules: RuleSet = new SimpleRuleSet(
      "traverse under depth-first and re-run effects",
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
    )

  checkAll("Traverse[PSVec]", effects.rules)

  "each[List].modifyA[List] returns every combination, not one shared result" >> {
    // The reported case: 2^3 DISTINCT sign patterns, in cats' order.
    val got = Traversal.each[List, Int].modifyA[List](a => List(a, -a))(List(1, 2, 3))
    (got === Traverse[List].traverse(List(1, 2, 3))(a => List(a, -a)))
      .and(got.distinct.size === 8)
  }
