package dev.constructive.eo

import cats.Functor

import forgetful.ForgetfulTraverse
import optics.Optic

/** Capability: the foci `A` inside an `S` can be rewritten effectfully, under the weakest
  * constraint `C` the optic needs on the effect `G` — `Functor` for a Lens, `Applicative` for a
  * Prism / Optional / Traversal / Fold, `Invariant` for an Iso / Getter, `Distributive` for a
  * Modify. The carrier-free surface of any optic whose carrier admits
  * [[forgetful.ForgetfulTraverse]]`[F, C]`.
  *
  * Contravariant in `C`: an optic that needs less serves any consumer that offers more, so
  * `CanModifyFP[…, Functor] <: CanModifyFP[…, Applicative]`. A consuming signature names the
  * effects it will use — `CanModifyF[S, A, Applicative]` accepts every optic above, while
  * `CanModifyF[S, A, Functor]` accepts only lenses and isos. The write-back is a `map`, so `G` is
  * always at least a `Functor`: the evidence is `C[G] & Functor[G]`, a single instance whenever `C`
  * extends `Functor`. For collecting the foci without effects, use [[CanFold.foci]]. See [[CanGet]]
  * for the doctrine and the coherence rule.
  */
trait CanModifyFP[S, T, A, B, -C[_[_]]]:
  def modifyF[G[_]](f: A => G[B])(using C[G] & Functor[G]): S => G[T]

/** Monomorphic [[CanModifyFP]] (`S = T`, `A = B`). */
type CanModifyF[S, A, -C[_[_]]] = CanModifyFP[S, S, A, A, C]

object CanModifyFP:

  final class ModifyFPOps[S, T, A, B](private val s: S) extends AnyVal:

    inline def apply[G[_], C[_[_]]](f: A => G[B])(using
        cm: CanModifyFP[S, T, A, B, C],
        G: C[G] & Functor[G],
    ): G[T] =
      cm.modifyF[G](f)(s)

  // CPD-OFF — the capability wrapper restates `Optic.modifyF`'s signature to forward to it.
  /** Derive from any optic in scope whose carrier traverses. Each carrier has exactly one
    * [[forgetful.ForgetfulTraverse]] instance, so `C` is inferred as the weakest constraint that
    * carrier supports. Optic before gate in the same using clause — see [[CanGet]] for the SIP-64
    * ordering rationale.
    */
  given [S, T, A, B, F[_, _], C[_[_]]](using
      o: Optic[S, T, A, B, F],
      ft: ForgetfulTraverse[F, C],
  ): CanModifyFP[S, T, A, B, C] with

    def modifyF[G[_]](f: A => G[B])(using G: C[G] & Functor[G]): S => G[T] =
      o.modifyF[G](f)

  // CPD-ON

export optics.Optic.modifyF

extension [S](s: S)

  /** Rewrite the `A` foci inside `s` effectfully via the [[CanModifyF]] instance in scope; the
    * effect `G` needs only what that optic's carrier needs (`Functor` for a Lens, `Applicative` for
    * a Prism or Traversal).
    */
  inline def modifyF[A]: CanModifyFP.ModifyFPOps[S, S, A, A] =
    new CanModifyFP.ModifyFPOps[S, S, A, A](s)

  /** Polymorphic counterpart to [[modifyF]] — allows focus and subject types to change. */
  inline def modifyFP[T, A, B]: CanModifyFP.ModifyFPOps[S, T, A, B] =
    new CanModifyFP.ModifyFPOps[S, T, A, B](s)
