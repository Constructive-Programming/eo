package dev.constructive.eo
package optics

import cats.{Applicative, Functor, Monoid}

import data.Affine

/** Constructor for `Optional` — the conditionally-present single-focus optic, backed by `Affine`.
  * "An `Optional[S, A]`" is prose shorthand for `Optic[S, S, A, A, Affine]` — there is NO
  * two-parameter alias to ascribe; the concrete [[Optional]] class below always takes all four
  * parameters (`Optional[S, S, A, A]` for the monomorphic case). Ascribing the concrete class is
  * fine — that keeps code on the hot, fused paths; what would knock it off is ascribing the generic
  * `Optic[…]`, which drops the fused compose overloads and capability mixins for generic dispatch.
  * [[Optional.apply]] returns that concrete class, so leave `val`s un-ascribed (or name the
  * four-parameter class) and let consuming signatures demand a capability (`CanGetOption[S, A]`,
  * `CanModify[S, A]`, …) instead. An optional encodes a field that may or may not be there.
  * Composes freely with `Lens` via cross-carrier `.andThen` (auto-morphs through
  * `Composer[Tuple2, Affine]`).
  *
  * '''Miss semantics (normative).''' A write through a missed optional is a silent identity
  * pass-through: `modify(f)(s)` and `replace(b)(s)` return `s` unchanged (`f` is never invoked),
  * and `foldMap` folds zero foci (returns `Monoid.empty`). No error, no signal. When hit-ness
  * matters, probe with `.getOption` first.
  */
object Optional:

  /** Construct an Optional from `getOrModify` (`Right(a)` on hit, `Left(t)` on miss) and
    * `reverseGet: (S, B) => T`.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int, name: String)
    * val adultAge = Optional[Person, Person, Int, Int](
    *   getOrModify = p => Either.cond(p.age >= 18, p.age, p),
    *   reverseGet  = { case (p, a) => p.copy(age = a) },
    * )
    *   }}}
    */
  def apply[S, T, A, B](
      getOrModify: S => Either[T, A],
      reverseGet: ((S, B)) => T,
  ): Optional[S, T, A, B] =
    new Optional.FromFunctions[S, T, A, B](getOrModify, (s, b) => reverseGet((s, b)))

  /** Read-only Optional — `T = Unit` rules out `.modify` / `.replace`. Delegates to
    * [[AffineFold.apply]]; see [[PickFold]] for the specialised `X = (Unit, Unit)` shape and its
    * per-hit allocation savings.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int)
    * val adultAge = Optional.readOnly(p => Option.when(p.age >= 18)(p.age))
    *   }}}
    */
  def readOnly[S, A](matches: S => Option[A]): PickFold[S, A] =
    AffineFold(matches)

  /** Filtering read-only Optional — mirror of [[Fold.select]] over a single focus.
    *
    * @group Constructors
    */
  def selectReadOnly[A](p: A => Boolean): PickFold[A, A] =
    AffineFold.select(p)

  /** [[Optional.apply]]'s optic: the user's `getOrModify` and `reverseGet`, with `M = S`. */
  final private[optics] class FromFunctions[S, T, A, B](
      read: S => Either[T, A],
      write: (S, B) => T,
  ) extends Optional[S, T, A, B]:
    type M = S

    def getOrModify(s: S): Either[T, A] = read(s)

    def to(s: S): Affine[X, A] =
      read(s) match
        case Right(a) => new Affine.Hit[X, A](s, a)
        case Left(t)  => new Affine.Miss[X](t)

    def rebuild(m: S, b: B): T = write(m, b)

    override def reverseGet(s: S, b: B): T = write(s, b)

    override def modifyAt(s: S, f: A => B): T =
      read(s) match
        case Right(a) => write(s, f(a))
        case Left(t)  => t

  /** A fused composition's optic: each operation supplied by the composing overload, every one of
    * them tearing the source once (`modify` as the two-argument [[Optional.modifyAt]] kernel).
    */
  final private[optics] class Fused[S, T, A, B, M0](
      read: S => Either[T, A],
      write: (S, A => B) => T,
      keep: S => Affine[(T, M0), A],
      restore: (M0, B) => T,
  ) extends Optional[S, T, A, B]:
    type M = M0
    def getOrModify(s: S): Either[T, A] = read(s)
    def to(s: S): Affine[X, A] = keep(s)
    def rebuild(m: M0, b: B): T = restore(m, b)
    override def modifyAt(s: S, f: A => B): T = write(s, f)

  /** An inner read's miss leftover `b`, lifted into the outer's `T` by `back`; a hit passes
    * through. The read side of every fused composition whose inner may miss. `inline` with an
    * `inline` `back`, so each call site expands to the hand-written match.
    */
  private[optics] inline def liftMiss[B, T, C](e: Either[B, C])(inline back: B => T): Either[T, C] =
    e match
      case Left(b)      => Left(back(b))
      case r @ Right(_) => r.widenLeft[T]

  /** An inner Optional's carrier read at `a`: a miss's leftover lifted into the outer's `T` by
    * `back`, a hit's intermediate kept as `keep(m)` — the `to` of every fused composition with an
    * inner Optional.
    */
  private[optics] inline def liftTo[A, B, C, D, T, M](inner: Optional[A, B, C, D], a: A)(
      inline back: B => T,
      inline keep: inner.M => M,
  ): Affine[(T, M), C] =
    inner.to(a) match
      case mi: Affine.Miss[inner.X]   => new Affine.Miss(back(mi.fst))
      case hi: Affine.Hit[inner.X, C] => new Affine.Hit(keep(hi.snd), hi.b)

  /** Fused `outer ∘ inner` for a total outer (`read`, and `back` to write a `B` into an `S`) over
    * an inner Optional: a hit keeps `keep(s, m)`, which `put` turns back into the outer's write.
    * The shared body of `Lens.andThen(Optional)` and `Iso.andThen(Optional)`, which differ only in
    * whether the write needs the source.
    */
  private[optics] inline def underTotal[S, T, A, B, C, D, M](inner: Optional[A, B, C, D])(
      inline read: S => A,
      inline back: (S, B) => T,
      inline keep: (S, inner.M) => M,
      inline put: (M, D) => T,
  ): Optional[S, T, C, D] =
    fused(
      getOrModify = s => liftMiss(inner.getOrModify(read(s)))(back(s, _)),
      modify = (s, f) => back(s, inner.modifyAt(read(s), f)),
      to = s => liftTo(inner, read(s))(back(s, _), keep(s, _)),
      rebuild = (m, d) => put(m, d),
    )

  /** Build a fused composition from its four single-tear operations (see [[Fused]]). */
  private[optics] def fused[S, T, A, B, M0](
      getOrModify: S => Either[T, A],
      modify: (S, A => B) => T,
      to: S => Affine[(T, M0), A],
      rebuild: (M0, B) => T,
  ): Optional[S, T, A, B] =
    new Fused(getOrModify, modify, to, rebuild)

