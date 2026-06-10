package dev.constructive.eo
package optics

import scala.compiletime.summonInline

import cats.arrow.Profunctor
import cats.syntax.functor.*
import cats.{Applicative, Functor, Monoid}

import data.*

/** Existential encoding of a profunctor optic — the single trait behind every optic family in
  * `cats-eo`. The optic is a pair of functions `(S => F[X, A], F[X, B] => T)` over a carrier
  * `F[_, _]` and an existential `X` that threads the leftover information needed to rebuild `T`.
  * Each family picks a different carrier (`Tuple2` for Lens, `Either` for Prism, `Affine` for
  * Optional, …). Operations live in the companion as capability-gated extensions; each extension's
  * `(using …)` clause names the typeclass on `F` that unlocks it.
  *
  * @tparam S
  *   source type being observed / modified
  * @tparam T
  *   result type after modification (often `= S`)
  * @tparam A
  *   focus read out of `S`
  * @tparam B
  *   focus written back to produce `T` (often `= A`)
  * @tparam F
  *   two-argument carrier; capabilities scale with the typeclasses `F` admits.
  *
  * @see
  *   [[Lens]], [[Prism]], [[Iso]], [[Optional]], [[Setter]], [[Traversal]], [[Getter]], [[Fold]]
  */
trait Optic[S, T, A, B, F[_, _]]:
  self =>

  /** Existential leftover carried alongside the focus — the type-level witness the carrier uses to
    * rebuild `T`. Concrete at construction (`Lens.apply` sets `X = S`, `Prism.apply` sets `X = S`,
    * …) and abstract when the optic is bound to `Optic[…, F]` without refinement.
    */
  type X

  /** Push the source `S` into the carrier, extracting the focus `A` and packing the leftover `X`.
    * Paired with [[from]] to reconstruct `T`.
    */
  def to(s: S): F[X, A]

  /** Close the carrier: given a modified focus `B` (and the leftover `X` already inside the `F`),
    * reassemble the result `T`.
    */
  def from(b: F[X, B]): T

  /** Compose with another optic under the shared carrier `F`. Requires `AssociativeFunctor[F]`.
    * Cross-carrier composition (Lens → Optional, Lens → Traversal, …) goes through the
    * `Morph`-summoning overload of this same method.
    *
    * @example
    *   {{{
    * case class Address(street: String)
    * case class Person(address: Address)
    *
    * val streetLens = lens[Person](_.address).andThen(lens[Address](_.street))
    *   }}}
    */
  @annotation.nowarn("id=E197")
  inline def andThen[C, D](o: Optic[A, B, C, D, F]): Optic[S, T, C, D, F] =
    val af = summonInline[AssociativeFunctor[F, self.X, o.X]]
    new Optic:
      type X = af.Z
      def to(s: S): F[X, C] = af.composeTo(s, self, o)
      def from(xd: F[X, D]): T = af.composeFrom(xd, o, self)

  /** ANY outer ∘ read-only inner — the inner is honestly one-way (`T = Unit`: a Getter, AffineFold,
    * or Fold), so only the two READ sides matter and the composite collapses to the read-only join
    * of their strengths via [[ReadCompose]] (`Getter` / `PickFold` / `ForgetFold`).
    *
    * A trait *member* (not an extension in the companion) deliberately: once a receiver is
    * statically one of the fused concrete classes, its `andThen` member overloads enter resolution
    * and Scala 3 never falls back to extension methods when they all fail — the collapse must be in
    * the member overload set to be reachable without an expected-type ascription.
    *
    * Only the inner's `T` is pinned to `Unit`; its `B` stays free (`IB`) even though read-only
    * inners always have `B = Unit`. That keeps this overload strictly LESS specific than the
    * same-carrier `andThen` above (which accepts every argument this one does whenever `B = Unit`
    * at the receiver), so a same-carrier read-only ∘ read-only call resolves unambiguously to the
    * `AssociativeFunctor` path and this one fires exactly on the cross-seam cells the generic
    * member cannot type.
    */
  def andThen[C, IB, G[_, _]](inner: Optic[A, Unit, C, IB, G])(using
      rc: ReadCompose[F, G]
  ): rc.Out[S, C] =
    rc.compose(self, inner)

  /** Build-then-observe across the *build-output ⇄ read-input* seam, **preserving structure**, on a
    * **shared carrier** `F`. Flip `self` (it must be reversible — `Accessor[F]` *and*
    * `ReverseAccessor[F]`, i.e. an `Iso` or `Review` over `Direct`) so it reads `T` from `B`, then
    * `andThen` `that` under the same carrier. The result is the full `Optic[B, A, C, D, F]`, *not*
    * a collapsed getter: its read capability follows the carrier (`.get` for `Direct`), and
    * `self`'s read focus `A` survives as the composite's write-back focus.
    *
    * This is exactly `self.reverse.andThen(that)`. The motivating case is `ana.cross(cata)`: a
    * `Review` (the unfold) crossed with a getter on the built `S` (the fold) — a (materializing)
    * hylomorphism whose `.get` reads the folded value. When `that` sits on a *different* carrier (a
    * Prism, a `Fold`, …), the cross-carrier `cross` overload below is selected instead.
    *
    * Seam: `that`'s source is `self`'s `T` and its back-type is `self`'s `S`.
    *
    * @group Operations
    */
  inline def cross[C, D](
      o: Optic[T, S, C, D, F]
  )(using Accessor[F], ReverseAccessor[F]): Optic[B, A, C, D, F] =
    self.reverse.andThen(o)

