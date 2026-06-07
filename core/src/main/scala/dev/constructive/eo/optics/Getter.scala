package dev.constructive.eo
package optics

import data.Direct

/** Constructor for `Getter` — read-only single-focus optic, backed by `Direct` with `T = B = Unit`.
  * `.get(s)` is the only meaningful operation; the write path is vestigial.
  *
  * Both the leftover `T` *and* the back-focus `B` are `Unit`, which makes the read-only-ness
  * explicit in the type (there is no `B` to put back). [[Getter.apply]] returns a concrete
  * [[ReadGetter]], so a Getter composes with another Getter through the ordinary `andThen` (the
  * fused `ReadGetter.andThen`) — `g1.andThen(g2)` reads `s => g2.get(g1.get(s))` — exactly as `Iso`
  * / `Lens` compose via their own fused subclasses.
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
  def apply[S, A](get: S => A): ReadGetter[S, A] =
    new ReadGetter(get)

/** Concrete Optic subclass for a read-only getter. Stores `get` directly (so the hot path skips the
  * `Accessor[Direct]` dispatch the generic extension would perform) and carries a fused `andThen`
  * for getter∘getter composition — the same shape as [[BijectionIso]] / [[GetReplaceLens]].
  * Returned by [[Getter.apply]] so hand-written getters pick up the fused path automatically.
  */
final class ReadGetter[S, A](val get: S => A) extends Optic[S, Unit, A, Unit, Direct]:
  type X = Nothing
  val to: S => A = get
  val from: Unit => Unit = identity

  /** Fused `Getter.andThen(Getter)` — composes the read functions; the vestigial `Unit` write path
    * needs no threading.
    */
  def andThen[B](inner: ReadGetter[A, B]): ReadGetter[S, B] =
    new ReadGetter(s => inner.get(get(s)))
