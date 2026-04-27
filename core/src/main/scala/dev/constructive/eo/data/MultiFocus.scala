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
  * `MultiFocus[F][X, A] = (X, F[A])`.
  *
  * The three families are structurally identical: each pairs a structural leftover `X` with a focus
  * collection / aggregate / rebuild-closure `F[A]`. Pre-unification:
  *   - `AlgLens[F]` exposed the F as a type parameter, used `Functor` / `Foldable` / `Traverse[F]`
  *     + `MonoidK[F]` from cats.
  *   - `Kaleidoscope` hid F as a path-dependent type member (`type FCarrier[_]`) and required the
  *     project-local `Reflector[F]` typeclass for its `.collect` universal.
  *   - `Grate` was `(A, X => A)` — pair of "lead position" + "rebuild closure" — for representable
  *     / Naperian shapes. Setting `F = Function1[X0, *]` collapses the pair into the same
  *     `(Y, F[A]) = (Y, X0 => A)` shape; the lead-position field was not externally observable in
  *     any shipped path-through (`grateFunctor.map`'s eager `f(a)` was discarded by every shipped
  *     `from`), so the spike dropped it entirely. See
  *     `docs/research/2026-04-29-fixedtraversal-fold-spike.md` for the empirical justification +
  *     perf evidence.
  *
  * `MultiFocus[F]` keeps the F-as-parameter encoding (matching AlgLens) and absorbs Kaleidoscope by
  * recognising that the only piece of `Reflector[F]` that wasn't already in cats was the
  * `reflect: (fa: F[A]) => (f: F[A] => B) => F[B]` operation. That operation has two natural
  * derivations and the choice is structural, not derived:
  *
  *   - "broadcast via `Functor.map`": `reflect(fa)(f) = fa.map(_ => f(fa))`. Length-preserving,
  *     matches the v1 ZipList Reflector and Const Reflector exactly. Requires only `Functor[F]`.
  *   - "broadcast via `Applicative.pure`": `reflect(fa)(f) = F.pure(f(fa))`. Singleton/cartesian,
  *     matches the v1 List Reflector. Requires `Applicative[F]`.
  *
  * Pick the first as the default. Users wanting the cartesian collapse compose with a downstream
  * `_.headOption` fold, OR construct `MultiFocus[List]` explicitly with the singleton choice via
  * the `collectVia` constructor.
  *
  * @tparam F
  *   classifier shape — constraint requirements depend on the operation:
  *   - `.modify` / `.replace` need `Functor[F]`.
  *   - `.foldMap` needs `Foldable[F]`.
  *   - `.modifyA` / `.all` need `Traverse[F]`.
  *   - `.collect` (the Kaleidoscope universal) needs `Functor[F]` for the default broadcast, or
  *     `Applicative[F]` for the cartesian-singleton variant.
  *   - Same-carrier `.andThen` needs `Traverse[F] + MultiFocusFromList[F]`.
  *   - Constructing a bridge via `fromPrismF` / `fromOptionalF` needs `MonoidK[F]` only.
  */
type MultiFocus[F[_]] = [X, A] =>> (X, F[A])

/** Singleton-classifier fast-path capability — preserved from AlgLensSingleton. Lets
  * `multiFocusAssoc` skip the `F.pure` wrap on push and the `pickSingletonOrThrow` on pull when the
  * inner optic is known to produce singletons. Sole shipped user: the `tuple2multifocus` Lens →
  * MultiFocus bridge.
  */
private[eo] trait MultiFocusSingleton[S, T, A, B, X0]:
  def singletonTo(s: S): (X0, A)
  def singletonFrom(x: X0, b: B): T