/** Companion for [[Optic]]. Hosts the profunctor instances and the capability-gated extension
  * catalogue — `.get`, `.modify`, `.replace`, `.foldMap`, `.modifyA`, `.all`, `.reverseGet`,
  * `.getOption`, `.put`, `.transform`, `.place`, `.transfer`, `.andThen` (carrier-morphing plus the
  * read-only / AffineFold / Setter / Review collapses), `.readOnly`, `.cross`, `.morph`,
  * `.headOption`, `.length`, `.exists`. Adding a new carrier means supplying the typeclass
  * instances of the operations it should support.
  */
object Optic:

  /** The identity optic — `S` is its own focus and modification has no effect. Carrier is
    * [[data.Direct]] (no leftover). Returns the concrete [[BijectionIso]] (not an anonymous
    * `Optic`) so `id.andThen(x)` picks up the fused compose members like any other Iso.
    *
    * @group Constructors
    */
  def id[A]: BijectionIso[A, A, A, A] =
    Iso[A, A, A, A](identity, identity)

  /** Profunctor over `(S, T)` — `dimap` on the outer parameter pair, letting callers pre-compose
    * the source side and post-compose the result side of a fixed-focus optic.
    *
    * @group Instances
    */
  given outerProfunctor[A, B, F[_, _]]: Profunctor[[S, T] =>> Optic[S, T, A, B, F]] with

    def dimap[S, T, R, U](
        o: Optic[S, T, A, B, F]
    )(f: R => S)(g: T => U): Optic[R, U, A, B, F] =
      new Optic[R, U, A, B, F]:
        type X = o.X
        def to(r: R): F[X, A] = o.to(f(r))
        def from(fxb: F[X, B]): U = g(o.from(fxb))

  /** Profunctor over `(B, A)` — `dimap` on the inner focus pair. Requires a `ForgetfulFunctor[F]`
    * so the carrier can map its focus in place.
    *
    * @group Instances
    */
  given innerProfunctor[S, T, F[_, _]](using
      F: ForgetfulFunctor[F]
  ): Profunctor[[B, A] =>> Optic[S, T, A, B, F]] with

    def dimap[B, A, D, C](
        o: Optic[S, T, A, B, F]
    )(f: D => B)(g: A => C): Optic[S, T, C, D, F] =
      new Optic[S, T, C, D, F]:
        type X = o.X
        def to(s: S): F[X, C] = F.map(o.to(s), g)
        def from(xd: F[X, D]): T = o.from(F.map(xd, f))

  /** Cross-carrier `.andThen` — picks the direction via a summoned [[Morph]] when the two optics'
    * carriers differ. With one exception (`forget2multifocus` / `multifocus2forget`), cats-eo ships
    * no bidirectional Composer pairs, so at most one Morph applies per carrier pair. That one pair
    * can make a `Forget[F]` ⇄ `MultiFocus[F]` chain ambiguous; it's resolved via the explicit
    * `Composer[..].to(o)` form (see `multifocus2forget`).
    *
    * @example
    *   {{{
    * lens[Person](_.phones)
    *   .andThen(Traversal.each[ArraySeq, Phone])
    *   .andThen(lens[Phone](_.isMobile))
    *   }}}
    */
  extension [S, T, A, B, F[_, _]](self: Optic[S, T, A, B, F])

    @annotation.targetName("andThenMorphed")
    inline def andThen[G[_, _], C, D](o: Optic[A, B, C, D, G])(using
        m: Morph[F, G]
    ): Optic[S, T, C, D, m.Out] =
      m.morphSelf(self).andThen(m.morphO(o))

    /** Re-express this optic over a different carrier `G`. Package-private; users compose
      * cross-carrier via [[andThen]] above. Reachable for law / behaviour specs inside `eo.*`.
      */
    def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
      cf.to(self)

  extension [S, T, A, B, F[_, _]](
      self: Optic[S, T, A, B, F]
  )(using accessor: Accessor[F])
    inline def get(s: S): A = accessor.get(self.to(s))

    inline def andThen[C](o: Getter[A, C]): Getter[S, C] =
      Getter(s => o.get(get(s)))

    inline def readOnly: Getter[S, A] =
      Getter(get)

  extension [S, T, A, B, F[_, _]](
      self: Optic[S, T, A, B, F]
  )(using A: PartialAccessor[F])
    inline def getOption(s: S): Option[A] = A.getOption(self.to(s))

    inline def andThen[C](o: Getter[A, C]): PickFold[S, C] =
      PickFold(s => getOption(s).map(o.get))

  /** Build the "no context" reverse — takes a fresh `B` and produces the corresponding `T`.
    * Available when the carrier has a `ReverseAccessor[F]` instance (today: `Either` and `Direct`).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      self: Optic[S, T, A, B, F]
  )(using ra: ReverseAccessor[F])
    inline def reverseGet(b: B): T = self.from(ra.reverseGet(b))

    inline def andThen[D](o: Review[B, D]): Review[T, D] =
      Review(d => reverseGet(o.reverseGet(d)))

    inline def writeOnly: Review[T, B] =
      Review(reverseGet)

  /** Flip the direction of an optic — only defined when the carrier admits both `Accessor[F]` and
    * `ReverseAccessor[F]` (i.e. iso-shaped carriers).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      self: Optic[S, T, A, B, F]
  )(using A: Accessor[F], RA: ReverseAccessor[F])

    def reverse: Optic[B, A, T, S, F] =
      new Optic[B, A, T, S, F]:
        type X = self.X
        def to(b: B): F[X, T] = RA.reverseGet(self.from(RA.reverseGet(b)))
        def from(fs: F[X, S]): A = A.get(self.to(A.get(fs)))

    @annotation.targetName("crossMorphed")
    inline def cross[G[_, _], C, D](o: Optic[T, S, C, D, G])(using
        m: Morph[F, G]
    ): Optic[B, A, C, D, m.Out] =
      m.morphSelf(reverse).andThen(m.morphO(o))

  /** Modify (`A => B`) or replace (by constant `B`) the focus in-place. Available for every carrier
    * with a `ForgetfulFunctor[F]` instance — i.e. every current optic family.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      self: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F])

    inline def modify(f: A => B): S => T =
      s => self.from(FF.map(self.to(s), f))

    inline def replace(b: B): S => T =
      s => self.from(FF.map(self.to(s), _ => b))

    inline def andThen[C, D](o: Setter[A, B, C, D]): Setter[S, T, C, D] =
      Setter(f => modify(o.modify(f)))

  /** Overwrite a `T`-shaped value at the focus — available when the carrier can witness `T => F[X,
    * B]` (e.g. `Direct`, where `F[X, B] = B`). [[transfer]] lifts a `C => B` into this same shape
    * with an extra `C` argument.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F], ev: T => F[o.X, B])

    inline def place(b: B): T => T =
      t => o.from(FF.map(ev(t), _ => b))

    inline def transfer[C](f: C => B): T => C => T =
      t => c => place(f(c))(t)

  /** Generalised [[place]]: transform a `D` at the focus via `D => B` rather than replacing
    * unconditionally.
    *
    * @group Operations
    */
  extension [S, T, A, B, D, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F], ev: T => F[o.X, D])

    inline def transform(f: D => B): T => T =
      t => o.from(FF.map(ev(t), f))

  /** Construct a `T` directly from an `A`, running `f: A => B` at the focus — available for
    * carriers with a `ForgetfulApplicative[F]` instance (today: `Direct`).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulApplicative[F])

    inline def put(f: A => B): A => T =
      a => o.from(FF.pure[o.X, B](f(a)))

  /** Effectful modify over any `Functor[G]` — generalises [[modify]] to monadic / effectful `A =>
    * G[B]` transformations.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Functor])

    inline def modifyF[G[_]](f: A => G[B])(using G: Functor[G]): S => G[T] =
      s => FT.traverse(o.to(s), f)(using G).map(o.from)

  /** Effectful modify over any `Applicative[G]`; unlike [[modifyF]] this variant also exposes
    * [[all]], which collects every visited focus via `Applicative[List]`.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Applicative])

    inline def modifyA[G[_]](f: A => G[B])(using G: Applicative[G]): S => G[T] =
      s => FT.traverse(o.to(s), f)(using G).map(o.from)

    inline def all(s: S): List[F[o.X, A]] =
      FT.traverse(o.to(s), List(_))(using Applicative[List])

  /** `foldMap` over the focus — the primary consumption path for [[Fold]] and any other carrier
    * with a `ForgetfulFold[F]` instance. Combines every focus through `Monoid[M]`.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFold[F])

    inline def foldMap[M: Monoid](f: A => M): S => M =
      s => FF.foldMap(f, o.to(s))

    /** First focus visible through the optic, if any. Available on any carrier admitting
      * `ForgetfulFold[F]` — `Forget[F]` (Fold), `MultiFocus[F]` (Traversal), `Affine` (Optional /
      * AffineFold), `Either` (Prism), `Tuple2` (Lens). For 0-or-1-focus carriers this is the same
      * Option you'd get from `.getOption`; for multi-focus carriers it picks the first focus the
      * underlying `Foldable` enumerates.
      *
      * Implemented via `foldMap` under a custom "first-`Some`" `Monoid[Option[A]]` — the same shape
      * Monocle's `Fold.headOption` uses. The custom monoid short-circuits via `_.orElse(_)`, so on
      * a `Foldable[F]` whose `foldMap` is itself short-circuit-aware (e.g. `LazyList`) the walk
      * stops at the first focus.
      *
      * @group Operations
      */
    def headOption(s: S): Option[A] =
      o.foldMap[Option[A]](a => Some(a))(using
        Monoid.instance[Option[A]](None, (l, r) => l.orElse(r))
      )(s)

    /** Number of foci visible through the optic. O(n) in the focus count via `Foldable.foldMap`
      * under `Monoid[Int]`. Distinct from `Foldable.size` only in that it routes through the
      * carrier's `ForgetfulFold[F]` — same end result, available wherever `foldMap` is.
      *
      * @group Operations
      */
    def length(s: S): Int =
      o.foldMap[Int](_ => 1)(using cats.Monoid[Int])(s)

    /** True iff at least one focus satisfies `p`. Short-circuits on the first hit when the
      * carrier's `ForgetfulFold[F]` is short-circuit-aware. The default `Monoid[Boolean]` cats
      * exposes is conjunction (`&&`); we instantiate a disjunction monoid inline so a single match
      * can win without consulting the rest.
      *
      * @group Operations
      */
    def exists(p: A => Boolean)(s: S): Boolean =
      o.foldMap[Boolean](p)(using Monoid.instance[Boolean](false, _ || _))(s)
