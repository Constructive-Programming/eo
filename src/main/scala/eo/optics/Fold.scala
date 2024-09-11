package eo
package optics

import cats.Foldable
import eo.data.Forget
import eo.data.Forgetful

object Fold:

  import Function.const

  def apply[F[_]: Foldable, A]: Optic[F[A], Unit, A, A, Forget[F]] =
    new Optic[F[A], Unit, A, A, Forget[F]]:
      type X = Nothing
      def to: F[A] => F[A] = identity
      def from: F[A] => Unit = const(())

  def select[A](p: A => Boolean): Optic[A, Unit, A, A, Forget[Option]] =
    new Optic[A, Unit, A, A, Forget[Option]]:
      type X = Nothing
      def to: A => Option[A] = Option(_).filter(p)
      def from: Option[A] => Unit = const(())
