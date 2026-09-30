package dev.constructive.eo
package data

import scala.annotation.tailrec

import cats.data.Chain
import cats.{Alternative, Applicative, Foldable, Functor, Monoid, MonoidK, Representable, Traverse}

import forgetful.*
import compose.*
import optics.Optic

/** Unified pair carrier for the algebraic-lens, kaleidoscope, and grate optic families —
  * `MultiFocus[F][X, A] = (X, Focus[F, A])`: a structural leftover `X` paired with a focus half.
  * One carrier serves all three families; only the choice of `F` differs (a container for
  * Traversal, `Function1`-shaped for the grate encoding, …).
  *
  * The focus half is a *sum*, because two shapes of optic share this carrier and the difference is
  * not derivable from `F`:
  *
  *   - [[MultiFocusK.Broadcast]] — an index-independent focus: one value `a`, known without
  *     consulting an index (`X0`-shaped carriers), plus the same value presented as the carrier's
  *     own `F[A]`. Written by `forgetful2multifocusFunction1` (the Iso shim), by
  *     `MultiFocus.broadcast`, and by every composition whose write collapsed to a single value.
  *   - `F[A]` itself — a genuine tabulation / focus vector: reading a position means reading this.
  *     Written by every other factory (`representable`, `tuple`, `apply`, …).
  *
  * Keeping the sum *inside* the carrier (rather than in an optic-level class) makes constancy a
  * property of the *data*, so it survives `map` / `collectWith` / `andThen` — the kernels read it
  * instead of inventing an index when a value is needed "at whatever position".
  * [[MultiFocusK.foci]] stays total by carrying the lifted `F[A]` in the broadcast case, so every
  * consumer that only wants "the focus vector" is unchanged.
  *
  * The kaleidoscope aggregation ("summarise the foci, write the summary back") has two natural
  * derivations, and the choice is structural — not derivable from a single typeclass:
  *
  *   - Functor-broadcast: `fa.map(_ => f(fa))`. Length-preserving; default. Needs `Functor[F]`.
  *   - Applicative-broadcast: `F.pure(f(fa))`. Singleton/cartesian. Needs `Applicative[F]`.
  *
  * The default is the first; List users wanting the singleton collapse can compose `_.headOption`
  * downstream or call `collectList` explicitly. See
  * `docs/research/2026-04-29-fixedtraversal-fold-spike.md` for the grate-absorption justification.
  *
  * @tparam F
  *   classifier shape — operation requirements:
  *   - `.modify` / `.replace` need `Functor[F]`.
  *   - `.foldMap` needs `Foldable[F]`.
  *   - `.modifyA` / `.all` need `Traverse[F]`.
  *   - `.collectMap` / `.collectWith` need `Functor[F]`; `collectList` is List-specific.
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
// the opaque is transparent; outside, `MultiFocus.apply` / [[MultiFocusK.context]] /
// [[MultiFocusK.foci]] are the (runtime-identity) boundary.
opaque type MultiFocusK[F[_], X, A] = (X, MultiFocusK.Focus[F, A])

/** Curried carrier view of [[MultiFocusK]] — the `F[_, _]` shape `Optic` expects. */
type MultiFocus[F[_]] = [X, A] =>> MultiFocusK[F, X, A]

/** Singleton-classifier fast-path. Lets `mfAssoc` skip the `F.pure` wrap and the
  * `pickSingletonOrThrow` pull when the inner is known to produce singletons (sole shipped user:
  * the `tuple2multifocus` Lens → MultiFocus bridge).
  */
private[eo] trait MultiFocusSingleton[S, T, A, B, X0]:
  def singletonTo(s: S): (X0, A)
  def singletonFrom(x: X0, b: B): T

  /** Tuple-free [[singletonTo]] for the PSVec fast path — appends the leftover to `ysBuf` and the
    * focus to `flatBuf` directly, exactly one element each; the caller pre-sizes both builders
    * (`unsafeAppend` contract). Mirrors [[MultiFocusPSMaybeHit.collectTo]]. The default
    * destructures [[singletonTo]]; bridges whose `singletonTo` would build the pair only for it to
    * be torn straight back down (the `GetReplaceLens` bridge) override with a pair-free body.
    */
  def collectSingletonTo(s: S, ysBuf: ObjArrBuilder, flatBuf: ObjArrBuilder): Unit =
    val (x, a) = singletonTo(s)
    ysBuf.unsafeAppend(x)
    flatBuf.unsafeAppend(a)

/** Per-F O(n) builder. Carried as a typeclass because `MonoidK[F].combineK` has inconsistent
  * asymptotics across F (O(n²) on Vector, lossy on Option), so deriving `fromList` from
  * `Traverse[F] + MonoidK[F]` is asymptotically wrong on the carriers we care about.
  */
private[eo] trait MultiFocusFromList[F[_]]:
  def fromList[A](xs: List[A]): F[A]

  def fromArraySlice[A](arr: Array[Any], from: Int, size: Int): F[A] =
    fromList(List.tabulate(size)(i => arr(from + i).asInstanceOf[A]))

private[eo] object MultiFocusFromList:

  given forList: MultiFocusFromList[List] with
    def fromList[A](xs: List[A]): List[A] = xs

    override def fromArraySlice[A](arr: Array[Any], from: Int, size: Int): List[A] =
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

    override def fromArraySlice[A](arr: Array[Any], from: Int, size: Int): Vector[A] =
      Vector.tabulate(size)(i => arr(from + i).asInstanceOf[A])

  given forChain: MultiFocusFromList[Chain] with
    def fromList[A](xs: List[A]): Chain[A] = Chain.fromSeq(xs)

  /** PSVec builder — `fromArraySlice` is zero-copy (returns a `PSVec.Slice` view over the source
    * array). The crucial perf hook that lets `mfAssocPSVec.composeFrom` hand each inner reassembly
    * an O(1) slice of the shared flat focus vector.
    */
  given forPSVec: MultiFocusFromList[PSVec] with

    def fromList[A](xs: List[A]): PSVec[A] = xs match
      case Nil      => PSVec.empty[A]
      case h :: Nil => PSVec.singleton[A](h)
      case _        =>
        val arr = new Array[Any](xs.size)
        @tailrec def loop(i: Int, cur: List[A]): Unit =
          if cur.nonEmpty then
            arr(i) = cur.head
            loop(i + 1, cur.tail)
        loop(0, xs)
        PSVec.unsafeWrap[A](arr)

    override def fromArraySlice[A](arr: Array[Any], from: Int, size: Int): PSVec[A] =
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

  def reconstructSingleton(y: Any, vys: PSVec[B], pos: Int, len: Int): T

/** Instance home for [[MultiFocusK]] — the capability instances (`mfFunctor` / `mfFold` /
  * `mfTraverse`), the same-carrier composition kernels (`mfAssoc` and its Function1 / PSVec
  * specialisations), the `Composer` bridges from every other carrier, the factories, and the
  * `.collect*` / `.at` aggregation surface. Being the opaque anchor's companion, everything here is
  * in implicit scope with no import; the public call-shape is the [[MultiFocus]] façade.
  */
/** The `Direct → MultiFocus[Function1[X0, *]]` bridge's product — a constant-broadcast optic whose
  * bundle is one focus, broadcast to every index ([[MultiFocusK.forgetful2multifocusFunction1]]).
  *
  * The type is itself a witness [[MultiFocusK.mfAssocFunction1]] matches on: a broadcast optic
  * builds exactly ONE value, so the kernel composes its write per index through [[broadcastFrom]] —
  * handing it a per-position bundle through `from` would sample one index and rebuild every
  * position from that single value. That is what makes `grate ∘ iso` rewrite every position. (The
  * carrier's sum says which *bundles* are index-free; this class says which *optics* write one
  * value, and the two agree wherever they overlap.)
  *
  * Its own `from` is the one place the library reads a bundle it did not build: an index-free
  * bundle is read outright ([[MultiFocusK.broadcast]]), and a tabulating one is read at the real
  * index the bridge was handed through [[RepresentativeIndex]] — a defined answer, never a forged
  * sentinel.
  */
