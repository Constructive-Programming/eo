package dev.constructive.eo
package iron

import cats.Foldable
import io.github.iltotore.iron.{:|, RefinedType, RuntimeConstraint}

import optics.{MendTearPrism, Prism}

/** Prisms for [[https://github.com/Iltotore/iron Iron]] refinements.
  *
  * A refinement checked at runtime is a partial conversion `A ⇀ A :| C` with a total way back, and
  * that is exactly a Prism: `getOption` tests the constraint (hit = the refined value, miss = the
  * untouched source), `reverseGet` forgets it. Both Prism laws hold for any pure constraint — a
  * value that passed the test passes it again, and forgetting a refinement gives back the very
  * value that was refined.
  *
  * Each constructor needs only Iron's `RuntimeConstraint`, which Iron derives from any compile-time
  * `Constraint` at a call site where the types are concrete, so every built-in and user-defined
  * constraint qualifies. Refinements Iron proves at compile time (`refine` on a literal) need no
  * optic: they cannot miss.
  *
  * {{{
  *   import io.github.iltotore.iron.constraint.numeric.Positive
  *
  *   val positive = IronPrism[Int, Positive]
  *   positive.getOption(42)                        // Some(42: Int :| Positive)
  *   positive.getOption(-1)                        // None
  *
  *   // Lens ∘ Prism = Optional: only valid ages get written.
  *   val age = Lens[Person, Int](_.age, (p, a) => p.copy(age = a)).andThen(positive)
  *
  *   // Traversal ∘ Prism = filtering Traversal: only the positive elements are visited.
  *   Traversal.each[List, Int].andThen(positive).foldMap(List(_))(List(3, -1, 4))  // List(3, 4)
  * }}}
  *
  * The constructors return the stock [[optics.MendTearPrism]], so every fused composition and
  * capability mixin of the core Prism applies unchanged. Like every Prism, a miss is a silent
  * pass-through on write; when the reason matters, Iron's own `refineEither` carries the
  * constraint's message.
  */
object IronPrism:

  /** The Prism `A ⇀ A :| C` for a runtime-checked constraint `C`.
    *
    * @group Constructors
    */
  def apply[A, C](using rtc: RuntimeConstraint[A, C]): MendTearPrism[A, A, A :| C, A :| C] =
    Prism[A, A :| C](
      a => if rtc.test(a) then Right(a.asInstanceOf[A :| C]) else Left(a),
      identity,
    )

  /** The Prism `BaseType ⇀ T` for an Iron `RefinedType` / `RefinedSubtype` newtype `T` — given
    * `object Age extends RefinedType[Int, Positive]; type Age = Age.T`, `IronPrism.refined[Age]`
    * focuses the `Int`s that are valid ages.
    *
    * @group Constructors
    */
  def refined[T](using m: RefinedType.Mirror[T]): MendTearPrism[m.BaseType, m.BaseType, T, T] =
    val rtc = m.ops.rtc
    Prism[m.BaseType, T](
      a => if rtc.test(a) then Right(a.asInstanceOf[T]) else Left(a),
      _.asInstanceOf[m.BaseType],
    )

  /** All-or-nothing Prism `F[A] ⇀ F[A :| C]`: hits only when EVERY element satisfies `C`, and then
    * refines the container as it stands — no copy, no per-element allocation (`A :| C` is an opaque
    * alias of `A`). For the elements that do satisfy `C` out of a mixed container, compose instead:
    * `Traversal.each[F, A].andThen(IronPrism[A, C])`.
    *
    * @group Constructors
    */
  def all[F[_], A, C](using
      F: Foldable[F],
      rtc: RuntimeConstraint[A, C],
  ): MendTearPrism[F[A], F[A], F[A :| C], F[A :| C]] =
    Prism[F[A], F[A :| C]](
      fa => if F.forall(fa)(a => rtc.test(a)) then Right(fa.asInstanceOf[F[A :| C]]) else Left(fa),
      _.asInstanceOf[F[A]],
    )
