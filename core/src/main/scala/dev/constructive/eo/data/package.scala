package dev.constructive.eo

/** Carriers — the `F[_, _]` shapes an [[optics.Optic]]'s `to` / `from` run through: [[Direct]]
  * (plain focus, no leftover), `Either` (prism miss), [[Affine]] (miss '''or''' hit-with-context),
  * [[Forget]] / opaque `ForgetK` (read-only collapse), [[ModifyF]] (effectful write),
  * [[MultiFocus]] / opaque `MultiFocusK` (many foci), plus the perf substrate ([[PSVec]] and the
  * array builders). Each carrier's companion hosts its typeclass instances, so composition resolves
  * with no imports.
  */
package object data
