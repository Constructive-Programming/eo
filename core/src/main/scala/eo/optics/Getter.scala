package eo
package optics

import data.Forgetful

/** Constructor for `Getter` — the read-only single-focus optic, backed by the `Forgetful` carrier
  * (identity in its focus parameter) with `T = Unit` to signal "no write path".
  *
  * A `Getter[S, A]` (short for `Optic[S, Unit, A, A, Forgetful]`) encodes a pure projection —
  * `name.length`, `person.age`, any derived view. `.get(s)` is the primary operation; `modify` /
  * `replace` / `reverseGet` are unavailable because `T = Unit` provides no way back to `S`.
  *
  * Composition note: `Getter` does not compose with other Getters via `Optic.andThen`, because the
  * inner Getter's `T = Unit` mismatches the outer Getter's expected `B` slot. Compose the
  * underlying functions directly, or use a `Lens` chain and call `.get` on the composed lens.
  */
object Getter:
  import Function.const

  /** Construct a Getter from `get: S => A`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val nameLength = Getter[Person, Int](_.name.length)
    *   }}}
    */
  def apply[S, A](get: S => A): Optic[S, Unit, A, A, Forgetful] =
    new Optic[S, Unit, A, A, Forgetful]:
      type X = Nothing
      val to: S => A = get
      val from: A => Unit = _ => ()
