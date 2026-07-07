package dev.constructive.eo
package data

import scala.annotation.tailrec

import cats.{Alternative, Applicative, Foldable, Functor, Monoid, MonoidK, Representable, Traverse}

import forgetful.*
import compose.*
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
// The uncurried [[MultiFocusK]] is `opaque` for the same reason [[Direct]] is: a transparent
// alias dealiases to a bare pair lambda with no companion in implicit scope, so every instance
// below (`mfAssoc*`, the `Composer` bridges, …) was invisible without
// `import data.MultiFocus.given`. With the opaque anchor inside, dealiasing
// `MultiFocus[F][X, A]` stops at `MultiFocusK[F, X, A]`, whose companion (where the instances
// live) *is* an implicit-scope anchor — no import needed. (`MultiFocus` itself must stay a plain
// alias: an opaque type cannot have the curried `[F[_]] => [X, A] =>> …` shape.) Within this file
// the opaque is transparent; outside, [[MultiFocusK.wrap]] / [[MultiFocusK.context]] /
// [[MultiFocusK.foci]] are the (runtime-identity) boundary.
opaque type MultiFocusK[F[_], X, A] = (X, F[A])

/** Curried carrier view of [[MultiFocusK]] — the `F[_, _]` shape `Optic` expects. */
type MultiFocus[F[_]] = [X, A] =>> MultiFocusK[F, X, A]

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
        @tailrec def loop(i: Int, cur: List[A]): Unit =
          if cur.nonEmpty then
            arr(i) = cur.head.asInstanceOf[AnyRef]
            loop(i + 1, cur.tail)
        loop(0, xs)
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

