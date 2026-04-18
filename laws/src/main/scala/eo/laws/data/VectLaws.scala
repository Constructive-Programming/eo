package eo.laws.data

import eo.data.Vect

import cats.{Functor, Id, Traverse}

/** Carrier-level laws for `Vect[N, A]`.
  *
  * `Vect` is a length-indexed heterogeneous vector with four constructors — `NilVect`, `ConsVect`,
  * `TConsVect`, `AdjacentVect`. These laws pin down its cats-kernel instances at a *fixed* size
  * `N`:
  *
  * * `Functor[[X] =>> Vect[N, X]]` — identity and composition. * `Traverse[[X] =>> Vect[N, X]]` at
  * `Id` — identity.
  *
  * Per-constructor structural invariants (concat associativity, slice length, cons/snoc symmetry)
  * live in [[eo.laws.data.discipline.VectTests]]'s counterpart behaviour spec rather than a
  * discipline RuleSet, because those invariants need the `N` phantom parameter to vary across the
  * law — the discipline framework is set up for one law per monomorphic type.
  */
trait VectLaws[N <: Int, A]:
  /** Cats `Functor` instance in scope — typically summoned from `eo.data.Vect.functor`.
    */
  def F: Functor[[X] =>> Vect[N, X]]

  /** Cats `Traverse` instance in scope — typically summoned from `eo.data.Vect.trav`.
    */
  def T: Traverse[[X] =>> Vect[N, X]]

  /** `map(identity)` is pointwise equal to the input. */
  def functorIdentity(xs: Vect[N, A]): Boolean =
    F.map(xs)(identity[A]) == xs

  /** `map(g) ∘ map(f) == map(f andThen g)`. */
  def functorComposition(xs: Vect[N, A], f: A => A, g: A => A): Boolean =
    F.map(F.map(xs)(f))(g) == F.map(xs)(f.andThen(g))

  /** `traverse[Id](identity)` is pointwise equal to the input. */
  def traverseIdentity(xs: Vect[N, A]): Boolean =
    T.traverse[Id, A, A](xs)(a => a: Id[A]) == xs

  /** `map(f)` preserves the runtime size. */
  def mapPreservesSize(xs: Vect[N, A], f: A => A): Boolean =
    F.map(xs)(f).size == xs.size
