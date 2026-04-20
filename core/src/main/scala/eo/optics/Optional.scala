package eo
package optics

import data.Affine

/** Constructor for `Optional` — the conditionally-present single-focus optic, backed by the
  * `Affine` carrier.
  *
  * An `Optional[S, A]` (short for `Optic[S, S, A, A, Affine]`) encodes a field that may or may not
  * be there: a predicate-gated access (`street` only when `isValid`), the `Some` case of an
  * `Option` field, a refinement-style narrowing that can fail.
  *
  * Compose freely with `Lens` via `lens.andThen(opt)` — the cross-carrier `.andThen` extension
  * auto-morphs the Lens into the Affine carrier via `Composer[Tuple2, Affine]`. The `Affine`
  * carrier admits unbounded X/Y via [[data.Affine.assoc]], so any abstract existential satisfies
  * the composition requirement.
  */
object Optional:

  /** Construct an Optional from a partial getter `getOrModify` (returns `Right(a)` on hit,
    * `Left(t)` on miss) and a re-assembler `reverseGet: (S, B) => T`.
    *
    * The `F` type parameter is carried for symmetry with other carrier-aware constructors but does
    * not alter the return type; every Optional is `Optic[…, Affine]`.
    *
    * @group Constructors
    * @tparam S
    *   source type
    * @tparam T
    *   result type (often `= S`)
    * @tparam A
    *   focus read from the hit branch
    * @tparam B
    *   focus written back to produce `T` (often `= A`)
    * @tparam F
    *   carrier parameter — unused; present for constructor-shape symmetry
    *
    * @example
    *   {{{
    * // Focus Person.age only when the person is a legal adult:
    * case class Person(age: Int, name: String)
    * val adultAge = Optional[Person, Person, Int, Int, Affine](
    *   getOrModify = p => Either.cond(p.age >= 18, p.age, p),
    *   reverseGet  = { case (p, a) => p.copy(age = a) },
    * )
    *   }}}
    */
  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Either[T, A],
      reverseGet: ((S, B)) => T,
  ) =
    new Optic[S, T, A, B, Affine]:
      type X = (T, S)
      val to: S => Affine[X, A] = s =>
        Affine(getOrModify(s) match
          case Right(a) => Right(s -> a)
          case Left(t)  => Left(t))
      val from: Affine[X, B] => T = a =>
        a.affine match
          case Right(p) => reverseGet(p)
          case Left(t)  => t

  /** Read-only construction — build an `Optic[S, Unit, A, A, Affine]` from just a partial
    * projection `S => Option[A]`, with no write-back needed.
    *
    * Sets `T = Unit` so the type rules out `.modify` / `.replace` at the caller — the resulting
    * optic only supports `.getOption` and `.foldMap`. Useful when the source shape has no natural
    * "rebuild the focus into S" path (`headOption` on a List, predicate-gated filters like
    * `Option.when(p(s))(s)`, etc.), and useful as an API-boundary declaration of "callers cannot
    * write through this."
    *
    * The `ForgetfulFold[Affine]` and `ForgetfulTraverse[Affine, Applicative]` instances (already
    * shipped for Optional proper) handle the read-side operations unchanged.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge: Optic[Person, Unit, Int, Int, Affine] =
    *   Optional.readOnly(p => Option.when(p.age >= 18)(p.age))
    * adultAge.getOption(Person(20))    // Some(20)
    * adultAge.getOption(Person(15))    // None
    *   }}}
    */
  def readOnly[S, A](matches: S => Option[A]): Optic[S, Unit, A, A, Affine] =
    new Optic[S, Unit, A, A, Affine]:
      type X = (Unit, S)
      val to: S => Affine[X, A] = s =>
        Affine(matches(s) match
          case Some(a) => Right((s, a))
          case None    => Left(()))
      val from: Affine[X, A] => Unit = _ => ()

  /** Filtering read-only Optional — keeps only inputs matching `p`, exposing them via `.getOption`
    * / `.foldMap`. Mirror of [[Fold.select]] but over a single focus.
    *
    * @group Constructors
    */
  def selectReadOnly[A](p: A => Boolean): Optic[A, Unit, A, A, Affine] =
    readOnly(a => Option(a).filter(p))
