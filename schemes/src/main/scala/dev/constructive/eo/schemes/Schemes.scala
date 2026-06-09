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
  * `someLens.andThen(cata(alg))`, and the materializing `ana(c).cross(cata(a))` (via the core
  * `Optic.cross` combinator) — the latter equal to `hylo` on the same computation (the hylo law).
  *
  * All three reduce to a single stack-safe post-order machine ([[unfoldFold]]): every node is
  * pushed as a heap frame, never recursed on the JVM call stack, so it is stack-safe for any
  * (terminating) input. `cata` fits the same machine by capturing the node `S` in its per-node
  * combiner closure. No `Eval` (no fixpoint axis). (A `< 512` on-stack fast path, as in
  * `Plated.transformMachine`, is a possible perf follow-up; v1 is pure heap-stack.)
  */
object Schemes:

  /** Closure-carrying coalgebra (encoding A): a node yields its child seeds plus a combiner from
    * the folded child results. A leaf is `(PSVec.empty, _ => value)`. The node's own payload is
    * available to the combiner via closure capture.
    *
    * The combiner is handed a `PSVec[R]` of exactly the same length as the child-seed vector, in
    * the same order; it is the combiner's responsibility to index it consistently with that arity
    * (a combiner that reads `kids(1)` of a 1-element vector throws `IndexOutOfBounds`; one that
    * ignores `kids(2)` of a 3-element vector silently drops that already-folded subtree).
    */
  type Coalg[N, R] = N => (PSVec[N], PSVec[R] => R)

  /** The single stack-safe post-order engine: expand a node to (children, combiner), recurse on
    * children (heap-stacked), then combine the folded results. Stack-safe for any *terminating*
    * `step`; output is `O(nodes)` only if the combiner builds a structure (it does for [[ana]], not
    * for [[hylo]]).
    *
    * Termination is the caller's contract: because the recursion lives on the heap (an `ArrayDeque`
    * of frames) rather than the JVM call stack, a non-terminating coalgebra exhausts the heap
    * (`OutOfMemoryError`, process-fatal) rather than overflowing the stack (`StackOverflowError`,
    * thread-local) — the price of stack-safety for the terminating case.
    */
  private def unfoldFold[N, R](step: Coalg[N, R]): N => R =
    n0 =>
      final class Frame(
          val kids: PSVec[N],
          val out: Array[AnyRef],
          val combine: PSVec[R] => R,
          var i: Int,
      )
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit =
        val (kids, combine) = step(n)
        if kids.isEmpty then ret = combine(PSVec.empty[R]).asInstanceOf[AnyRef]
        else stack.push(new Frame(kids, new Array[AnyRef](kids.length), combine, 0))
      enter(n0)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.out(fr.i - 1) = ret
        if fr.i < fr.kids.length then
          val child = fr.kids(fr.i)
          fr.i += 1
          enter(child)
        else
          ret = fr.combine(PSVec.unsafeWrap[R](fr.out)).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

  /** Catamorphism as a composable `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children. Stack-safe.
    */
  def cata[S, A](alg: (S, PSVec[A]) => A)(using P: Plated[S]): DirectGetter[S, A] =
    Getter[S, A](unfoldFold[S, A](s => (P.childrenVec(s), rs => alg(s, rs))))

  /** Anamorphism as a `Review` (reverse-construction optic): a stack-safe unfold `Seed => S`.
    * Materializing — the built `S` is `O(nodes)`; the unfold loop itself is heap-stacked.
    */
  def ana[Seed, S](coalg: Coalg[Seed, S]): Review[S, Seed] =
    Review[S, Seed](unfoldFold(coalg))

  /** Hylomorphism — the **fused** refold `Seed => A`, building **no intermediate `S`**: the
    * coalgebra expands seeds and the combiner folds to `A` in one post-order pass. Returned as a
    * `Getter[Seed, A]` so it composes further. Equal to `ana(c).cross(cata(a))` on the same
    * computation (the hylo law), but without materializing the structure.
    */
  def hylo[Seed, A](coalg: Coalg[Seed, A]): DirectGetter[Seed, A] =
    Getter[Seed, A](unfoldFold(coalg))
