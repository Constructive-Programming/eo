package dev.constructive.eo
package data

/** Dual of [[Accessor]] — build a fresh `F[X, A]` from an `A`. Required by `Optic.reverseGet`.
  *
  * @tparam F
  *   the carrier
  */
trait ReverseAccessor[F[_, _]]:
  def reverseGet[X, A](a: A): F[X, A]

/** Typeclass instances for [[ReverseAccessor]]. */
object ReverseAccessor:

  /** `Either` — wraps `A` as `Right[X, A](a)`. @group Instances */
  given eitherRevAccessor: ReverseAccessor[Either] with
    def reverseGet[X, A](a: A): Either[X, A] = Right[X, A](a)
