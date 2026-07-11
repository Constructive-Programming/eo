package dev.constructive.eo.circe

/** Compatibility alias for the multi-field Traversal shape — a [[JsonTraversal]] whose per-element
  * focus assembles a NamedTuple from selected fields (the internal `JsonFocus.Fields`). Kept so old
  * type ascriptions keep compiling.
  */
type JsonFieldsTraversal[A] = JsonTraversal[A]
