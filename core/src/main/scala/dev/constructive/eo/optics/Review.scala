package dev.constructive.eo
package optics

import accessor.ReverseAccessor
import data.Direct

/** Reverse-only counterpart to [[Getter]] — wraps `reverseGet: A => S` and is the exact **mirror of
  * [[Getter]]**. `Getter` is `Optic[S, Unit, A, Unit, Direct]` (a real `to` reading `S => A`,
  * vestigial `from`); `Review` is the dual `Optic[Unit, S, Unit, A, Direct]` — a vestigial `to`
  * (reads `Unit`) and a real `from` that *builds* `S` from the focus `A`.
  *
  * It therefore IS an [[Optic]] — with source `Unit` the `to` is exactly as vestigial as `Getter`'s
  * `from`, so "pure Review has no `to`" is no reason to sit outside the trait. A Review composes
  * with another Review through the fused `andThen` just as `Getter`s do, and slots into the
  * `Direct`-carrier optic surface.
  *
  * A `final class` storing `reverseGet` directly — NOT an abstract class with an abstract member —
  * for the same composed-dispatch reason documented on [[Getter]].
  *
  * To get the build direction out of an `Iso` or `Prism`, wrap its reverse directly —
  * `Review(iso.reverseGet)` / `Review(prism.mend)` — rather than via a bespoke factory: an
  * `Iso`/`Prism` already *is* a build direction, so cross-optic `from*` constructors would be
  * redundant (eo has no `Prism.fromIso` etc. for the same reason).
  *
  * Review builds from ONE focus; its many-rung sibling is [[Unfold]] (assemble a `T` from an
  * `F`-layer of parts), reachable by composition through the fused `andThen(Unfold)` below.
  */
final class Review[T, B](build: B => T)
    extends Optic[Unit, T, Unit, B, Direct],
      CanReverseGet[T, B]:
  type X = Nothing

  def reverseGet(b: B): T = build(b)

  def to(u: Unit): Direct[X, Unit] = Direct(u)
  def from(d: Direct[X, B]): T = build(d.value)

  /** Fused `Review.andThen(Review)` — the build-direction mirror of [[Getter.andThen]]. `this`
    * builds `S` from `A`, `inner` builds `A` from `B`, so the composite builds `S` from `B`
    * (`reverseGet ∘ inner.reverseGet`). `inline` so each composition splices a distinct per-level
    * lambda, sidestepping C2's recursive-inlining cap — the same reason [[Getter.andThen]] is
    * inline.
    */
  inline def andThen[D](inner: Review[B, D]): Review[T, D] =
    Review(d => reverseGet(inner.reverseGet(d)))

  /** Build-only outer ∘ ANY reversible inner — only the inner's build half is used (`reverseGet`,
    * available when `G` admits a `ReverseAccessor`: `Direct` for Iso, `Either` for Prism), so the
    * composite stays a build-only [[Review]]. The single-focus mirror of [[Unfold]]'s
    * `andThenBuildAny`.
    */
  inline def andThen[G[_, _], D](inner: Optic[?, B, ?, D, G])(using
      ReverseAccessor[G]
  ): Review[T, D] =
    Review(d => reverseGet(inner.reverseGet(d)))

  /** Fused `Review.andThen(Unfold)` — post-process the assembled whole. `inner` assembles `B` from
    * a layer `F[D]`; `this` builds `T` from that `B`, so the composite is an [[Unfold]] assembling
    * `T` from `F[D]` (`reverseGet ∘ inner.embed`). No constraint on `F` — the seam threads a single
    * `B`, never an `F`-layer, so pattern-functor algebras compose freely. A member (not an
    * extension) so it out-prioritises the `Morph`-summoning generic `andThen`, which would route
    * the same call through `Composer[Direct, Forget[F]]`'s singleton-pick.
    */
  inline def andThen[F[_], D](inner: Unfold[B, D, F]): Unfold[T, D, F] =
    inner.into(reverseGet)

/** Constructors for [[Review]]. */
object Review:

  /** Construct from `reverseGet: A => S`.
    *
    * @group Constructors
    */
  def apply[S, A](reverseGet: A => S): Review[S, A] = new Review(reverseGet)
