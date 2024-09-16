package eo
package data

import cats.Applicative
import cats.syntax.functor.*

class PowerSeries[A, B](val ps: (Snd[A], Vect[Fst[A], B]) ) extends AnyVal

object PowerSeries:
  given map: ForgetfulFunctor[PowerSeries] with
    def map[X, A, B]: PowerSeries[X, A] => (A => B) => PowerSeries[X, B] =
      (fa: PowerSeries[X, A]) =>
        (f: A => B) =>
          PowerSeries(fa.ps._1 -> Vect.functor[Fst[X]].map(fa.ps._2)(f))

  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      (fa: PowerSeries[X, A]) =>
        (f: A => G[B]) =>
          Vect.trav[Fst[X]].traverse(fa.ps._2)(f).map(b => PowerSeries(fa.ps._1 -> b))
