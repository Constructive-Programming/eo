package dev.constructive.eo
package schemes

import data.PSVec
import schemes.samples.Expr

/** The linear-spine schemes the stack-safety examples (`SchemesSpec`) and the on-stack mechanism
  * probes (JVM-only `SchemesStackSpec`) both run: the probes wrap these, they never restate them.
  */
object SchemeFixtures:

  /** Seed `n` expands to the single child seed `n - 1`; `n <= 0` is a leaf. */
  val spineExpand: Int => PSVec[Int] =
    n => if n <= 0 then PSVec.empty[Int] else PSVec.singleton(n - 1)

  /** Depth of the spine: a leaf is 0, a node is one more than its only child. */
  val spineDepth: (Int, PSVec[Int]) => Int = (n, rs) => if n <= 0 then 0 else rs(0) + 1

  /** Builds `Neg(Neg(… Lit(0.0)))`, `n` negations deep. */
  val negCoalg: Schemes.Coalg[Int, Expr] = n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(0.0))
    else (PSVec.singleton(n - 1), (ks: PSVec[Expr]) => Expr.Neg(ks(0)))

  /** Depth of a `Neg` spine (a literal is 0). */
  val negDepth: (Expr, PSVec[Int]) => Int = (node, kids) =>
    node match
      case Expr.Lit(_) => 0
      case _           => kids(0) + 1
