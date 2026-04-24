package eo
package data

import optics.Optic

import cats.{Alternative, Applicative, Foldable, Monoid, MonoidK, Traverse}

/** Pair carrier for the algebraic-lens family — `AlgLens[F][X, A] = (X, F[A])`. The `X` holds
  * structural leftover (the same role it plays for `Tuple2` / `Affine`); the `F[A]` holds
  * classifier candidates. Together they let a `Lens` / `Prism` / `Optional` survive the round-trip
  * that a phantom-`X` carrier like [[Forget]] cannot — the `from: (X, F[B]) => T` side still sees
  * the outer's leftover.
  *
  * When the `X` is genuinely unused (e.g. a [[Forget]] injection), set `type X = Unit` and the pair
  * degenerates to a thin wrapper around `F[A]`.
  *
  * @tparam F
  *   classifier shape — constraint requirements depend on the operation:
  *   - `.modify` / `.replace` on any AlgLens-carrier optic needs `Functor[F]` (supplies
  *     `ForgetfulFunctor[AlgLens[F]]`).
  *   - `.foldMap` needs `Foldable[F]`.
  *   - `.modifyA` / `.all` needs `Traverse[F]`.
  *   - Same-carrier `.andThen` (`assocAlgMonad`) needs `Applicative[F] + Traverse[F] + MonoidK[F] +
  *     AlgLensFromList[F]`.
  *   - Constructing a bridge via `fromPrismF` / `fromOptionalF` needs `MonoidK[F]` only;
  *     `fromLensF` has no `F` constraint at construction.
  */
type AlgLens[F[_]] = [X, A] =>> (X, F[A])

/** Fast-path capability trait for AlgLens-carrier optics whose classifier is always an F-singleton
  * (`F.pure(a)`). [[AlgLens.assocAlgMonad]] pattern-matches on this and bypasses both the `F.pure`
  * wrap on push and the `reduceLeftToOption(fb).get` unwrap on pull: `singletonTo` returns the raw
  * `(X0, A)` pair and `singletonFrom` takes the raw `(X0, B)` pair, directly backed by the
  * underlying carrier (typically a Lens's `to` / `from`).
  *
  * One current user: [[AlgLens.tuple2alg]] (Lens → AlgLens). The Prism bridge `either2alg` does NOT
  * qualify — its miss branch emits `F.empty` (cardinality 0), breaking the "always ≥ 1" invariant.
  * The F[A]-focus factories (`fromLensF`, `fromPrismF`, `fromOptionalF`) also don't qualify; their
  * classifier cardinality is whatever the outer optic's focus carries.
  *
  * @tparam X0
  *   the underlying optic's structural leftover — coincides with the carrying Optic's `type X` at
  *   mix-in time. Kept as a trait parameter (not a `type` member) so the `assocAlgMonad` match
  *   refinement aligns with `Xi`.
  */
private[eo] trait AlgLensSingleton[S, T, A, B, X0]:
  def singletonTo(s: S): (X0, A)
  def singletonFrom(x: X0, b: B): T

/** Build an `F[A]` from a `List[A]`, in O(n) for the shipped carriers. `cats.MonoidK`'s `combineK`
  * has inconsistent asymptotics across F — concatenative on `List` and `cats.data.Chain`,
  * copy-on-concat on `Vector` (O(n) per step → O(n²) total), and left-biased on `Option` (silently
  * drops size > 1). This trait bypasses `combineK` with a per-F specialisation, so
  * [[AlgLens.assocAlgMonad]]'s push/pull paths stay O(n) across every qualifying `F` and surface
  * explicit errors where cardinality would otherwise collapse.
  *
  * Required alongside `Applicative[F] + Traverse[F] + MonoidK[F]` by `assocAlgMonad`. Instances for
  * `List`, `Option`, `Vector`, `cats.data.Chain` live in the companion; users extending `AlgLens`
  * to a custom `F` supply their own instance.
  */
