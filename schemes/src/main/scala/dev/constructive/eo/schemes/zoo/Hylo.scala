package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Hylomorphism citizen — the **fused** refold ([[ReadScheme]]) with `X = Nothing`. Built by
  * [[Ana.cross]] or [[Hylo.apply]]; carries only the fused `refold`, no tree. Not a primitive — it
  * *is* `ana.cross(cata)`.
  *
  * The fused-refold family ([[Hylo]] / [[Chrono]] / [[Dyna]] / [[Codyna]] / [[Elgot]] /
  * [[Coelgot]]) share this shape — a `refold: Seed => A` with a vestigial build side, so
  * `X = Nothing` for all of them honestly. They are **nominally distinct** named types (one per
  * construction), not different existential indices: fusion is exactly the act of discarding the
  * intermediate index.
  */
final class Hylo[Seed, A] private[zoo] (private val refold: Seed => A) extends ReadScheme[Seed, A]:
  type X = Nothing
  protected def read(s: Seed): A = refold(s)

object Hylo:

  /** The fused refold `Seed => A`, building **no intermediate `S`** (needs only `Traverse[F]`).
    * `coalg` unfolds a seed into one typed layer; `alg` folds the layer's results (node-blind, like
    * [[Cata]]). Definitionally `ana(coalg).cross(cata(alg))`. Stack-safe.
    */
  def apply[F[_], Seed, A](coalg: Seed => F[Seed], alg: F[A] => A)(using
      F: Traverse[F]
  ): Hylo[Seed, A] =
    new Hylo[Seed, A](Machines.foldLayered[F, Seed, A](coalg, (_, fr) => alg(fr)))
