package dev.constructive.eo
package optics

import compose.Composer
import data.{MultiFocus, PSVec}

/** The shared row of the concrete single-focus optics — lens, iso, prism, optional: their write row
  * ([[Writes]]) at the carrier's effect gate `C`, plus the compositions that need a single focus
  * (with a [[Traversal]], with a [[Modify]]).
  */
trait SingleFocus[S, T, A, B, F[_, _], -C[_[_]]]
    extends Optic[S, T, A, B, F],
      Writes[S, T, A, B, C]:

  /** Fused `andThen(Traversal)`: this optic lifted once onto `MultiFocus[PSVec]` through its
    * carrier's `Composer` and composed with the traversal, returning the concrete [[Traversal]] —
    * so the composite stays nameable as a `Traversal` and keeps its fused inline `modify` /
    * `replace` / streaming `foldMap` (the generic `Morph` route would yield an anonymous
    * `Optic[…, MultiFocus[PSVec]]`). A prism or optional that misses contributes zero foci, so
    * writes pass the source through.
    */
  def andThen[C0, D](inner: Traversal[A, B, C0, D])(using
      br: Composer[F, MultiFocus[PSVec]]
  ): Traversal[S, T, C0, D] =
    Traversal.composed(br.to(this), inner)

  /** Fused `andThen(Modify)`: the composite writer is this optic's own `modify` around the inner
    * writer — a [[Modify]], with no `ModifyF` carrier round-trip (the generic writable ∘ `Modify`
    * extension writes through the carrier on every call).
    */
  def andThen[C0, D](inner: Modify[A, B, C0, D]): Modify[S, T, C0, D] =
    new Modify(g => modify(inner.modifyFn(g)))
