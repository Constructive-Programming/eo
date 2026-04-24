package eo
package laws
package typeclass

import optics.Optic
import optics.Optic.*

/** Laws governing [[AssociativeFunctor]] — the typeclass that drives same-carrier `Optic.andThen`
  * via `composeTo` (push) + `composeFrom` (pull).
  *
  * AssociativeFunctor's contract is operational, not category-theoretic in the stock "functor laws"
  * sense: there's no `pure` on the typeclass, and the "associative" part refers to packing `Xo` and
  * `Xi` into a combined existential `Z` rather than to any monoidal structure. The laws below pin
  * what the caller relies on when they reach for `outer.andThen(inner)`:
  *
  * A1. Compose-modify distributes — `outer.andThen(inner).modify(f)(s)` agrees with
  * `outer.modify(inner.modify(f))(s)`. The composed optic is structurally equivalent to the
  * hand-chained modify. A2. Compose-modify respects identity —
  * `outer.andThen(inner).modify(identity)(s) == s` for any monomorphic `S = T`, `A = B`, `C = D`
  * chain. A specialisation of A1 for `f = id` that can be witnessed without a Cogen instance.
  *
  * Both laws require `ForgetfulFunctor[F]` in scope (so `.modify` is defined). Every carrier
  * cats-eo ships an `AssociativeFunctor` for also ships a `ForgetfulFunctor`, so this is a natural
  * precondition rather than a tight coupling.
  *
  * @tparam S
  *   outer source type (monomorphic: S = T)
  * @tparam A
  *   outer focus / inner source type (monomorphic: A = B)
  * @tparam C
  *   inner focus type (monomorphic: C = D)
  * @tparam F
  *   shared carrier
  */
trait AssociativeFunctorLaws[S, A, C, F[_, _]]:

  def outer: Optic[S, S, A, A, F]
  def inner: Optic[A, A, C, C, F]

  /** `outer.andThen(inner)` supplied by the spec. The implicit `AssociativeFunctor[F, outer.X,
    * inner.X]` resolves at fixture-construction time with concrete `outer` / `inner` in scope; the
    * law trait itself never calls `.andThen` since `F`'s `Xo` / `Xi` are abstract here.
    */
  def composed: Optic[S, S, C, C, F]

  given functor: ForgetfulFunctor[F]

  /** A1 — compose-modify distributes. The composed optic's `modify` agrees with the hand-chained
    * `outer.modify(inner.modify(f))(s)` at every `(s, f)`.
    */
  def composeModifyDistributes(s: S, f: C => C): Boolean =
    composed.modify(f)(s) == outer.modify(inner.modify(f))(s)

  /** A2 — compose-modify respects identity. Specialisation of A1 for `f = identity`; holds without
    * needing a Cogen[C] since no function generator is involved.
    */
  def composeModifyIdentity(s: S): Boolean =
    composed.modify(identity[C])(s) == s
