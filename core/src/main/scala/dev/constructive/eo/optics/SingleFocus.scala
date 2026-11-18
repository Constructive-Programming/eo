package dev.constructive.eo
package optics

import compose.Composer
import data.{MultiFocus, PSVec}

/** The shared row of the concrete single-focus optics — lens, iso, prism, optional: plain modify
  * ([[CanModifyP]]) and effectful modify ([[CanModifyFP]]) at the effect gate `C` the carrier needs
  * (`Functor` for a lens, `Invariant` for an iso, `Applicative` for a prism or optional), and
  * composition with a [[Traversal]].
  */
trait SingleFocus[S, T, A, B, F[_, _], -C[_[_]]]
    extends Optic[S, T, A, B, F],
      CanModifyP[S, T, A, B],
      CanModifyFP[S, T, A, B, C]:

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
