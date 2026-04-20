package eo
package optics

/** The reverse-only counterpart to [[Getter]] — wraps a `reverseGet: A => S` build function with
  * composition support, for cases where you want to reconstruct an outer shape from a focus without
  * a read path in the other direction.
  *
  * A `Review[S, A]` deliberately does **not** extend [[Optic]]: the Optic trait requires a `to: S =>
  * F[X, A]` that observes the outer shape, and a pure Review has none. Same design choice as
  * [[eo.circe.JsonTraversal]] — when the integration surface is closed (compose with other Reviews,
  * run `.reverseGet`), reaching for the Optic trait would pay for a contract this type can't
  * satisfy.
  *
  * Reviews compose via [[andThen]] with other Reviews into a single `A => S` chain; the companion
  * exposes [[Review.fromIso]] and [[Review.fromPrism]] to pull the natural build-direction out of
  * an Iso or Prism, and the [[ReversedLens]] / [[ReversedPrism]] aliases give those factories their
  * Haskell-`optics`-style names.
  *
  * @example
  *   {{{
  * enum Shape:
  *   case Circle(r: Double)
  *   case Square(s: Double)
  * val circleReview = Review[Shape, Double](r => Shape.Circle(r))
  * circleReview.reverseGet(2.0)   // Shape.Circle(2.0)
  *   }}}
  */
final class Review[S, A](val reverseGet: A => S):

  /** Compose with another Review whose source is this one's focus. The chain runs right-to-left
    * under `reverseGet`: `andThen(inner).reverseGet(c) == reverseGet(inner.reverseGet(c))`.
    */
  def andThen[B](inner: Review[A, B]): Review[S, B] =
    new Review(b => reverseGet(inner.reverseGet(b)))

  override def toString(): String = s"Review($reverseGet)"

object Review:

  /** Construct a Review from a raw `A => S` build function. */
  def apply[S, A](reverseGet: A => S): Review[S, A] = new Review(reverseGet)

  /** Extract the natural build direction from an [[BijectionIso]]. Every Iso ships a
    * `reverseGet: A => S` — this lifts it into a standalone Review that composes with other Reviews
    * and Prism-derived builds.
    *
    * @group Constructors
    */
  def fromIso[S, A](iso: BijectionIso[S, S, A, A]): Review[S, A] =
    new Review(iso.reverseGet)

  /** Extract the natural build direction from a [[MendTearPrism]]. Every Prism ships a `mend: A =>
    * S` construction for its hit branch; a Review collapses the optic down to just that build path,
    * so the rest of the Prism's machinery (the partial read) is left behind.
    *
    * @group Constructors
    */
  def fromPrism[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    new Review(prism.mend)

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
  * a Prism's `mend: A => S` *is* a Review, and this factory names it as such for composition with
  * other Reviews.
  */
object ReversedPrism:

  /** Reverse a Prism — i.e. pull out its natural `A => S` build direction. See
    * [[Review.fromPrism]].
    */
  def apply[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    Review.fromPrism(prism)
