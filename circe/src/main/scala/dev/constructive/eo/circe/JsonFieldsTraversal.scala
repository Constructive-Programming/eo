package dev.constructive.eo.circe

/** Compatibility alias for the multi-field Traversal shape.
  *
  * '''2026-04-26 unification.''' Originally a separate class. Now a `JsonTraversal[A]` whose focus
  * is a [[JsonFocus.Fields]] — the elemParent / fieldNames / encoder / decoder storage moved into
  * the `Fields` focus, and the per-element walk delegates to it. See [[JsonPrism]]'s class comment
  * for the rethink history.
  */
type JsonFieldsTraversal[A] = JsonTraversal[A]