final private[eo] class Function1BroadcastOptic[S, T, A, B, X0](
    o: Optic[S, T, A, B, Direct],
    at: X0,
) extends Optic[S, T, A, B, MultiFocus[Function1[X0, *]]]:
  // `Unit`: a broadcast read has no leftover to hand the rebuild — the new focus travels in the
  // bundle (the carrier's second component), and there is no miss to pass through. `Nothing` (the
  // `Getter` / `Review` / `Unfold` choice) is unavailable: `from` must really build a `T`.
  type X = Unit

  /** Build the target from a single written focus — the write half of the broadcast, one value per
    * position. The kernel's per-index write path.
    */
  def broadcastFrom(b: B): T = o.from(Direct(b))

  // The index-free half: one value, known without consulting an index, so the read side of the
  // kernel reuses it verbatim (`inner.to(value)`) and no consumer has to sample anything.
  def to(s: S): MultiFocus[Function1[X0, *]][X, A] =
    MultiFocusK.broadcast[X0, Unit, A]((), o.to(s).value)

  /** '''Constant-bundle contract.''' `at` is correct because every bundle that reaches this method
    * on a shipped path is constant or index-free: `modify` / `replace` / `collect*` map this
    * optic's own broadcast, the kernel's bundle branch hands a `.from` its rebuild as an index-free
    * half, and the kernel writes per index through [[broadcastFrom]] rather than coming through
    * here at all. Given a *varying* bundle by hand, this reads position `at` — a defined answer,
    * not a forged one, and the caller's to choose through [[RepresentativeIndex]].
    */
  def from(fb: MultiFocus[Function1[X0, *]][X, B]): T =
    broadcastFrom(MultiFocusK.readAt(MultiFocusK.chunkOf(fb), at))

