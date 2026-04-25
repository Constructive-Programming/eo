package dev.constructive.eo.circe

/** One step on a [[JsonPrism]]'s flat navigation path — a field name or an array index.
  *
  * The path walker dispatches on the case to decide which of circe's representations to pierce:
  * `JsonObject` for named fields, or the underlying `Vector[Json]` for array indices.
  *
  * Visibility is package-default (public) so users can read `PathStep` values off [[JsonFailure]]
  * instances exposed by the default [[JsonPrism]] / [[JsonTraversal]] Ior-bearing surface. The
  * `Field(name)` and `Index(i)` constructors are stable across v0.2.
  */
enum PathStep:
  case Field(name: String)
  case Index(i: Int)
