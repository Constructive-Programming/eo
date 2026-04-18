package eo
package optics

import data.{FixedTraversal, Forget, PowerSeries, Vect}

import cats.{Applicative, MonoidK, Traverse}

/** Constructors for `Traversal` — the multi-focus optic that modifies every element of a
  * traversable container. Two carriers coexist:
  *
  *   - [[each]] uses `Forget[T]` — linear time, no intermediate representation, the fast path for
  *     "map over every element".
  *   - [[powerEach]] / [[pPowerEach]] use `PowerSeries` — supports downstream composition with
  *     [[Lens]] / [[Prism]] through the shared `PowerSeries` carrier, but pays a super-linear cost
  *     relative to `each` (see `benchmarks/PowerSeriesBench`).
  *
  * Use `each` by default; reach for `powerEach` when you need to extend the chain past the
  * traversal (`traversal.andThen(leafLens)`).
  *
  * The [[two]] / [[three]] / [[four]] fixed-arity variants expose a Traversal over a fixed number
  * of per-element getters — useful for "every element of this record that happens to share a type"
  * style fixtures.
  */
object Traversal:

  /** `each` on a `Traverse` container, using `Forget[T]` as the carrier.
    *
    * Linear-time, no downstream composition beyond the focus. The law / behaviour / benchmark
    * suites depend on this exact three-type-param signature — do not refactor without updating all
    * three.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * import cats.instances.list.given
    * import eo.optics.Optic.*
    * val listEach = Traversal.each[List, Int, Int]
    * listEach.modify(_ + 1)(List(1, 2, 3))   // List(2, 3, 4)
    *   }}}
    */
  def each[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, Forget[T]] =
    new Optic[T[A], T[B], A, B, Forget[T]]:
      type X = Nothing
      def to: T[A] => T[A] = identity
      def from: T[B] => T[B] = identity

  /** PowerSeries-carrier `each` — enables downstream optic composition but pays the
    * [[data.PowerSeries]] machinery cost (~O(n²) vs `each`'s O(n)). Require `Applicative` and
    * `MonoidK` on `T` in addition to `Traverse` so the `from` direction can fold the per-index
    * [[data.Vect]] back into the container via `foldMapK`.
    *
    * Reach for this when you need to `.andThen` a lens or prism after the traversal; for plain
    * element-wise modify use [[each]].
    *
    * @group Constructors
    */
  def powerEach[T[_]: Traverse: Applicative: MonoidK, A]: Optic[T[A], T[A], A, A, PowerSeries] =
    pPowerEach[T, A, A]

  /** Polymorphic counterpart to [[powerEach]] — allows the focus to change type along the
    * traversal.
    *
    * @group Constructors
    */
  def pPowerEach[T[_]: Traverse: Applicative: MonoidK, A, B]: Optic[T[A], T[B], A, B, PowerSeries] =
    new Optic[T[A], T[B], A, B, PowerSeries]:
      type X = (Int, Unit)

      def to: T[A] => PowerSeries[X, A] =
        ta =>
          PowerSeries(
            () -> Traverse[T].foldLeft(ta, Vect.nil[Int, A])((v, a) => (v :+ a).asInstanceOf)
          )

      def from: PowerSeries[X, B] => T[B] =
        ps => Vect.trav[Int].foldMapK(ps.ps._2)(Applicative[T].pure[B])

  /** Traversal over exactly two per-element getters. `reverse` reassembles the `T` from two
    * modified `B`s.
    *
    * @group Constructors
    */
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

  /** Fixed-arity-3 traversal. See [[two]].
    *
    * @group Constructors
    */
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

  /** Fixed-arity-4 traversal. See [[two]].
    *
    * @group Constructors
    */
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
