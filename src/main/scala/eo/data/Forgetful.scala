package eo
package data

import cats.{Applicative, Invariant}
import cats.syntax.functor._

type Forgetful[X, A] = A
type FId[A] = [X] =>> Forgetful[X, A]

object Forgetful {

  given accesor: Accessor[Forgetful] with
    def get[A]: [X] => Forgetful[X, A] => A =
      [X] => (fa: Forgetful[X, A]) => fa

  given revaccesor: ReverseAccessor[Forgetful] with
    def reverseGet[A]: [X] => A => Forgetful[X, A] =
      [X] => (a: A) => a

  given map: ForgetfulFunctor[Forgetful] with
    def map[X, A, B]: Forgetful[X, A] => (A => B) => Forgetful[X, B] =
      a => f => f(a)

  given traverse: ForgetfulTraverse[Forgetful, Invariant] with
    def traverse[X, A, B, G[_]: Invariant]
        : Forgetful[X, A] => (A => G[B]) => G[Forgetful[X, B]] =
      fa => _(fa)

  given assoc[X, Y]: AssociativeFunctor[Forgetful, X, Y] with
    type Z = Nothing
    def associateLeft[S, A, C]: (S, S => A, A => C) => C =
      case (s, f, g) => g(f(s))
    def associateRight[D, B, T]: (D, D => B, B => T) => T =
      case (d, g, f) => f(g(d))

}
