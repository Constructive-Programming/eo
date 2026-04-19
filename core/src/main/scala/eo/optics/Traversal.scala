package eo
package optics

import data.{FixedTraversal, Forget, ObjArrBuilder, PowerSeries}

import cats.{Applicative, MonoidK, Traverse}
import cats.syntax.semigroupk.*

/** Constructors for `Traversal` — the multi-focus optic that modifies every element of a
  * traversable container. Two carriers coexist:
  *
  *   - [[each]] / [[pEach]] use `PowerSeries` — the default. Supports downstream composition
  *     with [[Lens]] / [[Prism]] through the shared `PowerSeries` carrier. Pays a super-linear
  *     cost relative to [[forEach]] (see `benchmarks/PowerSeriesBench`), but the composition
  *     story is the whole point of having a Traversal in the first place, so `each` is named
  *     to match what Scala users reach for intuitively.
  *   - [[forEach]] uses `Forget[T]` — a map-only fast path. Identity-shaped carrier, linear
  *     time, no downstream composition. Use when the chain terminates at the traversal.
  *
  * Scala naming convention: `each` is the "power" tool that composes; `forEach` is the
  * "just map" escape hatch. If you ever find yourself writing `Traversal.each.something`
  * and wishing it were faster, check whether the chain actually continues past the
  * traversal — if not, [[forEach]] is the cheaper choice.
  *
  * The [[two]] / [[three]] / [[four]] fixed-arity variants expose a Traversal over a fixed number
  * of per-element getters — useful for "every element of this record that happens to share a type"
  * style fixtures.
  */
object Traversal:

  /** Map-only traversal over a `Traverse` container, using `Forget[T]` as the carrier.
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
    * val listForEach = Traversal.forEach[List, Int, Int]
    * listForEach.modify(_ + 1)(List(1, 2, 3))   // List(2, 3, 4)
    *   }}}
    */
  def forEach[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, Forget[T]] =
    new Optic[T[A], T[B], A, B, Forget[T]]:
      type X = Nothing
      val to: T[A] => T[A] = identity
      val from: T[B] => T[B] = identity

  /** `each` — the composable traversal, built on the `PowerSeries` carrier. Enables
    * downstream optic composition via the `PowerSeries` `AssociativeFunctor`.
    *
    * Reach for this when the chain continues past the traversal (`.andThen(lens)` etc.);
    * if the chain terminates, prefer [[forEach]] for the map-only fast path.
    *
    * @group Constructors
    */
  def each[T[_]: Traverse: Applicative: MonoidK, A]: Optic[T[A], T[A], A, A, PowerSeries] =
    pEach[T, A, A]

  /** Polymorphic counterpart to [[each]] — allows the focus to change type along the
    * traversal.
    *
    * @group Constructors
    */
  def pEach[T[_]: Traverse: Applicative: MonoidK, A, B]: Optic[T[A], T[B], A, B, PowerSeries] =
    new Optic[T[A], T[B], A, B, PowerSeries]:
      type X = (Int, Unit)

      val to: T[A] => PowerSeries[X, A] = ta =>
        val buf = new ObjArrBuilder()
        Traverse[T].foldLeft(ta, ())((_, a) => { buf.append(a.asInstanceOf[AnyRef]); () })
        PowerSeries((), buf.freezeAsPSVec[A])

      val from: PowerSeries[X, B] => T[B] = ps =>
        val vec   = ps.vs
        val n     = vec.length
        val pure  = Applicative[T].pure[B](_)
        val empty = MonoidK[T].empty[B]
        var acc   = empty
        var i     = 0
        while i < n do
          acc = acc <+> pure(vec(i))
          i += 1
        acc

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
      val to: S => (A, A, Unit) = s => (a(s), b(s), ())

      val from: FixedTraversal[2][X, B] => T = {
        case (b0, b1, _) => reverse(b0, b1)
      }

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
      val to: S => (A, A, A, Unit) = s => (a(s), b(s), c(s), ())

      val from: FixedTraversal[3][X, B] => T = {
        case (b0, b1, b2, _) => reverse(b0, b1, b2)
      }

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
      val to: S => (A, A, A, A, Unit) = s => (a(s), b(s), c(s), d(s), ())

      val from: FixedTraversal[4][X, B] => T = {
        case (b0, b1, b2, b3, _) => reverse(b0, b1, b2, b3)
      }
