package eo
package data

import cats.{Distributive, Traverse}
import cats.instances.function._

type SetterF[A, B] = A match
  case (s, a) => (s, a => B)

object SetterF:

  given map[S, A]: ForgetfulFunctor[SetterF] with

    def map[X, B, C]: SetterF[X, B] => (B => C) => SetterF[X, C] = {
      case (s: S, f: Function1[A, B]) => g => (s, f.andThen(g)).asInstanceOf[SetterF[X, C]]
    }

  given traverse[S, A]: ForgetfulTraverse[SetterF, Distributive] with

    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] = {
      case (s: S, f: Function1[A, B]) =>
        g => D.tupleLeft(D.distribute(f)(g), s).asInstanceOf[G[SetterF[X, C]]]
    }
