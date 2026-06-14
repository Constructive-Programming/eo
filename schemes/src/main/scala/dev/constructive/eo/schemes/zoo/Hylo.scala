package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

import data.Direct
import optics.Optic

/** Hylomorphism citizen — the **fused** refold worn as an optic over
  * [[dev.constructive.eo.data.Direct]] with `X = Nothing`. `Getter`-shaped (`Optic[Seed, Unit, A,
  * Unit, Direct]`), consumed via `.get`. Built by [[Ana.cross]] or [[Hylo.apply]]; carries only the
  * fused `refold`, no tree. Not a primitive — it *is* `ana.cross(cata)`.
  *
  * The fused-refold family ([[Hylo]] / [[Chrono]] / [[Dyna]] / [[Codyna]] / [[Elgot]] /
  * [[Coelgot]]) share this runtime shape — a `refold: Seed => A` with a vestigial build side, so
  * `X = Nothing` for all of them honestly. They are **nominally distinct** named types (one per
  * construction), not different existential indices: fusion is exactly the act of discarding the
  * intermediate index.
  */
final class Hylo[Seed, A] private[zoo] (private[zoo] val refold: Seed => A)
    extends Optic[Seed, Unit, A, Unit, Direct]:
  type X = Nothing
  def to(s: Seed): Direct[X, A] = Direct(refold(s))
  def from(b: Direct[X, Unit]): Unit = ()

object Hylo:

  /** The fused refold `Seed => A`, building **no intermediate `S`** (needs only `Traverse[F]`).
    * `coalg` unfolds a seed into one typed layer; `alg` folds the layer's results (node-blind, like
    * [[Cata]]). Definitionally `ana(coalg).cross(cata(alg))`. Stack-safe.
    */
  def apply[F[_], Seed, A](coalg: Seed => F[Seed], alg: F[A] => A)(using
      F: Traverse[F]
  ): Hylo[Seed, A] =
    new Hylo[Seed, A](Machines.foldLayered[F, Seed, A](coalg, (_, fr) => alg(fr)))
