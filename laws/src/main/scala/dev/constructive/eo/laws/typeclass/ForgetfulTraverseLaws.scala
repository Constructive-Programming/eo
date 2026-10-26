package dev.constructive.eo.laws.typeclass

import cats.data.{Nested, State, Tuple2K}
import cats.{Applicative, Id, Traverse}
import dev.constructive.eo.forgetful.{ForgetfulFold, ForgetfulTraverse}

/** Law equations for any `ForgetfulTraverse[F, Applicative]` — cats' `Traverse` laws restated for
  * the two-parameter carrier, at fixed effects:
  *
  *   - identity: `traverse[Id](fa)(a => a) == fa`;
  *   - sequential composition: traversing under `Nested[List, Option]` equals traversing under
  *     `List`, then each branch under `Option`;
  *   - parallel composition: traversing under `Tuple2K[List, Vector]` equals the pair of the two
  *     separate traversals;
  *   - multi-shot agreement: traversing under `List` gives cats' cartesian product
  *     `Traverse[List].traverse(foci)(f)` over the carrier's foci (read off `ForgetfulFold`), each
  *     choice written back into `fa`.
  *
  * The effects are deliberately MULTI-SHOT (`List`, `Vector` resume every continuation once per
  * branch; `Tuple2K` once per component): a traverse that threads one mutable result through `map2`
  * passes every single-shot law (`Id`, `Option`, `Const`) yet hands all branches the same result.
  * The last law is the one that pins a branch to its values; the composition laws compare the
  * carrier only with itself.
  *
  * The `[_ <: Distributive]` flavour (ModifyF) is not covered: it collapses to a tautology at `Id`.
  */
trait ForgetfulTraverseLaws[F[_, _], X, A]:

  /** `traverse[Id](fa)(a => a) == fa`. */
  def traverseIdentity(fa: F[X, A])(using
      FT: ForgetfulTraverse[F, Applicative]
  ): Boolean =
    FT.traverse[X, A, A, Id](fa, a => a: Id[A])(using Applicative[Id]) == fa

  /** `traverse[Nested[List, Option]](fa)(a => Nested(f(a).map(g))) ==
    * Nested(traverse[List](fa)(f).map(traverse[Option](_)(g)))`.
    */
  def traverseSequentialComposition(fa: F[X, A], f: A => List[A], g: A => Option[A])(using
      FT: ForgetfulTraverse[F, Applicative]
  ): Boolean =
    type LO[Z] = Nested[List, Option, Z]
    val nested = FT.traverse[X, A, A, LO](fa, a => Nested(f(a).map(g)))
    val stepwise = FT.traverse[X, A, A, List](fa, f).map(FT.traverse[X, A, A, Option](_, g))
    nested.value == stepwise

  /** `traverse[Tuple2K[List, Vector]](fa)(a => Tuple2K(f(a), g(a))) ==
    * Tuple2K(traverse[List](fa)(f), traverse[Vector](fa)(g))`.
    */
  def traverseParallelComposition(fa: F[X, A], f: A => List[A], g: A => Vector[A])(using
      FT: ForgetfulTraverse[F, Applicative]
  ): Boolean =
    type LV[Z] = Tuple2K[List, Vector, Z]
    val both = FT.traverse[X, A, A, LV](fa, a => Tuple2K(f(a), g(a)))
    both.first == FT.traverse[X, A, A, List](fa, f) &&
    both.second == FT.traverse[X, A, A, Vector](fa, g)

  /** `traverse[List](fa)(f) == Traverse[List].traverse(foci(fa))(f).map(refill(fa, _))`, with
    * `foci` the carrier's `foldMap(List(_))` and `refill` writing a list of foci back in order (a
    * single-shot `State` traverse).
    */
  def traverseMultiShotAgreesWithFoci(fa: F[X, A], f: A => List[A])(using
      FT: ForgetfulTraverse[F, Applicative],
      FF: ForgetfulFold[F],
  ): Boolean =
    type Refill[Z] = State[List[A], Z]
    val pop: A => Refill[A] = a =>
      State {
        case b :: rest => (rest, b)
        case Nil       => (Nil, a)
      }
    val refill: List[A] => F[X, A] = bs => FT.traverse[X, A, A, Refill](fa, pop).runA(bs).value
    val foci = FF.foldMap[X, A, List[A]](List(_), fa)
    FT.traverse[X, A, A, List](fa, f) == Traverse[List].traverse(foci)(f).map(refill)
