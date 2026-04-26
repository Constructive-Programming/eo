package dev.constructive.eo.avro

/** One step on an [[AvroPrism]]'s flat navigation path — a field name, an array index, or a union
  * branch.
  *
  * The path walker dispatches on the case to decide which of Avro's runtime representations to
  * pierce: [[org.apache.avro.generic.IndexedRecord]] for named record fields, [[java.util.List]]
  * (typically `org.apache.avro.generic.GenericData.Array`) for array indices, [[java.util.Map]] for
  * `map<T>` keys, and the runtime alternative directly for union branches.
  *
  * '''Gap-1 (per the eo-avro plan).''' This enum is duplicated from
  * `dev.constructive.eo.circe.PathStep` rather than shared. The two enums share `Field(name)` and
  * `Index(i)` but eo-avro adds `UnionBranch(branchName)` — Avro's schema-driven feature with no
  * JSON parallel. Sharing the type would force `UnionBranch` into eo-circe's dependency graph
  * (where unions don't exist), so the duplication is intentional. One file, ~25 LoC, deliberate
  * divergence.
  *
  * Visibility is package-default (public) so users can read [[PathStep]] values off [[AvroFailure]]
  * instances exposed by the default Ior-bearing surface. The `Field(name)`, `Index(i)`, and
  * `UnionBranch(branchName)` constructors are stable across v0.2.
  */
enum PathStep:

  /** Named field of a record (or string key of a `map<T>` — record-vs-map dispatch happens at the
    * walker, not at the path-step level).
    */
  case Field(name: String)

  /** Index into a `array<T>`. */
  case Index(i: Int)

  /** Resolve a `union<...>` to a specific branch by its type name. The branch name is the schema's
    * fully-qualified type name (e.g. `"long"` for the primitive long branch, `"my.ns.Record"` for a
    * record branch). Branches with the same type name across alternatives don't exist in valid Avro
    * schemas, so the name uniquely identifies the alternative.
    */
  case UnionBranch(branchName: String)