/** The conditionally-present single-focus optic over the `Affine` carrier. Every operation tears
  * the source ONCE: [[getOrModify]] / [[getOption]] / [[foldMap]] read without building anything a
  * write would need, [[modify]] / `replace` tear once and rebuild from what they tore, and the
  * carrier pair [[to]] / [[from]] keeps what the read computed — the existential intermediate [[M]]
  * — so a generic write through `to` / `from` never tears `S` a second time either.
  *
  * Two implementations, both private: [[Optional.apply]] (hand-written `getOrModify` +
  * `reverseGet`; `M = S`, the source is all the user's `reverseGet` needs) and the fused
  * compositions (`prism.andThen(lens)`, `optional.andThen(optional)`, …), whose `M` is whatever the
  * outer step already produced — a prism ∘ lens keeps the decoded domain value, so writing back
  * re-uses it instead of decoding again, and a nested chain keeps a tuple of its levels'
  * intermediates.
  *
  * Fused `.andThen` overloads skip the generic `AssociativeFunctor[Affine]` round-trip whenever the
  * other side is a known concrete class.
  */
sealed abstract class Optional[S, T, A, B]
    extends SingleFocus[S, T, A, B, Affine, Applicative],
      CanGetOption[S, A]:

  /** What a hit keeps for the write: the part of the read the rebuild needs. */
  type M
  type X = (T, M)

  /** The read: `Right(a)` on a hit, `Left(t)` (the pass-through `T`) on a miss. */
  def getOrModify(s: S): Either[T, A]

  /** The carrier read: a hit keeps its intermediate `m` beside the focus, for [[rebuild]]. */
  def to(s: S): Affine[X, A]

  /** Write `b` back from what a hit kept — never re-reads `S`. */
  def rebuild(m: M, b: B): T

  /** One tear, then rebuild: the write behind [[modify]] / `replace`, taking the source directly so
    * a `modify(f)(s)` call site allocates no `S => T` closure (`modify` is `inline`). Fused
    * compositions override it with a kernel that builds no carrier at all.
    */
  def modifyAt(s: S, f: A => B): T =
    to(s) match
      case h: Affine.Hit[X, A] => rebuild(h.snd, f(h.b))
      case m: Affine.Miss[X]   => m.fst

  /** `inline`, like `GetReplaceLens.modify`: `optional.modify(f)(s)` reduces to `modifyAt(s, f)`.
    */
  inline def modify(f: A => B): S => T = s => modifyAt(s, f)

  // CPD-OFF — one `CanModifyFP` contract, a fused body per carrier: the signature repeats in every
  // concrete optic that implements it.
  /** Fused effectful modify: one `to`, a hit maps the rebuild over the effect, a miss is `pure`. */
  def modifyF[G[_]](f: A => G[B])(using G: Applicative[G] & Functor[G]): S => G[T] =
    s =>
      to(s) match
        case h: Affine.Hit[X, A] => G.map(f(h.b))(b => rebuild(h.snd, b))
        case m: Affine.Miss[X]   => G.pure(m.fst)
  // CPD-ON

  override inline def replace(b: B): S => T = s => modifyAt(s, _ => b)

  final def from(a: Affine[X, B]): T =
    a match
      case h: Affine.Hit[X, B] => rebuild(h.snd, h.b)
      case m: Affine.Miss[X]   => m.fst

  /** Write `b` back into `s`: one tear, then [[rebuild]]; a miss passes `s`'s `T` through.
    * [[Optional.apply]]'s optics call the user's `reverseGet` directly instead.
    */
  def reverseGet(s: S, b: B): T =
    to(s) match
      case h: Affine.Hit[X, A] => rebuild(h.snd, b)
      case m: Affine.Miss[X]   => m.fst

  def getOption(s: S): Option[A] = getOrModify(s).toOption

  override def foldMap[F](f: A => F)(s: S)(using F: Monoid[F]): F =
    getOrModify(s) match
      case Right(a) => f(a)
      case Left(_)  => F.empty

  // CPD-OFF — the fused `andThen` matrix: one overload per inner optic, each a carrier-specific
  // kernel. Measured against the generic `AssociativeFunctor` / `Morph` route (AndThenBench, see the
  // 2.0 CHANGELOG): never more B/op per call, and only they keep a chain in concrete classes — the
  // generic route allocates per level (lens∘lens depth 6: 528 vs 120 B/op). The signatures and
  // the `Optional.fused(…)` shape repeat across them by construction.
  /** Fused `Optional.andThen(Optional)` — two partial focuses compose into another. An inner miss
    * lifts the inner's leftover back through the outer's [[rebuild]] (the outer's own intermediate,
    * not a re-read). `inline` so a deep `optional.andThen(optional)…` chain splices distinct
    * lambdas per level, under C2's recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s =>
        to(s) match
          case m: Affine.Miss[X]   => Left(m.fst)
          case h: Affine.Hit[X, A] =>
            Optional.liftMiss(inner.getOrModify(h.b))(b => rebuild(h.snd, b)),
      modify = (s, f) => modifyAt(s, a => inner.modifyAt(a, f)),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] =>
            Optional.liftTo(inner, h.b)(rebuild(h.snd, _), (h.snd, _)),
      rebuild = (m, d) => rebuild(m._1, inner.rebuild(m._2, d)),
    )

  /** Fused `Optional.andThen(Lens)` — always-present inner inside a partial outer. Stays partial;
    * the write keeps the outer's intermediate and the outer focus the lens writes into.
    */
  def andThen[C, D](inner: PLens[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s => getOrModify(s).map(inner.get),
      modify = (s, f) => modifyAt(s, a => inner.enplace(a, f(inner.get(a)))),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] => new Affine.Hit((h.snd, h.b), inner.get(h.b)),
      rebuild = (m, d) => rebuild(m._1, inner.enplace(m._2, d)),
    )

  /** Fused `Optional.andThen(Prism)` — outer Optional's hit fed into a Prism; an inner miss lifts
    * the prism's leftover through the outer's [[rebuild]].
    */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s =>
        to(s) match
          case m: Affine.Miss[X]   => Left(m.fst)
          case h: Affine.Hit[X, A] =>
            Optional.liftMiss(inner.tear(h.b))(b => rebuild(h.snd, b)),
      modify = (s, f) => modifyAt(s, inner.modify(f)),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.tear(h.b) match
              case Left(b)  => new Affine.Miss(rebuild(h.snd, b))
              case Right(c) => new Affine.Hit(h.snd, c),
      rebuild = (m, d) => rebuild(m, inner.mend(d)),
    )

  /** Fused `Optional.andThen(PickMendPrism)` — the untyped-tree kits' shape (`field("x")` then a
    * scalar prism such as `str`). Like the `MendTearPrism` overload, but the inner reads through
    * its `Option`-shaped `pick`, so no inner `Either` is built; an inner miss writes the outer
    * focus back through the outer's [[rebuild]].
    */
  def andThen[C, D](inner: PickMendPrism[A, C, D])(using ev: A =:= B): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s =>
        to(s) match
          case m: Affine.Miss[X]   => Left(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.pick(h.b) match
              case Some(c) => Right(c)
              case None    => Left(rebuild(h.snd, ev(h.b))),
      modify = (s, f) =>
        modifyAt(
          s,
          a =>
            inner.pick(a) match
              case Some(c) => ev(inner.mend(f(c)))
              case None    => ev(a),
        ),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.pick(h.b) match
              case Some(c) => new Affine.Hit(h.snd, c)
              case None    => new Affine.Miss(rebuild(h.snd, ev(h.b))),
      rebuild = (m, d) => rebuild(m, ev(inner.mend(d))),
    )

  /** Fused `Optional.andThen(Iso)` — iso is transparent; threads through the Optional's shape. */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused(
      getOrModify = s => getOrModify(s).map(inner.get),
      modify = (s, f) => modifyAt(s, a => inner.reverseGet(f(inner.get(a)))),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] => new Affine.Hit(h.snd, inner.get(h.b)),
      rebuild = (m, d) => rebuild(m, inner.reverseGet(d)),
    )

  // CPD-ON
