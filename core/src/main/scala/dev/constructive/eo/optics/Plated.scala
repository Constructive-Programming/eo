package dev.constructive.eo
package optics

import cats.{Eval, Monad}
import cats.syntax.flatMap.*

import data.{MultiFocus, PSVec, SetterF}
import data.MultiFocus.given // ForgetfulFunctor / Fold / Traverse for MultiFocus[PSVec]

/** A self-similar structure: a value of `S` whose immediate sub-terms are themselves `S`. The
  * single member [[plate]] is a `Traversal[S, S]` focusing those immediate children — the cats-eo
  * analogue of Haskell `lens`'s `Plated` class (`plate :: Traversal' a a`).
  *
  * The recursion combinators in the companion ([[Plated.transform]], [[Plated.rewrite]],
  * [[Plated.children]], [[Plated.universe]]) build on `plate` to walk the *whole* tree. They are
  * **stack-safe**: the structural recursions trampoline through `cats.Eval` (or an explicit
  * worklist), so a degenerate 100k-deep tree is fine.
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
  def childrenVec(s: S): PSVec[S] = plate.to(s)._2

  /** Reassemble `parent` with `children` swapped in (same arity / order) — the write-path
    * counterpart to [[childrenVec]]. Defaults to threading the carrier; [[Plated.fromChildren]] /
    * [[Plated.fromChildrenVec]] (hence every `generics.plate[S]`-derived instance) override it with
    * the raw rebuild function so [[Plated.transform]] rebuilds without allocating the carrier's
    * `(x, PSVec)` tuple per node.
    */
  def rebuild(parent: S, children: PSVec[S]): S =
    val p = plate // bind once so `p.X` is a single stable existential across `to` / `from`
    p.from((p.to(parent)._1, children))

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

  /** Bottom-up rewrite: rewrite every node's children first, then apply `f` to the node. The single
    * pass each `lens` user reaches for.
    *
    * Stack-safe *without* the per-node `Eval` allocation an `Eval`-trampolined recursion would pay:
    * an explicit post-order stack machine walks the tree on a heap-allocated stack, reading
    * children via [[Plated.childrenVec]] and rebuilding via [[Plated.rebuild]] (no carrier-tuple
    * per node). Leaves are applied in place — no frame, no rebuild copy — so a balanced tree
    * allocates a frame only for its internal nodes. Keeps the deep-tree guarantee while running
    * close to a hand-written recursive rebuild.
    */
  def transform[S](f: S => S)(root: S)(using P: Plated[S]): S =
    // One frame per internal node on the path to the cursor: the node, its children still to
    // rewrite, and the rebuilt-children array filled in as each child completes.
    final class Frame(val node: S, val kids: PSVec[S], val out: Array[AnyRef], var i: Int)
    val stack = new java.util.ArrayDeque[Frame]()
    var ret: S = root // overwritten before any read (only read once a child has completed)
    // Enter a node: apply `f` straight to a leaf; otherwise push a frame to rewrite its children.
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

  /** Effectful bottom-up rewrite over any monad `G` — the engine [[transform]] and [[rewrite]] are
    * specialisations of. Stack-safe when `G` is a trampolining monad (`Eval`, `IO`).
    */
  def transformM[S, G[_]: Monad](f: S => G[S])(s: S)(using P: Plated[S]): G[S] =
    def go(x: S): G[S] = P.plate.modifyA[G](go)(x).flatMap(f)
    go(s)

  /** Apply the rule everywhere, bottom-up, and keep re-firing on each rewritten sub-term until the
    * rule returns `None` at every node — Haskell `lens`'s `rewrite`. The caller owns termination (a
    * rule that always fires loops forever). Stack-safe via `Eval`.
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
