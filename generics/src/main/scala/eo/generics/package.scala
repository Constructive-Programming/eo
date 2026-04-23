package eo

import eo.optics.Optic

/** Auto-derivation entry points for EO optics.
  *
  * This module contains Scala 3 quoted macros that synthesise the boilerplate usually written by
  * hand for Lens / Prism.
  *
  * The internal scaffolding hooks into Mateusz Kubuszok's
  * [hearth](https://github.com/MateuszKubuszok/hearth) macro-commons library so richer derivations
  * (recursive lenses, whole-ADT Prism tables, etc.) can be added without restructuring the
  * call-site surface.
  */
package object generics:

  /** Derive a `Lens` — or on full coverage, an `Iso` — from one or more case-class field accessors.
    *
    * Two-step partial application: `lens[Person](_.age)`, `lens[Person](_.name, _.age)`. This lets
    * callers pin `S` while letting the focus type be inferred from the selectors (the same pattern
    * Monocle uses for `GenLens[S](_.field)`).
    *
    * Works on any N-field case class or Scala 3 enum case — the emitted `new S(…)` constructor call
    * avoids the `.copy` requirement that blocks derivation for enum cases.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    *
    * // Single-selector Lens — focus is the field type directly.
    * val ageLens = lens[Person](_.age)
    * ageLens.get(Person("Alice", 30))        // 30
    * ageLens.replace(31)(Person("Alice", 30))// Person("Alice", 31)
    *
    * // Multi-selector with full coverage — emits an Iso.
    * val asTuple = lens[Person](_.name, _.age)
    *   }}}
    */
  def lens[S]: PartiallyAppliedLens[S] = new PartiallyAppliedLens[S]

  final class PartiallyAppliedLens[S]:

    /** Unified varargs entry. `transparent inline` so the macro-synthesised concrete return type (a
      * `SimpleLens[S, A, XA]` on partial cover, `BijectionIso[S, S, T, T]` on full cover)
      * propagates to the call site. Chained `.andThen` picks up the fused concrete-subclass
      * overloads for free.
      */
    transparent inline def apply(
        inline selectors: (S => Any)*
    ): Optic[S, S, ?, ?, ?] =
      LensMacro.deriveMulti[S](selectors*)

  /** Derive a `Prism` focusing on a single variant of a sum type.
    *
    * Works on Scala 3 enums, sealed traits, and union types — the macro recognises all three
    * through Hearth's `Enum.parse[S]` and emits the appropriate pattern-match.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * enum Shape:
    *   case Circle(r: Double)
    *   case Square(s: Double)
    *
    * val circleP = prism[Shape, Shape.Circle]
    *
    * // Works on union types too:
    * val intP = prism[Int | String, Int]
    *   }}}
    */
  inline def prism[S, A <: S]: Optic[S, S, A, A, Either] =
    PrismMacro.derive[S, A]
