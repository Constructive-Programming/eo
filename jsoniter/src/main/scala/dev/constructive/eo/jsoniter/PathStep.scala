package dev.constructive.eo.jsoniter

/** One step of a JSON path expression. The supported subset matches v1 spike scope: object-field
  * dive, array-index pick. Wildcards / filters / recursive descent are deferred to phase 2.
  *
  * @group AST
  */
enum PathStep:

  /** `.field` — descend into an object property by exact name. */
  case Field(name: String)

  /** `[i]` — descend into an array element by zero-based index. */
  case Index(i: Int)
