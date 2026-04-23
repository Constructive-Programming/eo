package eo
package optics

import data.Affine

/** Read-only 0-or-1 focus — the observation side of an [[Optional]] with the write path pinned
  * shut.
  *
  * `AffineFold[S, A]` is a type alias for `Optic[S, Unit, A, A, Affine]`. The `T = Unit` slot
  * statically forbids `.modify` / `.replace` / `.put` (no `ForgetfulApplicative[Affine]` is offered
  * for `T = Unit`, and the only thing `from` can return is `()`). The observable surface is:
  *
  *   - [[Optic.getOption]] — the `Option[A]` read on the Affine carrier.
  *   - [[Optic.foldMap]] — via `ForgetfulFold[Affine]`; miss folds to `Monoid.empty`.
  *   - [[Optic.modifyA]] / [[Optic.modifyF]] — via `ForgetfulTraverse[Affine, Applicative]`;
  *     produces `G[Unit]` on any `Applicative[G]` / `Functor[G]`, useful for effectful reads.
  *
  * Compose freely with `Lens` (`lens.andThen(af)`) and `Prism` via the existing cross-carrier
  * `.andThen` extensions — the same `Morph` bridges used by full Optional also fire for AffineFold
  * since they key off the carrier, not the `T` slot.
  *
  * ### Relationship to [[Optional.readOnly]]
  *
  * `Optional.readOnly` is retained as an alias entry point for users coming from the "read-only
  * Optional" mental model; it delegates to [[AffineFold.apply]] and yields the same specialised
  * runtime shape described below.
  *
  * ### Specialised existential shape
  *
  * `AffineFold` picks `type X = (Unit, Unit)` rather than the `(Unit, S)` shape a full
  * `Optional[S, S, A, A]` would use. With `X = (Unit, Unit)`:
  *
  *   - The `Affine.Miss` branch stores `fst: Fst[X] = Unit` (same as Optional).
  *   - The `Affine.Hit` branch stores `snd: Snd[X] = Unit` + `b: A` — one reference slot less than
  *     Optional's `(S, A)` payload.
  *
  * Since `from` throws its input away (`T = Unit`), the `S` stored on the Hit branch would never be
  * observed. Dropping it saves one word per `Affine.Hit` allocation on every read — the common path
  * for any AffineFold consumer. Composition under `Affine.assoc` stays sound because that instance
  * is unbounded in `X` / `Y`.
  */
type AffineFold[S, A] = Optic[S, Unit, A, A, Affine]

/** Constructors for [[AffineFold]]. */
object AffineFold:

  /** Build an AffineFold from a partial projection `matches: S => Option[A]`. `None` yields a Miss;
    * `Some(a)` yields a Hit carrying the focus `a`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge: AffineFold[Person, Int] =
    *   AffineFold(p => Option.when(p.age >= 18)(p.age))
    * adultAge.getOption(Person(20))  // Some(20)
    * adultAge.getOption(Person(15))  // None
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

  /** Filtering AffineFold — hits only on inputs satisfying `p`, otherwise misses.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val even: AffineFold[Int, Int] = AffineFold.select(_ % 2 == 0)
    * even.getOption(4)  // Some(4)
    * even.getOption(3)  // None
    *   }}}
    */
  def select[A](p: A => Boolean): AffineFold[A, A] =
    apply(a => Option(a).filter(p))

  /** Drop the write path of an [[Optional]], retaining only its read side. `fromOptional(o)` holds
    * the same hit/miss decision function `o` does, wrapped in the specialised AffineFold shape.
    *
    * @group Constructors
    */
  def fromOptional[S, T, A, B](o: Optional[S, T, A, B]): AffineFold[S, A] =
    apply(s => o.getOrModify(s).toOption)

  /** Drop the build path of any `Either`-carrier optic (Prism, both `MendTearPrism` and
    * `PickMendPrism` flavours), retaining only the `Right` branch as an `Option[A]`.
    *
    * @group Constructors
    */
  def fromPrism[S, T, A, B](p: Optic[S, T, A, B, Either]): AffineFold[S, A] =
    apply(s => p.to(s).toOption)
