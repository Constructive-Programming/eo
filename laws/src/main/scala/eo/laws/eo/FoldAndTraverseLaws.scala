package eo
package laws
package eo

import optics.Optic
import optics.Optic.*
import data.Forget

import cats.Traverse

// Fold / Traverse family — the laws below pin down how EO's
// `foldMap` and `all` interact with the carriers that carry
// Traverse / Traversable structure (currently just `Forget[T]` plus
// the in-optic `ForgetfulFold`).
//
//   E1 — `foldMap` is a Monoid homomorphism.
//   G1/G2 — `all` on `Forget[T]` always returns a one-element list
//           whose head is the whole input container.
//   G3 — `all`-then-`map` agrees with `modify` on `Forget[T]`.

/** E1 — `optic.foldMap` is a Monoid homomorphism on the target
  * monoid (tested at `Int` with additive `Monoid[Int]`, which is
  * enough to witness the law). */
trait FoldMapHomomorphismLaws[S, A, F[_, _]]:
  def optic: Optic[S, S, A, A, F]

  def foldMapHomomorphism(s: S, f: A => Int, g: A => Int)(using
      ForgetfulFold[F]
  ): Boolean =
    optic.foldMap(a => f(a) + g(a))(s) ==
      optic.foldMap(f)(s) + optic.foldMap(g)(s)

  def foldMapEmpty(s: S)(using ForgetfulFold[F]): Boolean =
    optic.foldMap[Int](_ => 0)(s) == 0

/** G1/G2 — `Optic.all` on `Forget[T]` always returns a one-element
  * list whose head is the whole input container.
  *
  * `Optic.all` uses `traverse(List(_))` under the hood, which with
  * List's cartesian applicative yields a one-element list wrapping
  * the whole container — not a per-element enumeration. These laws
  * pin that behaviour down so later changes cannot silently break it.
  */
trait TraverseAllLaws[T[_], A](using val FT: Traverse[T]):
  def traversal: Optic[T[A], T[A], A, A, Forget[T]]

  /** G1 — `all` always has exactly one element. */
  def allHasLengthOne(s: T[A]): Boolean =
    traversal.all(s).length == 1

  /** G2 — that single element is the original container. */
  def allHeadIsInput(s: T[A]): Boolean =
    traversal.all(s).head == s

/** G3 — all-then-map agrees with modify on `Forget[T]`.
  *
  * For a `Forget[T]`-carrier optic, `all(s)` is `List(s)` — a single-
  * element list holding the whole container (see [[TraverseAllLaws]]).
  * Because `Forget[T][X, A]` type-reduces to `T[A]`, that head is
  * already an ordinary `T[A]` that `Functor[T].map` can walk. The
  * law says mapping the head agrees with `modify`, tying `all`'s
  * reading of the structure to `modify`'s rewriting of it.
  */
trait ForgetAllModifyLaws[T[_], A](using val T: Traverse[T]):
  def traversal: Optic[T[A], T[A], A, A, Forget[T]]

  def allMapEqualsModify(s: T[A], f: A => A): Boolean =
    val head: T[A] = traversal.all(s).head
    T.map(head)(f) == traversal.modify(f)(s)
