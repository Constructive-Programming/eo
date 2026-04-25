package dev.constructive.eo
package laws

import _root_.dev.constructive.eo.Reflector
import _root_.dev.constructive.eo.data.Kaleidoscope
import _root_.dev.constructive.eo.data.Kaleidoscope.given

import optics.Optic
import optics.Optic.*

/** Law equations for a `Kaleidoscope[S, A]` — `Optic[S, S, A, A, Kaleidoscope]` — parameterised by
  * the concrete Reflector `F[_]` that classifies the optic's aggregation semantics.
  *
  * Ported from Chris Penner's "Kaleidoscopes: lenses that never die"
  * (<https://chrispenner.ca/posts/kaleidoscopes>) and the Clarke et al. categorical formulation
  * (arXiv:2001.07488) — neither source pins a single compact law statement, so the three equations
  * below are the minimum distilled that exercise both the `ForgetfulFunctor` path and the
  * Kaleidoscope-specific `.collect` universal. See plan
  * `docs/plans/2026-04-23-006-feat-kaleidoscope-optic-family-plan.md` §D6.
  *
  *   - **K1 modifyIdentity** — `kaleidoscope.modify(identity)(s) == s`. Shared shape with every
  *     other family that admits `.modify`.
  *   - **K2 composeModify** —
  *     `kaleidoscope.modify(g)(kaleidoscope.modify(f)(s)) == kaleidoscope.modify(f andThen g)(s)`.
  *     Shared shape, witnesses that the carrier's `ForgetfulFunctor` path composes cleanly.
  *   - **K3 collectViaReflect** — `kaleidoscope.collect[F, A](f)(s) == reflector.reflect(s)(f)` at
  *     the generic `Kaleidoscope.apply[F, A]` factory, where `focus = s` and `rebuild = identity`.
  *     This is the Kaleidoscope-specific coherence law: the optic's universal is *precisely* the
  *     Reflector's `reflect` operation, threaded through the rebuild. Stated for the factory
  *     encoding because it's the only public constructor; any other constructor that preserves
  *     `rebuild = identity` at `X = F[A]` satisfies the same shape.
  *
  * **Why the three-way parameterisation `[S, A, F[_]]`.** K3 references `Reflector[F]` directly —
  * the law's statement needs to name `F` to invoke `reflect`. Extra type parameter vs. a
  * per-instance law class; inference has been fine at `checkAll` sites in practice.
  *
  * **Why no `replace`-idempotence law** (c.f. Grate's `G3 replaceIdempotent`). Grate's `.replace`
  * broadcasts a single value across every slot, so idempotence-of-replace is a structural
  * invariant. Kaleidoscope's `.replace(a)` similarly broadcasts via `kalFunctor.map`, but the
  * Kaleidoscope's real story is its `.collect` universal — which has no Grate analogue. K3 covers
  * the Kaleidoscope-specific side; K1/K2 cover the shared side. A replace-idempotence law would add
  * no incremental witness beyond K1's identity round-trip.
  *
  * @see
  *   `dev.constructive.eo.laws.discipline.KaleidoscopeTests` for the discipline RuleSet.
  */
trait KaleidoscopeLaws[S, A, F[_]]:
  def kaleidoscope: Optic[S, S, A, A, Kaleidoscope]
  given reflector: Reflector[F]

  /** K3 requires reading `kaleidoscope.collect[F, A]` — the law caller supplies the concrete `F` so
    * the extension dispatches against the right Reflector instance. `F` is the same `F` the
    * `kaleidoscope` was constructed with (the discipline RuleSet pins this by construction via the
    * `Arbitrary[S]` / fixture pairing).
    */

  def modifyIdentity(s: S): Boolean =
    kaleidoscope.modify(identity[A])(s) == s

  def composeModify(s: S, f: A => A, g: A => A): Boolean =
    kaleidoscope.modify(g)(kaleidoscope.modify(f)(s)) == kaleidoscope.modify(f.andThen(g))(s)

  /** K3 — `.collect` coherence with the Reflector. At the generic factory (`focus = s`, `rebuild =
    * identity`, `S = F[A]`), `.collect[F, A](f)(s)` is definitionally `rebuild(reflect(focus)(f))
    * = reflect(s)(f)`. The law takes an `agg: F[A] => A` as a ScalaCheck-driven aggregator — the
    * same-typed-result arm of the extension (where the output `C` coincides with the focus `A`),
    * which is the arm that round-trips through the generic factory's `from`.
    *
    * The law works at `S = F[A]` by assumption. That's true for every shipped
    * `Kaleidoscope.apply[F, A]` fixture; it would need broadening if a future factory ships `S`
    * distinct from `F[A]`.
    */
  def collectViaReflect(s: S, agg: F[A] => A)(using ev: S =:= F[A]): Boolean =
    val fa: F[A] = ev(s)
    kaleidoscope.collect[F, A](agg)(s) == ev.flip(reflector.reflect(fa)(agg))
