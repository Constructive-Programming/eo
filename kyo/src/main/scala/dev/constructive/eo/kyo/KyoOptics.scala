package dev.constructive.eo
package kyo

import scala.annotation.unused

import _root_.kyo.*

import optics.Lens

/** Kyo dependency-injection integration (kyo-prelude only — Env / Var / Layer / TypeMap; no
  * kyo-core IO).
  *
  * The same three seams as `cats-eo-zio`, at Kyo's names:
  *
  *   - [[service]] — a `TypeMap[R]` slot is a lawful Lens (`get[A]` / `add`), exactly like
  *     `ZEnvironment`: type-indexed maps are the DI substrate on both sides. Compose with field
  *     optics to drill from an environment map into a service field (`Env.runAll` test overrides).
  *   - [[focus(Env)]] / [[focus(Layer)]] — `Env.use` routed through `CanGet`, and `Layer.from` IS a
  *     `CanGet`-shaped wiring function, so an aggregate config service projects to sub-service
  *     layers through the optic that names the field.
  *   - `Var` focus ops — the runtime mutation analogue, mirroring the `zio.Ref` extensions. Kyo's
  *     surface is companion-static (`Var.update`, `Env.get`), so these extend the companion
  *     objects: `Var.updateFocus[Db, String](f)` reads like native Kyo.
  */

/** Lens from a `TypeMap[R]` onto its tagged `A` slot. Lawful for the same reason the `ZEnvironment`
  * service lens is: `add` on a present tag replaces exactly that entry, sibling slots are untouched
  * leftovers (`TypeMap[R & A] <: TypeMap[R]` by covariance). Returns the fused `GetReplaceLens`, so
  * drilling composes on the concrete hot path.
  */
def service[R, A >: R](using Tag[A]) =
  Lens[TypeMap[R], A](_.get[A], (tm, a) => tm.add(a))

extension (@unused E: Env.type)

  /** Read a focus out of the `R` service in the environment — `Env.use` through `CanGet` instead of
    * an ad-hoc projection lambda: `Env.focus[AppConfig, DbUrl]`.
    */
  def focus[R, A](using g: CanGet[R, A])(using Tag[R], Frame): A < Env[R] =
    Env.use[R](s => g.get(s))

extension (@unused L: Layer.type)

  /** Derive an `A` service layer from an `S` service by focusing — the optic IS the wiring:
    * `Layer.focus[AppConfig, DbConfig]` given a lens onto the `db` field, for `Env.runLayer`
    * graphs.
    */
  def focus[S, A](using g: CanGet[S, A])(using Tag[S], Tag[A], Frame): Layer[A, Env[S]] =
    Layer.from((s: S) => g.get(s))

extension (@unused V: Var.type)

  /** Read the focus of the current `Var` state. */
  def getFocus[S, A](using g: CanGet[S, A])(using Tag[Var[S]], Frame): A < Var[S] =
    Var.use[S](s => g.get(s))

  /** Read a partial focus (Prism / Optional / AffineFold evidence) of the current state. */
  def getFocusOption[S, A](using
      g: CanGetOption[S, A]
  )(using Tag[Var[S]], Frame): Option[A] < Var[S] =
    Var.use[S](s => g.getOption(s))

  /** Rewrite the focus in one `Var.updateDiscard` pass — one `CanModify`, not split get + set
    * evidence.
    */
  def updateFocus[S, A](f: A => A)(using
      m: CanModify[S, A]
  )(using Tag[Var[S]], Frame): Unit < Var[S] =
    Var.updateDiscard[S](m.modify(f))

  /** Overwrite the focus. */
  def setFocus[S, A](a: A)(using m: CanModify[S, A])(using Tag[Var[S]], Frame): Unit < Var[S] =
    Var.updateDiscard[S](m.replace(a))
