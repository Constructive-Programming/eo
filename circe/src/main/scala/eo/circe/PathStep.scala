package eo.circe

/** One step on a [[JsonPrism]]'s flat navigation path.
  *
  * The path walker dispatches on the case to decide which of circe's representations to pierce:
  * `JsonObject` for named fields, or the underlying `Vector[Json]` for array indices. `Each` is
  * reserved for the traversal path and expands at run time to a loop over every element of the
  * array at that point.
  */
private[circe] enum PathStep:
  case Field(name: String)
  case Index(i: Int)
