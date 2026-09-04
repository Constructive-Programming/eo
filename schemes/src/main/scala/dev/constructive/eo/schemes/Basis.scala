package dev.constructive.eo
package schemes

/** The pattern-functor correspondence `Project` / `Embed` / `Basis` now lives in `core`
  * ([[dev.constructive.eo.optics.Basis]]) so that core's [[dev.constructive.eo.optics.Plated]] can
  * derive from it ([[dev.constructive.eo.optics.Plated.fromBasis]]). Re-exported here at the
  * `schemes` package level so every scheme citizen keeps referring to `Project` / `Embed` / `Basis`
  * unqualified, exactly as before the move.
  */
export optics.{Basis, Embed, Project}