/** Per-F O(n) builder, identical role to the prior `AlgLensFromList`. `MonoidK[F].combineK` has
  * inconsistent asymptotics across F (O(n²) on Vector, lossy on Option), so we keep this typeclass
  * to gate the same-carrier `.andThen` push/pull paths.
  *
  * Multifocus-unification spike Q2 finding: `Traverse[F] + MonoidK[F]` is ENOUGH to derive
  * `fromList` in principle —
  * `xs.foldLeft(empty)((acc, a) => combineK(acc, pure(a)))` — but the asymptotics regress on Vector
  * (O(n²)) and the cardinality is silently dropped on Option. The typeclass is therefore a known
  * per-F cost we carry forward unchanged.
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
    * array). This is the crucial perf hook that lets `MultiFocus[PSVec]`'s `composeFrom` hand each
    * inner reassembly an O(1) slice of the shared flat focus vector, just like the legacy
    * `PowerSeries.assoc` did.
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

/** PSVec-specialised capability trait for the `MultiFocus[PSVec]` MaybeHit fast-path. Mirrors the
  * legacy `PowerSeries.PSSingleton` (the non-AlwaysHit variant): used by Prism / Optional morphs
  * that produce a 0- or 1-element focus vector, where the generic `inner.to(s)` would build an
  * `Either`/`Option`-shaped wrapper around the focus that the fast-path can elide.
  *
  * AlwaysHit (Lens) morphs already get a fast-path via the carrier-wide `MultiFocusSingleton`. This
  * trait is the maybe-hit complement, scoped to PSVec because its body writes directly into the
  * `IntArrBuilder` (per-element 0/1 length) and `ObjArrBuilder` (parallel ys / flat) the
  * PSVec-specialised `mfAssoc` body uses.
  *
  * Powerseries-fold-spike Q3 finding: kept after measurement. The Prism fast-path's elision of
  * the per-element `Option` / `Either` wrapper allocation is empirically observable on
  * `PowerSeriesPrismBench`.
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

  // ------------------------------------------------------------------
  // PSVec cats instances — load-bearing for `MultiFocus[PSVec]`'s
  // `mfFunctor` / `mfFold` / `mfTraverse` derivations. Ship them inside
  // the companion so callers picking up `import data.MultiFocus.given`
  // get them transitively, alongside `mfFunctor` etc.
  // ------------------------------------------------------------------

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

  // ------------------------------------------------------------------
  // Capability instances — Functor / Foldable / Traverse over the F[A] half.
  // (Ported verbatim from AlgLens; the Kaleidoscope-side `kalFunctor`'s
  // broadcast-rebuild trick is no longer needed because MultiFocus carries
  // the leftover X as a plain value, not as a `FCarrier[A] => X` closure.)
  // ------------------------------------------------------------------

  given mfFunctor[F[_]: Functor]: ForgetfulFunctor[MultiFocus[F]] with

    def map[X, A, B](xa: (X, F[A]), f: A => B): (X, F[B]) =
      (xa._1, Functor[F].map(xa._2)(f))

  given mfFold[F[_]: Foldable]: ForgetfulFold[MultiFocus[F]] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, F[A])) => M =
      f => xa => Foldable[F].foldMap(xa._2)(f)

  given mfTraverse[F[_]: Traverse]: ForgetfulTraverse[MultiFocus[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: ((X, F[A])) => (A => G[B]) => G[(X, F[B])] =
      xa => f => Applicative[G].map(Traverse[F].traverse(xa._2)(f))(fb => (xa._1, fb))

  // ------------------------------------------------------------------
  // Same-carrier composition — the AlgLens.assocAlgMonad logic, F-parametric.
  // (Singleton fast-path preserved; structurally identical.)
  // ------------------------------------------------------------------

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

  // ------------------------------------------------------------------
  // Same-carrier composition — Grate-shaped Function1[X0, *] specialisation.
  // Absorbs the v1 `grateAssoc`. The general `mfAssoc` requires `Traverse[F]`
  // + `MultiFocusFromList[F]`; `Function1[X0, *]` admits neither, so this
  // separate instance handles the Grate-style read-only / closure-rebuild
  // case at higher priority for the Function1-shaped F. Z = (Xo, Xi); the
  // outer rebuild is structurally unreachable post-collapse-through-outer-
  // focus, identical to the v1 grateAssoc's design — for every shipped outer
  // (iso-morphed, tuple-built) the rebuild is broadcast-shaped, so a constant
  // fallback is exact. See `docs/research/2026-04-29-fixedtraversal-fold-spike.md`
  // for the load-bearing invariant absorbed from the deleted Grate.scala.
  // ------------------------------------------------------------------

  given mfAssocFunction1[X0, Xo, Xi]: AssociativeFunctor[MultiFocus[Function1[X0, *]], Xo, Xi] with
    type Z = (Xo, Xi)

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[Function1[X0, *]]] { type X = Xi },
    ): ((Xo, Xi), X0 => C) =
      val (_, kO) = outer.to(s)
      // Read the outer focus via the rebuild closure at any X0 — null sentinel works because
      // `.modify` only consumes the rebuild, not the lead. The fixedtraversal-fold-spike's Q1
      // finding shows .modify doesn't observe the focus value, so a real index here would be
      // discarded by every shipped `from`.
      val a: A = kO(null.asInstanceOf[X0])
      val (_, kI) = inner.to(a)
      ((null.asInstanceOf[Xo], null.asInstanceOf[Xi]), kI)

    def composeFrom[S, T, A, B, C, D](
        xd: ((Xo, Xi), X0 => D),
        inner: Optic[A, B, C, D, MultiFocus[Function1[X0, *]]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] { type X = Xo },
    ): T =
      val (_, kD) = xd
      // Inner rebuild = the supplied X0 => D closure. Outer rebuild = constant broadcast,
      // identical to the v1 grateAssoc's design — for every shipped outer (iso-morphed,
      // tuple-built) the rebuild is broadcast-shaped, so a constant fallback is exact.
      val b: B = inner.from((null.asInstanceOf[Xi], kD))
      outer.from((null.asInstanceOf[Xo], (_: X0) => b))

  // ------------------------------------------------------------------
  // Same-carrier composition — `MultiFocus[PSVec]` specialisation.
  //
  // Absorbs the legacy `PowerSeries.assoc` body verbatim. The general
  // `mfAssoc` body would build two intermediate `List` accumulators per
  // composeTo (cList + xiList), then reverse + materialise via
  // `MultiFocusFromList[PSVec].fromList`. The specialised body below
  // writes directly into `IntArrBuilder` / `ObjArrBuilder` exactly as
  // `PowerSeries.assoc` did, recovering the parallel-array `AssocSndZ`
  // representation: `Z = (Xo, AssocSndZ[Xo, Xi])` carries an `Array[Int]`
  // of per-outer focus counts (or `null` for the always-hit case) plus an
  // `Array[AnyRef]` of per-outer leftovers, sidestepping the per-element
  // `(Xi, Int)` Tuple2 + `F[(Xi, Int)]` materialisation the generic body
  // pays.
  //
  // Singleton fast-path (powerseries-fold-spike Q3) — the AlwaysHit branch
  // reuses the carrier-wide `MultiFocusSingleton` trait (already mixed in
  // by `tuple2multifocus`); no new trait is needed for AlwaysHit. The
  // MaybeHit branch (Prism / Optional morphs) DOES use a separate
  // PSVec-scoped trait `MultiFocusPSMaybeHit` because its semantics —
  // pushing into `lenBuf` / `ysBuf` / `flatBuf` directly to skip the
  // intermediate `Either[X, Unit]` / `Affine[X, Unit]` wrappers the
  // generic morph would build — only make sense inside the PSVec body.
  // ------------------------------------------------------------------

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
          // mix in MultiFocusSingleton or MultiFocusPSMaybeHit. The shipped factories
          // (tuple2multifocusPSVec / either2multifocusPSVec / affine2multifocusPSVec) all do, so
          // this branch is the structural escape hatch for downstream PSVec-carrier authors.
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
          // Generic fallback — paired with the composeTo escape-hatch branch above. Reachable
          // only via user-built MultiFocus[PSVec] optics that don't mix in MultiFocusSingleton
          // or MultiFocusPSMaybeHit; every shipped factory provides one of those witnesses.
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

  /** Composed existential leftover for `MultiFocus[PSVec]` `andThen`. Stores the outer leftover, a
    * primitive `Array[Int]` of per-outer focus counts, and an `Array[AnyRef]` of per-outer inner
    * leftovers — parallel arrays rather than a Tuple2-of-two arrays, so each per-element entry pays
    * one primitive-int write and one reference write instead of allocating an intermediate
    * `(Int, Xi)` Tuple2. When the inner is a `MultiFocusSingleton` (a morphed Lens — every call
    * yields exactly one focus), `lens` is left `null`: every per-element length is implicitly 1.
    * Absorbed verbatim from the legacy `PowerSeries.AssocSndZ`.
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
      // List default: cartesian / singleton — matches v1 Reflector[List]. The mapping `T = List[B]`
      // is preserved by post-wrapping `List(b)`.
      (s: S) =>
        val (_, fa) = o.to(s)
        val b: B = agg(fa.asInstanceOf[List[A]])
        o.from((null.asInstanceOf[o.X], List(b)).asInstanceOf[(o.X, List[B])])

  /** Functor-broadcast variant — preserves F-shape via `map(_ => agg(fa))`. Works for any
    * `Functor[F]`. Matches v1 ZipList / Const semantics; collapses List into ZipList-style
    * length-preserving for List.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Functor[F])

    def collectMap[C](agg: F[A] => C)(using ev: C =:= B): S => T =
      val _ = ev
      (s: S) =>
        val (x, fa) = o.to(s)
        val b: C = agg(fa)
        val fb: F[B] = F.map(fa)(_ => b.asInstanceOf[B])
        o.from((x, fb))

  // ------------------------------------------------------------------
  // Read-only escape — `multiFocus.foldMap(f)` is provided by the
  // carrier-wide `Optic.foldMap` extension, gated on `ForgetfulFold[F]`
  // which `mfFold[F: Foldable]` already supplies. No MultiFocus-specific
  // extension method needed; the user's capability lights up via the
  // existing `Optic.foldMap` whenever `F: Foldable` is in scope.
  //
  // Why no `Composer[MultiFocus[F], Forget[F]]`? `forget2multifocus`
  // ships in the OPPOSITE direction; a bidirectional pair would create
  // ambiguity in Morph resolution and break every existing
  // `forget.andThen(multifocus)` chain.
  // ------------------------------------------------------------------

  // ------------------------------------------------------------------
  // Distributive / Representable typeclass-gated method set — absorbed
  // from the v1 Grate carrier's read surface. `.at(i)` is the single
  // load-bearing addition; provides O(1) per-position read once a
  // `Representable[F]` witness is in scope.
  // ------------------------------------------------------------------

  /** Read the focus at a representative position. Requires `Representable[F]` so the rebuild
    * closure can be probed at index `i` to recover the focus there. For the absorbed-Grate carrier
    * `MultiFocus[Function1[X0, *]]`, `Representable[Function1[X0, *]]` (from cats) makes this
    * trivial — `Representation = X0`, `index(fa)(i) = fa(i)`.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Representable[F])

    def at(i: F.Representation): S => A = (s: S) =>
      val (_, fa) = o.to(s)
      F.index(fa)(i)

  // ------------------------------------------------------------------
  // Constructors — preserved from both AlgLens (Forget/Tuple2/Either/Affine
  // bridges, F[A]-focus factories) and Kaleidoscope (apply[F]).
  // ------------------------------------------------------------------

  /** Generic factory mirroring `Kaleidoscope.apply[F, A]`. Encoding: `X = F[A]`, focus = fa,
    * rebuild = identity. Replaces `Kaleidoscope.apply` cleanly because under MultiFocus the F is in
    * the optic's type, not on a path-dependent member.
    */
  def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]] =
    new Optic[F[A], F[A], A, A, MultiFocus[F]]:
      type X = F[A]
      val to: F[A] => (F[A], F[A]) = (fa: F[A]) => (fa, fa)
      val from: ((F[A], F[A])) => F[A] = { case (_, fb) => fb }

  /** Iso → MultiFocus bridge. Replaces both `forgetful2kaleidoscope` and the Iso → AlgLens path
    * (which previously fell out of `Morph.viaTuple2`'s low-priority chain).
    *
    * Requires `Applicative[F]` because the Iso's focus is a plain `A` and we need to broadcast it
    * into a singleton `F[A]`. Mirrors `Composer[Forgetful, AlgLens[F]]`'s old constraint set.
    */
  given forgetful2multifocus[F[_]: Applicative: Foldable]: Composer[Forgetful, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forgetful]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), Applicative[F].pure(o.to(s)))
        val from: ((Unit, F[B])) => T = {
          // precondition: fb is singleton (Applicative.pure-wrapped); throws on |fb| != 1
          case (_, fb) => o.from(pickSingletonOrThrow(fb, "Forgetful"))
        }

  /** Forget[F] ↪ MultiFocus[F]. Same shape as the prior `forget2alg`. */
  given forget2multifocus[F[_]]: Composer[Forget[F], MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), o.to(s))
        val from: ((Unit, F[B])) => T = { case (_, fb) => o.from(fb) }

  /** Lens → MultiFocus[F]. Same shape as the prior `tuple2alg`; mixes in `MultiFocusSingleton` so
    * the mfAssoc fast-path fires.
    */
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
          // precondition: fb is singleton (Applicative.pure-wrapped); throws on |fb| != 1
          case (x, fb) =>
            o.from((x, pickSingletonOrThrow(fb, "Tuple2")))
        }

  /** Prism → MultiFocus[F]. Same shape as the prior `either2alg`. */
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
          // precondition on Right branch: fb is singleton (Applicative.pure-wrapped); throws on |fb| != 1
          case (Right(_), fb)   => o.from(Right(pickSingletonOrThrow(fb, "Either")))
        }

  /** Optional → MultiFocus[F]. Same shape as the prior `affine2alg`. */
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
          // precondition on Hit branch: fb is singleton (Applicative.pure-wrapped); throws on |fb| != 1
          case (Right(sndX), fb) =>
            o.from(new Affine.Hit[o.X, B](sndX, pickSingletonOrThrow(fb, "Affine")))
        }

  // ------------------------------------------------------------------
  // PSVec-specialised Composer instances. The generic `*2multifocus[F]`
  // bridges above demand `Applicative[F]` / `Alternative[F]`, neither of
  // which `PSVec` admits naturally (no zip-Applicative shape, no MonoidK
  // beyond concatenation we don't need). The specialised Composers below
  // directly use `PSVec.singleton` / `PSVec.empty`, mirroring the legacy
  // `PowerSeries.tuple2ps` / `either2ps` / `affine2ps` bridges.
  //
  // The Tuple2 → MultiFocus[PSVec] bridge specialises further on
  // `optics.GetReplaceLens` to skip the intermediate `(s, get(s))` Tuple2
  // the generic Tuple2 path would build — same fast-path the legacy
  // `GetReplaceLensInPS` had. This is observable on the `mfPath` and
  // `psPath` benches.
  //
  // The Either / Affine bridges mix in `MultiFocusPSMaybeHit` so the
  // PSVec-specialised `mfAssocPSVec` body picks up the Prism/Optional
  // fast-path (skip the per-element `Either[X, Unit]` / `Affine[X, Unit]`
  // wrapper that the generic morph would build).
  // ------------------------------------------------------------------

  /** `Composer[Tuple2, MultiFocus[PSVec]]` — lifts a Lens-carrier optic into MultiFocus[PSVec] so
    * `lens.andThen(traversal)` type-checks. Specialises on `optics.GetReplaceLens` for the fast
    * path (skips the intermediate `(s, get(s))` Tuple2 that the generic body would build on every
    * access). Mirror of the legacy `tuple2ps`.
    *
    * The `to` body builds anonymous Optic[…, MultiFocus[PSVec]] values inline rather than via
    * top-level classes — this is required so `MultiFocusSingleton[S, T, A, B, X0]` can refer to the
    * original `o.X` path-dependent type without tripping Scala 3's "class parent cannot refer to
    * constructor parameters" rule.
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

  /** `Composer[Affine, MultiFocus[PSVec]]` — lifts an Optional-carrier optic into MultiFocus[PSVec]
    * so `optional.andThen(traversal)` type-checks. Mixes in `MultiFocusPSMaybeHit` so the
    * PSVec-specialised `mfAssocPSVec` body skips the per-element `Affine[o.X, Unit]` wrapper the
    * generic `to` path would build. Mirror of the legacy `affine2ps`.
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

  /** MultiFocus[F] → SetterF. Replaces both `kaleidoscope2setter` and the (latent, never-shipped)
    * `alg2setter`. Closes the U → N gap from the composition gap analysis: the Kaleidoscope row of
    * the Composer matrix now has a uniform Setter widening for ALL F.
    */
  given multifocus2setter[F[_]: Functor]: Composer[MultiFocus[F], SetterF] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, SetterF] =
      new Optic[S, T, A, B, SetterF]:
        type X = (S, A)
        val to: S => SetterF[X, A] = s => SetterF((s, identity[A]))
        val from: SetterF[X, B] => T = sfxb =>
          val (s, f) = sfxb.setter
          val (x, fa) = o.to(s)
          o.from((x, Functor[F].map(fa)(f)))

  // ------------------------------------------------------------------
  // F[A]-focus factories — preserved from AlgLens.
  // ------------------------------------------------------------------

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

  // ------------------------------------------------------------------
  // Grate-fold absorbed factories — `representable` and `tuple` (both
  // produce `MultiFocus[Function1[X0, *]]`-carrier optics).
  // ------------------------------------------------------------------

  /** Generic Function1-shaped MultiFocus factory — any `Representable[F]` container `F[_]` with
    * element type `A` yields a `MultiFocus[Function1[F.Representation, *]]`-carrier optic over
    * `F[A]` with focus `A`. Absorbs the v1 `Grate.apply[F: Representable]`.
    *
    * Encoding: `X = Unit`. The rebuild slot is `F.Representation => A` — a per-index read of the
    * current container. On `to(fa)`, snapshot the index function `F.index(fa)`; on `from((_, k))`,
    * materialise via `F.tabulate(k)`.
    *
    * After `.modify(f)` via `mfFunctor[Function1[F.Representation, *]]`:
    *   1. `to(fa) = ((), F.index(fa))`
    *   2. `mfFunctor.map(_, f) = ((), F.index(fa) andThen f)`
    *   3. `from((_, k andThen f)) = F.tabulate(r => f(F.index(fa)(r))) = F.map(fa)(f)`
    *
    * which is exactly the Functor-map over the representable container — semantically identical to
    * the v1 Grate's `representableGrate` body.
    */
  def representable[F[_], A](using
      F: Representable[F]
  ): Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]] =
    new Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]:
      type X = Unit
      val to: F[A] => (Unit, F.Representation => A) = fa => ((), F.index(fa))
      val from: ((Unit, F.Representation => A)) => F[A] = { case (_, k) => F.tabulate(k) }

  /** Representable-indexed variant with explicit representative index — absorbs v1 `Grate.at`. The
    * `repr0` argument is currently unused at runtime (the rebuild closure operates pointwise via
    * `F.tabulate`); it is preserved for API parity with the v1 `Grate.at` and to leave the door
    * open for a future `.lead` accessor.
    */
  def representableAt[F[_], A](F: Representable[F])(
      repr0: F.Representation
  ): Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]] =
    val _ = repr0 // captured at construction; preserved for API parity with v1 `Grate.at`
    new Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]:
      type X = Unit
      val to: F[A] => (Unit, F.Representation => A) = fa => ((), F.index(fa))
      val from: ((Unit, F.Representation => A)) => F[A] = { case (_, k) => F.tabulate(k) }

  /** Polymorphic homogeneous-tuple Function1-shaped MultiFocus factory. Absorbs the v1
    * `Grate.tuple[T <: Tuple, A]`.
    *
    * Encoding: `X = Unit`, F = `Function1[Int, *]`. `to(t) = ((), i => t._i)`; `from((_, k))`
    * materialises the result as `Tuple.fromArray(Array.tabulate(size)(i => k(i)))` — applying the
    * rebuild at every slot. Same per-slot semantics as v1 Grate.tuple.
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

  /** Trivial injection `Forgetful ↪ MultiFocus[Function1[X0, *]]` for any X0. Lets an Iso-carrier
    * optic compose against a Function1-shaped MultiFocus-carrier optic via cross-carrier
    * `.andThen`. Absorbs v1 `forgetful2grate`.
    *
    * The Iso's forward `to: S => A` is broadcast to the constant rebuild closure `_ => o.to(s)`;
    * the reverse `from: B => T` reads the rebuild at any X0 (null sentinel) and drives the pull
    * side. This is a Forgetful → broadcasted-Function1 lift, semantically identical to v1
    * forgetful2grate's `(o.to(s), s0 => o.to(s0))` pair (the lead is dropped, see
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
