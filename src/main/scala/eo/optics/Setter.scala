package eo
package optics

import data.SetterF

object Setter:

  def apply[S, T, A, B](modify: (A => B) => S => T): Optic[S, T, A, B, SetterF] =
    new Optic[S, T, A, B, SetterF]:
      type X = (S, A)

      def to: S => (S, A => A) = s => (s, identity[A])

      def from: SetterF[X, B] => T = {
        case (s, f) => modify(f)(s)
      }
