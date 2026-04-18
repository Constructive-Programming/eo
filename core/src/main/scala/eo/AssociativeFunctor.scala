package eo

import cats.syntax.either._

/** "Push" side of optic composition — given two push-directions over a carrier `F`, combine them
  * into a single push that threads both existentials through a new `Z`.
  *
  * Required by [[optics.Optic.andThenLeft]]; most users get at this indirectly through
  * [[AssociativeFunctor]].
  *
  * @tparam F
  *   carrier
  * @tparam X
  *   left existential
  * @tparam Y
  *   right existential
  */
trait LeftAssociativeFunctor[F[_, _], X, Y]:
  /** Combined existential carried through the composed optic. */
  type Z
  def associateLeft[S, A, C]: (S, S => F[X, A], A => F[Y, C]) => F[Z, C]

/** Dual of [[LeftAssociativeFunctor]]: "pull" side of optic composition, used by
  * [[optics.Optic.andThenRight]].
  */
trait RightAssociativeFunctor[F[_, _], X, Y]:
  type Z
  def associateRight[D, B, T]: (F[Z, D], F[Y, D] => B, F[X, B] => T) => T

/** Sum of [[LeftAssociativeFunctor]] and [[RightAssociativeFunctor]] — required by `Optic.andThen`.
  * Any carrier that wants to support fully-bidirectional composition must supply an instance.
  */
trait AssociativeFunctor[F[_, _], X, Y]
    extends LeftAssociativeFunctor[F, X, Y],
      RightAssociativeFunctor[F, X, Y]

/** Typeclass instances for [[AssociativeFunctor]]. */
object AssociativeFunctor:

  /** `AssociativeFunctor[Tuple2, X, Y]` — pairs X and Y into `Z = (X, Y)`. Powers
    * `lens.andThen(lens)` composition.
    *
    * @group Instances
    */
  given tupleAssocF[X, Y]: AssociativeFunctor[Tuple2, X, Y] with
    type Z = (X, Y)

    def associateLeft[S, A, C]: (S, S => (X, A), A => (Y, C)) => (Z, C) =
      case (s, f, g) =>
        val (x, a) = f(s)
        val (y, c) = g(a)
        (x -> y, c)

    def associateRight[D, B, T]: ((Z, D), ((Y, D)) => B, ((X, B)) => T) => T =
      case (((x, y), d), g, f) => f(x, g(y, d))

  /** `AssociativeFunctor[Either, X, Y]` — threads through the `Left` miss branches by bubbling them
    * up. Powers `prism.andThen(prism)` composition.
    *
    * @group Instances
    */
  given eitherAssocF[X, Y]: AssociativeFunctor[Either, X, Y] with
    type Z = Either[X, Y]

    def associateLeft[S, A, C]: (S, S => Either[X, A], A => Either[Y, C]) => Either[Z, C] =
      case (s, f, g) =>
        f(s).fold(_.asLeft[Y].asLeft[C], g(_).leftMap(_.asRight[X]))

    def associateRight[D, B, T]: (Either[Z, D], Either[Y, D] => B, Either[X, B] => T) => T =
      case (Right(d), f, g)       => g(Right(f(Right(d))))
      case (Left(Right(y)), f, g) => g(Right(f(Left(y))))
      case (Left(Left(x)), _, g)  => g(Left(x))
