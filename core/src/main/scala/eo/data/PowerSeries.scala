package eo
package data

import optics.Optic

import cats.Applicative
import cats.syntax.functor.*

import scala.runtime.Tuples
import scala.compiletime.ops.int.*

class PowerSeries[A, B](val ps: Tuple2[Snd[A], Vect[Int, B]]) extends AnyVal:
    override def toString(): String = ps.toString()

object PowerSeries:
  def unapply[A, B](ps: PowerSeries[A, B]): Tuple2[Snd[A], Vect[Int, B]] =
    ps.ps

  given map: ForgetfulFunctor[PowerSeries] with
    def map[X, A, B]: PowerSeries[X, A] => (A => B) => PowerSeries[X, B] =
      psa => f =>
         val v = psa.ps._2
         PowerSeries(psa.ps._1 -> Vect.functor[v.Size].map(v)(f))

  given traverse: ForgetfulTraverse[PowerSeries, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: PowerSeries[X, A] => (A => G[B]) => G[PowerSeries[X, B]] =
      psa => f =>
        val v = psa.ps._2
        val gvb = Vect.trav[v.Size].traverse(v)(f)
        gvb.map(vb => PowerSeries(psa.ps._1 -> vb))

  given assoc[X, Y]: AssociativeFunctor[PowerSeries, X, Y] with
    type SndZ = (Snd[X], Vect[Int, (Int, Snd[Y])])
    type Z = (Int, SndZ)

    def associateLeft[S, A, C]: (S, S => PowerSeries[X, A], A => PowerSeries[Y, C]) => PowerSeries[Z, C] =
      (s, f, g) => f(s).ps match
        case (x, va)  =>
          val init = PowerSeries[Z, C](((x, Vect.nil), Vect.nil))
          Vect.trav[va.Size].foldLeft(va, init) {
            case (PowerSeries((x, ys), vys), a) =>
              val (y, vy) = g(a).ps
              val nys: Vect[Int, (Int, Snd[Y])] = (ys :+ (vy.size, y)).asInstanceOf
              val nvys: Vect[Int, C] = (vys ++ vy).asInstanceOf
              PowerSeries((x, nys) -> nvys)
          }

    def associateRight[D, B, T]: (PowerSeries[Z, D], PowerSeries[Y, D] => B, PowerSeries[X, B] => T) => T =
      (ps, g, f) => ps match
        case PowerSeries((x, ys), vys) =>
          val (_, vxs) = Vect.trav[ys.Size].foldLeft(ys, 0 -> Vect.nil[Int, B]) {
            case ((offset, xs), (len, y)) =>
              val nvys: Vect[Int, B] =
                (xs :+ g(PowerSeries(y -> vys.slice(offset, len)))).asInstanceOf
              (offset + len) -> nvys
          }
          f(PowerSeries(x -> vxs))

  given tuple2ps: Composer[Tuple2, PowerSeries] with
    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, o.X)
        def to: S => PowerSeries[X, A] = s =>
          PowerSeries(o.to(s).fmap(Vect.of))
        def from: PowerSeries[X, B] => T =
          case PowerSeries(x, Vect.Head[1, B](b)) => o.from(x -> b)

  given either2ps: Composer[Either, PowerSeries] with
    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Option[o.X])
        def to: S => PowerSeries[X, A] =
          s => o.to(s).fold(
            x => PowerSeries(Some(x) -> Vect.nil),
            a => PowerSeries(None -> Vect.of(a))
          )
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Some(x), _) =>
            o.from(Left(x))
          case PowerSeries(_, Vect.Head[1, B](b)) =>
            o.from(Right(b))


  given affine2ps: Composer[Affine, PowerSeries] with
    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, PowerSeries] =
      new Optic[S, T, A, B, PowerSeries]:
        type X = (1, Either[Fst[o.X], Snd[o.X]])
        def to: S => PowerSeries[X, A] =
          s => o.to(s).affine.fold(
            x0 => PowerSeries(Left(x0) -> Vect.nil),
            (x1, b) => PowerSeries(Right(x1) -> Vect.of(b))
          )
        def from: PowerSeries[X, B] => T =
          case PowerSeries(Left(fx), _) =>
            o.from(Affine.ofLeft(fx))
          case PowerSeries(Right(sx), Vect.Head[1, B](b)) =>
            o.from(Affine.ofRight(sx -> b))
