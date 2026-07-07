package dev.constructive.eo
package optics

/** Zero-alloc phantom recasts, the `Either` analogue of `Affine.Miss.widenB`: a `Left` never stores
  * its right type parameter and a `Right` never stores its left, so re-typing that side is a cast,
  * not a rebuild. The composition kernels below pass miss / hit wrappers through unchanged instead
  * of re-allocating `Left(t)` / `Right(c)` at every hop. (The stdlib's `withRight` / `withLeft` are
  * upcasts only — they can't re-type the phantom side to an unrelated type.)
  */
extension [L, R](l: Left[L, R])
  private[optics] inline def widenRight[R2]: Either[L, R2] = l.asInstanceOf[Either[L, R2]]

extension [L, R](r: Right[L, R])
  private[optics] inline def widenLeft[L2]: Either[L2, R] = r.asInstanceOf[Either[L2, R]]

/** Constructors for `Prism` — the partial single-focus optic, backed by `Either`. A `Prism[S, A]`
  * (short for `Optic[S, S, A, A, Either]`) encodes a branch of a sum type: `getOption(s)` succeeds
  * when `s` matches, `reverseGet(a)` lifts back. The `eo-generics` module's `prism[S, A]` macro
  * derives prisms on enums / sealed traits / union types.
  */
object Prism:

  /** Monomorphic constructor (`S = T`, `A = B`). `getOrModify` returns `Right(a)` on match and
    * `Left(s)` on miss.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * enum Shape:
    *   case Circle(r: Double)
    *   case Square(s: Double)
    *
    * val circleP = Prism[Shape, Shape.Circle](
    *   { case c: Shape.Circle => Right(c); case other => Left(other) },
    *   identity,
    * )
    *   }}}
    */
  def apply[S, A](
      getOrModify: S => Either[S, A],
      reverseGet: A => S,
  ) =
    pPrism(getOrModify, reverseGet)

  /** Polymorphic constructor — allows the miss branch to produce a different `T`. For
    * refinement-style conversions; most code wants [[apply]].
    *
    * @group Constructors
    */
  def pPrism[S, T, A, B](
      getOrModify: S => Either[T, A],
      reverseGet: B => T,
  ) =
    MendTearPrism(getOrModify, reverseGet)

  /** `Option`-shaped constructor — returns a [[PickMendPrism]] whose fused extensions avoid the
    * intermediate `Either` the generic constructor must build. Prefer when the projection is
    * already `S => Option[A]`.
    *
    * @group Constructors
    */
  def optional[S, A](getOption: S => Option[A], reverseGet: A => S) =
    PickMendPrism[S, A, A](getOption, reverseGet)

  /** Polymorphic counterpart to [[optional]] — type change on write.
    *
    * @group Constructors
    */
  def pOptional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    PickMendPrism[S, A, B](getOption, reverseGet)

/** Concrete Optic subclass storing `tear` (`getOrModify`) and `mend` (`reverseGet`) directly. The
  * fused-`andThen` overloads pattern-match once and skip the generic `AssociativeFunctor[Either]`
  * round-trip. All `Prism.*` constructors return this type.
  */
