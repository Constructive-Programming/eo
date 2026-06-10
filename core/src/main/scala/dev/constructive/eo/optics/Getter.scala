package dev.constructive.eo
package optics

import data.Direct

/** Concrete Optic subclass for a read-only getter. A `final class` storing `get` directly — NOT an
  * abstract class with an abstract `get`: the CI A/B showed composed-getter dispatch through the
  * abstract-class form costs ~1.8x (eoGet_3 5.1ns final vs 11.5ns abstract) even with a fused
  * `andThen`, while every fused path that stayed a concrete class (`GetReplaceLens` lens-reuse) was
  * flat. The hot path skips the `Accessor[Direct]` dispatch the generic extension would perform —
  * the same shape as [[BijectionIso]] / [[GetReplaceLens]]. Returned by [[Getter.apply]] so
  * hand-written getters pick up the fused path automatically.
  */
final class Getter[S, A](val get: S => A) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Nothing

  def to(s: S): Direct[X, A] = Direct(get(s))
  def from(d: Direct[X, Unit]): Unit = ()

  /** Fused `Getter.andThen(Getter)` — composes the read functions; the vestigial `Unit` write path
    * needs no threading.
    *
    * `inline` so each composition call site splices its own copy of the `s => inner.get(get(s))`
    * lambda, yielding a *distinct* synthetic method per level. A plain `def` compiles the lambda
    * once (`andThen$$anonfun$1`), so a depth-N runtime chain reuses that one bytecode and C2 treats
    * the `.get` cascade as recursion — capping inlining at `MaxRecursiveInlineLevel` and leaving
    * the deep tail as virtual `Function1.apply`. Distinct per-level methods sidestep that cap with
    * no JVM flag, the same way Monocle's per-compose anonymous classes do.
    */
  inline def andThen[B](inner: Getter[A, B]): Getter[S, B] =
    new Getter(s => inner.get(get(s)))

  /** Read-only outer ∘ ANY inner — a `Getter` only reads, so the inner's write side is irrelevant
    * and the composite is the read-only join via [[ReadCompose]] (`Getter` when the inner reads
    * totally, `PickFold` when partially, `ForgetFold` when it has many foci). This member covers
    * the writable inners (`getter ∘ lens / prism / optional / traversal`) that the trait's
    * read-only-inner overload cannot reach; the fused `andThen(Getter)` / `andThen(PickFold)`
    * members above stay as the more-specific fast paths.
    */
  @annotation.targetName("andThenReadAny")
  def andThen[C, IT, IB, FI[_, _]](inner: Optic[A, IT, C, IB, FI])(using
      rc: ReadCompose[Direct, FI]
  ): rc.Out[S, C] =
    rc.compose(this, inner)

  /** Override of the trait's read-only-inner overload, re-homed in this class so it shares an owner
    * with `andThenReadAny` above — otherwise a read-only inner is ambiguous between the two (the
    * trait one wins on signature specificity, the class one on owner derivation, and dotty calls
    * the split a tie). Same `ReadCompose` route either way.
    */
  override def andThen[C, IB, G[_, _]](inner: Optic[A, Unit, C, IB, G])(using
      rc: ReadCompose[Direct, G]
  ): rc.Out[S, C] =
    rc.compose(this, inner)

/** Constructor for `Getter` — read-only single-focus optic, backed by `Direct` with `T = B = Unit`.
  * `.get(s)` is the only meaningful operation; the write path is vestigial.
  *
  * Both the leftover `T` *and* the back-focus `B` are `Unit`, which makes the read-only-ness
  * explicit in the type (there is no `B` to put back). [[Getter.apply]] returns a concrete
  * [[Getter]], so a Getter composes with another Getter through the ordinary `andThen` (the fused
  * `Getter.andThen`) — `g1.andThen(g2)` reads `s => g2.get(g1.get(s))` — exactly as `Iso` / `Lens`
  * compose via their own fused subclasses.
  */
object Getter:

  /** Construct from `get: S => A`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val nameLength = Getter[Person, Int](_.name.length)
    *   }}}
    */
  def apply[S, A](get: S => A): Getter[S, A] =
    new Getter(get)
