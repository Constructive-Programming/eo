package dev.constructive.eo
package optics

import cats.Eval

import data.{MultiFocus, PSVec, SetterF}

/** A self-similar structure: a value of `S` whose immediate sub-terms are themselves `S`. The
  * single member [[plate]] is a `Traversal[S, S]` focusing those immediate children — the cats-eo
  * analogue of Haskell `lens`'s `Plated` class (`plate :: Traversal' a a`).
  *
  * The recursion combinators in the companion ([[Plated.transform]], [[Plated.rewrite]],
  * [[Plated.children]], [[Plated.universe]]) build on `plate` to walk the *whole* tree. They are
  * **stack-safe** on deep trees: `transform` recurses on the call stack while shallow and falls
  * back to a heap-stack machine past a depth bound, `rewrite` trampolines through `cats.Eval`, and
  * `universe` / `children` use an explicit worklist — a degenerate 100k-deep tree is fine.
  *
  * The children are carried as a `PSVec` (the `MultiFocus[PSVec]` carrier's focus vector), so the
  * read path and the write path share one representation with no `List` round-trips — see
  * [[childrenVec]].
  *
  * Get an instance by hand via [[Plated.fromChildren]] / [[Plated.fromChildrenVec]], by deriving
  * one with `dev.constructive.eo.generics.plate[S]`, or — when you have already built the
  * self-traversal as an optic — by calling `.asPlated` on it (see the extension methods below).
  *
  * @see
  *   [[Traversal.selfChildren]] for the underlying carrier.
  */
trait Plated[S]:
  /** The immediate-children self-traversal. */
  def plate: Optic[S, S, S, S, MultiFocus[PSVec]]

  /** Immediate children as a `PSVec` — the representation both the read path ([[Plated.children]] /
    * [[Plated.universe]]) and the write path ([[Plated.transform]] / [[Plated.rewrite]], through
    * the carrier) share. Defaults to reading the carrier directly (`plate.to(s)._2`);
    * [[Plated.fromChildren]] / [[Plated.fromChildrenVec]] (hence every `generics.plate[S]`-derived
    * instance) override it to return the children with no tuple allocation. Either way nothing is
    * converted to a `List` and back — that round-trip was the read/write hot-path tax.
    */
  def childrenVec(s: S): PSVec[S] = plate.to(s).foci

  /** The immediate children as a **fresh `Array[AnyRef]` the caller may mutate in place**. The
    * default copies (`childrenVec(s).toAnyRefArray`) so it is safe for any instance;
    * [[Plated.fromChildrenVec]] overrides it to hand back the freshly-built backing array
    * copy-free. The recursion-scheme `cata` engine folds each child's result into this array in
    * place, reusing the one array instead of allocating a separate result accumulator per node.
    */
  def childrenArray(s: S): Array[AnyRef] = childrenVec(s).toAnyRefArray

  /** Reassemble `parent` with `children` swapped in (same arity / order) — the write-path
    * counterpart to [[childrenVec]]. Defaults to threading the carrier; [[Plated.fromChildren]] /
    * [[Plated.fromChildrenVec]] (hence every `generics.plate[S]`-derived instance) override it with
    * the raw rebuild function so [[Plated.transform]] rebuilds without allocating the carrier's
    * `(x, PSVec)` tuple per node. On the default (`.asPlated`) path this and [[childrenVec]] each
    * call `plate.to`, so a `transform` over a hand-built optic pays two `to` calls per internal
    * node — derived / `fromChildrenVec` instances override both and pay none.
    */
  def rebuild(parent: S, children: PSVec[S]): S =
    val p = plate // bind once so `p.X` is a single stable existential across `to` / `from`
    p.from(MultiFocus(p.to(parent).context, children))

/** Combinators over [[Plated]] — recursion schemes faithful to `Control.Lens.Plated`, all
  * stack-safe. Each is offered both as a `using Plated[S]` method here and as an extension on any
  * self-traversal optic you build directly (so `myPlate.transformAll(f)(s)` works without a
  * typeclass).
  */
