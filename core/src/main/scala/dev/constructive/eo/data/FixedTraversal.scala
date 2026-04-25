package dev.constructive.eo
package data

import scala.compiletime.ops.int.*

/** Match-type helper that builds the fixed-arity traversal carrier tuple shape. Reduces to `A *:
  * EmptyTuple` at `C = 0` (the phantom-only base) and extends by one `B *:` per successor step.
  */
type FixedTraversal_[C, A, B] = C match
  case 0    => A *: EmptyTuple
  case S[n] => B *: FixedTraversal_[n, A, B]

/** Two-parameter carrier for a fixed-arity traversal.
  *
  * `FixedTraversal[N][X, A]` reduces to an (N+1)-tuple `A *: A *: … *: X *: EmptyTuple` — exactly
  * `N` focus slots followed by the phantom leftover `X`. Used by the `Traversal.two` / `.three` /
  * `.four` constructors for compile-time-known-count traversals.
  *
  * See [[FixedTraversal]] (the companion object) for the per-arity typeclass instances.
  */
type FixedTraversal[C] = [A, B] =>> FixedTraversal_[C, A, B]

/** Typeclass instances for the fixed-arity traversal carriers.
  *
  * `FixedTraversal[N][X, A]` reduces to an (N+1)-tuple shaped as `A *: A *: … *: X *: EmptyTuple` —
  * `N` element slots followed by the phantom `X`. Mapping therefore rewrites the first `N`
  * positions and leaves the phantom alone.
  *
  * IMPORTANT: each pattern match below is manually coupled to the type-level definition of
  * `FixedTraversal_` above. If the tuple element ordering ever changes, these patterns must be
  * updated in lockstep — the compiler cannot check that they stay in sync.
  */
object FixedTraversal:

  /** `ForgetfulFunctor[FixedTraversal[2]]` — maps both focus slots through `f`, leaves the phantom
    * slot alone. Unlocks `.modify` / `.replace` on every arity-2 fixed traversal.
    *
    * @group Instances
    */
  given two: ForgetfulFunctor[FixedTraversal[2]] with

    def map[X, A, B](fa: FixedTraversal[2][X, A], f: A => B): FixedTraversal[2][X, B] =
      fa match
        case (a0, a1, x) => (f(a0), f(a1), x)

  /** `ForgetfulFunctor[FixedTraversal[3]]` — arity-3 counterpart of [[two]]. Maps all three focus
    * slots through `f`.
    *
    * @group Instances
    */
  given three: ForgetfulFunctor[FixedTraversal[3]] with

    def map[X, A, B](fa: FixedTraversal[3][X, A], f: A => B): FixedTraversal[3][X, B] =
      fa match
        case (a0, a1, a2, x) => (f(a0), f(a1), f(a2), x)

  /** `ForgetfulFunctor[FixedTraversal[4]]` — arity-4 counterpart of [[two]]. Maps all four focus
    * slots through `f`.
    *
    * @group Instances
    */
  given four: ForgetfulFunctor[FixedTraversal[4]] with

    def map[X, A, B](fa: FixedTraversal[4][X, A], f: A => B): FixedTraversal[4][X, B] =
      fa match
        case (a0, a1, a2, a3, x) => (f(a0), f(a1), f(a2), f(a3), x)
