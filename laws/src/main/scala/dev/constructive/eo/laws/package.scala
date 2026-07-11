package dev.constructive.eo

/** Law definitions for every optic family — one trait per shape (`LensLaws`, `PrismLaws`,
  * `IsoLaws`, `OptionalLaws`, `TraversalLaws`, `FoldLaws`, `GetterLaws`, `ModifyLaws`,
  * `MultiFocusLaws`, `PlatedLaws`, `UnfoldLaws`, `SeamLaws`, …), each method one equation. Reusable
  * by downstream projects; the ScalaCheck/specs2 bundles live in [[laws.discipline]], with
  * eo-internal seam laws under [[laws.eo]], carrier laws under [[laws.data]] and typeclass laws
  * under [[laws.typeclass]].
  */
package object laws
