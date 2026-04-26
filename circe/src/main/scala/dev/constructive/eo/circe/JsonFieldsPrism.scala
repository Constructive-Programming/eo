package dev.constructive.eo.circe

/** Compatibility alias for the multi-field Prism shape.
  *
  * '''2026-04-26 unification.''' `JsonFieldsPrism[A]` used to be a separate class with its own
  * (parentPath, fieldNames, encoder, decoder) storage and 200 lines of read/modify/transform/place
  * surface that mirrored [[JsonPrism]] line-for-line. The two classes only differed in the "where
  * does the focus live" axis (single leaf vs assembled NamedTuple from selected fields), which is
  * now factored into [[JsonFocus]]. A multi-field prism is just a `JsonPrism[NT]` whose focus
  * happens to be a [[JsonFocus.Fields]]; this alias keeps old type ascriptions compiling.
  *
  * Users seeing this alias in error messages should read it as "a `JsonPrism[A]` whose focus
  * assembles `A` (a NamedTuple) from selected fields" — same surface, same Optic supertype, same
  * Ior diagnostics.
  */
type JsonFieldsPrism[A] = JsonPrism[A]