object MultiFocusK:

  /** The focus half of a [[MultiFocusK]] bundle — the sum the carrier is built on. Implementation
    * detail: unreachable from user code, so the only way to *observe* an index-free bundle is
    * [[broadcast]] and the only way to build one is [[MultiFocus.broadcast]].
    *
    *   - `F[A]` is a genuine tabulation (or focus vector): reading position `i` means reading it.
    *   - the broadcast half is one value, the focus at *every* index, known without consulting one.
    *
    * Both cases are `F[A]`-shaped through [[foci]], so the sum is invisible to consumers who only
    * want the focus vector; consumers that would otherwise have to invent an index (the
    * `Function1`-shaped composition kernel, the Iso shim's `from`) branch on it instead.
    */
  private[eo] type Focus[F[_], A] = F[A] | Broadcast[F, A]

  /** Index-independent focus half: `value` is the focus at every index, and `lifted` is that same
    * value presented as the carrier's own `F[A]` (for the `Function1`-shaped carrier, the constant
    * function `_ => value`) so [[foci]] stays total with no `Pointed`-shaped constraint.
    *
    * Built by [[MultiFocus.broadcast]], by a broadcast optic's `to`, and by every composition whose
    * write collapsed to a single value. `value` and `lifted` are kept in agreement by construction
    * — every producer builds both from one value — and `map` keeps them together up to the purity
    * of the mapping function (`Functor[F].map` on the lifted form, `f` on the value), which is the
    * same assumption every optic law in this library makes.
    *
    * `private[eo]` on purpose: the carrier classifies a half by testing for this class, so a value
    * of it must never be able to *be* somebody's `F[A]` (a user focus whose type happens to be
    * `Broadcast`, or `Any`, would otherwise be mis-read as a broadcast half).
    */
  final private[eo] class Broadcast[F[_], A](val value: A, val lifted: F[A])

  /** The grate-shaped carrier as a plain type constructor: `MultiFocus[Function1[X0, *]]`. Spelled
    * without the type lambda where a signature has to match an eta-expanded carrier (an override of
    * a member generic in `F`), and handy for writing one's own `Function1`-carried optics.
    */
  type Function1Carrier[X0] = [X, A] =>> MultiFocusK[Function1[X0, *], X, A]

  // Construct via the façade `MultiFocus(x, fa)` (an `apply` on the public object below); these
  // are the unwrap boundary. `context` is identity at runtime; `foci` is identity for a tabulation
  // and one field read for a broadcast.
  extension [F[_], X, A](self: MultiFocusK[F, X, A])
    /** The structural leftover. Identity at runtime. */
    transparent inline def context: X = self._1

    /** The focus vector — the carrier's own `F[A]` view of the focus half. Identity at runtime for
      * a tabulation; for a [[Broadcast]] it returns the index-free value's lifted `F[A]`, so every
      * consumer keeps working without an index.
      */
    transparent inline def foci: F[A] =
      self._2 match
        case b: Broadcast[F, A] @unchecked => b.lifted
        case f: F[A] @unchecked            => f

    /** The index-independent focus value, when this bundle has one: `Some(a)` for a [[Broadcast]]
      * bundle, `None` for a genuine tabulation. The accessor that makes constancy *observable* —
      * callers that need "the focus, whichever index" can ask here instead of inventing one.
      */
    transparent inline def broadcast: Option[A] =
      self._2 match
        case b: Broadcast[F, A] @unchecked => Some(b.value)
        case _                             => None

  /** The raw focus half — the un-lifted sum, for kernels that must branch on the *shape* rather
    * than read the value. In-file only: outside this file the sum is reached through [[foci]] /
    * [[broadcast]].
    */
  private[eo] inline def chunkOf[F[_], X, A](mf: MultiFocusK[F, X, A]): Focus[F, A] = mf._2

  /** Read a `Function1`-shaped bundle at `i` — the one read that never needs an index when the
    * bundle is a [[Broadcast]].
    */
  private[eo] inline def readAt[X0, A](half: Focus[Function1[X0, *], A], i: X0): A = half match
    case b: Broadcast[Function1[X0, *], A] @unchecked => b.value
    case k: (X0 => A) @unchecked                      => k(i)

  /** Wrap an index-free value as a broadcast half for the `Function1[X0, *]`-shaped carrier. */
  private inline def broadcastHalf[X0, A](a: A): Broadcast[Function1[X0, *], A] =
    new Broadcast(a, (_: X0) => a)

  /** Whether a focus half is index-free — the read side of the sum, for kernels that need to branch
    * on the shape without reading the value.
    */
  private inline def isIndexFree[F[_], A](half: Focus[F, A]): Boolean = half match
    case _: Broadcast[F, A] @unchecked => true
    case _                             => false

  // Capability instances — Functor / Foldable / Traverse over the F[A] half.

  // NOTE: member signatures below are spelled `MultiFocus[F][X, A]`, not `(X, F[A])`. The two are
  // equal inside this file (the opaque is transparent here), but a `given … with` instance is
  // typed by its anonymous class, so the *written* member signatures are what inline extensions
  // (`Optic.modify` / `.modifyA` / `.foldMap`, …) splice against at call sites — where only the
  // opaque spelling typechecks.

  /** `ForgetfulFunctor[MultiFocus[F]]` via `Functor[F]` — maps every focus, leftover untouched.
    * Unlocks `.modify` / `.replace` on Traversal-family optics.
    *
    * @group Instances
    */
  given mfFunctor[F[_]: Functor]: ForgetfulFunctor[MultiFocus[F]] with

    def map[X, A, B](xa: MultiFocus[F][X, A], f: A => B): MultiFocus[F][X, B] =
      // Constancy survives mapping: a broadcast half stays a broadcast half (one value, one lifted
      // image), which is what keeps a broadcast outer's write index-free through `.modify` /
      // `.collectWith`.
      chunkOf(xa) match
        case b: Broadcast[F, A] @unchecked =>
          (xa._1, new Broadcast[F, B](f(b.value), Functor[F].map(b.lifted)(f)))
        case _ =>
          val fh: F[A] = xa.foci
          (xa._1, Functor[F].map(fh)(f))

  /** `ForgetfulFold[MultiFocus[F]]` via `Foldable[F]` — folds the focus vector, discarding the
    * leftover. Unlocks `.foldMap` / `.headOption` / `.length` / `.exists`.
    *
    * @group Instances
    */
  given mfFold[F[_]: Foldable]: ForgetfulFold[MultiFocus[F]] with

    def foldMap[X, A, M: Monoid](f: A => M, xa: MultiFocus[F][X, A]): M =
      Foldable[F].foldMap(xa.foci)(f)

  /** `ForgetfulTraverse[MultiFocus[F], Applicative]` via `Traverse[F]` — effectful rewrite of every
    * focus in `F`'s traversal order. Unlocks `.modifyA` / `.all`.
    *
    * @group Instances
    */
  given mfTraverse[F[_]: Traverse]: ForgetfulTraverse[MultiFocus[F], Applicative] with

    def traverse[X, A, B, G[_]: Applicative](
        xa: MultiFocus[F][X, A],
        f: A => G[B],
    ): G[MultiFocus[F][X, B]] =
      Applicative[G].map(Traverse[F].traverse(xa.foci)(f))(fb => (xa._1, fb))

  /** Same-carrier composition for `MultiFocus[F]` — `traversal.andThen(traversal)`, F-parametric.
    * `Z = (Xo, F[(Xi, Int)])`: the outer leftover plus, per outer focus, the inner leftover and its
    * focus count, so `composeFrom` can carve the flat modified vector back into per-element slices
    * (cursor over an array snapshot). Inners that mix in `MultiFocusSingleton` (the Lens bridge)
    * take an always-hit fast path that skips the per-element inner fold. Flat focus order is the
    * outer's traversal order, inner order within each element.
    *
    * @group Instances
    */
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
      val outerBundle = outer.to(s)
      val xo: Xo = outerBundle.context
      val fa: F[A] = outerBundle.foci
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
              val innerBundle = inner.to(a)
              val xi: Xi = innerBundle.context
              val fc: F[C] = innerBundle.foci
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
      val (xo, fxiSize) = xd.context
      val fd: F[D] = xd.foci
      val dArr: Array[Any] = foldableToArray[F, D](fd)
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

  private def foldableToArray[F[_]: Foldable, D](fd: F[D]): Array[Any] =
    val n = Foldable[F].size(fd).toInt
    val arr = new Array[Any](n)
    var i = 0
    Foldable[F].foldLeft(fd, ()) { (_, d) =>
      arr(i) = d
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

  /** `Function1`-shaped same-carrier composition — the grate-absorbed case. The general [[mfAssoc]]
    * requires `Traverse[F]` + `MultiFocusFromList[F]`; `Function1[X0, *]` admits neither, so this
    * instance composes the rebuild closures directly.
    *
    * The two sides of a composed optic need not have the same shape, and the carrier says which is
    * which per bundle (see [[MultiFocusK.Focus]]), so the kernel branches on *data* rather than on
    * the optic's class — which also means a composite (an anonymous `Optic`) keeps reporting its
    * true shape to the next composition up:
    *
    *   - read — an index-free outer (`Broadcast` half) is read ONCE and its value handed to the
    *     inner (`inner.to(a)`), so the composite inherits the inner's shape. A tabulating outer is
    *     read per index: `i => inner.to(readO(i))` read at `i`. Both are exact; neither invents an
    *     index.
    *   - write — the composite's write shape follows the *read* shape, recorded in [[AssocF1Z]]. An
    *     index-free outer collapses the written bundle once (`inner.from(bundle)` — exact whenever
    *     the incoming bundle is itself broadcast, which every shipped `.modify` / `.replace` /
    *     `.collectWith` pipeline produces) and hands the outer a `Broadcast` half, so the outer's
    *     rebuild needs no index. A tabulating outer writes each position its own value, so
    *     `tuple.andThen(<iso shim>).modify(f)` reaches every slot with its own `f`.
    *
    * No write here needs a stand-in value: every inner is handed the leftover its own read produced
    * and, when it has one, the index its bridge was constructed with.
    *
    * Two tabulating sides (`MultiFocus.tuple` ∘ `MultiFocus.tuple`) are the one composition whose
    * read is *not* a bijection: the composite reads one position per index (the diagonal), so the
    * off-diagonal of each inner structure is not in the composite's focus vector. The plain
    * `composeFrom` can only be lossy there (no `C => D` exists to put `C` values back into a `D`
    * bundle); when the inner optic carries `Optic.SameFocus` — the witness `Optic.andThen` mixes
    * into every composite from that call site's `C =:= D`, and the monomorphic factories mix in
    * themselves — this kernel puts every position back where it was read, which is exactly the
    * inverse of that diagonal read.
    */
  given mfAssocFunction1[X0, Xo, Xi]: AssociativeFunctor[Function1Carrier[X0], Xo, Xi] with
    type Z = AssocF1Z[X0, Xo, Xi]

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, Function1Carrier[X0]] { type X = Xo },
        inner: Optic[A, B, C, D, Function1Carrier[X0]] { type X = Xi },
    ): Function1Carrier[X0][Z, C] =
      val outerBundle = outer.to(s)
      val xo: Xo = outerBundle.context
      chunkOf(outerBundle) match
        case b: Broadcast[Function1[X0, *], A] @unchecked =>
          // The outer reads the same value at every index: one inner read, and the composite read
          // keeps whatever shape the inner has. The inner leftover that read produced is the one the
          // composite's write consumes (its write is a single value too).
          val innerBundle = inner.to(b.value)
          (new AssocF1Z(xo, Left(innerBundle.context)), chunkOf(innerBundle))
        case k: (X0 => A) @unchecked =>
          // The outer reads a real position: one inner read per index. The plan records what each of
          // those reads produced — leftover, read bundle, and shape — because the composite's write
          // runs one inner write per position and needs all three.
          val read: X0 => C = (i: X0) => readAt(chunkOf(inner.to(k(i))), i)
          val innerReads: X0 => AssocF1InnerRead[X0, Xi] = (i: X0) =>
            val innerBundle = inner.to(k(i))
            val half = chunkOf(innerBundle)
            new AssocF1InnerRead(
              innerBundle.context,
              (j: X0) => readAt(half, j),
              isIndexFree(half),
            )
          (new AssocF1Z(xo, Right(innerReads)), read)

    def composeFrom[S, T, A, B, C, D](
        xd: Function1Carrier[X0][Z, D],
        inner: Optic[A, B, C, D, Function1Carrier[X0]] { type X = Xi },
        outer: Optic[S, T, A, B, Function1Carrier[X0]] { type X = Xo },
    ): T =
      val z: AssocF1Z[X0, Xo, Xi] = xd.context
      val writeHalf = chunkOf(xd)
      z.innerPlan match
        case Left(xi) =>
          // Index-free read: the composite's write is one value, and it collapses inside the inner
          // (where the written bundle belongs). Handing the outer a `Broadcast` half means the
          // outer's `from` reads the value outright — no sampling of a rebuild closure.
          val b: B = inner.from((xi, writeHalf))
          outer.from((z.xo, broadcastHalf[X0, B](b)))
        case Right(innerReads) =>
          inner match
            case bc: Function1BroadcastOptic[A, B, C, D, X0] @unchecked =>
              // The broadcast optic's own hook: it builds one value per position, so position `i`
              // is built from the written focus `i` — no bundle read it did not build.
              outer.from((z.xo, (i: X0) => bc.broadcastFrom(readAt(writeHalf, i))))
            case w: Optic.SameFocus[C, D, Function1Carrier[X0]] @unchecked =>
              // The inner says its read and write types coincide, so each position can take the
              // written value and keep the rest of what its read produced.
              val perIndex: X0 => B = (i: X0) =>
                val read = innerReads(i)
                inner.from((read.xi, restoredBundle(read, i, w.sameFocus, writeHalf)))
              outer.from((z.xo, perIndex))
            case _ =>
              // No witness: one value per position, the documented lossy default.
              val perIndex: X0 => B = (i: X0) =>
                val read = innerReads(i)
                inner.from((read.xi, broadcastHalf[X0, D](readAt(writeHalf, i))))
              outer.from((z.xo, perIndex))

  /** The bundle to hand a stable-focus inner at outer position `i`: the value written at `i`, and
    * every other position restored from the read that position came from — the exact inverse of
    * [[mfAssocFunction1]]'s diagonal read. An inner with no positions of its own (`indexFree`) has
    * nothing to restore, so it gets the single written value, exactly as the witness-less write
    * gives it.
    */
  private def restoredBundle[X0, C, D, Xi](
      read: AssocF1InnerRead[X0, Xi],
      i: X0,
      same: Option[C =:= D],
      writeHalf: Focus[Function1[X0, *], D],
  ): Focus[Function1[X0, *], D] =
    same match
      case Some(ev) if !read.indexFree =>
        // `read.read` was stored by this kernel's own `composeTo`, which built it as `X0 => C`.
        val restored: X0 => C = read.read.asInstanceOf[X0 => C]
        (j: X0) => if j == i then readAt(writeHalf, j) else ev(restored(j))
      case _ => broadcastHalf[X0, D](readAt(writeHalf, i))

  /** Composed existential for the `Function1[X0, *]` same-carrier kernel: the outer's leftover plus
    * the inner reads the read observed — `Left(xi)` when the outer read was index-free (one inner
    * read, one leftover, and the composite's write is a single value too), `Right(innerReads)` when
    * it read a real position (per index: the inner leftover, its read bundle, and whether that
    * bundle was index-free). The read shape and the leftover layout are the same fact, so they ride
    * in one field; [[mfAssocFunction1]]'s write needs the read bundle when the inner can take its
    * reads back (`Optic.SameFocus`) and only the leftover otherwise.
    *
    * Threading the *observed* inner leftovers (rather than leaving them for `composeFrom` to invent
    * — as the pre-sum kernel did) is what makes a composite composable on either side: a
    * right-associated inner is itself an `AssocF1Z`-carrying composite and reads that context back.
    * [[mfAssoc]] and [[mfAssocPSVec]] record per-element inner leftovers for the same reason.
    */
  final private[eo] class AssocF1Z[X0, Xo, Xi](
      val xo: Xo,
      val innerPlan: Either[Xi, X0 => AssocF1InnerRead[X0, Xi]],
  )

  /** One observed inner read at an outer position: its leftover, its read bundle *as a function*,
    * and whether that bundle was index-free.
    *
    * `read` is erased to `X0 => Any` because its element type is the method's `C`; both writers
    * cast it back inside the same kernel instantiation that built it, so the cast cannot lie (the
    * same existential-storage pattern as `AssocSndZ.ys` and the `Affine` hit / miss markers).
    */
  final private[eo] class AssocF1InnerRead[X0, Xi](
      val xi: Xi,
      val read: X0 => Any,
      val indexFree: Boolean,
  )

  /** PSVec-specialised same-carrier composition. Where the generic [[mfAssoc]] body builds two
    * intermediate List accumulators + materialises via `fromList`, this body writes directly into
    * `IntArrBuilder` / `ObjArrBuilder` and stores the existential as parallel arrays in
    * `AssocSndZ`, sidestepping the per-element `(Xi, Int)` Tuple2 the generic path pays. Fast paths
    * for `MultiFocusSingleton` (always-hit, Lens bridge) and `MultiFocusPSMaybeHit` (Prism /
    * Optional bridges) inners; reassembly hands each inner an O(1) `PSVec.slice` view. See
    * `docs/research/2026-04-29-powerseries-fold-spike.md`.
    *
    * @group Instances
    */
  given mfAssocPSVec[Xo, Xi]: AssociativeFunctor[MultiFocus[PSVec], Xo, Xi] with
    type SndZ = AssocSndZ[Xo, Xi]
    type Z = SndZ

    // `composeTo` is a small dispatcher over four private per-branch bodies, NOT one method —
    // the monolithic body measured 432 bytecode bytes, past C2's 325-byte hot-inline ceiling
    // (`FreqInlineSize`), so it failed to inline at EVERY call site ("hot method too big",
    // PrintInlining 2026-07-23, PowerSeriesNestedBench). Split, the dispatcher inlines and each
    // branch body sits under the ceiling, so per-site profiles can flatten the whole path —
    // the same forwarder pattern JsoniterPrism.to/from documents.

    def composeTo[S, T, A, B, C, D](
        s: S,
        outer: Optic[S, T, A, B, MultiFocus[PSVec]] { type X = Xo },
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
    ): MultiFocus[PSVec][Z, C] =
      // `PSVec`-carried bundles are never broadcast (no factory writes a `Broadcast` half here), so
      // the focus half is read through the façade: `.foci` is identity for a tabulation and the
      // stored vector for a broadcast.
      val bundle = outer.to(s)
      val xo: Xo = bundle.context
      val va: PSVec[A] = bundle.foci
      inner match
        case ah: MultiFocusSingleton[A, B, C, D, Xi] @unchecked => singletonTo(xo, va, ah)
        case mh: MultiFocusPSMaybeHit[A, B, C, D] @unchecked    => maybeHitTo(xo, va, mh)
        case _ if va.length == 1                                => singleOuterTo(xo, va, inner)
        case _                                                  => genericTo(xo, va, inner)

    /** Always-hit fast path (mfSingleton): every call produces exactly one focus. */
    private def singletonTo[A, B, C, D](
        xo: Xo,
        va: PSVec[A],
        ah: MultiFocusSingleton[A, B, C, D, Xi],
    ): MultiFocus[PSVec][Z, C] =
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      val flatBuf = new ObjArrBuilder(n)
      @tailrec def loop(i: Int): Unit =
        if i < n then
          ah.collectSingletonTo(va(i), ysBuf, flatBuf)
          loop(i + 1)
      loop(0)
      val sndZ = new AssocSndZ[Xo, Xi](xo, null, ysBuf.freezeArr)
      (sndZ, flatBuf.freezeAsPSVec[C])

    /** Maybe-hit fast path (Prism / Optional morphs). */
    private def maybeHitTo[A, B, C, D](
        xo: Xo,
        va: PSVec[A],
        mh: MultiFocusPSMaybeHit[A, B, C, D],
    ): MultiFocus[PSVec][Z, C] =
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      val lenBuf = new IntArrBuilder(n)
      val flatBuf = new ObjArrBuilder(n)
      @tailrec def loop(i: Int): Unit =
        if i < n then
          mh.collectTo(va(i), lenBuf, ysBuf, flatBuf)
          loop(i + 1)
      loop(0)
      val sndZ = new AssocSndZ[Xo, Xi](xo, lenBuf.freeze, ysBuf.freezeArr)
      (sndZ, flatBuf.freezeAsPSVec[C])

    /** Single outer focus (e.g. a Lens onto a collection field, then `each`): the inner's own focus
      * vector IS the entire flat focus. Emit it zero-copy — no `flatBuf`, no grow, no O(n)
      * `appendAllFromPSVec` copy. `composeFrom`'s generic branch reconstructs from a one-entry
      * `lens` array + `vys.slice`, so this stays symmetric.
      */
    private def singleOuterTo[A, B, C, D](
        xo: Xo,
        va: PSVec[A],
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
    ): MultiFocus[PSVec][Z, C] =
      val ysBuf = new ObjArrBuilder(1)
      val innerBundle = inner.to(va.head)
      val xi: Xi = innerBundle.context
      val vy: PSVec[C] = innerBundle.foci
      ysBuf.unsafeAppend(xi)
      val lenArr = new Array[Int](1)
      lenArr(0) = vy.length
      val sndZ = new AssocSndZ[Xo, Xi](xo, lenArr, ysBuf.freezeArr)
      (sndZ, vy)

    /** Generic fallback: taken when `inner` is neither a `MultiFocusSingleton` (Lens bridge) nor a
      * `MultiFocusPSMaybeHit` (Prism / Optional bridge) and the outer is multi-focus — i.e. a
      * multi-focus inner such as `each` itself, or any composed `MultiFocus[PSVec]` optic. Reached
      * on `each.andThen(each)` shapes, so it is a real hot path, not merely a downstream escape
      * hatch.
      */
    private def genericTo[A, B, C, D](
        xo: Xo,
        va: PSVec[A],
        inner: Optic[A, B, C, D, MultiFocus[PSVec]] { type X = Xi },
    ): MultiFocus[PSVec][Z, C] =
      val n = va.length
      val ysBuf = new ObjArrBuilder(n)
      // `flatBuf`'s final size is the sum of the inners' cardinalities — unknown up front.
      // Floor the capacity at `max(n, 16)`: `n` is a lower bound on the total, and the `16`
      // keeps a small-`n`/high-fanout chain (e.g. `each.andThen(each)` over few-but-large
      // sub-containers) from doubling *more* than the old default-capacity-16 builder did.
      val lenBuf = new IntArrBuilder(n)
      val flatBuf = new ObjArrBuilder(math.max(n, 16))
      @tailrec def loop(i: Int): Unit =
        if i < n then
          val innerBundle = inner.to(va(i))
          val xi: Xi = innerBundle.context
          val vy: PSVec[C] = innerBundle.foci
          lenBuf.append(vy.length)
          ysBuf.append(xi)
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
      val sndZ = xd.context
      val vys: PSVec[D] = xd.foci
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
                ah.singletonFrom(ys(i).asInstanceOf[Xi], vys(i))
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
                mh.reconstructSingleton(ys(i), vys, offset, len)
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
              resultBuf.unsafeAppend(inner.from((y, chunk)))
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
      val ys: Array[Any],
  )

  // ------------------------------------------------------------------
  // Kaleidoscope universal — the `.collect*` aggregation surface. Not derivable from a single
  // typeclass in a way that covers both aggregation shapes, so two variants ship and the user
  // picks (see the carrier doc above).
  // ------------------------------------------------------------------

  /** Singleton / cartesian aggregation, List-specific — collapses the whole focus list to ONE
    * aggregated element: the result list has length 1 regardless of the focus count.
    * Shape-changing, which is exactly what no `Functor`-based combinator can express; the
    * length-preserving alternatives are [[collectMap]] / [[collectWith]].
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, MultiFocus[List]])

    def collectList(agg: List[A] => B)(using ev: S =:= List[A], ev2: T =:= List[B]): S => T =
      val _ = (ev, ev2)
      // Cartesian / singleton — T = List[B] preserved via List(b). The collapse replaces the whole
      // focus vector, so no *new* leftover can be derived for it; the one the read produced is the
      // right thing to hand back, and it is the only thing that works for an optic whose `from`
      // reads its own leftover.
      (s: S) =>
        val bundle = o.to(s)
        o.from((bundle.context, List(agg(bundle.foci))))

  /** Functor-broadcast aggregation — preserves F-shape via `map(_ => agg(fa))`; every focus
    * position receives the aggregate. Works for any `Functor[F]`; for List this is the
    * ZipList-style length-preserving aggregation (use [[collectList]] for the shape-collapsing
    * List-singleton semantics). For the generic [[apply]] factory (`X = F[A]`, rebuild = identity),
    * `.collectMap` is `s => s.map(_ => agg(s))` — semantically `Functor.map` over the source.
    */
  extension [S, T, A, B, F[_]](o: Optic[S, T, A, B, MultiFocus[F]])(using F: Functor[F])

    def collectMap[C](agg: F[A] => C)(using ev: C =:= B): S => T =
      val _ = ev
      (s: S) =>
        val bundle = o.to(s)
        val b: C = agg(bundle.foci)
        // Route through the carrier's own `map`: a broadcast half stays a broadcast half, so a
        // broadcast optic's write stays index-free.
        o.from(summon[ForgetfulFunctor[MultiFocus[F]]].map(bundle, (_: A) => ev(b)))

    /** The algebraic-lens universal for the map-shaped collects — `agg` sees the whole focus
      * collection ONCE, returns the per-focus rewrite, and that rewrite is mapped back over every
      * position: `fa.map(a => agg(fa)(a))`. The currying is load-bearing: compute the batch summary
      * in `agg(fa)`, and the returned `A => B` runs per position without recomputing it.
      *
      * Subsumes both map-shaped siblings — `collectMap(agg) = collectWith(fa => _ => agg(fa))`
      * (constant per-focus function) and `modify(f) = collectWith(_ => f)` (aggregate ignored);
      * pinned as laws MF4 / MF5. The shape-collapsing [[collectList]] is NOT expressible here: it
      * changes the focus count, which no `Functor`-based combinator can. Requires only
      * `Functor[F]`, like [[collectMap]].
      */
    def collectWith(agg: F[A] => A => B): S => T =
      (s: S) =>
        val bundle = o.to(s)
        // `agg(bundle.foci)` runs ONCE per call (the load-bearing currying); the returned function
        // then runs per focus position.
        o.from(summon[ForgetfulFunctor[MultiFocus[F]]].map(bundle, agg(bundle.foci)))

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

    def at(i: F.Representation): S => A = (s: S) => F.index(o.to(s).foci)(i)

  /** Generic factory: `X = F[A]`, focus = fa, rebuild = identity.
    *
    * @group Constructors
    */
  def apply[F[_], A]: Optic[F[A], F[A], A, A, MultiFocus[F]] =
    // Its own body (not `pApply[F, A, A]`) so the monotone case can carry `Optic.SameFocus`: read
    // and write types coincide here by construction, which is what lets a kernel whose read cannot
    // be inverted from the bundle alone put read values back (`mfAssocFunction1`).
    new Optic[F[A], F[A], A, A, MultiFocus[F]] with Optic.SameFocus[A, A, MultiFocus[F]]:
      type X = F[A]
      def sameFocus: Option[A =:= A] = Some(summon[A =:= A])
      def to(fa: F[A]): MultiFocus[F][F[A], A] = (fa, fa)
      def from(mf: MultiFocus[F][F[A], A]): F[A] = mf.foci

  /** Polymorphic counterpart to [[apply]] — allows focus type change (`F[A] => F[B]`), the
    * `Traversal.pEach` analogue at the generic factory. Sound for the same reason: the rebuild is
    * identity on the written-back `F[B]`, so no `B` ever has to fit an `A`-shaped hole. This is
    * what un-pins the collect flavours' element type — `pApply[F, A, Row].collectWith(...)` emits a
    * different row type than it read.
    *
    * @group Constructors
    */
  def pApply[F[_], A, B]: Optic[F[A], F[B], A, B, MultiFocus[F]] =
    new Optic[F[A], F[B], A, B, MultiFocus[F]]:
      type X = F[A]
      def to(fa: F[A]): MultiFocus[F][F[A], A] = (fa, fa)
      def from(mf: MultiFocus[F][F[A], B]): F[B] = mf.foci

  /** Iso → MultiFocus[F]. Requires `Applicative[F]` to broadcast the Iso's plain `A` focus into a
    * singleton `F[A]`; the write-back picks the singleton out (throws on cardinality ≠ 1).
    *
    * @group Instances
    */
  given forgetful2multifocus[F[_]: Applicative: Foldable]: Composer[Direct, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Direct]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        def to(s: S): MultiFocus[F][Unit, A] = ((), Applicative[F].pure(o.to(s).value))
        def from(mf: MultiFocus[F][Unit, B]): T =
          o.from(Direct(pickSingletonOrThrow(mf.foci, "Direct")))

  /** Forget[F] ↪ MultiFocus[F] — a Fold slots into the pair carrier with `X = Unit`.
    *
    * @group Instances
    */
  given forget2multifocus[F[_]]: Composer[Forget[F], MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Forget[F]]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Unit
        def to(s: S): MultiFocus[F][Unit, A] = ((), o.to(s).value)
        def from(mf: MultiFocus[F][Unit, B]): T = o.from(ForgetK(mf.foci))

  /** MultiFocus[F] ↪ Forget[F] — read-only escape: discard the structural leftover, keep the
    * focused `F[A]`. An explicit carrier morph alongside the carrier-wide `Optic.foldMap` /
    * `.headOption` / `.length` / `.exists` extension methods.
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
    *
    * @group Instances
    */
  given multifocus2forget[F[_]]: Composer[MultiFocus[F], Forget[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, Forget[F]] =
      new Optic[S, T, A, B, Forget[F]]:
        type X = Unit
        def to(s: S): ForgetK[F, X, A] = ForgetK(o.to(s).foci)
        def from(fb: ForgetK[F, X, B]): T =
          // Reachable only when `T = Unit` (Forget-carrier optics have T = Unit by construction).
          // The cast surfaces a ClassCastException if a user's MultiFocus optic has T ≠ Unit AND
          // they explicitly routed through this Composer — defensive only.
          ().asInstanceOf[T]

  /** Lens → MultiFocus[F]. Mixes in `MultiFocusSingleton` so the [[mfAssoc]] fast-path fires.
    *
    * @group Instances
    */
  given tuple2multifocus[F[_]: Applicative: Foldable]: Composer[Tuple2, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]] with MultiFocusSingleton[S, T, A, B, o.X]:
        type X = o.X
        def singletonTo(s: S): (o.X, A) = o.to(s)
        def singletonFrom(x: o.X, b: B): T = o.from((x, b))
        def to(s: S): MultiFocus[F][X, A] =
          val (x, a) = o.to(s)
          (x, Applicative[F].pure(a))
        def from(mf: MultiFocus[F][X, B]): T =
          o.from((mf.context, pickSingletonOrThrow(mf.foci, "Tuple2")))

  /** Shared hit marker for the `X = Either[…, Unit]` bridges — covariance upcasts
    * `Either[Nothing, Unit]` to any `Either[x, Unit]`, so one instance serves every hit.
    */
  private val hitUnit: Either[Nothing, Unit] = Right(())

  /** Prism → MultiFocus[F] — hit becomes a `pure` singleton, miss becomes `Alternative[F].empty`.
    *
    * @group Instances
    */
  given either2multifocus[F[_]: Alternative: Foldable]: Composer[Either, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        type X = Either[o.X, Unit]
        def to(s: S): MultiFocus[F][X, A] =
          o.to(s) match
            case Right(a)    => (hitUnit, Applicative[F].pure(a))
            case l @ Left(_) => (l.widenRight[Unit], Alternative[F].empty[A])
        def from(mf: MultiFocus[F][X, B]): T =
          mf.context match
            case l @ Left(_) => o.from(l.widenRight[B])
            case Right(_)    => o.from(Right(pickSingletonOrThrow(mf.foci, "Either")))

  /** Optional → MultiFocus[F] — mirror of [[either2multifocus]] over the `Affine` miss / hit split.
    *
    * @group Instances
    */
  given affine2multifocus[F[_]: Alternative: Foldable]: Composer[Affine, MultiFocus[F]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[F]] =
      new Optic[S, T, A, B, MultiFocus[F]]:
        // X = the Affine itself (miss recycled via covariant retype, both directions) rather than an
        // unpacked Either[Fst, Snd] — same shape as `multifocusF2multifocus` below.
        type X = Affine[o.X, Unit]
        def to(s: S): MultiFocus[F][X, A] =
          o.to(s) match
            case h: Affine.Hit[o.X, A] =>
              (new Affine.Hit[o.X, Unit](h.snd, ()), Applicative[F].pure(h.b))
            case m: Affine.Miss[o.X] =>
              (m, Alternative[F].empty[A])
        def from(mf: MultiFocus[F][X, B]): T =
          mf.context match
            case m: Affine.Miss[o.X] @unchecked =>
              o.from(m)
            case h: Affine.Hit[o.X, Unit] @unchecked =>
              o.from(new Affine.Hit[o.X, B](h.snd, pickSingletonOrThrow(mf.foci, "Affine")))

  // PSVec-specialised Composer instances. PSVec admits neither Applicative nor Alternative
  // naturally; these use `PSVec.singleton` / `PSVec.empty` directly. The Tuple2 bridge specialises
  // on `GetReplaceLens` to skip the intermediate `(s, get(s))` Tuple2; Either / Affine bridges mix
  // in `MultiFocusPSMaybeHit` so `mfAssocPSVec` picks up the Prism/Optional fast-path.

  /** Lens → MultiFocus[PSVec]. `GetReplaceLens` fast-path elides the `(s, get(s))` Tuple2 the
    * generic body would build. The `to` body builds anonymous Optic values inline so
    * `MultiFocusSingleton` can refer to `o.X` without tripping Scala 3's "class parent cannot refer
    * to constructor parameters" rule.
    *
    * @group Instances
    */
  given tuple2multifocusPSVec: Composer[Tuple2, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Tuple2]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      o match
        case lens: optics.GetReplaceLens[S, T, A, B] @unchecked =>
          new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusSingleton[S, T, A, B, S]:
            type X = S
            def to(s: S): MultiFocus[PSVec][S, A] = (s, PSVec.singleton[A](lens.get(s)))
            def from(mf: MultiFocus[PSVec][S, B]): T =
              lens.enplace(mf.context, mf.foci.head)
            def singletonTo(s: S): (S, A) = (s, lens.get(s))
            def singletonFrom(x: S, b: B): T = lens.enplace(x, b)
            override def collectSingletonTo(
                s: S,
                ysBuf: ObjArrBuilder,
                flatBuf: ObjArrBuilder,
            ): Unit =
              ysBuf.unsafeAppend(s)
              flatBuf.unsafeAppend(lens.get(s))
        case _ =>
          new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusSingleton[S, T, A, B, o.X]:
            type X = o.X
            def to(s: S): MultiFocus[PSVec][o.X, A] =
              val (xo, a) = o.to(s)
              (xo, PSVec.singleton[A](a))
            def from(mf: MultiFocus[PSVec][o.X, B]): T =
              o.from((mf.context, mf.foci.head))
            def singletonTo(s: S): (o.X, A) = o.to(s)
            def singletonFrom(x: o.X, b: B): T = o.from((x, b))

  /** Prism → MultiFocus[PSVec] — lifts a Prism-carrier optic so `prism.andThen(traversal)`
    * type-checks. Mixes in `MultiFocusPSMaybeHit` so the PSVec-specialised [[mfAssocPSVec]] body
    * skips the per-element `Either[o.X, Unit]` wrapper the generic `to` path would build.
    *
    * @group Instances
    */
  given either2multifocusPSVec: Composer[Either, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Either]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusPSMaybeHit[S, T, A, B]:
        type X = Option[o.X]
        def to(s: S): MultiFocus[PSVec][Option[o.X], A] =
          o.to(s) match
            case Left(x)  => (Some(x), PSVec.empty[A])
            case Right(a) => (None, PSVec.singleton[A](a))
        def from(mf: MultiFocus[PSVec][Option[o.X], B]): T =
          mf.context match
            case Some(x) => o.from(Left(x))
            case None    => o.from(Right(mf.foci.head))

        def collectTo(
            s: S,
            lenBuf: IntArrBuilder,
            ysBuf: ObjArrBuilder,
            flatBuf: ObjArrBuilder,
        ): Unit =
          o.to(s) match
            case Left(x) =>
              lenBuf.unsafeAppend(0)
              ysBuf.unsafeAppend(x)
            case Right(a) =>
              lenBuf.unsafeAppend(1)
              ysBuf.unsafeAppend(null)
              flatBuf.unsafeAppend(a)

        def reconstructSingleton(y: Any, vys: PSVec[B], pos: Int, len: Int): T =
          if len == 0 then o.from(Left(y.asInstanceOf[o.X]))
          else o.from(Right(vys(pos)))

  /** Optional → MultiFocus[PSVec]. Mixes in `MultiFocusPSMaybeHit` so [[mfAssocPSVec]] skips the
    * per-element `Affine[o.X, Unit]` wrapper the generic path would build.
    *
    * @group Instances
    */
  given affine2multifocusPSVec: Composer[Affine, MultiFocus[PSVec]] with

    def to[S, T, A, B](o: Optic[S, T, A, B, Affine]): Optic[S, T, A, B, MultiFocus[PSVec]] =
      new Optic[S, T, A, B, MultiFocus[PSVec]] with MultiFocusPSMaybeHit[S, T, A, B]:
        // X = the Affine itself (miss recycled via covariant retype, both directions) — see
        // `affine2multifocus`. The collectTo / reconstructSingleton buffer protocol below
        // is X-independent and keeps its unpacked fst / snd encoding.
        type X = Affine[o.X, Unit]
        def to(s: S): MultiFocus[PSVec][X, A] =
          o.to(s) match
            case m: Affine.Miss[o.X]   => (m, PSVec.empty[A])
            case h: Affine.Hit[o.X, A] =>
              (new Affine.Hit[o.X, Unit](h.snd, ()), PSVec.singleton[A](h.b))
        def from(mf: MultiFocus[PSVec][X, B]): T =
          mf.context match
            case m: Affine.Miss[o.X] @unchecked      => o.from(m)
            case h: Affine.Hit[o.X, Unit] @unchecked =>
              o.from(new Affine.Hit[o.X, B](h.snd, mf.foci.head))

        def collectTo(
            s: S,
            lenBuf: IntArrBuilder,
            ysBuf: ObjArrBuilder,
            flatBuf: ObjArrBuilder,
        ): Unit =
          o.to(s) match
            case m: Affine.Miss[o.X] =>
              lenBuf.unsafeAppend(0)
              ysBuf.unsafeAppend(m.fst)
            case h: Affine.Hit[o.X, A] =>
              lenBuf.unsafeAppend(1)
              ysBuf.unsafeAppend(h.snd)
              flatBuf.unsafeAppend(h.b)

        def reconstructSingleton(y: Any, vys: PSVec[B], pos: Int, len: Int): T =
          if len == 0 then o.from(new Affine.Miss[o.X](y.asInstanceOf[Fst[o.X]]))
          else o.from(new Affine.Hit[o.X, B](y.asInstanceOf[Snd[o.X]], vys(pos)))

  /** MultiFocus[F] → ModifyF. Uniform Modify widening for any `Functor[F]`.
    *
    * @group Instances
    */
  given multifocus2modify[F[_]: Functor]: Composer[MultiFocus[F], ModifyF] with

    def to[S, T, A, B](o: Optic[S, T, A, B, MultiFocus[F]]): Optic[S, T, A, B, ModifyF] =
      new Optic[S, T, A, B, ModifyF]:
        type X = (S, A)
        def to(s: S): ModifyF[X, A] = ModifyF((s, identity[A]))
        def from(sfxb: ModifyF[X, B]): T =
          val (s, f) = sfxb.modifier
          val bundle = o.to(s)
          o.from(summon[ForgetfulFunctor[MultiFocus[F]]].map(bundle, f))

  // F[A]-focus factories.

  /** Reinterpret a Lens whose focus is already an `F[A]` as a MultiFocus optic over the elements —
    * the container the Lens reads IS the focus vector; `X` and the rebuild pass straight through.
    *
    * @group Constructors
    */
  def fromLensF[F[_], S, T, A, B](
      lens: Optic[S, T, F[A], F[B], Tuple2]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = lens.X
      def to(s: S): MultiFocus[F][X, A] = lens.to(s)
      def from(mf: MultiFocus[F][X, B]): T = lens.from((mf.context, mf.foci))

  /** Reinterpret a Prism whose focus is an `F[A]` as a MultiFocus optic over the elements — the
    * miss branch surfaces as `MonoidK[F].empty` (zero foci) and passes the leftover back through
    * the prism's build path on write.
    *
    * @group Constructors
    */
  def fromPrismF[F[_]: MonoidK, S, T, A, B](
      prism: Optic[S, T, F[A], F[B], Either]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Either[prism.X, Unit]
      def to(s: S): MultiFocus[F][X, A] =
        prism.to(s) match
          case Right(fa)   => (hitUnit, fa)
          case l @ Left(_) => (l.widenRight[Unit], MonoidK[F].empty[A])
      def from(mf: MultiFocus[F][X, B]): T =
        mf.context match
          case l @ Left(_) => prism.from(l.widenRight[F[B]])
          case Right(_)    => prism.from(Right(mf.foci))

  // Function1-shaped MultiFocus factories (the Grate-absorbed surface).

  /** Generic Function1-shaped factory — any `Representable[F]` container yields a
    * `MultiFocus[Function1[F.Representation, *]]`-carrier optic over `F[A]` with focus `A`.
    * Encoding: `X = Unit`, rebuild = `F.Representation => A`. On `to(fa)` snapshot `F.index(fa)`;
    * on `from((_, k))` materialise via `F.tabulate(k)`. The `.modify(f)` round-trip is exactly
    * `F.map(fa)(f)`.
    *
    * Position is a read-time argument, never a property of the optic: the index is supplied per
    * call by the `.at(i)` extension (`g.at(i)(fa) == F.index(fa)(i)`), and `Representable` has no
    * canonical `Representation` to hand a constructor anyway — `Function1[Boolean, *]` has no
    * privileged `Boolean`, and `Function1[Nothing, *]` has no index at all. So this factory takes
    * no representative index, and the built optic carries none: `X = Unit`, and the whole
    * index-parametric read lives in the focus bundle. (Pre-0.19 a second name,
    * `representableAt(F)(repr0)`, took exactly such an index; it built this same optic — see the
    * changelog for the removal.)
    *
    * @group Constructors
    */
  def representable[F[_], A](using
      F: Representable[F]
  ): Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]] =
    new Optic[F[A], F[A], A, A, MultiFocus[Function1[F.Representation, *]]]
      with Optic.SameFocus[A, A, MultiFocus[Function1[F.Representation, *]]]:
      type X = Unit
      def sameFocus: Option[A =:= A] = Some(summon[A =:= A])
      def to(fa: F[A]): MultiFocus[Function1[F.Representation, *]][Unit, A] = ((), F.index(fa))
      def from(mf: MultiFocus[Function1[F.Representation, *]][Unit, A]): F[A] =
        F.tabulate(mf.foci)

  /** Pointwise ZIP of two containers — a Grate's reason for existing, and the one operation
    * [[representable]] made possible but never exposed.
    *
    * A Grate is `((F[A] => A) => B) => F[B]`: unlike a Traversal it can see EVERY focus at once
    * while rebuilding, which is exactly what combining two structures needs. `Traversal` cannot
    * express this — it visits one focus at a time with no access to a second structure — and
    * neither can `modify`. Here the shape is concrete: read both containers as index functions and
    * tabulate their pointwise combination.
    *
    * Lawful for any `Representable[F]` (no shape to mismatch — every representation point exists in
    * both), and `zipWith(fa, fa)(f) == F.map(fa)(a => f(a, a))`.
    *
    * @example
    *   {{{
    *   // Two configurations merged field-by-field:
    *   val merged = MultiFocus.zipWith(defaults, overrides)((d, o) => o.orElse(d))
    *   }}}
    *
    * @group Constructors
    */
  def zipWith[F[_], A, B, C](fa: F[A], fb: F[B])(f: (A, B) => C)(using F: Representable[F]): F[C] =
    val ia = F.index(fa)
    val ib = F.index(fb)
    F.tabulate(r => f(ia(r), ib(r)))

  /** [[zipWith]]'s pairing special case — the `F[(A, B)]` product of two containers. */
  def zip[F[_], A, B](fa: F[A], fb: F[B])(using Representable[F]): F[(A, B)] =
    zipWith(fa, fb)((a, b) => (a, b))

  /** Polymorphic homogeneous-tuple Function1-shaped factory. `to(t) = ((), i => t._i)`,
    * `from((_, k))` materialises via `Tuple.fromArray(Array.tabulate(size)(i => k(i)))`.
    *
    * @example
    *   {{{
    *   val g3 = MultiFocus.tuple[(Int, Int, Int), Int]
    *   g3.modify(_ + 1)((1, 2, 3))   // (2, 3, 4)
    *   g3.replace(42)((1, 2, 3))     // (42, 42, 42)
    *   }}}
    *
    * @group Constructors
    */
  def tuple[T <: Tuple, A](using
      sz: ValueOf[Tuple.Size[T]],
      ev: Tuple.Union[T] <:< A,
  ): Optic[T, T, A, A, MultiFocus[Function1[Int, *]]] =
    val _ = ev
    val size = sz.value
    new Optic[T, T, A, A, MultiFocus[Function1[Int, *]]]
      with Optic.SameFocus[A, A, MultiFocus[Function1[Int, *]]]:
      type X = Unit
      def sameFocus: Option[A =:= A] = Some(summon[A =:= A])
      def to(t: T): MultiFocus[Function1[Int, *]][Unit, A] =
        val read: Int => A = (i: Int) => t.productElement(i).asInstanceOf[A]
        ((), read)
      def from(mf: MultiFocus[Function1[Int, *]][Unit, A]): T =
        val k = mf.foci
        val arr = new Array[Any](size)
        @tailrec def loop(i: Int): Unit =
          if i < size then
            arr(i) = k(i)
            loop(i + 1)
        loop(0)
        Tuple.fromArray(arr).asInstanceOf[T]

  /** Construct an index-free (broadcast) bundle on the `Function1[X0, *]`-shaped carrier: `a` is
    * the focus at every index, and it is *known* without consulting one. The mirror of
    * [[MultiFocusK.foci]] (`F[A]`-shaped consumers) for consumers that need the value itself.
    *
    * Use it when an optic's source has no `X0`-index of its own — an Iso reshaped into this carrier
    * ([[forgetful2multifocusFunction1]]), or any optic that broadcasts one focus across the index
    * set. Compositions read the difference: an index-free outer is read once (not per index), and
    * the write of a composite whose read was index-free stays index-free all the way out.
    *
    * @group Constructors
    */
  def broadcast[X0, X, A](x: X, a: A): MultiFocusK[Function1[X0, *], X, A] =
    (x, broadcastHalf[X0, A](a))

  /** Iso ↪ MultiFocus[Function1[X0, *]] — the Iso side of the grate-shaped surface. Iso's forward
    * `to: S => A` is broadcast to every index, so the bundle it produces is index-free
    * ([[broadcast]]) and a composite reading through it never has to sample an index.
    *
    * The product is a [[Function1BroadcastOptic]] — a class because the *optic* is the one thing
    * the carrier's sum cannot describe before a read: a broadcast optic builds exactly one value
    * per position, so [[mfAssocFunction1]] writes it through the class's own hook
    * ([[Function1BroadcastOptic.broadcastFrom]]) rather than through a bundle read it did not
    * build. The class also owns the one read of a bundle it did not build: an index-free bundle is
    * read outright, and a tabulating one at the real index the [[RepresentativeIndex]] witness
    * supplies — a defined answer, never a forged sentinel, and never a value the index types the
    * Grate factories fix (`Int`, `Boolean`, `Unit`, singletons) have to supply themselves.
    *
    * The `using` clause is the cost side of that contract, exactly as on `main`: index types with a
    * canonical value resolve import-free off [[RepresentativeIndex]]; anything else is refused
    * rather than guessed — no index is ever forged, and nothing else needs a stand-in either.
    *
    * @group Instances
    */
  given forgetful2multifocusFunction1[X0](using
      idx: RepresentativeIndex[X0]
  ): Composer[Direct, MultiFocus[Function1[X0, *]]] with

    def to[S, T, A, B](
        o: Optic[S, T, A, B, Direct]
    ): Optic[S, T, A, B, MultiFocus[Function1[X0, *]]] =
      new Function1BroadcastOptic[S, T, A, B, X0](o, idx.index)

  /** Reinterpret an Optional whose focus is an `F[A]` as a MultiFocus optic over the elements — the
    * mirror of [[fromPrismF]] over the `Affine` miss / hit split (miss recycled covariantly, both
    * directions).
    *
    * @group Constructors
    */
  def fromOptionalF[F[_]: MonoidK, S, T, A, B](
      opt: Optic[S, T, F[A], F[B], Affine]
  ): Optic[S, T, A, B, MultiFocus[F]] =
    new Optic[S, T, A, B, MultiFocus[F]]:
      type X = Affine[opt.X, Unit]
      def to(s: S): MultiFocus[F][X, A] =
        opt.to(s) match
          case m: Affine.Miss[opt.X] @unchecked =>
            (m, MonoidK[F].empty[A])
          case h: Affine.Hit[opt.X, F[A]] @unchecked =>
            (new Affine.Hit[opt.X, Unit](h.snd, ()), h.b)
      def from(mf: MultiFocus[F][X, B]): T =
        mf.context match
          case m: Affine.Miss[opt.X] @unchecked =>
            opt.from(m)
          case h: Affine.Hit[opt.X, Unit] @unchecked =>
            opt.from(new Affine.Hit[opt.X, F[B]](h.snd, mf.foci))

/** API façade under the carrier's public name. The instances live in [[MultiFocusK]] (the opaque
  * anchor's companion, where implicit scope finds them); this re-export keeps `MultiFocus.apply` /
  * `MultiFocus.fromLensF` call-shapes and legacy `import data.MultiFocus.given` working.
  */
object MultiFocus:
  export MultiFocusK.{given, *}

  /** Pair a leftover `x` with a focus vector `fa` as the carrier value. Identity at runtime (the
    * opaque type erases to the pair); the unwrap boundary is [[MultiFocusK.context]] /
    * [[MultiFocusK.foci]].
    */
  transparent inline def apply[F[_], X, A](x: X, fa: F[A]): MultiFocusK[F, X, A] = (x, fa)
