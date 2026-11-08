package dev.constructive.eo

import cats.Functor

import forgetful.ForgetfulTraverse
import optics.Optic

/** Capability: the focused `A` inside an `S` can be rewritten effectfully under any `Functor[G]` —
  * the carrier-free surface of any optic whose carrier admits
  * [[forgetful.ForgetfulTraverse]]`[F, Functor]` (today: Lens).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanModifyF]] alias covers the
  * common case. See [[CanGet]] for the doctrine and the coherence rule.
  */
trait CanModifyFP[S, T, A, B]:
  def modifyF[G[_]](f: A => G[B])(using Functor[G]): S => G[T]

/** Monomorphic [[CanModifyFP]] (`S = T`, `A = B`). */
type CanModifyF[S, A] = CanModifyFP[S, S, A, A]

object CanModifyFP:

  final class ModifyFOps[S, A](private val s: S) extends AnyVal:

    inline def apply[G[_]](f: A => G[A])(using cm: CanModifyF[S, A], F: Functor[G]): G[S] =
      cm.modifyF[G](f)(s)

  final class ModifyFPOps[S, T, A, B](private val s: S) extends AnyVal:

    inline def apply[G[_]](f: A => G[B])(using cm: CanModifyFP[S, T, A, B], F: Functor[G]): G[T] =
      cm.modifyF[G](f)(s)

  /** Derive from any optic in scope whose carrier traverses under `Functor`. Optic before gate in
    * the same using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ft: ForgetfulTraverse[F, Functor],
  ): CanModifyFP[S, T, A, B] with
    def modifyF[G[_]](f: A => G[B])(using Functor[G]): S => G[T] = o.modifyF(f)

export optics.Optic.modifyF

extension [S](s: S)

  /** Rewrite the `A` focus inside `s` effectfully under `Functor[G]` via the [[CanModifyF]]
    * instance in scope.
    */
  inline def modifyF[A]: CanModifyFP.ModifyFOps[S, A] =
    new CanModifyFP.ModifyFOps[S, A](s)

  /** Polymorphic counterpart to [[modifyF]] — allows focus and subject types to change. */
  inline def modifyFP[T, A, B]: CanModifyFP.ModifyFPOps[S, T, A, B] =
    new CanModifyFP.ModifyFPOps[S, T, A, B](s)
