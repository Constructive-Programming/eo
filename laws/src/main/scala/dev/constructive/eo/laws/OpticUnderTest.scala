package dev.constructive.eo
package laws

import optics.Optic

/** The monomorphic optic a law trait states its equations about — the shared header of the
  * optic-level law traits (`ModifyA*Laws`, `FoldMapHomomorphismLaws`, `MorphLaws`).
  */
trait OpticUnderTest[S, A, F[_, _]]:
  /** The optic under test. */
  def optic: Optic[S, S, A, A, F]
