package dev.constructive.eo
package laws
package typeclass

import dev.constructive.eo.forgetful.*

import optics.Optic
import optics.Optic.*

/** Laws for [[dev.constructive.eo.compose.AssociativeFunctor]]. Operational, not
  * category-theoretic: the "associative" part refers to packing `Xo` / `Xi` into `Z`, not monoidal
  * structure.
  *
  *   - A1 compose-modify distributes: `composed.modify(f)(s) == outer.modify(inner.modify(f))(s)`.
  *   - A2 compose-modify identity: a Cogen-free specialisation of A1.
  *
  * Both require `ForgetfulFunctor[F]` (every cats-eo `AssociativeFunctor` carrier also ships one).
  *
  * @tparam S
  *   outer source (mono S = T)
  * @tparam A
  *   outer focus / inner source (mono A = B)
  * @tparam C
  *   inner focus (mono C = D)
  * @tparam F
  *   shared carrier
  */
trait AssociativeFunctorLaws[S, A, C, F[_, _]]:

  /** Outer optic. */
  def outer: Optic[S, S, A, A, F]

  /** Inner optic. */
  def inner: Optic[A, A, C, C, F]

  /** `outer.andThen(inner)` materialised by the spec — the law trait itself can't call `.andThen`
    * because `F`'s `Xo` / `Xi` are abstract here.
    */
  def composed: Optic[S, S, C, C, F]

  /** `ForgetfulFunctor` evidence driving `modify` on the shared carrier. */
  given functor: ForgetfulFunctor[F]

  /** A1 — `composed.modify(f)(s) == outer.modify(inner.modify(f))(s)`. */
  def composeModifyDistributes(s: S, f: C => C): Boolean =
    composed.modify(f)(s) == outer.modify(inner.modify(f))(s)

  /** A2 — `composed.modify(identity)(s) == s`. */
  def composeModifyIdentity(s: S): Boolean =
    composed.modify(identity[C])(s) == s
