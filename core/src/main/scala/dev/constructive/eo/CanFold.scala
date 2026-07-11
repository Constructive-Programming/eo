package dev.constructive.eo

import kernel.Monoid
import forgetful.ForgetfulFold
import optics.Optic

/** Capability: the foci `A` visible in an `S` can be folded through a `Monoid` — the carrier-free
  * surface of any optic whose carrier admits [[forgetful.ForgetfulFold]] (every readable family;
  * the primary surface of Fold and Traversal).
  *
  * `foldMap` is the kernel; `headOption` / `length` / `exists` / `foci` ride on it. `foci` is the
  * carrier-free counterpart of the raw-optic `all` extension (which returns foci still inside their
  * carrier, `List[F[X, A]]`). Prefer this trait in consuming signatures; see [[CanGet]] for the
  * doctrine and the coherence rule.
  */
trait CanFold[S, A]:
  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M

  def headOption(s: S): kyo.Maybe[A] =
    foldMap(a => kyo.Maybe.Present(a): kyo.Maybe[A])(s)(using
      Monoid.instance(kyo.Maybe.Absent, (l, r) => l.orElse(r))
    )

  def length(s: S): Int =
    foldMap(_ => 1)(s)

  def exists(p: A => Boolean)(s: S): Boolean =
    foldMap(p)(s)(using Monoid.instance(false, _ || _))

  def foci(s: S): List[A] =
    foldMap(a => List(a))(s)

object CanFold:

  /** Derive from any optic in scope whose carrier can fold. Optic before gate in the same using
    * clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ff: ForgetfulFold[F],
  ): CanFold[S, A] with
    def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = o.foldMap(f)(s)