private[eo] trait AlgLensFromList[F[_]]:
  /** Build `F[A]` from a `List[A]`, preserving order and cardinality. Throw `IllegalStateException`
    * if `F` cannot represent the supplied list's cardinality (Option with size > 1 is the only such
    * case across shipped instances).
    */
  def fromList[A](xs: List[A]): F[A]

  /** Build `F[A]` from a slice of an `Array[AnyRef]`. Defaults to `fromList` after materialising
    * the slice as a `List[A]`; instances may override with a direct construction when that's
    * cheaper (e.g. `Vector.tabulate`, `Chain.fromSeq` on `ArraySeq.unsafeWrapArray`).
    */
  def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): F[A] =
    fromList(List.tabulate(size)(i => arr(from + i).asInstanceOf[A]))

private[eo] object AlgLensFromList:

  given forList: AlgLensFromList[List] with
    def fromList[A](xs: List[A]): List[A] = xs

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): List[A] =
      List.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forOption: AlgLensFromList[Option] with

    def fromList[A](xs: List[A]): Option[A] = xs match
      case Nil      => None
      case h :: Nil => Some(h)
      case _        =>
        throw new IllegalStateException(
          s"AlgLensFromList[Option]: cannot represent a list of ${xs.size} elements; " +
            "Option's classifier cardinality is 0 or 1 by construction. An AlgLens[Option] " +
            "composition that tried to hand a multi-element chunk to this instance indicates " +
            "a push-side invariant violation — a `to` produced F[A] with > 1 element for " +
            "Option, which Option cannot hold."
        )

  given forVector: AlgLensFromList[Vector] with
    def fromList[A](xs: List[A]): Vector[A] = xs.toVector

    override def fromArraySlice[A](arr: Array[AnyRef], from: Int, size: Int): Vector[A] =
      Vector.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forChain: AlgLensFromList[cats.data.Chain] with
    def fromList[A](xs: List[A]): cats.data.Chain[A] = cats.data.Chain.fromSeq(xs)

/** Typeclass instances and cross-carrier composers for [[AlgLens]]. Ships `ForgetfulFunctor` (any
  * `Functor[F]`), `ForgetfulFold` (any `Foldable[F]`), `ForgetfulTraverse[_, Applicative]` (any
  * `Traverse[F]`), the same-carrier `AssociativeFunctor` (`assocAlgMonad`, requires
  * `Monad + Traverse + MonoidK + AlgLensFromList` on F), and the inbound Composers from
  * `Tuple2 / Either / Forget / Forgetful`. No outbound Composers — `AlgLens[F]` is a composition
  * sink by design.
  */
