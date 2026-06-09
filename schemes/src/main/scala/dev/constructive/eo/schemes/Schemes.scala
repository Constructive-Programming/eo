package dev.constructive.eo
package schemes

import data.PSVec
import optics.{DirectGetter, Getter, Plated, Review}

/** Recursion schemes as composable optics, built on the core optic surface.
  *
  *   - [[cata]] is a `DirectGetter[S, A]` driven by `Plated[S]` — the structural fold, generalising
  *     `Plated.transform` from `S => S` to `S => A`.
  *   - [[ana]] is a `Review[S, Seed]` — the unfold (build `S` from a seed).
  *   - [[hylo]] is a **fused** `DirectGetter[Seed, A]` — refold with **no intermediate `S`** built.
  *
  * Because they produce core optic types, they compose with the rest of the optic algebra:
  * `someLens.andThen(cata(alg))`, and the materializing `ana(…).cross(cata(…))` (via the core
  * `Optic.cross` combinator) — the latter equal to `hylo` on the same computation (the hylo law).
  *
  * All three are two halves of one shape — an `expand: N => PSVec[N]` (the children/seeds) and a
  * `combine: (N, PSVec[R]) => R` (the fold, which sees the original node, paramorphism-flavored).
  * `cata` takes `expand` from `Plated`; `ana`/`hylo` take it explicitly. They all run on a single
  * stack-safe engine ([[unfoldFold]]): a `< 512`-deep on-stack fast path (no heap frames) that
  * falls back, per deep subtree, to a heap `ArrayDeque` machine — the same hybrid as
  * `Plated.transform`. So shallow trees pay no frame allocation, and arbitrarily deep ones stay
  * stack-safe.
  */
object Schemes:

  /** Depth at which [[unfoldFold]]'s on-stack recursion hands a subtree to the heap machine —
    * mirrors `Plated.transformRecursionLimit`. Balanced trees (depth ~log n) never reach it.
    */
  final private val OnStackLimit = 512

  /** The single stack-safe post-order engine. `expand` yields a node's children; `combine` folds a
    * node plus its already-folded children to `R`. Recurses on the JVM stack while shallow (no
    * per-node `Frame`/`ArrayDeque`), switching any subtree deeper than [[OnStackLimit]] to a heap
    * machine — so it is stack-safe for any *terminating* `expand`.
    *
    * Termination is the caller's contract: past the on-stack limit the recursion lives on the heap,
    * so a non-terminating `expand` exhausts the heap (`OutOfMemoryError`) rather than overflowing
    * the stack — the price of stack-safety for the terminating case.
    *
    * `combine` is handed a `PSVec[R]` the same length and order as `expand`'s child vector; it is
    * the caller's responsibility to index it consistently with that arity (reading `kids(1)` of a
    * 1-element vector throws `IndexOutOfBounds`; ignoring `kids(2)` of a 3-element one silently
    * drops that already-folded subtree).
    */
  private def unfoldFold[N, R](expand: N => PSVec[N], combine: (N, PSVec[R]) => R): N => R =

    // Heap fallback for subtrees deeper than the on-stack limit: an explicit post-order machine,
    // the node carried in the frame so `combine` needs no per-node closure.
    def heap(root: N): R =
      final class Frame(val node: N, val kids: PSVec[N], val out: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit =
        val kids = expand(n)
        if kids.isEmpty then ret = combine(n, PSVec.empty[R]).asInstanceOf[AnyRef]
        else stack.push(new Frame(n, kids, new Array[AnyRef](kids.length), 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.out(fr.i - 1) = ret
        if fr.i < fr.kids.length then
          val child = fr.kids(fr.i)
          fr.i += 1
          enter(child)
        else
          ret = combine(fr.node, PSVec.unsafeWrap[R](fr.out)).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

    // On-stack fast path: no Frame, no ArrayDeque — just recursion bounded by OnStackLimit.
    def rec(n: N, depth: Int): R =
      val kids = expand(n)
      val k = kids.length
      if k == 0 then combine(n, PSVec.empty[R])
      else if depth >= OnStackLimit then heap(n)
      else
        val out = new Array[AnyRef](k)
        var i = 0
        while i < k do
          out(i) = rec(kids(i), depth + 1).asInstanceOf[AnyRef]
          i += 1
        combine(n, PSVec.unsafeWrap[R](out))

    n0 => rec(n0, 0)

  /** Catamorphism as a composable `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children. Stack-safe.
    */
  def cata[S, A](alg: (S, PSVec[A]) => A)(using P: Plated[S]): DirectGetter[S, A] =
    Getter[S, A](unfoldFold[S, A](P.childrenVec, alg))

  /** Anamorphism as a `Review` (reverse-construction optic): a stack-safe unfold `Seed => S`.
    * `expand` yields each seed's child seeds; `build` reassembles a node from its built children.
    * Materializing — the built `S` is `O(nodes)`.
    */
  def ana[Seed, S](expand: Seed => PSVec[Seed], build: (Seed, PSVec[S]) => S): Review[S, Seed] =
    Review[S, Seed](unfoldFold(expand, build))

  /** Hylomorphism — the **fused** refold `Seed => A`, building **no intermediate `S`**: `expand`
    * unfolds seeds and `alg` folds to `A` in one post-order pass. Returned as a `Getter[Seed, A]`
    * so it composes further. Equal to `ana(expand, build).cross(cata(alg))` on the same computation
    * (the hylo law), but without materializing the structure.
    */
  def hylo[Seed, A](
      expand: Seed => PSVec[Seed],
      alg: (Seed, PSVec[A]) => A,
  ): DirectGetter[Seed, A] =
    Getter[Seed, A](unfoldFold(expand, alg))
