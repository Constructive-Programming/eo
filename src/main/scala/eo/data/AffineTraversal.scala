package eo
package data

import cats.{Applicative, Functor, Monad, Traverse}

trait AffineTraversal[T[_]]:

  type Travel[A, B] = A match
    case T[a] => T[a]

object AffineTraversal:

  type AT[T[_]] = [X, Y] =>> AffineTraversal[T]#Travel[X, Y]

  given map[T[_]](using F: Functor[T]): ForgetfulFunctor[AT[T]] with
    def map[X, A, B]: AT[T][X, A] => (A => B) => AT[T][X, B] = {
      case at: T[A] =>
        F.map(at)(_: A => B).asInstanceOf[AT[T][X, B]]
    }

  given traverse[T[_]](using T: Traverse[T]): ForgetfulTraverse[AT[T], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: AT[T][X, A] => (A => G[B]) => G[AT[T][X, B]] = {
      case at: T[A] =>
        T.traverse(at)(_: A => G[B]).asInstanceOf[G[AT[T][X, B]]]
    }

  given assoc[T[_], X, Y](using M: Monad[T]): AssociativeFunctor[AT[T], X, Y] with
    type Z = Y

    def associateLeft[S, A, C]: (S, S => AT[T][X, A], A => AT[T][Y, C]) => AT[T][Z, C] =
      case (s, f, g) => M.flatMap[A, C](f(s).asInstanceOf[T[A]])(g.asInstanceOf[A => T[C]]).asInstanceOf[AT[T][Z, C]]

    def associateRight[D, B, R]: (AT[T][Z, D], AT[T][Y, D] => B, AT[T][X, B] => R) => R =
      case (td, g, f) => f(M.pure(g(td)).asInstanceOf[AT[T][X, B]])
