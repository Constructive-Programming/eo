package dev.constructive.eo
package optics

import scala.collection.immutable.ArraySeq

import cats.Traverse

import data.{MultiFocus, ObjArrBuilder, PSVec}

/** Constructors for `Traversal`. [[each]] / [[pEach]] use the `MultiFocus[PSVec]` carrier; the
  * [[two]] / [[three]] / [[four]] fixed-arity variants ride `MultiFocus[Function1[Int, *]]` over a
  * fixed number of per-element getters.
  */
object Traversal:

  /** Monomorphic Traversal over `Traverse[T]` — `S = T = T[A]`, focus preserved.
    *
    * @group Constructors
    */
  def each[T[_]: Traverse, A]: Optic[T[A], T[A], A, A, MultiFocus[PSVec]] =
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
  def pEach[T[_]: Traverse, A, B]: Optic[T[A], T[B], A, B, MultiFocus[PSVec]] =
    new Optic[T[A], T[B], A, B, MultiFocus[PSVec]]:
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
  ): Optic[S, S, S, S, MultiFocus[PSVec]] =
    new Optic[S, S, S, S, MultiFocus[PSVec]]:
      type X = S
      def to(s: S): MultiFocus[PSVec][X, S] = MultiFocus(s, children(s))
      def from(pair: MultiFocus[PSVec][X, S]): S = rebuild(pair.context, pair.foci)

  /** Traversal over two per-element getters with a `reverse` reassembly. Carrier is
    * `MultiFocus[Function1[Int, *]]` — the homogeneous-arity Tuple_N-shaped traversal encoded as an
    * `Int => A` lookup over the N getters. Lens / Prism / Optional inputs aren't bridged
    * (Function1[Int, *] lacks `Foldable` / `Alternative`).
    *
    * @group Constructors
    */
  def two[S, T, A, B](
      a: S => A,
      b: S => A,
      reverse: (B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      def to(s: S): MultiFocus[Function1[Int, *]][X, A] =
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
        MultiFocus((), read)

      def from(pair: MultiFocus[Function1[Int, *]][X, B]): T = reverse(pair.foci(0), pair.foci(1))

  /** Fixed-arity-3 — see [[two]]. @group Constructors */
  def three[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      reverse: (B, B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      def to(s: S): MultiFocus[Function1[Int, *]][X, A] =
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
            case 2 => c(s)
        MultiFocus((), read)

      def from(pair: MultiFocus[Function1[Int, *]][X, B]): T =
        reverse(pair.foci(0), pair.foci(1), pair.foci(2))

  /** Fixed-arity-4 — see [[two]]. @group Constructors */
  def four[S, T, A, B](
      a: S => A,
      b: S => A,
      c: S => A,
      d: S => A,
      reverse: (B, B, B, B) => T,
  ): Optic[S, T, A, B, MultiFocus[Function1[Int, *]]] =
    new Optic[S, T, A, B, MultiFocus[Function1[Int, *]]]:
      type X = Unit

      def to(s: S): MultiFocus[Function1[Int, *]][X, A] =
        val read: Int => A = (i: Int) =>
          i match
            case 0 => a(s)
            case 1 => b(s)
            case 2 => c(s)
            case 3 => d(s)
        MultiFocus((), read)

      def from(pair: MultiFocus[Function1[Int, *]][X, B]): T =
        reverse(pair.foci(0), pair.foci(1), pair.foci(2), pair.foci(3))
