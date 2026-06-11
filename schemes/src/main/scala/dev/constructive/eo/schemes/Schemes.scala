package dev.constructive.eo
package schemes

import cats.Traverse

import data.{Forget, ForgetK, PSVec}
import optics.{Getter, Optic, Plated, Review, Unfold}

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

  /** Shared zero-length children array for leaf nodes — avoids a per-leaf empty-array allocation in
    * the typed [[foldLayered]] machine.
    */
  private val EmptyAnyRefs: Array[AnyRef] = new Array[AnyRef](0)

  /** Engine for the *fold* schemes ([[cata]] / [[hylo]]). `expand` yields a node's children;
    * `combine` folds a node plus its already-folded children — re-supplied the node, so it needs no
    * per-node closure. Stack-safe for any *terminating* `expand` (past the on-stack limit the
    * recursion lives on the heap, so a non-terminating `expand` exhausts the heap —
    * `OutOfMemoryError` — rather than overflowing the stack).
    */
  private def unfoldFold[N, R](expand: N => PSVec[N], combine: (N, PSVec[R]) => R): N => R =

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

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        val kids = expand(n)
        val k = kids.length
        if k == 0 then combine(n, PSVec.empty[R])
        else
          val out = new Array[AnyRef](k)
          var i = 0
          while i < k do
            out(i) = rec(kids(i), depth + 1).asInstanceOf[AnyRef]
            i += 1
          combine(n, PSVec.unsafeWrap[R](out))

    n0 => rec(n0, 0)

  /** Engine for the *build* scheme ([[ana]]). One [[Coalg]] call per node yields its children and
    * its combiner closure (stored in the frame on the heap path). Same on-stack/heap hybrid and
    * stack-safety as [[unfoldFold]].
    */
  private def unfoldCoalg[N, R](coalg: Coalg[N, R]): N => R =

    def heap(root: N): R =
      final class Frame(
          val combine: PSVec[R] => R,
          val kids: PSVec[N],
          val out: Array[AnyRef],
          var i: Int,
      )
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(n: N): Unit =
        val (kids, combine) = coalg(n)
        if kids.isEmpty then ret = combine(PSVec.empty[R]).asInstanceOf[AnyRef]
        else stack.push(new Frame(combine, kids, new Array[AnyRef](kids.length), 0))
      enter(root)
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

    def rec(n: N, depth: Int): R =
      if depth >= OnStackLimit then heap(n)
      else
        val (kids, combine) = coalg(n)
        val k = kids.length
        if k == 0 then combine(PSVec.empty[R])
        else
          val out = new Array[AnyRef](k)
          var i = 0
          while i < k do
            out(i) = rec(kids(i), depth + 1).asInstanceOf[AnyRef]
            i += 1
          combine(PSVec.unsafeWrap[R](out))

    n0 => rec(n0, 0)

  /** In-place fold engine for [[cata]]. `childrenOf` returns a **fresh, owned** `Array[AnyRef]` of
    * the node's children (via `Plated.childrenArray`); the engine folds each child and **overwrites
    * its slot with the result**, reusing that one array as the result accumulator instead of
    * allocating a separate out-array per node — then wraps it once for `alg`. Same on-stack/heap
    * hybrid and stack-safety as [[unfoldFold]]. Safe because `childrenArray`'s contract guarantees
    * the array is freshly allocated and not aliased.
    */
  private def foldInPlace[S, A](childrenOf: S => Array[AnyRef], alg: (S, PSVec[A]) => A): S => A =

    def heap(root: S): A =
      final class Frame(val node: S, val arr: Array[AnyRef], var i: Int)
      val stack = new java.util.ArrayDeque[Frame]()
      var ret: AnyRef = null.asInstanceOf[AnyRef]
      def enter(s: S): Unit =
        val arr = childrenOf(s)
        if arr.length == 0 then ret = alg(s, PSVec.empty[A]).asInstanceOf[AnyRef]
        else stack.push(new Frame(s, arr, 0))
      enter(root)
      while !stack.isEmpty do
        val fr = stack.peek()
        if fr.i > 0 then fr.arr(fr.i - 1) = ret // overwrite the just-folded child's slot
        if fr.i < fr.arr.length then
          val child = fr.arr(fr.i).asInstanceOf[S]
          fr.i += 1
          enter(child)
        else
          ret = alg(fr.node, PSVec.unsafeWrap[A](fr.arr)).asInstanceOf[AnyRef]
          val _ = stack.pop()
      ret.asInstanceOf[A]

    def rec(s: S, depth: Int): A =
      if depth >= OnStackLimit then heap(s)
      else
        val arr = childrenOf(s)
        val k = arr.length
        if k == 0 then alg(s, PSVec.empty[A])
        else
          var i = 0
          while i < k do
            arr(i) = rec(arr(i).asInstanceOf[S], depth + 1).asInstanceOf[AnyRef]
            i += 1
          alg(s, PSVec.unsafeWrap[A](arr))

    s0 => rec(s0, 0)

  /** Catamorphism as a composable `Getter`, driven by `Plated[S]`. The algebra sees the original
    * node `S` (paramorphism-flavored) plus its already-folded children. Stack-safe; folds child
    * results in place (see [[foldInPlace]]) so it allocates one array per node, not two.
    */
  def cata[S, A](alg: (S, PSVec[A]) => A)(using P: Plated[S]): Getter[S, A] =
    Getter[S, A](foldInPlace[S, A](P.childrenArray, alg))

  /** Catamorphism from a build-only optic citizen: a *pure* algebra `PSVec[A] => A` carried as an
    * [[dev.constructive.eo.optics.Unfold]], so an algebra built by optic composition
    * (`review.andThen(unfold)`, `unfold.andThen(review)`) drops straight into the fold engine.
    *
    * Note the honesty limit of the untyped path: a `PSVec` layer is node-blind, so a pure
    * `PSVec[A] => A` can express only constructor-independent folds (`size`, child counts, …) —
    * `eval`-style algebras need the para-flavored `(S, PSVec[A]) => A` overload above. The typed
    * pattern-functor path is where a pure `F[A] => A` algebra is fully expressive, because `F`'s
    * constructors carry what `PSVec` erases.
    */
  def cata[S, A](alg: Unfold[A, A, PSVec])(using Plated[S]): Getter[S, A] =
    cata[S, A]((_, kids) => alg.embed(kids))

  /** Anamorphism as a `Review` (reverse-construction optic): a stack-safe unfold `Seed => S` driven
    * by a [[Coalg]]. Materializing — the built `S` is `O(nodes)`.
    */
  def ana[Seed, S](coalg: Coalg[Seed, S]): Review[S, Seed] =
    Review[S, Seed](unfoldCoalg(coalg))

  /** Hylomorphism — the **fused** refold `Seed => A`, building **no intermediate `S`**: `expand`
    * unfolds seeds and `alg` folds to `A` in one post-order pass. Returned as a `Getter[Seed, A]`
    * so it composes further. Equal to `ana(…).cross(cata(alg))` on the same computation (the hylo
    * law), but without materializing the structure.
    */
  def hylo[Seed, A](
      expand: Seed => PSVec[Seed],
      alg: (Seed, PSVec[A]) => A,
  ): Getter[Seed, A] =
    Getter[Seed, A](unfoldFold(expand, alg))

  // ===========================================================================================
  // Typed pattern-functor path — the opt-in, type-safe complement to the PSVec schemes above.
  //
  // The user supplies a pattern functor `F[_]` + its `Traverse[F]`, and (for cata/ana)
  // `Project[F, S]` / `Embed[F, S]`. Algebras pattern-match `F`'s NAMED constructors
  // (`case BranchF(l, r) => l + r`) — no `PSVec[AnyRef]`, no positional indexing. See [[Basis]].
  //
  // These run on the SAME `< 512`-on-stack / heap-`ArrayDeque` hybrid as the PSVec schemes above
  // (see [[foldLayered]]) — NOT a `cats.Eval` trampoline. The deep recursion is driven by the
  // machine; the user's `Traverse[F]` is used only per *layer* (bounded fanout: `foldLeft` to read a
  // node's children, `map` to rebuild the typed `F[result]` for the algebra), never across the
  // spine. So stack-safety needs no `Eval`-lazy `foldRight` from the user — any lawful `Traverse[F]`
  // works — and allocation is close to the PSVec path (one children/result array + the typed `F`
  // layers per node), not the ~Eval-node-per-layer a trampoline would cost.
  // ===========================================================================================

  /** Rebuild a typed `F[R]` from the original layer `fn: F[N]` and its children's results, stored
    * positionally in `out` in `Foldable` order — which `Functor.map` matches for a lawful
    * `Traverse`. Lets the schemes hand the algebra a typed `F[R]` (named constructors) rather than
    * a positional vector.
    */
  private def rebuildLayer[F[_], N, R](fn: F[N], out: Array[AnyRef])(using F: Traverse[F]): F[R] =
    if out.length == 0 then
      fn.asInstanceOf[F[R]] // leaf: no N-slots, so F[N] is phantom-recast to F[R]
    else
      var i = -1
      F.map(fn) { _ =>
        i += 1
        out(i).asInstanceOf[R]
      }

  /** Shared typed engine for the `F`-path schemes. `expand` peels a node into one typed layer of
    * child nodes; the engine folds each child to an `R` (post-order), then calls `combine` with the
    * node, its layer `F[N]`, and the children's results (positional, `Foldable` order). `combine`
    * rebuilds the typed `F[R]` via [[rebuildLayer]] and applies the user's algebra / embed. Same
    * `< 512`-on-stack / heap-`ArrayDeque` hybrid (and stack-safety) as [[unfoldFold]] /
    * [[foldInPlace]]; the per-node child array is reused as the result accumulator (folded in
    * place).
    */
  private def foldLayered[F[_], N, R](
      expand: N => F[N],
      combine: (N, F[N], Array[AnyRef]) => R,
  )(using F: Traverse[F]): N => R =

    def childrenArr(fn: F[N]): Array[AnyRef] =
      val n = F.size(fn).toInt
      if n == 0 then EmptyAnyRefs
      else
        val arr = new Array[AnyRef](n)
        val _ = F.foldLeft(fn, 0) { (i, child) =>
          arr(i) = child.asInstanceOf[AnyRef]
          i + 1
        }
        arr

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

  /** The single *layer* optic for a pattern functor `F`: `project`/`embed` worn as the existing
    * [[dev.constructive.eo.data.Forget]] carrier. `to = project: S => F[S]`, `from = embed: F[S] =>
    * S`, so it is a genuine `Optic[S, S, S, S, Forget[F]]` with **no change to the `Optic` trait**.
    *
    * It is a single-layer *peel/glue* (like `Plated`'s `plate`, but one layer, not the recursion).
    * The recursive schemes below drive `to`/`from` themselves and return `Direct`-carried optics,
    * so `fLayer` is mainly the concrete proof that a typed `F` is an optic carrier, plus an
    * observational read: given `Foldable[F]` it reads its layer's foci via `.foldMap`. Note it does
    * NOT compose as freely as `plate` (a `Traversal`): same-carrier `andThen` over `Forget[F]`
    * needs `Monad[F]`, which most pattern functors are not — so `fLayer` is a one-layer lens on the
    * structure, not a composable traversal.
    */
  def fLayer[F[_], S](using P: Project[F, S], E: Embed[F, S]): Optic[S, S, S, S, Forget[F]] =
    new Optic[S, S, S, S, Forget[F]]:
      type X = Any
      def to(s: S): Forget[F][X, S] = ForgetK(P.project(s))
      def from(fs: Forget[F][X, S]): S = E.embed(fs.value)

  /** Catamorphism over a typed pattern functor `F`, as a composable `Getter`. `alg` sees the
    * original node `S` (paramorphism-flavored) plus its already-folded children as a typed `F[A]`.
    * Pure `F[A] => A` folds ignore the `S`. Stack-safe to arbitrary depth (the [[foldLayered]]
    * machine, not a trampoline). Requires `Project[F, S]` (to peel each layer) and `Traverse[F]`
    * (any lawful instance — the machine, not the user's `foldRight`, provides stack-safety).
    */
  def cataF[F[_], S, A](
      alg: (S, F[A]) => A
  )(using F: Traverse[F], P: Project[F, S]): Getter[S, A] =
    Getter[S, A](
      foldLayered[F, S, A](P.project, (s, fs, out) => alg(s, rebuildLayer[F, S, A](fs, out)))
    )

  /** Anamorphism over a typed pattern functor `F`, as a `Review`. The single fused coalgebra `Seed
    * => F[Seed]` yields one typed layer of child seeds; [[Embed]] assembles each layer into the
    * built `S`. Materializing — the built `S` is O(nodes). Stack-safe (the [[foldLayered]] machine).
    * Requires `Embed[F, S]` and `Traverse[F]`. Type params are `[F, Seed, S]` (input before output)
    * to match [[hyloF]] and the `PSVec` [[ana]].
    */
  def anaF[F[_], Seed, S](
      coalg: Seed => F[Seed]
  )(using F: Traverse[F], E: Embed[F, S]): Review[S, Seed] =
    Review[S, Seed](
      foldLayered[F, Seed, S](
        coalg,
        (_, fSeed, out) => E.embed(rebuildLayer[F, Seed, S](fSeed, out)),
      )
    )

  /** Hylomorphism over a typed pattern functor `F` — the **fused** refold `Seed => A`, building
    * **no intermediate `S`** (so it needs neither `Project` nor `Embed`, only `Traverse[F]`).
    * `coalg` unfolds a seed into one typed layer; `alg` folds the layer's results to `A` (the seed
    * is supplied, paramorphism-flavored). Stack-safe (the [[foldLayered]] machine). Equal to
    * `anaF(coalg).cross(cataF(alg))` for a *pure* algebra (the hylo law); for a node-reading para
    * algebra the two agree only under the seed↔`embed(coalg(seed))` correspondence.
    */
  def hyloF[F[_], Seed, A](
      coalg: Seed => F[Seed],
      alg: (Seed, F[A]) => A,
  )(using F: Traverse[F]): Getter[Seed, A] =
    Getter[Seed, A](
      foldLayered[F, Seed, A](
        coalg,
        (seed, fSeed, out) => alg(seed, rebuildLayer[F, Seed, A](fSeed, out)),
      )
    )
