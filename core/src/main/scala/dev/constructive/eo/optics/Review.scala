package dev.constructive.eo
package optics

/** Reverse-only counterpart to [[Getter]] — wraps `reverseGet: A => S`. Doesn't extend [[Optic]]
  * (Optic requires a `to: S => F[X, A]`; pure Review has none). Compose via plain function
  * composition on the underlying `A => S`.
  *
  * [[Review.fromIso]] / [[Review.fromPrism]] (with [[ReversedLens]] / [[ReversedPrism]] aliases)
  * pull the build direction out of an Iso / Prism.
  */
final case class Review[S, A](reverseGet: A => S)

/** Constructors for [[Review]]. */
object Review:

  /** Build direction from a [[BijectionIso]]. @group Constructors */
  def fromIso[S, A](iso: BijectionIso[S, S, A, A]): Review[S, A] =
    Review(iso.reverseGet)

  /** Build direction from a [[MendTearPrism]] (drops the partial read). @group Constructors */
  def fromPrism[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    Review(prism.mend)

/** Alias for [[Review.fromIso]] — Haskell-`optics`-package naming. Unlike Haskell's `ReversedLens`,
  * cats-eo's takes a [[BijectionIso]] because a non-bijective Lens can't reconstruct its source
  * from the focus alone.
  */
object ReversedLens:

  def apply[S, A](iso: BijectionIso[S, S, A, A]): Review[S, A] =
    Review.fromIso(iso)

/** Alias for [[Review.fromPrism]] — Haskell-`optics`-package naming. */
object ReversedPrism:

  def apply[S, A](prism: MendTearPrism[S, S, A, A]): Review[S, A] =
    Review.fromPrism(prism)
