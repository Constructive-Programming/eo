package dev.constructive.eo

/** Law definitions for every optic family — one trait per shape (`LensLaws`, `PrismLaws`,
  * `IsoLaws`, `OptionalLaws`, `TraversalLaws`, `FoldLaws`, `GetterLaws`, `ModifyLaws`,
  * `MultiFocusLaws`, `PlatedLaws`, `UnfoldLaws`, `SeamLaws`, …), each method one equation. Reusable
  * by downstream projects; the ScalaCheck/specs2 bundles live in [[laws.discipline]], with
  * eo-internal seam laws under [[laws.eo]], carrier laws under [[laws.data]] and typeclass laws
  * under [[laws.typeclass]].
  *
  * This module ships law equations and rule-sets ONLY — no `Arbitrary`, `Cogen`, or `Eq` instances.
  * Your suite supplies the generators for its own `S` / `A` types (and, for [[SeamLaws]], the
  * structural equality — see below).
  *
  * '''Codec-backed optics''' (Avro records, serialised JSON, …) are lawful only ''up to canonical
  * re-encoding'': the family laws compare with raw `==` (mirroring Monocle), which a
  * decode-then-re-encode round-trip can fail for representation-sensitive equality (e.g. Avro's
  * schema-instance-sensitive `GenericData.Record.equals`) even when the values are structurally
  * equal. Run those laws on canonical fixtures — sources round-tripped through the codec once — or
  * on a full-cover face whose rebuild goes through the same codec instance; [[SeamLaws]] instead
  * takes an INJECTED equality for exactly this reason. `AvroPrismLawsSpec` in the `avro` module is
  * the worked reference.
  */
package object laws
