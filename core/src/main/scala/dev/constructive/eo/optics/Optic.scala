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
  def andThen[C, D](o: Optic[A, B, C, D, F])(using
      af: AssociativeFunctor[F, self.X, o.X]
  ): Optic[S, T, C, D, F] =
    val outerRef = self.asInstanceOf[Optic[S, T, A, B, F] { type X = self.X }]
    val innerRef = o.asInstanceOf[Optic[A, B, C, D, F] { type X = o.X }]
    new Optic:
      type X = af.Z
      def to(s: S): F[X, C] = af.composeTo(s, outerRef, innerRef)
      def from(xd: F[X, D]): T = af.composeFrom(xd, innerRef, outerRef)

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
  inline def cross[C, D](o: Optic[T, S, C, D, F])(using
      A: Accessor[F],
      RA: ReverseAccessor[F],
  ): Optic[B, A, C, D, F] =
    val r = self.reverse
    r.andThen(o)(using summonInline[AssociativeFunctor[F, r.X, o.X]])

/** Companion for [[Optic]]. Hosts the profunctor instances and the capability-gated extension
  * catalogue — `.get`, `.modify`, `.replace`, `.foldMap`, `.modifyA`, `.all`, `.reverseGet`,
  * `.getOption`, `.put`, `.transform`, `.place`, `.transfer`, `.andThen`, `.morph`, `.headOption`,
  * `.length`, `.exists`. Adding a new carrier means supplying the typeclass instances of the
  * operations it should support.
  */
