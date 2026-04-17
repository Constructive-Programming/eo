package eo.laws.data

import eo.data.Affine
import eo.data.Affine.given
import eo.{ForgetfulFunctor, ForgetfulTraverse}

import cats.{Applicative, Id}

/** Carrier-level laws for `Affine[X, A]`.
  *
  * `Affine` is the carrier behind `Optional`: a sum of a "no write
  * path" case (`Fst[X]`) and a tuple-like "got the focus" case
  * (`(Snd[X], A)`). The laws pin down its two main type-class
  * instances:
  *
  *   * `ForgetfulFunctor[Affine]` — identity and composition.
  *   * `ForgetfulTraverse[Affine, Applicative]` at `Id` — identity.
  *
  * The `AssociativeFunctor[Affine, X, Y]` instance is already
  * exercised by `Optional ∘ Optional` at the optic level
  * (see [[eo.laws.eo.OptionalComposeLaws]]); re-stating its
  * associativity equations as a standalone law class would duplicate
  * that coverage without adding signal.
  */
trait AffineLaws[X, A]:
  /** A sample `Affine` value produced by the concrete subclass. The
    * subclass typically supplies scalacheck-generated values through
    * the `forAll` calls in [[eo.laws.data.discipline.AffineTests]]. */
  def functorIdentity(fa: Affine[X, A])(using
      FF: ForgetfulFunctor[Affine]
  ): Boolean =
    FF.map(fa, identity[A]) == fa

  def functorComposition(fa: Affine[X, A], f: A => A, g: A => A)(using
      FF: ForgetfulFunctor[Affine]
  ): Boolean =
    FF.map(FF.map(fa, f), g) == FF.map(fa, f.andThen(g))

  /** `traverse[Id]` is `map` — the degenerate case of the traverse
    * identity law. */
  def traverseIdentity(fa: Affine[X, A])(using
      FT: ForgetfulTraverse[Affine, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](using Applicative[Id])(fa)(a => (a: Id[A])) ==
      fa
