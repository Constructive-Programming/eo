package dev.constructive.eo
package optics

import scala.quoted.*

import data.{MultiFocus, PSVec}

/** Macro backend for the fixed-arity [[Traversal]] constructors (`two` / `three` / `four`).
  *
  * One generator emits the per-arity tabulating subclass instead of N hand-written copies.
  * Expansion happens at the CALL site (the companion's `inline def`s splice these impls), which
  * keeps the use-site encoding doctrine intact: every call site gets its own anonymous `Traversal`
  * subclass with monomorphic `to` / `from` bodies — no shared wrapper body hosting per-instance
  * `Function1` dispatch — and literal selector / reverse lambdas are beta-reduced straight into the
  * tabulation. Adding a `five` is a three-line inline def plus a `fiveImpl` here.
  */
private[optics] object TraversalArityMacro:

  /** Shared generator: tabulate `getters` into the `MultiFocus[PSVec]` carrier on the read side,
    * hand the foci to `applyReverse` (arity-matched by the per-arity impls) on the write side.
    */
  private def tabulate[S: Type, T: Type, A: Type, B: Type](
      getters: List[Expr[S => A]],
      applyReverse: List[Expr[B]] => Expr[T],
  )(using Quotes): Expr[Traversal[S, T, A, B]] =
    '{
      new Traversal[S, T, A, B]:
        type X = Unit

        def to(s: S): MultiFocus[PSVec][X, A] =
          val arr = new Array[AnyRef](${ Expr(getters.length) })
          ${
            Expr.block(
              getters.zipWithIndex.map { (g, i) =>
                '{ arr(${ Expr(i) }) = ${ Expr.betaReduce('{ $g(s) }) }.asInstanceOf[AnyRef] }
              },
              '{ () },
            )
          }
          MultiFocus((), PSVec.unsafeWrap[A](arr))

        def from(pair: MultiFocus[PSVec][X, B]): T =
          ${ applyReverse(List.tabulate(getters.length)(i => '{ pair.foci(${ Expr(i) }) })) }
    }

  def twoImpl[S: Type, T: Type, A: Type, B: Type](
      a: Expr[S => A],
      b: Expr[S => A],
      reverse: Expr[(B, B) => T],
  )(using Quotes): Expr[Traversal[S, T, A, B]] =
    tabulate(List(a, b), bs => Expr.betaReduce('{ $reverse(${ bs(0) }, ${ bs(1) }) }))

  def threeImpl[S: Type, T: Type, A: Type, B: Type](
      a: Expr[S => A],
      b: Expr[S => A],
      c: Expr[S => A],
      reverse: Expr[(B, B, B) => T],
  )(using Quotes): Expr[Traversal[S, T, A, B]] =
    tabulate(
      List(a, b, c),
      bs => Expr.betaReduce('{ $reverse(${ bs(0) }, ${ bs(1) }, ${ bs(2) }) }),
    )

  def fourImpl[S: Type, T: Type, A: Type, B: Type](
      a: Expr[S => A],
      b: Expr[S => A],
      c: Expr[S => A],
      d: Expr[S => A],
      reverse: Expr[(B, B, B, B) => T],
  )(using Quotes): Expr[Traversal[S, T, A, B]] =
    tabulate(
      List(a, b, c, d),
      bs => Expr.betaReduce('{ $reverse(${ bs(0) }, ${ bs(1) }, ${ bs(2) }, ${ bs(3) }) }),
    )