object Optic:

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

  /** The identity optic — `S` is its own focus and modification has no effect. Carrier is
    * [[data.Direct]] (no leftover).
    *
    * @group Constructors
    */
  def id[A]: Optic[A, A, A, A, Direct] =
    new Optic[A, A, A, A, Direct]:
      type X = Nothing
      def to(a: A): Direct[X, A] = Direct(a)
      def from(da: Direct[X, A]): A = da.value

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
      val morphedSelf = m.morphSelf(self)
      val morphedO = m.morphO(o)
      morphedSelf.andThen(morphedO)(using
        summonInline[AssociativeFunctor[m.Out, morphedSelf.X, morphedO.X]]
      )

    /** Compose with a read-only [[Getter]] → the **read-only collapse** of this optic, for a
      * **total reader** (`Accessor[F]` — `Iso`/`Getter` on `Direct`, `Lens` on `Tuple2`). A
      * `Getter`'s back-focus is `Unit`, so it can never thread through a writable `B` as a writable
      * optic; the only sound result is read-only, and that is exactly what's wanted ("focus `A`,
      * then read `C`"): read `self` through the accessor, then `o.get` — a [[DirectGetter]].
      * Partial readers take the `andThenAffineFold` overload below; together they give
      * `lens.andThen(getter) = Getter`, `optional/prism.andThen(getter) = AffineFold` — the
      * read-side dual of the `andThen(Setter)` write-side collapse. Routed through the carrier's
      * capability, not per-class fused overloads.
      *
      * @group Operations
      */
    @annotation.targetName("andThenReadOnly")
    inline def andThen[C](o: DirectGetter[A, C])(using af: Accessor[F]): DirectGetter[S, C] =
      Getter[S, C](s => o.get(af.get(self.to(s))))

    /** Compose with a read-only [[Getter]] → the **read-only collapse** of this optic, for a
      * **partial reader** (no `Accessor[F]` — `Optional`/[[AffineFold]] on `Affine`, `Prism` on
      * `Either`). Takes the [[readOnly]] projection (write side forgotten, so the `Unit` seam lines
      * up against the getter's `T = Unit`) and composes through `Morph[F, Direct]` pinned to the
      * `Affine` carrier (`rightToLeft` for `Affine` itself, `bothViaAffine` for `Either`) — an
      * [[AffineFold]]. The `NotGiven[Accessor[F]]` guard keeps total readers on the
      * `andThenReadOnly` overload above, unambiguously.
      *
      * @group Operations
      */
    @annotation.targetName("andThenAffineFold")
    inline def andThen[C](o: DirectGetter[A, C])(using
        ng: scala.util.NotGiven[Accessor[F]],
        m: Morph[F, Direct] { type Out[X, Y] = Affine[X, Y] },
    ): AffineFold[S, C] =
      val ms = m.morphSelf(self.readOnly)
      val mo = m.morphO(o)
      ms.andThen(mo)(using summonInline[AssociativeFunctor[Affine, ms.X, mo.X]])

    /** Compose with a write-only [[Setter]] → a concrete [[SetterOptic]] (the **write-side dual**
      * of `andThen(Getter)`). A `Setter`'s read side is trivial, so the composite is write-only: it
      * modifies `this` optic's focus through the inner setter. Routed through `Morph[F, SetterF]`
      * (every writable carrier ships a `Composer[·, SetterF]`), then re-wrapped as a `SetterOptic`
      * so the concrete fused `.modify` surface is preserved. So `lens.andThen(setter)` /
      * `optional.andThen(setter)` / `prism.andThen(setter)` are all `Setter`s.
      *
      * @group Operations
      */
    @annotation.targetName("andThenSetter")
    inline def andThen[C, D](o: SetterOptic[A, B, C, D])(using
        m: Morph[F, SetterF] { type Out[X, Y] = SetterF[X, Y] }
    ): SetterOptic[S, T, C, D] =
      val ms = m.morphSelf(self)
      val mo = m.morphO(o)
      val composed = ms.andThen(mo)(using summonInline[AssociativeFunctor[SetterF, ms.X, mo.X]])
      Setter[S, T, C, D](f => composed.modify(f))

    /** Compose with a build-only [[Review]] → a [[Review]] (the build-direction collapse). A
      * `Review`'s source/read is trivial (`Unit`), so the composite is build-only: build the inner
      * focus, then rebuild `T` through `self`. Requires `this` to be reversible
      * (`ReverseAccessor[F]`).
      *
      * @group Operations
      */
    @annotation.targetName("andThenReview")
    def andThen[C](o: Optic[Unit, B, Unit, C, F])(using RA: ReverseAccessor[F]): Review[T, C] =
      Review[T, C](c => self.from(RA.reverseGet(o.from(RA.reverseGet(c)))))

    /** The **read-only projection** of this optic — same `to`, write side forgotten (`T = B =
      * Unit`). Sound for every carrier (`from` ignores its input); this is the seam adapter the
      * partial-reader `andThen(Getter)` collapse composes through.
      *
      * @group Operations
      */
    def readOnly: Optic[S, Unit, A, Unit, F] =
      new Optic[S, Unit, A, Unit, F]:
        type X = self.X
        def to(s: S): F[X, A] = self.to(s)
        def from(b: F[X, Unit]): Unit = ()

    /** Cross-carrier [[cross]] (mirrors `andThen` vs `andThenMorphed`) — when `that` sits on a
      * *different* carrier `G`, a `Morph[F, G]` bridges the two before composing, so the result's
      * read capability follows the *composed* carrier: `.getOption` through a Prism, `.foldMap`
      * through a `Fold` (the latter via the `Composer[Direct, Forget]` bridge — read-many falls out
      * of this overload). Same seam and structure-preservation as the same-carrier `cross`.
      * Reversibility still excludes `Prism`-as-`self` (`Either` has no `Accessor`); build from a
      * Prism by wrapping its `mend` in a `Review` first.
      *
      * @group Operations
      */
    @annotation.targetName("crossMorphed")
    inline def cross[G[_, _], C, D](o: Optic[T, S, C, D, G])(using
        A: Accessor[F],
        RA: ReverseAccessor[F],
        m: Morph[F, G],
    ): Optic[B, A, C, D, m.Out] =
      val r = self.reverse
      val ms = m.morphSelf(r)
      val mo = m.morphO(o)
      ms.andThen(mo)(using summonInline[AssociativeFunctor[m.Out, ms.X, mo.X]])

    /** Re-express this optic over a different carrier `G`. Package-private; users compose
      * cross-carrier via [[andThen]] above. Reachable for law / behaviour specs inside `eo.*`.
      */
    def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
      cf.to(self)

  // Capability-gated extensions follow. The `(using …)` clause names the typeclass each extension
  // requires; carriers without that instance simply don't see the method.

  /** Read the focus out of `s`. Available when the carrier has an `Accessor[F]` instance (today:
    * `Tuple2` and `Direct`).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F]) inline def get(s: S): A = A.get(o.to(s))

  /** Build the "no context" reverse — takes a fresh `B` and produces the corresponding `T`.
    * Available when the carrier has a `ReverseAccessor[F]` instance (today: `Either` and `Direct`).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using RA: ReverseAccessor[F]) inline def reverseGet(b: B): T = o.from(RA.reverseGet(b))

  /** Flip the direction of an optic — only defined when the carrier admits both `Accessor[F]` and
    * `ReverseAccessor[F]` (i.e. iso-shaped carriers).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F], RA: ReverseAccessor[F])

    // Not `inline`: the body builds a fresh anonymous `Optic`; `inline` would duplicate that
    // class definition per call site (E197) without eliminating the allocation.
    def reverse: Optic[B, A, T, S, F] =
      new Optic[B, A, T, S, F]:
        type X = o.X
        def to(b: B): F[X, T] = RA.reverseGet(o.from(RA.reverseGet(b)))
        def from(fs: F[X, S]): A = A.get(o.to(A.get(fs)))

  /** Modify (`A => B`) or replace (by constant `B`) the focus in-place. Available for every carrier
    * with a `ForgetfulFunctor[F]` instance — i.e. every current optic family.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFunctor[F])

    inline def modify(f: A => B): S => T =
      s => o.from(FF.map(o.to(s), f))

    inline def replace(b: B): S => T =
      s => o.from(FF.map(o.to(s), _ => b))

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

  /** `getOption` over an `Affine`-carrier optic — the canonical read for [[Optional]] and
    * [[AffineFold]]. Pattern-matches the Affine directly; miss produces `None`, hit wraps the focus
    * in `Some` (no `Monoid[A]` needed, unlike [[foldMap]]).
    *
    * @group Operations
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, Affine])

    inline def getOption(s: S): Option[A] =
      o.to(s).fold(_ => None, (_, a) => Some(a))

  /** `getOption` over an `Either`-carrier optic — the canonical read for a [[Prism]]. Mirrors the
    * `Affine` overload above so a derived `prism` (which surfaces as the bare
    * `Optic[S, T, A, B, Either]`, not the concrete [[Prism]] subclass) reads through the same
    * `getOption` call. `Either#toOption` discards the residual `Left` (the miss branch); a hit's
    * `Right(a)` becomes `Some(a)`. The concrete `Prism`'s own `getOption` member still wins at its
    * static type (member over extension), so this only fills the gap for the erased carrier.
    *
    * @group Operations
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, Either])

    // `@targetName` disambiguates from the `Affine` overload above: the carrier type param erases,
    // so both `getOption`s collide at the JVM level. Call sites still resolve by carrier statically.
    @annotation.targetName("getOptionEither")
    inline def getOption(s: S): Option[A] =
      o.to(s).toOption
