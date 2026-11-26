package dev.constructive.eo

/** What the core specs share on top of spec-kit's `SpecPrelude`. Lives in `src/test` (JVM +
  * Scala.js) with the specs2 specs; munit and testkit sources, which also build for Scala Native,
  * keep their own imports.
  *
  * No carriers (`Direct`, `Affine`, …): an exported higher-kinded type is a type-lambda alias, and
  * a carrier inferred through it no longer reaches the givens in its companion (`Accessor[Direct]`
  * in `object Direct`). Typeclasses are fine: an applied alias reduces to the class. No `Optic.*`
  * either: in this package its extension methods would shadow core's top-level capability
  * extensions (`s.get[A]`).
  */
export dev.constructive.eo.accessor.{Accessor, PartialAccessor, ReverseAccessor}
export dev.constructive.eo.compose.{AssociativeFunctor, Composer, Morph, ReadCompose}
export dev.constructive.eo.forgetful.{
  ForgetfulApplicative,
  ForgetfulFold,
  ForgetfulFunctor,
  ForgetfulTraverse,
}
export dev.constructive.eo.optics.{Getter, Iso, Lens, Optic, Optional, Prism, Traversal}
