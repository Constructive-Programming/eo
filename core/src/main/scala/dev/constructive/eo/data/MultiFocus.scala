package dev.constructive.eo
package data

import cats.{
  Alternative,
  Applicative,
  Eval,
  Foldable,
  Functor,
  Monoid,
  MonoidK,
  Representable,
  Traverse,
}

import optics.Optic

/** Unified pair carrier for the `AlgLens`, `Kaleidoscope`, and `Grate` optic families —
  * `MultiFocus[F][X, A] = (X, F[A])`. Each pairs a structural leftover `X` with a focus collection
  * / aggregate / rebuild-closure `F[A]`; the three v1 carriers were structurally identical, only
  * differing in how `F` was represented (type parameter vs path-dependent vs Function1-shaped).
  *
  * Kaleidoscope's `Reflector[F].reflect` operation was the only non-cats piece; it has two natural
  * derivations and the choice is structural, not derivable from a single typeclass:
  *
  *   - Functor-broadcast: `fa.map(_ => f(fa))`. Length-preserving; default. Needs `Functor[F]`.
  *   - Applicative-broadcast: `F.pure(f(fa))`. Singleton/cartesian. Needs `Applicative[F]`.
  *
  * The default is the first; List users wanting the singleton collapse can compose `_.headOption`
  * downstream or call `collectList` explicitly. See
  * `docs/research/2026-04-29-fixedtraversal-fold-spike.md` for the Grate-absorption justification.
  *
  * @tparam F
  *   classifier shape — operation requirements:
  *   - `.modify` / `.replace` need `Functor[F]`.
  *   - `.foldMap` needs `Foldable[F]`.
  *   - `.modifyA` / `.all` need `Traverse[F]`.
  *   - `.collectMap` needs `Functor[F]`; `collectList` is List-specific.
  *   - Same-carrier `.andThen` needs `Traverse[F] + MultiFocusFromList[F]`.
  *   - `fromPrismF` / `fromOptionalF` need `MonoidK[F]`.
  */
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])

/** Singleton-classifier fast-path. Lets `mfAssoc` skip the `F.pure` wrap and the
  * `pickSingletonOrThrow` pull when the inner is known to produce singletons (sole shipped user:
  * the `tuple2multifocus` Lens → MultiFocus bridge).
  */
private[eo] trait MultiFocusSingleton[S, T, A, B, X0]:
  def singletonTo(s: S): (X0, A)
  def singletonFrom(x: X0, b: B): T

/** Per-F O(n) builder. Carried as a typeclass because `MonoidK[F].combineK` has inconsistent
  * asymptotics across F (O(n²) on Vector, lossy on Option), so deriving `fromList` from
  * `Traverse[F] + MonoidK[F]` is asymptotically wrong on the carriers we care about.
  */
private[eo] trait MultiFocusFromList[F[_]]:
  def fromList[A](xs: List[A]): F[A]

  def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): F[A] =
    fromList(List.tabulate(size)(i => arr(from + i).asInstanceOf[A]))

private[eo] object MultiFocusFromList:

  given forList: MultiFocusFromList[List] with
    def fromList[A](xs: List[A]): List[A] = xs

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): List[A] =
      List.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forOption: MultiFocusFromList[Option] with

    def fromList[A](xs: List[A]): Option[A] = xs match
      case Nil      => None
      case h :: Nil => Some(h)
      case _        =>
        throw new IllegalStateException(
          s"MultiFocusFromList[Option]: cannot represent ${xs.size} elements; cardinality is 0 or 1."
        )

  given forVector: MultiFocusFromList[Vector] with
    def fromList[A](xs: List[A]): Vector[A] = xs.toVector

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): Vector[A] =
      Vector.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forChain: MultiFocusFromList[cats.data.Chain] with
    def fromList[A](xs: List[A]): cats.data.Chain[A] = cats.data.Chain.fromSeq(xs)

  /** PSVec builder — `fromArraySlice` is zero-copy (returns a `PSVec.Slice` view over the source
    * array). The crucial perf hook that lets `mfAssocPSVec.composeFrom` hand each inner reassembly
    * an O(1) slice of the shared flat focus vector.
    */
  given forPSVec: MultiFocusFromList[PSVec] with

    def fromList[A](xs: List[A]): PSVec[A] = xs match
      case Nil      => PSVec.empty[A]
      case h :: Nil => PSVec.singleton[A](h)
      case _        =>
        val arr = new Array[AnyRef](xs.size)
        var i = 0
        var cur = xs
        while cur.nonEmpty do
          arr(i) = cur.head.asInstanceOf[AnyRef]
          i += 1
          cur = cur.tail
        PSVec.unsafeWrap[A](arr)

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): PSVec[A] =
      size match
        case 0 => PSVec.empty[A]
        case 1 => PSVec.singleton[A](arr(from).asInstanceOf[A])
        case _ => new PSVec.Slice[A](arr, from, size)

