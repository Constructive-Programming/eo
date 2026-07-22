package dev.constructive.eo
package optics

import scala.collection.immutable.ArraySeq

import cats.{Monoid, MonoidK, Traverse}

import data.{MultiFocus, MultiFocusK, PSVec}

/** Concrete family class for `Traversal` — the many-focus optic on the `MultiFocus[PSVec]` carrier.
  * Every constructor in the [[Traversal$]] companion (and [[Each]]) returns this type, so "a
  * `Traversal[S, A]`" is spelled `Traversal[S, S, A, A]` (like the four-parameter [[Optional]] and
  * [[Modify]] classes). Ascribing it is SAFE — the fused members below are `inline`, so they
  * survive ascription at THIS type (only ascribing the generic `Optic[…]` falls back to the generic
  * extensions); capability evidence (`CanFold[S, A]`, `CanModify[S, A]`) is served by the derived
  * givens in each capability's companion.
  *
  * '''Fused `modify` / `replace` / `foldMap`, by measurement''' (JMH `TraversalBench.eoModify`,
  * size 64, `-prof gc`): the generic extensions summon the PARAMETERIZED `mfFunctor[F: Functor]` /
  * `mfFold[F: Foldable]` givens — and a parameterized given instantiates per call — plus an extra
  * capture in the spliced closure shape; together a fixed 40 B/op per operation (4 904 → 4 864 B/op
  * measured, ±0.001). The members below splice the same logic with the CACHED `PSVec.pSVecFunctor`
  * / `pSVecFoldable` instances instead. [[modify]] / [[replace]] are `inline` deliberately: each
  * call site gets its own spliced body, so the `to` / `from` dispatch sites stay per-site
  * monomorphic across the many subclasses (pEach / selfChildren / fixed-arity / the byte-carried
  * integration traversals) — a plain `def` here would be ONE shared body accumulating every
  * subclass's type profile, the megamorphic trap documented on [[Getter]] and [[PickFold]] (a
  * plain-`def` variant was also measured: it recovers only 16 of the 40 B/op). [[foldMap]] is the
  * deliberate exception — a virtual `def` the constructors override with STREAMING folds that skip
  * `to(s)`'s focus-vector build entirely; see its scaladoc.
  *
  * Each instance keeps its own existential `X` (the reassembly context: the original container for
  * [[Traversal.pEach]], the node for [[Traversal.selfChildren]], `Unit` for the fixed-arity
  * tabulations). The byte-carried integration traversals — eo-jsoniter's `JsoniterTraversal` and
  * eo-avro's `AvroTraversal` — extend this class too (same `Array[Byte]` / `MultiFocus[PSVec]`
  * shape), so ascribing them as `Traversal[Array[Byte], Array[Byte], A, A]` is safe; only
  * eo-circe's `JsonTraversal` is NOT a subtype — its surface is a bespoke
  * `Ior[Chain[JsonFailure], _]`-accumulating one, not the `MultiFocus[PSVec]` carrier.
  */
