package dev.constructive.eo
package optics

/** The write row of a concrete optic: plain modify ([[CanModifyP]]) and effectful modify
  * ([[CanModifyFP]]) at the effect gate `C` its carrier needs — `Functor` for a lens, `Invariant`
  * for an iso, `Applicative` for a prism or optional. One mixin so each class names its gate once.
  */
trait Writes[S, T, A, B, -C[_[_]]] extends CanModifyP[S, T, A, B], CanModifyFP[S, T, A, B, C]
