package dev.constructive.eo
package optics

import scala.collection.immutable.ArraySeq

import cats.Traverse

import data.{MultiFocus, ObjArrBuilder, PSVec}

/** Constructors for `Traversal` — the multi-focus optic that modifies every element of a
  * traversable container.
  *
  *   - [[each]] / [[pEach]] use `MultiFocus[PSVec]`. The single Traversal carrier — supports
  *     `.modify`, `.foldMap`, `.modifyA`, AND downstream composition with [[Lens]] / [[Prism]] /
  *     [[Optional]] / [[Iso]] / further Traversals through the shared `MultiFocus[PSVec]`
  *     `AssociativeFunctor`. Read-only escape via the `.foldMap` extension method on
  *     `MultiFocus[F]`-carrier optics.
  *
  * The [[two]] / [[three]] / [[four]] fixed-arity variants expose a Traversal over a fixed number
  * of per-element getters — useful for "every element of this record that happens to share a type"
  * style fixtures.
  */
object Traversal:

  /** `each` — the Traversal constructor, built on the `MultiFocus[PSVec]` carrier. Supports
    * `.modify` / `.replace` (`Functor[PSVec]`), `.foldMap` (`Foldable[PSVec]`), `.modifyA` / `.all`
    * (`Traverse[PSVec]`), and downstream optic composition via the `MultiFocus[PSVec]`
    * `AssociativeFunctor`.
    *
    * @group Constructors
    * @tparam T
    *   container type constructor
    * @tparam A
    *   element type (monomorphic — `S = T = T[A]`, `A = B`).
    */
  def each[T[_]: Traverse, A]: Optic[T[A], T[A], A, A, MultiFocus[PSVec]] =
    pEach[T, A, A]

  /** Polymorphic counterpart to [[each]] — allows the focus to change type along the traversal.
    *
    * Reassembly uses `Functor.map` to walk the original container shape exactly once, pulling the
    * next focus value off a positional index maintained by a captured `var`. This matches what
    * `Traverse.mapAccumulate` would compute (Traverse laws guarantee `map` and `mapAccumulate`
    * visit elements in the same order), but without the `State[Int, _]` thunk chain that the
    * default `mapAccumulate` implementation builds — the dominant CPU cost of composed chains
    * ending in `pEach` on profiling. The original `T[A]` is stashed in the existential leftover
    * `X = (Int, T[A])` so `from` has something `Functor.map`-shaped to traverse.
    *
    * For [[ArraySeq]] specifically `.from` skips `Traverse[ArraySeq].map` entirely — cats's
    * instance delegates to stdlib's `ArraySeq.map`, which threads through
    * `StrictOptimizedSeqOps.strictOptimizedMap` → `this.size` via the non-devirtualised
    * `SeqOps.size$` forwarder. That dispatch appeared as 18% CPU on the nested-traversal bench. The
    * specialised path builds an `Array[AnyRef]` directly from the flat focus vector and wraps with
    * `ArraySeq.unsafeWrapArray` — one allocation, no builder-sizing round-trip.
    *
    * @group Constructors
    * @tparam T
    *   container type constructor
    * @tparam A
    *   element type being read
    * @tparam B
    *   element type being written back (may differ from `A` — polymorphic write path).
    */
  def pEach[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, MultiFocus[PSVec]] =
    new Optic[T[A], T[B], A, B, MultiFocus[PSVec]]:
      type X = T[A]

      val to: T[A] => (T[A], PSVec[A]) = ta =>
        ta match
          case refArr: ArraySeq.ofRef[?] =>
            // `ArraySeq.ofRef.unsafeArray` IS already an `Array[AnyRef]` (Scala's
            // type is `Array[_ <: AnyRef | Null]`, erased on the JVM to
            // `Object[]`). Wrap it directly — `Functor[PSVec].map` allocates a
            // fresh output array (never mutates the Slice's backing array), and
            // `ArraySeq` is contractually immutable, so aliasing is safe.
            (ta, PSVec.unsafeWrap[A](refArr.unsafeArray.asInstanceOf[Array[AnyRef]]))
          case _ =>
            val buf = new ObjArrBuilder()
            Traverse[T].foldLeft(ta, ())((_, a) => { buf.append(a.asInstanceOf[AnyRef]); () })
            (ta, buf.freezeAsPSVec[A])

      val from: ((T[A], PSVec[B])) => T[B] = {
        case (xo, vec) =>
          xo match
            case _: ArraySeq[?] =>
              // `unsafeShareableArray` hands back the Slice's own backing array when
              // the Slice densely covers it (offset=0, length=arr.length — always true
              // when `vec` came from `composeFrom`'s freshly-allocated resultBuf). We
              // then wrap via `ArraySeq.unsafeWrapArray`, whose contract also forbids
              // mutation, so the aliasing is safe end-to-end. Fallback path (non-dense
              // Slice, or other PSVec variants) does the System.arraycopy copy via
              // `toAnyRefArray`.
              ArraySeq.unsafeWrapArray(vec.unsafeShareableArray).asInstanceOf[T[B]]
            case _ =>
              var idx = 0
              Traverse[T].map(xo) { _ =>
                val b = vec(idx)
                idx += 1
                b
              }
      }

  /** Traversal over exactly two per-element getters. `reverse` reassembles the `T` from two
    * modified `B`s.
    *
    * Carrier: `MultiFocus[Function1[Int, *]]` — the homogeneous-arity `Tuple_N`-shaped traversal
    * encoded as a representable `Int => A` lookup over the N getters. Same shape as
    * [[MultiFocus.tuple]] but specialised to the per-element-getter API instead of the
    * homogeneous-tuple-source API. Inherits the standard `MultiFocus[Function1[Int, *]]`
    * composability surface: `Iso → MF[Function1[Int, *]]` (via `forgetful2multifocusFunction1`),
    * `MF[Function1[Int, *]] → SetterF` (via `multifocus2setter`), and same-carrier `.andThen` (via
    * `mfAssocFunction1`). Lens / Prism / Optional inputs are NOT bridged because
    * `Function1[Int, *]` lacks `Foldable` / `Alternative` — same constraint as v1 Grate.
    *
    * @group Constructors
    * @tparam S
    *   source type
    * @tparam T
    *   result type after reassembly
    * @tparam A
    *   per-element read focus
    * @tparam B
    *   per-element written-back focus
    */
  def two[S, T, A, B](
      a: S => A,
      b: S => A,
      reverse: (B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      val to: S => (Unit, Int => A) = s =>
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
        ((), read)

      val from: ((Unit, Int => B)) => T = {
        case (_, k) => reverse(k(0), k(1))
      }

  /** Fixed-arity-3 traversal. See [[two]].
    *
    * @group Constructors
    */
  def three[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      reverse: (B, B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      val to: S => (Unit, Int => A) = s =>
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
            case 2 => c(s)
        ((), read)

      val from: ((Unit, Int => B)) => T = {
        case (_, k) => reverse(k(0), k(1), k(2))
      }

  /** Fixed-arity-4 traversal. See [[two]].
    *
    * @group Constructors
    */
  def four[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      d: S => A,
      reverse: (B, B, B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      val to: S => (Unit, Int => A) = s =>
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
            case 2 => c(s)
            case 3 => d(s)
        ((), read)

      val from: ((Unit, Int => B)) => T = {
        case (_, k) => reverse(k(0), k(1), k(2), k(3))
      }
