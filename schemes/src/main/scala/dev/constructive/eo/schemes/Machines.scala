package dev.constructive.eo
package schemes

import scala.annotation.tailrec

import cats.{Monad, Traverse}

/** Internal stack-safe fold engines for the typed recursion-scheme path.
  *
  * ==Shape==
  *
  * Every engine is the same two-phase walk — '''descend''' (peel a layer, push a [[Frame]] per
  * interior node onto an immutable `List` stack) and '''bubble''' (store a finished child's result
  * into the top frame's slot, then resume at the next sibling or combine the completed frame) —
  * expressed as ONE `@tailrec` loop whose state is the [[Ascend]] sentinel encoding shared with
  * [[foldLayeredM]]'s `tailRecM` loop (two mutually-recursive phase functions would grow the JVM
  * stack on their cross-calls; a single self-tail-recursive loop compiles to a jump).
  *
  * Mutation is confined to the per-node result buffer (an `Array[AnyRef]`, reused in place as the
  * accumulator) and the frame's slot index — both owned by exactly one walk. Below [[OnStackLimit]]
  * the engines use plain tree recursion instead (the natural functional expression of a fold, and
  * the allocation-free hot path); only deep subtrees pay for frames.
  *
  * [[foldLayered]] and [[foldLayeredOr]] stay separate rather than unifying on the Or-shape: the
  * plain engine's descend is `Either`-free, which keeps the hot cata/hylo path at its pinned
  * allocation profile (CI: 361k B/op on the 8k-node fixture).
  *
  * ==Thread-safety model==
  *
  * Every machine allocates its mutable state (the frame stack, the per-node child/result arrays)
  * '''per invocation''' — and, for the `M` path, per '''force''', inside `M.flatMap(M.unit)` so
  * that re-forcing the same `M[R]` value allocates fresh state on each evaluation. No mutable state
  * is shared across invocations or forces.
  *
  * The only shared values are immutable sentinels:
  *
  *   - [[NoChildren]]: a zero-length `Array[AnyRef]`, shared by all leaf layers (see its own
  *     scaladoc for the immutability argument).
  *   - [[Ascend]]: a stable identity object used as the "ascend" marker in [[foldLayeredM]]'s loop.
  *     It is never written and carries no mutable state.
  *
  * Concurrent invocations of recursion schemes in a single JVM process are therefore safe — each
  * call owns its own heap region and neither reads nor writes the shared sentinels' contents.
  * '''Concurrent forcing of a single `M[R]` value''' is a different question and remains
  * unsupported (two concurrent forces of the exact same suspended `M[R]` could interleave their
  * tailRecM steps); each `run(s)` call returns an independent `M[R]`, and those are safe to force
  * concurrently.
  */
