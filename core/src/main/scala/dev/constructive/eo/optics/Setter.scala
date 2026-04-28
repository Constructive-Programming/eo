package dev.constructive.eo
package optics

import data.SetterF

/** Constructor for `Setter` — write-only single-focus optic, backed by `SetterF`. The caller
  * applies a function at the focus but cannot read it back; useful when observation would leak
  * information or when the focus is genuinely unreadable (e.g. inside a closure).
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
  def apply[S, T, A, B](modify: (A => B) => S => T): Optic[S, T, A, B, SetterF] =
    new Optic[S, T, A, B, SetterF]:
      type X = (S, A)

      val to: S => SetterF[X, A] = s => SetterF(s, identity[A])

      val from: SetterF[X, B] => T = s => modify(s.setter._2)(s.setter._1)