object AlgLens:

  /** `ForgetfulFunctor[AlgLens[F]]` — `.modify` / `.replace` on the focus by mapping inside the
    * `F`-wrapped candidates and leaving `X` untouched.
    *
    * @group Instances
    */
  given algFunctor[F[_]: cats.Functor]: ForgetfulFunctor[AlgLens[F]] with

    def map[X, A, B](xa: (X, F[A]), f: A => B): (X, F[B]) =
      (xa._1, cats.Functor[F].map(xa._2)(f))

  /** `ForgetfulFold[AlgLens[F]]` — folds across the classifier candidates with `Foldable[F]`. The
    * structural leftover is ignored (a fold walks candidates, not context) — same convention as
    * `ForgetfulFold[Tuple2]` / `ForgetfulFold[Affine]` where the complement slot doesn't contribute
    * to the fold.
    *
    * @group Instances
    */
  given algFold[F[_]: Foldable]: ForgetfulFold[AlgLens[F]] with

    def foldMap[X, A, M: Monoid]: (A => M) => ((X, F[A])) => M =
      f => xa => Foldable[F].foldMap(xa._2)(f)

  /** `ForgetfulTraverse[AlgLens[F], Applicative]` — effectful traverse across the classifier
    * candidates, preserving the structural leftover on the outside of the `G`-applicative.
    *
    * @group Instances
    */
  given algTraverse[F[_]: Traverse]: ForgetfulTraverse[AlgLens[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative]: ((X, F[A])) => (A => G[B]) => G[(X, F[B])] =
      xa => f => Applicative[G].map(Traverse[F].traverse(xa._2)(f))(fb => (xa._1, fb))

  /** Algebraic-lens composition of two `AlgLens[F]`-carrier optics.
    *
    * `Z = (Xo, F[(Xi, Int)])` — the outer's leftover is a single value; each inner call records its
    * own leftover alongside the cardinality of its classifier output. Storing per-candidate
    * cardinalities on push lets the pull side route each slice of `F[D]` to the inner call that
    * produced it — no matter whether the inner was a singleton (Lens-morphed) classifier, a
    * multi-element (fromLensF-style) classifier, or a mix of cardinalities per outer candidate.
    *
    * Semantics (cardinality-preserving chunking):
    *   - push: one `Traverse.mapAccumulate` pass runs `inner.to` per outer element, accumulates a
    *     per-xi `Int` count into the `fxiSize` annotation, and builds the flat `F[C]` in a single
    *     sweep — no intermediate `F[(Xi, F[C])]` stage.
    *   - pull: another `mapAccumulate` threads the flattened `F[D]` (materialised once as an
    *     `Array[AnyRef]`) across `F[(Xi, Int)]`; each `(xi, size)` consumes `size` elements via an
    *     index cursor and hands the chunk (rebuilt via [[arraySliceToF]] for the general path, or
    *     `Applicative.pure` for the singleton path) to `inner.from`.
    *
    * Fast-path: if the inner side implements [[AlgLensSingleton]] the algorithm degenerates to a
    * chunk-free equivalent — each outer candidate contributes exactly one focus to `fd`, and each
    * `inner.from` gets `Applicative.pure(d)` directly. This is how the `tuple2alg` / `either2alg`
    * bridges avoid paying the full chunking cost when the inner is a singleton classifier (the
    * common case for `Lens → AlgLens → Lens`).
    *
    * A size mismatch on pull (cardinality skew between outer's `fa` and the flattened `fc`) cannot
    * arise from the standard `.modify` / `.andThen` surface because `fd = fc.map(f)` preserves
    * length and the per-xi `Int` sizes sum to `|fd|` by construction of push.
    *
    * Constraints: `Applicative[F]` (`pure` + `map` for push), `Traverse[F]` (stateful traverse for
    * pull — also carries the `Foldable[F]` the body's fold / `toList` / `size` calls need),
    * `MonoidK[F]` (`empty`), and [[AlgLensFromList]] (per-F O(n) rebuild, replacing the generic
    * `combineK` prepend loop which was O(n²) for `Vector` and silently lossy for `Option`). `List`,
    * `Option`, `Vector`, `cats.data.Chain` all ship `AlgLensFromList` instances; `NonEmptyList`
    * does not (no `MonoidK.empty`).
    *
    * Performance: ~1.5-2.6× slower than `PowerSeries`'s specialised traversal carrier on the
    * traversal-over-list-field shape (see `docs/research/2026-04-22-alglens-vs-powerseries.md`).
    * AlgLens earns its keep on non- uniform classifier cardinality and non-traversable-container
    * `F` shapes, not on vanilla list traversal.
    *
    * @group Instances
    */
  given assocAlgMonad[F[_]: Applicative: Traverse: MonoidK: AlgLensFromList, Xo, Xi]
      : AssociativeFunctor[AlgLens[F], Xo, Xi] with
    type Z = (Xo, F[(Xi, Int)])

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, AlgLens[F]] { type X = Xo },
        inner: Optic[A, B, C, D, AlgLens[F]] { type X = Xi },
    ): ((Xo, F[(Xi, Int)]), F[C]) =
      val Tr = Traverse[F]
      val FL = summon[AlgLensFromList[F]]
      val (xo, fa) = outer.to(s)
      inner match
        case is: AlgLensSingleton[A, B, C, D, Xi] @unchecked =>
          // Inner-singleton fast path: `is.singletonTo(a)` returns `(Xi, C)` directly without
          // going through the generic Optic carrier — no `F.pure(c)` allocation on push, no
          // `reduceLeftToOption` unwrap on pull. `inner.to(a)` would have produced
          // `(xi, F.pure(c))`; `is.singletonTo(a)` produces `(xi, c)`.
          //
          // One `mapAccumulate` pass walks `fa`, accumulates the `c`s and `(xi, 1)` pairs
          // into reverse-order Lists. `AlgLensFromList[F].fromList(list.reverse)` rebuilds
          // each in forward order; per-F instances guarantee O(n).
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
          // General path: run `inner.to` once per outer element in a single
          // `Traverse.mapAccumulate` pass. Each step's inner `foldLeft` over the per-element
          // `fc` simultaneously appends to the running `List[C]` and counts cardinality —
          // one traversal fuses the copy and the size measurement, avoiding a second
          // `Foldable.size` walk over each `fc`.
          val (flatList, fxiSize) =
            Tr.mapAccumulate(List.empty[C], fa) { (acc, a) =>
              val (xi, fc) = inner.to(a)
              val (acc2, count) = Tr.foldLeft(fc, (acc, 0)) {
                case ((l, n), c) =>
                  (c :: l, n + 1)
              }
              (acc2, (xi, count))
            }
          // `flatList` is in reverse visiting order (built cons-style via `::`).
          // `AlgLensFromList[F].fromList(.reverse)` rebuilds in forward order — O(n) per
          // instance. Previously this built an intermediate `Array[AnyRef]` and looped via
          // `combineK(pure, acc)` which was O(n²) for `Vector` and silent-truncating for
          // `Option`; the typeclass dispatch sidesteps both.
          val fc: F[C] = FL.fromList(flatList.reverse)
          ((xo, fxiSize), fc)

    def composeFrom[S, T, A, B, C, D](
        xd: ((Xo, F[(Xi, Int)]), F[D]),
        inner: Optic[A, B, C, D, AlgLens[F]] { type X = Xi },
        outer: Optic[S, T, A, B, AlgLens[F]] { type X = Xo },
    ): T =
      val Tr = Traverse[F]
      val ((xo, fxiSize), fd) = xd
      // Materialise `fd` once to a contiguous `Array[AnyRef]` for O(1) cursor advance on pull.
      // Same array backs both fast-path and general-path cursors — consistent shape lets the
      // two branches share the defensive structure against cardinality skew.
      val dArr: Array[AnyRef] = foldableToArray[F, D](fd)
      inner match
        case is: AlgLensSingleton[A, B, C, D, Xi] @unchecked =>
          // Inner-singleton fast path: every chunk is size 1. `is.singletonFrom(xi, d)`
          // returns the underlying carrier's `B` directly — no `F.pure(d)` wrap, no
          // `pickSingletonOrThrow` inside the bridged optic's `from`.
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, _)) =>
              val d = dArr(cursor).asInstanceOf[D]
              (cursor + 1, is.singletonFrom(xi, d))
          }
          outer.from((xo, fb))
        case _ =>
          // General path: per xi, pull `size` elements from the cursor and rebuild the F[D]
          // chunk via the `AlgLensFromList[F]` typeclass — O(n) per instance, no
          // `combineK` round-trip.
          val FL = summon[AlgLensFromList[F]]
          val (_, fb) = Tr.mapAccumulate(0, fxiSize) {
            case (cursor, (xi, size)) =>
              (cursor + size, inner.from((xi, FL.fromArraySlice[D](dArr, cursor, size))))
          }
          outer.from((xo, fb))

  /** Materialise a `Foldable[F]` as a fresh `Array[AnyRef]` without an intermediate `List`. One
    * `Foldable.size` call to size the array, then a single `foldLeft` fills the slots. Used by
    * [[assocAlgMonad.composeFrom]] so the pull side can cursor-walk by index.
    */
  private def foldableToArray[F[_]: Foldable, D](fd: F[D]): Array[AnyRef] =
    val n = Foldable[F].size(fd).toInt
    val arr = new Array[AnyRef](n)
    var i = 0
    Foldable[F].foldLeft(fd, ()) { (_, d) =>
      arr(i) = d.asInstanceOf[AnyRef]
      i += 1
    }
    arr

  /** Pull the single `B` out of an `F[B]` that must have cardinality exactly 1 by construction.
    * Used by the classical bridges (`tuple2alg.from`, `either2alg.from` on hit) whose own `to`
    * guarantees this invariant. Strict: raises `IllegalStateException` on both empty and
    * multi-element `F[B]` — either would indicate a cardinality skew the bridge's contract forbids.
    * Structurally unreachable through `.modify` / `.andThen`; a throw here is a hand-construction
    * diagnostic.
    */
  private def pickSingletonOrThrow[F[_]: Foldable, B](
      fb: F[B],
      carrier: String,
  ): B =
    val sz = Foldable[F].size(fb)
    if sz == 1 then Foldable[F].reduceLeftToOption(fb)(identity[B])((_, b) => b).get
    else
      throw new IllegalStateException(
        s"Composer[$carrier, AlgLens[F]]: expected F[B] of cardinality 1, got $sz. " +
          "This is structurally unreachable through .modify / .andThen; indicates a " +
          "hand-constructed call that violates the bridge's singleton contract."
      )

  /** Trivial injection `Forget[F] ↪ AlgLens[F]` — `X` is genuinely phantom in `Forget[F]`, so the
    * adapter sets `type X = Unit` and wraps the `F[A]` in a pair. **The bridge itself has no `F`
    * constraint**, but downstream operations on the resulting optic (`.modify`, `.foldMap`,
    * `.andThen`) do — see the [[AlgLens]] `@tparam F` note for the per-operation requirements.
    *
    * Does **not** mix in [[AlgLensSingleton]] — `Forget[F]`'s classifier preserves whatever
    * cardinality `F` carries (e.g. `Forget[List]` has list-sized classifiers), which is generally
    * not singleton. Composition with this adapter on the inner side goes through
    * [[assocAlgMonad]]'s general path.
    *
    * This is what makes existing `Forget[F]`-carrier optics (Traversals, Folds, and any hand-rolled
    * classifier) compose with Lens/Prism/Optional-adapted algebraic lenses — both sides morph up
    * into `AlgLens[F]` and meet under [[assocAlgMonad]].
    *
    * @group Instances
    */
  given forget2alg[F[_]]: Composer[Forget[F], AlgLens[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, AlgLens[F]] =
      new Optic[S, T, A, B, AlgLens[F]]:
        type X = Unit
        val to: S => (Unit, F[A]) = s => ((), o.to(s))
        val from: ((Unit, F[B])) => T = { case (_, fb) => o.from(fb) }

  /** Bridge `Tuple2 ↪ AlgLens[F]` — a Lens becomes an algebraic lens whose classifier is a pure
    * singleton `F.pure(a)` at read time. The structural leftover `X` survives unchanged.
    *
    * Mixes in [[AlgLensSingleton]] so [[assocAlgMonad]] picks the singleton fast-path when this
    * adapter is used as the inner side of a composition (the common case in a `Lens → AlgLens →
    * Lens` chain).
    *
    * Unreachability of the empty-fb case: `.to` produces `F.pure(a)` (cardinality 1). Under the
    * normal optic surface (`.modify` / `.andThen`), the composed `fb` at this adapter's `.from` is
    * either that same `F.pure(a)` mapped by the user's `A => B` (still cardinality 1) or — via
    * [[assocAlgMonad]]'s chunking — a freshly-wrapped `F.pure(d)` produced by [[arraySliceToF]] on
    * a single-element chunk. Empty `fb` is therefore structurally impossible;
    * [[pickSingletonOrThrow]] picks the only element, and the throw branch exists only to guard
    * against hand-constructed callers that bypass the composition machinery.
    *
    * @group Instances
    */
  // Iso → AlgLens[F] composition is handled by the low-priority `Morph.viaTuple2` fallback
  // (see `eo.LowPriorityMorphInstances`). No direct `Composer[Forgetful, AlgLens[F]]` is
  // shipped here — the fallback fires cleanly once `Composer.chain` is at low priority.

  given tuple2alg[F[_]: Applicative: Foldable]: Composer[Tuple2, AlgLens[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, AlgLens[F]] =
      new Optic[S, T, A, B, AlgLens[F]] with AlgLensSingleton[S, T, A, B, o.X]:
        type X = o.X
        // AlgLensSingleton's raw-access methods — directly delegate to the underlying Lens's
        // `to` / `from` so the assoc fast path can skip the `F.pure` wrap + fold unwrap
        // altogether.
        def singletonTo(s: S): (o.X, A) = o.to(s)
        def singletonFrom(x: o.X, b: B): T = o.from((x, b))
        val to: S => (X, F[A]) = s =>
          val (x, a) = o.to(s)
          (x, Applicative[F].pure(a))
        val from: ((X, F[B])) => T = {
          case (x, fb) =>
            o.from((x, pickSingletonOrThrow(fb, "Tuple2")))
        }

  /** Bridge `Either ↪ AlgLens[F]` — a Prism becomes an algebraic lens. Requires `Alternative[F]`
    * (for the hit branch's `pure`; the miss branch's `empty` comes from the `MonoidK` superclass of
    * `Alternative`); `Foldable[F]` collapses the pull side on hit.
    *
    * Encoding: the adapter's `X` widens the Prism's `X` with a hit/miss tag (`Either[PrismX,
    * Unit]`). On miss the outer's `X` is preserved so `from` can call `prism.from(Left(x))`
    * identity-style; on hit the tag is `Right(())` and `from` picks the (unique-by-construction)
    * `B` from the classifier and calls `prism.from(Right(b))`. Same empty-fb unreachability note as
    * [[tuple2alg]] applies to the hit branch.
    *
    * Does NOT mix in [[AlgLensSingleton]]: the miss branch emits `F.empty[A]` (cardinality 0),
    * which would violate the singleton fast path's "classifier always has ≥ 1 element" invariant.
    * The general-path chunking in [[assocAlgMonad]] tolerates size-0 per-xi fine — it just records
    * `(xi, 0)` in `fxiSize` and contributes nothing to `fc`.
    *
    * @group Instances
    */
  given either2alg[F[_]: Alternative: Foldable]: Composer[Either, AlgLens[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, AlgLens[F]] =
      new Optic[S, T, A, B, AlgLens[F]]:
        type X = Either[o.X, Unit]
        val to: S => (X, F[A]) = s =>
          o.to(s) match
            case Right(a) => (Right(()), Applicative[F].pure(a))
            case Left(x)  => (Left(x), Alternative[F].empty[A])
        val from: ((X, F[B])) => T = {
          case (tag, fb) =>
            tag match
              case Left(xMiss) =>
                o.from(Left(xMiss))
              case Right(_) =>
                o.from(Right(pickSingletonOrThrow(fb, "Either")))
        }

  // -------------------------------------------------------------------
  // F[A]-focus bridges — factory methods for the case where the outer
  // optic's focus is already `F[A]` (e.g. a `Lens[Row, List[Int]]` viewed
  // as an `AlgLens[List][Row, Int]`). These relax the `Foldable` constraint
  // across the board — and the `Applicative` constraint for the Tuple2
  // case — because the `F` structure is already exposed by the outer
  // optic rather than being synthesised via `pure` / collapsed via fold.
  // None of these mix in `AlgLensSingleton` — the classifier's cardinality
  // is whatever the outer optic's `F[A]` focus happens to carry.
  // -------------------------------------------------------------------

  /** Lift a `Tuple2`-carrier optic whose focus is `F[A]` — i.e. a lens that already "points at" the
    * classifier — into `AlgLens[F]` with focus `A`. Structurally the pair `(X, F[A])` on the Lens
    * side and the `(X, F[A])` on the AlgLens side are identical, so the adapter is a pure rewrap
    * with no constraints on `F`.
    *
    * Use this when the outer Lens already has `F[A]` as its observable focus (collections, option
    * fields, classifier outputs) and you want downstream AlgLens operations to see the inner `A`
    * directly.
    *
    * @group Factories
    */
  def fromLensF[F[_], S, T, A, B](
      lens: Optic[S, T, F[A], F[B], Tuple2]
  ): Optic[S, T, A, B, AlgLens[F]] =
    new Optic[S, T, A, B, AlgLens[F]]:
      type X = lens.X
      val to: S => (X, F[A]) = lens.to
      val from: ((X, F[B])) => T = lens.from

  /** Lift an `Either`-carrier optic (Prism) whose focus is `F[A]` into `AlgLens[F]` with focus `A`.
    * Requires `MonoidK[F]` only (tighter than `Alternative` — we never need `pure`, just `empty`,
    * and we don't fold on the pull side either).
    *
    * Encoding: `X = Either[PrismX, Unit]` tags the variant. Miss carries the original `PrismX` and
    * pairs it with `F.empty`; Hit pairs `Right(())` with the original `F[A]`.
    *
    * @group Factories
    */
  def fromPrismF[F[_]: MonoidK, S, T, A, B](
      prism: Optic[S, T, F[A], F[B], Either]
  ): Optic[S, T, A, B, AlgLens[F]] =
    new Optic[S, T, A, B, AlgLens[F]]:
      type X = Either[prism.X, Unit]
      val to: S => (X, F[A]) = s =>
        prism.to(s) match
          case Right(fa) => (Right(()), fa)
          case Left(x)   => (Left(x), MonoidK[F].empty[A])
      val from: ((X, F[B])) => T = {
        case (tag, fb) =>
          tag match
            case Left(xMiss) => prism.from(Left(xMiss))
            case Right(_)    => prism.from(Right(fb))
      }

  /** Lift an `Affine`-carrier optic (Optional) whose focus is `F[A]` into `AlgLens[F]` with focus
    * `A`. Requires `MonoidK[F]` only (we need `empty` for the miss branch; `pure` is never
    * synthesised because the hit branch already carries the classifier `F[A]`).
    *
    * Encoding: the variant (`Miss` vs `Hit`) is encoded into `X` by reusing `Affine[AffineX, Unit]`
    * — Miss carries `Fst[AffineX]`, Hit carries `Snd[AffineX]` with a `Unit` focus placeholder. On
    * pull, the `F[B]` flows through the Hit variant's focus slot, preserving F-structure.
    *
    * @group Factories
    */
  def fromOptionalF[F[_]: MonoidK, S, T, A, B](
      opt: Optic[S, T, F[A], F[B], Affine]
  ): Optic[S, T, A, B, AlgLens[F]] =
    new Optic[S, T, A, B, AlgLens[F]]:
      type X = Affine[opt.X, Unit]

      // Miss stores only `fst`; `B` is phantom, so `Miss#widenB` re-types in place without
      // allocating. Hit stores `b: B` for real, so Hit branches reallocate to change the focus.
      val to: S => (X, F[A]) = s =>
        opt.to(s) match
          case m: Affine.Miss[opt.X, F[A]] @unchecked =>
            (m.widenB[Unit], MonoidK[F].empty[A])
          case h: Affine.Hit[opt.X, F[A]] @unchecked =>
            (new Affine.Hit[opt.X, Unit](h.snd, ()), h.b)
      val from: ((X, F[B])) => T = {
        case (tag, fb) =>
          tag match
            case m: Affine.Miss[opt.X, Unit] @unchecked =>
              opt.from(m.widenB[F[B]])
            case h: Affine.Hit[opt.X, Unit] @unchecked =>
              opt.from(new Affine.Hit[opt.X, F[B]](h.snd, fb))
      }