abstract class Traversal[S, T, A, B] extends Optic[S, T, A, B, MultiFocus[PSVec]]:

  /** Fused `modify` — same logic as the generic extension, with the cached `pSVecFunctor` in place
    * of the per-call `mfFunctor[PSVec]` given instantiation (−40 B/op with [[replace]] /
    * [[foldMap]]'s sibling savings; see the class scaladoc). `inline` so each call site splices its
    * own copy — per-site monomorphic `to` / `from` dispatch.
    */
  inline def modify(f: A => B): S => T =
    s =>
      val mf = to(s)
      from(MultiFocus(mf.context, PSVec.pSVecFunctor.map(mf.foci)(f)))

  /** Fused `replace` — constant-function [[modify]]. */
  inline def replace(b: B): S => T =
    modify(_ => b)

  /** Fused `foldMap` — the default folds the focus vector via the cached `pSVecFoldable`, skipping
    * the per-call `mfFold[PSVec]` given instantiation the generic extension pays. Deliberately a
    * plain overridable `def`, unlike [[modify]]: the constructors override it with STREAMING folds
    * that never call `to(s)` at all — a fold has no use for the reassembly context `to` must pack,
    * so materializing the focus vector is pure overhead on the read path. [[Traversal.pEach]] folds
    * the container directly through `Traverse[T]`, [[Traversal.selfChildren]] folds the children
    * vector without the carrier wrapper, and [[Traversal.composed]] nests the two sides' folds so a
    * depth-k chain streams with no arrays at any depth. The O(n) allocation win dwarfs the fixed
    * per-call cost the old inline splice protected against.
    */
  def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    PSVec.pSVecFoldable.foldMap(to(s).foci)(f)

  /** First focus, if any, via the streaming [[foldMap]] under a first-`Some` monoid — the member
    * twin of the generic `Optic.headOption` extension, here so concrete Traversals skip the focus
    * vector build (the extension routes through `to(s)`).
    */
  def headOption(s: S): Option[A] =
    foldMap[Option[A]](Some(_))(s)(using MonoidK[Option].algebra[A])

  /** Focus count via the streaming [[foldMap]] — member twin of the generic `Optic.length`. */
  def length(s: S): Int =
    foldMap(_ => 1)(s)

  /** True iff a focus satisfies `p`, via the streaming [[foldMap]] under the disjunction monoid —
    * member twin of the generic `Optic.exists`.
    */
  def exists(p: A => Boolean)(s: S): Boolean =
    foldMap(p)(s)(using Monoid.instance[Boolean](false, _ || _))

  /** Fused same-carrier `Traversal.andThen(Traversal)` — same composition as the generic
    * `Optic.andThen` (the `mfAssocPSVec` kernel), re-homed under the concrete [[Traversal]] class
    * via [[Traversal.composed]]. The generic member returns an anonymous `Optic`, so a composed
    * chain's `.modify` / `.foldMap` resolve to the generic extensions — which re-instantiate the
    * parameterized `mfFunctor[PSVec]` / `mfFold[PSVec]` givens per call (the measured 40 B/op of
    * the class scaladoc) and share one megamorphic body. Returning [[Traversal]] keeps the fused
    * inline members (and per-site monomorphic `to` / `from` dispatch) on every chain.
    */
  def andThen[C, D](inner: Traversal[A, B, C, D]): Traversal[S, T, C, D] =
    Traversal.composed(this, inner)

  /** Fused `Traversal.andThen(Lens)` — lifts the lens through the same `tuple2multifocusPSVec`
    * bridge the `Morph`-routed generic extension uses, then composes on the shared carrier. See the
    * `andThen(inner: Traversal)` overload for why the concrete return type matters.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Traversal[S, T, C, D] =
    Traversal.composed(this, MultiFocusK.tuple2multifocusPSVec.to(inner))

  /** Fused `Traversal.andThen(Lens)` — [[SplitCombineLens]] / [[SimpleLens]] (macro-lens) inner. */
  def andThen[C, D, XI](inner: SplitCombineLens[A, B, C, D, XI]): Traversal[S, T, C, D] =
    Traversal.composed(this, MultiFocusK.tuple2multifocusPSVec.to(inner))

/** Constructors for [[Traversal]]. Every constructor here — [[each]] / [[pEach]] / [[selfChildren]]
  * and the [[two]] / [[three]] / [[four]] fixed-arity variants — rides the `MultiFocus[PSVec]`
  * carrier, so they all compose through the standard `.andThen` in both directions (past a Lens, a
  * Prism, another traversal, …).
  *
  * '''Keyed access lives in [[Index]] / [[At]] — as constructor objects, not typeclasses.'''
  * `Index(i)` / `Index(k)` focuses one positional or keyed slot (write on an absent slot passes
  * through — no insert), [[At]]`(k)` is the total Map lens to `Option[V]` (`Some` upserts, `None`
  * deletes), and [[Each]] is [[each]] under the Monocle-searched name. Each returns an ordinary
  * optic that composes after `.andThen` like any other; a bespoke keyed [[Optional]] remains a
  * one-liner when the container isn't a `Seq`/`Map`:
  *
  * {{{
  * def index[K, V](k: K): Optional[Map[K, V], Map[K, V], V, V] =
  *   Optional[Map[K, V], Map[K, V], V, V](
  *     getOrModify = m => m.get(k).toRight(m),
  *     reverseGet = { case (m, v) => m.updated(k, v) },
  *   )
  * }}}
  *
  * Known-arity focus sets go through [[two]] / [[three]] / [[four]]. A predicate-filtering WRITE
  * optic (Monocle's deprecated `filterIndex` / lens-library `filtered`) is unlawful — a write can
  * flip which elements the predicate selects, breaking composition laws — so it is not provided;
  * filter on the read side instead (`Fold.select` / `AffineFold.select` /
  * [[Optional.selectReadOnly]]).
  */
