package dev.constructive.eo
package optics

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
  def to: S => F[X, A]

  /** Close the carrier: given a modified focus `B` (and the leftover `X` already inside the `F`),
    * reassemble the result `T`.
    */
  def from: F[X, B] => T

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
      val to: S => F[X, C] = s => af.composeTo(s, outerRef, innerRef)
      val from: F[X, D] => T = xd => af.composeFrom(xd, innerRef, outerRef)

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
        val to: R => F[X, A] = r => o.to(f(r))
        val from: F[X, B] => U = fxb => g(o.from(fxb))

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
        val to: S => F[X, C] = s => F.map(o.to(s), g)
        val from: F[X, D] => T = d => o.from(F.map(d, f))

  /** The identity optic — `S` is its own focus and modification has no effect. Carrier is
    * [[data.Forgetful]] (no leftover).
    *
    * @group Constructors
    */
  def id[A]: Optic[A, A, A, A, Forgetful] =
    new Optic[A, A, A, A, Forgetful]:
      type X = Nothing
      val to: A => A = identity
      val from: A => A = identity

  /** Cross-carrier `.andThen` — picks the direction via a summoned [[Morph]] when the two optics'
    * carriers differ. cats-eo doesn't ship bidirectional composers, so at most one Morph applies
    * per carrier pair (no ambiguity).
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
        scala.compiletime.summonInline[AssociativeFunctor[m.Out, morphedSelf.X, morphedO.X]]
      )

    /** Re-express this optic over a different carrier `G`. Package-private; users compose
      * cross-carrier via [[andThen]] above. Reachable for law / behaviour specs inside `eo.*`.
      */
    private[eo] def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
      cf.to(self)

  // Capability-gated extensions follow. The `(using …)` clause names the typeclass each extension
  // requires; carriers without that instance simply don't see the method.

  /** Read the focus out of `s`. Available when the carrier has an `Accessor[F]` instance (today:
    * `Tuple2` and `Forgetful`).
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using A: Accessor[F]) inline def get(s: S): A = A.get(o.to(s))

  /** Build the "no context" reverse — takes a fresh `B` and produces the corresponding `T`.
    * Available when the carrier has a `ReverseAccessor[F]` instance (today: `Either` and
    * `Forgetful`).
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
        val to: B => F[X, T] = (b: B) => RA.reverseGet(o.from(RA.reverseGet(b)))
        val from: F[X, S] => A = (fs: F[X, S]) => A.get(o.to(A.get(fs)))

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
    * B]` (e.g. `Forgetful`, where `F[X, B] = B`). [[transfer]] lifts a `C => B` into this same
    * shape with an extra `C` argument.
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
    * carriers with a `ForgetfulApplicative[F]` instance (today: `Forgetful`).
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
      s => FT.traverse(using G)(o.to(s))(f).map(o.from)

  /** Effectful modify over any `Applicative[G]`; unlike [[modifyF]] this variant also exposes
    * [[all]], which collects every visited focus via `Applicative[List]`.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FT: ForgetfulTraverse[F, Applicative])

    inline def modifyA[G[_]](f: A => G[B])(using G: Applicative[G]): S => G[T] =
      s => FT.traverse(using G)(o.to(s))(f).map(o.from)

    inline def all(s: S): List[F[o.X, A]] =
      FT.traverse(using Applicative[List])(o.to(s))(List(_))

  /** `foldMap` over the focus — the primary consumption path for [[Fold]] and any other carrier
    * with a `ForgetfulFold[F]` instance. Combines every focus through `Monoid[M]`.
    *
    * @group Operations
    */
  extension [S, T, A, B, F[_, _]](
      o: Optic[S, T, A, B, F]
  )(using FF: ForgetfulFold[F])

    inline def foldMap[M: Monoid](f: A => M): S => M =
      s => FF.foldMap(using Monoid[M])(f)(o.to(s))

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
