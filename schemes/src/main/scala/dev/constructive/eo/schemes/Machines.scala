package dev.constructive.eo
package schemes

import cats.{Monad, Traverse}

/** Internal stack-safe fold engines for the typed recursion-scheme path.
  *
  * ==Thread-safety model==
  *
  * Every machine in this object allocates its mutable state (the frame deque, the `ret` variable,
  * the per-node child/result arrays) **per invocation** — and, for the `M` path, per '''force''',
  * inside `M.flatMap(M.unit)` so that re-forcing the same `M[R]` value allocates fresh state on
  * each evaluation. No mutable state is shared across invocations or forces.
  *
  * The only shared values are immutable sentinels:
  *
  *   - [[EmptyAnyRefs]]: a zero-length `Array[AnyRef]`, shared by all leaf layers (see its own
  *     scaladoc for the immutability argument).
  *   - [[AscendToken]]: a stable identity object used as the "ascend" marker in [[foldLayeredM]]'s
  *     loop. It is never written and carries no mutable state.
  *
  * Concurrent invocations of recursion schemes in a single JVM process are therefore safe — each
  * call owns its own heap region and neither reads nor writes the shared sentinels' contents.
  *
  * Note that '''concurrent forcing of a single `M[R]` value''' is a different question (not the
  * concurrency of independent `run(s)` calls) and remains unsupported: the mutable frame deque is
  * allocated '''inside''' the `M` action, so two concurrent forces of the exact same suspended
  * `M[R]` could interleave their tailRecM steps and corrupt each other's deques. Each `run(s)` call
  * returns an independent `M[R]`, and those are safe to force concurrently.
  */
