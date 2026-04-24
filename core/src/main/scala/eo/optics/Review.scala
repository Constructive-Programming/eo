package eo
package optics

/** The reverse-only counterpart to [[Getter]] — wraps a `reverseGet: A => S` build function, for
  * cases where you want to reconstruct an outer shape from a focus without a read path in the
  * other direction.
  *
  * A `Review[S, A]` deliberately does **not** extend [[Optic]]: the Optic trait requires a `to: S
  * => F[X, A]` that observes the outer shape, and a pure Review has none. Same design call as
  * [[eo.circe.JsonTraversal]] — when the integration surface is closed, reaching for the Optic
  * trait would pay for a contract this type can't satisfy.
  *
  * Reviews are a plain case-class wrapper. Compose via direct function composition on the
  * underlying `A => S`:
  *
  * {{{
  *   val r1 = Review[Int, String](_.length)
  *   val r2 = Review[Option[Int], Int](Some(_))
  *   val composed = Review[Option[Int], String](s => r2.reverseGet(r1.reverseGet(s)))
  * }}}
  *
  * [[Review.fromIso]] / [[Review.fromPrism]] (and their [[ReversedLens]] / [[ReversedPrism]]
  * aliases) pull the natural build-direction out of an Iso or Prism.
  */
final case class Review[S, A](reverseGet: A => S)

/** Constructors for [[Review]]. Two entry points: [[Review.fromIso]] (extract the build direction
  * from a `BijectionIso`) and [[Review.fromPrism]] (extract the `mend` direction from a
  * `MendTearPrism`). Free-form `Review.apply(f)` also works — `Review` is a plain case class
  * wrapping `A => S`.
  */
object Review:

  /** Extract the natural build direction from a [[BijectionIso]]. Every Iso ships a
    * `reverseGet: A => S` — this lifts it into a standalone Review.
    *
    * @group Constructors
    */
  def fromIso[S, A](iso: BijectionIso[S, S, A, A]): Review[S, A] =
    Review(iso.reverseGet)

  /** Extract the natural build direction from a [[MendTearPrism]]. Every Prism ships a `mend: A =>
    * S` construction for its hit branch; a Review collapses the optic down to just that build path,
    * so the rest of the Prism's machinery (the partial read) is left behind.
    *
    * @group Constructors
    */
  def fromPrism[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    Review(prism.mend)

/** Alias-factory for [[Review.fromIso]]. Matches the Haskell `optics`-package naming convention for
  * users who expect to find "ReversedLens" next to Lens / Prism / Iso in the optics reference.
  *
  * Note: unlike Haskell's `ReversedLens` (which is a phantom-tagged view over any Lens), cats-eo's
  * ReversedLens takes a bijective [[BijectionIso]] because a non-bijective Lens doesn't carry
  * enough information to reconstruct its source from the focus alone. For a general Lens you'd need
  * to provide the missing "leftover" yourself — at which point you're writing
  * `Review.apply(a => rebuildOuter(a))` directly anyway.
  */
object ReversedLens:

  /** Reverse a bijective Lens (i.e. an Iso) into a Review. See [[Review.fromIso]]. */
  def apply[S, A](iso: BijectionIso[S, S, A, A]): Review[S, A] =
    Review.fromIso(iso)

/** Alias-factory for [[Review.fromPrism]]. Matches the Haskell `optics`-package naming convention —
  * a Prism's `mend: A => S` *is* a Review, and this factory names it as such.
  */
object ReversedPrism:

  /** Reverse a Prism — i.e. pull out its natural `A => S` build direction. See
    * [[Review.fromPrism]].
    */
  def apply[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    Review.fromPrism(prism)
