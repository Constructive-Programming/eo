package dev.constructive.eo
package optics

import data.Affine

/** Constructor for `Optional` — the conditionally-present single-focus optic, backed by `Affine`.
  * An `Optional[S, A]` (short for `Optic[S, S, A, A, Affine]`) encodes a field that may or may not
  * be there. Composes freely with `Lens` via cross-carrier `.andThen` (auto-morphs through
  * `Composer[Tuple2, Affine]`).
  */
object Optional:

  /** Construct an Optional from `getOrModify` (`Right(a)` on hit, `Left(t)` on miss) and
    * `reverseGet: (S, B) => T`. The `F` parameter is unused — kept for constructor-shape symmetry.
    *
    * @group Constructors
    *
    * @example
    *   {{{
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

  /** Read-only Optional — `T = Unit` rules out `.modify` / `.replace`. Delegates to
    * [[AffineFold.apply]]; see that constructor for the specialised `X = (Unit, Unit)` shape and
    * its per-hit allocation savings.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge: Optic[Person, Unit, Int, Int, Affine] =
    *   Optional.readOnly(p => Option.when(p.age >= 18)(p.age))
    *   }}}
    */
  def readOnly[S, A](matches: S => Option[A]): AffineFold[S, A] =
    AffineFold(matches)

  /** Filtering read-only Optional — mirror of [[Fold.select]] over a single focus.
    *
    * @group Constructors
    */
  def selectReadOnly[A](p: A => Boolean): AffineFold[A, A] =
    AffineFold.select(p)

/** Concrete Optic subclass for `Optional.apply` — stores `getOrModify` / `reverseGet` directly so
  * the fused `.andThen` overloads can skip the generic `AssociativeFunctor[Affine]` round-trip
  * whenever the other side is also a known subclass.
  */
final class Optional[S, T, A, B](
    val getOrModify: S => Either[T, A],
    val reverseGet: (S, B) => T,
) extends Optic[S, T, A, B, Affine],
      CanGetOption[S, A],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = (T, S)

  def to(s: S): Affine[X, A] =
    getOrModify(s) match
      case Right(a) => new Affine.Hit[X, A](s, a)
      case Left(t)  => new Affine.Miss[X, A](t)

  def from(a: Affine[X, B]): T =
    a match
      case h: Affine.Hit[X, B]  => reverseGet(h.snd, h.b)
      case m: Affine.Miss[X, B] => m.fst

  /** Fused reads / writes — pattern-match `getOrModify` once, skipping the `Affine` carrier
    * round-trip the generic extensions perform.
    */
  def getOption(s: S): Option[A] = getOrModify(s).toOption

  def modify(f: A => B): S => T =
    s =>
      getOrModify(s) match
        case Right(a) => reverseGet(s, f(a))
        case Left(t)  => t

  def foldMap[M](f: A => M)(s: S)(using M: cats.Monoid[M]): M =
    getOrModify(s) match
      case Right(a) => f(a)
      case Left(_)  => M.empty

  /** Shared fused-andThen kernel for `Optional` outer. Outer-miss short-circuits; outer-hit threads
    * through `innerHit` (read) and `innerWrite` (write). `inline` so call sites allocate the same
    * single Optional they would pre-refactor.
    *
    *   - `innerHit(s, a)` — given the original `s` and outer-hit focus `a`, returns either a final
    *     `T` (inner-miss, lifted via outer.reverseGet) or a combined focus `C`.
    *   - `innerWrite(a, d)` — given outer-hit focus `a` and write-back `d`, produces the `B` the
    *     outer's reverseGet expects.
    */
  private inline def fuseToOptional[C, D](
      innerHit: (S, A) => Either[T, C],
      innerWrite: (A, D) => B,
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        getOrModify(s) match
          case l @ Left(_) => l.widenRight[C]
          case Right(a)    => innerHit(s, a),
      reverseGet = (s, d) =>
        getOrModify(s) match
          case Left(t)  => t
          case Right(a) => reverseGet(s, innerWrite(a, d)),
    )

  /** Fused `Optional.andThen(Optional)` — two partial focuses compose into another. `inline` so a
    * deep `optional.andThen(optional)…` chain splices distinct lambdas per level, under C2's
    * recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) =>
        inner.getOrModify(a) match
          case Left(b)      => Left(reverseGet(s, b))
          case r @ Right(_) => r.widenLeft[T],
      innerWrite = (a, d) => inner.reverseGet(a, d),
    )

  /** Fused `Optional.andThen(Lens)` — always-present inner inside a partial outer. Stays partial.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Right(inner.get(a)),
      innerWrite = (a, d) => inner.enplace(a, d),
    )

  /** Fused `Optional.andThen(Prism)` — outer Optional's hit fed into a Prism. */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (s, a) =>
        inner.tear(a) match
          case Left(b)      => Left(reverseGet(s, b))
          case r @ Right(_) => r.widenLeft[T],
      innerWrite = (_, d) => inner.mend(d),
    )

  /** Fused `Optional.andThen(Iso)` — iso is transparent; threads through the Optional's shape. */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = (_, a) => Right(inner.get(a)),
      innerWrite = (_, d) => inner.reverseGet(d),
    )
