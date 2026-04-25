package eo

import cats.syntax.either._

import optics.Optic

/** Composition algebra for a two-parameter carrier `F[_, _]` — given two optics that share `F`,
  * [[composeTo]] produces the combined `F[Z, C]` from a source `S` and [[composeFrom]] unfolds a
  * combined `F[Z, D]` back through the inner and outer optics.
  *
  * The class-level type parameters `Xo` ("outer" X) and `Xi` ("inner" X) are the existentials of
  * the two optics being composed: `Xo` is the outer optic's existential, `Xi` is the inner's. Named
  * without shadowing the `type X` member on [[Optic]] so [[composeTo]] and [[composeFrom]] can
  * refinement-type on them.
  *
  * Any carrier that wants to support `Optic.andThen` must supply an instance. Carriers can
  * specialise on concrete optic shapes by pattern-matching on the `outer` / `inner` arguments.
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

  /** Push-side composition — run `outer.to(s)` to get an `F[Xo, A]`, feed the focus through
    * `inner.to` to produce `F[Xi, C]`, reassemble as `F[Z, C]`.
    *
    * @tparam S
    *   outer source type
    * @tparam T
    *   outer result type
    * @tparam A
    *   outer focus read (which becomes the inner source)
    * @tparam B
    *   outer focus written back
    * @tparam C
    *   inner focus read (the combined optic's focus)
    * @tparam D
    *   inner focus written back
    */
  def composeTo[S, T, A, B, C, D](
      s: S,
      outer: Optic[S, T, A, B, F] { type X = Xo },
      inner: Optic[A, B, C, D, F] { type X = Xi },
  ): F[Z, C]

  /** Pull-side composition — unfold `F[Z, D]` back through `inner.from` and `outer.from` to produce
    * `T`. Dual of [[composeTo]].
    *
    * @tparam S
    *   outer source type
    * @tparam T
    *   outer result type
    * @tparam A
    *   outer focus read
    * @tparam B
    *   outer focus written back (intermediate — produced by `inner.from`)
    * @tparam C
    *   inner focus read
    * @tparam D
    *   inner focus written back
    */
  def composeFrom[S, T, A, B, C, D](
      xd: F[Z, D],
      inner: Optic[A, B, C, D, F] { type X = Xi },
      outer: Optic[S, T, A, B, F] { type X = Xo },
  ): T

/** Typeclass instances for [[AssociativeFunctor]]. */
object AssociativeFunctor:

  /** `AssociativeFunctor[Tuple2, Xo, Xi]` — pairs Xo and Xi into `Z = (Xo, Xi)`. Powers
    * `lens.andThen(lens)` composition.
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

  /** `AssociativeFunctor[Either, Xo, Xi]` — threads `Left` miss branches by bubbling them up.
    * Powers `prism.andThen(prism)` composition.
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
