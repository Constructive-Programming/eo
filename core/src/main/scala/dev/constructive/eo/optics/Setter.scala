package dev.constructive.eo
package optics

import data.SetterF

/** Constructor for `Setter` — write-only single-focus optic, backed by `SetterF`. The caller
  * applies a function at the focus but cannot read it back; useful when observation would leak
  * information or when the focus is genuinely unreadable (e.g. inside a closure).
  *
  * [[Setter.apply]] returns a concrete [[SetterOptic]], so a Setter composes with another Setter
  * through the ordinary `andThen` (the fused [[SetterOptic.andThen]]) — `s1.andThen(s2).modify(f) ==
  * s1.modify(s2.modify(f))` — exactly as `Iso` / `Lens` / `Getter` compose via their own fused
  * subclasses, bypassing the generic `AssociativeFunctor[SetterF]` round-trip.
  */
object Setter:

  /** Construct from `modify: (A => B) => S => T`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Config(values: Map[String, Int])
    * val bumpAll = Setter[Config, Config, Int, Int] { f => cfg =>
    *   cfg.copy(values = cfg.values.view.mapValues(f).toMap)
    * }
    * bumpAll.modify(_ + 1)(cfg)
    *   }}}
    */
  def apply[S, T, A, B](modify: (A => B) => S => T): Setter[S, T, A, B] =
    new Setter(modify)

/** Concrete Optic subclass for a write-only setter. Stores the writer `modifyFn` directly (so the
  * hot path skips the `SetterF` carrier round-trip the generic extension performs) and carries a
  * fused `andThen` for setter∘setter composition — the same shape as [[GetReplaceLens]] /
  * [[Getter]]. Returned by [[Setter.apply]] so hand-written setters pick up the fused path
  * automatically.
  */
final class Setter[S, T, A, B](val modifyFn: (A => B) => S => T) extends Optic[S, T, A, B, SetterF]:
  type X = (S, A)

  def to(s: S): SetterF[X, A] = SetterF(s, identity[A])

  def from(s: SetterF[X, B]): T = modifyFn(s.setter._2)(s.setter._1)

  /** Fused `.modify` — applies the stored writer directly, bypassing the `SetterF` carrier. */
  inline def modify(f: A => B): S => T = modifyFn(f)

  /** Fused `.replace` — constant-writer special case of [[modify]]. */
  inline def replace(b: B): S => T = modifyFn(_ => b)

  /** Fused `Setter.andThen(Setter)` — composes the writers directly:
    * `modify(g) == outer.modify(inner.modify(g))`, skipping the generic
    * `AssociativeFunctor[SetterF]` (`assocSetterF`) round-trip and its per-hop `SetterF` allocation
    * + `asInstanceOf` threading. Scala's overload resolution picks this when both sides are
    * statically `SetterOptic`; mixed-carrier composition falls back to the inherited generic
    * `Optic.andThen`.
    *
    * `inline` so a deep `setter.andThen(setter)…` chain splices distinct writer lambdas per level,
    * staying under C2's recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: Setter[A, B, C, D]): Setter[S, T, C, D] =
    new Setter(cd => modifyFn(inner.modifyFn(cd)))
