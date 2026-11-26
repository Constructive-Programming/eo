package dev.constructive.eo

/** The specs2 / ScalaCheck / discipline names every module's specs use, in scope for any spec whose
  * package clause chains through `dev.constructive.eo` (`package dev.constructive.eo` then
  * `package avro`). A module's own `TestPrelude` adds only what is particular to it.
  *
  * No `forAll` here: munit suites inherit one, and an inherited member clashes with a package-level
  * one.
  */
export org.scalacheck.{Arbitrary, Cogen, Gen, Prop}
export org.specs2.ScalaCheck
export org.specs2.mutable.Specification
export org.typelevel.discipline.specs2.mutable.Discipline
