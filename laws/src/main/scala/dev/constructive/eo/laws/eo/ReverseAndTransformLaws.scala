package dev.constructive.eo
package laws
package eo

import _root_.dev.constructive.eo.data.Forgetful
import _root_.dev.constructive.eo.data.Forgetful.given

import optics.Optic
import optics.Optic.*

// EO-specific laws that live at the boundary of the Iso / Lens machinery:
//
//   * B1  — Iso `reverse` is an involution.
//   * H1/H2/H4 — Lens `place` / `transform` / `transfer` agree with each other.
//   * H3  — Iso `put` equals `reverseGet ∘ pure`.
//
// All three families touch the `reverse` / "write back" side of an
// optic, so they sit together in this file.

/** B1 — Iso `reverse` is involutive: `iso.reverse.reverse` behaves pointwise like `iso` on both
  * `get` and `reverseGet`.
  */
trait ReverseInvolutionLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  def reverseInvolutionGet(s: S): Boolean =
    iso.reverse.reverse.get(s) == iso.get(s)

  def reverseInvolutionReverseGet(a: A): Boolean =
    iso.reverse.reverse.reverseGet(a) == iso.reverseGet(a)

/** H1/H2/H4 — Lens `transform` / `place` / `transfer` mutually agree.
  *
  * The `transform` extension requires an implicit `ev: S => F[o.X, A]` whose type depends on the
  * optic's existential `X`. To make the law reusable rather than inline, we:
  *
  * * declare `optic` as a `val` so `optic.X` is a stable path- dependent type, and * leave the
  * `given transformEv` abstract so each concrete subclass supplies the `S => (optic.X, A)` evidence
  * (identity for `Lens.second`, swap for `Lens.first`, …).
  */
trait TransformLaws[S, A, X0]:
  /** The lens-shaped optic under test, with its existential `X` surfaced as the explicit `X0`
    * parameter so the `ev` given can refer to it without path-dependent shenanigans.
    */
  val optic: Optic[S, S, A, A, Tuple2] { type X = X0 }

  /** Evidence that `S` can be viewed as `(X0, A)`. Concrete subclass supplies this — `identity` for
    * `Lens.second`, `_.swap` for `Lens.first`, etc.
    */
  given transformEv: (S => (X0, A))

  /** H4 — transform identity is a no-op. */
  def transformIdentity(t: S): Boolean =
    optic.transform(identity[A])(t) == t

  /** H2 — `transfer(f)(t)(c)` equals `place(f(c))(t)`. */
  def transferIsCurriedPlace(t: S, c: A, f: A => A): Boolean =
    optic.transfer(f)(t)(c) == optic.place(f(c))(t)

  /** H1 — `place(a)` equals `transform(_ => a)`. */
  def placeIsTransformConst(t: S, a: A): Boolean =
    optic.place(a)(t) == optic.transform(_ => a)(t)

/** H3 — Iso `put` is the curried `reverseGet ∘ pure`. */
trait PutIsReverseGetLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  def putIsReverseGetCompose(a: A, f: A => A): Boolean =
    iso.put(f)(a) == iso.reverseGet(f(a))
