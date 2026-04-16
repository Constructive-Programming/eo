package eo
package generics
package hearth

/** Opt-in Hearth integration surface.
  *
  * The current derivation in [[eo.generics.LensMacro]] and
  * [[eo.generics.PrismMacro]] uses vanilla Scala 3 quoted reflection
  * because the initial shapes we care about (single-field Lens,
  * single-variant Prism) don't benefit meaningfully from Hearth's
  * higher-level abstractions.
  *
  * However, Hearth is on the `eo-generics` classpath specifically so
  * that the richer derivations tracked in the module's TODO list can
  * be implemented here without another round of sbt surgery:
  *
  *   - Recursive lenses on nested paths: `lens[A](_.b.c.d)` composed
  *     automatically through Hearth's field iteration.
  *   - Whole-ADT Prism tables: `prisms[S]` returning a typeclass
  *     instance whose members are `Prism[S, V]` for every variant `V`
  *     of a sealed / enum `S`.
  *   - Case-class-as-tuple Iso: `iso[S]` mapping between a product
  *     type and its structural tuple, built via Hearth's product
  *     deconstruction.
  *
  * The Scala 3 adapter pattern for Hearth is:
  * {{{
  * import hearth.MacroCommonsScala3
  * import scala.quoted.*
  *
  * trait MyDerivation { this: hearth.MacroCommons =>
  *   def derive[A: Type]: Expr[...] = {
  *     // Hearth gives: field iteration, sum deconstruction,
  *     // uniform error reporting, cross-compile-friendly helpers.
  *   }
  * }
  *
  * class MyMacro(q: Quotes) extends MacroCommonsScala3(using q), MyDerivation
  *
  * object MyMacro:
  *   def deriveExpr[A: Type](using q: Quotes): Expr[...] =
  *     new MyMacro(q).derive[A]
  *
  * inline def myDerive[A]: ... = ${ MyMacro.deriveExpr[A] }
  * }}}
  *
  * This file is intentionally kept empty of executable code: its
  * purpose is to document the landing zone for follow-up work, and
  * to justify the presence of the `hearth` dependency in
  * `build.sbt` for anyone auditing `eo-generics`.
  */
private[generics] object HearthScaffolding:
  /** Hearth version pinned in `build.sbt`. Tracked here so a diff
    * on the dep bump shows up in code review alongside any API
    * adaptation needed at the derivation layer.
    */
  final val version: String = "0.3.0"
