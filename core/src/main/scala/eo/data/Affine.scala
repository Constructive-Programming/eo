package eo
package data

import cats.Applicative
import cats.syntax.traverse._
import cats.syntax.applicative._
import cats.syntax.bifunctor._
import cats.syntax.either._
import cats.syntax.functor._

import optics.Optic

/** Extract the first element type of a `Tuple2`. Stays as an unreduced match type when `T` is not a
  * `Tuple2` — this is load bearing for [[Affine.assoc]] accepting unbounded existentials.
  */
type Fst[T] = T match
  case (f, s) => f

/** Extract the second element type of a `Tuple2`. See [[Fst]]. */
type Snd[T] = T match
  case (f, s) => s

/** Carrier for the `Optional` family of optics: `Affine[A, B]` encodes `Either[Fst[A], (Snd[A],
  * B)]` — either a miss branch carrying the original-shape value (`Fst[A]`), or a hit branch
  * carrying a leftover (`Snd[A]`) plus the focus (`B`).
  *
  * At every constructor site the existential `A` is instantiated to a concrete `Tuple2` (so
  * `Fst[A]` / `Snd[A]` reduce); when Affine is carried through the existential position of an
  * `Optic[…, Affine]` value, `A` becomes abstract and the match types stay inert. Both uses are
  * supported.
  *
  * @tparam A
  *   existential leftover tuple — the `Tuple2` encoding of the miss and hit contexts
  * @tparam B
  *   focus type the caller reads / writes
  */
class Affine[A, B](val affine: Either[Fst[A], (Snd[A], B)]) extends AnyVal:
  import Affine.*

  /** Monoidally fold both branches of the underlying `Either`, producing a new `Affine[A, C]` over
    * a different focus type.
    */
  def aFold[C](
      f: Fst[A] => Either[Fst[A], (Snd[A], C)],
      g: ((Snd[A], B)) => Either[Fst[A], (Snd[A], C)],
  ): Affine[A, C] =
    Affine(affine.fold(f, g))

  /** Effectful traversal over the focus — runs `f` only on the hit branch, passes the miss branch
    * through unchanged.
    */
  def aTraverse[C, G[_]: Applicative](f: B => G[C]): G[Affine[A, C]] =
    affine.fold(ofLeft(_).pure[G], _.traverse(f).map(ofRight))

