package dev.constructive.eo
package kernel

import scala.annotation.tailrec

import kyo.Maybe.{Absent, Present}
import kyo.{Chunk, Maybe}

/** Minimal typeclass kernel for the kyo port of cats-eo.
  *
  * kyo ships no typeclass hierarchy (it is direct-style by design), so the handful of typeclasses
  * core actually consumes are vendored here 1:1 with the cats shapes the code was written against.
  * Data types come from kyo-data instead: `Maybe` (partial reads), `Result` (the Prism carrier),
  * `Chunk` (multi-focus container). Only the methods core calls exist — this is not a general FP
  * library.
  */

trait Monoid[M]:
  def empty: M
  def combine(l: M, r: M): M

object Monoid:
  inline def apply[M](using M: Monoid[M]): Monoid[M] = M

  def instance[M](e: M, c: (M, M) => M): Monoid[M] = new Monoid[M]:
    def empty: M = e
    def combine(l: M, r: M): M = c(l, r)

  given intMonoid: Monoid[Int] with
    def empty: Int = 0
    def combine(l: Int, r: Int): Int = l + r

  given listMonoid[A]: Monoid[List[A]] with
    def empty: List[A] = Nil
    def combine(l: List[A], r: List[A]): List[A] = l ::: r

trait Invariant[F[_]]:
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]

trait Functor[F[_]] extends Invariant[F]:
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B] = map(fa)(f)
  def tupleLeft[A, B](fa: F[A], b: B): F[(B, A)] = map(fa)(a => (b, a))

trait Applicative[F[_]] extends Functor[F]:
  def pure[A](a: A): F[A]
  def map2[A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]

trait FlatMap[F[_]]:
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

trait Foldable[F[_]]:
  def foldLeft[A, B](fa: F[A], b: B)(f: (B, A) => B): B

  def foldMap[A, M](fa: F[A])(f: A => M)(using M: Monoid[M]): M =
    foldLeft(fa, M.empty)((m, a) => M.combine(m, f(a)))

  def size[A](fa: F[A]): Long = foldLeft(fa, 0L)((n, _) => n + 1)

  def reduceLeftToOption[A, B](fa: F[A])(f: A => B)(g: (B, A) => B): Option[B] =
    foldLeft(fa, Option.empty[B]):
      case (None, a)    => Some(f(a))
      case (Some(b), a) => Some(g(b, a))

trait Traverse[F[_]] extends Functor[F], Foldable[F]:
  def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]
  def mapAccumulate[S, A, B](init: S, fa: F[A])(f: (S, A) => (S, B)): (S, F[B])
  def map[A, B](fa: F[A])(f: A => B): F[B] = mapAccumulate((), fa)((_, a) => ((), f(a)))._2

trait MonoidK[F[_]]:
  def empty[A]: F[A]

trait Distributive[G[_]] extends Functor[G]:
  def distribute[F[_]: Functor, A, B](fa: F[A])(f: A => G[B]): G[F[B]]

trait Representable[F[_]]:
  type Representation
  def index[A](fa: F[A]): Representation => A
  def tabulate[A](f: Representation => A): F[A]

trait Profunctor[P[_, _]]:
  def dimap[A, B, C, D](p: P[A, B])(f: C => A)(g: B => D): P[C, D]

/** Container instances, exported into each typeclass companion below so implicit scope finds them
  * without imports (the same reason eo's carrier instances live on carrier companions).
  */
