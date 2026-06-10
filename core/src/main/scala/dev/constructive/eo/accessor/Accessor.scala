package dev.constructive.eo
package accessor

/** Read the focus `A` out of a two-parameter carrier `F[X, A]`. Required by `Optic.get`;
  * always-hitting carriers (`Tuple2` for Lens, `Direct` for Iso / Getter) supply one.
  *
  * @tparam F
  *   the carrier
  */
trait Accessor[F[_, _]]:
  def get[X, A](fa: F[X, A]): A

/** Typeclass instances for [[Accessor]]. */
object Accessor:

  /** `Tuple2` — picks the right element. @group Instances */
  given tupleAccessor: Accessor[Tuple2] with
    def get[X, A](fa: (X, A)): A = fa._2
