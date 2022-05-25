package eo
package optics

import data.Forgetful

object Iso:

  def apply[S, T, A, B](f: S => A, g: B => T): Optic[S, T, A, B, Forgetful] =
    new Optic[S, T, A, B, Forgetful]:
      type X = Nothing
      def to: S => A = f
      def from: B => T = g
