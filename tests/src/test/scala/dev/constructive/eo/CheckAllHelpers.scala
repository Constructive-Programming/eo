package dev.constructive.eo

import cats.{Applicative, Functor, Monoid}
import dev.constructive.eo.accessor.*
import dev.constructive.eo.forgetful.*
import org.scalacheck.{Arbitrary, Cogen}
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.core.Fragment
import org.typelevel.discipline.specs2.mutable.Discipline

import data.{MultiFocus, PSVec}
import laws.{FoldLaws, MultiFocusLaws, TraversalLaws}
import laws.discipline.{FoldTests, MultiFocusTests, TraversalTests}
import laws.eo.{
  ComposedFoldMapLaws,
  ComposedGetLaws,
  ComposedGetOptionLaws,
  ComposedReverseGetLaws,
  FoldMapHomomorphismLaws,
  MorphLaws
}
import laws.eo.discipline.{
  ComposedFoldMapTests,
  ComposedGetOptionTests,
  ComposedGetTests,
  ComposedReverseGetTests,
  FoldMapHomomorphismTests,
  MorphTests
}
import laws.typeclass.{ForgetfulFunctorLaws, ForgetfulTraverseLaws}
import laws.typeclass.discipline.{ForgetfulFunctorTests, ForgetfulTraverseTests}
import optics.Optic
import optics.Optic.*

/** Path B helpers — collapse the highly repetitive `checkAll(...)` patterns in `OpticsLawsSpec` and
  * `EoSpecificLawsSpec` into one-liners.
  *
  * Each helper takes a single carrier / arity instantiation worth of inputs (typed-evidence and a
  * descriptive name) and feeds it to the matching `*Tests` runner. The original carrier the call
  * site exercises stays auditable via a `// covers: ...` comment at each invocation.
  *
  * The trait composes on top of `Discipline` so callers can mix `with CheckAllHelpers` instead of
  * `with Discipline` directly — the `checkAll` method is inherited transitively. Helpers return the
  * `Fragment` produced by `checkAll` to silence Scala 3's `-Wnonunit-statement` discard warning at
  * the call site (the `Fragment` is registered as a side-effect on the spec's mutable fragment list
  * — its return value is informational).
  */
