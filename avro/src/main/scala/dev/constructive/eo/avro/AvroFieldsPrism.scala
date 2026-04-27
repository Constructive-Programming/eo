package dev.constructive.eo.avro

/** Compatibility alias for the multi-field Prism shape on the Avro carrier.
  *
  * '''2026-04-27 (Unit 5).''' `AvroFieldsPrism[A]` was originally planned as a separate class with
  * its own `(parentPath, fieldNames, codec, rootSchema)` storage and a parallel set of
  * read/modify/transform/place hooks mirroring [[AvroPrism]] line-for-line. The two would only
  * differ in the "where does the focus live" axis (single leaf vs assembled NamedTuple from
  * selected fields), which is now factored into [[AvroFocus]]. A multi-field prism is just an
  * `AvroPrism[NT]` whose focus happens to be an [[AvroFocus.Fields]]; this alias keeps any
  * downstream type ascriptions compiling.
  *
  * Users seeing this alias in error messages should read it as "an `AvroPrism[A]` whose focus
  * assembles `A` (a NamedTuple) from selected fields" — same surface, same Optic supertype, same
  * Ior diagnostics.
  *
  * Mirrors `dev.constructive.eo.circe.JsonFieldsPrism` (which is similarly a `type =` alias for
  * `JsonPrism[A]`).
  */
type AvroFieldsPrism[A] = AvroPrism[A]
