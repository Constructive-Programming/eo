package dev.constructive.eo
package optics

import cats.Monoid

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

    override def modify(f: A => B): S => T =
      s =>
        read(s) match
          case Right(a) => write(s, f(a))
          case Left(t)  => t

  /** A fused composition's optic: each operation supplied by the composing overload, every one of
    * them tearing the source once.
    */
  final private[optics] class Fused[S, T, A, B, M0](
      read: S => Either[T, A],
      write: (A => B) => S => T,
      keep: S => Affine[(T, M0), A],
      restore: (M0, B) => T,
  ) extends Optional[S, T, A, B]:
    type M = M0
    def getOrModify(s: S): Either[T, A] = read(s)
    def to(s: S): Affine[X, A] = keep(s)
    def rebuild(m: M0, b: B): T = restore(m, b)
    override def modify(f: A => B): S => T = write(f)

  /** Build a fused composition from its four single-tear operations (see [[Fused]]). */
  private[optics] def fused[S, T, A, B, M0](
      getOrModify: S => Either[T, A],
      modify: (A => B) => S => T,
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
    extends Optic[S, T, A, B, Affine],
      CanGetOption[S, A],
      CanModifyP[S, T, A, B],
      CanFold[S, A]:

  /** What a hit keeps for the write: the part of the read the rebuild needs. */
  type M
  type X = (T, M)

  /** The read: `Right(a)` on a hit, `Left(t)` (the pass-through `T`) on a miss. */
  def getOrModify(s: S): Either[T, A]

  /** The carrier read: a hit keeps its intermediate `m` beside the focus, for [[rebuild]]. */
  def to(s: S): Affine[X, A]

  /** Write `b` back from what a hit kept — never re-reads `S`. */
  def rebuild(m: M, b: B): T

  /** One tear, then rebuild. Fused compositions override it with a kernel that allocates no carrier
    * at all (`outer.modify(inner.modify(f))`).
    */
  def modify(f: A => B): S => T =
    s =>
      to(s) match
        case h: Affine.Hit[X, A] => rebuild(h.snd, f(h.b))
        case m: Affine.Miss[X]   => m.fst

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

  def foldMap[F](f: A => F)(s: S)(using F: Monoid[F]): F =
    getOrModify(s) match
      case Right(a) => f(a)
      case Left(_)  => F.empty

  /** Fused `Optional.andThen(Optional)` — two partial focuses compose into another. An inner miss
    * lifts the inner's leftover back through the outer's [[rebuild]] (the outer's own intermediate,
    * not a re-read). `inline` so a deep `optional.andThen(optional)…` chain splices distinct
    * lambdas per level, under C2's recursive-inline cap (see [[Getter.andThen]]).
    */
  inline def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused[S, T, C, D, (M, inner.M)](
      getOrModify = s =>
        to(s) match
          case m: Affine.Miss[X]   => Left(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.getOrModify(h.b) match
              case Left(b)      => Left(rebuild(h.snd, b))
              case r @ Right(_) => r.widenLeft[T],
      modify = f => modify(inner.modify(f)),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.to(h.b) match
              case mi: Affine.Miss[inner.X]   => new Affine.Miss(rebuild(h.snd, mi.fst))
              case hi: Affine.Hit[inner.X, C] => new Affine.Hit((h.snd, hi.snd), hi.b),
      rebuild = (m, d) => rebuild(m._1, inner.rebuild(m._2, d)),
    )

  /** Fused `Optional.andThen(Lens)` — always-present inner inside a partial outer. Stays partial;
    * the write keeps the outer's intermediate and the outer focus the lens writes into.
    */
  def andThen[C, D](inner: GetReplaceLens[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused[S, T, C, D, (M, A)](
      getOrModify = s => getOrModify(s).map(inner.get),
      modify = f => modify(a => inner.enplace(a, f(inner.get(a)))),
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
    Optional.fused[S, T, C, D, M](
      getOrModify = s =>
        to(s) match
          case m: Affine.Miss[X]   => Left(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.tear(h.b) match
              case Left(b)      => Left(rebuild(h.snd, b))
              case r @ Right(_) => r.widenLeft[T],
      modify = f => modify(inner.modify(f)),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] =>
            inner.tear(h.b) match
              case Left(b)  => new Affine.Miss(rebuild(h.snd, b))
              case Right(c) => new Affine.Hit(h.snd, c),
      rebuild = (m, d) => rebuild(m, inner.mend(d)),
    )

  /** Fused `Optional.andThen(Iso)` — iso is transparent; threads through the Optional's shape. */
  def andThen[C, D](inner: BijectionIso[A, B, C, D]): Optional[S, T, C, D] =
    Optional.fused[S, T, C, D, M](
      getOrModify = s => getOrModify(s).map(inner.get),
      modify = f => modify(a => inner.reverseGet(f(inner.get(a)))),
      to = s =>
        to(s) match
          case m: Affine.Miss[X]   => new Affine.Miss(m.fst)
          case h: Affine.Hit[X, A] => new Affine.Hit(h.snd, inner.get(h.b)),
      rebuild = (m, d) => rebuild(m, inner.reverseGet(d)),
    )
