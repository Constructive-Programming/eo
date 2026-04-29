package dev.constructive.eo.jsoniter

/** One step of a JSON path expression. Phase-1.5 scope:
  *
  *   - `.field` — descend into an object property by exact name (powers [[JsoniterPrism]]).
  *   - `[i]` — descend into an array element by zero-based index (powers [[JsoniterPrism]]).
  *   - `[*]` — fan out across every element of the current array (powers [[JsoniterTraversal]]).
  *
  * Filters / recursive descent are still deferred to phase 2+.
  *
  * @group AST
  */
enum PathStep:

  /** `.field` — descend into an object property by exact name. */
  case Field(name: String)

  /** `[i]` — descend into an array element by zero-based index. */
  case Index(i: Int)

  /** `[*]` — fan out: when scanning, this step expands the current array into one branch per
    * element, each continuing with the remaining path. Only meaningful inside a multi-focus
    * traversal — [[JsoniterPrism]] rejects paths containing this step at construction.
    */
  case Wildcard
