package dev.constructive.eo
package zio

import _root_.zio.{Ref, Tag, Trace, UIO, URIO, ZEnvironment, ZIO, ZLayer}

import optics.Lens

/** ZIO dependency-injection integration.
  *
  * Three seams, none of which invents a new carrier:
  *
  *   - [[service]] — every tagged service slot of a `ZEnvironment[R]` is a lawful Lens
  *     (`env.get[A]` / `env.update[A]`), so environment surgery (test overrides,
  *     `provideSomeEnvironment`) composes with ordinary field optics: drill from the environment
  *     into a field of a service with the same `.andThen` as everywhere else.
  *   - [[focusLayer]] / [[serviceFocus]] — ZLayer's DI plan wires services from other services;
  *     `CanGet[S, A]` is exactly a wiring function, so an aggregate `Config` service projects to
  *     sub-service layers through the optic that names the field.
  *   - `Ref` focus ops — capability-driven reads/writes of a focus inside a `Ref[S]`, the runtime
  *     mutation analogue. `zio.Ref` is sealed, so these are extensions demanding the weakest
  *     capability, not a wrapped `Ref[A]` view — which is the doctrine anyway: consume via
  *     capability, construct via optic.
  */

/** Lens from a `ZEnvironment[R]` onto its tagged `A` service slot. Lawful because `ZEnvironment` is
  * a type-indexed map: get-put, put-get, put-put all hold per slot, and sibling services are
  * untouched leftovers. Returns the fused `GetReplaceLens`, so `service[R, Config].andThen(dbUrlL)`
  * stays on the concrete hot path and carries the capability mixins.
  */
def service[R, A >: R](using Tag[A]) =
  Lens[ZEnvironment[R], A](_.get[A], (env, a) => env.update[A](_ => a))

/** Read a focus out of the `S` service in the environment — `ZIO.serviceWith` routed through
  * `CanGet` instead of an ad-hoc projection lambda.
  */
def serviceFocus[S, A](using Tag[S])(using g: CanGet[S, A])(using Trace): URIO[S, A] =
  ZIO.serviceWith[S](g.get)

/** Derive an `A` service layer from an `S` service by focusing — the optic IS the wiring: e.g.
  * `focusLayer[AppConfig, DbConfig]` given a lens onto the `db` field. Sub-service layers from one
  * aggregate config service, without hand-written `ZLayer.fromFunction` projections.
  */
def focusLayer[S, A](using
    Tag[S],
    Tag[A]
)(using
    g: CanGet[S, A]
)(using
    Trace
): ZLayer[S, Nothing, A] =
  ZLayer.fromZIO(ZIO.serviceWith[S](g.get))

extension [S](ref: Ref[S])

  /** Read the focus of the current value. */
  def getFocus[A](using g: CanGet[S, A])(using Trace): UIO[A] =
    ref.get.map(g.get)

  /** Read a partial focus (Prism / Optional / AffineFold evidence) of the current value. */
  def getFocusOption[A](using g: CanGetOption[S, A])(using Trace): UIO[Option[A]] =
    ref.get.map(g.getOption)

  /** Atomically rewrite the focus. One `CanModify`, not split get + set evidence — the update is a
    * single `Ref.update` pass.
    */
  def updateFocus[A](f: A => A)(using m: CanModify[S, A])(using Trace): UIO[Unit] =
    ref.update(m.modify(f))

  /** Atomically overwrite the focus. */
  def setFocus[A](a: A)(using m: CanModify[S, A])(using Trace): UIO[Unit] =
    ref.update(m.replace(a))
