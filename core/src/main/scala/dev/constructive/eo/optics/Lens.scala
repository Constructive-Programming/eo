package dev.constructive.eo
package optics

import cats.Functor

import data.Affine

/** Monomorphic `Lens` — an alias for [[PLens]], the sealed base of both concrete lens classes
  * ([[GetReplaceLens]], [[SplitCombineLens]] / [[SimpleLens]]), not for the generic
  * `Optic[S, S, A, A, Tuple2]`. Ascribing `val ageL: Lens[Person, Int] = …` keeps the fused `get` /
  * `modify` / `replace` / `modifyF` members, the fused `andThen` overloads (they live on `PLens`)
  * and the capability mixins statically visible.
  */
type Lens[S, A] = PLens[S, S, A, A]

/** Constructors for `Lens` — the always-present single-focus optic, backed by `Tuple2`. Every
  * constructor returns a concrete subclass of [[PLens]] ([[GetReplaceLens]] here, [[SimpleLens]]
  * from [[first]] / [[second]] and the `eo-generics` macro). Consuming signatures should still
  * demand a capability (`CanGet[S, A]`, `CanModify[S, A]`, …) rather than a concrete optic type. A
  * lens reads a field via `get(s)` and rewrites it via `modify` / `replace`. The `eo-generics`
  * module's `lens[S](_.field)` macro derives both.
  */
object Lens:
  import Function.uncurried

  /** Polymorphic constructor — allows `S` and `T` to differ.
    *
    * @group Constructors
    */
  def pLens[S, T, A, B](get: S => A, enplace: (S, B) => T) =
    GetReplaceLens(get, enplace)

  /** Monomorphic constructor (`S = T`, `A = B`).
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(name: String, age: Int)
    * val ageL = Lens[Person, Int](_.age, (p, a) => p.copy(age = a))
    *   }}}
    */
  def apply[S, A](get: S => A, enplace: (S, A) => S) =
    pLens[S, S, A, A](get, enplace)

  /** `Representable[F]` (`F[A] ≅ Representation => A`) as a lawful Lens into ONE position: `get =
    * index(fa)(r)`, and the write rebuilds via `tabulate` with every other position read back from
    * the original — siblings survive, all three Lens laws hold. A Lens into a function's value at a
    * point, or position `r` of any tabulated shape — the one optic no whole-container bridge
    * ([[Traversal.each]], [[Modify.functor]], [[Indexed.representable]]) can produce.
    *
    * Lawful provided `==` is meaningful on `Representation` (it keys the rebuild).
    *
    * @group Constructors
    */
  def representable[F[_], A](using
      R: cats.Representable[F]
  )(r: R.Representation): GetReplaceLens[F[A], F[A], A, A] =
    Lens[F[A], A](
      fa => R.index(fa)(r),
      (fa, a) => R.tabulate(x => if x == r then a else R.index(fa)(x)),
    )

  /** Curried variant — accepts `replace: A => S => S` instead of `(S, A) => S`.
    *
    * @group Constructors
    */
  def curried[S, A](get: S => A, replace: A => S => S) =
    pLens[S, S, A, A](get, (s, a) => replace(a)(s))

  /** Polymorphic counterpart to [[curried]].
    *
    * @group Constructors
    */
  def pCurried[S, T, A, B](get: S => A, replace: B => S => T) =
    pLens(get, uncurried(replace))

  /** Lens focusing the first element of a `Tuple2`. Returns a [[SimpleLens]] so `place` /
    * `transfer` / `transform` land without extra evidence (`S = T`, `A = B`).
    *
    * @group Constructors
    */
  def first[A, B] =
    SimpleLens[(A, B), A, B](
      _._1,
      _.swap,
      (b, a) => (a, b),
    )

  /** Lens focusing the second element of a `Tuple2`.
    *
    * @group Constructors
    */
  def second[A, B] =
    SimpleLens[(A, B), B, A](
      _._2,
      identity,
      (a, b) => (a, b),
    )

/** The shared core of both concrete lens classes: a total read `get` plus the write `enplace`, from
  * which every capability a lens has is one fused line — `get` (and `foldMap`, which [[CanGet]]
  * derives from it), `modify` / `replace` and the `Functor`-gated `modifyF`. The two classes differ
  * only in what they store and in their carrier view (`to` / `from`): [[GetReplaceLens]] keeps the
  * whole source as the leftover, [[SplitCombineLens]] the complement its splitter returns.
  */
