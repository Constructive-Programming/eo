package dev.constructive.eo
package optics

import data.{Direct, ReverseAccessor}

/** Reverse-only counterpart to [[Getter]] — wraps `reverseGet: A => S` and is the exact **mirror of
  * [[Getter]]**. `Getter` is `Optic[S, Unit, A, Unit, Direct]` (a real `to` reading `S => A`,
  * vestigial `from`); `Review` is the dual `Optic[Unit, S, Unit, A, Direct]` — a vestigial `to`
  * (reads `Unit`) and a real `from` that *builds* `S` from the focus `A`.
  *
  * It therefore IS an [[Optic]]. (An earlier version was a bare `case class` that did not extend
  * `Optic` on the grounds that "pure Review has no `to`" — but with source `Unit` the `to` is
  * exactly as vestigial as `Getter`'s `from`, so that asymmetry with `Getter` was a choice, not a
  * necessity.) A Review composes with another Review through the fused `andThen` just as `Getter`s
  * do, and slots into the `Direct`-carrier optic surface.
  *
  * A `final class` storing `reverseGet` directly — NOT an abstract class with an abstract member —
  * for the same composed-dispatch reason documented on [[Getter]].
  *
  * To get the build direction out of an `Iso` or `Prism`, wrap its reverse directly —
  * `Review(iso.reverseGet)` / `Review(prism.mend)` — rather than via a bespoke factory: an
  * `Iso`/`Prism` already *is* a build direction, so cross-optic `from*` constructors would be
  * redundant (eo has no `Prism.fromIso` etc. for the same reason).
  */
final class Review[T, B](val reverseGet: B => T) extends Optic[Unit, T, Unit, B, Direct]:
  type X = Nothing

  def to(u: Unit): Direct[X, Unit] = Direct(u)
  def from(d: Direct[X, B]): T = reverseGet(d.value)

  /** Fused `Review.andThen(Review)` — the build-direction mirror of [[Getter.andThen]]. `this`
    * builds `S` from `A`, `inner` builds `A` from `B`, so the composite builds `S` from `B`
    * (`reverseGet ∘ inner.reverseGet`). `inline` so each composition splices a distinct per-level
    * lambda, sidestepping C2's recursive-inlining cap — the same reason [[Getter.andThen]] is
    * inline.
    */
  inline def andThen[D](inner: Review[B, D]): Review[T, D] =
    Review(d => reverseGet(inner.reverseGet(d)))

  inline def andThen[G[_, _], D](inner: Optic[?, B, ?, D, G])(using
      ReverseAccessor[G]
  ): Review[T, D] =
    Review(d => reverseGet(inner.reverseGet(d)))

/** Constructors for [[Review]]. */
object Review:

  /** Construct from `reverseGet: A => S`.
    *
    * @group Constructors
    */
  def apply[S, A](reverseGet: A => S): Review[S, A] = new Review(reverseGet)
