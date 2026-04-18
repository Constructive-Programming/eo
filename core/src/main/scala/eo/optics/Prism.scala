package eo
package optics

/** Constructors for `Prism` — the partial single-focus optic, backed
  * by the `Either` carrier.
  *
  * A `Prism[S, A]` (short for `Optic[S, S, A, A, Either]`) encodes a
  * branch of a sum type: `getOption(s): Option[A]` succeeds when `s`
  * matches the branch, `reverseGet(a): S` lifts an `a` back into
  * `S`. Composition with other prisms (via `Optic.andThen` on the
  * `Either` carrier) produces deeper sum-type drill-downs.
  *
  * For derived prisms on enums / sealed traits / union types the
  * `eo-generics` module exposes the `prism[S, A]` macro.
  */
object Prism:

  /** Monomorphic constructor — `S = T`, `A = B`. `getOrModify` returns
    * `Right(a)` on match and `Left(s)` on miss.
    *
    * @group Constructors
    *
    * @example
    * {{{
    * enum Shape:
    *   case Circle(r: Double)
    *   case Square(s: Double)
    *
    * val circleP = Prism[Shape, Shape.Circle](
    *   {
    *     case c: Shape.Circle => Right(c)
    *     case other           => Left(other)
    *   },
    *   identity,
    * )
    * }}} */
  def apply[S, A](
        getOrModify: S => Either[S, A],
        reverseGet: A => S
    ) =
    pPrism(getOrModify, reverseGet)

  /** Polymorphic constructor — allows the miss branch to produce a
    * different type `T`. Most production code uses [[apply]]; this
    * form matters for refinement-style conversions.
    *
    * @group Constructors */
  def pPrism[S, T, A, B](
        getOrModify: S => Either[T, A],
        reverseGet: B => T
    ) =
    MendTearPrism(getOrModify, reverseGet)

  /** `Option`-shaped constructor — returns a [[PickMendPrism]] whose
    * fused extensions avoid the intermediate `Either` that the
    * generic constructor must build. Preferred when the underlying
    * projection is already `S => Option[A]`.
    *
    * @group Constructors */
  def optional[S, A](getOption: S => Option[A], reverseGet: A => S) =
    PickMendPrism[S, A, A](getOption, reverseGet)

  /** Polymorphic counterpart to [[optional]] — allows type change
    * on write.
    *
    * @group Constructors */
  def pOptional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    PickMendPrism[S, A, B](getOption, reverseGet)

/** Concrete Optic subclass that stores `getOrModify` and `reverseGet`
  * directly, enabling fused extensions on [[Optic]] that bypass the
  * Either carrier: pattern-match once, no intermediate allocation.
  *
  * All `Prism.*` constructors return this type so both hand-written
  * and macro-derived prisms benefit from the fused hot path.
  */
final class MendTearPrism[S, T, A, B](
    val tear: S => Either[T, A],
    val mend: B => T,
) extends Optic[S, T, A, B, Either]:
  type X = T
  inline def to: S => Either[T, A] = tear
  inline def from: Either[T, B] => T = _.fold(identity, mend)
  inline def modify[X](f: A => B): S => T =
    s => tear(s) match
      case Right(a) => mend(f(a))
      case Left(t)  => t
  inline def replace[X](b: B): S => T =
    s => tear(s) match
      case Right(_) => mend(b)
      case Left(t)  => t
  inline def getOption(s: S): Option[A] = tear(s).toOption
  inline def reverseGet(b: B): T = mend(b)

/** Concrete Optic subclass for the `Option`-shaped Prism constructor
  * (`Prism.optional` / `Prism.pOptional`).
  *
  * Stores `pick: S => Option[A]` and `mend: B => S` directly. The fused
  * extensions pattern-match on `Option` so the hot path never allocates
  * the intermediate `Either[S, A]` that the generic `MendTearPrism` must
  * build. In particular `getOption` returns the result of `pick` verbatim
  * -- zero allocation when the caller hands in a `Some` and the pick is
  * an identity-shaped function.
  */
final class PickMendPrism[S, A, B](
    val pick: S => Option[A],
    val mend: B => S,
) extends Optic[S, S, A, B, Either]:
  type X = S
  inline def to: S => Either[S, A] = s => pick(s).toRight(s)
  inline def from: Either[S, B] => S = _.fold(identity, mend)
  inline def modify[X](f: A => B): S => S =
    s => pick(s) match
      case Some(a) => mend(f(a))
      case None    => s
  inline def replace[X](b: B): S => S =
    s => pick(s) match
      case Some(_) => mend(b)
      case None    => s
  inline def getOption(s: S): Option[A] = pick(s)
  inline def reverseGet(b: B): S = mend(b)
