package eo
package data

import scala.annotation.tailrec
import scala.compiletime.ops.int.*

type FixedTraversal_[C, A, B] = C match
  case 0    => A *: EmptyTuple
  case S[n] => B *: FixedTraversal_[n, A, B]

type FixedTraversal[C] = [A, B] =>> FixedTraversal_[C, A, B]

/** Typeclass instances for the fixed-arity traversal carriers.
  *
  * `FixedTraversal[N][X, A]` reduces to an (N+1)-tuple shaped as
  * `A *: A *: … *: X *: EmptyTuple` — `N` element slots followed by the
  * phantom `X`. Mapping therefore rewrites the first `N` positions and
  * leaves the phantom alone.
  */
object FixedTraversal:

  given two: ForgetfulFunctor[FixedTraversal[2]] with
    def map[X, A, B]
        : FixedTraversal[2][X, A] => (A => B) => FixedTraversal[2][X, B] =
      fa =>
        f =>
          fa match
            case (a0, a1, x) => (f(a0), f(a1), x)

  given three: ForgetfulFunctor[FixedTraversal[3]] with
    def map[X, A, B]
        : FixedTraversal[3][X, A] => (A => B) => FixedTraversal[3][X, B] =
      fa =>
        f =>
          fa match
            case (a0, a1, a2, x) => (f(a0), f(a1), f(a2), x)

  given four: ForgetfulFunctor[FixedTraversal[4]] with
    def map[X, A, B]
        : FixedTraversal[4][X, A] => (A => B) => FixedTraversal[4][X, B] =
      fa =>
        f =>
          fa match
            case (a0, a1, a2, a3, x) => (f(a0), f(a1), f(a2), f(a3), x)
