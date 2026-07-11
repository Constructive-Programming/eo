package dev.constructive.eo
package optics

import cats.Monoid

/** Constructors for `Lens` — the always-present single-focus optic, backed by `Tuple2`. A
  * `Lens[S, A]` (short for `Optic[S, S, A, A, Tuple2]`) reads a field via `get(s)` and rewrites it
  * via `modify` / `replace`. The `eo-generics` module's `lens[S](_.field)` macro derives both.
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

/** Concrete Optic subclass storing `get` and `enplace` directly, enabling the fused-`andThen`
  * overloads below to bypass the `Tuple2` carrier entirely. Returned by every `Lens.*` constructor
  * so hand-written lenses pick up the fused hot path automatically.
  */
class GetReplaceLens[S, T, A, B](
    read: S => A,
    val enplace: (S, B) => T,
) extends Optic[S, T, A, B, Tuple2],
      CanGet[S, A],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = S
  def get(s: S): A = read(s)
  def to(s: S): (S, A) = (s, read(s))
  def from(pair: (S, B)): T = enplace(pair._1, pair._2)

  override inline def replace(b: B): S => T = s => enplace(s, b)

  inline def modify(f: A => B): S => T =
    s => enplace(s, f(get(s)))

  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = f(read(s))

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
  inline def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
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
    new Optional(
      getOrModify = s =>
        inner.tear(get(s)) match
          case Left(b)      => Left(enplace(s, b))
          case r @ Right(_) => r.widenLeft[T],
      reverseGet = (s, d) => enplace(s, inner.mend(d)),
    )

  /** Fused `Lens.andThen(Prism)` — Option-fast-path inner (PickMend). Mono inner required for type
    * closure.
    */
  def andThen[C, D](inner: PickMendPrism[A, C, D])(using
      ev: A =:= B
  ): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.pick(get(s)) match
          case Some(c) => Right(c)
          case None    => Left(enplace(s, ev(get(s)))),
      reverseGet = (s, d) => enplace(s, ev(inner.mend(d))),
    )

  /** Fused `Lens.andThen(Optional)` — outer always hits, inner may miss. Result is `Optional`. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.getOrModify(get(s)) match
          case Left(b)      => Left(enplace(s, b))
          case r @ Right(_) => r.widenLeft[T],
      reverseGet = (s, d) =>
        val newB = inner.reverseGet(get(s), d)
        enplace(s, newB),
    )

/** Polymorphic split-combine lens — splitter `S => (XA, A)` + combiner `(XA, B) => T`, surfacing
  * the complement as `X = XA`. Does not ship `place` / `transfer` (would require a `T => (XA, B)`
  * evidence that isn't recoverable when `T ≠ S`); callers route through the generic [[Optic]]
  * extensions when the evidence is available.
  */
class SplitCombineLens[S, T, A, B, XA](
    read: S => A,
    val split: S => (XA, A),
    val combine: (XA, B) => T,
) extends Optic[S, T, A, B, Tuple2],
      CanGet[S, A],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:
  type X = XA
  def get(s: S): A = read(s)
  def to(s: S): (XA, A) = split(s)
  def from(pair: (XA, B)): T = combine(pair._1, pair._2)

  inline def modify(f: A => B): S => T =
    s =>
      val (x, a) = to(s)
      combine(x, f(a))

  override inline def replace(b: B): S => T =
    s =>
      val (x, _) = to(s)
      combine(x, b)

  def foldMap[M](f: A => M)(s: S)(using Monoid[M]): M = f(read(s))

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