/** PSVec-specialised maybe-hit fast-path. Used by Prism / Optional morphs that produce a 0- or
  * 1-element focus vector, where the generic `inner.to(s)` would build an `Either`/`Option`-shaped
  * wrapper the fast-path can elide. AlwaysHit (Lens) morphs already get a fast-path via the
  * carrier-wide `MultiFocusSingleton`; this is the maybe-hit complement, scoped to PSVec because
  * its body writes directly into the `IntArrBuilder` / `ObjArrBuilder` that `mfAssocPSVec` uses.
  */
private[eo] trait MultiFocusPSMaybeHit[S, T, A, B]:

  def collectTo(
      s: S,
      lenBuf: IntArrBuilder,
      ysBuf: ObjArrBuilder,
      flatBuf: ObjArrBuilder,
  ): Unit

  def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T

object MultiFocus:

  // PSVec cats instances — kept in this companion so callers picking up
  // `import data.MultiFocus.given` get them transitively alongside `mfFunctor` / etc.

  given pSVecFunctor: Functor[PSVec] with

    def map[A, B](fa: PSVec[A])(f: A => B): PSVec[B] =
      val n = fa.length
      if n == 0 then PSVec.empty[B]
      else
        val arr = new Array[AnyRef](n)
        var i = 0
        while i < n do
          arr(i) = f(fa(i)).asInstanceOf[AnyRef]
          i += 1
        PSVec.unsafeWrap[B](arr)

  given pSVecFoldable: Foldable[PSVec] with

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      var acc = b
      val n = fa.length
      var i = 0
      while i < n do
        acc = f(acc, fa(i))
        i += 1
      acc

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] =
      val n = fa.length
      def loop(i: Int): Eval[B] =
        if i >= n then lb
        else f(fa(i), Eval.defer(loop(i + 1)))
      Eval.defer(loop(0))

    override def size[A](fa: PSVec[A]): Long = fa.length.toLong

  given pSVecTraverse: Traverse[PSVec] with

    def traverse[G[_]: Applicative, A, B](fa: PSVec[A])(f: A => G[B]): G[PSVec[B]] =
      val G = Applicative[G]
      val n = fa.length
      if n == 0 then G.pure(PSVec.empty[B])
      else
        var acc: G[Array[AnyRef]] = G.pure(new Array[AnyRef](n))
        var i = 0
        while i < n do
          val idx = i
          val gb = f(fa(idx))
          acc = G.map2(acc, gb) { (a, b) =>
            a(idx) = b.asInstanceOf[AnyRef]
            a
          }
          i += 1
        G.map(acc)(arr => PSVec.unsafeWrap[B](arr))

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      pSVecFoldable.foldLeft(fa, b)(f)

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] = pSVecFoldable.foldRight(fa, lb)(f)

  // Capability instances — Functor / Foldable / Traverse over the F[A] half.

  given mfFunctor[F[_]: Functor]: ForgetfulFunctor[MultiFocus[F]] with

    def map[X, A, B](xa: (X, F[A]), f: A => B): (X, F[B]) =
      (xa._1, Functor[F].map(xa._2)(f))

  given mfFold[F[_]: Foldable]: ForgetfulFold[MultiFocus[F]] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, F[A])) => M =
      f => xa => Foldable[F].foldMap(xa._2)(f)

  given mfTraverse[F[_]: Traverse]: ForgetfulTraverse[MultiFocus[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: ((X, F[A])) => (A => G[B]) => G[(X, F[B])] =
      xa => f => Applicative[G].map(Traverse[F].traverse(xa._2)(f))(fb => (xa._1, fb))

  // Same-carrier composition — F-parametric, with `MultiFocusSingleton` fast-path on the inner.

  given mfAssoc[F[_]: Traverse: MultiFocusFromList, Xo, Xi]
      : AssociativeFunctor[MultiFocus[F], Xo, Xi] with
    type Z = (Xo, F[(Xi, Int)])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[F]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[F]] { type X = Xi },
    ): ((Xo, F[(Xi, Int)]), F[C]) =
      val Tr = Traverse[F]
      val FL = summon[MultiFocusFromList[F]]
      val (xo, fa) = outer.to(s)
      inner match
        case is: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          val (cList, xiList) =
            Tr.mapAccumulate((List.empty[C], List.empty[(Xi, Int)]), fa) {
              case ((accC, accXi), a) =>
                val (xi, c) = is.singletonTo(a)
                ((c :: accC, (xi, 1) :: accXi), ())
            }._1
          val fc: F[C] = FL.fromList(cList.reverse)
          val fxiSize: F[(Xi, Int)] = FL.fromList(xiList.reverse)
          ((xo, fxiSize), fc)
        case _ =>
          val (flatList, fxiSize) =
            Tr.mapAccumulate(List.empty[C], fa) { (acc, a) =>
              val (xi, fc) = inner.to(a)
              val (acc2, count) = Tr.foldLeft(fc, (acc, 0)) {
                case ((l, n), c) =>
                  (c :: l, n + 1)
              }
              (acc2, (xi, count))
            }
          val fc: F[C] = FL.fromList(flatList.reverse)
          ((xo, fxiSize), fc)

    def composeFrom[S, T, A, B, C, D](
        xd: ((Xo, F[(Xi, Int)]), F[D]),
        inner: Optic[A, B, C, D, MultiFocus[F]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[F]] { type X = Xo },
    ): T =
      val Tr = Traverse[F]
      val ((xo, fxiSize), fd) = xd
      val dArr: Array[AnyRef] = foldableToArray[F, D](fd)
      inner match
        case is: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, _)) =>
              val d = dArr(cursor).asInstanceOf[D]
              (cursor + 1, is.singletonFrom(xi, d))
          }
          outer.from((xo, fb))
        case _ =>
          val FL = summon[MultiFocusFromList[F]]
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, size)) =>
              (cursor + size, inner.from((xi, FL.fromArraySlice[D](dArr, cursor, size))))
          }
          outer.from((xo, fb))

  private def foldableToArray[F[_]: Foldable, D](fd: F[D]): Array[AnyRef] =
    val n = Foldable[F].size(fd).toInt
    val arr = new Array[AnyRef](n)
    var i = 0
    Foldable[F].foldLeft(fd, ()) { (_, d) =>
      arr(i) = d.asInstanceOf[AnyRef]
      i += 1
    }
    arr

  private def pickSingletonOrThrow[F[_]: Foldable, B](fb: F[B], carrier: String): B =
    val sz = Foldable[F].size(fb)
    if sz == 1 then Foldable[F].reduceLeftToOption(fb)(identity[B])((_, b) => b).get
    else
      throw new IllegalStateException(
        s"Composer[$carrier, MultiFocus[F]]: expected F[B] of cardinality 1, got $sz."
      )

  // Function1-shaped specialisation — the Grate-absorbed case. The general `mfAssoc` requires
  // `Traverse[F]` + `MultiFocusFromList[F]`; Function1[X0, *] admits neither. The outer rebuild is
  // a constant broadcast: every shipped outer (iso-morphed, tuple-built) rebuilds via broadcast
  // anyway. See `docs/research/2026-04-29-fixedtraversal-fold-spike.md`.

  given mfAssocFunction1[X0, Xo, Xi]: AssociativeFunctor[MultiFocus[Function1[X0, *]], Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[Function1[X0, *]]] { type X = Xi },
    ): ((Xo, Xi), X0 => C) =
      val (_, kO) = outer.to(s)
      // Null sentinel works because `.modify` doesn't observe the focus value (spike Q1).
      val a: A = kO(null.asInstanceOf[X0])
      val (_, kI) = inner.to(a)
      ((null.asInstanceOf[Xo], null.asInstanceOf[Xi]), kI)

    def composeFrom[S, T, A, B, C, D](
        xd: ((Xo, Xi), X0 => D),
        inner: Optic[A, B, C, D, MultiFocus[Function1[X0, *]]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] { type X = Xo },
    ): T =
      val (_, kD) = xd
      val b: B = inner.from((null.asInstanceOf[Xi], kD))
      outer.from((null.asInstanceOf[Xo], (_: X0) => b))

  // PSVec-specialised same-carrier composition. The generic `mfAssoc` body builds two intermediate
  // List accumulators + materialises via `fromList`; this body writes directly into `IntArrBuilder`
  // / `ObjArrBuilder` and stores the existential as parallel arrays in `AssocSndZ`, sidestepping the
  // per-element `(Xi, Int)` Tuple2 the generic path pays. See `docs/research/2026-04-29-powerseries-fold-spike.md`.

  given mfAssocPSVec[Xo, Xi]: AssociativeFunctor[MultiFocus[PSVec], Xo, Xi] with
    type SndZ = AssocSndZ[Xo, Xi]
    type Z = SndZ

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[PSVec]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
    ): (Z, PSVec[C]) =
      val (xo, va) = outer.to(s)
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      inner match
        case ah: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          // Always-hit fast path (mfSingleton): every call produces exactly one focus.
          val flatBuf = new ObjArrBuilder(n)
          var i = 0
          while i < n do
            val (xi, c) = ah.singletonTo(va(i))
            ysBuf.unsafeAppend(xi.asInstanceOf[AnyRef])
            flatBuf.unsafeAppend(c.asInstanceOf[AnyRef])
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, null, ysBuf.freezeArr)
          (sndZ, flatBuf.freezeAsPSVec[C])
        case mh: MultiFocusPSMaybeHit[A, B, C, D] @unchecked =>
          // Maybe-hit fast path (Prism / Optional morphs).
          val lenBuf = new IntArrBuilder(n)
          val flatBuf = new ObjArrBuilder(n)
          var i = 0
          while i < n do
            mh.collectTo(va(i), lenBuf, ysBuf, flatBuf)
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
          (sndZ, flatBuf.freezeAsPSVec[C])
        case _ =>
          // Generic fallback: reachable only via user-built MultiFocus[PSVec] optics that don't
          // mix in MultiFocusSingleton or MultiFocusPSMaybeHit (escape hatch for downstream).
          val lenBuf = new IntArrBuilder(n)
          val flatBuf = new ObjArrBuilder()
          var i = 0
          while i < n do
            val (xi, vy) = inner.to(va(i))
            lenBuf.append(vy.length)
            ysBuf.append(xi.asInstanceOf[AnyRef])
            flatBuf.appendAllFromPSVec(vy)
            i += 1
          val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
          (sndZ, flatBuf.freezeAsPSVec[C])

    def composeFrom[S, T, A, B, C, D](
        xd: (Z, PSVec[D]),
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[PSVec]] { type X = Xo },
    ): T =
      val (sndZ, vys) = xd
      val lens = sndZ.lens
      val ys = sndZ.ys
      val resultBuf = new ObjArrBuilder(ys.length)
      inner match
        case ah: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          // Always-hit fast path (lens == null, every element hits exactly once).
          val n = ys.length
          var i = 0
          while i < n do
            resultBuf.append(
              ah.singletonFrom(ys(i).asInstanceOf[Xi], vys(i)).asInstanceOf[AnyRef]
            )
            i += 1
        case mh: MultiFocusPSMaybeHit[A, B, C, D] @unchecked =>
          val lensArr = lens.nn
          val n = lensArr.length
          var offset = 0
          var i = 0
          while i < n do
            val len = lensArr(i)
            resultBuf.append(
              mh.reconstructSingleton(ys(i), vys, offset, len).asInstanceOf[AnyRef]
            )
            offset += len
            i += 1
        case _ =>
          // Generic fallback paired with composeTo's escape-hatch branch above.
          val lensArr = lens.nn
          val n = lensArr.length
          var offset = 0
          var i = 0
          while i < n do
            val len = lensArr(i)
            val y = ys(i).asInstanceOf[Xi]
            val chunk = vys.slice(offset, offset + len)
            resultBuf.append(inner.from((y, chunk)).asInstanceOf[AnyRef])
            offset += len
            i += 1
      outer.from((sndZ.xo, resultBuf.freezeAsPSVec[B]))

  /** Composed existential leftover for `MultiFocus[PSVec]` `andThen`. Parallel arrays rather than a
    * pair of arrays so each per-element entry pays one primitive-int write and one reference write
    * (no intermediate `(Int, Xi)` Tuple2). When `lens` is `null` the inner is a
    * `MultiFocusSingleton` — every per-element length is implicitly 1.
    */
  final private[eo] class AssocSndZ[Xo, Xi](
      val xo: Xo,
      val lens: Array[Int] | Null,
      val ys: Array[AnyRef],
  )

  // ------------------------------------------------------------------
  // Kaleidoscope universal — `.collect`. Multifocus-unification spike Q1
  // finding: NOT derivable from Apply alone in a way that preserves all
  // three v1 Reflector instances. We expose two variants and let the user
  // pick the aggregation shape.
  // ------------------------------------------------------------------

  /** Length-preserving broadcast — `reflect(fa)(f) = fa.map(_ => f(fa))`. Matches the v1
    * `forZipList` and `forConst` Reflector instances exactly; CHANGES the v1 `forList` semantics
    * (was singleton, becomes length-preserving). Requires only `Functor[F]`.
    *
    * For the generic `MultiFocus.apply[F, A]` factory (X = F[A], rebuild = identity), `.collectMap`
    * is `s => s.map(_ => agg(s))` — semantically identical to `Functor.map(_ => agg)` over the
    * source.
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, MultiFocus[List]])

    def collectList(agg: List[A] => B)(using ev: S =:= List[A], ev2: T =:= List[B]): S => T =
      val _ = (ev, ev2)
      // Cartesian / singleton — matches v1 Reflector[List]. T = List[B] preserved via List(b).
      (s: S) =>
        val (_, fa) = o.to(s)
        val b: B = agg(fa.asInstanceOf[List[A]])
        o.from((null.asInstanceOf[o.X], List(b)).asInstanceOf[(o.X, List[B])])

  /** Functor-broadcast variant — preserves F-shape via `map(_ => agg(fa))`. Works for any
    * `Functor[F]`; for List this is the ZipList-style length-preserving aggregation. Use
    * [[collectList]] for the v1 List-singleton semantics.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Functor[F])

    def collectMap[C](agg: F[A] => C)(using ev: C =:= B): S => T =
      val _ = ev
      (s: S) =>
        val (x, fa) = o.to(s)
        val b: C = agg(fa)
        val fb: F[B] = F.map(fa)(_ => b.asInstanceOf[B])
        o.from((x, fb))

  // Read-only escape (`.foldMap`) is provided by the carrier-wide `Optic.foldMap` extension via
  // `ForgetfulFold[MultiFocus[F]]` (`mfFold[F: Foldable]`). No `Composer[MultiFocus[F], Forget[F]]`
  // ships — `forget2multifocus` goes the other direction; a bidirectional pair would break Morph
  // resolution.

  /** Read the focus at a representative position. Requires `Representable[F]`. For
    * `MultiFocus[Function1[X0, *]]`, this is `index(fa)(i) = fa(i)`.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Representable[F])

    def at(i: F.Representation): S => A = (s: S) =>
      val (_, fa) = o.to(s)
      F.index(fa)(i)

  /** Generic factory: `X = F[A]`, focus = fa, rebuild = identity. Replaces the v1
    * `Kaleidoscope.apply`.
    */
  def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]] =
    new Optic[F[A], F[A], A, A, MultiFocus[F]]:
      type X = F[A]
      val to: F[A] => (F[A], F[A]) = (fa: F[A]) => (fa, fa)
      val from: ((F[A], F[A])) => F[A] = { case (_, fb) => fb }

  /** Iso → MultiFocus[F]. Requires `Applicative[F]` to broadcast the Iso's plain `A` focus into a
    * singleton `F[A]`.
    */
  given forgetful2multifocus[F[_]: Applicative: Foldable]: Composer[Forgetful, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), Applicative[F].pure(o.to(s)))
        val from: ((Unit, F[B])) => T = {
          case (_, fb) => o.from(pickSingletonOrThrow(fb, "Forgetful"))
        }

  /** Forget[F] ↪ MultiFocus[F]. */
  given forget2multifocus[F[_]]: Composer[Forget[F], MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), o.to(s))
        val from: ((Unit, F[B])) => T = { case (_, fb) => o.from(fb) }

  /** MultiFocus[F] ↪ Forget[F] — read-only escape: discard the structural leftover, keep the
    * focused `F[A]`. Closes the top-5 plan's gap #2 by giving users an explicit carrier morph
    * (alongside the carrier-wide `Optic.foldMap` / `.headOption` / `.length` / `.exists` extension
    * methods).
    *
    * Structurally this is the inverse of [[forget2multifocus]] — both Composer directions ship.
    * That's normally banned by the cats-eo Morph resolution invariant (a bidirectional pair makes
    * `Morph[Forget[F], MultiFocus[F]]` ambiguous because both `leftToRight` and `rightToLeft`
    * fire). The Composer ships anyway because:
    *   1. The `from` side requires `T = Unit` (Forget loses the leftover, so it can't reconstruct a
    *      T ≠ Unit). Only T-`Unit` MultiFocus optics qualify, which the type system enforces at use
    *      sites.
    *   2. Any chain-resolution ambiguity surfaces at `forget.andThen(multifocus)` /
    *      `multifocus.andThen(fold)` call sites — the user resolves by routing through the explicit
    *      `Composer[..].to(o)` form rather than `.andThen`.
    *
    * Practical Morph fallout: if a user actually hits the ambiguity, they get a clear implicit-not-
    * found message naming both Composers; the workaround is one extra `.morph`-shaped call.
    */
  given multifocus2forget[F[_]]: Composer[MultiFocus[F], Forget[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, Forget[F]] =
      new Optic[S, T, A, B, Forget[F]]:
        type X = Unit
        val to: S => F[A] = s => o.to(s)._2
        val from: F[B] => T = _ =>
          // Reachable only when `T = Unit` (Forget-carrier optics have T = Unit by construction).
          // The cast surfaces a ClassCastException if a user's MultiFocus optic has T ≠ Unit AND
          // they explicitly routed through this Composer — defensive only.
          ().asInstanceOf[T]

  /** Lens → MultiFocus[F]. Mixes in `MultiFocusSingleton` so the `mfAssoc` fast-path fires. */
  given tuple2multifocus[F[_]: Applicative: Foldable]: Composer[Tuple2, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]] with MultiFocusSingleton[S, T, A, B, o.X]:
        type X = o.X
        def singletonTo(s: S): (o.X, A) = o.to(s)
        def singletonFrom(x: o.X, b: B): T = o.from((x, b))
        val to: S => (X, F[A]) = s =>
          val (x, a) = o.to(s)
          (x, Applicative[F].pure(a))
        val from: ((X, F[B])) => T = {
          case (x, fb) =>
            o.from((x, pickSingletonOrThrow(fb, "Tuple2")))
        }

  /** Prism → MultiFocus[F]. */
  given either2multifocus[F[_]: Alternative: Foldable]: Composer[Either, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[o.X, Unit]
        val to: S => (X, F[A]) = s =>
          o.to(s) match
            case Right(a) => (Right(()), Applicative[F].pure(a))
            case Left(x)  => (Left(x), Alternative[F].empty[A])
        val from: ((X, F[B])) => T = {
          case (Left(xMiss), _) => o.from(Left(xMiss))
          case (Right(_), fb)   => o.from(Right(pickSingletonOrThrow(fb, "Either")))
        }

  /** Optional → MultiFocus[F]. */
  given affine2multifocus[F[_]: Alternative: Foldable]: Composer[Affine, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[Fst[o.X], Snd[o.X]]
        val to: S => (X, F[A]) = s =>
          o.to(s) match
            case h: Affine.Hit[o.X, A] =>
              (Right(h.snd), Applicative[F].pure(h.b))
            case m: Affine.Miss[o.X, A] =>
              (Left(m.fst), Alternative[F].empty[A])
        val from: ((X, F[B])) => T = {
          case (Left(fstX), _) =>
            o.from(new Affine.Miss[o.X, B](fstX))
          case (Right(sndX), fb) =>
            o.from(new Affine.Hit[o.X, B](sndX, pickSingletonOrThrow(fb, "Affine")))
        }

  // PSVec-specialised Composer instances. PSVec admits neither Applicative nor Alternative
  // naturally; these use `PSVec.singleton` / `PSVec.empty` directly. The Tuple2 bridge specialises
  // on `GetReplaceLens` to skip the intermediate `(s, get(s))` Tuple2; Either / Affine bridges mix
  // in `MultiFocusPSMaybeHit` so `mfAssocPSVec` picks up the Prism/Optional fast-path.

  /** Lens → MultiFocus[PSVec]. `GetReplaceLens` fast-path elides the `(s, get(s))` Tuple2 the
    * generic body would build. The `to` body builds anonymous Optic values inline so
    * `MultiFocusSingleton` can refer to `o.X` without tripping Scala 3's "class parent cannot refer
    * to constructor parameters" rule.
    */
  given tuple2multifocusPSVec: Composer[Tuple2, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      o match
        case glr: optics.GetReplaceLens[?, ?, ?, ?] =>
          val lens = glr.asInstanceOf[optics.GetReplaceLens[S, T, A, B]]
          new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusSingleton[S, T, A, B, S]:
            type X = S
            val to: S => (S, PSVec[A]) = s => (s, PSVec.singleton[A](lens.get(s)))
            val from: ((S, PSVec[B])) => T = { case (s, vs) => lens.enplace(s, vs.head) }
            def singletonTo(s: S): (S, A) = (s, lens.get(s))
            def singletonFrom(x: S, b: B): T = lens.enplace(x, b)
        case _ =>
          new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusSingleton[S, T, A, B, o.X]:
            type X = o.X
            val to: S => (o.X, PSVec[A]) = s =>
              val (xo, a) = o.to(s)
              (xo, PSVec.singleton[A](a))
            val from: ((o.X, PSVec[B])) => T = {
              case (xo, vs) => o.from((xo, vs.head))
            }
            def singletonTo(s: S): (o.X, A) = o.to(s)
            def singletonFrom(x: o.X, b: B): T = o.from((x, b))

  /** `Composer[Either, MultiFocus[PSVec]]` — lifts a Prism-carrier optic into MultiFocus[PSVec] so
    * `prism.andThen(traversal)` type-checks. Mixes in `MultiFocusPSMaybeHit` so the
    * PSVec-specialised `mfAssocPSVec` body skips the per-element `Either[o.X, Unit]` wrapper the
    * generic `to` path would build. Mirror of the legacy `either2ps`.
    */
  given either2multifocusPSVec: Composer[Either, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusPSMaybeHit[S, T, A, B]:
        type X = Option[o.X]
        val to: S => (Option[o.X], PSVec[A]) = s =>
          o.to(s) match
            case Left(x)  => (Some(x), PSVec.empty[A])
            case Right(a) => (None, PSVec.singleton[A](a))
        val from: ((Option[o.X], PSVec[B])) => T = {
          case (Some(x), _) => o.from(Left(x))
          case (None, vs)   => o.from(Right(vs.head))
        }

        def collectTo(
            s: S,
            lenBuf: IntArrBuilder,
            ysBuf: ObjArrBuilder,
            flatBuf: ObjArrBuilder,
        ): Unit =
          o.to(s) match
            case Left(x) =>
              lenBuf.unsafeAppend(0)
              ysBuf.unsafeAppend(x.asInstanceOf[AnyRef])
            case Right(a) =>
              lenBuf.unsafeAppend(1)
              ysBuf.unsafeAppend(null.asInstanceOf[AnyRef])
              flatBuf.unsafeAppend(a.asInstanceOf[AnyRef])

        def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
          if len == 0 then o.from(Left(y.asInstanceOf[o.X]))
          else o.from(Right(vys(pos)))

  /** Optional → MultiFocus[PSVec]. Mixes in `MultiFocusPSMaybeHit` so `mfAssocPSVec` skips the
    * per-element `Affine[o.X, Unit]` wrapper the generic path would build.
    */
  given affine2multifocusPSVec: Composer[Affine, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusPSMaybeHit[S, T, A, B]:
        type X = Either[Fst[o.X], Snd[o.X]]
        val to: S => (X, PSVec[A]) = s =>
          o.to(s) match
            case m: Affine.Miss[o.X, A] => (Left(m.fst), PSVec.empty[A])
            case h: Affine.Hit[o.X, A]  => (Right(h.snd), PSVec.singleton[A](h.b))
        val from: ((X, PSVec[B])) => T = {
          case (Left(fx), _)   => o.from(new Affine.Miss[o.X, B](fx))
          case (Right(sx), vs) => o.from(new Affine.Hit[o.X, B](sx, vs.head))
        }

        def collectTo(
            s: S,
            lenBuf: IntArrBuilder,
            ysBuf: ObjArrBuilder,
            flatBuf: ObjArrBuilder,
        ): Unit =
          o.to(s) match
            case m: Affine.Miss[o.X, A] =>
              lenBuf.unsafeAppend(0)
              ysBuf.unsafeAppend(m.fst.asInstanceOf[AnyRef])
            case h: Affine.Hit[o.X, A] =>
              lenBuf.unsafeAppend(1)
              ysBuf.unsafeAppend(h.snd.asInstanceOf[AnyRef])
              flatBuf.unsafeAppend(h.b.asInstanceOf[AnyRef])

        def reconstructSingleton(y: AnyRef, vys: PSVec[B], pos: Int, len: Int): T =
          if len == 0 then o.from(new Affine.Miss[o.X, B](y.asInstanceOf[Fst[o.X]]))
          else o.from(new Affine.Hit[o.X, B](y.asInstanceOf[Snd[o.X]], vys(pos)))

  /** MultiFocus[F] → SetterF. Uniform Setter widening for any `Functor[F]`. */
  given multifocus2setter[F[_]: Functor]: Composer[MultiFocus[F], SetterF] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, SetterF] =
      new Optic[S, T, A, B, SetterF]:
        type X = (S, A)
        val to: S => SetterF[X, A] = s => SetterF((s, identity[A]))
        val from: SetterF[X, B] => T = sfxb =>
          val (s, f) = sfxb.setter
          val (x, fa) = o.to(s)
          o.from((x, Functor[F].map(fa)(f)))

  // F[A]-focus factories.

  def fromLensF[F[_], S, T, A, B](
      lens: Optic[S, T, F[A], F[B], Tuple2]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = lens.X
      val to: S => (X, F[A]) = lens.to
      val from: ((X, F[B])) => T = lens.from

  def fromPrismF[F[_]: MonoidK, S, T, A, B](
      prism: Optic[S, T, F[A], F[B], Either]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Either[prism.X, Unit]
      val to: S => (X, F[A]) = s =>
        prism.to(s) match
          case Right(fa) => (Right(()), fa)
          case Left(x)   => (Left(x), MonoidK[F].empty[A])
      val from: ((X, F[B])) => T = {
        case (Left(xMiss), _) => prism.from(Left(xMiss))
        case (Right(_), fb)   => prism.from(Right(fb))
      }

  // Function1-shaped MultiFocus factories (the Grate-absorbed surface).

  /** Generic Function1-shaped factory — any `Representable[F]` container yields a
    * `MultiFocus[Function1[F.Representation, *]]`-carrier optic over `F[A]` with focus `A`.
    * Encoding: `X = Unit`, rebuild = `F.Representation => A`. On `to(fa)` snapshot `F.index(fa)`;
    * on `from((_, k))` materialise via `F.tabulate(k)`. The `.modify(f)` round-trip is exactly
    * `F.map(fa)(f)`.
    */
  def representable[F[_], A](using
      F: Representable[F]
  ): Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]] =
    new Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]:
      type X = Unit
      val to: F[A] => (Unit, F.Representation => A) = fa => ((), F.index(fa))
      val from: ((Unit, F.Representation => A)) => F[A] = { case (_, k) => F.tabulate(k) }

  /** Representable-indexed variant with explicit representative index. The `repr0` argument is
    * unused at runtime (rebuild operates pointwise via `F.tabulate`); preserved for API parity and
    * to leave the door open for a future `.lead` accessor.
    */
  def representableAt[F[_], A](F: Representable[F])(
      repr0: F.Representation
  ): Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]] =
    val _ = repr0
    new Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]:
      type X = Unit
      val to: F[A] => (Unit, F.Representation => A) = fa => ((), F.index(fa))
      val from: ((Unit, F.Representation => A)) => F[A] = { case (_, k) => F.tabulate(k) }

  /** Polymorphic homogeneous-tuple Function1-shaped factory. `to(t) = ((), i => t._i)`,
    * `from((_, k))` materialises via `Tuple.fromArray(Array.tabulate(size)(i => k(i)))`.
    *
    * @example
    *   {{{
    *   val g3 = MultiFocus.tuple[(Int, Int, Int), Int]
    *   g3.modify(_ + 1)((1, 2, 3))   // (2, 3, 4)
    *   g3.replace(42)((1, 2, 3))     // (42, 42, 42)
    *   }}}
    */
  def tuple[T <: Tuple, A](using
      sz: ValueOf[Tuple.Size[T]],
      ev: Tuple.Union[T] <:< A,
  ): Optic[T, T, A, A, MultiFocus[Function1[Int, *]]] =
    val _ = ev
    val size = sz.value
    new Optic[T, T, A, A, MultiFocus[Function1[Int, *]]]:
      type X = Unit
      val to: T => (Unit, Int => A) = t =>
        val read: Int => A = (i: Int) => t.productElement(i).asInstanceOf[A]
        ((), read)
      val from: ((Unit, Int => A)) => T = {
        case (_, k) =>
          val arr = new Array[Object](size)
          var i = 0
          while i < size do
            arr(i) = k(i).asInstanceOf[Object]
            i += 1
          Tuple.fromArray(arr).asInstanceOf[T]
      }

  // ------------------------------------------------------------------
  // Iso → Function1-shaped MultiFocus bridge — absorbs v1 `forgetful2grate`.
  // ------------------------------------------------------------------

  /** Iso ↪ MultiFocus[Function1[X0, *]]. Iso's forward `to: S => A` is broadcast to the constant
    * rebuild `_ => a`; the reverse reads the rebuild at any X0 (null sentinel — lead dropped per
    * fixedtraversal-fold-spike Q1).
    */
  given forgetful2multifocusFunction1[X0]: Composer[Forgetful, MultiFocus[Function1[X0, *]]] with

    def to[S, T, A, B](
        o: Optic[S, T, A, B, Forgetful]
    ): Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] =
      new Optic[S, T, A, B, MultiFocus[Function1[X0, *]]]:
        type X = Unit
        val to: S => (Unit, X0 => A) = s =>
          val a = o.to(s)
          ((), (_: X0) => a)
        val from: ((Unit, X0 => B)) => T = {
          case (_, k) => o.from(k(null.asInstanceOf[X0]))
        }

  def fromOptionalF[F[_]: MonoidK, S, T, A, B](
      opt: Optic[S, T, F[A], F[B], Affine]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Affine[opt.X, Unit]
      val to: S => (X, F[A]) = s =>
        opt.to(s) match
          case m: Affine.Miss[opt.X, F[A]] @unchecked =>
            (m.widenB[Unit], MonoidK[F].empty[A])
          case h: Affine.Hit[opt.X, F[A]] @unchecked =>
            (new Affine.Hit[opt.X, Unit](h.snd, ()), h.b)
      val from: ((X, F[B])) => T = {
        case (m: Affine.Miss[opt.X, Unit] @unchecked, _) =>
          opt.from(m.widenB[F[B]])
        case (h: Affine.Hit[opt.X, Unit] @unchecked, fb) =>
          opt.from(new Affine.Hit[opt.X, F[B]](h.snd, fb))
      }
