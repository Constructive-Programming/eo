package eo
package optics

import data.AffineTraversal

import cats.{Applicative, Foldable}
import cats.instances.option._

object Fold {
  import Function.const

  def select[A](p: A => Boolean): Optic[A, Unit, A, A, AffineTraversal[Option]#Travel] =
    new Optic[A, Unit, A, A, AffineTraversal[Option]#Travel] {
      type X = Option[A]
      def to: A => Option[A] = Option(_).filter(p)
      def from: Option[A] => Unit = const(())
    }

  def folding[T[_]: Foldable, A]: Optic[T[A], Unit, A, A, AffineTraversal[T]#Travel] =
    new Optic[T[A], Unit, A, A, AffineTraversal[T]#Travel] {
      type X = T[A]
      def to: T[A] => T[A] = identity
      def from: T[A] => Unit = const(())

    }
}
