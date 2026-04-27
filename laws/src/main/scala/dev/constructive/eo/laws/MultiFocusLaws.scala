package dev.constructive.eo
package laws

import cats.Functor

import _root_.dev.constructive.eo.data.MultiFocus
import _root_.dev.constructive.eo.data.MultiFocus.given

import optics.Optic
import optics.Optic.*

/** Law equations for a `MultiFocus[F][S, A]` — `Optic[S, S, A, A, MultiFocus[F]]` — at a concrete
  * classifier shape `F[_]`.
  *
  * `MultiFocus[F]` is the unified successor of `AlgLens[F]` and `Kaleidoscope`. The structural
  * pair `(X, F[A])` is identical to the v1 `AlgLens[F]`, but the Kaleidoscope `.collect` universal
  * is absorbed via the carrier-wide `collectMap` extension (Functor-broadcast, derives from
  * `Functor[F].map`). The `List`-singleton variant survives at the call site as `collectList`, but
  * is not surfaced as a discipline law — the K3 statement now reads as a single
  * `collectViaMap` law that holds for every `Functor[F]`.
  *
  *   - **MF1 modifyIdentity** — `mf.modify(identity)(s) == s`. Shared with every other family that
  *     admits `.modify`.
  *   - **MF2 composeModify** —
  *     `mf.modify(g)(mf.modify(f)(s)) == mf.modify(f andThen g)(s)`. Shared shape; witnesses that
  *     `ForgetfulFunctor[MultiFocus[F]]` composes cleanly.
  *   - **MF3 collectViaMap** — `mf.collectMap[A](agg)(s) == ev.flip(F.map(ev(s))(_ => agg(ev(s))))`
  *     at the generic `MultiFocus.apply[F, A]` factory (`X = F[A]`, rebuild = identity, `S =
  *     F[A]`). Witnesses that the carrier-wide `.collectMap` universal is precisely the
  *     Functor-broadcast through the rebuild. Replaces the v1 K3 `collectViaReflect` law.
  *
  * Per the user's option (a) on the K3 restatement: ship a single Functor-broadcast law at the
  * carrier level. List users can still get the v1 cartesian-Reflector singleton story via the
  * `collectList` extension at the call site — it's a function, not a law.
  *
  * @see
  *   `dev.constructive.eo.laws.discipline.MultiFocusTests` for the discipline RuleSet.
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