sealed abstract class PLens[S, T, A, B](read: S => A, val enplace: (S, B) => T)
    extends SingleFocus[S, T, A, B, Tuple2, Functor],
      CanGet[S, A]:
  def get(s: S): A = read(s)

  override inline def replace(b: B): S => T = s => enplace(s, b)

  inline def modify(f: A => B): S => T =
    s => enplace(s, f(get(s)))

  /** Fused effectful modify — reads the focus and maps the write straight over the effect, no
    * `(X, A)` carrier tuple and no `ForgetfulTraverse` hop. Makes the lens its own
    * `CanModifyF[…, Functor]`, so a capability call dispatches here rather than through the derived
    * wrapper.
    */
  def modifyF[G[_]](f: A => G[B])(using G: Functor[G] & Functor[G]): S => G[T] =
    s => G.map(f(get(s)))(b => enplace(s, b))

  // The fused `andThen` matrix: one overload per inner optic, each a carrier-specific kernel.
  // Measured against the generic `AssociativeFunctor` / `Morph` route (AndThenBench, see the 2.0
  // CHANGELOG): never more B/op per call, and only they keep a chain in concrete classes — the
  // generic route allocates per level (lens∘lens depth 6: 528 vs 120 B/op).
  /** Fused `Lens.andThen(Lens)` — collapses into another `GetReplaceLens` directly on `get` /
    * `enplace`, skipping the generic `AssociativeFunctor[Tuple2]` round-trip. Scala's overload
    * resolution picks this when both sides are statically `GetReplaceLens`; for mixed-shape
    * composition the inherited generic `Optic.andThen` fires.
    *
    * `inline` so each compose site splices its own `get` / `enplace` lambdas (distinct synthetic
    * methods per level). A plain `def` shares one `andThen$$anonfun$*` across a depth-N chain, so
    * C2 reads the `.get` / `.enplace` cascade as recursion and caps inlining at
    * `MaxRecursiveInlineLevel`, leaving the deep tail as virtual `Function1.apply` — the same
    * same-bytecode trap fixed on [[Getter.andThen]]. This is the recursive composer, so it's the
    * one that matters for deep chains; the terminal mixed-carrier overloads below fire once and
    * stay plain `def` to avoid duplicating their larger bodies.
    */
  inline def andThen[C, D](inner: PLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      read = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.enplace(get(s), d)),
    )

  /** Fused `Lens.andThen(Iso)` — collapses the iso into the lens's shape; skips the
    * `Composer[Direct, Tuple2]` hop.
    */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      read = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.reverseGet(d)),
    )

  /** Fused `Lens.andThen(Prism)` — outer always hits, inner may miss. Result is `Optional`; skips
    * cross-carrier `Morph.bothViaAffine`.
    */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s => Optional.liftMiss(inner.tear(get(s)))(b => enplace(s, b)),
      modify = (s, f) => enplace(s, inner.modify(f)(get(s))),
      to = s => Optional.tearTo(inner.tear(get(s)))(enplace(s, _), s),
      rebuild = (s, d) => enplace(s, inner.mend(d)),
    )

  /** Fused `Lens.andThen(Prism)` — Option-fast-path inner (PickMend). Mono inner required for type
    * closure.
    */
  def andThen[C, D](inner: PickMendPrism[A, C, D])(using
      ev: A =:= B
  ): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s =>
        val a = get(s)
        Optional.pickOr(inner.pick(a))(enplace(s, ev(a)))
      ,
      modify = (s, f) =>
        val a = get(s)
        inner.pick(a) match
          case Some(c) => enplace(s, ev(inner.mend(f(c))))
          case None    => enplace(s, ev(a)),
      to = s =>
        val a = get(s)
        Optional.pickTo(inner.pick(a))(enplace(s, ev(a)), s)
      ,
      rebuild = (s, d) => enplace(s, ev(inner.mend(d))),
    )

  /** Fused `Lens.andThen(Optional)` — outer always hits, inner may miss. Result is `Optional`. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s => Optional.liftMiss(inner.getOrModify(get(s)))(b => enplace(s, b)),
      modify = (s, f) => enplace(s, inner.modifyAt(get(s), f)),
      to = s => Optional.liftTo(inner, get(s))(enplace(s, _), (s, _)),
      rebuild = (m, d) => enplace(m._1, inner.rebuild(m._2, d)),
    )

/** Concrete lens storing `get` and `enplace` directly; the fused-`andThen` overloads on [[PLens]]
  * bypass the `Tuple2` carrier entirely. Returned by every `Lens.*` constructor so hand-written
  * lenses pick up the fused hot path automatically.
  */
