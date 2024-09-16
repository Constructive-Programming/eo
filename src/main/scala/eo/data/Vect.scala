package eo
package data

import cats.{Eval, Functor, Traverse}
import cats.syntax.functor.*
import cats.syntax.apply.*
import cats.syntax.applicative.*

import scala.annotation.tailrec
import scala.compiletime.ops.int.*
import cats.Applicative

type Vect[N, +A] = N match
  case 0    => EmptyTuple
  case S[n] => A *: Vect[n, A]

type FixedTraversal[N <: Int] = [A, B] =>> Vect[N, B]

object Vect:
  given functor[N]: Functor[[A] =>> Vect[N, A]] with
    def map[A, B](fa: Vect[N, A])(f: A => B): Vect[N, B] = fa match
      case EmptyTuple:Vect[0, A] => EmptyTuple.asInstanceOf[Vect[N, B]]
      case ((a:A) *: (v:Vect[k, A])) => (f(a) *: functor[k].map(v)(f).asInstanceOf[Tuple]).asInstanceOf[Vect[N, B]]

  given trav[N]: Traverse[[A] =>> Vect[N, A]] with
    def traverse[G[_]: Applicative, A, B](fa: Vect[N, A])(f: A => G[B]): G[Vect[N, B]] = fa match
      case EmptyTuple:Vect[0, A] => EmptyTuple.asInstanceOf[Vect[N, B]].pure[G]
      case ((a:A) *: (v:Vect[k, A])) =>
        f(a).map2(trav[k].traverse[G, A, B](v)(f)){
          (b, v) => (b *: v.asInstanceOf[Tuple]).asInstanceOf[Vect[N, B]]
        }
    def foldLeft[A, B](fa: Vect[N, A], b: B)(f: (B, A) => B): B = fa match
      case EmptyTuple:Vect[0, A] => b
      case ((a:A) *: (v:Vect[k, A])) => f(trav[k].foldLeft(v, b)(f), a)
    def foldRight[A, B](fa: Vect[N, A], b: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match
      case EmptyTuple:Vect[0, A] => b
      case ((a:A) *: (v:Vect[k, A])) => f(a, trav[k].foldRight(v, b)(f))
