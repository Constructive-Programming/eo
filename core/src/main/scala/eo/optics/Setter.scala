package eo
package optics

import data.SetterF

/** Constructor for `Setter` — the modify-only single-focus optic, backed by the `SetterF` carrier.
  *
  * A `Setter[S, A]` (short for `Optic[S, S, A, A, SetterF]`) encodes a write-through position where
  * the caller can apply a function at the focus but cannot read the focus back out. Useful when
  * observation would leak information the author doesn't want exposed, or when the focus is
  * genuinely unreadable (e.g. a value defined only inside a closure).
  */
object Setter:

  /** Construct a Setter from a `modify: (A => B) => S => T` that applies an arbitrary function at
    * the focus and returns the modified `T`.
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
  def apply[S, T, A, B](modify: (A => B) => S => T): Optic[S, T, A, B, SetterF] =
    new Optic[S, T, A, B, SetterF]:
      type X = (S, A)

      val to: S => SetterF[X, A] = s => SetterF(s, identity[A])

      val from: SetterF[X, B] => T = s =>
        modify(s.setter._2)(s.setter._1)
