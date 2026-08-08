package dev.constructive.eo
package zio

import _root_.zio.stm.{TMap, TRef, USTM}
import _root_.zio.{Exit, Ref, Tag, Trace, UIO, URIO, ZEnvironment, ZIO, ZLayer}

import optics.{GetReplaceLens, Lens, PickMendPrism, Prism}

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

// ---- STM focus ops ------------------------------------------------------
//
// The same four ops on `TRef` (and keyed `-At` variants on `TMap`),
// returning `USTM`: focused updates across SEVERAL transactional
// references compose into ONE atomic transaction under
// `STM.atomically` — something the `Ref` ops structurally cannot
// express. They live in this file deliberately: same-name extension
// groups only form one overload set when declared in the same scope,
// and a separate file breaks `ref.updateFocus[String](f)(using myLens)`
// at every import site.

extension [S](ref: TRef[S])

  /** Read the focus of the current value. */
  def getFocus[A](using g: CanGet[S, A]): USTM[A] =
    ref.get.map(g.get)

  /** Read a partial focus (Prism / Optional / AffineFold evidence) of the current value. */
  def getFocusOption[A](using g: CanGetOption[S, A]): USTM[Option[A]] =
    ref.get.map(g.getOption)

  /** Rewrite the focus — one `CanModify`, one `TRef.update` pass, atomic within the transaction. */
  def updateFocus[A](f: A => A)(using m: CanModify[S, A]): USTM[Unit] =
    ref.update(m.modify(f))

  /** Overwrite the focus. */
  def setFocus[A](a: A)(using m: CanModify[S, A]): USTM[Unit] =
    ref.update(m.replace(a))

extension [K, V](tmap: TMap[K, V])

  /** Read the focus of the value at `k` — `None` when the key is absent. (Named `-At`, not
    * `getFocus`: a keyed overload has a different parameter shape from the `Ref`/`TRef` ops, and
    * mixed-shape extension overloads break explicit `(using myLens)` calls on ALL of them.)
    */
  def getFocusAt[A](k: K)(using g: CanGet[V, A]): USTM[Option[A]] =
    tmap.get(k).map(_.map(g.get))

  /** Rewrite the focus of the value at `k` — absent keys pass through untouched (no entry is
    * created), present ones update in place.
    */
  def updateFocusAt[A](k: K)(f: A => A)(using m: CanModify[V, A]): USTM[Unit] =
    tmap.updateWith(k)(_.map(m.modify(f))).unit

// ---- automatic capability provision ------------------------------------
//
// `import dev.constructive.eo.zio.given` and ZIO's types provide eo
// capabilities on their own — generic capability-consuming code
// (`def render[T](t: T)(using CanGet[T, Config])`) accepts a ZIO subject
// with no hand-written given. Both givens are declared at the concrete
// optic class, so the class's own capability mixins satisfy the direct
// summons (lexical givens win over the capability companions'
// derivations) and the `Optic` facet feeds the derived ones
// (`CanModifyF`, ...). Coherence: this is the ONE optic given per
// `(S, A)` pair for these types — don't add competing ones.

/** Service-slot optic given for `ZEnvironment[R]`: `CanGet` / `CanModify` / `CanFold` (and the
  * `Optic`-derived capabilities) for every tagged `A` of `R`, summoned per pair through
  * [[service]].
  */
given environmentOptic[R, A >: R](using
    Tag[A]
): GetReplaceLens[ZEnvironment[R], ZEnvironment[R], A, A] =
  service[R, A]

/** Success prism given for `Exit[E, A]`: `CanGetOption` / `CanModify` / `CanReverseGet` / `CanFold`
  * on the completed branch; failed exits pass through writes untouched.
  */
given exitOptic[E, A]: PickMendPrism[Exit[E, A], A, A] =
  Prism.optional(_.foldExit(_ => None, Some(_)), Exit.succeed)
