package dev.constructive.eo
package laws

import cats.{Applicative, Functor}

import optics.{Review, Unfold}
import optics.Optic.*

/** Law equations for an `Unfold[T, B, F]` — the build-only / many optic (`Optic[Unit, T, Unit, B,
  * Forget[F]]` whose real map is `embed: F[B] => T`).
  *
  * Like [[GetterLaws]], the primary law is *constructor-correctness*: the only observable behaviour
  * of a build-only optic is `embed`, so it must equal whatever reference function the caller claims
  * the unfold represents. The two coherence laws pin the fused `andThen` members to their
  * specification — post-composition through a `Review` re-homes the assembled whole (`(rev ∘
  * u).embed = rev.reverseGet ∘ u.embed`) and pre-composition maps each part (`(u ∘ rev).embed =
  * u.embed ∘ map(rev.reverseGet)`) — so an algebra assembled by optic composition cannot drift from
  * the functions it was assembled from.
  *
  * [[vestigialSingleton]] applies only to `Applicative`-carrier unfolds (built via `Unfold.apply`):
  * the vestigial read side is `pure(())`, so a `modify` round-trip must equal embedding the
  * singleton layer. Pattern-functor unfolds (built via `Unfold.algebra`) have no lawful read side
  * at all — their vestigial `to` throws by specification, which is a behaviour check, not a law
  * (see `UnfoldSpec`).
  */
trait UnfoldLaws[T, B, F[_]]:
  /** The optic under test. */
  def unfold: Unfold[T, B, F]

  /** The function this unfold is declared to represent — typically the same `F[B] => T` passed to
    * `Unfold.apply` / `Unfold.algebra`.
    */
  def reference: F[B] => T

  /** `unfold.embed(fb)` equals the reference function applied at `fb`. */
  def embedConsistent(fb: F[B]): Boolean =
    unfold.embed(fb) == reference(fb)

  /** `Review(f).andThen(unfold)` re-homes the whole: its embed is `f ∘ embed`. */
  def postComposeCoherent(f: T => Int, fb: F[B]): Boolean =
    Review(f).andThen(unfold).embed(fb) == f(unfold.embed(fb))

  /** `unfold.andThen(Review(g))` maps each part: its embed is `embed ∘ map(g)`. */
  def preComposeCoherent(g: Int => B, fi: F[Int])(using F: Functor[F]): Boolean =
    unfold.andThen(Review(g)).embed(fi) == unfold.embed(F.map(fi)(g))

  /** Vestigial-read degradation for `Applicative` carriers: the `modify` round-trip through the
    * vestigial `to = pure(())` equals embedding the singleton layer.
    */
  def vestigialSingleton(b: B)(using F: Applicative[F]): Boolean =
    unfold.modify(_ => b)(()) == unfold.embed(F.pure(b))
