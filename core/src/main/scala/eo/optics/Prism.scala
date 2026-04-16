package eo
package optics

object Prism:
  def apply[S, A](
        getOrModify: S => Either[S, A],
        reverseGet: A => S
    ) =
    pPrism(getOrModify, reverseGet)

  def pPrism[S, T, A, B](
        getOrModify: S => Either[T, A],
        reverseGet: B => T
    ) =
    MendTearOptic(getOrModify, reverseGet)

  def optional[S, A](getOption: S => Option[A], reverseGet: A => S) =
    PickMendOptic[S, A, A](getOption, reverseGet)

  def pOptional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    PickMendOptic[S, A, B](getOption, reverseGet)

/** Concrete Optic subclass that stores `getOrModify` and `reverseGet`
  * directly, enabling fused extensions on [[Optic]] that bypass the
  * Either carrier: pattern-match once, no intermediate allocation.
  *
  * All `Prism.*` constructors return this type so both hand-written
  * and macro-derived prisms benefit from the fused hot path.
  */
final class MendTearOptic[S, T, A, B](
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
  * the intermediate `Either[S, A]` that the generic `MendTearOptic` must
  * build. In particular `getOption` returns the result of `pick` verbatim
  * -- zero allocation when the caller hands in a `Some` and the pick is
  * an identity-shaped function.
  */
final class PickMendOptic[S, A, B](
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
