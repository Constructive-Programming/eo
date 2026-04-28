package dev.constructive.eo

import dev.constructive.eo.optics.Optic

/** Auto-derivation entry points for EO optics — Scala 3 quoted macros for Lens / Prism. Scaffolds
  * on Mateusz Kubuszok's [hearth](https://github.com/MateuszKubuszok/hearth) macro-commons library.
  */
package object generics:

  /** Derive a `Lens` (or, on full coverage, an `Iso`) from one or more case-class field accessors.
    * Two-step partial application — `lens[Person](_.age)` — pins `S` while letting the focus type
    * be inferred. Works on any N-field case class or Scala 3 enum case (emits `new S(…)`, doesn't
    * require `.copy`).
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val ageLens = lens[Person](_.age)
    * val asTuple = lens[Person](_.name, _.age)  // full cover → Iso
    *   }}}
    */
  def lens[S]: PartiallyAppliedLens[S] = new PartiallyAppliedLens[S]

  /** Partially-applied witness produced by [[lens]]. */
  final class PartiallyAppliedLens[S]:

    /** Varargs entry — `transparent inline` so the concrete subclass (`SimpleLens` partial /
      * `BijectionIso` full) propagates to the call site, picking up fused `.andThen` overloads.
      *
      * @group Constructors
      */
    transparent inline def apply(
        inline selectors: (S => Any)*
    ): Optic[S, S, ?, ?, ?] =
      LensMacro.deriveMulti[S](selectors*)

  /** Derive a `Prism` focusing on one variant of a sum type. Works on Scala 3 enums, sealed traits,
    * and union types via Hearth's `Enum.parse[S]`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * enum Shape:
    *   case Circle(r: Double)
    *   case Square(s: Double)
    * val circleP = prism[Shape, Shape.Circle]
    * val intP    = prism[Int | String, Int]
    *   }}}
    */
  inline def prism[S, A <: S]: Optic[S, S, A, A, Either] =
    PrismMacro.derive[S, A]
