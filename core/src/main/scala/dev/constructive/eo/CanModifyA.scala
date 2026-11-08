package dev.constructive.eo

import cats.Applicative

import forgetful.ForgetfulTraverse
import optics.Optic

/** Capability: the foci `A` inside an `S` can be rewritten effectfully under any `Applicative[G]` —
  * the carrier-free surface of any optic whose carrier admits
  * [[forgetful.ForgetfulTraverse]]`[F, Applicative]` (Lens, Prism, Optional, Traversal, Fold).
  *
  * Prefer this trait in consuming signatures; the monomorphic [[CanModifyA]] alias covers the
  * common case. For collecting the foci without effects, use [[CanFold.foci]] (the raw-optic
  * spelling is the `all` extension, which needs only a fold). See [[CanGet]] for the doctrine and
  * the coherence rule.
  */
trait CanModifyAP[S, T, A, B]:
  def modifyA[G[_]](f: A => G[B])(using Applicative[G]): S => G[T]

/** Monomorphic [[CanModifyAP]] (`S = T`, `A = B`). */
type CanModifyA[S, A] = CanModifyAP[S, S, A, A]

object CanModifyAP:

  final class ModifyAOps[S, A](private val s: S) extends AnyVal:

    inline def apply[G[_]](f: A => G[A])(using cm: CanModifyA[S, A], A: Applicative[G]): G[S] =
      cm.modifyA[G](f)(s)

  final class ModifyAPOps[S, T, A, B](private val s: S) extends AnyVal:

    inline def apply[G[_]](f: A => G[B])(using
        cm: CanModifyAP[S, T, A, B],
        A: Applicative[G],
    ): G[T] =
      cm.modifyA[G](f)(s)

  /** Derive from any optic in scope whose carrier traverses under `Applicative`. Optic before gate
    * in the same using clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ft: ForgetfulTraverse[F, Applicative],
  ): CanModifyAP[S, T, A, B] with
    def modifyA[G[_]](f: A => G[B])(using Applicative[G]): S => G[T] = o.modifyA(f)

export optics.Optic.modifyA

extension [S](s: S)

  /** Rewrite the `A` foci inside `s` effectfully under `Applicative[G]` via the [[CanModifyA]]
    * instance in scope.
    */
  inline def modifyA[A]: CanModifyAP.ModifyAOps[S, A] =
    new CanModifyAP.ModifyAOps[S, A](s)

  /** Polymorphic counterpart to [[modifyA]] — allows focus and subject types to change. */
  inline def modifyAP[T, A, B]: CanModifyAP.ModifyAPOps[S, T, A, B] =
    new CanModifyAP.ModifyAPOps[S, T, A, B](s)
