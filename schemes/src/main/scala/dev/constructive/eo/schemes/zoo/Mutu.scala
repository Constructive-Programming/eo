package dev.constructive.eo
package schemes
package zoo

import cats.Traverse

/** Mutumorphism citizen — a fold by **mutual recursion** ([[ReadScheme]]) with **`X = F[(A, B)]`**:
  * two algebras compute a pair `(A, B)` per node, each free to read both halves of its children.
  *
  * `algA: F[(A, B)] => A` and `algB: F[(A, B)] => B` are the two mutually-recursive functions; the
  * citizen returns the `A` half. It generalises [[Zygo]] — `zygo(aux)(alg)` is `mutu` where the
  * second algebra ignores the `A` half (`algB = aux ∘ map(_._2)`) — and so, transitively, [[Para]]
  * and [[Cata]]. The two results are computed in **one pass** over the structure. Stack-safe (the
  * [[Machines.foldLayered]] machine).
  */
final class Mutu[F[_], S, A, B](
    private[zoo] val algA: F[(A, B)] => A,
    private[zoo] val algB: F[(A, B)] => B,
)(using F: Traverse[F], P: Project[F, S])
    extends ReadScheme[S, A]:
  type X = F[(A, B)]

  private val run: S => A =
    val fold: S => (A, B) =
      Machines.foldLayered[F, S, (A, B)](P.project, (_, fab) => (algA(fab), algB(fab)))
    s => fold(s)._1

  protected def read(s: S): A = run(s)
