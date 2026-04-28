package dev.constructive.eo
package optics

import data.Affine

/** Read-only 0-or-1-focus optic — `Optic[S, Unit, A, A, Affine]`. `T = Unit` rules out `.modify` /
  * `.replace`; the surface is `.getOption`, `.foldMap`, and effectful `.modifyA` / `.modifyF`
  * (which produce `G[Unit]`). Composes with Lens / Prism via the same `Morph` bridges full Optional
  * uses (they key off the carrier, not the `T` slot).
  *
  * Specialised existential `X = (Unit, Unit)`: `Affine.Hit` stores `snd: Unit + b: A` — one
  * reference slot less than the `(S, A)` payload of a full Optional, since `from` throws its input
  * away anyway.
  */
type AffineFold[S, A] = Optic[S, Unit, A, A, Affine]

/** Constructors for [[AffineFold]]. */
object AffineFold:

  /** Build from `matches: S => Option[A]`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge = AffineFold[Person, Int](p => Option.when(p.age >= 18)(p.age))
    *   }}}
    */
  def apply[S, A](matches: S => Option[A]): AffineFold[S, A] =
    new Optic[S, Unit, A, A, Affine]:
      type X = (Unit, Unit)
      val to: S => Affine[X, A] = s =>
        matches(s) match
          case Some(a) => new Affine.Hit[X, A]((), a)
          case None    => new Affine.Miss[X, A](())
      val from: Affine[X, A] => Unit = _ => ()

  /** Filtering — hits only on inputs satisfying `p`. @group Constructors */
  def select[A](p: A => Boolean): AffineFold[A, A] =
    apply(a => Option(a).filter(p))

  /** Drop the write path of an [[Optional]]. @group Constructors */
  def fromOptional[S, T, A, B](o: Optional[S, T, A, B]): AffineFold[S, A] =
    apply(s => o.getOrModify(s).toOption)

  /** Drop the build path of any `Either`-carrier optic, retaining the `Right` branch as
    * `Option[A]`.
    *
    * @group Constructors
    */
  def fromPrism[S, T, A, B](p: Optic[S, T, A, B, Either]): AffineFold[S, A] =
    apply(s => p.to(s).toOption)
