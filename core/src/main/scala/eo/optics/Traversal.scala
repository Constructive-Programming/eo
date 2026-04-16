package eo
package optics

import data.{FixedTraversal, Forget, PowerSeries, Vect}

import cats.{Applicative, MonoidK, Traverse}

object Traversal:

  /** `each` on a `Traverse` container, using `Forget[T]` as the carrier.
    *
    * Stable API — the law / behaviour / benchmark suites all depend on
    * this three-type-param signature. The PowerSeries-based variant
    * introduced on the `unthreaded` branch lives below as
    * [[powerEach]] / [[pPowerEach]], so both co-exist.
    */
  def each[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, Forget[T]] =
    new Optic[T[A], T[B], A, B, Forget[T]]:
      type X = Nothing
      def to: T[A] => T[A] = identity
      def from: T[B] => T[B] = identity

  /** PowerSeries-carrier `each`, imported from the `unthreaded`
    * exploration. Requires `Applicative` and `MonoidK` on `T` in
    * addition to `Traverse` so the `from` direction can fold the
    * vector back into the container via `foldMapK`. See
    * tests/src/test/scala/eo/Unthreaded.scala for a worked example.
    */
  def powerEach[T[_]: Traverse: Applicative: MonoidK, A]
      : Optic[T[A], T[A], A, A, PowerSeries] =
    pPowerEach[T, A, A]

  def pPowerEach[T[_]: Traverse: Applicative: MonoidK, A, B]
      : Optic[T[A], T[B], A, B, PowerSeries] =
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