object MultiFocusK:

  // Construct via the façade `MultiFocus(x, fa)` (an `apply` on the public object below); these
  // are the unwrap boundary. Both are identity at runtime (the opaque erases to the pair).
  extension [F[_], X, A](self: MultiFocusK[F, X, A])
    /** The structural leftover. Identity at runtime. */
    transparent inline def context: X = self._1

    /** The focus vector. Identity at runtime. */
    transparent inline def foci: F[A] = self._2

  // Capability instances — Functor / Foldable / Traverse over the F[A] half.

  // NOTE: member signatures below are spelled `MultiFocus[F][X, A]`, not `(X, F[A])`. The two are
  // equal inside this file (the opaque is transparent here), but a `given … with` instance is
  // typed by its anonymous class, so the *written* member signatures are what inline extensions
  // (`Optic.modify` / `.modifyA` / `.foldMap`, …) splice against at call sites — where only the
  // opaque spelling typechecks.
  given mfFunctor[F[_]: Functor]: ForgetfulFunctor[MultiFocus[F]] with

    def map[X, A, B](xa: MultiFocus[F][X, A], f: A => B): MultiFocus[F][X, B] =
      (xa._1, Functor[F].map(xa._2)(f))

  given mfFold[F[_]: Foldable]: ForgetfulFold[MultiFocus[F]] with

    def foldMap[X, A, M: Monoid](f: A => M, xa: MultiFocus[F][X, A]): M =
      Foldable[F].foldMap(xa._2)(f)

  given mfTraverse[F[_]: Traverse]: ForgetfulTraverse[MultiFocus[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative](
        xa: MultiFocus[F][X, A],
        f: A => G[B],
    ): G[MultiFocus[F][X, B]] =
      Applicative[G].map(Traverse[F].traverse(xa._2)(f))(fb => (xa._1, fb))

  // Same-carrier composition — F-parametric, with `MultiFocusSingleton` fast-path on the inner.

  given mfAssoc[F[_]: Traverse: MultiFocusFromList, Xo, Xi]
      : AssociativeFunctor[MultiFocus[F], Xo, Xi] with
    type Z = (Xo, F[(Xi, Int)])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[F]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[F]] { type X = Xi },
    ): MultiFocus[F][Z, C] =
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
        xd: MultiFocus[F][Z, D],
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

  private[eo] def pickSingletonOrThrow[F[_]: Foldable, B](fb: F[B], carrier: String): B =
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
    ): MultiFocus[Function1[X0, *]][Z, C] =
      val (_, kO) = outer.to(s)
      // Null sentinel works because `.modify` doesn't observe the focus value (spike Q1).
      val a: A = kO(null.asInstanceOf[X0])
      val (_, kI) = inner.to(a)
      ((null.asInstanceOf[Xo], null.asInstanceOf[Xi]), kI)

    def composeFrom[S, T, A, B, C, D](
        xd: MultiFocus[Function1[X0, *]][Z, D],
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
    ): MultiFocus[PSVec][Z, C] =
      val (xo, va) = outer.to(s)
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      inner match
        case ah: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          // Always-hit fast path (mfSingleton): every call produces exactly one focus.
          val flatBuf = new ObjArrBuilder(n)
          @tailrec def loop(i: Int): Unit =
            if i < n then
              val (xi, c) = ah.singletonTo(va(i))
              ysBuf.unsafeAppend(xi.asInstanceOf[AnyRef])
              flatBuf.unsafeAppend(c.asInstanceOf[AnyRef])
              loop(i + 1)
          loop(0)
          val sndZ = new AssocSndZ[Xo, Xi](xo, null, ysBuf.freezeArr)
          (sndZ, flatBuf.freezeAsPSVec[C])
        case mh: MultiFocusPSMaybeHit[A, B, C, D] @unchecked =>
          // Maybe-hit fast path (Prism / Optional morphs).
          val lenBuf = new IntArrBuilder(n)
          val flatBuf = new ObjArrBuilder(n)
          @tailrec def loop(i: Int): Unit =
            if i < n then
              mh.collectTo(va(i), lenBuf, ysBuf, flatBuf)
              loop(i + 1)
          loop(0)
          val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
          (sndZ, flatBuf.freezeAsPSVec[C])
        case _ =>
          // Generic fallback: taken when `inner` is neither a `MultiFocusSingleton` (Lens bridge)
          // nor a `MultiFocusPSMaybeHit` (Prism / Optional bridge) — i.e. a multi-focus inner such
          // as `each` itself, or any composed `MultiFocus[PSVec]` optic. Reached on the common
          // left-associated `lens.andThen(each)…` shape, so it is a real hot path, not merely a
          // downstream escape hatch.
          if n == 1 then
            // Single outer focus (e.g. a Lens onto a collection field, then `each`): the inner's
            // own focus vector IS the entire flat focus. Emit it zero-copy — no `flatBuf`, no
            // grow, no O(n) `appendAllFromPSVec` copy. `composeFrom`'s generic branch reconstructs
            // from a one-entry `lens` array + `vys.slice`, so this stays symmetric.
            val (xi, vy) = inner.to(va(0))
            ysBuf.unsafeAppend(xi.asInstanceOf[AnyRef])
            val lenArr = new Array[Int](1)
            lenArr(0) = vy.length
            val sndZ = new AssocSndZ[Xo, Xi](xo, lenArr, ysBuf.freezeArr)
            (sndZ, vy)
          else
            // `flatBuf`'s final size is the sum of the inners' cardinalities — unknown up front.
            // Floor the capacity at `max(n, 16)`: `n` is a lower bound on the total, and the `16`
            // keeps a small-`n`/high-fanout chain (e.g. `each.andThen(each)` over few-but-large
            // sub-containers) from doubling *more* than the old default-capacity-16 builder did.
            val lenBuf = new IntArrBuilder(n)
            val flatBuf = new ObjArrBuilder(math.max(n, 16))
            @tailrec def loop(i: Int): Unit =
              if i < n then
                val (xi, vy) = inner.to(va(i))
                lenBuf.append(vy.length)
                ysBuf.append(xi.asInstanceOf[AnyRef])
                flatBuf.appendAllFromPSVec(vy)
                loop(i + 1)
            loop(0)
            val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
            (sndZ, flatBuf.freezeAsPSVec[C])

    def composeFrom[S, T, A, B, C, D](
        xd: MultiFocus[PSVec][Z, D],
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
        outer: Optic[S, T, A, B, MultiFocus[PSVec]] { type X = Xo },
    ): T =
      val (sndZ, vys) = xd
      val lens = sndZ.lens
      val ys = sndZ.ys
      // `resultBuf` is sized to `ys.length` and every branch below appends EXACTLY `ys.length`
      // times (each outer element produced one `ys` entry and one `lens` entry in `composeTo`, so
      // `lensArr.length == ys.length`). That invariant is what makes the `unsafeAppend`s sound — it
      // must hold for all three branches; do not append conditionally.
      val resultBuf = new ObjArrBuilder(ys.length)
      inner match
        case ah: MultiFocusSingleton[A, B, C, D, Xi] @unchecked =>
          // Always-hit fast path (lens == null, every element hits exactly once).
          val n = ys.length
          @tailrec def loop(i: Int): Unit =
            if i < n then
              resultBuf.unsafeAppend(
                ah.singletonFrom(ys(i).asInstanceOf[Xi], vys(i)).asInstanceOf[AnyRef]
              )
              loop(i + 1)
          loop(0)
        case mh: MultiFocusPSMaybeHit[A, B, C, D] @unchecked =>
          val lensArr = lens.nn
          val n = lensArr.length
          @tailrec def loop(i: Int, offset: Int): Unit =
            if i < n then
              val len = lensArr(i)
              resultBuf.unsafeAppend(
                mh.reconstructSingleton(ys(i), vys, offset, len).asInstanceOf[AnyRef]
              )
              loop(i + 1, offset + len)
          loop(0, 0)
        case _ =>
          // Generic fallback paired with composeTo's escape-hatch branch above.
          val lensArr = lens.nn
          val n = lensArr.length
          @tailrec def loop(i: Int, offset: Int): Unit =
            if i < n then
              val len = lensArr(i)
              val y = ys(i).asInstanceOf[Xi]
              val chunk = vys.slice(offset, offset + len)
              resultBuf.unsafeAppend(inner.from((y, chunk)).asInstanceOf[AnyRef])
              loop(i + 1, offset + len)
          loop(0, 0)
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
  // `ForgetfulFold[MultiFocus[F]]` (`mfFold[F: Foldable]`). An explicit `Composer[MultiFocus[F],
  // Forget[F]]` (`multifocus2forget`, defined below) also ships as a read-only escape — it's the
  // structural inverse of `forget2multifocus`. A bidirectional pair would normally break Morph
  // resolution; this one ships safely only because it's restricted to `T = Unit` (see that given's
  // docstring for the full rationale).

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
      def to(fa: F[A]): (F[A], F[A]) = (fa, fa)
      def from(pair: (F[A], F[A])): F[A] = pair._2

  /** Iso → MultiFocus[F]. Requires `Applicative[F]` to broadcast the Iso's plain `A` focus into a
    * singleton `F[A]`.
    */
  given forgetful2multifocus[F[_]: Applicative: Foldable]: Composer[Direct, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        def to(s: S): (Unit, F[A]) = ((), Applicative[F].pure(o.to(s).value))
        def from(pair: (Unit, F[B])): T =
          o.from(Direct(pickSingletonOrThrow(pair._2, "Direct")))

  /** Forget[F] ↪ MultiFocus[F]. */
  given forget2multifocus[F[_]]: Composer[Forget[F], MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        def to(s: S): (Unit, F[A]) = ((), o.to(s).value)
        def from(pair: (Unit, F[B])): T = o.from(ForgetK(pair._2))

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
        def to(s: S): ForgetK[F, X, A] = ForgetK(o.to(s)._2)
        def from(fb: ForgetK[F, X, B]): T =
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
        def to(s: S): (X, F[A]) =
          val (x, a) = o.to(s)
          (x, Applicative[F].pure(a))
        def from(pair: (X, F[B])): T =
          o.from((pair._1, pickSingletonOrThrow(pair._2, "Tuple2")))

  /** Shared hit marker for the `X = Either[…, Unit]` bridges — covariance upcasts
    * `Either[Nothing, Unit]` to any `Either[x, Unit]`, so one instance serves every hit.
    */
  private val hitUnit: Either[Nothing, Unit] = Right(())

  /** Prism → MultiFocus[F]. */
  given either2multifocus[F[_]: Alternative: Foldable]: Composer[Either, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[o.X, Unit]
        def to(s: S): (X, F[A]) =
          o.to(s) match
            case Right(a)    => (hitUnit, Applicative[F].pure(a))
            case l @ Left(_) => (l.widenRight[Unit], Alternative[F].empty[A])
        def from(pair: (X, F[B])): T =
          pair match
            case (l @ Left(_), _) => o.from(l.widenRight[B])
            case (Right(_), fb)   => o.from(Right(pickSingletonOrThrow(fb, "Either")))

  /** Optional → MultiFocus[F]. */
  given affine2multifocus[F[_]: Alternative: Foldable]: Composer[Affine, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        // X = the Affine itself (miss recycled via widenB, both directions) rather than an
        // unpacked Either[Fst, Snd] — same shape as `multifocusF2multifocus` below.
        type X = Affine[o.X, Unit]
        def to(s: S): (X, F[A]) =
          o.to(s) match
            case h: Affine.Hit[o.X, A] =>
              (new Affine.Hit[o.X, Unit](h.snd, ()), Applicative[F].pure(h.b))
            case m: Affine.Miss[o.X, A] =>
              (m.widenB[Unit], Alternative[F].empty[A])
        def from(pair: (X, F[B])): T =
          pair match
            case (m: Affine.Miss[o.X, Unit] @unchecked, _) =>
              o.from(m.widenB[B])
            case (h: Affine.Hit[o.X, Unit] @unchecked, fb) =>
              o.from(new Affine.Hit[o.X, B](h.snd, pickSingletonOrThrow(fb, "Affine")))

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
            def to(s: S): (S, PSVec[A]) = (s, PSVec.singleton[A](lens.get(s)))
            def from(pair: (S, PSVec[B])): T = lens.enplace(pair._1, pair._2.head)
            def singletonTo(s: S): (S, A) = (s, lens.get(s))
            def singletonFrom(x: S, b: B): T = lens.enplace(x, b)
        case _ =>
          new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusSingleton[S, T, A, B, o.X]:
            type X = o.X
            def to(s: S): (o.X, PSVec[A]) =
              val (xo, a) = o.to(s)
              (xo, PSVec.singleton[A](a))
            def from(pair: (o.X, PSVec[B])): T = o.from((pair._1, pair._2.head))
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
        def to(s: S): (Option[o.X], PSVec[A]) =
          o.to(s) match
            case Left(x)  => (Some(x), PSVec.empty[A])
            case Right(a) => (None, PSVec.singleton[A](a))
        def from(pair: (Option[o.X], PSVec[B])): T =
          pair match
            case (Some(x), _) => o.from(Left(x))
            case (None, vs)   => o.from(Right(vs.head))

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
        // X = the Affine itself (miss recycled via widenB, both directions) — see
        // `affine2multifocus`. The collectTo / reconstructSingleton buffer protocol below
        // is X-independent and keeps its unpacked fst / snd encoding.
        type X = Affine[o.X, Unit]
        def to(s: S): (X, PSVec[A]) =
          o.to(s) match
            case m: Affine.Miss[o.X, A] => (m.widenB[Unit], PSVec.empty[A])
            case h: Affine.Hit[o.X, A]  =>
              (new Affine.Hit[o.X, Unit](h.snd, ()), PSVec.singleton[A](h.b))
        def from(pair: (X, PSVec[B])): T =
          pair match
            case (m: Affine.Miss[o.X, Unit] @unchecked, _) => o.from(m.widenB[B])
            case (h: Affine.Hit[o.X, Unit] @unchecked, vs) =>
              o.from(new Affine.Hit[o.X, B](h.snd, vs.head))

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

  /** MultiFocus[F] → ModifyF. Uniform Modify widening for any `Functor[F]`. */
  given multifocus2modify[F[_]: Functor]: Composer[MultiFocus[F], ModifyF] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, ModifyF] =
      new Optic[S, T, A, B, ModifyF]:
        type X = (S, A)
        def to(s: S): ModifyF[X, A] = ModifyF((s, identity[A]))
        def from(sfxb: ModifyF[X, B]): T =
          val (s, f) = sfxb.modifier
          val (x, fa) = o.to(s)
          o.from((x, Functor[F].map(fa)(f)))

  // F[A]-focus factories.

  def fromLensF[F[_], S, T, A, B](
      lens: Optic[S, T, F[A], F[B], Tuple2]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = lens.X
      def to(s: S): (X, F[A]) = lens.to(s)
      def from(pair: (X, F[B])): T = lens.from(pair)

  def fromPrismF[F[_]: MonoidK, S, T, A, B](
      prism: Optic[S, T, F[A], F[B], Either]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Either[prism.X, Unit]
      def to(s: S): (X, F[A]) =
        prism.to(s) match
          case Right(fa)   => (hitUnit, fa)
          case l @ Left(_) => (l.widenRight[Unit], MonoidK[F].empty[A])
      def from(pair: (X, F[B])): T =
        pair match
          case (l @ Left(_), _) => prism.from(l.widenRight[F[B]])
          case (Right(_), fb)   => prism.from(Right(fb))

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
      def to(fa: F[A]): (Unit, F.Representation => A) = ((), F.index(fa))
      def from(pair: (Unit, F.Representation => A)): F[A] = F.tabulate(pair._2)

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
      def to(fa: F[A]): (Unit, F.Representation => A) = ((), F.index(fa))
      def from(pair: (Unit, F.Representation => A)): F[A] = F.tabulate(pair._2)

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
      def to(t: T): (Unit, Int => A) =
        val read: Int => A = (i: Int) => t.productElement(i).asInstanceOf[A]
        ((), read)
      def from(pair: (Unit, Int => A)): T =
        val k = pair._2
        val arr = new Array[Object](size)
        @tailrec def loop(i: Int): Unit =
          if i < size then
            arr(i) = k(i).asInstanceOf[Object]
            loop(i + 1)
        loop(0)
        Tuple.fromArray(arr).asInstanceOf[T]

  // ------------------------------------------------------------------
  // Iso → Function1-shaped MultiFocus bridge — absorbs v1 `forgetful2grate`.
  // ------------------------------------------------------------------

  /** Iso ↪ MultiFocus[Function1[X0, *]]. Iso's forward `to: S => A` is broadcast to the constant
    * rebuild `_ => a`; the reverse reads the rebuild at any X0 (null sentinel — lead dropped per
    * fixedtraversal-fold-spike Q1).
    */
  given forgetful2multifocusFunction1[X0]: Composer[Direct, MultiFocus[Function1[X0, *]]] with

    def to[S, T, A, B](
        o: Optic[S, T, A, B, Direct]
    ): Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] =
      new Optic[S, T, A, B, MultiFocus[Function1[X0, *]]]:
        type X = Unit
        def to(s: S): (Unit, X0 => A) =
          val a = o.to(s).value
          ((), (_: X0) => a)
        def from(pair: (Unit, X0 => B)): T =
          o.from(Direct(pair._2(null.asInstanceOf[X0])))

  def fromOptionalF[F[_]: MonoidK, S, T, A, B](
      opt: Optic[S, T, F[A], F[B], Affine]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Affine[opt.X, Unit]
      def to(s: S): (X, F[A]) =
        opt.to(s) match
          case m: Affine.Miss[opt.X, F[A]] @unchecked =>
            (m.widenB[Unit], MonoidK[F].empty[A])
          case h: Affine.Hit[opt.X, F[A]] @unchecked =>
            (new Affine.Hit[opt.X, Unit](h.snd, ()), h.b)
      def from(pair: (X, F[B])): T =
        pair match
          case (m: Affine.Miss[opt.X, Unit] @unchecked, _) =>
            opt.from(m.widenB[F[B]])
          case (h: Affine.Hit[opt.X, Unit] @unchecked, fb) =>
            opt.from(new Affine.Hit[opt.X, F[B]](h.snd, fb))

/** API façade under the carrier's public name. The instances live in [[MultiFocusK]] (the opaque
  * anchor's companion, where implicit scope finds them); this re-export keeps `MultiFocus.apply` /
  * `MultiFocus.fromLensF` call-shapes and legacy `import data.MultiFocus.given` working.
  */
object MultiFocus:
  export MultiFocusK.{given, *}

  transparent inline def apply[F[_], X, A](x: X, fa: F[A]): MultiFocusK[F, X, A] = (x, fa)