class GetReplaceLens[S, T, A, B](
    read: S => A,
    enplace: (S, B) => T,
) extends PLens[S, T, A, B](read, enplace):
  type X = S
  def to(s: S): (S, A) = (s, get(s))
  def from(pair: (S, B)): T = enplace(pair._1, pair._2)

/** Polymorphic split-combine lens — splitter `S => (XA, A)` + combiner `(XA, B) => T`, surfacing
  * the complement as `X = XA`; a write splits once and combines. Does not ship `place` / `transfer`
  * (would require a `T => (XA, B)` evidence that isn't recoverable when `T ≠ S`); callers route
  * through the generic [[Optic]] extensions when the evidence is available.
  */
class SplitCombineLens[S, T, A, B, XA](
    read: S => A,
    val split: S => (XA, A),
    val combine: (XA, B) => T,
) extends PLens[S, T, A, B](read, (s, b) => combine(split(s)._1, b)):
  type X = XA
  def to(s: S): (XA, A) = split(s)
  def from(pair: (XA, B)): T = combine(pair._1, pair._2)

  /** Fused `SplitCombineLens.andThen(SplitCombineLens)` — pairs the two leftovers directly instead
    * of routing through the generic `AssociativeFunctor[Tuple2]`, so macro-derived lens chains
    * (`lens[Person](_.address).andThen(lens[Address](_.street))`) compose into another concrete
    * class: the composite keeps the fused read / write paths AND the capability mixins (CanGet /
    * CanModifyP / CanFold) that let it be passed as evidence directly. `inline` so each compose
    * site splices distinct lambdas per level — the same C2 recursive-inline-cap dodge as
    * [[GetReplaceLens.andThen]].
    */
  inline def andThen[C, D, XI](
      inner: SplitCombineLens[A, B, C, D, XI]
  ): SplitCombineLens[S, T, C, D, (XA, XI)] =
    new SplitCombineLens(
      read = s => inner.get(get(s)),
      split = s =>
        val (xa, a) = split(s)
        val (xi, c) = inner.split(a)
        ((xa, xi), c)
      ,
      combine = (x, d) => combine(x._1, inner.combine(x._2, d)),
    )

/** Monomorphic split-combine lens (`S = T`, `A = B`). The matched source / target lets the `to`
  * splitter double as the `T => (X, A)` evidence the mutation extensions need, so `place` /
  * `transfer` land on the class body directly. Used by [[Lens.first]], [[Lens.second]], and the
  * `eo-generics` `lens[S](_.field)` macro.
  */
final class SimpleLens[S, A, XA](
    get: S => A,
    split: S => (XA, A),
    combine: (XA, A) => S,
) extends SplitCombineLens[S, S, A, A, XA](get, split, combine):

  /** Overwrite the focus in an already-built `S` — the class-level twin of the generic
    * `Optic.place` extension, with the `T => (X, A)` evidence supplied by `split`.
    */
  inline def place(a: A): S => S =
    s =>
      val (x, _) = to(s)
      combine(x, a)

  /** Lift a `C => A` into a focus overwrite — the class-level twin of the generic `Optic.transfer`
    * extension.
    */
  inline def transfer[C](f: C => A): S => C => S =
    s =>
      c =>
        val (x, _) = to(s)
        combine(x, f(c))

/** Companion for [[SimpleLens]]. Hands out a `transformEvidence` given so the generic
  * `Optic.transform` / `.place` / `.transfer` extensions pick up the same behaviour as the
  * class-level methods.
  */
object SimpleLens:

  /** `split` already has the shape `S => (X, A)` the generic mutation extensions require. */
  given transformEvidence[S, A, XA](using
      o: SimpleLens[S, A, XA]
  ): (S => (o.X, A)) = o.split
