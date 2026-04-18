package eo
package data

import cats.{Applicative, Eval, Functor, Traverse}
import cats.syntax.functor.*
import cats.syntax.apply.*
import cats.syntax.applicative.*

import scala.annotation.tailrec
import scala.compiletime.ops.int.*


/** Length-indexed immutable vector: `Vect[N, A]` holds exactly `N`
  * elements of type `A`, with the length visible at the type level.
  *
  * Four constructors encode four algebraic shapes:
  *   - [[NilVect]] — the empty vector (`N = 0`).
  *   - [[ConsVect]] — head + tail.
  *   - [[TConsVect]] — init + last.
  *   - [[AdjacentVect]] — structurally-concatenated pair.
  *
  * `slice` / `++` / `+:` / `:+` all operate at the type level on
  * the length index, so invalid accesses (`slice(5, 3)` on a
  * `Vect[2, A]`) are rejected at compile time. The phantom
  * `N <: Int` is the whole point — runtime bounds checks are not
  * a substitute.
  *
  * Primary consumer is [[PowerSeries]] (per-index composition of
  * traversals); [[FixedTraversal]] and the `Traversal.two` /
  * `.three` / `.four` constructors also build on it.
  *
  * @tparam N length of the vector (0, 1, 2, …)
  * @tparam A element type
  */
sealed trait Vect[N <: Int, A]:
  type Size = N
  def size: Size
  def +:(h: A): Vect[S[N], A] = ConsVect(h, this)
  def :+(l: A): Vect[S[N], A] = TConsVect(this, l)
  def ++[M <: Int](o: Vect[M, A]): Vect[N + M, A] =AdjacentVect(this, o)
  def slice[L <: Int](offset: Int, len: L): Vect[L, A] = this match
    case _ if len == 0 => Vect.nil[L, A]
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
      // `<=` not `<`: when the slice range lands exactly at the end
      // of `init` (offset + len == init.size), we want to recurse
      // into `init` rather than drop a window by one and append
      // `last`. The `<` form silently took the "cross the boundary"
      // path for single-element slices at offset 0 on a size-1 init,
      // returning `[last]` instead of `[init[0]]`.
      if (offset + len <= init.size) init.slice(offset, len)
      else (init.slice(offset, len - 1) :+ last).asInstanceOf[Vect[L, A]]

object NilVect extends Vect[0, Nothing]:
    def size = 0

case class ConsVect[N <: Int, H, T <: Vect[N, H]](head: H, tail: T) extends Vect[S[N], H]:
    def size = (tail.size + 1).asInstanceOf[Size]

case class TConsVect[N <: Int, H, T <: Vect[N, H]](init: T, last: H) extends Vect[S[N], H]:
    def size = (init.size + 1).asInstanceOf[Size]

case class AdjacentVect[N <: Int, M <: Int, H, I <: Vect[N, H], T <: Vect[M, H]](init: I, tail: T) extends Vect[N + M, H]:
    def size = (init.size + tail.size).asInstanceOf[Size]

// NOTE: a tuple-based `FixedTraversal[N] = [A, B] =>> TupleList[N, B]`
// existed here on the `unthreaded` branch but collided with the
// phantom-X flavour carried by `FixedTraversal.scala`. Dropped during
// the unthreaded merge; the `TupleList` alias remains in case Vect
// helpers want it.
type TupleList[N <: Int, A] = N match
  case 0 => EmptyTuple
  case S[n] => A *: TupleList[n, A]

object Vect:

  object Head:
    def unapply[N <: Int, A](v: Vect[N, A]): Option[A] = v match
      case NilVect => None
      case ConsVect(h, _) => Some(h)
      case TConsVect(init, l) =>
        unapply(init).orElse(Some(l))
      case AdjacentVect(init, tail) =>
        unapply(init).orElse(unapply(tail))

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
      // ConsVect(h, t) is head-first: accumulate h, then foldLeft tail.
      case ConsVect(h, t) => trav[t.Size].foldLeft(t, f(b, h))(f)
      // TConsVect(i, l) is init-then-last: foldLeft init, then accumulate l.
      case TConsVect(i, l) => f(trav[i.Size].foldLeft(i, b)(f), l)
      case AdjacentVect(i, t) =>
        val b2 = trav[i.Size].foldLeft(i, b)(f)
        trav[t.Size].foldLeft(t, b2)(f)
    def foldRight[A, B](fa: Vect[N, A], b: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match
      case NilVect => b
      // ConsVect(h, t) is head-first: foldRight tail, then combine with h.
      case ConsVect(h, t) => f(h, trav[t.Size].foldRight(t, b)(f))
      // TConsVect(i, l) is init-then-last: foldRight init with f(l, b) accumulator.
      case TConsVect(i, l) => trav[i.Size].foldRight(i, f(l, b))(f)
      case AdjacentVect(i, t) =>
        val b2 = trav[t.Size].foldRight(t, b)(f)
        trav[i.Size].foldRight(i, b2)(f)
