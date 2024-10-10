package eo
package data

import cats.{Applicative, Eval, Functor, Traverse}
import cats.syntax.functor.*
import cats.syntax.apply.*
import cats.syntax.applicative.*

import scala.annotation.tailrec
import scala.compiletime.ops.int.*


sealed trait Vect[N <: Int, A]:
  type Size = N
  def size: Size
  def +:(h: A): Vect[S[N], A] = ConsVect(h, this)
  def :+(l: A): Vect[S[N], A] = TConsVect(this, l)
  def ++[M <: Int](o: Vect[M, A]): Vect[N + M, A] =AdjacentVect(this, o)
  def slice[L <: Int](offset: Int, len: L): Vect[L, A] = this match
    case _ if offset == 0 && len == size => this.asInstanceOf[Vect[L, A]]
    case NilVect => this.asInstanceOf[Vect[L, A]]
    case AdjacentVect(init, tail) =>
      if (offset + len <= init.size) init.slice(offset, len)
      else if (offset >= init.size) tail.slice(offset - init.size, len)
      else (init.slice(offset, init.size - offset) ++ tail.slice(0, len + offset - init.size)).asInstanceOf
    case ConsVect(head, tail)  =>
      if (offset > 0) tail.slice(offset - 1, len)
      else (head +: tail.slice(0, len - 1)).asInstanceOf[Vect[L, A]]
    case TConsVect(init, last) =>
      if (offset + len < init.size) init.slice(offset, len)
      else (init.slice(offset, len - 1) :+ last).asInstanceOf[Vect[L, A]]

case object NilVect extends Vect[0, Nothing]:
    def size = 0
case class ConsVect[N <: Int, H, T <: Vect[N, H]](head: H, tail: T) extends Vect[S[N], H]:
    def size = (tail.size + 1).asInstanceOf[Size]
case class TConsVect[N <: Int, H, T <: Vect[N, H]](init: T, last: H) extends Vect[S[N], H]:
    def size = (init.size + 1).asInstanceOf[Size]
case class AdjacentVect[N <: Int, M <: Int, H, I <: Vect[N, H], T <: Vect[M, H]](init: I, tail: T) extends Vect[N + M, H]:
    def size = (init.size + tail.size).asInstanceOf[Size]

type FixedTraversal[N <: Int] = [A, B] =>> TupleList[N, B]

type TupleList[N <: Int, A] = N match
  case 0 => EmptyTuple
  case S[n] => A *: TupleList[n, A]

object Vect:

  class Head[N <: Int, A]:
    def unapply(v: Vect[N, A]): Option[A] = v match
      case NilVect => None
      case ConsVect(h, _) => Some(h)
      case TConsVect(init, l) =>
        unapply(init.asInstanceOf)
          .orElse(Some(l))
      case AdjacentVect(init, tail) =>
        unapply(init.asInstanceOf)
          .orElse(unapply(tail.asInstanceOf))

  def nil[N <: Int, A]: Vect[N, A] = NilVect.asInstanceOf

  def of[N <: Int, A](a: A): Vect[N, A] = ConsVect(a, nil).asInstanceOf

  given functor[N <: Int]: Functor[[A] =>> Vect[N, A]] with
    def map[A, B](fa: Vect[N, A])(f: A => B): Vect[N, B] = fa match
      case NilVect => nil
      case ConsVect(h, t) => (f(h) +: functor[t.Size].map(t)(f))
      case TConsVect(t, h) => functor[t.Size].map(t)(f) :+ f(h)
      case AdjacentVect(i, t) => functor[i.Size].map(i)(f) ++ functor[t.Size].map(t)(f)

  given trav[N <: Int]: Traverse[[A] =>> Vect[N, A]] with
    def traverse[G[_]: Applicative, A, B](fa: Vect[N, A])(f: A => G[B]): G[Vect[N, B]] = fa match
      case NilVect => Applicative[G].pure(nil)
      case ConsVect(h, t) => f(h).map2(trav[t.Size].traverse[G, A, B](t)(f))(_ +: _)
      case TConsVect(i, l) => trav[i.Size].traverse[G, A, B](i)(f).map2(f(l))(_ :+ _)
      case AdjacentVect(i, t) =>
        trav[i.Size].traverse(i)(f).map2(trav[t.Size].traverse(t)(f))(_ ++ _)
    def foldLeft[A, B](fa: Vect[N, A], b: B)(f: (B, A) => B): B = fa match
      case NilVect => b
      case ConsVect(h, t) => f(trav[t.Size].foldLeft(t, b)(f), h)
      case TConsVect(i, l) => f(trav[i.Size].foldRight(i, Eval.now(b))((a, eb) => eb.map(f(_, a))).value, l)
      case AdjacentVect(i, t) =>
        val b2 = trav[i.Size].foldLeft(i, b)(f)
        trav[t.Size].foldLeft(t, b2)(f)
    def foldRight[A, B](fa: Vect[N, A], b: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match
      case NilVect => b
      case ConsVect(h, t) => f(h, trav[t.Size].foldRight(t, b)(f))
      case TConsVect(i, l) => f(l, trav[i.Size].foldLeft(i, b)((b, a) => f(a, b)))
      case AdjacentVect(i, t) =>
        val b2 = trav[t.Size].foldRight(t, b)(f)
        trav[i.Size].foldRight(i, b2)(f)
