package eo
package optics

/** Constructors and stdlib instances for `Lens` — the always-present single-focus optic, backed by
  * the `Tuple2` carrier.
  *
  * A `Lens[S, A]` (short for `Optic[S, S, A, A, Tuple2]`) encodes a field of a product type:
  * `get(s): A` reads the field, `modify` / `replace` rewrite it. The `Tuple2` carrier stores the
  * leftover structure of `S` alongside the focus so rebuilding is cheap.
  *
  * For derived lenses the companion `eo-generics` module exposes `lens[S](_.field)` which writes
  * both the `get` and the `replace` half for you.
  */
object Lens:
  import Function.uncurried

  /** Witness that a `(A, B)` can be flipped to `(B, A)` — used by `Composer` bridges to swap
    * `Tuple2`'s sides when threading Lens-based chains through non-`Tuple2` carriers.
    *
    * @group Instances
    */
  given tupleInterchangeable[A, B]: (((A, B)) => (B, A)) with
    def apply(t: (A, B)): (B, A) = t.swap

  /** Polymorphic constructor — allows `S` and `T` to differ, i.e. genuine type change on write.
    *
    * @group Constructors
    * @tparam S
    *   source type being read
    * @tparam T
    *   result type after the write
    * @tparam A
    *   focus being read out of `S`
    * @tparam B
    *   focus being written back to produce `T`
    */
  def pLens[S, T, A, B](get: S => A, enplace: (S, B) => T) =
    GetReplaceLens(get, enplace)

  /** Monomorphic constructor — the common case where `S = T` and `A = B`. Use this for plain field
    * access; [[pLens]] for type-changing writes.
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

  /** Curried variant of [[apply]] — accepts `replace: A => S => S` instead of `(S, A) => S`. Easier
    * to reuse when an existing `replace` function is already in the user's form.
    *
    * @group Constructors
    */
  def curried[S, A](get: S => A, replace: A => S => S) =
    pLens[S, S, A, A](get, (s, a) => replace(a)(s))

  /** Polymorphic counterpart to [[curried]] — accepts a curried `replace: B => S => T`.
    *
    * @group Constructors
    */
  def pCurried[S, T, A, B](get: S => A, replace: B => S => T) =
    pLens(get, uncurried(replace))

  /** Lens focusing the first element of a `Tuple2`. Returns a [[SimpleLens]] so `place` /
    * `transfer` / `transform` all land without extra evidence.
    *
    * @group Constructors
    */
  // first / second produce a `SimpleLens` so `transform` / `place` /
  // `transfer` land for free — `S = T` and `A = B` in both cases, so
  // the `to` splitter doubles as the `T => (X, A)` evidence that the
  // mutation extensions need.
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

/** Concrete Optic subclass that stores `get` and `replace` directly, enabling fused extensions on
  * [[Optic]] that bypass the Tuple2 carrier entirely: `s => _replace(s, f(_get(s)))`.
  *
  * Returned by [[Lens.apply]] / [[Lens.pLens]] / [[Lens.curried]] / [[Lens.pCurried]] so
  * hand-written lenses with opaque complement types still benefit from the fused hot path.
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

  /** Fused composition — `GetReplaceLens.andThen(GetReplaceLens)` collapses into another
    * `GetReplaceLens` with `get = inner.get ∘ outer.get` and an enplace that threads the inner
    * update through the outer. Skips the generic `AssociativeFunctor[Tuple2]` composeTo /
    * composeFrom path entirely — no Tuple2 pairings, no existential nesting, no carrier round-trip.
    *
    * Scala's overload resolution picks this concrete-typed overload over the inherited
    * `Optic.andThen` whenever both sides are known to be `GetReplaceLens` at the call site (true
    * for `Lens.apply` results — which preserves the concrete type through inference). When the
    * inner side is a different concrete subclass or a generic `Optic[…, Tuple2]`, the inherited
    * method fires and the result uses the generic carrier path. Either way the user-facing
    * behaviour is unchanged; fusion is a runtime-only acceleration.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      get = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.enplace(get(s), d)),
    )

  /** Fused `GetReplaceLens.andThen(BijectionIso)` — collapses the iso into the lens's shape,
    * producing another `GetReplaceLens`. Skips the cross-carrier `Composer[Forgetful, Tuple2]` hop
    * that the generic path would take.
    */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      get = s => inner.get(get(s)),
      enplace = (s, d) => enplace(s, inner.reverseGet(d)),
    )

  /** Fused `GetReplaceLens.andThen(MendTearPrism)` — always-present outer field focused further
    * through a partial prism. Result is an `Optional`: the outer always hits, but the inner may
    * miss. Skips cross-carrier `Morph.bothViaAffine`.
    */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    new Optional(
      getOrModify = s =>
        inner.tear(get(s)) match
          case Left(b)  => Left(enplace(s, b))
          case Right(c) => Right(c),
      reverseGet = (s, d) => enplace(s, inner.mend(d)),
    )

  /** Fused `GetReplaceLens.andThen(PickMendPrism)` — same shape as `andThen(MendTearPrism)` but the
    * inner uses the Option-fast-path PickMend shape. Monomorphic inner (A = B on the prism)
    * required so types close.
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

  /** Fused `GetReplaceLens.andThen(Optional)` — lens focuses always-present inner, then Optional
    * may miss on the inner. Result is an `Optional`.
    */
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

/** Polymorphic split-combine lens. Encodes a lens as a splitter (`S => (XA, A)`) plus a combiner
  * (`(XA, B) => T`), with the structural complement surfaced as the existential `X = XA`.
  *
  * Supports genuine type change on the write path. Does NOT ship `place` / `transfer` — those would
  * require a `T => (XA, B)` evidence that is not recoverable from `to` / `combine` alone when `T`
  * is genuinely a different type from `S`. Callers who have that evidence can still reach `place` /
  * `transfer` through the generic extensions in [[Optic]].
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

/** Monomorphic split-combine lens — the common case (`S = T`, `A = B`). Extends
  * [[SplitCombineLens]] and adds `place` / `transfer` directly on the class body: because the
  * source and target types match, the `to` splitter is pointwise equal to the complement that the
  * mutation extensions need. No extra constructor parameter; no evidence plumbing at the call site.
  *
  * Used by [[Lens.first]], [[Lens.second]], and the `eo-generics` `lens[S](_.field)` macro — all of
  * which know the complement type structurally (the sibling tuple slot, or the remaining case-class
  * fields).
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

object SimpleLens:

  /** `to` already has the shape the generic `Optic.transform` / `place` extensions need (`S => (X,
    * A)`), so we hand it out as the evidence. Lets callers that route through the generic
    * extensions pick up the same behaviour as the class-level methods.
    */
  given transformEvidence[S, A, XA](using
      o: SimpleLens[S, A, XA]
  ): (S => (o.X, A)) = o.to