final class MendTearPrism[S, T, A, B](
    val tear: S => Either[T, A],
    val mend: B => T,
) extends Optic[S, T, A, B, Either]:
  type X = T
  def to(s: S): Either[T, A] = tear(s)

  def from(e: Either[T, B]): T =
    e match
      case Right(b) => mend(b)
      case Left(t)  => t

  inline def modify(f: A => B): S => T =
    s =>
      tear(s) match
        case Right(a) => mend(f(a))
        case Left(t)  => t

  inline def replace(b: B): S => T =
    s =>
      tear(s) match
        case Right(_) => mend(b)
        case Left(t)  => t

  inline def getOption(s: S): Option[A] = tear(s).toOption
  inline def reverseGet(b: B): T = mend(b)

  /** Shared kernel for `MendTearPrism` ∘ Either-carrier composition. Outer-miss short-circuits;
    * outer-hit threads through `innerTear` (read) and `innerMend` (write). `inline` so the call
    * site allocates the same single `MendTearPrism` it would pre-refactor.
    */
  private inline def fuseToMendTear[C, D](
      innerTear: A => Either[T, C],
      innerMend: D => B,
  ): MendTearPrism[S, T, C, D] =
    new MendTearPrism(
      tear = s =>
        tear(s) match
          case l @ Left(_) => l.widenRight[C]
          case Right(a)    => innerTear(a),
      mend = d => mend(innerMend(d)),
    )

  /** Shared kernel for `MendTearPrism` ∘ {Lens, Optional} — produces `Optional`. Outer-miss
    * surfaces as `Left(t)`; on hit, `innerHit` decides between focus-arrived (`Right(c)`) or
    * inner-miss-needs-outer-lift (encoded by the caller).
    */
  private inline def fuseToOptional[C, D](
      innerHit: A => Either[T, C],
      innerWrite: (A, D) => B,
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        tear(s) match
          case l @ Left(_) => l.widenRight[C]
          case Right(a)    => innerHit(a),
      reverseGet = (s, d) =>
        tear(s) match
          case Left(t)  => t
          case Right(a) => mend(innerWrite(a, d)),
    )

  /** Fused `Prism.andThen(Prism)` — composed tear + `mend = outer.mend ∘ inner.mend`, no nested
    * Either wrappers. Picked when both sides are statically `MendTearPrism`. `inline` so a deep
    * prism chain splices distinct lambdas per level, under C2's recursive-inline cap (see
    * [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: MendTearPrism[A, B, C, D]): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a =>
        inner.tear(a) match
          case Left(b)      => Left(mend(b))
          case r @ Right(_) => r.widenLeft[T],
      innerMend = d => inner.mend(d),
    )

  /** Fused `Prism.andThen(Iso)` — collapses the iso into the prism. Skips
    * `Composer[Direct, Either]`.
    */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a => Right(inner.get(a)),
      innerMend = d => inner.reverseGet(d),
    )

  /** Fused `MendTearPrism.andThen(PickMendPrism)` — outer mono in focus; produces the more general
    * `MendTearPrism` shape (PickMendPrism's `T = S` invariant doesn't survive composition).
    */
  def andThen[C, D](inner: PickMendPrism[A, C, D])(using
      ev: A =:= B
  ): MendTearPrism[S, T, C, D] =
    fuseToMendTear(
      innerTear = a =>
        inner.pick(a) match
          case Some(c) => Right(c)
          case None    => Left(mend(ev(a))),
      innerMend = d => ev(inner.mend(d)),
    )

  /** Fused `Prism.andThen(Lens)` — prism may miss, lens focuses on hit. Result is `Optional`. */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = a => Right(inner.get(a)),
      innerWrite = (a, d) => inner.enplace(a, d),
    )

  /** Fused `Prism.andThen(Optional)` — nested partial focuses. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    fuseToOptional(
      innerHit = a =>
        inner.getOrModify(a) match
          case Left(b)      => Left(mend(b))
          case r @ Right(_) => r.widenLeft[T],
      innerWrite = (a, d) => inner.reverseGet(a, d),
    )

/** Concrete Optic subclass for the `Option`-shaped Prism (`Prism.optional` / `Prism.pOptional`).
  * Stores `pick` and `mend` directly; the fused extensions pattern-match on `Option` so the hot
  * path never builds the intermediate `Either[S, A]` the generic `MendTearPrism` would.
  */
final class PickMendPrism[S, A, B](
    val pick: S => Option[A],
    val mend: B => S,
) extends Optic[S, S, A, B, Either]:
  type X = S

  def to(s: S): Either[S, A] =
    pick(s) match
      case Some(a) => Right(a)
      case None    => Left(s)

  def from(e: Either[S, B]): S =
    e match
      case Right(b) => mend(b)
      case Left(s)  => s

  inline def modify(f: A => B): S => S =
    s =>
      pick(s) match
        case Some(a) => mend(f(a))
        case None    => s

  inline def replace(b: B): S => S =
    s =>
      pick(s) match
        case Some(_) => mend(b)
        case None    => s

  inline def getOption(s: S): Option[A] = pick(s)
  inline def reverseGet(b: B): S = mend(b)

  /** Fused `PickMend.andThen(PickMend)` — outer mono in focus; preserves the Option-fast-path shape
    * rather than materialising an intermediate Either. `inline` so a deep fast-path prism chain
    * splices distinct lambdas per level, under C2's recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](
      inner: PickMendPrism[A, C, D]
  )(using ev: A =:= B): PickMendPrism[S, C, D] =
    new PickMendPrism(
      pick = s => pick(s).flatMap(inner.pick),
      mend = d => mend(ev(inner.mend(d))),
    )

  /** Fused `PickMend.andThen(MendTear)` — outer mono in focus. Produces a `MendTearPrism`. */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D])(using
      ev: A =:= B
  ): MendTearPrism[S, S, C, D] =
    new MendTearPrism(
      tear = s =>
        pick(s) match
          case None    => Left(s)
          case Some(a) =>
            inner.tear(a) match
              case Left(b)      => Left(mend(b))
              case r @ Right(_) => r.widenLeft[S],
      mend = d => mend(inner.mend(d)),
    )

  /** Fused `PickMend.andThen(Iso)` — iso collapses into the PickMend's focus side. */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): PickMendPrism[S, C, D] =
    new PickMendPrism(
      pick = s => pick(s).map(inner.get),
      mend = d => mend(inner.reverseGet(d)),
    )
