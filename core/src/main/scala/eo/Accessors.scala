package eo
package data

/** Ability to read the focus `A` out of a two-parameter carrier `F[X, A]`. Required by the
  * `Optic.get` extension — every always-hitting carrier (`Tuple2` for Lens, `Forgetful` for Iso /
  * Getter) supplies one.
  *
  * @tparam F
  *   the carrier
  */
trait Accessor[F[_, _]]:
  /** Extract the focus from the carrier, erasing the existential `X` (which the caller cannot
    * observe through `Accessor`).
    */
  def get[A]: [X] => F[X, A] => A

/** Typeclass instances for [[Accessor]]. */
object Accessor:

  /** `Accessor[Tuple2]` — picks the right element of the pair.
    *
    * @group Instances
    */
  given tupleAccessor: Accessor[Tuple2] with
    def get[A]: [X] => ((X, A)) => A = [X] => (_: (X, A))._2

/** Dual of [[Accessor]] — build a fresh `F[X, A]` from an `A` alone. Required by
  * `Optic.reverseGet`; supplied by carriers whose "miss" branch can always be constructed from the
  * focus.
  *
  * @tparam F
  *   the carrier
  */
trait ReverseAccessor[F[_, _]]:
  /** Lift an `A` into the carrier, choosing the right branch by construction.
    */
  def reverseGet[A]: [X] => A => F[X, A]

/** Typeclass instances for [[ReverseAccessor]]. */
object ReverseAccessor:

  /** `ReverseAccessor[Either]` — wraps `A` as `Right[X, A](a)`.
    *
    * @group Instances
    */
  given eitherRevAccessor: ReverseAccessor[Either] with
    def reverseGet[A]: [X] => A => Either[X, A] = [X] => Right[X, A](_: A)