object Traversal:

  /** Same-carrier composition into the concrete class — the exact anonymous-`Optic` shape the
    * generic `Optic.andThen` builds over the `mfAssocPSVec` kernel, re-homed under [[Traversal]] so
    * the fused inline `modify` / `replace` / `foldMap` members survive composition. Shared by the
    * class-level fused `andThen` overloads here and on the Lens family.
    */
  private[optics] def composed[S, T, A, B, C, D](
      outer: Optic[S, T, A, B, MultiFocus[PSVec]],
      inner: Optic[A, B, C, D, MultiFocus[PSVec]],
  ): Traversal[S, T, C, D] =
    new ComposedTraversal[S, T, A, B, C, D, outer.X, inner.X](outer, inner)

  /** Monomorphic Traversal over `Traverse[T]` — `S = T = T[A]`, focus preserved.
    *
    * @group Constructors
    */
  def each[T[_]: Traverse, A]: Traversal[T[A], T[A], A, A] =
    pEach[T, A, A]

  /** Polymorphic counterpart to [[each]] — allows focus type change.
    *
    * Reassembly walks the original container with `Functor.map` plus a captured positional `var` —
    * equivalent to `Traverse.mapAccumulate` but without the State thunk chain (the dominant CPU
    * cost on profiled nested traversals). For `ArraySeq` the `.from` body bypasses
    * `Traverse[ArraySeq].map` entirely (which threads through `StrictOptimizedSeqOps` →
    * non-devirtualised `SeqOps.size$`, observed as 18% CPU), instead wrapping the focus vector
    * directly via `ArraySeq.unsafeWrapArray`.
    *
    * @group Constructors
    */
  def pEach[T[_]: Traverse, A, B]: Traversal[T[A], T[B], A, B] =
    new TraverseTraversal[T, A, B]

  /** Monomorphic self-traversal from an explicit immediate-children view — focuses the values of
    * `S` that are themselves `S` (the immediate sub-terms), with `S` itself as the leftover
    * skeleton. `children(s)` is the immediate children as a focus vector; `rebuild(s, vec)`
    * reassembles `s` with that same number of children swapped in (same order). The two must agree
    * on arity and order: `rebuild(s, children(s)) == s`.
    *
    * PSVec-native: `to` / `from` pass the `PSVec` straight through, so both the read path
    * ([[Plated.children]] / `universe`, which read `to(s)._2`) and the write path
    * ([[Plated.transform]] / `rewrite`, which traverse and rebuild through the carrier) avoid any
    * `List` ↔ `PSVec` conversion. This is the carrier behind [[Plated]] — a recursive ADT's
    * `plate`. Unlike [[each]] / [[pEach]] there is no container `T[_]`; the children of a node are
    * gathered case-by-case, and the variable arity across cases (a binary node has two children, a
    * leaf none) is exactly the `PSVec` shape.
    *
    * @group Constructors
    */
  def selfChildren[S](
      children: S => PSVec[S],
      rebuild: (S, PSVec[S]) => S,
  ): Traversal[S, S, S, S] =
    new Traversal[S, S, S, S]:
      type X = S
      def to(s: S): MultiFocus[PSVec][X, S] = MultiFocus(s, children(s))
      def from(pair: MultiFocus[PSVec][X, S]): S = rebuild(pair.context, pair.foci)

      // Streaming read path: fold the children vector directly, skipping the carrier wrapper.
      override def foldMap[M](f: S => M)(s: S)(using Monoid[M]): M =
        PSVec.pSVecFoldable.foldMap(children(s))(f)

  /** Traversal over two per-element getters with a `reverse` reassembly. The arity is known at
    * construction time, so the foci tabulate straight into the `MultiFocus[PSVec]` carrier and the
    * result composes like any other traversal — past `each`, a Lens, a Prism, or another
    * fixed-arity hop. `reverse` sees only the (modified) foci: the constructor is full-cover, so
    * any leftover context of `S` must be rebuilt by `reverse` itself (drill with a Lens first when
    * there are sibling fields). For the Grate-shaped `Int => A` encoding, see `MultiFocus.tuple`.
    *
    * The tabulating subclass is macro-generated per CALL SITE (see [[TraversalArityMacro]]): each
    * site keeps its own monomorphic `to` / `from` bodies, and literal selector / reverse lambdas
    * are beta-reduced straight into the tabulation.
    *
    * @group Constructors
    */
  inline def two[S, T, A, B](
      inline a: S => A,
      inline b: S => A,
      inline reverse: (B, B) => T,
  ): Traversal[S, T, A, B] =
    ${ TraversalArityMacro.twoImpl('a, 'b, 'reverse) }

  /** Fixed-arity-3 — see [[two]]. @group Constructors */
  inline def three[S, T, A, B](
      inline a: S => A,
      inline b: S => A,
      inline c: S => A,
      inline reverse: (B, B, B) => T,
  ): Traversal[S, T, A, B] =
    ${ TraversalArityMacro.threeImpl('a, 'b, 'c, 'reverse) }

  /** Fixed-arity-4 — see [[two]]. @group Constructors */
  inline def four[S, T, A, B](
      inline a: S => A,
      inline b: S => A,
      inline c: S => A,
      inline d: S => A,
      inline reverse: (B, B, B, B) => T,
  ): Traversal[S, T, A, B] =
    ${ TraversalArityMacro.fourImpl('a, 'b, 'c, 'd, 'reverse) }

/** The concrete class behind [[composed]] — see its scaladoc for why the composition re-homes under
  * [[Traversal]].
  *
  * NO foldMap override — composed folds keep the base class's materialize-then-fold walk, BY
  * MEASUREMENT (2026-07-22, -prof gc, List[LineItem] fixtures). Streaming was tried in three shapes
  * and lost to the flat `composeTo` array walk in every regime that matters: per-element dispatch
  * 2x'd B/op on `lens∘each∘lens` at n ≥ 32 (megamorphic loop body — nothing inlines, the singleton
  * bridge's pair + boxes reach the heap); a pair-free `f ∘ singletonFocus` variant with the inner
  * resolved once per call still lost ~35% B/op at n ≥ 256 (won only below ~32 foci); and even the
  * each∘each shape — where inner dispatch amortizes over whole sub-containers — measured +37% B/op
  * streamed (TraversalBench.eoFoldNested vs its Optic-ascribed materialized twin). The tight
  * `@tailrec` walk over one flat array is what the JIT rewards; only the LEAF constructors (pEach /
  * selfChildren) stream. Do not re-stream composed folds without re-measuring.
  */
final class ComposedTraversal[S, T, A, B, C, D, Xo, Xi](
    outer: Optic[S, T, A, B, MultiFocus[PSVec]] { type X = Xo },
    inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
) extends Traversal[S, T, C, D]:
  private[optics] val af = MultiFocusK.mfAssocPSVec[Xo, Xi]
  type X = af.Z
  def to(s: S): MultiFocus[PSVec][X, C] = af.composeTo(s, outer, inner)
  def from(xd: MultiFocus[PSVec][X, D]): T = af.composeFrom(xd, inner, outer)

/** The concrete class behind [[each]] / [[pEach]] — a Traversal over any `Traverse[T]`, with the
  * original container as the reassembly context.
  */
final class TraverseTraversal[T[_]: Traverse, A, B] extends Traversal[T[A], T[B], A, B]:
  type X = T[A]

  def to(ta: T[A]): MultiFocus[PSVec][X, A] =
    MultiFocus(ta, PSVec.from(ta))

  def from(pair: MultiFocus[PSVec][X, B]): T[B] =
    val (xo, vec) = (pair.context, pair.foci)
    xo match
      case _: ArraySeq[?] =>
        // `unsafeShareableArray` returns the Slice's backing array when it densely covers
        // it (always true post-`composeFrom`); ArraySeq.unsafeWrapArray forbids mutation,
        // so aliasing is safe end-to-end. Fallback PSVec shapes copy via toAnyRefArray.
        ArraySeq.unsafeWrapArray(vec.unsafeShareableArray).asInstanceOf[T[B]]
      case _ =>
        var idx = 0
        Traverse[T].map(xo) { _ =>
          val b = vec(idx)
          idx += 1
          b
        }

  // Streaming read path: fold the container directly — `to(ta)`'s focus vector (and for the
  // non-ArraySeq shapes, its collect array) exists only to serve `from`, which a fold never
  // calls. Same focus order as `to` by construction (both follow Traverse[T]).
  override def foldMap[M](f: A => M)(ta: T[A])(using Monoid[M]): M =
    Traverse[T].foldMap(ta)(f)