trait CheckAllHelpers extends Discipline:
  self: SpecificationLike =>

  // ===== ForgetfulFunctor / ForgetfulTraverse over an `F[_, _]` carrier =====

  /** Runs `ForgetfulFunctorTests` for carrier `F` against the supplied Arbitrary/Cogen evidence. */
  def checkAllForgetfulFunctorFor[F[_, _], X, A](name: String)(using
      Arbitrary[F[X, A]],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[F],
  ): Fragment =
    checkAll(
      name,
      new ForgetfulFunctorTests[F, X, A]:
        val laws = new ForgetfulFunctorLaws[F, X, A] {}
      .forgetfulFunctor,
    )

  /** Runs `ForgetfulTraverseTests` for carrier `F` against the supplied Arbitrary evidence. */
  def checkAllForgetfulTraverseFor[F[_, _], X, A](name: String)(using
      Arbitrary[F[X, A]],
      ForgetfulTraverse[F, Applicative],
  ): Fragment =
    checkAll(
      name,
      new ForgetfulTraverseTests[F, X, A]:
        val laws = new ForgetfulTraverseLaws[F, X, A] {}
      .forgetfulTraverse,
    )

  // ===== MultiFocus over a Functor-bearing F =====

  /** Runs `MultiFocusTests` for an optic at carrier `MultiFocus[F]` and Functor `F`. The `=:='`
    * evidence is what `MultiFocusTests.multiFocus` needs to thread `F[A]` through `MF3`.
    */
  def checkAllMultiFocusFor[S, A, F[_]](
      name: String,
      mfOptic: Optic[S, S, A, A, data.MultiFocus[F]],
  )(using
      Functor[F],
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      Cogen[F[A]],
      S =:= F[A],
  ): Fragment =
    checkAll(
      name,
      new MultiFocusTests[S, A, F]:
        val laws = new MultiFocusLaws[S, A, F]:
          val multiFocus = mfOptic
          val functor = summon[Functor[F]]
      .multiFocus,
    )

  // ===== Fold / FoldMapHomomorphism over a `Forget[F]`-style carrier =====

  /** Runs `FoldTests` for an optic with a `ForgetfulFold` carrier `F`. */
  def checkAllFoldFor[S, A, F[_, _]](
      name: String,
      foldOptic: Optic[S, Unit, A, Unit, F],
  )(using Arbitrary[S], Cogen[A], ForgetfulFold[F]): Fragment =
    checkAll(
      name,
      new FoldTests[S, A, F]:
        val laws = new FoldLaws[S, A, F]:
          val fold = foldOptic
      .fold,
    )

  /** Runs `FoldMapHomomorphismTests` for any optic whose carrier `F` has `ForgetfulFold[F]`. */
  def checkAllFoldMapHomomorphismFor[S, A, F[_, _]](
      name: String,
      o: Optic[S, S, A, A, F],
  )(using Arbitrary[S], Cogen[A], ForgetfulFold[F]): Fragment =
    checkAll(
      name,
      new FoldMapHomomorphismTests[S, A, F]:
        val laws = new FoldMapHomomorphismLaws[S, A, F]:
          val optic = o
      .foldMapHomomorphism,
    )

  // ===== Morph over an optic + its morphed image =====

  /** Runs `MorphTests.morphPreservesModify` for an optic at source carrier `F` morphed to `G`. */
  def checkAllMorphPreservesModifyFor[S, A, F[_, _], G[_, _]](
      name: String,
      original: Optic[S, S, A, A, F],
      morphedOptic: Optic[S, S, A, A, G],
  )(using
      Arbitrary[S],
      Arbitrary[A],
      Cogen[A],
      ForgetfulFunctor[F],
      ForgetfulFunctor[G],
  ): Fragment =
    checkAll(
      name,
      new MorphTests[S, A, F, G]:
        val laws = new MorphLaws[S, A, F, G]:
          val optic = original
          val morphed = morphedOptic
      .morphPreservesModify,
    )

  /** Runs `MorphTests.morphPreservesGet` for an optic at source carrier `F` morphed to `G`. */
  def checkAllMorphPreservesGetFor[S, A, F[_, _], G[_, _]](
      name: String,
      original: Optic[S, S, A, A, F],
      morphedOptic: Optic[S, S, A, A, G],
  )(using
      Arbitrary[S],
      accessor.Accessor[F],
      accessor.Accessor[G],
  ): Fragment =
    checkAll(
      name,
      new MorphTests[S, A, F, G]:
        val laws = new MorphLaws[S, A, F, G]:
          val optic = original
          val morphed = morphedOptic
      .morphPreservesGet,
    )

  // ===== Traversal-as-MultiFocus[PSVec] over T[_] =====
  //
  // Not currently used by EoSpecificLawsSpec (only one Traversal there); kept here for the
  // OpticsLawsSpec single-site to round out the helper set.

  /** Runs `TraversalTests.traversal` for a `Traversal.each[T, A]` optic. */
  def checkAllTraversalFor[T[_]: Functor, A](
      name: String,
      traversalOptic: Optic[T[A], T[A], A, A, MultiFocus[PSVec]],
  )(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): Fragment =
    checkAll(
      name,
      new TraversalTests[T, A]:
        val laws = new TraversalLaws[T, A]:
          val traversal = traversalOptic
      .traversal,
    )

  // ===== Capability-keyed compose-coherence over three legs =====
  //
  // The `*Cap` builders wrap a concrete optic into the carrier-free capability its carrier admits;
  // the `checkAllComposed*For` runners feed the three legs (outer / inner / composed) to the
  // matching capability RuleSet. Because the laws are keyed by capability, ONE runner serves every
  // family and cross-family cell — the call site just supplies which optics play the three legs.

  /** Wrap any total-read optic as [[CanGet]]. */
  protected def getCap[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
      Accessor[F]
  ): CanGet[S, A] = s => o.get(s)

  /** Wrap any partial-read optic as [[CanGetOption]]. */
  protected def getOptionCap[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
      PartialAccessor[F]
  ): CanGetOption[S, A] = s => o.getOption(s)

  /** Wrap any foldable optic as [[CanFold]]. */
  protected def foldCap[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
      ForgetfulFold[F]
  ): CanFold[S, A] =
    new CanFold[S, A]:
      def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = o.foldMap(f)(s)

  /** Wrap any build optic as [[CanReverseGet]]. */
  protected def reverseGetCap[S, T, A, B, F[_, _]](o: Optic[S, T, A, B, F])(using
      ReverseAccessor[F]
  ): CanReverseGet[T, B] = b => o.reverseGet(b)

  /** Runs `ComposedGetTests` for the three read legs of a composition. */
  def checkAllComposedGetFor[S, A, B](name: String)(
      outer: CanGet[S, A],
      inner: CanGet[A, B],
      composed: CanGet[S, B],
  )(using Arbitrary[S]): Fragment =
    checkAll(
      name,
      new ComposedGetTests[S, A, B]:
        val laws = new ComposedGetLaws[S, A, B]:
          val getOuter = outer
          val getInner = inner
          val getComposed = composed
      .composedGet,
    )

  /** Runs `ComposedGetOptionTests` for the three partial-read legs of a composition. */
  def checkAllComposedGetOptionFor[S, A, B](name: String)(
      outer: CanGetOption[S, A],
      inner: CanGetOption[A, B],
      composed: CanGetOption[S, B],
  )(using Arbitrary[S]): Fragment =
    checkAll(
      name,
      new ComposedGetOptionTests[S, A, B]:
        val laws = new ComposedGetOptionLaws[S, A, B]:
          val getOptionOuter = outer
          val getOptionInner = inner
          val getOptionComposed = composed
      .composedGetOption,
    )

  /** Runs `ComposedFoldMapTests` for the three fold legs of a composition. */
  def checkAllComposedFoldMapFor[S, A, B](name: String)(
      outer: CanFold[S, A],
      inner: CanFold[A, B],
      composed: CanFold[S, B],
  )(using Arbitrary[S], Cogen[B]): Fragment =
    checkAll(
      name,
      new ComposedFoldMapTests[S, A, B]:
        val laws = new ComposedFoldMapLaws[S, A, B]:
          val foldOuter = outer
          val foldInner = inner
          val foldComposed = composed
      .composedFoldMap,
    )

  /** Runs `ComposedReverseGetTests` for the three build legs of a composition. */
  def checkAllComposedReverseGetFor[S, A, B](name: String)(
      outer: CanReverseGet[S, A],
      inner: CanReverseGet[A, B],
      composed: CanReverseGet[S, B],
  )(using Arbitrary[B]): Fragment =
    checkAll(
      name,
      new ComposedReverseGetTests[S, A, B]:
        val laws = new ComposedReverseGetLaws[S, A, B]:
          val reverseOuter = outer
          val reverseInner = inner
          val reverseComposed = composed
      .composedReverseGet,
    )
