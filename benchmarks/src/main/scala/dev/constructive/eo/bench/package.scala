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

  // Optic extensions (`.andThen`, `.morph`, …) need to be re-exported from the optics
  // companion-object; an `export` brings each extension into the package's scope so bench files
  // don't have to write `import dev.constructive.eo.optics.Optic.*`. The names the root package
  // `dev.constructive.eo` already exports — overloaded there with the subject-first capability
  // extensions (`s.get[A]`, `s.modifyF[A]`, …) — are excluded: re-exporting them here would
  // shadow the outer overload set and hide the subject-first spellings from bench code.
  export dev.constructive.eo.optics.Optic.{
    exists as _,
    foldMap as _,
    get as _,
    getOption as _,
    headOption as _,
    length as _,
    modify as _,
    modifyF as _,
    place as _,
    put as _,
    replace as _,
    reverseGet as _,
    transfer as _,
    transform as _,
    *,
  }

  // Renamed re-exports for the EO carriers used by every bench class.
  export dev.constructive.eo.optics.{
    Getter => EoGetter,
    Lens => EoLens,
    Prism => EoPrism,
    Modify => EoModify,
    Traversal => EoTraversal,
  }
