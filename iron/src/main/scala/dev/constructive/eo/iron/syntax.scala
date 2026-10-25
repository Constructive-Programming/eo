package dev.constructive.eo
package iron

import io.github.iltotore.iron.{:|, Constraint, RefinedSubtype, RefinedType, RuntimeConstraint}

import optics.MendTearPrism

/** `.prism` on a runtime constraint: the [[IronPrism]] `A ⇀ A :| C` for the constraint `C` it
  * checks.
  *
  * {{{
  *   import dev.constructive.eo.iron.prism
  *
  *   val positiveInt: RuntimeConstraint[Int, Positive] = summon
  *   positiveInt.prism.getOption(-1)               // None
  * }}}
  *
  * @group Constructors
  */
extension [A, C](rtc: RuntimeConstraint[A, C])
  def prism: MendTearPrism[A, A, A :| C, A :| C] = IronPrism(using rtc)

/** `.prism` on a compile-time constraint, as Iron's own givens provide them: the [[IronPrism]]
  * `A ⇀ A :| C`. Inline, like Iron's `RuntimeConstraint.derived`, so the constraint's inline `test`
  * is expanded at the call site — the receiver must be a statically known instance
  * (`summon[Constraint[Int, Positive]].prism`).
  *
  * @group Constructors
  */
extension [A, C](inline c: Constraint[A, C])

  inline def prism: MendTearPrism[A, A, A :| C, A :| C] =
    IronPrism(using RuntimeConstraint.derived(using c))

/** `.prism` on a `RefinedType` companion: given `object Age extends RefinedType[Int, Positive]`,
  * `Age.prism` is the Prism `Int ⇀ Age` — the same as `IronPrism.refined[Age]`.
  *
  * @group Constructors
  */
extension [A, C](companion: RefinedType[A, C])

  def prism: MendTearPrism[A, A, companion.T, companion.T] =
    IronPrism.newtype[A, C, companion.T](companion.rtc)

/** `.prism` on a `RefinedSubtype` companion: the Prism `A ⇀ T`, as for `RefinedType`. Iron's two
  * newtype flavours share no public supertype, hence the twin.
  *
  * @group Constructors
  */
extension [A, C](companion: RefinedSubtype[A, C])

  def prism: MendTearPrism[A, A, companion.T, companion.T] =
    IronPrism.newtype[A, C, companion.T](companion.rtc)
