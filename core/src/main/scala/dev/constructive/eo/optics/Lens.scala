package dev.constructive.eo
package optics

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
    val get: S => A,
    val enplace: (S, B) => T,
) extends Optic[S, T, A, B, Tuple2]:
  type X = S
  val to: S => (S, A) = s => (s, get(s))
  val from: ((S, B)) => T = pair => enplace(pair._1, pair._2)

  inline def replace: B => S => T = b => s => enplace(s, b)

  inline def modify(f: A => B): S => T =
    s => enplace(s, f(get(s)))

  /** Fused `Lens.andThen(Lens)` — collapses into another `GetReplaceLens` directly on `get` /
    * `enplace`, skipping the generic `AssociativeFunctor[Tuple2]` round-trip. Scala's overload
    * resolution picks this when both sides are statically `GetReplaceLens`; for mixed-shape
    * composition the inherited generic `Optic.andThen` fires.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      get = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.enplace(get(s), d)),
    )

  /** Fused `Lens.andThen(Iso)` — collapses the iso into the lens's shape; skips the
    * `Composer[Forgetful, Tuple2]` hop.
    */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      get = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.reverseGet(d)),
    )

  /** Fused `Lens.andThen(Prism)` — outer always hits, inner may miss. Result is `Optional`; skips
    * cross-carrier `Morph.bothViaAffine`.
    */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.tear(get(s)) match
          case Left(b)  => Left(enplace(s, b))
          case Right(c) => Right(c),
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
          case Left(b)  => Left(enplace(s, b))
          case Right(c) => Right(c),
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
    val get: S => A,
    val to: S => (XA, A),
    val combine: (XA, B) => T,
) extends Optic[S, T, A, B, Tuple2]:
  type X = XA
  val from: ((XA, B)) => T = pair => combine(pair._1, pair._2)

  inline def modify(f: A => B): S => T =
    s =>
      val (x, a) = to(s)
      combine(x, f(a))

  inline def replace(b: B): S => T =
    s =>
      val (x, _) = to(s)
      combine(x, b)

/** Monomorphic split-combine lens (`S = T`, `A = B`). The matched source / target lets the `to`
  * splitter double as the `T => (X, A)` evidence the mutation extensions need, so `place` /
  * `transfer` land on the class body directly. Used by [[Lens.first]], [[Lens.second]], and the
  * `eo-generics` `lens[S](_.field)` macro.
  */
final class SimpleLens[S, A, XA](
    get: S => A,
    to: S => (XA, A),
    combine: (XA, A) => S,
) extends SplitCombineLens[S, S, A, A, XA](get, to, combine):

  inline def place(a: A): S => S =
    s =>
      val (x, _) = to(s)
      combine(x, a)

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

  /** `to` already has the shape `S => (X, A)` the generic mutation extensions require. */
  given transformEvidence[S, A, XA](using
      o: SimpleLens[S, A, XA]
  ): (S => (o.X, A)) = o.to
