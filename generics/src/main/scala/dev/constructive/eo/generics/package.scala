package dev.constructive.eo

import dev.constructive.eo.optics.{Optic, Plated}

/** Auto-derivation entry points for EO optics — Scala 3 quoted macros for Lens / Prism / Plated.
  * Scaffolds on Mateusz Kubuszok's [hearth](https://github.com/MateuszKubuszok/hearth)
  * macro-commons library.
  *
  * Derived setters call the primary constructor directly (`new S(...)`, never `.copy`) — uniform
  * across case classes and enum cases, but the emitted call carries no outer accessor, so derive
  * only for '''top-level''' ADTs, not types nested inside a class.
  */
package object generics:

  /** Derive a lens from one or more case-class field accessors. Two-step partial application —
    * `lens[Person](_.age)` — pins `S` while letting the focus type be inferred. Works on any
    * N-field case class or Scala 3 enum case (emits `new S(…)`, doesn't require `.copy`).
    *
    * The return type is arity-and-cover driven:
    *   - partial cover, one selector → `SimpleLens[S, A, XA]` with `XA` the NamedTuple complement
    *     of the non-focused fields;
    *   - partial cover, N ≥ 2 selectors → `SimpleLens` with NamedTuple focus in '''selector'''
    *     order and NamedTuple complement in '''declaration''' order among non-focused fields;
    *   - full cover at any arity (including `lens[Wrapper](_.value)` on a 1-field case class) →
    *     `BijectionIso[S, S, NT, NT]`.
    *
    * Even at arity 1 the full-cover focus is a 1-field NamedTuple, so
    * `lens[Wrapper](_.value).get(w)` yields `(value = 42)`, not the bare `42`.
    *
    * Monocle users: this is `GenLens[S](_.field)` / `Focus[S](_.field)`, except nested paths are
    * compile errors (chain `.andThen`), full cover takes the `GenIso` role, and enum cases work.
    *
    * Selectors must be single-field accessors — nested paths (`_.a.b`), duplicates, and unknown
    * fields are compile errors.
    *
    * The declared `Optic[S, S, ?, ?, ?]` on `PartiallyAppliedLens.apply` is not the type you get:
    * `transparent inline` narrows the result to the concrete `SimpleLens` / `BijectionIso` at each
    * call site. Never ascribe the wildcard type to a derived optic — the ascription throws the
    * narrowing (and the fused overloads that ride on it) away.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val ageLens = lens[Person](_.age)
    * val asTuple = lens[Person](_.name, _.age)  // full cover → BijectionIso
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
      ${ LensMacro.deriveMultiImpl[S]('selectors) }

  /** Derive a `Prism` focusing on one variant of a sum type. Works on Scala 3 enums, sealed traits,
    * and union types via Hearth's `Enum.parse[S]`. `A` must be a '''direct child''' of `S` —
    * anything else is a compile error listing the known children. Monocle users: this is
    * `GenPrism`, extended to union types.
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
    ${ PrismMacro.deriveImpl[S, A] }

  /** Derive a [[dev.constructive.eo.optics.Plated]] for a recursive ADT — the immediate
    * same-typed-children self-traversal that powers `transform` / `rewrite` / `children` /
    * `universe`. Focuses every field whose type is exactly `S` across all cases; works on enums,
    * sealed hierarchies, and recursive case classes.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * enum Expr:
    *   case Var(name: String)
    *   case App(f: Expr, x: Expr)
    *   case Lam(bind: String, body: Expr)
    * given Plated[Expr] = plate[Expr]
    * Plated.transform { case Expr.Var(n) => Expr.Var(n.toUpperCase); case e => e }(tree)
    *   }}}
    */
  inline def plate[S]: Plated[S] =
    ${ PlateMacro.deriveImpl[S] }
