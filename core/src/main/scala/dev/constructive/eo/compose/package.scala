package dev.constructive.eo

/** The composition seam: [[AssociativeFunctor]] powers the generic `Optic.andThen` (threading an
  * inner carrier through an outer one), [[Composer]] names the carrier-pair-specific fusions,
  * [[Morph]] re-expresses an optic over a different carrier so cross-carrier `andThen` can pick a
  * direction, and [[ReadCompose]] routes read-only collapses (getter ∘ getter and friends) without
  * building intermediate structure.
  */
package object compose
