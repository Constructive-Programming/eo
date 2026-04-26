package dev.constructive.eo

/** Package-level re-exports for the bench module.
  *
  * '''2026-04-26 dedup.''' Every bench file used to spell the same import boilerplate at the top:
  *
  * {{{
  *   import dev.constructive.eo.optics.Optic.*
  *   import dev.constructive.eo.optics.{Lens => EoLens, Prism => EoPrism, Traversal => EoTraversal}
  *   import scala.collection.immutable.ArraySeq
  *   import scala.compiletime.uninitialized
  * }}}
  *
  * The re-exports below pull the most-frequently-imported names into package scope so per-file
  * imports drop to the bench-specific subset (e.g. `cats.instances.arraySeq.given` for benches that
  * need cats's collection instances, the per-bench fixture wildcard).
  */
package object bench:

  // Optic extensions (`.modify`, `.replace`, `.andThen`, `.morph`, …) need to be re-exported
  // from the optics companion-object; an `export` brings each extension into the package's
  // scope so bench files don't have to write `import dev.constructive.eo.optics.Optic.*`.
  export dev.constructive.eo.optics.Optic.*

  // Renamed re-exports for the EO carriers used by every bench class.
  export dev.constructive.eo.optics.{
    Getter => EoGetter,
    Lens => EoLens,
    Prism => EoPrism,
    Setter => EoSetter,
    Traversal => EoTraversal,
  }
