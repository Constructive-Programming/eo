package eo
package data

import cats.{Applicative, Functor, Traverse}

trait AffineTraversal[T[_]]:

  type Travel[A, B] = A match
    case T[a] => T[a]

object AffineTraversal:

  type AT[T[_]] = [X, Y] =>> AffineTraversal[T]#Travel[X, Y]

  given map[T[_]](using F: Functor[T]): ForgetfulFunctor[AT[T]] with
    def map[X, A, B]: AT[T][X, A] => (A => B) => AT[T][X, B] = {
      case at: T[_] =>
        F.map(at.asInstanceOf[T[A]])(_: A => B).asInstanceOf[AT[T][X, B]]
    }

  given traverse[T[_]](using T: Traverse[T]): ForgetfulTraverse[AT[T], Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: AT[T][X, A] => (A => G[B]) => G[AT[T][X, B]] = {
      case at: T[_] =>
        T.traverse(at.asInstanceOf[T[A]])(_: A => G[B]).asInstanceOf[G[AT[T][X, B]]]
    }

