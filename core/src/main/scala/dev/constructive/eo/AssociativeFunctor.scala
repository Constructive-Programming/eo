package dev.constructive.eo

import cats.syntax.either.*

import optics.Optic

/** Composition algebra for a two-parameter carrier `F[_, _]`. [[composeTo]] threads the focus
  * through inner ∘ outer to produce `F[Z, C]`; [[composeFrom]] unfolds it back. `Xo` / `Xi` are the
  * outer / inner optics' existentials; named distinct from `Optic#X` so `composeTo` / `composeFrom`
  * can refinement-type on them. Carriers can specialise by pattern-matching on the `outer` /
  * `inner` arguments.
  *
  * @tparam F
  *   carrier
  * @tparam Xo
  *   outer-optic existential
  * @tparam Xi
  *   inner-optic existential
  */
trait AssociativeFunctor[F[_, _], Xo, Xi]:
  /** Combined existential carried through the composed optic. */
  type Z

  /** Push-side: `outer.to(s)` → focus through `inner.to` → reassemble as `F[Z, C]`. */
  def composeTo[S, T, A, B, C, D](
      s: S,
      outer: Optic[S, T, A, B, F] { type X = Xo },
      inner: Optic[A, B, C, D, F] { type X = Xi },
  ): F[Z, C]

  /** Pull-side: unfold `F[Z, D]` back through `inner.from` and `outer.from`. */
  def composeFrom[S, T, A, B, C, D](
      xd: F[Z, D],
      inner: Optic[A, B, C, D, F] { type X = Xi },
      outer: Optic[S, T, A, B, F] { type X = Xo },
  ): T

/** Typeclass instances for [[AssociativeFunctor]]. */
object AssociativeFunctor:

  /** `Tuple2` — `Z = (Xo, Xi)`. Powers `lens.andThen(lens)`.
    *
    * @group Instances
    */
  given tupleAssocF[Xo, Xi]: AssociativeFunctor[Tuple2, Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Tuple2] { type X = Xo },
        inner: Optic[A, B, C, D, Tuple2] { type X = Xi },
    ): (Z, C) =
      val (x, a) = outer.to(s)
      val (y, c) = inner.to(a)
      (x -> y, c)

    def composeFrom[S, T, A, B, C, D](
        xd: (Z, D),
        inner: Optic[A, B, C, D, Tuple2] { type X = Xi },
        outer: Optic[S, T, A, B, Tuple2] { type X = Xo },
    ): T =
      val ((x, y), d) = xd
      outer.from(x, inner.from(y, d))

  /** `Either` — bubbles `Left` miss branches up. Powers `prism.andThen(prism)`.
    *
    * @group Instances
    */
  given eitherAssocF[Xo, Xi]: AssociativeFunctor[Either, Xo, Xi] with
    type Z = Either[Xo, Xi]

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Either] { type X = Xo },
        inner: Optic[A, B, C, D, Either] { type X = Xi },
    ): Either[Z, C] =
      outer.to(s).fold(_.asLeft[Xi].asLeft[C], inner.to(_).leftMap(_.asRight[Xo]))

    def composeFrom[S, T, A, B, C, D](
        xd: Either[Z, D],
        inner: Optic[A, B, C, D, Either] { type X = Xi },
        outer: Optic[S, T, A, B, Either] { type X = Xo },
    ): T =
      xd match
        case Right(d)       => outer.from(Right(inner.from(Right(d))))
        case Left(Right(y)) => outer.from(Right(inner.from(Left(y))))
        case Left(Left(x))  => outer.from(Left(x))