object Plated:

  /** Summon the instance. */
  def apply[S](using p: Plated[S]): Plated[S] = p

  /** Build a [[Plated]] from an explicit children focus-vector view — the PSVec-native primitive.
    * `children(s)` is the immediate sub-terms as a `PSVec`; `rebuild(s, vec)` reassembles `s` with
    * that many children swapped in, same order. This is what `generics.plate[S]` emits, and it pays
    * zero `List` conversion on either the read or the write path.
    */
  def fromChildrenVec[S](childrenFn: S => PSVec[S], rebuildFn: (S, PSVec[S]) => S): Plated[S] =
    new Plated[S]:
      val plate: Optic[S, S, S, S, MultiFocus[PSVec]] =
        Traversal.selfChildren(childrenFn, rebuildFn)
      override def childrenVec(s: S): PSVec[S] = childrenFn(s)
      // `childrenFn` must return a *freshly-allocated* vector (every shipped instance does —
      // `generics.plate` emits `unsafeWrap(new Array(...))`, circe / `fromChildren` go through
      // `PSVec.fromIterable`, both fresh per call). `unsafeShareableArray` then hands back that
      // backing array copy-free, and [[childrenArray]]'s contract lets the cata engine mutate it.
      override def childrenArray(s: S): Array[AnyRef] = childrenFn(s).unsafeShareableArray
      override def rebuild(parent: S, kids: PSVec[S]): S = rebuildFn(parent, kids)

  /** Build a [[Plated]] from a `List`-shaped children view — the ergonomic hand-writing entry point
    * (`{ case Node(l, r) => List(l, r); … }`). Wraps the `List` into the PSVec-native
    * [[fromChildrenVec]]; fine for hand-written plates, while the performance-critical derived
    * instances avoid the wrap.
    */
  def fromChildren[S](children: S => List[S], rebuild: (S, List[S]) => S): Plated[S] =
    fromChildrenVec(
      s => PSVec.fromIterable(children(s)),
      (s, vec) => rebuild(s, vec.toList),
    )

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
    * Hybrid for speed without giving up stack-safety: it recurses on the JVM call stack (≈ a
    * hand-written recursive rebuild — no per-node heap `Frame`) while the depth stays under
    * [[transformRecursionLimit]], and hands any deeper subtree to [[transformMachine]] — an
    * explicit heap-stack post-order walk — so a degenerate spine of any depth (100k+) still can't
    * overflow. Both paths read children via [[Plated.childrenVec]] and rebuild via
    * [[Plated.rebuild]] (no carrier tuple per node), and apply `f` in place at leaves — the `f(s)`
    * shortcut (rather than `f(rebuild(s, empty))`) matches the non-leaf path because
    * `rebuild(leaf, empty) == leaf`, the `plateModifyIdentity` law.
    */
  def transform[S](f: S => S)(root: S)(using P: Plated[S]): S =
    def rec(s: S, depth: Int): S =
      val kids = P.childrenVec(s)
      if kids.isEmpty then f(s) // leaf
      else if depth >= transformRecursionLimit then transformMachine(f)(s) // deep: go to the heap
      else
        val n = kids.length
        val out = new Array[AnyRef](n)
        var i = 0
        while i < n do
          out(i) = rec(kids(i), depth + 1).asInstanceOf[AnyRef]
          i += 1
        f(P.rebuild(s, PSVec.unsafeWrap[S](out)))
    rec(root, 0)

  /** The stack-safe fallback for [[transform]] — an explicit post-order stack machine on the heap,
    * so depth is bounded by available heap, not the call stack. One frame per internal node on the
    * path to the cursor, holding its children and the rebuilt-children array filled in as each
    * child completes; leaves are applied in place.
    */
  private def transformMachine[S](f: S => S)(root: S)(using P: Plated[S]): S =
    final class Frame(val node: S, val kids: PSVec[S], val out: Array[AnyRef], var i: Int)
    val stack = new java.util.ArrayDeque[Frame]()
    var ret: S = root // overwritten before any read (only read once a child has completed)
    def enter(s: S): Unit =
      val kids = P.childrenVec(s)
      if kids.isEmpty then ret = f(s)
      else stack.push(new Frame(s, kids, new Array[AnyRef](kids.length), 0))
    enter(root)
    while !stack.isEmpty do
      val fr = stack.peek()
      if fr.i > 0 then fr.out(fr.i - 1) = ret.asInstanceOf[AnyRef] // stash the child just finished
      if fr.i < fr.kids.length then
        val child = fr.kids(fr.i)
        fr.i += 1
        enter(child)
      else
        ret = f(P.rebuild(fr.node, PSVec.unsafeWrap[S](fr.out)))
        val _ = stack.pop()
    ret

  /** The whole structure as a composable optic that reaches *every* sub-term — `transform` in optic
    * form. `everywhere.modify(h) == transform(h)`, so composing a downstream optic and modifying
    * rewrites that focus at every depth, bottom-up:
    *
    * {{{
    * // uppercase EVERY variable anywhere in the tree:
    * everywhere[Expr].andThen(varPrism).andThen(nameLens).modify(_.toUpperCase)(tree)
    * }}}
    *
    * It is a write-side optic (a [[Setter]] whose `modify` is the recursive [[transform]]); it
    * composes as the *outer* of `.andThen` with any inner optic that bridges into `SetterF` (Lens /
    * Prism / Optional / …), and the composite is `transform(inner.modify(_))` by the optic
    * composition law. For the read side — every sub-term as a list — use [[universe]].
    */
  def everywhere[S](using P: Plated[S]): Optic[S, S, S, S, SetterF] =
    Setter[S, S, S, S](g => s => transform(g)(s))

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
    def go(x: S): Eval[S] = Eval.defer(P.plate.modifyA[Eval](go)(x)).flatMap(step)
    go(s).value

  /** The immediate sub-terms of `s` (one level down). Reads the plate's children vector directly.
    */
  def children[S](s: S)(using P: Plated[S]): List[S] =
    P.childrenVec(s).toList

  /** Every sub-term of `s`, `s` itself first, in pre-order. Stack-safe via an explicit worklist;
    * children are pushed straight off the `PSVec` (no per-node `List`).
    */
  def universe[S](s: S)(using P: Plated[S]): List[S] =
    @annotation.tailrec
    def loop(stack: List[S], acc: List[S]): List[S] =
      stack match
        case Nil    => acc.reverse
        case h :: t =>
          val vec = P.childrenVec(h)
          var pushed = t
          var i = vec.length - 1
          while i >= 0 do
            pushed = vec(i) :: pushed
            i -= 1
          loop(pushed, h :: acc)
    loop(s :: Nil, Nil)

  /** The "both" surface: treat any self-traversal optic as a [[Plated]] and call the combinators on
    * it directly. Lets you skip the typeclass when you have built `plate` by hand.
    */
  extension [S](self: Optic[S, S, S, S, MultiFocus[PSVec]])

    def asPlated: Plated[S] =
      new Plated[S]:
        val plate: Optic[S, S, S, S, MultiFocus[PSVec]] = self

    def transformAll(f: S => S)(s: S): S = Plated.transform(f)(s)(using asPlated)
    def rewriteAll(f: S => Option[S])(s: S): S = Plated.rewrite(f)(s)(using asPlated)
    def childrenOf(s: S): List[S] = Plated.children(s)(using asPlated)
    def universeOf(s: S): List[S] = Plated.universe(s)(using asPlated)
