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
    pPrism((s: S) => getOption(s).toRight(s), reverseGet)

  def pOptional[S, A, B](getOption: S => Option[A], reverseGet: B => S) =
    pPrism((s: S) => getOption(s).toRight(s), reverseGet)

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
  def to: S => Either[T, A] = tear
  def from: Either[T, B] => T = _.fold(identity, mend)
  def modify[X](f: A => B): S => T =
    s => tear(s) match
      case Right(a) => mend(f(a))
      case Left(t)  => t
  def replace[X](b: B): S => T =
    s => tear(s) match
      case Right(_) => mend(b)
      case Left(t)  => t
