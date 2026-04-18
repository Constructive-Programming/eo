package eo
package data

import cats.Applicative
import cats.syntax.traverse._
import cats.syntax.applicative._
import cats.syntax.bifunctor._
import cats.syntax.either._
import cats.syntax.functor._

type Fst[T] = T match
  case (f, s) => f

type Snd[T] = T match
  case (f, s) => s

class Affine[A, B](val affine: Either[Fst[A], (Snd[A], B)]) extends AnyVal:
  import Affine.*

  def aFold[C](f: Fst[A] => Either[Fst[A], (Snd[A], C)],
              g: ((Snd[A], B)) => Either[Fst[A], (Snd[A], C)]): Affine[A, C] =
    Affine(affine.fold(f, g))

  def aTraverse[C, G[_]: Applicative](f: B => G[C]): G[Affine[A, C]] =
    affine.fold(ofLeft(_).pure[G], _.traverse(f).map(ofRight))

object Affine:
  extension [X <: Tuple, B](e: Either[Fst[X], (Snd[X], B)])
    def affine: Affine[X, B] = Affine(e)

  def ofLeft[X, B](l: Fst[X]): Affine[X, B] =
    Affine[X, B](l.asLeft[(Snd[X], B)])

  def ofRight[X, B](r: (Snd[X], B)): Affine[X, B] =
    Affine[X, B](r.asRight[Fst[X]])

  given map: ForgetfulFunctor[Affine] with
    def map[X, A, B](fa: Affine[X, A], f: A => B): Affine[X, B] =
      fa.aFold[B](_.asLeft[(Snd[X], B)], _.map(f).asRight[Fst[X]])

  given traverse: ForgetfulTraverse[Affine, Applicative] with
    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.aTraverse(f)

  /** Composition functor for `Affine` carriers.
    *
    * The type parameters `X` and `Y` are **deliberately unbounded**.
    * Affine's internals use `Fst[X]` / `Snd[X]` match types, which
    * stay inert (they do not reduce, but do not error either) when
    * `X` is not a `Tuple`; this makes the instance sound for all
    * concrete optic constructors we ship (`Optional.apply` sets
    * `type X = (T, S)`; `Composer[Tuple2, Affine].to` sets
    * `type X = (T, o.X)`; `Composer[Either, Affine].to` sets
    * `type X = (o.X, S)` — every concrete X is a Tuple2).
    *
    * Prior to 0.1.0 this given carried `X <: Tuple, Y <: Tuple`.
    * The bound was load-bearing *defensively* — it prevented
    * pathological composition over an Affine whose X isn't a
    * tuple — but it also blocked legitimate `Lens.andThen(Optional)`
    * because the post-`morph[Affine]` existential is abstract and
    * Scala could not prove the bound through the Optic trait's
    * unbounded `F[_, _]` slot. Dropping the bound preserves
    * composition at the cost of the defensive guard.
    *
    * **Future work — `ValidCarrier[F, X]` witness.** A cleaner
    * long-term story is to thread a `ValidCarrier[F[_, _], X]`
    * typeclass through every optic operation, so each carrier can
    * declare which existentials it admits (`ValidCarrier[Affine, X]`
    * requires `X <: Tuple`; `ValidCarrier[Tuple2, X]` is universal).
    * This lives the constraint at the call-site rather than at
    * the class, and avoids the kind-compatibility wall that
    * hits any attempt to put the bound on `Affine` itself
    * (`class Affine[A <: Tuple, B]` produces a kind mismatch when
    * Affine is passed to `Optic[…, F[_, _]]`). Tracked for a
    * post-0.1.0 release when the public API is stable enough to
    * absorb the witness threading. */
  given assoc[X, Y]: AssociativeFunctor[Affine, X, Y] with
    type Z = (Either[Fst[X], (Snd[X], Fst[Y])], (Snd[X], Snd[Y]))

    def associateLeft[S, A, C]: (S, S => Affine[X, A], A => Affine[Y, C]) => Affine[Z, C] =
      case (s, f, g) =>
        inline def fLeft(x: Fst[X]): Affine[Z, C] =
          ofLeft(x.asLeft[(Snd[X], Fst[Y])])
        inline def fRight(xa: (Snd[X], A)): Affine[Z, C] =
          g(xa._2).affine.fold(gLeft(xa._1), gRight(xa._1))
        inline def gLeft(x1: Snd[X])(y0: Fst[Y]): Affine[Z, C] =
          ofLeft((x1, y0).asRight[(Fst[X])])
        inline def gRight(x1: Snd[X])(yc: (Snd[Y], C)): Affine[Z, C] =
          ofRight((x1, yc._1) -> yc._2)
        f(s).affine.fold(fLeft, fRight)

    def associateRight[D, B, T]: (Affine[Z, D], Affine[Y, D] => B, Affine[X, B] => T) => T =
      case (az, g, f) =>
        inline def zLeft(z: Either[Fst[X], (Snd[X], Fst[Y])]): T =
          z.fold(yLeft, yRight)
        inline def zRight(z: ((Snd[X], Snd[Y]), D)): T =
          val b: B = g(ofRight(z._1._2 -> z._2))
          f(ofRight(z._1._1 -> b))
        inline def yLeft(y: Fst[X]): T = f(ofLeft(y))
        inline def yRight(y: (Snd[X], Fst[Y])): T =
          val b: B = g(ofLeft(y._2))
          f(ofRight(y._1 -> b))
        az.affine.fold(zLeft, zRight)

  given tuple2affine: Composer[Tuple2, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (T, o.X)
        def to: S => Affine[X, A] = s => Affine(Right(o.to(s)))

        def from: Affine[X, B] => T = _.affine match
          case Left(t)  => t
          case Right(p) => o.from(p)

  given either2affine: Composer[Either, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (o.X, S)
        def to: S => Affine[X, A] = s => o.to(s).map(s -> _).affine
        def from: Affine[X, B] => T = xb => o.from(xb.affine.map(_._2))
