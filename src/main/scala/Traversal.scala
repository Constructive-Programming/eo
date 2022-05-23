package eo

import cats.{Applicative, Traverse}

trait AffineTraversal[T[_]: Traverse] {
  type Tr[A, B] = A match
    case T[a] => T[a]
}

object AffineTraversal {
  given traverse[T[_]](using
      T: Traverse[T]
  ): ForgetfulTraverse[AffineTraversal[T]#Tr, Applicative] with
    type AT[A, B] = AffineTraversal[T]#Tr[A, B]
    def map[X, A, B]: AT[X, A] => (A => B) => AT[X, B] = { case at: T[_] =>
      T.map(at.asInstanceOf[T[A]])(_: A => B).asInstanceOf[AT[X, B]]
    }
    def traverse[X, A, B, G[_]: Applicative]
        : AT[X, A] => (A => G[B]) => G[AT[X, B]] = { case at: T[_] =>
      T.traverse(at.asInstanceOf[T[A]])(_: A => G[B]).asInstanceOf[G[AT[X, B]]]
    }
}

object Traversal {

  def over[T[_]: Traverse, A, B](
      f: A => B
  ): Optic[T[A], T[B], A, B, AffineTraversal[T]#Tr] =
    new Optic[T[A], T[B], A, B, AffineTraversal[T]#Tr] {
      type X = T[A]
      def to: T[A] => T[A] = identity
      def from: T[A] => T[B] = Traverse[T].map(_)(f)
    }

  def each[T[_]: Traverse, A]: Optic[T[A], T[A], A, A, AffineTraversal[T]#Tr] =
    new Optic[T[A], T[A], A, A, AffineTraversal[T]#Tr] {
      type X = T[A]
      def to: T[A] => T[A] = identity
      def from: T[A] => T[A] = identity
    }

}
