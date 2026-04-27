package dev.constructive.eo.avro

/** Compatibility alias for the multi-field Traversal shape on the Avro carrier.
  *
  * '''2026-04-27 (Unit 6).''' Originally planned as a separate class. Now an `AvroTraversal[A]`
  * whose focus is an [[AvroFocus.Fields]] — the parentPath / fieldNames / codec storage moved into
  * the `Fields` focus, and the per-element walk delegates to it. See [[AvroPrism]]'s class comment
  * for the rethink history; mirrors `dev.constructive.eo.circe.JsonFieldsTraversal`.
  */
type AvroFieldsTraversal[A] = AvroTraversal[A]
