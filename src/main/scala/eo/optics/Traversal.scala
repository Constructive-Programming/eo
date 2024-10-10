package eo
package optics

import data.{FixedTraversal, PowerSeries, Vect}

import cats.{Applicative, Monad, Traverse, MonoidK}
import cats.syntax.foldable.*
import cats.Applicative

object Traversal:

  def each[T[_]: Traverse: Applicative: MonoidK, A]: Optic[T[A], T[A], A, A, PowerSeries] =
    pEach[T, A, A]

  def pEach[T[_]: Traverse: Applicative: MonoidK, A, B]: Optic[T[A], T[B], A, B, PowerSeries] =
    new Optic[T[A], T[B], A, B, PowerSeries]:
      type X = (Int, Unit)
      def to: T[A] => PowerSeries[X, A] =
        ta => PowerSeries(() -> Traverse[T].foldLeft(ta, Vect.nil[Int, A])(
                            (v, a) => (v :+ a).asInstanceOf))
      def from: PowerSeries[X, B] => T[B] =
        ps => Vect.trav[Int].foldMapK(ps.ps._2)(Applicative[T].pure[B])

  def two[S, T, A, B](
      a: S => A,
      b: S => A,
      reverse: (B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[2]] =
    new Optic[S, T, A, B, FixedTraversal[2]]:
      type X = EmptyTuple
      def to: S => (A, A) = s => (a(s), b(s))
      def from: FixedTraversal[2][X, B] => T =
        case (b0, b1) => reverse(b0, b1)

  def three[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      reverse: (B, B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[3]] =
    new Optic[S, T, A, B, FixedTraversal[3]]:
      type X = EmptyTuple
      def to: S => (A, A, A) = s => (a(s), b(s), c(s))
      def from: FixedTraversal[3][X, B] => T =
        case (b0, b1, b2) => reverse(b0, b1, b2)


  def four[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      d: S => A,
      reverse: (B, B, B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[4]] =
    new Optic[S, T, A, B, FixedTraversal[4]]:
      type X = Unit
      def to: S => (A, A, A, A) = s => (a(s), b(s), c(s), d(s))
      def from: FixedTraversal[4][X, B] => T =
        case (b0, b1, b2, b3) => reverse(b0, b1, b2, b3)
