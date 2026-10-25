package dev.constructive.eo
package chimney

import scala.annotation.targetName

import _root_.io.scalaland.chimney.partial
import cats.Monoid

import accessor.{Accessor, PartialAccessor}
import compose.ReadCompose
import data.Affine
import optics.{Getter, Optic, PickFold}

/** Every `ResultFold` miss is `Miss(())` — one shared instance serves all (the
  * [[dev.constructive.eo.optics.PickFold]] trick).
  */
private val resultFoldMiss: Affine.Miss[(Unit, Unit)] = new Affine.Miss(())

/** A read-only, 0-or-1-focus optic whose miss carries chimney's `partial.Result.Errors` — what a
  * `PartialTransformer[S, A]` becomes in eo (see [[Chimney.resultFold]] / `.toResultFold`).
  *
  * It is an ordinary affine fold to the rest of eo — `Optic[S, Unit, A, Unit, Affine]`, the
  * [[dev.constructive.eo.optics.PickFold]] shape, so `.getOption`, `.foldMap` and every read-only
  * composition work — plus [[result]], which says WHY a read missed: the `Errors`, with chimney's
  * field paths.
  *
  * '''Composition keeps the errors''' when the inner reads a single focus:
  *   - `andThen(ResultFold)` — `flatMap`s the results (an inner miss contributes its own errors);
  *   - `andThen(Getter)` and `andThen` any total single-focus optic (Lens, Iso — eo-generics'
  *     `lens[A](_.field)` included) — `map`s the result;
  *   - `andThen(PickFold)` and `andThen` any partial single-focus optic (Prism, Optional) — an
  *     inner miss becomes chimney's "empty value" error (`partial.Result.fromEmpty`).
  *
  * Multi-focus inners (Traversal, Fold) have no single result to keep: compose them on
  * [[affineFold]], the error-free view.
  */
final class ResultFold[S, A](val result: S => partial.Result[A])
    extends Optic[S, Unit, A, Unit, Affine],
      CanGetOption[S, A],
      CanFold[S, A]:
  type X = (Unit, Unit)

  def to(s: S): Affine[X, A] =
    result(s) match
      case partial.Result.Value(a)  => new Affine.Hit[X, A]((), a)
      case _: partial.Result.Errors => resultFoldMiss

  def from(a: Affine[X, Unit]): Unit = ()

  def getOption(s: S): Option[A] = result(s).asOption

  def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    result(s) match
      case partial.Result.Value(a)  => f(a)
      case _: partial.Result.Errors => M.empty

  /** The focus or the errors, as an `Either` — `result(s).asEither`. */
  def either(s: S): Either[partial.Result.Errors, A] = result(s).asEither

  /** The error-free view: a plain [[dev.constructive.eo.optics.PickFold]] with the same hits. */
  def affineFold: PickFold[S, A] = new PickFold(getOption)

  /** Fused `ResultFold.andThen(ResultFold)` — `flatMap`s the results, so both sides' errors
    * surface.
    */
  inline def andThen[C](inner: ResultFold[A, C]): ResultFold[S, C] =
    new ResultFold(s => result(s).flatMap(inner.result))

  /** Fused `ResultFold.andThen(Getter)` — `map`s the result. */
  inline def andThen[C](inner: Getter[A, C]): ResultFold[S, C] =
    new ResultFold(s => result(s).map(inner.get))

  /** Fused `ResultFold.andThen(PickFold)` — an inner miss is chimney's empty-value error. */
  inline def andThen[C](inner: PickFold[A, C]): ResultFold[S, C] =
    new ResultFold(s => result(s).flatMap(a => partial.Result.fromOption(inner.pick(a))))

  /** ResultFold ∘ any single-focus writable optic (Lens / Iso / Prism / Optional). The inner's
    * write side is irrelevant — the composite only reads — and [[ResultFold.Step]] reads its focus
    * as a `partial.Result`, keeping the outer errors.
    */
  @targetName("andThenSingle")
  def andThen[C, IT, IB, FI[_, _]](inner: Optic[A, IT, C, IB, FI])(using
      step: ResultFold.Step[FI]
  ): ResultFold[S, C] =
    // The inner's own `get` / `getOption` when it carries one — skips the carrier value `to`
    // builds (a split-combine lens's `(complement, focus)` tuple), as core's `ReadCompose` does.
    val read: A => partial.Result[C] = inner match
      case g: CanGet[A @unchecked, C @unchecked]       => a => partial.Result.Value(g.get(a))
      case g: CanGetOption[A @unchecked, C @unchecked] =>
        a => partial.Result.fromOption(g.getOption(a))
      case _ => a => step.read(inner.to(a))
    new ResultFold(s => result(s).flatMap(read))

  /** Override of the trait's read-only-inner overload, re-homed here so it shares an owner with
    * `andThenSingle` above — see `Getter`'s twin override for the ambiguity rationale. Read-only
    * inners other than the fused ones above go through `ReadCompose` and drop the errors.
    */
  override def andThen[C, IB, G[_, _]](inner: Optic[A, Unit, C, IB, G])(using
      rc: ReadCompose[Affine, G]
  ): rc.Out[S, C] =
    rc.compose(this, inner)

object ResultFold:

  /** Build from `result: S => partial.Result[A]`.
    *
    * @group Constructors
    */
  def apply[S, A](result: S => partial.Result[A]): ResultFold[S, A] =
    new ResultFold(result)

  /** How a single-focus carrier's focus reads as a `partial.Result`: a total carrier always yields
    * a value, a partial one turns a miss into chimney's empty-value error. Multi-focus carriers
    * have no instance.
    */
  trait Step[F[_, _]]:
    def read[X, C](fc: F[X, C]): partial.Result[C]

  object Step extends LowPriorityStep:

    given total[F[_, _]](using acc: Accessor[F]): Step[F] with
      def read[X, C](fc: F[X, C]): partial.Result[C] = partial.Result.Value(acc.get(fc))

  trait LowPriorityStep:

    given partialStep[F[_, _]](using acc: PartialAccessor[F]): Step[F] with
      def read[X, C](fc: F[X, C]): partial.Result[C] = partial.Result.fromOption(acc.getOption(fc))
