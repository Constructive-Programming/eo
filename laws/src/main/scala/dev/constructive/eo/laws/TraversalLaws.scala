package dev.constructive.eo
package laws

import optics.Optic
import _root_.dev.constructive.eo.data.Forget
import _root_.dev.constructive.eo.data.Forget.given

import cats.Functor

/** Law equations for a `Traversal[T[_], A]` — `Optic[T[A], T[A], A, A, Forget[T]]`.
  *
  * For `Traversal.forEach` the carrier is `Forget[T]`, which carries the right `ForgetfulFunctor` /
  * `ForgetfulFold` instances to drive the modify-side laws.
  *
  * We deliberately do NOT port `getAll` / `headOption` here — EO's `Optic.all` returns the whole
  * container wrapped in a single-element list (cartesian-product semantics from
  * `traverse(List(_))`), not the individual elements, so Monocle's phrasing of those laws would not
  * be testing what the name suggests. See `dev.constructive.eo.laws.eo.TraverseAllLaws` for the
  * EO-specific laws that pin down `all`'s real behaviour.
  */
trait TraversalLaws[T[_], A](using val FT: Functor[T]):
  def traversal: Optic[T[A], T[A], A, A, Forget[T]]

  def modifyIdentity(s: T[A]): Boolean =
    traversal.modify(identity[A])(s) == s

  def composeModify(s: T[A], f: A => A, g: A => A): Boolean =
    traversal.modify(g)(traversal.modify(f)(s)) ==
      traversal.modify(f.andThen(g))(s)

  def replaceIdempotent(s: T[A], a: A): Boolean =
    traversal.replace(a)(traversal.replace(a)(s)) == traversal.replace(a)(s)

  def consistentReplaceModify(s: T[A], a: A): Boolean =
    traversal.replace(a)(s) == traversal.modify(_ => a)(s)