private[schemes] object Machines:

  /** Depth at which the on-stack recursion hands a subtree to the heap machine — mirrors
    * `Plated.transformRecursionLimit`. Balanced trees (depth ~log n) never reach it.
    */
  final val OnStackLimit = 512

  /** Shared zero-length children array for leaf nodes.
    *
    * '''What it is:''' a single `Array[AnyRef]` of length 0, allocated once and reused by every
    * leaf layer encountered by [[childrenArr]], [[foldLayered]], [[foldLayeredOr]], and
    * [[foldLayeredM]].
    *
    * '''Who uses it:''' [[childrenArr]] returns this value whenever `F.size(fn) == 0` (a leaf
    * layer — `LeafF`-like constructors with no recursive slots). Since leaf layers are a common
    * case in typed pattern functors, the shared sentinel avoids a per-leaf empty-array allocation.
    *
    * '''Why it is thread-safe:''' the array has length 0. The store loops in all three engines
    * (`foldLayered`, `foldLayeredOr`, `foldLayeredM`) are bounded by `arr.length`, so they
    * execute zero iterations when `arr` is [[EmptyAnyRefs]]. No element is ever written into it.
    * An immutable zero-length array is safe to share across any number of concurrent callers.
    */
  private[schemes] val EmptyAnyRefs: Array[AnyRef] = new Array[AnyRef](0)

  /** Collect the children of typed layer `fn` into a flat `Array[AnyRef]`, single-pass via
    * `ObjArrBuilder`. Returns [[EmptyAnyRefs]] for leaf layers (zero children) to avoid a per-leaf
    * empty-array allocation. Used by [[foldLayered]], [[foldLayeredOr]], and [[foldLayeredM]] — one
    * definition replaces the three identical nested `def childrenArr` that previously lived inside
    * each engine.
    *
    * Leaf layers are a common case in typed pattern functors (every `LeafF`-like constructor
    * carries no recursive slots), so the shared `EmptyAnyRefs` guard pays for itself.
    */
  private[schemes] def childrenArr[F[_], N](fn: F[N])(using F: Traverse[F]): Array[AnyRef] =
    val n = F.size(fn).toInt
    if n == 0 then EmptyAnyRefs
    else
      val b = new data.ObjArrBuilder(n)
      val _ = F.foldLeft(fn, ()) { (_, child) =>
        b.unsafeAppend(child.asInstanceOf[AnyRef])
      }
      b.freezeArr

  /** Sentinel op for [[foldLayeredM]]'s loop state — "ascend": consume `ret` against the top frame.
    * Anything else on the loop is the node to descend into.
    */
  private[schemes] object AscendToken

  /** Rebuild a typed `F[R]` from the original layer `fn: F[N]` and its children's results, stored
    * positionally in `out` in `Foldable` order — which `Functor.map` matches for a lawful
    * `Traverse`. Lets the schemes hand the algebra a typed `F[R]` (named constructors) rather than
    * a positional vector.
    */
  private[schemes] def rebuildLayer[F[_], N, R](fn: F[N], out: Array[AnyRef])(using
      F: Traverse[F]
  ): F[R] =
    if out.length == 0 then
      fn.asInstanceOf[F[R]] // leaf: no N-slots, so F[N] is phantom-recast to F[R]
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
    if out.length == 0 then fn.asInstanceOf[F[(N, R)]] // leaf: no N-slots, phantom-recast
    else
      var i = -1
      F.map(fn) { n =>
        i += 1
        (n, out(i).asInstanceOf[R])
      }

  /** Shared typed engine for the `F`-path schemes. `expand` peels a node into one typed layer of
    * child nodes; the engine folds each child to an `R` (post-order), then calls `combine` with the
    * node, its layer `F[N]`, and the children's results (positional, `Foldable` order). `combine`
    * rebuilds the typed `F[R]` via [[rebuildLayer]] and applies the user's algebra / embed. Same
    * `< 512`-on-stack / heap-`ArrayDeque` hybrid (and stack-safety) as [[unfoldFold]] /
    * [[foldInPlace]]; the per-node child array is reused as the result accumulator (folded in
    * place).
    */
  private[schemes] def foldLayered[F[_], N, R](
      expand: N => F[N],
      combine: (N, F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      final class Frame(val node: N, val layer: F[N], val arr: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit =
        val layer = expand(n)
        val arr = childrenArr(layer)
        if arr.length == 0 then ret = combine(n, layer, arr).asInstanceOf[AnyRef]
        else stack.push(new Frame(n, layer, arr, 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret // overwrite the just-folded child's slot
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[N]
          fr.i += 1
          enter(child)
        else
          ret = combine(fr.node, fr.layer, fr.arr).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        val layer = expand(n)
        val arr = childrenArr(layer)
        val k = arr.length
        if k == 0 then combine(n, layer, arr)
        else
          var i = 0
          while i < k do
            arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
            i += 1
          combine(n, layer, arr)

    n => rec(n, 0)

  /** [[foldLayered]]'s graft-aware sibling — the apomorphism engine. `expandOr` answers each node
    * event with `Left(r)` (an **already-finished result**: placed into its slot directly — O(1), no
    * recursion, no projection) or `Right(layer)` (keep going). Same `< 512`-on-stack /
    * heap-`ArrayDeque` hybrid and stack-safety as [[foldLayered]].
    */
  private[schemes] def foldLayeredOr[F[_], N, R](
      expandOr: N => Either[R, F[N]],
      combine: (F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def heap(root: N): R =
      final class Frame(val layer: F[N], val arr: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit = expandOr(n) match
        case Left(r)      => ret = r.asInstanceOf[AnyRef] // graft: finished, by reference
        case Right(layer) =>
          val arr = childrenArr(layer)
          if arr.length == 0 then ret = combine(layer, arr).asInstanceOf[AnyRef]
          else stack.push(new Frame(layer, arr, 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[N]
          fr.i += 1
          enter(child)
        else
          ret = combine(fr.layer, fr.arr).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[R]

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        expandOr(n) match
          case Left(r)      => r // graft: finished, by reference
          case Right(layer) =>
            val arr = childrenArr(layer)
            val k = arr.length
            if k == 0 then combine(layer, arr)
            else
              var i = 0
              while i < k do
                arr(i) = rec(arr(i).asInstanceOf[N], depth + 1).asInstanceOf[AnyRef]
                i += 1
              combine(layer, arr)

    n => rec(n, 0)

  // ===========================================================================================
  // The M-generic path — the foldLayered state machine LIFTED into a Monad[M] (no M = Id
  // special-case: that is what makes the fast-path agreement laws a real cross-architecture
  // pin). State = the explicit frame deque, threaded through Monad[M].tailRecM, one iteration
  // per node event (each paying tailRecM's per-step Either — the structural B/op floor vs the
  // pure machine). NOT droste's hyloM (flatMap-recursive: O(depth) call stack on a strict M).
  // Stack-safety reduces to the lawfulness of M's tailRecM — per-M and tested (Id/Eval to 10^6).
  //
  // Supported Ms are SINGLE-PASS and LINEAR: the machine's state is mutable, so a branching /
  // replaying M (List, retrying or streaming effects) shares it across branches and corrupts
  // the fold — the documented contract, exercised by the boundary test in SchemesMSpec.
  // M must also be SEQUENTIALLY evaluated — each map/flatMap callback completes before the next
  // tailRecM step (true of Id/Eval/State/IO); async/concurrent step evaluation is unsupported
  // even for lawful Monads.
  //
  // The expand is Or-SHAPED (N => M[Either[R, F[N]]]) per the elgot-seam gate
  // (docs/brainstorms/2026-06-12-elgot-seam-sketch.md): v1 drivers always pass Right; the
  // elgot/apoM follow-up supplies Left answers with no re-architecture.
  // ===========================================================================================

  /** The lifted machine. One `M`-action per `tailRecM` iteration: `Down(n)` runs `expandOr`, exits
    * run `combine`; the mutable frame deque is allocated per-force (inside the `M`) so re-forcing
    * the same `M[R]` value allocates fresh state. Concurrent forcing of a single `M[R]` value
    * remains unsupported (mutable state, linear-M contract); each `run(s)` call is independent.
    */
  private[schemes] def foldLayeredM[M[_], F[_], N, R](
      expandOr: N => M[Either[R, F[N]]],
      combine: (N, F[N], Array[AnyRef]) => M[R],
  )(using M: Monad[M], F: Traverse[F]): N => M[R] =

    final class Frame(val node: N, val layer: F[N], val arr: Array[AnyRef], var i: Int)

    n0 =>
      M.flatMap(M.unit) { _ =>
        val stack = new java.util.ArrayDeque[Frame]()
        var ret: AnyRef = null.asInstanceOf[AnyRef]
        // Op encoding (allocation-lean — CI 2026-06-12: per-event Either allocation
        // dominated the M path's 1.6M B/op): the loop state is a bare AnyRef — the
        // AscendToken sentinel means "consume ret against the top frame", anything else
        // is the node to descend into. One Left per descend (vs nested Left(Right(n)));
        // the ascend step is the hoisted constant.
        val ascend: Either[AnyRef, R] = Left(AscendToken)
        M.tailRecM[AnyRef, R](n0.asInstanceOf[AnyRef]) { op =>
          if op.asInstanceOf[AnyRef] ne AscendToken then
            val n = op.asInstanceOf[N]
            M.flatMap(expandOr(n)) {
              case Left(r) => // graft/short-circuit arm (unused by v1 drivers)
                ret = r.asInstanceOf[AnyRef]
                M.pure(ascend)
              case Right(layer) =>
                val arr = childrenArr(layer)(using F)
                if arr.length == 0 then
                  // leaf: combine INLINE (a constant second bind — no frame, no extra event)
                  M.map(combine(n, layer, arr)) { r =>
                    ret = r.asInstanceOf[AnyRef]
                    ascend
                  }
                else
                  stack.push(new Frame(n, layer, arr, 0))
                  M.pure(Left(arr(0)))
            }
          else if stack.isEmpty then M.pure(Right(ret.asInstanceOf[R]))
          else
            val fr = stack.peek()
            fr.arr(fr.i) = ret // store the just-folded child's result
            fr.i += 1
            if fr.i < fr.arr.length then M.pure(Left(fr.arr(fr.i)))
            else
              // last child stored: combine NOW (merged — no intermediate pure event)
              M.map(combine(fr.node, fr.layer, fr.arr)) { r =>
                val _ = stack.pop()
                ret = r.asInstanceOf[AnyRef]
                ascend
              }
        }
      }

  /** Single-pass paired machine in `M` backing the fused `AnaM.andThen(CataM)` — the M mirror of
    * [[fusedPairedFold]]: each node built once, folded immediately, no `M[S]` whole-structure
    * materialization. Mirrors the pure version exactly: `F[S]` and `F[A]` are built straight from
    * the out-array with two `F.map(fSeed)` passes and `var i = -1` counters, avoiding the
    * `F[(S,A)]` intermediate. Leaf layers are phantom-recast (valid because pattern-functor leaves
    * have no recursive slots by definition).
    */
  private[schemes] def fusedPairedFoldM[M[_], F[_], Seed, S, A](
      coalgM: Seed => M[F[Seed]],
      algM: (S, F[A]) => M[A],
  )(using M: Monad[M], F: Traverse[F], E: Embed[F, S]): Seed => M[(S, A)] =
    foldLayeredM[M, F, Seed, (S, A)](
      seed => M.map(coalgM(seed))(Right(_)),
      (_, fSeed, out) =>
        // Build F[S] and F[A] straight from the out-array — no F[(S,A)] intermediate.
        val fS =
          if out.length == 0 then fSeed.asInstanceOf[F[S]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._1
            }
        val fA =
          if out.length == 0 then fSeed.asInstanceOf[F[A]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._2
            }
        val s = E.embed(fS)
        M.map(algM(s, fA))(a => (s, a)),
    )

  /** The single-pass paired machine backing the fused `Ana.cross(Cata)`: each node is built once
    * (the algebra is node-supplied — construction is semantically required), folded immediately,
    * and released as the fold ascends. No full-tree retention, no second traversal. Leaf layers are
    * phantom-recast (valid because pattern-functor leaves have no recursive slots by definition).
    */
  private[schemes] def fusedPairedFold[F[_], Seed, S, A](
      coalg: Seed => F[Seed],
      alg: (S, F[A]) => A,
  )(using F: Traverse[F], E: Embed[F, S]): Seed => (S, A) =
    foldLayered[F, Seed, (S, A)](
      coalg,
      (_, fSeed, out) =>
        // Build F[S] and F[A] straight from the out-array — no F[(S, A)] intermediate
        // (CI 2026-06-12: that third F-alloc per node put the fused cross ABOVE the
        // materializing composition in B/op, 1049k vs 886k).
        val fS =
          if out.length == 0 then fSeed.asInstanceOf[F[S]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._1
            }
        val fA =
          if out.length == 0 then fSeed.asInstanceOf[F[A]]
          else
            var i = -1
            F.map(fSeed) { _ =>
              i += 1
              out(i).asInstanceOf[(S, A)]._2
            }
        val s = E.embed(fS)
        (s, alg(s, fA)),
    )