object instances:

  given listInstance: (Traverse[List] & Applicative[List] & MonoidK[List] & FlatMap[List]) =
    new Traverse[List] with Applicative[List] with MonoidK[List] with FlatMap[List]:
      override def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
      def foldLeft[A, B](fa: List[A], b: B)(f: (B, A) => B): B = fa.foldLeft(b)(f)
      def pure[A](a: A): List[A] = a :: Nil
      def empty[A]: List[A] = Nil
      def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] = fa.flatMap(f)
      def map2[A, B, C](fa: List[A], fb: List[B])(f: (A, B) => C): List[C] =
        fa.flatMap(a => fb.map(b => f(a, b)))
      def traverse[G[_], A, B](fa: List[A])(f: A => G[B])(using G: Applicative[G]): G[List[B]] =
        fa.foldRight(G.pure(List.empty[B]))((a, acc) => G.map2(f(a), acc)(_ :: _))
      def mapAccumulate[S, A, B](init: S, fa: List[A])(f: (S, A) => (S, B)): (S, List[B]) =
        @tailrec def loop(s: S, rest: List[A], acc: List[B]): (S, List[B]) =
          rest match
            case Nil    => (s, acc.reverse)
            case h :: t =>
              val (s2, b) = f(s, h)
              loop(s2, t, b :: acc)
        loop(init, fa, Nil)

  given vectorInstance: (Traverse[Vector] & Applicative[Vector] & MonoidK[Vector]) =
    new Traverse[Vector] with Applicative[Vector] with MonoidK[Vector]:
      override def map[A, B](fa: Vector[A])(f: A => B): Vector[B] = fa.map(f)
      def foldLeft[A, B](fa: Vector[A], b: B)(f: (B, A) => B): B = fa.foldLeft(b)(f)
      def pure[A](a: A): Vector[A] = Vector(a)
      def empty[A]: Vector[A] = Vector.empty
      def map2[A, B, C](fa: Vector[A], fb: Vector[B])(f: (A, B) => C): Vector[C] =
        fa.flatMap(a => fb.map(b => f(a, b)))
      def traverse[G[_], A, B](fa: Vector[A])(f: A => G[B])(using G: Applicative[G]): G[Vector[B]] =
        fa.foldLeft(G.pure(Vector.empty[B]))((acc, a) => G.map2(acc, f(a))(_ :+ _))
      def mapAccumulate[S, A, B](init: S, fa: Vector[A])(f: (S, A) => (S, B)): (S, Vector[B]) =
        val b = Vector.newBuilder[B]
        val s = fa.foldLeft(init): (s, a) =>
          val (s2, out) = f(s, a)
          b += out
          s2
        (s, b.result())

  given optionInstance
      : (Traverse[Option] & Applicative[Option] & MonoidK[Option] & FlatMap[Option]) =
    new Traverse[Option] with Applicative[Option] with MonoidK[Option] with FlatMap[Option]:
      override def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
      def foldLeft[A, B](fa: Option[A], b: B)(f: (B, A) => B): B = fa.fold(b)(a => f(b, a))
      def pure[A](a: A): Option[A] = Some(a)
      def empty[A]: Option[A] = None
      def flatMap[A, B](fa: Option[A])(f: A => Option[B]): Option[B] = fa.flatMap(f)
      def map2[A, B, C](fa: Option[A], fb: Option[B])(f: (A, B) => C): Option[C] =
        fa.flatMap(a => fb.map(b => f(a, b)))
      def traverse[G[_], A, B](fa: Option[A])(f: A => G[B])(using G: Applicative[G]): G[Option[B]] =
        fa.fold(G.pure(Option.empty[B]))(a => G.map(f(a))(Some(_)))
      def mapAccumulate[S, A, B](init: S, fa: Option[A])(f: (S, A) => (S, B)): (S, Option[B]) =
        fa.fold((init, Option.empty[B])): a =>
          val (s2, b) = f(init, a)
          (s2, Some(b))

  given maybeInstance: (Traverse[Maybe] & Applicative[Maybe] & MonoidK[Maybe] & FlatMap[Maybe]) =
    new Traverse[Maybe] with Applicative[Maybe] with MonoidK[Maybe] with FlatMap[Maybe]:
      override def map[A, B](fa: Maybe[A])(f: A => B): Maybe[B] = fa.map(f)
      def foldLeft[A, B](fa: Maybe[A], b: B)(f: (B, A) => B): B = fa.fold(b)(a => f(b, a))
      def pure[A](a: A): Maybe[A] = Present(a)
      def empty[A]: Maybe[A] = Absent
      def flatMap[A, B](fa: Maybe[A])(f: A => Maybe[B]): Maybe[B] = fa.flatMap(f)
      def map2[A, B, C](fa: Maybe[A], fb: Maybe[B])(f: (A, B) => C): Maybe[C] =
        fa.flatMap(a => fb.map(b => f(a, b)))
      def traverse[G[_], A, B](fa: Maybe[A])(f: A => G[B])(using G: Applicative[G]): G[Maybe[B]] =
        fa.fold(G.pure(Maybe.empty[B]))(a => G.map(f(a))(Present(_)))
      def mapAccumulate[S, A, B](init: S, fa: Maybe[A])(f: (S, A) => (S, B)): (S, Maybe[B]) =
        fa.fold((init, Maybe.empty[B])): a =>
          val (s2, b) = f(init, a)
          (s2, Present(b))

  given chunkInstance: (Traverse[Chunk] & Applicative[Chunk] & MonoidK[Chunk]) =
    new Traverse[Chunk] with Applicative[Chunk] with MonoidK[Chunk]:
      override def map[A, B](fa: Chunk[A])(f: A => B): Chunk[B] = Chunk.from(fa.map(f))
      def foldLeft[A, B](fa: Chunk[A], b: B)(f: (B, A) => B): B = fa.foldLeft(b)(f)
      def pure[A](a: A): Chunk[A] = Chunk(a)
      def empty[A]: Chunk[A] = Chunk.empty
      def map2[A, B, C](fa: Chunk[A], fb: Chunk[B])(f: (A, B) => C): Chunk[C] =
        Chunk.from(fa.flatMap(a => fb.map(b => f(a, b))))
      def traverse[G[_], A, B](fa: Chunk[A])(f: A => G[B])(using G: Applicative[G]): G[Chunk[B]] =
        G.map(fa.foldLeft(G.pure(List.empty[B]))((acc, a) => G.map2(acc, f(a))((l, b) => b :: l)))(
          l => Chunk.from(l.reverse)
        )
      def mapAccumulate[S, A, B](init: S, fa: Chunk[A])(f: (S, A) => (S, B)): (S, Chunk[B]) =
        val b = List.newBuilder[B]
        val s = fa.foldLeft(init): (s, a) =>
          val (s2, out) = f(s, a)
          b += out
          s2
        (s, Chunk.from(b.result()))

  given function1Functor[R]: Functor[[A] =>> R => A] with
    def map[A, B](fa: R => A)(f: A => B): R => B = fa.andThen(f)

object Invariant:
  inline def apply[F[_]](using F: Invariant[F]): Invariant[F] = F

object Functor:
  inline def apply[F[_]](using F: Functor[F]): Functor[F] = F
  export instances.given

object Applicative:
  inline def apply[F[_]](using F: Applicative[F]): Applicative[F] = F
  export instances.given

object FlatMap:
  inline def apply[F[_]](using F: FlatMap[F]): FlatMap[F] = F
  export instances.given

object Foldable:
  inline def apply[F[_]](using F: Foldable[F]): Foldable[F] = F
  export instances.given

object Traverse:
  inline def apply[F[_]](using F: Traverse[F]): Traverse[F] = F
  export instances.given

object MonoidK:
  inline def apply[F[_]](using F: MonoidK[F]): MonoidK[F] = F
  export instances.given

object Distributive:
  inline def apply[G[_]](using G: Distributive[G]): Distributive[G] = G
