package dev.constructive.eo
package optics

import scala.annotation.targetName

import cats.{Applicative, Functor}

import accessor.ReverseAccessor
import data.Forget

/** Build-only counterpart to [[Fold]] — the inhabitant of the build-only / many cell of the optic
  * lattice, exactly as [[Review]] is [[Getter]]'s build-only mirror on the total rung. `Fold` is
  * `Optic[S, Unit, A, Unit, Forget[F]]` (a real `to` reading `S => F[A]`, vestigial `from`);
  * `Unfold` is the across-both-axes dual `Optic[Unit, T, Unit, B, Forget[F]]` — a vestigial `to`
  * and a real `from` that *assembles* one `T` from a layer of parts `F[B]`:
  *
  * {{{
  *   embed :  F[B] => T        // many → one: the algebra of a recursion scheme
  * }}}
  *
  * This is the F-shape `embed` of a `Corecursive` instance (`Fold`'s `S => F[A]` being the
  * `project` half), and the aggregation arrow "build an `Order` from its line-items". `Plated`'s
  * `plate` bundles both halves inside one read-write traversal; `Unfold` exposes the embed half
  * standalone, so an algebra can be carried, composed, and consumed as an optic.
  *
  * '''The vestigial `to`.''' Read-only optics zero out their write side for free (`from` discards
  * into `Unit`, the terminal object). The build-only dual is not free: `to: Unit => F[Unit]` must
  * *produce* an `F`-layer, and there is no canonical `F[Unit]` without `Applicative[F]`
  * (`pure(())`). Two constructors, two answers:
  *
  *   - [[Unfold.apply]] (`F: Applicative`) — honest vestigial `to = pure(())`. Read-side operations
  *     that reach `to` (`.modify`, `.foldMap`, …) degrade to the singleton layer.
  *   - [[Unfold.algebra]] (no constraint) — for pattern functors (`BinF`, `RoseF`, …), which admit
  *     `Functor`/`Traverse` but no `Applicative` (`pure` cannot pick a constructor). Its `to` is
  *     genuinely unreachable through the build-only surface and THROWS if forced — the mirror of
  *     `Composer.direct2forget`'s formerly-`???` `from`.
  *
  * A `final class` storing `embed` directly — NOT an abstract member — per the composed-dispatch
  * findings on [[Getter]] / [[Review]]. Fused `andThen` members keep build-only chains concrete.
  */
final class Unfold[T, B, F[_]] @scala.annotation.publicInBinary private (
    val embed: F[B] => T,
    private[optics] val vestigialTo: () => F[Unit],
) extends Optic[Unit, T, Unit, B, Forget[F]]:
  type X = Nothing

  def to(u: Unit): Forget[F][X, Unit] = Forget(vestigialTo())
  def from(fb: Forget[F][X, B]): T = embed(fb.value)

  /** Fused `Unfold.andThen(Review)` — pre-process each part. `inner` builds the part `B` from a
    * `D`, so the composite assembles `T` from a layer of `D`s: `embed ∘ map(inner.reverseGet)`.
    * Needs only `Functor[F]`, so it is available to pattern-functor algebras. `inline` for the same
    * per-level-lambda reason as [[Getter.andThen]] / [[Review.andThen]].
    */
  inline def andThen[D](inner: Review[B, D])(using F: Functor[F]): Unfold[T, D, F] =
    new Unfold(fd => embed(F.map(fd)(inner.reverseGet)), vestigialTo)

  /** Fused `Unfold.andThen(Unfold)` — layered algebra composition on a shared `F`. `inner`
    * assembles the part `B` from its own layer `F[D]`; the composite assembles `T` from that layer
    * by re-lifting the single assembled `B` via `pure` — the same algebraic-lens pull as
    * `assocForgetMonad` (`from_outer ∘ pure ∘ from_inner`), so `Applicative[F]` is required and
    * pattern functors are excluded, exactly as they are from same-carrier `Fold.andThen`.
    */
  inline def andThen[D](inner: Unfold[B, D, F])(using F: Applicative[F]): Unfold[T, D, F] =
    new Unfold(fd => embed(F.pure(inner.embed(fd))), () => F.pure(()))

  /** Build-only outer ∘ ANY reversible inner — an `Unfold` only builds, so the inner's read side is
    * irrelevant and only its build half is threaded: each part `D` is mended to a `B` via
    * `reverseGet` (available when `G` admits a `ReverseAccessor`: `Direct` for Iso, `Either` for
    * Prism), then the layer embeds. The build-direction mirror of [[Getter]]'s `andThenReadAny`;
    * the fused `andThen(Review)` member above stays as the more-specific fast path.
    */
  @targetName("andThenBuildAny")
  inline def andThen[G[_, _], D](inner: Optic[?, B, ?, D, G])(using
      ra: ReverseAccessor[G],
      F: Functor[F],
  ): Unfold[T, D, F] =
    new Unfold(fd => embed(F.map(fd)(d => inner.reverseGet(d))), vestigialTo)

  /** `g ∘ embed` — rehome the assembled whole. Backs the fused [[Review.andThen]] (which cannot
    * reach the private constructor from its own file).
    */
  private[optics] def into[U](g: T => U): Unfold[U, B, F] =
    new Unfold(fb => g(embed(fb)), vestigialTo)

/** Constructors for [[Unfold]] — build-only multi-focus optic, backed by `Forget[F]` (`Forget[F][X,
  * B] = F[B]`) with `S = A = Unit` ruling out the read path; `.embed` is the consumption surface.
  */
object Unfold:

  /** Construct from `embed: F[B] => T` for an `Applicative[F]` — the vestigial `to` is honestly
    * `pure(())`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val sum = Unfold((xs: List[Int]) => xs.sum)
    * sum.embed(List(1, 2, 3))   // 6
    *   }}}
    */
  def apply[T, B, F[_]](embed: F[B] => T)(using F: Applicative[F]): Unfold[T, B, F] =
    new Unfold(embed, () => F.pure(()))

  /** Construct from a recursion-scheme algebra over a pattern functor — `F` need not (and usually
    * cannot) be `Applicative`, so the vestigial `to` has no canonical value and THROWS if a
    * read-side operation forces it. The build-only surface (`.embed`, the fused `andThen`s, the
    * build side of composition) never does.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * enum BinF[+A]:
    *   case LeafF(n: Int); case BranchF(l: A, r: A)
    *
    * val eval = Unfold.algebra[Int, Int, BinF] {
    *   case BinF.LeafF(n)      => n
    *   case BinF.BranchF(l, r) => l + r
    * }
    *   }}}
    */
  def algebra[T, B, F[_]](embed: F[B] => T): Unfold[T, B, F] =
    new Unfold(
      embed,
      () =>
        throw new UnsupportedOperationException(
          "Unfold.algebra: the vestigial read side of a build-only optic over a " +
            "non-Applicative F has no canonical F[Unit]; only build-side operations are available."
        ),
    )
