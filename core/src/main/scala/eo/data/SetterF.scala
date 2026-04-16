package eo
package data

import cats.{Bifunctor, Distributive, Traverse}
import cats.instances.function._
import cats.syntax.functor._

class SetterF[A, B](val setter: (Fst[A], Snd[A] => B)) extends AnyVal

object SetterF:
  given map[S, A]: ForgetfulFunctor[SetterF] with
    def map[X, B, C]: SetterF[X, B] => (B => C) => SetterF[X, C] =
      s => g => SetterF(s.setter._1, s.setter._2.andThen(g))

  given traverse[S, A]: ForgetfulTraverse[SetterF, Distributive] with
    def traverse[X, B, C, G[_]](using
        D: Distributive[G]
    ): SetterF[X, B] => (B => G[C]) => G[SetterF[X, C]] =
      s => g => D.tupleLeft(D.distribute(s.setter._2)(g), s.setter._1).map(SetterF(_))