/** Constructors and typeclass instances for [[Affine]]. */
object Affine:

  /** Convenience wrapping extension — turns a raw `Either[Fst[X], (Snd[X], B)]` into an `Affine[X,
    * B]` without writing the constructor call.
    *
    * @group Instances
    */
  extension [X <: Tuple, B](e: Either[Fst[X], (Snd[X], B)]) def affine: Affine[X, B] = Affine(e)

  /** Miss-branch constructor — produce an `Affine[X, B]` whose branch is `Left(l)`.
    *
    * @group Constructors
    */
  def ofLeft[X, B](l: Fst[X]): Affine[X, B] =
    Affine[X, B](l.asLeft[(Snd[X], B)])

  /** Hit-branch constructor — produce an `Affine[X, B]` whose branch is `Right((xa, b))`.
    *
    * @group Constructors
    */
  def ofRight[X, B](r: (Snd[X], B)): Affine[X, B] =
    Affine[X, B](r.asRight[Fst[X]])

  /** `ForgetfulFunctor[Affine]` — maps the focus `B` through `f` while leaving the miss branch
    * untouched. Unlocks `.modify` and `.replace` on every Affine-carrier optic.
    *
    * @group Instances
    */
  given map: ForgetfulFunctor[Affine] with

    def map[X, A, B](fa: Affine[X, A], f: A => B): Affine[X, B] =
      fa.aFold[B](_.asLeft[(Snd[X], B)], _.map(f).asRight[Fst[X]])

  /** `ForgetfulTraverse[Affine, Applicative]` — lifts a focus-level `A => G[B]` into an `Affine[X,
    * A] => G[Affine[X, B]]`. Unlocks `.modifyA` / `.all` / `.modifyF` on Affine-carrier optics.
    *
    * @group Instances
    */
  given traverse: ForgetfulTraverse[Affine, Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: Affine[X, A] => (A => G[B]) => G[Affine[X, B]] =
      fa => f => fa.aTraverse(f)

  /** Composition functor for `Affine` carriers.
    *
    * The type parameters `X` and `Y` are **deliberately unbounded**. Affine's internals use
    * `Fst[X]` / `Snd[X]` match types, which stay inert (they do not reduce, but do not error
    * either) when `X` is not a `Tuple`; this makes the instance sound for all concrete optic
    * constructors we ship (`Optional.apply` sets `type X = (T, S)`; `Composer[Tuple2, Affine].to`
    * sets `type X = (T, o.X)`; `Composer[Either, Affine].to` sets `type X = (o.X, S)` — every
    * concrete X is a Tuple2).
    *
    * Prior to 0.1.0 this given carried `X <: Tuple, Y <: Tuple`. The bound was load-bearing
    * *defensively* — it prevented pathological composition over an Affine whose X isn't a tuple —
    * but it also blocked legitimate `Lens.andThen(Optional)` because the post-`morph[Affine]`
    * existential is abstract and Scala could not prove the bound through the Optic trait's
    * unbounded `F[_, _]` slot. Dropping the bound preserves composition at the cost of the
    * defensive guard.
    *
    * **Future work — `ValidCarrier[F, X]` witness.** A cleaner long-term story is to thread a
    * `ValidCarrier[F[_, _], X]` typeclass through every optic operation, so each carrier can
    * declare which existentials it admits (`ValidCarrier[Affine, X]` requires `X <: Tuple`;
    * `ValidCarrier[Tuple2, X]` is universal). This lives the constraint at the call-site rather
    * than at the class, and avoids the kind-compatibility wall that hits any attempt to put the
    * bound on `Affine` itself (`class Affine[A <: Tuple, B]` produces a kind mismatch when Affine
    * is passed to `Optic[…, F[_, _]]`). Tracked for a post-0.1.0 release when the public API is
    * stable enough to absorb the witness threading.
    *
    * @group Instances
    */
  given assoc[Xo, Xi]: AssociativeFunctor[Affine, Xo, Xi] with
    type Z = (Either[Fst[Xo], (Snd[Xo], Fst[Xi])], (Snd[Xo], Snd[Xi]))

    def composeTo[S, T, A, B, C, D](
        s:     S,
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
    ): Affine[Z, C] =
      inline def fLeft(x: Fst[Xo]): Affine[Z, C] =
        ofLeft(x.asLeft[(Snd[Xo], Fst[Xi])])
      inline def fRight(xa: (Snd[Xo], A)): Affine[Z, C] =
        inner.to(xa._2).affine.fold(gLeft(xa._1), gRight(xa._1))
      inline def gLeft(x1: Snd[Xo])(y0: Fst[Xi]): Affine[Z, C] =
        ofLeft((x1, y0).asRight[Fst[Xo]])
      inline def gRight(x1: Snd[Xo])(yc: (Snd[Xi], C)): Affine[Z, C] =
        ofRight((x1, yc._1) -> yc._2)
      outer.to(s).affine.fold(fLeft, fRight)

    def composeFrom[S, T, A, B, C, D](
        xd:    Affine[Z, D],
        inner: Optic[A, B, C, D, Affine] { type X = Xi },
        outer: Optic[S, T, A, B, Affine] { type X = Xo },
    ): T =
      inline def zLeft(z: Either[Fst[Xo], (Snd[Xo], Fst[Xi])]): T =
        z.fold(yLeft, yRight)
      inline def zRight(z: ((Snd[Xo], Snd[Xi]), D)): T =
        val b: B = inner.from(ofRight(z._1._2 -> z._2))
        outer.from(ofRight(z._1._1 -> b))
      inline def yLeft(y: Fst[Xo]): T = outer.from(ofLeft(y))
      inline def yRight(y: (Snd[Xo], Fst[Xi])): T =
        val b: B = inner.from(ofLeft(y._2))
        outer.from(ofRight(y._1 -> b))
      xd.affine.fold(zLeft, zRight)

  /** `Composer[Tuple2, Affine]` — lets a Lens be expressed as an Optional so
    * `lens.andThen(optional)` type-checks through cross-carrier `.andThen` (which summons a
    * `Morph[Tuple2, Affine]` whose resolution picks up this composer). The resulting
    * `Optic[…, Affine]` always takes the "Right" branch at read time: the Lens never fails.
    *
    * @group Instances
    */
  given tuple2affine: Composer[Tuple2, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (T, o.X)
        val to: S => Affine[X, A] = s => Affine(Right(o.to(s)))

        val from: Affine[X, B] => T = a => a.affine match
          case Left(t)  => t
          case Right(p) => o.from(p)

  /** `Composer[Either, Affine]` — express a Prism as an Optional by reusing its `Either`
    * decomposition for Affine's miss / hit branches.
    *
    * @group Instances
    */
  given either2affine: Composer[Either, Affine] with
    import optics.Optic

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, Affine] =
      new Optic[S, T, A, B, Affine]:
        type X = (o.X, S)
        val to: S => Affine[X, A] = s =>
          Affine(o.to(s) match
            case Right(a) => Right(s -> a)
            case Left(x)  => Left(x))
        val from: Affine[X, B] => T = xb => xb.affine match
          case Right((_, b)) => o.from(Right(b))
          case Left(x)       => o.from(Left(x))
