package dev.constructive.eo
package chimney

import scala.annotation.tailrec

import _root_.io.scalaland.chimney.partial
import cats.{Eval, Foldable, Functor, FunctorFilter, Monad}

import optics.{ForgetFold, PickFold}

/** chimney's `partial.Result[A]` as the container of a [[optics.ForgetFold]]: what
  * `Chimney.resultFold` and `.toResultFold()` read into.
  *
  * An opaque alias, not a new type — the runtime value is chimney's own `partial.Result` — so its
  * companion can carry the cats instances a fold needs and core's `ReadCompose` finds them in
  * implicit scope with no import (the public cats instances for `partial.Result` belong to
  * chimney-cats; these are scoped to this alias). With them, a `ForgetFold[S, PartialResult, A]`
  * keeps chimney's errors through composition:
  *   - `andThen` a lens / iso / `Getter` maps the result (`Functor`);
  *   - `andThen` a prism / optional / `AffineFold` maps it too, an inner miss becoming chimney's
  *     "empty value" error (`FunctorFilter.mapFilter`, via `partial.Result.fromEmpty`);
  *   - `andThen` another `ForgetFold[A, PartialResult, C]` flat-maps, so both sides' errors surface
  *     (`Monad`).
  *
  * Multi-focus inners (Traversal, Fold over a collection) have no single result to keep and fall
  * back to core's `List` fold.
  */
opaque type PartialResult[A] = partial.Result[A]

object PartialResult:

  /** Wrap a chimney result (no runtime cost). */
  inline def apply[A](r: partial.Result[A]): PartialResult[A] = r

  extension [A](r: PartialResult[A])
    /** The chimney result (no runtime cost). */
    inline def toResult: partial.Result[A] = r

  given monad: Monad[PartialResult] with
    def pure[A](a: A): PartialResult[A] = partial.Result.Value(a)

    override def map[A, B](fa: PartialResult[A])(f: A => B): PartialResult[B] = fa.map(f)

    def flatMap[A, B](fa: PartialResult[A])(f: A => PartialResult[B]): PartialResult[B] =
      fa.flatMap(f)

    @tailrec
    def tailRecM[A, B](a: A)(f: A => PartialResult[Either[A, B]]): PartialResult[B] =
      f(a) match
        case partial.Result.Value(Left(next)) => tailRecM(next)(f)
        case partial.Result.Value(Right(b))   => partial.Result.Value(b)
        case e: partial.Result.Errors         => e

  given foldable: Foldable[PartialResult] with

    def foldLeft[A, B](fa: PartialResult[A], b: B)(f: (B, A) => B): B =
      fa match
        case partial.Result.Value(a)  => f(b, a)
        case _: partial.Result.Errors => b

    def foldRight[A, B](fa: PartialResult[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match
        case partial.Result.Value(a)  => f(a, lb)
        case _: partial.Result.Errors => lb

  given functorFilter: FunctorFilter[PartialResult] with
    def functor: Functor[PartialResult] = monad

    def mapFilter[A, B](fa: PartialResult[A])(f: A => Option[B]): PartialResult[B] =
      fa.flatMap(a => partial.Result.fromOption(f(a)))

// A fold into a chimney result reads like the `partial.Result` it holds. (Within this file the
// opaque alias is transparent, so `fold.read(s)` already is the `partial.Result`.)

extension [S, A](fold: ForgetFold[S, PartialResult, A])

  /** The focus or chimney's errors, with their field paths. */
  def result(s: S): partial.Result[A] = fold.read(s)

  /** The focus or the errors, as an `Either` — `result(s).asEither`. */
  def either(s: S): Either[partial.Result.Errors, A] = fold.read(s).asEither

  /** The focus, dropping the errors. */
  def getOption(s: S): Option[A] = fold.read(s).asOption

  /** The error-free view: a plain [[optics.PickFold]] with the same hits. */
  def affineFold: PickFold[S, A] = PickFold(getOption)
