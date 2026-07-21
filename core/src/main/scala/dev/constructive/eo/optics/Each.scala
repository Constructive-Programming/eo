package dev.constructive.eo
package optics

import cats.Traverse

/** Monocle's `Each` as a plain constructor object — NOT a typeclass. `Each[F, A]` is exactly
  * [[Traversal.each]]: a Traversal over every element of any `cats.Traverse` container, provided
  * under the name Monocle users grep for. There is no `Each[S, A]` typeclass to instance and
  * nothing to derive — the result is an ordinary optic that composes through `.andThen` and serves
  * capability evidence (`CanFold`, `CanModify`) like any other.
  *
  * @group Optics
  */
object Each:

  /** Traversal over every element of a `Traverse` container — alias for [[Traversal.each]].
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * val everyInt = Each[List, Int]      // ≡ Traversal.each[List, Int]
    * everyInt.modify(_ + 1)(List(1, 2))  // List(2, 3)
    *   }}}
    */
  def apply[T[_]: Traverse, A]: Traversal[T[A], T[A], A, A] =
    Traversal.each[T, A]
