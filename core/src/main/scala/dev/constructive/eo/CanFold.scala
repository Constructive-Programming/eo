package dev.constructive.eo

import cats.Monoid

import forgetful.ForgetfulFold
import optics.Optic

/** Capability: the foci `A` visible in an `S` can be folded through a `Monoid` — the carrier-free
  * surface of any optic whose carrier admits [[forgetful.ForgetfulFold]] (every readable family;
  * the primary surface of Fold and Traversal).
  *
  * `foldMap` is the kernel; `headOption` / `length` / `exists` / `foci` ride on it. `foci` is the
  * carrier-free spelling of the raw-optic `all` extension: both are `foldMap(List(_))`, every focus
  * in traversal order. Prefer this trait in consuming signatures; see [[CanGet]] for the doctrine
  * and the coherence rule.
  */
trait CanFold[S, A]:
  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M

  def headOption(s: S): Option[A] =
    foldMap(a => Some(a): Option[A])(s)(using Monoid.instance(None, (l, r) => l.orElse(r)))

  def length(s: S): Int =
    foldMap(_ => 1)(s)

  def exists(p: A => Boolean)(s: S): Boolean =
    foldMap(p)(s)(using Monoid.instance(false, _ || _))

  def foci(s: S): List[A] =
    foldMap(a => List(a))(s)

object CanFold:

  final class FoldMapOps[S, A](private val s: S) extends AnyVal:

    inline def apply[M](f: A => M)(using cf: CanFold[S, A], m: Monoid[M]): M =
      cf.foldMap(f)(s)

  /** Derive from any optic in scope whose carrier can fold. Optic before gate in the same using
    * clause — see [[CanGet]] for the SIP-64 ordering rationale.
    */
  given [S, T, A, B, F[_, _]](using
      o: Optic[S, T, A, B, F],
      ff: ForgetfulFold[F],
  ): CanFold[S, A] with
    def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = o.foldMap(f)(s)

export optics.Optic.{exists, foldMap, headOption, length}

extension [S](s: S)

  /** Fold the `A` foci inside `s` through `Monoid[M]` via the [[CanFold]] instance in scope. */
  inline def foldMap[A]: CanFold.FoldMapOps[S, A] =
    new CanFold.FoldMapOps[S, A](s)

  /** First `A` focus inside `s`, if any, via the [[CanFold]] instance in scope. */
  inline def headOption[A](using cf: CanFold[S, A]): Option[A] = cf.headOption(s)

  /** Number of `A` foci inside `s` via the [[CanFold]] instance in scope. */
  inline def length[A](using cf: CanFold[S, A]): Int = cf.length(s)

  /** True iff at least one `A` focus inside `s` satisfies `p`, via the [[CanFold]] instance in
    * scope.
    */
  inline def exists[A](p: A => Boolean)(using cf: CanFold[S, A]): Boolean = cf.exists(p)(s)

  /** Every `A` focus inside `s` in traversal order, via the [[CanFold]] instance in scope. */
  inline def foci[A](using cf: CanFold[S, A]): List[A] = cf.foci(s)
