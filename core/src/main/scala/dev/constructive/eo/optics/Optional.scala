package dev.constructive.eo
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
  ): Optional[S, T, A, B] =
    new Optional[S, T, A, B](getOrModify, (s, b) => reverseGet((s, b)))

  /** Read-only construction — build an `Optic[S, Unit, A, A, Affine]` from just a partial
    * projection `S => Option[A]`, with no write-back needed.
    *
    * Sets `T = Unit` so the type rules out `.modify` / `.replace` at the caller — the resulting
    * optic only supports `.getOption` and `.foldMap`. Useful when the source shape has no natural
    * "rebuild the focus into S" path (`headOption` on a List, predicate-gated filters like
    * `Option.when(p(s))(s)`, etc.), and useful as an API-boundary declaration of "callers cannot
    * write through this."
    *
    * Delegates to [[AffineFold.apply]]; see that constructor for the specialised `X = (Unit, Unit)`
    * shape it produces and the per-hit allocation savings over the full-Optional layout.
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
  def readOnly[S, A](matches: S => Option[A]): AffineFold[S, A] =
    AffineFold(matches)

  /** Filtering read-only Optional — keeps only inputs matching `p`, exposing them via `.getOption`
    * / `.foldMap`. Mirror of [[Fold.select]] but over a single focus. Delegates to
    * [[AffineFold.select]].
    *
    * @group Constructors
    */
  def selectReadOnly[A](p: A => Boolean): AffineFold[A, A] =
    AffineFold.select(p)

/** Concrete Optic subclass for `Optional.apply` — stores `getOrModify` and `reverseGet` directly,
  * enabling fused `.andThen` overloads that skip the generic `AssociativeFunctor[Affine]` composeTo
  * / composeFrom path whenever the other side is also a known concrete subclass (`GetReplaceLens`,
  * `MendTearPrism`, `BijectionIso`, `Optional`).
  *
  * Shares the Affine-carrier contract with the anonymous form Optional.apply used to return, so all
  * the generic `.modify` / `.foldMap` / `.modifyA` extensions continue to work unchanged. The extra
  * surface over the anonymous form is just the fused overloads.
  *
  * The cross-carrier path (e.g. `lens.andThen(optional)` where outer is a `GetReplaceLens`) is
  * handled by overloads on the OTHER subclasses targeting `Optional` as the result type — same
  * pattern as `GetReplaceLens.andThen(BijectionIso)`.
  */
final class Optional[S, T, A, B](
    val getOrModify: S => Either[T, A],
    val reverseGet: (S, B) => T,
) extends Optic[S, T, A, B, Affine]:
  type X = (T, S)

  val to: S => Affine[X, A] = s =>
    getOrModify(s) match
      case Right(a) => new Affine.Hit[X, A](s, a)
      case Left(t)  => new Affine.Miss[X, A](t)

  val from: Affine[X, B] => T = a =>
    a match
      case h: Affine.Hit[X, B]  => reverseGet(h.snd, h.b)
      case m: Affine.Miss[X, B] => m.fst

  /** Single fused-andThen kernel for the Optional outer. Every concrete-typed inner shape
    * (`Optional`, `GetReplaceLens`, `MendTearPrism`, `BijectionIso`) lowers into the same skeleton
    * — outer-miss short-circuits, outer-hit threads the focus through `innerHit` (read side) and
    * `innerWrite` (write side). The previous 4-way duplication of `getOrModify` / `reverseGet`
    * blocks is collapsed here; each `andThen` overload below is a 1-2 line shell that synthesises
    * the inner-shape adapters.
    *
    * `inline` so the call-site materialises the same Optional construction it would have done
    * before the refactor — the helper exists only to share source, not to add a function-call
    * indirection on the hot path.
    *
    *   - `innerHit(s, a)` — given the original `s` and the outer-hit focus `a`, return either a
    *     final `T` (when the inner misses, lifted via outer.reverseGet) or the combined focus `C`.
    *   - `innerWrite(a, d)` — given the outer-hit focus `a` and a write-back `d`, produce the
    *     `B`-shaped value the outer's reverseGet expects on the write path.
    */
  private inline def fuseToOptional[C, D](
      innerHit: (S, A) => Either[T, C],
      innerWrite: (A, D) => B,
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        getOrModify(s) match
          case Left(t)  => Left(t)
          case Right(a) => innerHit(s, a),
      reverseGet = (s, d) =>
        getOrModify(s) match
          case Left(t)  => t
          case Right(a) => reverseGet(s, innerWrite(a, d)),
    )

  /** Fused `Optional.andThen(Optional)` — two partial focuses compose into another partial focus.
    * Outer miss passes through; outer hit + inner miss lifts the inner miss via the outer's
    * reverseGet (which takes the original S + inner's B-shaped leftover); inner hit yields the
    * combined hit focus C.
    */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) =>
        inner.getOrModify(a) match
          case Left(b)  => Left(reverseGet(s, b))
          case Right(c) => Right(c),
      innerWrite = (a, d) => inner.reverseGet(a, d),
    )

  /** Fused `Optional.andThen(GetReplaceLens)` — always-present inner field inside a partial outer
    * focus. Stays partial (`Optional`): outer miss is still a miss.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Right(inner.get(a)),
      innerWrite = (a, d) => inner.enplace(a, d),
    )

  /** Fused `Optional.andThen(MendTearPrism)` — outer Optional's hit branch fed into a Prism. Miss
    * on outer passes through; hit + inner miss lifts via outer.reverseGet after inner.mend wraps
    * the inner's B-shaped leftover back to the outer's A shape.
    */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) =>
        inner.tear(a) match
          case Left(b)  => Left(reverseGet(s, b))
          case Right(c) => Right(c),
      innerWrite = (_, d) => inner.mend(d),
    )

  /** Fused `Optional.andThen(BijectionIso)` — iso is transparent; threads the get / reverseGet
    * through the Optional's existing shape.
    *
    * Historical note: prior to the de-duplication refactor this overload skipped the
    * outer-miss short-circuit on the write path (`reverseGet = (s, d) => reverseGet(s,
    * inner.reverseGet(d))`), which observably differed from the other three Optional fused
    * overloads. That was a latent bug — the unfused `from` returns the Miss leftover `t`
    * directly, never invoking `reverseGet`. Routing through `fuseToOptional` aligns this case
    * with the others.
    */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Right(inner.get(a)),
      innerWrite = (_, d) => inner.reverseGet(d),
    )
