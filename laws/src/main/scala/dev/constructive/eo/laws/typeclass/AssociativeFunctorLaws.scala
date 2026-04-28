package dev.constructive.eo
package laws
package typeclass

import optics.Optic
import optics.Optic.*

/** Laws for [[AssociativeFunctor]]. Operational, not category-theoretic: the "associative" part
  * refers to packing `Xo` / `Xi` into `Z`, not monoidal structure.
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

  def outer: Optic[S, S, A, A, F]
  def inner: Optic[A, A, C, C, F]

  /** `outer.andThen(inner)` materialised by the spec — the law trait itself can't call `.andThen`
    * because `F`'s `Xo` / `Xi` are abstract here.
    */
  def composed: Optic[S, S, C, C, F]

  given functor: ForgetfulFunctor[F]

  def composeModifyDistributes(s: S, f: C => C): Boolean =
    composed.modify(f)(s) == outer.modify(inner.modify(f))(s)

  def composeModifyIdentity(s: S): Boolean =
    composed.modify(identity[C])(s) == s
