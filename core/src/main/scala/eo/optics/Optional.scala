package eo
package optics

import data.Affine

/** Constructor for `Optional` — the conditionally-present single-focus optic, backed by the
  * `Affine` carrier.
  *
  * An `Optional[S, A]` (short for `Optic[S, S, A, A, Affine]`) encodes a field that may or may not
  * be there: a predicate-gated access (`street` only when `isValid`), the `Some` case of an
  * `Option` field, a refinement-style narrowing that can fail.
  *
  * Compose freely with `Lens` via `lens.andThen(opt)` — the cross-carrier `.andThen` extension
  * auto-morphs the Lens into the Affine carrier via `Composer[Tuple2, Affine]`. The `Affine`
  * carrier admits unbounded X/Y via [[data.Affine.assoc]], so any abstract existential
  * satisfies the composition requirement.
  */
object Optional:

  /** Construct an Optional from a partial getter `getOrModify` (returns `Right(a)` on hit,
    * `Left(t)` on miss) and a re-assembler `reverseGet: (S, B) => T`.
    *
    * The `F` type parameter is carried for symmetry with other carrier-aware constructors but does
    * not alter the return type; every Optional is `Optic[…, Affine]`.
    *
    * @group Constructors
    * @tparam S
    *   source type
    * @tparam T
    *   result type (often `= S`)
    * @tparam A
    *   focus read from the hit branch
    * @tparam B
    *   focus written back to produce `T` (often `= A`)
    * @tparam F
    *   carrier parameter — unused; present for constructor-shape symmetry
    *
    * @example
    *   {{{
    * // Focus Person.age only when the person is a legal adult:
    * case class Person(age: Int, name: String)
    * val adultAge = Optional[Person, Person, Int, Int, Affine](
    *   getOrModify = p => Either.cond(p.age >= 18, p.age, p),
    *   reverseGet  = { case (p, a) => p.copy(age = a) },
    * )
    *   }}}
    */
  def apply[S, T, A, B, F[_, _]](
      getOrModify: S => Either[T, A],
      reverseGet: ((S, B)) => T,
  ) =
    new Optic[S, T, A, B, Affine]:
      type X = (T, S)
      def to: S => Affine[X, A] = s => Affine(getOrModify(s).map(s -> _))
      def from: Affine[X, B] => T = _.affine.fold(identity, reverseGet)