private[schemes] object Machines:

  /** Depth at which the on-stack recursion hands a subtree to the heap machine — mirrors
    * `Plated.transformRecursionLimit`. Balanced trees (depth ~log n) never reach it.
    */
  final val OnStackLimit = 512

  /** The shared "this layer has no children" sentinel.
    *
    * '''What it is:''' a single `Array[AnyRef]` of length 0, allocated once and returned by
    * [[childrenArr]] for every leaf layer (`LeafF`-like constructors with no recursive slots).
    *
    * '''Who uses it:''' every engine, via [[childrenArr]] — leaf layers are the most common case in
    * typed pattern functors, so the shared sentinel avoids a per-leaf empty-array allocation.
    *
    * '''Why it is thread-safe:''' the array has length 0 and no element is ever written into it —
    * every store in the engines targets `arr(i)` for `i < arr.length`. An immutable zero-length
    * array is safe to share across any number of concurrent walks.
    */
  private[schemes] val NoChildren: Array[AnyRef] = new Array[AnyRef](0)

  /** Collect the children of typed layer `fn` into a flat `Array[AnyRef]`, single-pass via
    * `ObjArrBuilder`; [[NoChildren]] for leaf layers.
    */
  private[schemes] def childrenArr[F[_], N](fn: F[N])(using F: Traverse[F]): Array[AnyRef] =
    val n = F.size(fn).toInt
    if n == 0 then NoChildren
    else
      val b = new data.ObjArrBuilder(n)
      val _ = F.foldLeft(fn, ()) { (_, child) =>
        b.unsafeAppend(child.asInstanceOf[AnyRef])
      }
      b.freezeArr

  /** Sentinel op for [[foldLayeredM]]'s loop state — "ascend": consume the pending result against
    * the top frame. Anything else on the loop is the node to descend into.
    */
  private[schemes] object Ascend

  /** Placeholder for the `pending` slot while descending (no result is in flight). Never read —
    * `pending` is consumed only on the [[Ascend]] arm, which is reached only after a real result
    * was threaded in.
    */
  private val NoResult: AnyRef = new AnyRef

  /** One suspended interior node: its layer, the child/result buffer (children overwritten in place
    * by their results), and the index of the next slot awaiting a result.
    */
  final private class Frame[F[_], N](
      val node: N,
      val layer: F[N],
      val arr: Array[AnyRef],
      var i: Int,
  )

  /** Rebuild a typed `F[R]` from the original layer `fn: F[N]` and its children's results, stored
    * positionally in `out` in `Foldable` order — which `Functor.map` matches for a lawful
    * `Traverse`. Lets the schemes hand the algebra a typed `F[R]` (named constructors) rather than
    * a positional vector. Leaf layers are phantom-recast (valid because pattern-functor leaves have
    * no recursive slots by definition).
    */
  private[schemes] def rebuildLayer[F[_], N, R](fn: F[N], out: Array[AnyRef])(using
      F: Traverse[F]
  ): F[R] =
    if out.length == 0 then fn.asInstanceOf[F[R]]
    else
      var i = -1
      F.map(fn) { _ =>
        i += 1
        out(i).asInstanceOf[R]
      }

  /** [[rebuildLayer]]'s paramorphic sibling: pair each original child `N` with its folded result
    * from `out` (positional, `Foldable` order). The subterms come from the layer the machine
    * already holds — no re-`embed`.
    */
  private[schemes] def rebuildLayerPaired[F[_], N, R](fn: F[N], out: Array[AnyRef])(using
      F: Traverse[F]
  ): F[(N, R)] =
    if out.length == 0 then fn.asInstanceOf[F[(N, R)]]
    else
      var i = -1
      F.map(fn) { n =>
        i += 1
        (n, out(i).asInstanceOf[R])
      }

  /** Shared typed engine for the `F`-path schemes. `expand` peels a node into one typed layer of
    * child nodes; the engine folds each child to an `R` (post-order), then calls `combine` with the
    * node, its layer `F[N]`, and the children's results (positional, `Foldable` order).
    * `< [[OnStackLimit]]` deep: plain tree recursion; past it, the descend/bubble heap walk (see
    * the object scaladoc). Stack-safe for any *terminating* `expand` (a non-terminating one
    * exhausts the heap — `OutOfMemoryError` — rather than the stack).
    */
  private[schemes] def foldLayered[F[_], N, R](
      expand: N => F[N],
      combine: (N, F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      // One tail-recursive loop over both phases, [[Ascend]]-sentinel encoded like
      // [[foldLayeredM]] (mutually-recursive descend/bubble functions would grow the JVM
      // stack on their cross-calls): `op` is either the node to descend into or [[Ascend]],
      // in which case `pending` carries the result to store against the top frame.
      @tailrec def loop(op: AnyRef, pending: AnyRef, stack: List[Frame[F, N]]): R =
        if op ne Ascend then
          val n = op.asInstanceOf[N]
          val layer = expand(n)
          val arr = childrenArr(layer)
          if arr.length == 0 then loop(Ascend, combine(n, layer, arr).asInstanceOf[AnyRef], stack)
          else loop(arr(0), NoResult, new Frame(n, layer, arr, 0) :: stack)
        else
          stack match
            case Nil        => pending.asInstanceOf[R]
            case fr :: rest =>
              fr.arr(fr.i) = pending // overwrite the just-folded child's slot
              fr.i += 1
              if fr.i < fr.arr.length then loop(fr.arr(fr.i), NoResult, stack)
              else loop(Ascend, combine(fr.node, fr.layer, fr.arr).asInstanceOf[AnyRef], rest)

      loop(root.asInstanceOf[AnyRef], NoResult, Nil)

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        val layer = expand(n)
        val arr = childrenArr(layer)
        var i = 0
        while i < arr.length do
          arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
          i += 1
        combine(n, layer, arr)

    n => rec(n, 0)

  /** [[foldLayered]]'s graft-aware sibling — the apomorphism engine. `expandOr` answers each node
    * event with `Left(r)` (an **already-finished result**: placed into its slot directly — O(1), no
    * recursion, no projection) or `Right(layer)` (keep going). Same on-stack / descend-bubble
    * hybrid and stack-safety as [[foldLayered]].
    */
  private[schemes] def foldLayeredOr[F[_], N, R](
      expandOr: N => Either[R, F[N]],
      combine: (F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      // Same single-loop sentinel encoding as [[foldLayered]]'s heap walk; the graft arm
      // (`Left`) feeds `pending` directly — finished, by reference.
      @tailrec def loop(op: AnyRef, pending: AnyRef, stack: List[Frame[F, N]]): R =
        if op ne Ascend then
          expandOr(op.asInstanceOf[N]) match
            case Left(r)      => loop(Ascend, r.asInstanceOf[AnyRef], stack)
            case Right(layer) =>
              val arr = childrenArr(layer)
              if arr.length == 0 then loop(Ascend, combine(layer, arr).asInstanceOf[AnyRef], stack)
              else loop(arr(0), NoResult, new Frame(null.asInstanceOf[N], layer, arr, 0) :: stack)
        else
          stack match
            case Nil        => pending.asInstanceOf[R]
            case fr :: rest =>
              fr.arr(fr.i) = pending
              fr.i += 1
              if fr.i < fr.arr.length then loop(fr.arr(fr.i), NoResult, stack)
              else loop(Ascend, combine(fr.layer, fr.arr).asInstanceOf[AnyRef], rest)

      loop(root.asInstanceOf[AnyRef], NoResult, Nil)

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        expandOr(n) match
          case Left(r)      => r // graft: finished, by reference
          case Right(layer) =>
            val arr = childrenArr(layer)
            var i = 0
            while i < arr.length do
              arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
              i += 1
            combine(layer, arr)

    n => rec(n, 0)

  // ===========================================================================================
  // The M-generic path — the foldLayered walk LIFTED into a Monad[M] (no M = Id special-case:
  // that is what makes the fast-path agreement laws a real cross-architecture pin). One
  // M-action per node event, threaded through Monad[M].tailRecM (each step paying tailRecM's
  // per-event Either — the structural B/op floor vs the pure machine). NOT droste's hyloM
  // (flatMap-recursive: O(depth) call stack on a strict M). Stack-safety reduces to the
  // lawfulness of M's tailRecM — per-M and tested (Id/Eval to 10^6).
  //
  // Supported Ms are SINGLE-PASS and LINEAR: the walk's state is mutable, so a branching /
  // replaying M (List, retrying or streaming effects) shares it across branches and corrupts
  // the fold — the documented contract, exercised by the boundary test in SchemesMSpec.
  // M must also be SEQUENTIALLY evaluated — each map/flatMap callback completes before the
  // next tailRecM step (true of Id/Eval/State/IO); async/concurrent step evaluation is
  // unsupported even for lawful Monads.
  //
  // The expand is Or-SHAPED (N => M[Either[R, F[N]]]) per the elgot-seam gate
  // (docs/brainstorms/2026-06-12-elgot-seam-sketch.md): v1 drivers always pass Right; the
  // elgot/apoM follow-up supplies Left answers with no re-architecture.
  // ===========================================================================================

  /** The lifted machine. `M.tailRecM` is the loop; each iteration handles one node event — either a
    * '''descend''' into the node carried by the loop state, or (on the [[Ascend]] sentinel) a
    * '''bubble''' step against the top frame. The mutable walk state is allocated per-force (inside
    * the `M`), so re-forcing the same `M[R]` value is safe; concurrent forcing of a single `M[R]`
    * value is not (see the object scaladoc).
    *
    * Loop-state encoding (allocation-lean — CI 2026-06-12: per-event `Either` nesting dominated the
    * M path's B/op): the state is a bare `AnyRef` — [[Ascend]] means "bubble", anything else is the
    * node to descend into; the ascend transition is a hoisted constant.
    */
  private[schemes] def foldLayeredM[M[_], F[_], N, R](
      expandOr: N => M[Either[R, F[N]]],
      combine: (N, F[N], Array[AnyRef]) => M[R],
  )(using M: Monad[M], F: Traverse[F]): N => M[R] =
    n0 =>
      M.flatMap(M.unit) { _ =>
        // ArrayDeque, not List: the M machine has no on-stack phase, so it frames EVERY
        // interior node — the deque reuses its slots across pushes (zero steady-state
        // allocation), where cons cells would cost one per node (CI-visible on eoHyloM).
        val stack = new java.util.ArrayDeque[Frame[F, N]]()
        var pending: AnyRef = null.asInstanceOf[AnyRef]
        val ascend: Either[AnyRef, R] = Left(Ascend)

        inline def bubbled(r: AnyRef): Either[AnyRef, R] =
          pending = r
          ascend

        def onDescend(n: N): M[Either[AnyRef, R]] =
          M.flatMap(expandOr(n)) {
            case Left(r) => M.pure(bubbled(r.asInstanceOf[AnyRef])) // graft / short-circuit arm
            case Right(layer) =>
              val arr = childrenArr(layer)
              if arr.length == 0 then
                // leaf: combine inline — no frame, no extra loop event
                M.map(combine(n, layer, arr))(r => bubbled(r.asInstanceOf[AnyRef]))
              else
                stack.push(new Frame(n, layer, arr, 0))
                M.pure(Left(arr(0)))
          }

        def onAscend(): M[Either[AnyRef, R]] =
          val fr = stack.peek()
          if fr == null then M.pure(Right(pending.asInstanceOf[R]))
          else
            fr.arr(fr.i) = pending // store the just-folded child's result
            fr.i += 1
            if fr.i < fr.arr.length then M.pure(Left(fr.arr(fr.i)))
            else
              // last child stored: combine now — no intermediate pure event
              M.map(combine(fr.node, fr.layer, fr.arr)) { r =>
                val _ = stack.pop()
                bubbled(r.asInstanceOf[AnyRef])
              }

        M.tailRecM[AnyRef, R](n0.asInstanceOf[AnyRef]) { op =>
          if op ne Ascend then onDescend(op.asInstanceOf[N]) else onAscend()
        }
      }

  /** Single-pass paired machine in `M` backing the fused `AnaM.andThen(CataM)` — the M mirror of
    * [[fusedPairedFold]]: each node built once, folded immediately, no `M[S]` whole-structure
    * materialization.
    */
  private[schemes] def fusedPairedFoldM[M[_], F[_], Seed, S, A](
      coalgM: Seed => M[F[Seed]],
      algM: (S, F[A]) => M[A],
  )(using M: Monad[M], F: Traverse[F], E: Embed[F, S]): Seed => M[(S, A)] =
    foldLayeredM[M, F, Seed, (S, A)](
      seed => M.map(coalgM(seed))(Right(_)),
      (_, fSeed, out) =>
        val s = E.embed(splitLayer[F, Seed, S](fSeed, out, p => p._1.asInstanceOf[S]))
        M.map(algM(s, splitLayer[F, Seed, A](fSeed, out, p => p._2.asInstanceOf[A])))(a => (s, a)),
    )

  /** The single-pass paired machine backing the fused `Ana.cross(Cata)`: each node is built once
    * (the algebra is node-supplied — construction is semantically required), folded immediately,
    * and released as the fold ascends. No full-tree retention, no second traversal.
    */
  private[schemes] def fusedPairedFold[F[_], Seed, S, A](
      coalg: Seed => F[Seed],
      alg: (S, F[A]) => A,
  )(using F: Traverse[F], E: Embed[F, S]): Seed => (S, A) =
    foldLayered[F, Seed, (S, A)](
      coalg,
      (_, fSeed, out) =>
        val s = E.embed(splitLayer[F, Seed, S](fSeed, out, p => p._1.asInstanceOf[S]))
        (s, alg(s, splitLayer[F, Seed, A](fSeed, out, p => p._2.asInstanceOf[A]))),
    )

  /** Project one half of an `(S, A)`-pair out-array straight into a typed layer — the fused
    * machines build `F[S]` and `F[A]` with two of these instead of one `F[(S, A)]` intermediate (CI
    * 2026-06-12: that third F-alloc per node put the fused cross ABOVE the materializing
    * composition in B/op, 1049k vs 886k). Leaf layers are phantom-recast (valid because
    * pattern-functor leaves have no recursive slots by definition).
    */
  private inline def splitLayer[F[_], N, T](
      fn: F[N],
      out: Array[AnyRef],
      inline half: ((Any, Any)) => T,
  )(using F: Traverse[F]): F[T] =
    if out.length == 0 then fn.asInstanceOf[F[T]]
    else
      var i = -1
      F.map(fn) { _ =>
        i += 1
        half(out(i).asInstanceOf[(Any, Any)])
      }
