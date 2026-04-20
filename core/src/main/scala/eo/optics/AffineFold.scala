package eo
package optics

import data.Affine

/** Constructors for `AffineFold` — the read-only zero-or-one-focus optic, backed by the `Affine`
  * carrier with `T = Unit` to signal "no write path".
  *
  * An `AffineFold[S, A]` (short for `Optic[S, Unit, A, A, Affine]`) is the read-only counterpart to
  * [[Optional]]: it exposes the focus when present via `.getOption(s)` and `.foldMap(f)(s)`, but
  * has no `.modify` / `.replace` because `T = Unit` provides no way back to `S`.
  *
  * Composition note: like [[Getter]] and [[Fold]], an `AffineFold` doesn't compose with other
  * AffineFolds via `Optic.andThen` (inner's `T = Unit` mismatches the outer's `B` slot). Compose
  * the readable chain through Lens / Prism / Optional and call `.getOption` / `.foldMap` on the
  * composed result instead.
  */
object AffineFold:

  /** Construct an AffineFold from a `matches: S => Option[A]` partial-read function.
    *
    * `.getOption(s)` returns the result of `matches` directly; `.foldMap(f)(s)` yields `f(a)` when
    * `matches(s)` is `Some(a)` and `Monoid[M].empty` when it's `None`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge = AffineFold[Person, Int](p => Option.when(p.age >= 18)(p.age))
    * adultAge.getOption(Person(20))             // Some(20)
    * adultAge.getOption(Person(15))             // None
    * adultAge.foldMap(identity[Int])(Person(20)) // 20
    *   }}}
    */
  def apply[S, A](matches: S => Option[A]): MatchAffineFold[S, A] =
    new MatchAffineFold[S, A](matches)

  /** Filtering AffineFold — keeps only inputs matching `p`, returning them via `.getOption` and
    * `.foldMap`. Symmetric to [[Fold.select]] but over a single focus.
    *
    * @group Constructors
    */
  def select[A](p: A => Boolean): MatchAffineFold[A, A] =
    new MatchAffineFold[A, A](a => Option(a).filter(p))

/** Concrete `Optic` subclass for an `AffineFold`. Stores the `matches: S => Option[A]` directly so
  * the hot path fuses `.getOption` without going through the generic carrier round-trip.
  *
  * Extends `Optic[S, Unit, A, A, Affine]` — satisfies the generic Optic contract so `.foldMap` and
  * `.modifyA` reach through the `ForgetfulFold[Affine]` / `ForgetfulTraverse[Affine, Applicative]`
  * instances — while also exposing a direct `.getOption` that skips the Affine materialisation
  * entirely.
  */
final class MatchAffineFold[S, A] private[optics] (val matches: S => Option[A])
    extends Optic[S, Unit, A, A, Affine]:
  type X = (Unit, S)

  val to: S => Affine[X, A] = s =>
    Affine(matches(s) match
      case Some(a) => Right((s, a))
      case None    => Left(()))

  val from: Affine[X, A] => Unit = _ => ()

  /** Direct `S => Option[A]` — the fused read path. Bypasses carrier materialisation. */
  inline def getOption(s: S): Option[A] = matches(s)
