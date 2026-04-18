package eo

import eo.optics.{Optic, SimpleLens}

/** Auto-derivation entry points for EO optics.
  *
  * This module contains Scala 3 quoted macros that synthesise the
  * boilerplate usually written by hand for Lens / Prism.
  *
  * The internal scaffolding hooks into Mateusz Kubuszok's
  * [hearth](https://github.com/MateuszKubuszok/hearth) macro-commons
  * library so richer derivations (recursive lenses, whole-ADT
  * Prism tables, etc.) can be added without restructuring the
  * call-site surface.
  */
package object generics:

  /** Derive a `Lens` from a case-class field accessor.
    *
    * Two-step partial application: `lens[Person](_.age)`. This lets
    * callers pin `S` while letting `A` be inferred from the selector
    * (the same pattern Monocle uses for `GenLens[S](_.field)`).
    *
    * Works on any N-field case class or Scala 3 enum case — the
    * emitted `new S(…)` constructor call avoids the `.copy`
    * requirement that blocks derivation for enum cases.
    *
    * @group Constructors
    *
    * @example
    * {{{
    * case class Person(name: String, age: Int)
    * val ageLens = lens[Person](_.age)
    * ageLens.get(Person("Alice", 30))        // 30
    * ageLens.replace(31)(Person("Alice", 30))// Person("Alice", 31)
    * }}}
    */
  def lens[S]: PartiallyAppliedLens[S] = new PartiallyAppliedLens[S]

  final class PartiallyAppliedLens[S]:
    // `transparent inline` so the specific `XA` the macro synthesises
    // (EmptyTuple for 1-field, sibling type for 2-field) propagates to
    // the call site, enabling transform/place/transfer without any
    // external evidence.
    transparent inline def apply[A](
        inline selector: S => A,
    ): SimpleLens[S, A, ?] =
      LensMacro.derive[S, A](selector)

  /** Derive a `Prism` focusing on a single variant of a sum type.
    *
    * Works on Scala 3 enums, sealed traits, and union types — the
    * macro recognises all three through Hearth's `Enum.parse[S]`
    * and emits the appropriate pattern-match.
    *
    * @group Constructors
    *
    * @example
    * {{{
    * enum Shape:
    *   case Circle(r: Double)
    *   case Square(s: Double)
    *
    * val circleP = prism[Shape, Shape.Circle]
    *
    * // Works on union types too:
    * val intP = prism[Int | String, Int]
    * }}}
    */
  inline def prism[S, A <: S]: Optic[S, S, A, A, Either] =
    PrismMacro.derive[S, A]
