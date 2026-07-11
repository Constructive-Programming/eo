package dev.constructive.eo
package accessor

import data.Affine

/** Partial counterpart to [[Accessor]] — read the focus `A` out of `F[X, A]` when present, `None`
  * on the miss branch. Required by `Optic.getOption`; maybe-hitting carriers (`Either` for Prism,
  * `Affine` for Optional / AffineFold) supply one.
  *
  * @tparam F
  *   the carrier
  */
trait PartialAccessor[F[_, _]]:

  def getOption[X, A](fa: F[X, A]): Option[A]

/** Typeclass instances for [[PartialAccessor]]. */
object PartialAccessor:

  /** `Either` — `Right` is the hit branch. @group Instances */
  given eitherAccessor: PartialAccessor[Either] with
    def getOption[X, A](fa: Either[X, A]) = fa.toOption

  /** `Affine` — `Hit` yields the focus, `Miss` yields `None`. @group Instances */
  given affineAccessor: PartialAccessor[Affine] with
    def getOption[X, A](fa: Affine[X, A]): Option[A] = fa.fold(_ => None, (_, h) => Some(h))
