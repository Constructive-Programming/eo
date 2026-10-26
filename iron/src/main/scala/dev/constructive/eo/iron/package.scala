package dev.constructive.eo

/** [[https://github.com/Iltotore/iron Iron]] refined types as optics: every runtime-checked
  * refinement `A :| C` is the lawful Prism `A ⇀ A :| C` ([[iron.IronPrism]]) — test the constraint
  * on the way in, forget it on the way out.
  *
  * {{{
  *   val positive = IronPrism[Int, Positive]
  *   positive.getOption(-1)                        // None
  *   ageLens.andThen(positive)                     // Optional[Person, Int :| Positive]
  * }}}
  */
package object iron
