package dev.constructive.eo
package data

/** Read the focus `A` out of a two-parameter carrier `F[X, A]`. Required by `Optic.get`;
  * always-hitting carriers (`Tuple2` for Lens, `Forgetful` for Iso / Getter) supply one.
  *
  * @tparam F
  *   the carrier
  */
trait Accessor[F[_, _]]:
  def get[A]: [X] => F[X, A] => A

/** Typeclass instances for [[Accessor]]. */
object Accessor:

  /** `Tuple2` — picks the right element. @group Instances */
  given tupleAccessor: Accessor[Tuple2] with
    def get[A]: [X] => ((X, A)) => A = [X] => (_: (X, A))._2

/** Dual of [[Accessor]] — build a fresh `F[X, A]` from an `A`. Required by `Optic.reverseGet`.
  *
  * @tparam F
  *   the carrier
  */
trait ReverseAccessor[F[_, _]]:
  def reverseGet[A]: [X] => A => F[X, A]

/** Typeclass instances for [[ReverseAccessor]]. */
object ReverseAccessor:

  /** `Either` — wraps `A` as `Right[X, A](a)`. @group Instances */
  given eitherRevAccessor: ReverseAccessor[Either] with
    def reverseGet[A]: [X] => A => Either[X, A] = [X] => Right[X, A](_: A)
