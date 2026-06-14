package dev.constructive.eo
package schemes
package zoo

import optics.Optic

/** Metamorphism citizen — a **fold-then-unfold** worn as an optic over [[Scheme]], reading `S => T`
  * (`Getter`-shaped, consumed via `.get`). The fold-direction dual of [[Hylo]]: where `hylo` is the
  * fused unfold-then-fold (`X = Nothing`, deforests), `meta` is the fold-then-unfold whose
  * existential **`X = A` is the neck** — the intermediate value the fold produces and the unfold
  * consumes.
  *
  * That non-trivial `X` is the thesis stating the obvious honestly: `meta` **cannot fuse**. It
  * folds a functor `F` down to `A`, then unfolds a *different* functor `G` back up; with `F ≠ G`
  * there is no `project ∘ embed` cancellation to ride, so `A` is genuinely materialised. (Contrast
  * `hylo` / `chrono`, whose single shared functor makes the neck cancel — `X = Nothing`.) Built by
  * [[Cata.meta]] / [[Histo.meta]] or [[Schemes.meta]] / [[Schemes.metaChrono]].
  *
  * @tparam A
  *   the neck — the retained intermediate value type (the optic's existential `X`)
  */
final class Meta[S, A, T](private[zoo] val run: S => T) extends Optic[S, Unit, T, Unit, Scheme]:
  type X = A
  def to(s: S): Scheme[X, T] = Scheme(run(s))
  def from(b: Scheme[X, Unit]): Unit = ()
