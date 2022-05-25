package eo
package optics

import data.AffineTraversal

import cats.Traverse

object Traversal:

  def over[T[_]: Traverse, A, B](f: A => B): Optic[T[A], T[B], A, B, AffineTraversal[T]#Travel] =
    new Optic[T[A], T[B], A, B, AffineTraversal[T]#Travel]:
      type X = T[A]
      def to: T[A] => T[A] = identity
      def from: T[A] => T[B] = Traverse[T].map(_)(f)

  def each[T[_]: Traverse, A]: Optic[T[A], T[A], A, A, AffineTraversal[T]#Travel] =
    new Optic[T[A], T[A], A, A, AffineTraversal[T]#Travel]:
      type X = T[A]
      def to: T[A] => T[A] = identity
      def from: T[A] => T[A] = identity
