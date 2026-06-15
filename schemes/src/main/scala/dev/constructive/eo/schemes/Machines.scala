package dev.constructive.eo
package schemes

import scala.annotation.tailrec

import cats.{Monad, Traverse}

/** Internal stack-safe fold engines for the typed recursion-scheme path.
  *
  * ==Shape==
  *
  * Every engine is the same two-phase walk — '''descend''' (peel a layer, push a [[Frame]] per
  * interior node onto the frame stack) and '''bubble''' (store a finished child's result into the
  * top frame's slot, then resume at the next sibling or combine the completed frame) — expressed as
  * ONE `@tailrec` loop whose state is the [[Ascend]] sentinel encoding shared with
  * [[foldLayeredM]]'s `tailRecM` loop (two mutually-recursive phase functions would grow the JVM
  * stack on their cross-calls; a single self-tail-recursive loop compiles to a jump). The pure
  * machines share their walk ([[heapWalk]]); the `M` machine is the same walk threaded through
  * `tailRecM`.
  *
  * Slot buffers are union-typed ([[Slot]] = `N | R`): a cell starts life as the child node and is
  * overwritten in place by that child's result, so stores are cast-free and each phase narrows its
  * reads at one documented point. Below [[OnStackLimit]] the engines use plain tree recursion
  * instead (the natural functional expression of a fold, and the allocation-free hot path); only
  * deep subtrees pay for frames.
  *
  * ==Thread-safety model==
  *
  * Every machine allocates its mutable state (the frame stack, the per-node slot buffers) '''per
  * invocation''' — and, for the `M` path, per '''force''', inside `M.flatMap(M.unit)` so that
  * re-forcing the same `M[R]` value allocates fresh state on each evaluation. No mutable state is
  * shared across invocations or forces.
  *
  * The only shared values are immutable sentinels:
  *
  *   - [[NoChildren]]: a zero-length slot buffer, shared by all leaf layers (see its own scaladoc
  *     for the immutability argument).
  *   - [[Ascend]] / [[NoResult]]: stable identity objects marking the loop's bubble states. Never
  *     written, no mutable state.
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

  /** One child/result buffer cell: starts life as the child node `N`, overwritten in place by that
    * child's folded result `R`. The walk's index discipline decides which half is live — cells
    * below a frame's `next` hold results, cells at and above it still hold children — so stores are
    * cast-free (`N <: Slot` and `R <: Slot`) and each phase narrows its reads at one point.
    */
  private[schemes] type Slot[N, R] = N | R

  /** "Bubble" loop-state marker: consume the pending result against the top frame. */
  private[schemes] object Ascend

  /** Loop op state: the node to descend into, or [[Ascend]]. */
  private[schemes] type Op[N] = N | Ascend.type

  /** Placeholder for the pending-result state while descending (no result is in flight). Never read
    * — `pending` is consumed only on the [[Ascend]] arm, which is reached only after a real result
    * was threaded in.
    */
  private[schemes] object NoResult

  /** The pending-result loop state: a result bubbling up, or [[NoResult]] while descending. */
  private[schemes] type Pending[R] = R | NoResult.type

  // === The engine's only unchecked narrowings ================================================
  // Abstract type params `N` / `R` have NO runtime type test — `case n: N` is rejected under
  // -Werror ("the type test for N cannot be checked at runtime"), and Scala 3 does not subtract
  // a matched singleton from a union binder (`N | Ascend.type` minus `Ascend.type` is not `N`).
  // So narrowing a Slot / Op / Pending to its live half is necessarily an `asInstanceOf`. Every
  // such cast lives HERE, `inline` (zero overhead — same bytecode as the bare cast) and named by
  // the walk invariant it relies on, instead of scattered through the loop bodies. These five are
  // the engine's complete unchecked-narrowing surface; the two remaining casts (the builder
  // widening + the array-erasure reinterpret in `childrenSlots`) are always-safe boundary casts,
  // not narrowings.

  /** A slot still holding its child node — the walk reads this only at indices `>= frame.next`
    * (slots below `next` already hold results), or for a freshly-built layer's slot 0.
    */
  private inline def childAt[N, R](slot: Slot[N, R]): N = slot.asInstanceOf[N]

  /** A slot holding a fold result — the walk reads this only at indices `< frame.next`. */
  private inline def resultAt[N, R](slot: Slot[N, R]): R = slot.asInstanceOf[R]

  /** The descend target carried by the loop op — read only on the non-[[Ascend]] arm. */
  private inline def nodeOf[N](op: Op[N]): N = op.asInstanceOf[N]

  /** The pending result — read only on the [[Ascend]] arm, reached only after a real result was
    * threaded in (so `pending` is never [[NoResult]] here).
    */
  private inline def forced[R](pending: Pending[R]): R = pending.asInstanceOf[R]

  /** Phantom-recast an empty leaf layer `F[A]` to `F[B]` — valid because a pattern-functor leaf has
    * no recursive slots, so no `A` is ever read as a `B`. Avoids the `F.map` reallocation a leaf
    * would otherwise pay (the leaf is the most common node; this is allocation-pinned).
    */
  private inline def leafRecast[F[_], A, B](fn: F[A]): F[B] = fn.asInstanceOf[F[B]]

  /** The shared "this layer has no children" sentinel.
    *
    * '''What it is:''' a single zero-length slot buffer, allocated once and returned by
    * [[childrenSlots]] for every leaf layer (`LeafF`-like constructors with no recursive slots).
    *
    * '''Who uses it:''' every engine, via [[childrenSlots]] — leaf layers are the most common case
    * in typed pattern functors, so the shared sentinel avoids a per-leaf empty-array allocation.
    *
    * '''Why it is thread-safe:''' the array has length 0 and no element is ever written into it —
    * every store in the engines targets `slots(i)` for `i < slots.length`. An immutable zero-length
    * array is safe to share across any number of concurrent walks.
    */
  private val NoChildren: Array[AnyRef] = new Array[AnyRef](0)

  /** Collect the children of typed layer `fn` into a flat slot buffer, single-pass via
    * `ObjArrBuilder`; [[NoChildren]] for leaf layers. (`Array[Slot[N, R]]` erases to
    * `Array[AnyRef]` — the recast here is the buffer's single allocation-site cast; every
    * subsequent store is union-typed.)
    */
  private[schemes] def childrenSlots[F[_], N, R](fn: F[N])(using
      F: Traverse[F]
  ): Array[Slot[N, R]] =
    val n = F.size(fn).toInt
    val raw =
      if n == 0 then NoChildren
      else
        val b = new data.ObjArrBuilder(n)
        val _ = F.foldLeft(fn, ()) { (_, child) =>
          b.unsafeAppend(child.asInstanceOf[AnyRef])
        }
        b.freezeArr
    raw.asInstanceOf[Array[Slot[N, R]]]

  /** One suspended interior node: its layer, the child/result slot buffer, and the index of the
    * next slot awaiting a result (slots below `next` hold results, slots at and above it still hold
    * children).
    */
  final private class Frame[F[_], N, R](
      val node: N,
      val layer: F[N],
      val slots: Array[Slot[N, R]],
      var next: Int,
  )

  /** Rebuild a typed `F[R]` from the original layer `fn: F[N]` and its children's results, stored
    * positionally in `out` in `Foldable` order — which `Functor.map` matches for a lawful
    * `Traverse`. Lets the schemes hand the algebra a typed `F[R]` (named constructors) rather than
    * a positional vector. Leaf layers are phantom-recast (valid because pattern-functor leaves have
    * no recursive slots by definition); non-leaf reads narrow the slot union (every cell holds an
    * `R` by the time a layer is rebuilt).
    */
  private[schemes] def rebuildLayer[F[_], N, R](fn: F[N], out: Array[Slot[N, R]])(using
      F: Traverse[F]
  ): F[R] =
    if out.length == 0 then leafRecast(fn)
    else
      var i = -1
      F.map(fn) { _ =>
        i += 1
        resultAt(out(i))
      }

  /** The descend/bubble heap walk shared by [[foldLayered]] and [[foldLayeredOr]] (previously two
    * near-identical loops). `expandOr`'s `Left` arm is the graft/short-circuit channel —
    * [[foldLayered]] instantiates it with a constant `Right`. This walk runs only past
    * [[OnStackLimit]] — the cold path (the hot on-stack recursion stays specialized in each
    * engine), so the step parameters are ordinary functions; nothing here is hot enough for
    * inlining to matter.
    *
    * The loop body delegates to two `transparent inline` phase helpers; their `loop` calls are in
    * tail position after inlining, so `@tailrec` still verifies.
    */
  private def heapWalk[F[_], N, R](
      root: N,
      expandOr: N => Either[R, F[N]],
      combine: (N, F[R]) => R,
  )(using F: Traverse[F]): R =
    @tailrec def loop(op: Op[N], pending: Pending[R], stack: List[Frame[F, N, R]]): R =

      transparent inline def descend(n: N): R = expandOr(n) match
        case Left(finished) => loop(Ascend, finished, stack) // graft: finished, by reference
        case Right(layer)   =>
          val slots = childrenSlots[F, N, R](layer)
          if slots.length == 0 then loop(Ascend, combine(n, rebuildLayer(layer, slots)), stack)
          else loop(childAt(slots(0)), NoResult, new Frame(n, layer, slots, 0) :: stack)

      transparent inline def bubble: R = stack match
        case Nil        => forced(pending) // the walk's final result
        case fr :: rest =>
          fr.slots(fr.next) = forced(pending) // overwrite the just-folded child's slot
          fr.next += 1
          if fr.next < fr.slots.length then loop(childAt(fr.slots(fr.next)), NoResult, stack)
          else loop(Ascend, combine(fr.node, rebuildLayer(fr.layer, fr.slots)), rest)

      op match
        case Ascend => bubble
        case n      => descend(nodeOf(n)) // the op union's other inhabitant is the node

    loop(root, NoResult, Nil)

  /** Shared typed engine for the `F`-path schemes. `expand` peels a node into one typed layer of
    * child nodes; the engine folds each child to an `R` (post-order), rebuilds the layer's results
    * into a typed `F[R]` (named constructors — the raw `Slot` buffer never leaves the engine), then
    * calls `combine` with the node and that `F[R]`. `< [[OnStackLimit]]` deep: plain tree
    * recursion; past it, the shared [[heapWalk]]. Stack-safe for any *terminating* `expand` (a
    * non-terminating one exhausts the heap — `OutOfMemoryError` — rather than the stack).
    */
  private[schemes] def foldLayered[F[_], N, R](
      expand: N => F[N],
      combine: (N, F[R]) => R,
  )(using F: Traverse[F]): N => R =

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heapWalk(n, m => Right(expand(m)), combine)
      else
        val layer = expand(n)
        val slots = childrenSlots[F, N, R](layer)
        var i = 0
        while i < slots.length do
          slots(i) = rec(childAt(slots(i)), depth + 1)
          i += 1
        combine(n, rebuildLayer(layer, slots))

    n => rec(n, 0)

  /** The unfold driver — [[foldLayered]] with the combine fixed to `Embed`, the shape every
    * non-grafting unfold shares (`ana` / `futu` / `cozygo` / `comutu`, and the unfold half of the
    * metamorphisms): peel each seed with `expand`, glue each rebuilt layer back with `embed`.
    */
  private[schemes] def buildLayered[F[_], N, S](expand: N => F[N])(using
      F: Traverse[F],
      E: Embed[F, S],
  ): N => S =
    foldLayered[F, N, S](expand, (_, fr) => E.embed(fr))

  /** [[foldLayered]]'s graft-aware sibling — the apomorphism engine. `expandOr` answers each node
    * event with `Left(r)` (an **already-finished result**: placed into its slot directly — O(1), no
    * recursion, no projection) or `Right(layer)` (keep going). Same on-stack / [[heapWalk]] hybrid
    * and stack-safety as [[foldLayered]].
    */
  private[schemes] def foldLayeredOr[F[_], N, R](
      expandOr: N => Either[R, F[N]],
      combine: F[R] => R,
  )(using F: Traverse[F]): N => R =

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heapWalk(n, expandOr, (_, fr) => combine(fr))
      else
        expandOr(n) match
          case Left(r)      => r // graft: finished, by reference
          case Right(layer) =>
            val slots = childrenSlots[F, N, R](layer)
            var i = 0
            while i < slots.length do
              slots(i) = rec(childAt(slots(i)), depth + 1)
              i += 1
            combine(rebuildLayer(layer, slots))

    n => rec(n, 0)

  // ===========================================================================================
  // The M-generic path — the heapWalk LIFTED into a Monad[M] (no M = Id special-case: that is
  // what makes the fast-path agreement laws a real cross-architecture pin). One M-action per
  // node event, threaded through Monad[M].tailRecM (each step paying tailRecM's per-event
  // Either — the structural B/op floor vs the pure machine). NOT droste's hyloM
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
    * M path's B/op): the state is an [[Op]] — [[Ascend]] means "bubble", anything else is the node
    * to descend into; the ascend transition is a hoisted constant. The frame stack is an
    * `ArrayDeque`, not a `List`: this machine has no on-stack phase, so it frames EVERY interior
    * node — deque slot reuse is CI-visible (List conses cost ~+98k B/op on eoHyloM).
    */
  private[schemes] def foldLayeredM[M[_], F[_], N, R](
      expandOr: N => M[Either[R, F[N]]],
      combine: (N, F[R]) => M[R],
  )(using M: Monad[M], F: Traverse[F]): N => M[R] =
    n0 =>
      M.flatMap(M.unit) { _ =>
        val stack = new java.util.ArrayDeque[Frame[F, N, R]]()
        var pending: Pending[R] = NoResult
        val ascend: Either[Op[N], R] = Left(Ascend)

        inline def bubbled(r: R): Either[Op[N], R] =
          pending = r
          ascend

        def onDescend(n: N): M[Either[Op[N], R]] =
          M.flatMap(expandOr(n)) {
            case Left(finished) => M.pure(bubbled(finished)) // graft / short-circuit arm
            case Right(layer)   =>
              val slots = childrenSlots[F, N, R](layer)
              if slots.length == 0 then
                // leaf: combine inline — no frame, no extra loop event
                M.map(combine(n, rebuildLayer(layer, slots)))(bubbled)
              else
                stack.push(new Frame(n, layer, slots, 0))
                M.pure(Left(childAt(slots(0))))
          }

        def onAscend(): M[Either[Op[N], R]] =
          val fr = stack.peek()
          if fr == null then M.pure(Right(forced(pending)))
          else
            fr.slots(fr.next) = forced(pending) // store the just-folded child's result
            fr.next += 1
            if fr.next < fr.slots.length then M.pure(Left(childAt(fr.slots(fr.next))))
            else
              // last child stored: combine now — no intermediate pure event
              M.map(combine(fr.node, rebuildLayer(fr.layer, fr.slots))) { r =>
                val _ = stack.pop()
                bubbled(r)
              }

        M.tailRecM[Op[N], R](n0) { op =>
          op match
            case Ascend => onAscend()
            case n      => onDescend(nodeOf(n))
        }
      }
