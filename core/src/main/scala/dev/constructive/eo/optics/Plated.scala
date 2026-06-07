package dev.constructive.eo
package optics

import cats.{Eval, Monad}
import cats.instances.list.given // Monoid[List[S]] for children's foldMap
import cats.syntax.flatMap.*

import data.{MultiFocus, PSVec}
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

  /** Immediate children as a `List` — the read-side fast path behind [[Plated.children]] /
    * [[Plated.universe]]. Defaults to a `foldMap` through [[plate]], but [[Plated.fromChildren]]
    * (and therefore every `generics.plate[S]`-derived instance) overrides it with the raw children
    * function, so the read path skips the `List → Array → PSVec → List` round-trip the optic's `to`
    * / `foldMap` would otherwise pay per node.
    */
  def childrenList(s: S): List[S] =
    plate.foldMap[List[S]](List(_))(s)

/** Combinators over [[Plated]] — recursion schemes faithful to `Control.Lens.Plated`, all
  * stack-safe. Each is offered both as a `using Plated[S]` method here and as an extension on any
  * self-traversal optic you build directly (so `myPlate.transform(f)(s)` works without a
  * typeclass).
  */
object Plated:

  /** Summon the instance. */
  def apply[S](using p: Plated[S]): Plated[S] = p

  /** Build a [[Plated]] from an explicit immediate-children view. `children(s)` lists the immediate
    * sub-terms; `rebuild(s, cs)` reassembles `s` with that many children swapped in, same order.
    */
  def fromChildren[S](children: S => List[S], rebuild: (S, List[S]) => S): Plated[S] =
    new Plated[S]:
      val plate: Optic[S, S, S, S, MultiFocus[PSVec]] = Traversal.selfChildren(children, rebuild)
      override def childrenList(s: S): List[S] = children(s)

  /** Bottom-up rewrite: rewrite every node's children first, then apply `f` to the node. The single
    * pass each `lens` user reaches for. Stack-safe via an `Eval` trampoline.
    */
  def transform[S](f: S => S)(s: S)(using P: Plated[S]): S =
    def go(x: S): Eval[S] = Eval.defer(P.plate.modifyA[Eval](go)(x)).map(f)
    go(s).value

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

  /** The immediate sub-terms of `s` (one level down). Routes through [[Plated.childrenList]], so a
    * derived / `fromChildren` plate reads its children directly rather than through the optic.
    */
  def children[S](s: S)(using P: Plated[S]): List[S] =
    P.childrenList(s)

  /** Every sub-term of `s`, `s` itself first, in pre-order. Stack-safe via an explicit worklist. */
  def universe[S](s: S)(using P: Plated[S]): List[S] =
    @annotation.tailrec
    def loop(stack: List[S], acc: List[S]): List[S] =
      stack match
        case Nil    => acc.reverse
        case h :: t => loop(P.childrenList(h) ::: t, h :: acc)
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
