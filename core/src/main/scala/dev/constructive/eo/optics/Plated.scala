package dev.constructive.eo
package optics

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

import cats.{Eval, Monad}

import data.{MultiFocus, ObjArrBuilder, PSVec}

/** A self-similar structure: a value of `S` whose immediate sub-terms are themselves `S`. The
  * single member [[plate]] is a `Traversal[S, S]` focusing those immediate children — the eo
  * analogue of Haskell `lens`'s `Plated` class (`plate :: Traversal' a a`).
  *
  * The recursion combinators in the companion ([[Plated.transform]], [[Plated.rewrite]],
  * [[Plated.children]], [[Plated.universe]]) build on `plate` to walk the *whole* tree. They are
  * **stack-safe** on deep trees: `transform` recurses on the call stack while shallow and falls
  * back to a heap-stack machine past a depth bound, `rewrite` trampolines through `cats.Eval`, and
  * `universe` uses an explicit worklist — a degenerate 100k-deep tree is fine.
  *
  * The public read is [[childrenView]], a reusable [[Children]] view (non-strict, `knownSize`,
  * `toList`, `toAnyArray`). It stores the shape the node already holds — a `List` field is shared,
  * a `List`-of-`List` is flattened lazily — and the common shapes allocate no wrapper. The
  * recursion combinators go one step lower through the internal `childrenArray` / `rebuildArray`
  * hooks: one fresh array per node, each slot overwritten with the rewritten child, then rebuilt
  * from that same array.
  *
  * Get an instance by hand via [[Plated.fromChildren]], by deriving one with
  * `dev.constructive.eo.generics.plate[S]`, or — when you have already built the self-traversal as
  * an optic — by calling `.asPlated` on it (see the extension methods below).
  *
  * @see
  *   [[Traversal.selfChildren]] for the underlying carrier.
  */
trait Plated[S]:
  /** The immediate-children self-traversal. */
  def plate: Optic[S, S, S, S, MultiFocus[PSVec]]

  /** The immediate children as a reusable, non-strict [[Children]] view — the one public
    * representation. The default reads the carrier directly (`plate.to(s).foci`); derived instances
    * and the `fromChildren*` constructors override it with the shape the node actually holds, so a
    * single `List[S]` field is handed back as is. The engine's array and the old `PSVec` view are
    * internal hooks below.
    */
  def childrenView(s: S): Children[S] = Children.fromPSVec(plate.to(s).foci)

  /** The carrier's focus vector. Internal: [[childrenView]] is the public read. */
  private[eo] def childrenVec(s: S): PSVec[S] = plate.to(s).foci

  /** A **fresh `Array[Any]` the caller may mutate in place** — the cata engine's currency.
    * Internal: derived instances override it with their fast array shape.
    */
  private[eo] def childrenArray(s: S): Array[Any] = childrenView(s).toAnyArray

  /** Reassemble `parent` with `children` swapped in (same arity / order) — the carrier's write
    * path. Internal: the public write operations are [[Plated.mapChildren]] / [[Plated.transform]].
    */
  private[eo] def rebuild(parent: S, children: PSVec[S]): S =
    val p = plate // bind once so `p.X` is a single stable existential across `to` / `from`
    p.from(MultiFocus(p.to(parent).context, children))

  /** [[rebuild]] from a children array — the engine's fast rebuild. Internal; the derived
    * `fromChildren` instance implements it from the node's own constructor.
    */
  private[eo] def rebuildArray(parent: S, children: Array[Any]): S =
    rebuild(parent, PSVec.unsafeWrap[S](children))

  /** `parent` with `g` applied to each immediate child, in order — Uniplate's `descend`, the
    * one-level rewrite [[Plated.transform]] recurses through. A leaf comes back as itself. The
    * default goes through [[childrenArray]] / [[rebuildArray]]; `generics.plate[S]` instances
    * generate it as a single pattern match that rebuilds the node from `g`'s results directly — the
    * shape of a hand-written visitor, with no children array.
    */
  def mapChildren(parent: S, g: S => S): S =
    val kids = childrenArray(parent)
    val n = kids.length
    if n == 0 then parent
    else
      @tailrec def fill(i: Int): Unit =
        if i < n then
          kids(i) = g(kids(i).asInstanceOf[S])
          fill(i + 1)
      fill(0)
      rebuildArray(parent, kids)

/** Combinators over [[Plated]] — recursion schemes faithful to `Control.Lens.Plated`, all
  * stack-safe. Each is offered both as a `using Plated[S]` method here and as an extension on any
  * self-traversal optic you build directly (so `myPlate.transformAll(f)(s)` works without a
  * typeclass).
  */
