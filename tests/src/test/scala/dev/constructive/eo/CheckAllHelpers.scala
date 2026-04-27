package dev.constructive.eo

import cats.{Applicative, Functor}
import org.scalacheck.{Arbitrary, Cogen}
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.core.Fragment
import org.typelevel.discipline.specs2.mutable.Discipline

import data.Forget
import laws.{FoldLaws, MultiFocusLaws, TraversalLaws}
import laws.discipline.{FoldTests, MultiFocusTests, TraversalTests}
import laws.eo.{FoldMapHomomorphismLaws, MorphLaws}
import laws.eo.discipline.{FoldMapHomomorphismTests, MorphTests}
import laws.typeclass.{ForgetfulFunctorLaws, ForgetfulTraverseLaws}
import laws.typeclass.discipline.{ForgetfulFunctorTests, ForgetfulTraverseTests}
import optics.Optic

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
      foldOptic: Optic[S, Unit, A, A, F],
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
      data.Accessor[F],
      data.Accessor[G],
  ): Fragment =
    checkAll(
      name,
      new MorphTests[S, A, F, G]:
        val laws = new MorphLaws[S, A, F, G]:
          val optic = original
          val morphed = morphedOptic
      .morphPreservesGet,
    )

  // ===== Traversal-as-Forget over T[_] =====
  //
  // Not currently used by EoSpecificLawsSpec (only one Traversal there); kept here for the
  // OpticsLawsSpec single-site to round out the helper set.

  /** Runs `TraversalTests.traversal` for a `Traversal.forEach[T, A]` optic. */
  def checkAllTraversalFor[T[_]: Functor, A](
      name: String,
      traversalOptic: Optic[T[A], T[A], A, A, Forget[T]],
  )(using Arbitrary[T[A]], Arbitrary[A], Cogen[A]): Fragment =
    checkAll(
      name,
      new TraversalTests[T, A]:
        val laws = new TraversalLaws[T, A]:
          val traversal = traversalOptic
      .traversal,
    )
