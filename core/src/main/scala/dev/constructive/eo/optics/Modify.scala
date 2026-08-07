package dev.constructive.eo
package optics

import cats.Functor

import data.ModifyF

/** Constructor for `Modify` — write-only single-focus optic, backed by `ModifyF`. The caller
  * applies a function at the focus but cannot read it back; useful when observation would leak
  * information or when the focus is genuinely unreadable (e.g. inside a closure).
  *
  * '''This is Monocle's / Haskell lens's `Setter`, renamed.''' The mapping is `Setter.modify` ↦
  * `Modify.modify` and `Setter.set` ↦ `Modify.replace` — do not look for a `Setter` type here.
  *
  * [[Modify.apply]] returns a concrete [[Modify]], so a Modify composes with another Modify through
  * the ordinary `andThen` (the fused [[Modify.andThen]]) — `s1.andThen(s2).modify(f) ==
  * s1.modify(s2.modify(f))` — exactly as `Iso` / `Lens` / `Getter` compose via their own fused
  * subclasses, bypassing the generic `AssociativeFunctor[ModifyF]` round-trip.
  */
object Modify:

  /** Construct from `modify: (A => B) => S => T`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Config(values: Map[String, Int])
    * val bumpAll = Modify[Config, Config, Int, Int] { f => cfg =>
    *   cfg.copy(values = cfg.values.view.mapValues(f).toMap)
    * }
    * bumpAll.modify(_ + 1)(cfg)
    *   }}}
    */
  def apply[S, T, A, B](modify: (A => B) => S => T): Modify[S, T, A, B] =
    new Modify(modify)

  /** Monomorphic write-only Modify over any `Functor[F]`: `modify = F.map` — the [[Traversal.each]]
    * analog for containers that are mappable but not foldable into view (`Function1[R, *]`, `Eval`,
    * …). `Modify` extends `CanModifyP` directly, so the result is capability evidence as-is.
    *
    * @group Constructors
    */
  def forFunctor[F[_]: Functor, A]: Modify[F[A], F[A], A, A] =
    pForFunctor[F, A, A]

  /** Polymorphic counterpart to [[forFunctor]] — allows focus type change.
    *
    * @group Constructors
    */
  def pForFunctor[F[_], A, B](using F: Functor[F]): Modify[F[A], F[B], A, B] =
    Modify(f => fa => F.map(fa)(f))

/** Concrete Optic subclass for a write-only modifier. Stores the writer `modifyFn` directly (so the
  * hot path skips the `ModifyF` carrier round-trip the generic extension performs) and carries a
  * fused `andThen` for modify∘modify composition — the same shape as [[GetReplaceLens]] /
  * [[Getter]]. Returned by [[Modify.apply]] so hand-written modifiers pick up the fused path
  * automatically.
  */
final class Modify[S, T, A, B](val modifyFn: (A => B) => S => T)
    extends Optic[S, T, A, B, ModifyF],
      CanModifyP[S, T, A, B]:
  type X = (S, A)

  def to(s: S): ModifyF[X, A] = ModifyF(s, identity[A])

  def from(s: ModifyF[X, B]): T = modifyFn(s.modifier._2)(s.modifier._1)

  /** Fused `.modify` — applies the stored writer directly, bypassing the `ModifyF` carrier. */
  inline def modify(f: A => B): S => T = modifyFn(f)

  /** Fused `.replace` — constant-writer special case of [[modify]]. */
  override inline def replace(b: B): S => T = modifyFn(_ => b)

  /** Fused `Modify.andThen(Modify)` — composes the writers directly:
    * `modify(g) == outer.modify(inner.modify(g))`, skipping the generic
    * `AssociativeFunctor[ModifyF]` (`assocModifyF`) round-trip and its per-hop `ModifyF` allocation
    * + `asInstanceOf` threading. Scala's overload resolution picks this when both sides are
    * statically `Modify`; mixed-carrier composition falls back to the inherited generic
    * `Optic.andThen`.
    *
    * `inline` so a deep `modify.andThen(modify)…` chain splices distinct writer lambdas per level,
    * staying under C2's recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: Modify[A, B, C, D]): Modify[S, T, C, D] =
    new Modify(cd => modifyFn(inner.modifyFn(cd)))
