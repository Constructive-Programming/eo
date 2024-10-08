package eo
package optics

import cats.syntax.arrow._

object Lens:
  import Function.uncurried

  given tupleInterchangeable[A, B]: (((A, B)) => (B, A)) with
    def apply(t: (A, B)): (B, A) = t.swap

  def apply[S, A](get: S => A, replace: (S, A) => S) =
    pLens[S, S, A, A](get, replace)

  def pLens[S, T, A, B](get: S => A, replace: (S, B) => T) =
    new Optic[S, T, A, B, Tuple2]:
      type X = S
      def to: S => (S, A) = identity[S] &&& get
      def from: ((S, B)) => T = replace.tupled

  def curried[S, A](get: S => A, replace: A => S => S) =
   apply(get, (s, a) => replace(a)(s))

  def pCurried[S, T, A, B](get: S => A, replace: B => S => T) =
   pLens(get, uncurried(replace))

  def first[A, B] =
    new Optic[(A, B), (A, B), A, A, Tuple2]:
      type X = B
      def to: ((A, B)) => (X, A) = _.swap
      def from: ((X, A)) => (A, B) = _.swap

  def second[A, B] =
    new Optic[(A, B), (A, B), B, B, Tuple2]:
      type X = A
      def to: ((A, B)) => (X, B) = identity
      def from: ((X, B)) => (A, B) = identity
