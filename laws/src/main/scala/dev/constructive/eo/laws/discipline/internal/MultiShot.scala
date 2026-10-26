package dev.constructive.eo
package laws
package discipline
package internal

import org.scalacheck.{Arbitrary, Cogen, Gen}

/** Generators for the laws that run a traverse under a MULTI-SHOT effect (`List`, `Vector`): an
  * applicative that resumes each continuation once per branch, which is what catches a traverse
  * sharing one mutable result across continuations. Single-shot effects (`Id`, `Option`, `Const`)
  * run every continuation once and cannot see that.
  *
  * A multi-shot result has one branch per CHOICE of effect value at every focus, so its size is the
  * product of the per-focus widths. The widths here are 0..2 (0 checks the empty short-circuit, 1
  * the single-shot path, 2 the branching one), and [[capped]] keeps the structure under test small.
  */
private[laws] object MultiShot:

  /** `Gen.resize` cap for the structure under test — at most `2^MaxSize` branches for a structure
    * that respects ScalaCheck's size parameter (a hand-written generator should stay this small).
    */
  val MaxSize: Int = 8

  /** `arbitrary[T]` at a size of at most [[MaxSize]]. */
  def capped[T](using a: Arbitrary[T]): Gen[T] = Gen.resize(MaxSize, a.arbitrary)

  /** A function `A => G[A]` returning 0, 1 or 2 values (weighted towards 2), built by `mk`. */
  def fn[A, G[_]](mk: List[A] => G[A])(using a: Arbitrary[A], c: Cogen[A]): Gen[A => G[A]] =
    Gen.function1[A, G[A]](
      Gen.frequency(1 -> 0, 3 -> 1, 6 -> 2).flatMap(Gen.listOfN(_, a.arbitrary)).map(mk)
    )
