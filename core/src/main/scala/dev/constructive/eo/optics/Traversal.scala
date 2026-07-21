package dev.constructive.eo
package optics

import scala.collection.immutable.ArraySeq

import cats.{Monoid, Traverse}

import data.{MultiFocus, ObjArrBuilder, PSVec}

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
  * / `pSVecFoldable` instances instead. They are `inline` deliberately: each call site gets its own
  * spliced body, so the `to` / `from` dispatch sites stay per-site monomorphic across the many
  * subclasses (pEach / selfChildren / fixed-arity / the byte-carried integration traversals) — a
  * plain `def` here would be ONE shared body accumulating every subclass's type profile, the
  * megamorphic trap documented on [[Getter]] and [[PickFold]] (a plain-`def` variant was also
  * measured: it recovers only 16 of the 40 B/op).
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

  /** Fused `foldMap` — folds the focus vector via the cached `pSVecFoldable`, skipping the per-call
    * `mfFold[PSVec]` given instantiation the generic extension pays.
    */
  inline def foldMap[M](f: A => M)(s: S)(using M: Monoid[M]): M =
    PSVec.pSVecFoldable.foldMap(to(s).foci)(f)

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
    new Traversal[T[A], T[B], A, B]:
      type X = T[A]

      def to(ta: T[A]): MultiFocus[PSVec][X, A] =
        ta match
          case refArr: ArraySeq.ofRef[?] =>
            // ArraySeq.ofRef.unsafeArray is already an Array[AnyRef]; aliasing is safe because
            // ArraySeq is immutable and Functor[PSVec].map allocates fresh output.
            MultiFocus(
              ta,
              PSVec.unsafeWrap[A](refArr.unsafeArray.asInstanceOf[Array[AnyRef]]),
            )
          case _ =>
            val buf = new ObjArrBuilder()
            Traverse[T].foldLeft(ta, ())((_, a) => { buf.append(a.asInstanceOf[AnyRef]); () })
            MultiFocus(ta, buf.freezeAsPSVec[A])

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
