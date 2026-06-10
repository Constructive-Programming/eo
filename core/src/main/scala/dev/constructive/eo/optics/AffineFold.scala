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

/** Concrete Optic subclass for [[AffineFold]] — a `final class` storing `pick` directly, NOT a
  * shared anonymous wrapper: a single anon class would host one `pick.apply` bytecode site for
  * every AffineFold instance, the megamorphic-dispatch trap PrintInlining exposed on the
  * abstract-class [[DirectGetter]] (the JIT profiles that one site over all instances' lambdas and
  * gives up inlining). Same storage shape as [[PickMendPrism]] (`pick`) and [[ForgetFold]]
  * (`read`). Returned by [[AffineFold.apply]] / [[AffineFold.select]] so hand-written affine folds
  * pick up the fused members automatically.
  */
final class PickFold[S, A](val pick: S => Option[A]) extends Optic[S, Unit, A, Unit, Affine]:
  type X = (Unit, Unit)

  def to(s: S): Affine[X, A] =
    pick(s) match
      case Some(a) => new Affine.Hit[X, A]((), a)
      case None    => new Affine.Miss[X, A](())

  def from(a: Affine[X, Unit]): Unit = ()

  /** Fused `getOption` — reads `pick` directly, skipping the `Affine` carrier round-trip the
    * generic extension performs. Wins over the extension by member precedence at static type.
    */
  inline def getOption(s: S): Option[A] = pick(s)

  /** Fused `AffineFold.andThen(AffineFold)` — `flatMap`s the picks; no `Affine` wrappers, no
    * `Morph`. `inline` so each compose site splices its own lambda (per-site monomorphic dispatch —
    * see [[DirectGetter.andThen]]).
    */
  inline def andThen[C](inner: PickFold[A, C]): PickFold[S, C] =
    new PickFold(s => pick(s).flatMap(inner.pick))

  /** Fused `AffineFold.andThen(Getter)` — `map`s the read over the pick; the concrete fast path of
    * the generic `andThenAffineFold` collapse.
    */
  inline def andThen[C](inner: DirectGetter[A, C]): PickFold[S, C] =
    new PickFold(s => pick(s).map(inner.get))

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
  def apply[S, A](matches: S => Option[A]): PickFold[S, A] =
    new PickFold(matches)

  /** Filtering — hits only on inputs satisfying `p`. @group Constructors */
  def select[A](p: A => Boolean): PickFold[A, A] =
    new PickFold(a => Option(a).filter(p))

  // No `fromOptional` / `fromPrism` weakening factories: a writable optic's read-only view is
  // just `AffineFold(optic.getOption)` (`.getOption` is defined on both the Affine and Either
  // carriers), and eo provides no analogous `Getter.fromLens` / `Fold.fromTraversal`, so a
  // bespoke cross-optic conversion here would be an inconsistent one-off.
