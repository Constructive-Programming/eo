package dev.constructive.eo.circe

/** Compatibility alias for the multi-field Prism shape — a [[JsonPrism]] whose focus assembles `A`
  * (a NamedTuple) from selected fields under a parent object. Same surface, same Optic supertype,
  * same Ior diagnostics; the single- vs multi-field split lives in the internal `JsonFocus`. Kept
  * so old type ascriptions keep compiling.
  */
type JsonFieldsPrism[A] = JsonPrism[A]
