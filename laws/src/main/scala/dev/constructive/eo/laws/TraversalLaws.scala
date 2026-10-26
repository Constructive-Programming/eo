package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.{MultiFocus, PSVec}
import cats.Functor

import optics.Optic

/** Law equations for a `Traversal[T[_], A]` — `Optic[T[A], T[A], A, A, MultiFocus[PSVec]]`.
  *
  * The `MultiFocus[PSVec]` carrier carries the right `ForgetfulFunctor` instance to drive the
  * modify-side laws.
  *
  * Monocle's `getAll` / `headOption` laws are not restated here: EO's `Optic.all` (Monocle's
  * `getAll`) is `foldMap(List(_))` for every fold-capable carrier, pinned once by E2 `allIsFoci` in
  * `eo.FoldMapHomomorphismLaws`, and `headOption` rides on the same `foldMap`.
  */
trait TraversalLaws[T[_], A](using val FT: Functor[T]):
  /** The optic under test. */
  def traversal: Optic[T[A], T[A], A, A, MultiFocus[PSVec]]

  /** `modify(identity) == identity`. */
  def modifyIdentity(s: T[A]): Boolean =
    traversal.modify(identity[A])(s) == s

  /** `modify(g) ∘ modify(f) == modify(f andThen g)`. */
  def composeModify(s: T[A], f: A => A, g: A => A): Boolean =
    traversal.modify(g)(traversal.modify(f)(s)) ==
      traversal.modify(f.andThen(g))(s)

  /** put-put: `replace(a) ∘ replace(a) == replace(a)`. */
  def replaceIdempotent(s: T[A], a: A): Boolean =
    traversal.replace(a)(traversal.replace(a)(s)) == traversal.replace(a)(s)

  /** `replace(a) == modify(_ => a)`. */
  def consistentReplaceModify(s: T[A], a: A): Boolean =
    traversal.replace(a)(s) == traversal.modify(_ => a)(s)
