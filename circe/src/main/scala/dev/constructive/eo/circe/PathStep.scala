package dev.constructive.eo.circe

/** One step on a [[JsonPrism]]'s flat navigation path — a field name or an array index.
  *
  * The path walker dispatches on the case to decide which of circe's representations to pierce:
  * `JsonObject` for named fields, or the underlying `Vector[Json]` for array indices.
  *
  * Public so users can read `PathStep` values off [[JsonFailure]] instances exposed by the default
  * [[JsonPrism]] / [[JsonTraversal]] Ior-bearing surface.
  */
enum PathStep:

  /** Descend into the named object field. */
  case Field(name: String)

  /** Descend into the array element at zero-based index `i`. */
  case Index(i: Int)