object Plated:

  /** Summon the instance. */
  def apply[S](using p: Plated[S]): Plated[S] = p

  /** Build a [[Plated]] from the children view and a rebuild function — the public hand-writing
    * entry point, and what `generics.plate[S]` emits (the four-argument overload adds the generated
    * `mapChildren` / `rebuildArray` fast paths). `children(s)` must report the immediate children
    * in the order `rebuild` expects; `rebuild(s, kids)` consumes `kids` once and reassembles `s`.
    */
  def fromChildren[S](
      children: S => Children[S],
      rebuildFn: (S, Children[S]) => S,
  ): Plated[S] =
    fromChildren(
      children,
      rebuildFn,
      (parent, g) =>
        val kids = children(parent).toList
        if kids.isEmpty then parent
        else rebuildFn(parent, Children.fromList(kids.map(g)))
      ,
      (parent, kids) => rebuildFn(parent, Children.fromArray(kids)),
    )

  /** As the two-argument [[fromChildren]], with the generated direct `mapChildren` and
    * `rebuildArray` implementations — what `generics.plate[S]` emits.
    */
  def fromChildren[S](
      children: S => Children[S],
      rebuildFn: (S, Children[S]) => S,
      mapChildrenFn: (S, S => S) => S,
      rebuildArrayFn: (S, Array[Any]) => S,
  ): Plated[S] =
    fromChildren(
      children,
      rebuildFn,
      mapChildrenFn,
      rebuildArrayFn,
      s => children(s).toAnyArray,
    )

  /** As the four-argument [[fromChildren]], with a direct `childrenArray` fast path — what
    * `generics.plate[S]` emits for an all-direct product (one fresh array, no `Children` wrapper on
    * the engine path).
    */
  def fromChildren[S](
      children: S => Children[S],
      rebuildFn: (S, Children[S]) => S,
      mapChildrenFn: (S, S => S) => S,
      rebuildArrayFn: (S, Array[Any]) => S,
      childrenArrayFn: S => Array[Any],
  ): Plated[S] =
    new SelfChildrenPlated[S]:
      override private[eo] def childrenVec(s: S): PSVec[S] = Children.toPSVec(children(s))
      override private[eo] def rebuild(parent: S, kids: PSVec[S]): S =
        rebuildFn(parent, Children.fromPSVec(kids))
      override def childrenView(s: S): Children[S] = children(s)
      override private[eo] def childrenArray(s: S): Array[Any] = childrenArrayFn(s)
      override def mapChildren(parent: S, g: S => S): S = mapChildrenFn(parent, g)
      override private[eo] def rebuildArray(parent: S, kids: Array[Any]): S =
        rebuildArrayFn(parent, kids)

  /** Internal: the PSVec-native carrier constructor (circe / avro / zio / kyo / spark rows). */
  private[eo] def fromChildrenVec[S](
      childrenFn: S => PSVec[S],
      rebuildFn: (S, PSVec[S]) => S,
  ): Plated[S] =
    new SelfChildrenPlated[S]:
      override def childrenView(s: S): Children[S] = Children.fromPSVec(childrenFn(s))
      override private[eo] def childrenVec(s: S): PSVec[S] = childrenFn(s)
      // `childrenFn` must return a *freshly-allocated* vector (every shipped instance does —
      // circe / avro / zio / kyo go through `PSVec.from`, both fresh per call).
      // `unsafeShareableArray` then hands back that backing array copy-free, and the cata engine
      // may mutate it.
      override private[eo] def childrenArray(s: S): Array[Any] =
        childrenFn(s).unsafeShareableArray
      override private[eo] def rebuild(parent: S, kids: PSVec[S]): S = rebuildFn(parent, kids)

  /** Internal: the fresh-array carrier constructor (spark schemas). `childrenFn(s)` must return a
    * newly allocated array on every call; the engine overwrites its slots in place.
    */
  private[eo] def fromChildrenArray[S](
      childrenFn: S => Array[Any],
      rebuildFn: (S, Array[Any]) => S,
  ): Plated[S] =
    new ArrayPlated[S](childrenFn, rebuildFn)

  private type ArrayChildrenFn[S] = S => Array[Any]
  private type ArrayRebuildFn[S] = (S, Array[Any]) => S

  /** A [[Plated]] whose `plate` traverses through its own `childrenVec` / `rebuild`, which every
    * subclass overrides — so reads and rebuilds skip the carrier, and the optic agrees with them.
    */
  abstract private class SelfChildrenPlated[S] extends Plated[S]:
    val plate: Optic[S, S, S, S, MultiFocus[PSVec]] = Traversal.selfChildren(childrenVec, rebuild)

  private class ArrayPlated[S](kidsOf: ArrayChildrenFn[S], build: ArrayRebuildFn[S])
      extends SelfChildrenPlated[S]:
    override def childrenView(s: S): Children[S] = Children.fromOwnedArray(kidsOf(s))
    override private[eo] def childrenVec(s: S): PSVec[S] = PSVec.unsafeWrap[S](kidsOf(s))
    override private[eo] def childrenArray(s: S): Array[Any] = kidsOf(s)

    override private[eo] def rebuild(parent: S, kids: PSVec[S]): S =
      build(parent, kids.unsafeShareableArray)

    override private[eo] def rebuildArray(parent: S, kids: Array[Any]): S = build(parent, kids)

  /** One internal node on a heap machine's stack ([[transform]] / [[transformM]] past their depth
    * budgets): the node's fresh children array, whose slot `i - 1` is overwritten with the
    * rewritten child as each one completes, so the node rebuilds from that same array.
    */
  final private class Frame[S](node: S, kids: Array[Any]):
    private var i = 0

    /** Record `done` as the rewritten previous child (if any); `true` while children remain. */
    def offer(done: Any): Boolean =
      if i > 0 then kids(i - 1) = done
      i < kids.length

    def next(): S =
      i += 1
      kids(i - 1).asInstanceOf[S]

    def rebuilt(using P: Plated[S]): S = P.rebuildArray(node, kids)

  /** Largest call-stack recursion depth [[transform]] takes before handing a subtree to the heap
    * machine. Tree *depth*, not node count — a balanced tree of a billion nodes is ~30 deep, so it
    * stays entirely on the fast recursive path; only a degenerate spine deeper than this crosses
    * into the machine. Kept well under any reasonable `-Xss` so the recursion itself can't
    * overflow.
    */
  final private val transformRecursionLimit = 512

  /** Bottom-up rewrite: rewrite every node's children first, then apply `f` to the node. The single
    * pass each `lens` user reaches for.
    *
    * Hybrid for speed without giving up stack-safety: it recurses on the JVM call stack through
    * [[Plated.mapChildren]] (≈ a hand-written recursive visitor — one match per node, no children
    * array for a derived plate) while the depth stays under [[transformRecursionLimit]], and hands
    * any deeper subtree to an explicit heap-stack post-order walk, so a degenerate spine of any
    * depth (100k+) still can't overflow.
    */
  def transform[S](f: S => S)(root: S)(using P: Plated[S]): S =
    // The recursive path is `f(P.mapChildren(s, rec))` — a derived plate's `mapChildren` is one
    // match that rebuilds the node from `rec`'s results, so a node costs what a hand-written
    // visitor's does. One `Rec` per call doubles as the child function (no closure per node) and
    // tracks the call-stack depth around each descent.
    final class Rec extends (S => S):
      private var depth = 0
      def apply(s: S): S =
        if depth >= transformRecursionLimit then machine(s) // deep: go to the heap
        else
          depth += 1
          val r = f(P.mapChildren(s, this))
          depth -= 1
          r

    // The stack-safe fallback: a post-order walk on a heap stack of [[Frame]]s, so depth is
    // bounded by heap, not the call stack. Leaves are applied in place.
    def machine(start: S): S =
      // ArrayBuffer, not java.util.ArrayDeque: Scala.js 1.22's ArrayDeque returns null from peek()
      // on a non-empty deque once it has grown and wrapped (reproduced), so the machine would NPE.
      val stack = new ArrayBuffer[Frame[S]]()
      var ret: S = start // overwritten before any read (only read once a child has completed)
      def enter(s: S): Unit =
        val kids = P.childrenArray(s)
        if kids.length == 0 then ret = f(s) else stack.append(new Frame(s, kids))
      enter(start)
      @tailrec def drive(): Unit =
        if !stack.isEmpty then
          val fr = stack.last
          if fr.offer(ret) then enter(fr.next())
          else
            ret = f(fr.rebuilt)
            val _ = stack.dropRightInPlace(1)
          drive()
      drive()
      ret

    new Rec()(root)

  /** The whole structure as a composable optic that reaches *every* sub-term — `transform` in optic
    * form. `everywhere.modify(h) == transform(h)`, so composing a downstream optic and modifying
    * rewrites that focus at every depth, bottom-up:
    *
    * {{{
    * // uppercase EVERY variable anywhere in the tree:
    * everywhere[Expr].andThen(varPrism).andThen(nameLens).modify(_.toUpperCase)(tree)
    * }}}
    *
    * It is a write-side optic (a [[Modify]] whose `modify` is the recursive [[transform]]); it
    * composes as the *outer* of `.andThen` with any inner optic that bridges into `ModifyF` (Lens /
    * Prism / Optional / …), and the composite is `transform(inner.modify(_))` by the optic
    * composition law. For the read side — every sub-term as a list — use [[universe]].
    */
  def everywhere[S](using P: Plated[S]): Modify[S, S, S, S] =
    Modify[S, S, S, S](g => s => transform(g)(s))

  /** [[everywhere]] scoped to a member sort `T <: S` of a multi-sorted `Plated[S]` union (e.g.
    * `everywhereIn[Lang, Stm]` or `everywhereIn[Schemas.Node, DataType]`): walks the whole `S` tree
    * under a `T` root and returns the rebuilt `T`, provided downstream modifications are
    * sort-preserving on `T` (as any `prism[S, Member]` composition is — a miss on non-`Member`
    * nodes is `purePlate`'s identity pass-through from O'Connor 2011 §5.1).
    */
  def everywhereIn[S, T <: S](using P: Plated[S]): Modify[T, T, S, S] =
    Modify[T, T, S, S](g => t => transform(g)(t).asInstanceOf[T])

  /** Effectful bottom-up rewrite over any single-shot lawful `Monad[M]` — Haskell `lens`'s
    * `transformM` and O'Connor's `mapFamilyM` (2011 §5.1–5.2), made stack-safe.
    *
    * Hybrid, like [[transform]]: shallow, narrow subtrees recurse through `M.flatMap` (no per-node
    * heap frame, no `tailRecM` wrapper per step); a subtree too deep or too wide for the call stack
    * goes to a post-order `ArrayBuffer`-stack heap machine driven through `M.tailRecM`, so neither
    * the descent nor the post-order rebuild overflows — as long as `M.tailRecM` is itself
    * stack-safe. That is a cats convention, not a law: every cats-provided instance honours it
    * (`Option`, `Either`, `Eval`, `State`, `Id`, … all clear a 100k-deep spine without
    * `StackOverflowError`), but a third-party `Monad` with a naive `tailRecM` will not. The
    * single-shot requirement matters too: the machine's frames are mutable, so a monad that re-runs
    * a continuation (`List`) is unsupported. Short-circuiting monads (`Option`, `Either`) abort
    * immediately on the first failing node without visiting the rest of the tree. Leaves apply `f`
    * directly without calling `rebuild`.
    */
  def transformM[M[_], S](f: S => M[S])(root: S)(using M: Monad[M], P: Plated[S]): M[S] =
    // Fast path: plain recursion through `M.flatMap`, no Frame / tailRecM wrappers. `budget`
    // counts the call-stack nesting a strict monad (`Either`, `Option`, `Id`) builds: one per tree
    // level plus one per already-visited sibling (each sibling's continuation runs inside the
    // previous one's `flatMap`). A node that would push it past the limit — too deep or too
    // wide — goes to the heap machine. Lazy monads (`Eval`, `State`) nest less; the count is a
    // safe overestimate for them.
    def rec(s: S, budget: Int): M[S] =
      val kids = P.childrenArray(s) // fresh: each slot is overwritten with its rewritten child
      val n = kids.length
      if n == 0 then f(s)
      else if budget + n >= transformMRecursionLimit then machine(s, kids)
      else
        def fill(i: Int): M[S] =
          if i < n then
            M.flatMap(rec(kids(i).asInstanceOf[S], budget + i + 1)) { c =>
              kids(i) = c
              fill(i + 1)
            }
          else f(P.rebuildArray(s, kids))
        fill(0)

    // The stack-safe fallback: [[transform]]'s heap machine, driven through `M.tailRecM` so
    // neither descent nor rebuild touches the call stack.
    def machine(start: S, startKids: Array[Any]): M[S] =
      // ArrayBuffer, not java.util.ArrayDeque: Scala.js 1.22's ArrayDeque returns null from peek()
      // on a non-empty deque once it has grown and wrapped (reproduced), so the machine would NPE.
      val stack = new ArrayBuffer[Frame[S]]()
      stack.append(new Frame(start, startKids))
      M.tailRecM[S, S](start) { last =>
        val fr = stack.last
        if fr.offer(last) then
          val child = fr.next()
          val kids = P.childrenArray(child)
          if kids.length == 0 then M.map(f(child))(Left(_))
          else
            stack.append(new Frame(child, kids))
            M.pure(Left(child))
        else
          val _ = stack.dropRightInPlace(1)
          val done = stack.isEmpty // read now: a lazy monad runs the map callback later
          M.map(f(fr.rebuilt))(r => if done then Right(r) else Left(r))
      }

    rec(root, 0)

  /** Call-stack budget for [[transformM]]'s recursive fast path — lower than
    * [[transformRecursionLimit]] because each unit costs several JVM frames (`fill` → `flatMap` →
    * continuation) under a strict monad.
    */
  final private val transformMRecursionLimit = 256

  /** Apply the rule everywhere, bottom-up, and keep re-firing on each rewritten sub-term until the
    * rule returns `None` at every node — Haskell `lens`'s `rewrite`. The caller owns termination (a
    * rule that always fires loops forever).
    *
    * Trampolined through `cats.Eval`, so it is '''fully stack-safe on both axes''': the bottom-up
    * descent over a deep tree (a 100k-deep spine is fine) '''and''' the fixpoint re-firing — a rule
    * that fires in a long chain at one position (e.g. `Counter(n) => Counter(n-1)` repeated many
    * times) bounces through the heap, not the call stack, so it won't overflow either. (This is why
    * `rewrite` keeps the `Eval` trampoline rather than reusing [[transform]]'s synchronous
    * call-stack/heap-machine the way `everywhere` does — sharing it would put the re-fire chain
    * back on the JVM call stack.)
    */
  def rewrite[S](f: S => Option[S])(s: S)(using P: Plated[S]): S =
    def step(x: S): Eval[S] = f(x).fold(Eval.now(x))(go)
    def go(x: S): Eval[S] = Eval.defer(P.plate.modifyF[Eval](go)(x)).flatMap(step)
    go(s).value

  /** The immediate sub-terms of `s` (one level down). Reads the plate's [[Plated.childrenView]]
    * directly; derived instances hand a single `List[S]` field back without copying it.
    */
  def children[S](s: S)(using P: Plated[S]): List[S] =
    P.childrenView(s).toList

  /** Every sub-term of `s`, `s` itself first, in pre-order. Stack-safe via an explicit worklist;
    * children are pushed straight off [[Plated.childrenArray]] (no per-node `List` or wrapper).
    */
  def universe[S](s: S)(using P: Plated[S]): List[S] =
    // Result built forwards (one cons per node, no reverse); worklist on a growable array stack
    // (no cons per pending node). Children are pushed right-to-left so the leftmost pops first.
    val out = ListBuffer.empty[S]
    val stack = new ObjArrBuilder(16)
    stack.append(s)
    @tailrec def loop(): Unit =
      if stack.size > 0 then
        val h = stack.pop().asInstanceOf[S]
        out += h
        val kids = P.childrenArray(h)
        @tailrec def push(i: Int): Unit =
          if i >= 0 then
            stack.append(kids(i))
            push(i - 1)
        push(kids.length - 1)
        loop()
    loop()
    out.toList

  /** The "both" surface: treat any self-traversal optic as a [[Plated]] and call the combinators on
    * it directly. Lets you skip the typeclass when you have built `plate` by hand.
    */
  extension [S](self: Optic[S, S, S, S, MultiFocus[PSVec]])

    /** Wrap this self-traversal as a [[Plated]] instance (the default [[Plated.childrenView]] reads
      * the carrier; a derived / [[Plated.fromChildren]] instance is cheaper).
      */
    def asPlated: Plated[S] =
      new Plated[S]:
        val plate: Optic[S, S, S, S, MultiFocus[PSVec]] = self

    /** [[Plated.transform]] over this optic — bottom-up rewrite of every sub-term. */
    def transformAll(f: S => S)(s: S): S = Plated.transform(f)(s)(using asPlated)

    /** [[Plated.transformM]] over this optic — effectful bottom-up rewrite of every sub-term. */
    def transformAllM[M[_]: Monad](f: S => M[S])(s: S): M[S] =
      Plated.transformM(f)(s)(using summon[Monad[M]], asPlated)

    /** [[Plated.rewrite]] over this optic — bottom-up rewrite to a fixpoint. */
    def rewriteAll(f: S => Option[S])(s: S): S = Plated.rewrite(f)(s)(using asPlated)

    /** [[Plated.children]] over this optic — the immediate sub-terms. */
    def childrenOf(s: S): List[S] = Plated.children(s)(using asPlated)

    /** [[Plated.universe]] over this optic — every sub-term, pre-order, `s` first. */
    def universeOf(s: S): List[S] = Plated.universe(s)(using asPlated)
