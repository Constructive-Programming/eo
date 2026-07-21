package dev.constructive.eo
package schemes

import scala.annotation.tailrec

import java.util.ArrayDeque

import data.PSVec
import optics.{Getter, Plated, Review, Unfold}

/** Recursion schemes as composable optics, built on the core optic surface.
  *
  *   - [[cata]] is a `Getter[S, A]` driven by `Plated[S]` — the structural fold, generalising
  *     `Plated.transform` from `S => S` to `S => A`.
  *   - [[ana]] is a `Review[S, Seed]` — the unfold (build `S` from a seed), taking a [[Coalg]].
  *   - [[hylo]] is a **fused** `Getter[Seed, A]` — refold with **no intermediate `S`** built.
  *
  * Because they produce core optic types, they compose with the rest of the optic algebra:
  * `someLens.andThen(cata(alg))`, and the materializing `ana(…).cross(cata(…))` (via the core
  * `Optic.cross` combinator) — the latter equal to `hylo` on the same computation (the hylo law).
  *
  * Two usage modes. Run the optic directly (`cata(alg).get(tree)`, `ana(coalg).reverseGet(seed)`,
  * `hylo(expand, alg).get(seed)`) — or hand it to capability-consuming code: the concrete optic
  * types implement the capability traits, so a [[cata]] / [[hylo]] result satisfies `CanGet[S, A]`
  * (and `CanFold[S, A]`) and an [[ana]] result satisfies `CanReverseGet[S, Seed]`, meaning a
  * consuming signature like
  * {{{
  * def report[S](s: S)(using g: CanGet[S, Int]): String
  * }}}
  * accepts a catamorphism without ever naming `Getter`.
  *
  * The *fold* schemes ([[cata]] / [[hylo]]) take an algebra `(N, PSVec[R]) => R` — a node plus its
  * already-folded children (paramorphism-flavored). The *build* scheme ([[ana]]) takes a [[Coalg]],
  * the canonical anamorphism shape: a seed yields its child seeds together with how to assemble the
  * node. Both run on one stack-safe engine: a `< 512`-deep on-stack fast path (no heap frames) that
  * falls back, per deep subtree, to a heap `ArrayDeque` machine — the same hybrid as
  * `Plated.transform`. Shallow trees pay no frame allocation; arbitrarily deep ones stay
  * stack-safe.
  */
