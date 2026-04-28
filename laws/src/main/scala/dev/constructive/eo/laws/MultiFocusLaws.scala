package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.data.MultiFocus
import _root_.dev.constructive.eo.data.MultiFocus.collectMap
import _root_.dev.constructive.eo.data.MultiFocus.given
import cats.Functor

import optics.Optic
import optics.Optic.*

/** Laws for `MultiFocus[F][S, A]` — `Optic[S, S, A, A, MultiFocus[F]]` — at a concrete `F[_]`.
  *
  *   - MF1 modifyIdentity — `mf.modify(identity)(s) == s`.
  *   - MF2 composeModify — `mf.modify(g) ∘ mf.modify(f) == mf.modify(f andThen g)`.
  *   - MF3 collectViaMap — `mf.collectMap[A](agg)(s) == F.map(s)(_ => agg(s))` at the generic
  *     `MultiFocus.apply[F, A]` factory.
  *
  * @see
  *   `dev.constructive.eo.laws.discipline.MultiFocusTests`.
  */
trait MultiFocusLaws[S, A, F[_]]:
  def multiFocus: Optic[S, S, A, A, MultiFocus[F]]
  given functor: Functor[F]

  def modifyIdentity(s: S): Boolean =
    multiFocus.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    multiFocus.modify(g)(multiFocus.modify(f)(s)) == multiFocus.modify(f.andThen(g))(s)

  /** MF3 — `.collectMap` coherence with `Functor[F].map`. At the generic factory (`focus = s`,
    * `rebuild = identity`, `S = F[A]`), `.collectMap[A](agg)(s)` is definitionally
    * `F.map(focus)(_ => agg(focus))`. The law works at `S = F[A]` by assumption; that's true for
    * every shipped `MultiFocus.apply[F, A]` fixture.
    */
  def collectViaMap(s: S, agg: F[A] => A)(using ev: S =:= F[A]): Boolean =
    val fa: F[A] = ev(s)
    val expected: F[A] = Functor[F].map(fa)(_ => agg(fa))
    multiFocus.collectMap[A](agg)(s) == ev.flip(expected)
