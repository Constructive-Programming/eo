package dev.constructive.eo.avro

/** Compatibility alias for the multi-field Prism shape on the Avro carrier.
  *
  * The "where does the focus live" axis (single leaf vs NamedTuple assembled from selected fields)
  * is factored into the internal `AvroFocus` storage, so a multi-field prism is just an
  * `AvroPrism[NT]` whose focus happens to be a Fields focus; this alias keeps any downstream type
  * ascriptions compiling.
  *
  * Users seeing this alias in error messages should read it as "an `AvroPrism[A]` whose focus
  * assembles `A` (a NamedTuple) from selected fields" — same surface, same byte-carried Optic
  * supertype, same Ior diagnostics behind `.record`.
  *
  * Mirrors `dev.constructive.eo.circe.JsonFieldsPrism` (which is similarly a `type =` alias for
  * `JsonPrism[A]`).
  */
type AvroFieldsPrism[A] = AvroPrism[A]
