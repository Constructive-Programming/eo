package dev.constructive.eo.avro

/** Compatibility alias for the multi-field Traversal shape on the Avro carrier: an
  * `AvroTraversal[A]` whose per-element focus assembles `A` (a NamedTuple) from selected fields —
  * the parentPath / fieldNames / codec storage lives in the internal Fields focus, and the
  * per-element walk delegates to it (byte-carried by default; the record-carried Ior surface sits
  * behind `.record`). Mirrors `dev.constructive.eo.circe.JsonFieldsTraversal`; see
  * [[AvroFieldsPrism]] for the same factoring on the prism side.
  */
type AvroFieldsTraversal[A] = AvroTraversal[A]
