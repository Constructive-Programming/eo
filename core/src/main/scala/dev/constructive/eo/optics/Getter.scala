package dev.constructive.eo
package optics

import data.Direct

/** Constructor for `Getter` — read-only single-focus optic, backed by `Direct` with `T = Unit`.
  * `.get(s)` is the only operation; no write path. Doesn't compose with other Getters (the inner
  * `T = Unit` mismatches the outer `B` slot); use a Lens chain and call `.get` on the result.
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
  def apply[S, A](get: S => A): Optic[S, Unit, A, A, Direct] =
    new Optic[S, Unit, A, A, Direct]:
      type X = Nothing
      val to: S => A = get
      val from: A => Unit = _ => ()
