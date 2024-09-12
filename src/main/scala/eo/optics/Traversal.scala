package eo
package optics

import data.{FixedTraversal, Forget}

import cats.Traverse

object Traversal:

  def each[T[_]: Traverse, A]: Optic[T[A], T[A], A, A, Forget[T]] =
    pEach[T, A, A]

  def pEach[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, Forget[T]] =
    new Optic[T[A], T[B], A, B, Forget[T]]:
      type X = Nothing
      def to: T[A] => T[A] = identity
      def from: T[B] => T[B] = identity

  def two[S, T, A, B](
      a: S => A,
      b: S => A,
      reverse: (B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[2]] =
    new Optic[S, T, A, B, FixedTraversal[2]]:
      type X = Unit
      def to: S => (A, A, Unit) = s => (a(s), b(s), ())
      def from: FixedTraversal[2][X, B] => T =
        case (b0, b1, _) => reverse(b0, b1)

  def three[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      reverse: (B, B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[3]] =
    new Optic[S, T, A, B, FixedTraversal[3]]:
      type X = Unit
      def to: S => (A, A, A, Unit) = s => (a(s), b(s), c(s), ())
      def from: FixedTraversal[3][X, B] => T =
        case (b0, b1, b2, _) => reverse(b0, b1, b2)


  def four[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      d: S => A,
      reverse: (B, B, B, B) => T,
  ): Optic[S, T, A, B, FixedTraversal[4]] =
    new Optic[S, T, A, B, FixedTraversal[4]]:
      type X = Unit
      def to: S => (A, A, A, A, Unit) = s => (a(s), b(s), c(s), d(s), ())
      def from: FixedTraversal[4][X, B] => T =
        case (b0, b1, b2, b3, _) => reverse(b0, b1, b2, b3)
