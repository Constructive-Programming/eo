package dev.constructive.eo
package optics

import data.Affine

/** Read-only 0-or-1-focus optic — `Optic[S, Unit, A, Unit, Affine]`. Both `T` and the write-focus
  * `B` are `Unit` (honestly one-way, like `Getter` / `Fold`): there is no value to put back, so
  * `.modify` / `.replace` don't apply. The surface is `.getOption` and `.foldMap`. Composes with
  * Lens / Prism via the same `Morph` bridges full Optional uses (they key off the carrier, not the
  * `T` slot).
  *
  * Specialised existential `X = (Unit, Unit)`: `Affine.Hit` stores `snd: Unit + b: A` — one
  * reference slot less than the `(S, A)` payload of a full Optional, since `from` throws its input
  * away anyway.
  */
type AffineFold[S, A] = Optic[S, Unit, A, Unit, Affine]

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
    new Optic[S, Unit, A, Unit, Affine]:
      type X = (Unit, Unit)
      val to: S => Affine[X, A] = s =>
        matches(s) match
          case Some(a) => new Affine.Hit[X, A]((), a)
          case None    => new Affine.Miss[X, A](())
      val from: Affine[X, Unit] => Unit = _ => ()

  /** Filtering — hits only on inputs satisfying `p`. @group Constructors */
  def select[A](p: A => Boolean): AffineFold[A, A] =
    apply(a => Option(a).filter(p))

  // No `fromOptional` / `fromPrism` weakening factories: a writable optic's read-only view is
  // just `AffineFold(optic.getOption)` (`.getOption` is defined on both the Affine and Either
  // carriers), and eo provides no analogous `Getter.fromLens` / `Fold.fromTraversal`, so a
  // bespoke cross-optic conversion here would be an inconsistent one-off.
