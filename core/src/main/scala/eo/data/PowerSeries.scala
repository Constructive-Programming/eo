package eo
package data

import scala.annotation.nowarn

import cats.Applicative
import cats.instances.vector.given
import cats.syntax.functor.*
import cats.syntax.traverse.*

import optics.Optic

/** Carrier for the `PowerSeries`-style `Traversal`: pairs an existential leftover `Snd[A]` with a
  * flat `Vector[B]` of focused elements.
  *
  * The design goal is **downstream optic composition through a traversal**:
  * `traversal.andThen(lens)` only type-checks when the carrier admits a meaningful
  * `AssociativeFunctor`, and `Forget[F]` (the `each` carrier) does not. `PowerSeries` provides
  * that instance — see [[PowerSeries.assoc]].
  *
  * The `associateLeft` / `associateRight` fold loops below use a `VectorBuilder` to accumulate
  * results in amortised-constant append and freeze to `Vector` at publish time; both legs run
  * in **linear time** in the total number of elements. The per-slice cost in `associateRight`
  * is O(log n) for `Vector.slice`, making the full assoc chain O(N log n) rather than the
  * O(N²) the earlier `Vect`-based implementation paid for persistent concat.
  *
  * See `benchmarks/src/main/scala/eo/bench/PowerSeriesBench.scala` for the runtime profile.
  */
class PowerSeries[A, B](val ps: Tuple2[Snd[A], Vector[B]]) extends AnyVal:
  override def toString(): String = ps.toString()

/** Typeclass instances for [[PowerSeries]]. */
object PowerSeries:

  def unapply[A, B](ps: PowerSeries[A, B]): Tuple2[Snd[A], Vector[B]] =
    ps.ps

  given map: ForgetfulFunctor[PowerSeries] with
    def map[X, A, B](psa: PowerSeries[X, A], f: A => B): PowerSeries[X, B] =
      PowerSeries(psa.ps._1 -> psa.ps._2.map(f))

  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]
        : PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      psa => f => psa.ps._2.traverse(f).map(vb => PowerSeries(psa.ps._1 -> vb))

  given assoc[X, Y]: AssociativeFunctor[PowerSeries, X, Y] with
    type SndZ = (Snd[X], Vector[(Int, Snd[Y])])
    type Z    = (Int, SndZ)

    // `associateLeft` composes N per-outer steps, each contributing a
    // variable-size inner chunk. `VectorBuilder` is amortised O(1) per
    // append — total work O(N_total).
    def associateLeft[S, A, C]
        : (S, S => PowerSeries[X, A], A => PowerSeries[Y, C]) => PowerSeries[Z, C] =
      (s, f, g) =>
        val (x, va)  = f(s).ps
        val indexBld = Vector.newBuilder[(Int, Snd[Y])]
        val flatBld  = Vector.newBuilder[C]
        var i        = 0
        while i < va.length do
          val (y, vy) = g(va(i)).ps
          indexBld   += ((vy.length, y))
          flatBld   ++= vy
          i          += 1
        PowerSeries((x, indexBld.result()) -> flatBld.result())

    // `associateRight` walks the outer index list and hands each inner
    // chunk to `g`. `Vector.slice` is O(log n) per slice; total slice
    // work is O(N log n).
    def associateRight[D, B, T]
        : (PowerSeries[Z, D], PowerSeries[Y, D] => B, PowerSeries[X, B] => T) => T =
      (ps, g, f) =>
        val ((x, ys), vys) = ps.ps
        val resultBld      = Vector.newBuilder[B]
        var offset         = 0
        var i              = 0
        while i < ys.length do
          val (len, y) = ys(i)
          val chunk    = vys.slice(offset, offset + len)
          resultBld   += g(PowerSeries(y -> chunk))
          offset      += len
          i           += 1
        f(PowerSeries(x -> resultBld.result()))

  given tuple2ps: Composer[Tuple2, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, o.X)

        def to: S => PowerSeries[X, A] =
          s => PowerSeries(o.to(s).fmap(Vector(_)))

        def from: PowerSeries[X, B] => T =
          ps => o.from(ps.ps._1 -> ps.ps._2.head)

  given either2ps: Composer[Either, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Option[o.X])

        def to: S => PowerSeries[X, A] =
          s =>
            o.to(s)
              .fold(
                x => PowerSeries(Some(x) -> Vector.empty[A]),
                a => PowerSeries(None    -> Vector(a)),
              )

        @nowarn("msg=cannot be checked at runtime")
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Some(x), _) => o.from(Left(x))
          case PowerSeries(_, vec)     => o.from(Right(vec.head))

  given affine2ps: Composer[Affine, PowerSeries] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Either[Fst[o.X], Snd[o.X]])

        def to: S => PowerSeries[X, A] =
          s =>
            o.to(s)
              .affine
              .fold(
                x0     => PowerSeries(Left(x0)  -> Vector.empty[A]),
                (x1, b) => PowerSeries(Right(x1) -> Vector(b)),
              )

        @nowarn("msg=cannot be checked at runtime")
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Left(fx), _)    => o.from(Affine.ofLeft(fx))
          case PowerSeries(Right(sx), vec) => o.from(Affine.ofRight(sx -> vec.head))