object Schemes:

  /** Closure-carrying coalgebra (the anamorphism input): a seed yields its child seeds plus a
    * combiner from the built/folded child results. A leaf is `(PSVec.empty, _ => value)`. The
    * combiner is handed a `PSVec[R]` of the same length and order as the child-seed vector — index
    * it consistently with that arity (reading `kids(1)` of a 1-element vector throws
    * `IndexOutOfBounds`; ignoring `kids(2)` of a 3-element one silently drops that subtree). A
    * builder that captures nothing (e.g. `ks => Node(ks(0), ks(1))`) is a singleton in Scala 3, so
    * the per-node cost of this bundled shape is just the tuple.
    */
  type Coalg[N, R] = N => (PSVec[N], PSVec[R] => R)

  /** Depth at which the on-stack recursion hands a subtree to the heap machine — mirrors
    * `Plated.transformRecursionLimit`. Balanced trees (depth ~log n) never reach it.
    */
  final private val OnStackLimit = 512

  /** Engine for the *build* scheme ([[ana]]) and — via a per-node `(kids, combine)` bundling of
    * `expand` + `alg` — for the fused [[hylo]]. One [[Coalg]] call per node yields its children and
    * its combiner closure (stored in the frame on the heap path). On-stack fast path below
    * [[OnStackLimit]], heap `ArrayDeque` machine past it; stack-safe for any terminating coalgebra.
    */
  private def unfoldCoalg[N, R](coalg: Coalg[N, R]): N => R =
    n0 => unfoldCoalgRec(coalg, n0, 0)

  /** On-stack fast path for [[unfoldCoalg]]: one [[Coalg]] call per node yields its children and
    * combiner; recurses directly up to [[OnStackLimit]], then defers to [[unfoldCoalgHeap]]. The
    * inner `@tailrec loop` fills the child-result slots left to right.
    */
  private def unfoldCoalgRec[N, R](coalg: Coalg[N, R], n: N, depth: Int): R =
    if depth >= OnStackLimit then unfoldCoalgHeap(coalg, n)
    else
      val (kids, combine) = coalg(n)
      val k = kids.length
      if k == 0 then combine(PSVec.empty[R])
      else
        val out = new Array[AnyRef](k)
        @tailrec def loop(i: Int): Unit =
          if i < k then
            out(i) = unfoldCoalgRec(coalg, kids(i), depth + 1).asInstanceOf[AnyRef]
            loop(i + 1)
        loop(0)
        combine(PSVec.unsafeWrap[R](out))

  /** Heap trampoline for [[unfoldCoalg]]: an explicit `ArrayDeque` post-order walk with each node's
    * combiner closure stored in its frame. `enter` and the `@tailrec loop` driver share the one
    * `stack` + `ret` cell; `loop`'s self-call stays in tail position for stack-safety.
    */
  private def unfoldCoalgHeap[N, R](coalg: Coalg[N, R], root: N): R =
    final class Frame(
        val combine: PSVec[R] => R,
        val kids: PSVec[N],
        val out: Array[AnyRef],
        var i: Int,
    )
    val stack = new ArrayDeque[Frame]()
    var ret: AnyRef = null.asInstanceOf[AnyRef]
    def enter(n: N): Unit =
      val (kids, combine) = coalg(n)
      if kids.isEmpty then ret = combine(PSVec.empty[R]).asInstanceOf[AnyRef]
      else stack.push(new Frame(combine, kids, new Array[AnyRef](kids.length), 0))
    enter(root)
    @tailrec def loop(): R =
      if stack.isEmpty then ret.asInstanceOf[R]
      else
        val fr = stack.peek()
        if fr.i > 0 then fr.out(fr.i - 1) = ret
        if fr.i < fr.kids.length then
          val child = fr.kids(fr.i)
          fr.i += 1
          enter(child)
        else
          ret = fr.combine(PSVec.unsafeWrap[R](fr.out)).asInstanceOf[AnyRef]
          val _ = stack.pop()
        loop()
    loop()

  /** In-place fold engine for [[cata]]. `childrenOf` returns a **fresh, owned** `Array[AnyRef]` of
    * the node's children (via `Plated.childrenArray`); the engine folds each child and **overwrites
    * its slot with the result**, reusing that one array as the result accumulator instead of
    * allocating a separate out-array per node — then wraps it once for `alg`. Same on-stack/heap
    * hybrid and stack-safety as [[unfoldCoalg]]. Safe because `childrenArray`'s contract guarantees
    * the array is freshly allocated and not aliased.
    */
  private def foldInPlace[S, A](childrenOf: S => Array[AnyRef], alg: (S, PSVec[A]) => A): S => A =
    s0 => foldInPlaceRec(childrenOf, alg, s0, 0)

  /** On-stack fast path for [[foldInPlace]]: post-order recursion up to [[OnStackLimit]] that folds
    * each child **into its own slot** of the freshly-owned children array (reusing it as the result
    * accumulator), then defers deep subtrees to [[foldInPlaceHeap]].
    */
  private def foldInPlaceRec[S, A](
      childrenOf: S => Array[AnyRef],
      alg: (S, PSVec[A]) => A,
      s: S,
      depth: Int,
  ): A =
    if depth >= OnStackLimit then foldInPlaceHeap(childrenOf, alg, s)
    else
      val arr = childrenOf(s)
      val k = arr.length
      if k == 0 then alg(s, PSVec.empty[A])
      else
        @tailrec def loop(i: Int): Unit =
          if i < k then
            val child = arr(i).asInstanceOf[S]
            arr(i) = foldInPlaceRec(childrenOf, alg, child, depth + 1).asInstanceOf[AnyRef]
            loop(i + 1)
        loop(0)
        alg(s, PSVec.unsafeWrap[A](arr))

  /** Heap trampoline for [[foldInPlace]]: the [[unfoldCoalgHeap]] walk specialised to overwrite
    * each child's slot in the owned array with its fold result (no separate out-array). `enter` and
    * the `@tailrec loop` driver share the one `stack` + `ret` cell; `loop`'s self-call stays in
    * tail position for stack-safety.
    */
  private def foldInPlaceHeap[S, A](
      childrenOf: S => Array[AnyRef],
      alg: (S, PSVec[A]) => A,
      root: S,
  ): A =
    final class Frame(val node: S, val arr: Array[AnyRef], var i: Int)
    val stack = new ArrayDeque[Frame]()
    var ret: AnyRef = null.asInstanceOf[AnyRef]
    def enter(s: S): Unit =
      val arr = childrenOf(s)
      if arr.length == 0 then ret = alg(s, PSVec.empty[A]).asInstanceOf[AnyRef]
      else stack.push(new Frame(s, arr, 0))
    enter(root)
    @tailrec def loop(): A =
      if stack.isEmpty then ret.asInstanceOf[A]
      else
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret // overwrite the just-folded child's slot
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[S]
          fr.i += 1
          enter(child)
        else
          ret = alg(fr.node, PSVec.unsafeWrap[A](fr.arr)).asInstanceOf[AnyRef]
          val _ = stack.pop()
        loop()
    loop()

  /** Catamorphism as a composable `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children.
    *
    * Stack-safety contract: below the 512-frame on-stack limit the fold recurses directly on the
    * JVM stack (no heap frames); past it, each deep subtree is handed to a heap `ArrayDeque`
    * machine — so any *finite* tree folds without `StackOverflowError`, at any depth, and a
    * `Plated` whose children never bottom out fails by exhausting the heap (`OutOfMemoryError`),
    * not the stack. Folds child results in place (see the private `foldInPlace` engine) so it
    * allocates one array per node, not two.
    */
  def cata[S, A](alg: (S, PSVec[A]) => A)(using P: Plated[S]): Getter[S, A] =
    Getter[S, A](foldInPlace[S, A](P.childrenArray, alg))

  /** Catamorphism from a build-only optic citizen: a *pure* algebra `PSVec[A] => A` carried as an
    * [[dev.constructive.eo.optics.Unfold]], so an algebra built by optic composition
    * (`review.andThen(unfold)`, `unfold.andThen(review)`) drops straight into the fold engine.
    *
    * Note the honesty limit of the untyped path: a `PSVec` layer is node-blind, so a pure
    * `PSVec[A] => A` can express only constructor-independent folds (`size`, child counts, …) —
    * `eval`-style algebras need the para-flavored `(S, PSVec[A]) => A` overload above. A typed
    * pattern-functor path — where a pure `F[A] => A` algebra would be fully expressive, because
    * `F`'s constructors carry what `PSVec` erases — is ''planned'' but not part of this artifact
    * yet: there is no `cataF` entry point to look for.
    */
  def cata[S, A](alg: Unfold[A, A, PSVec])(using Plated[S]): Getter[S, A] =
    cata[S, A]((_, kids) => alg.embed(kids))

  /** Anamorphism as a `Review` (reverse-construction optic): a stack-safe unfold `Seed => S` driven
    * by a [[Coalg]]. Materializing — the built `S` is `O(nodes)`.
    *
    * Stack-safety contract: below the 512-frame on-stack limit the unfold recurses directly on the
    * JVM stack; past it, each deep subtree is handed to a heap `ArrayDeque` machine — so any
    * ''terminating'' coalgebra builds without `StackOverflowError`, at any depth, and a
    * non-terminating one fails by exhausting the heap (`OutOfMemoryError`), not the stack.
    */
  def ana[Seed, S](coalg: Coalg[Seed, S]): Review[S, Seed] =
    Review[S, Seed](unfoldCoalg(coalg))

  /** Hylomorphism — the **fused** refold `Seed => A`, building **no intermediate `S`**: `expand`
    * unfolds seeds and `alg` folds to `A` in one post-order pass. Returned as a `Getter[Seed, A]`
    * so it composes further. Equal to `ana(…).cross(cata(alg))` on the same computation (the hylo
    * law), but without materializing the structure.
    *
    * Stack-safety contract (same as [[cata]] / [[ana]]): on-stack recursion below the 512-frame
    * limit, then a heap `ArrayDeque` machine per deep subtree — safe for any ''terminating''
    * `expand` at any depth; a non-terminating `expand` fails by exhausting the heap
    * (`OutOfMemoryError`), not by `StackOverflowError`.
    */
  def hylo[Seed, A](
      expand: Seed => PSVec[Seed],
      alg: (Seed, PSVec[A]) => A,
  ): Getter[Seed, A] =
    // Routed through the one Coalg engine — B/op-checked vs the dedicated
    // unfoldFold engine it replaced (SchemesBench.eoHylo, -prof gc).
    Getter[Seed, A](unfoldCoalg[Seed, A](seed => (expand(seed), rs => alg(seed, rs))))
