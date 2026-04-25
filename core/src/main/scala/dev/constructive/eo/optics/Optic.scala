package dev.constructive.eo
package optics

import cats.arrow.Profunctor
import cats.syntax.functor.*
import cats.{Applicative, Functor, Monoid}

import data.*

/** Existential encoding of a profunctor optic — the single trait behind every optic family in
  * `cats-eo`.
  *
  * The profunctor presentation of optics quantifies *universally* over a profunctor: `type Optic[S,
  * T, A, B] = forall p. Profunctor[p]
  * => p[A, B] => p[S, T]`. The existential presentation flips the quantifier: it exposes a carrier
  * `F[_, _]` and an existential witness `X`, so the optic becomes a plain value — a pair of
  * functions `(S => F[X, A], F[X, B] => T)` — rather than a polymorphic method. Each optic family
  * picks a different carrier (`Tuple2` for Lens, `Either` for Prism, `Affine` for Optional, …); the
  * existential `X` threads the "leftover" information the carrier needs to rebuild `T` from a
  * modified `B`.
  *
  * A typical call site drives the optic through extension methods in this companion (`.get`,
  * `.modify`, `.replace`, `.foldMap`, …); the carrier-specific typeclasses (`Accessor[F]`,
  * `ForgetfulFunctor[F]`, …) wire each extension to the right `F[X, A]` manipulation.
  *
  * @tparam S
  *   source type being observed / modified
  * @tparam T
  *   result type after modification (often `= S`)
  * @tparam A
  *   focus type read out of `S`
  * @tparam B
  *   focus type written back to produce `T` (often `= A`)
  * @tparam F
  *   carrier functor-of-two-arguments; the family's carrier determines which operations are
  *   available (e.g. `Accessor[F]` unlocks `.get`, `ForgetfulFunctor[F]` unlocks `.modify` /
  *   `.replace`).
  *
  * @see
  *   [[Lens]], [[Prism]], [[Iso]], [[Optional]], [[Setter]], [[Traversal]], [[Getter]], [[Fold]]
  */
trait Optic[S, T, A, B, F[_, _]]:
  self =>

  /** Existential "leftover" carried alongside the focus — the type-level witness each carrier uses
    * to rebuild `T` from a modified `B`. Concrete at each construction site (`Lens.apply` sets it
    * to `Tuple2[S, ?]`'s second slot, `Prism.apply` sets it to `S`, …) and abstract when the optic
    * is bound to `Optic[…, F]` without further refinement.
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

  /** Compose with another optic under the shared carrier `F`. Requires an `AssociativeFunctor`
    * instance for `F`; `Tuple2` / `Either` / `Forgetful` / `Forget[F]` / `Affine` / `PowerSeries`
    * all ship one.
    *
    * For cross-carrier composition (e.g. `Lens → Optional` or `Lens → Traversal`), use the
    * cross-carrier overloads of this same method: they take an `o` whose carrier `G` differs from
    * `F`, summon a `Composer[F, G]` or `Composer[G, F]` to bring both sides under a common carrier,
    * and then compose under that carrier.
    *
    * @example
    *   {{{
    * import dev.constructive.eo.optics.Optic.*
    * import dev.constructive.eo.generics.lens
    *
    * case class Address(street: String)
    * case class Person(address: Address)
    *
    * val streetLens =
    *   lens[Person](_.address).andThen(lens[Address](_.street))
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

/** Companion for [[Optic]]. Hosts the profunctor instances (`outerProfunctor` / `innerProfunctor`)
  * and the catalogue of extension methods that drive every public operation on
  * `Optic[S, T, A, B, F]` — `.get`, `.modify`, `.replace`, `.foldMap`, `.modifyA`, `.all`,
  * `.reverseGet`, `.getOption`, `.put`, `.transform`, `.place`, `.transfer`, `.andThen`, `.morph`.
  * Each extension is gated on a capability typeclass instance for the carrier (`Accessor[F]` for
  * `.get`, `ForgetfulFunctor[F]` for `.modify`, etc.), so adding a new carrier that wants to
  * support `.modify` means supplying a `ForgetfulFunctor[F]` and nothing else.
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

  // ---- Cross-carrier composition (auto-morph) -----------------------
  //
  // One extension, one typeclass (`Morph[F, G]`), three implicit
  // directions (same, left-morph via `Composer[F, G]`, right-morph via
  // `Composer[G, F]`). Scala picks whichever is available; since we
  // don't ship bidirectional composers, at most one applies per
  // carrier pair and there's no ambiguity.

  /** Cross-carrier composition — when the two optics carry different `F` and `G`, this extension
    * picks the direction via a summoned [[Morph]] (which wraps the appropriate `Composer`) and
    * composes under the resulting shared carrier.
    *
    * @example
    *   {{{
    * // `lens.andThen(each).andThen(lens)` — no explicit `.morph` anywhere.
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

    /** Re-express this optic over a different carrier `G`. Package-private — users compose
      * cross-carrier via the [[andThen]] overload above, which invokes this internally via the
      * `Composer.to` it summons. Still available to law / behaviour specs inside `eo.*` for direct
      * testing of the `Composer[F, G]` bridges.
      */
    private[eo] def morph[G[_, _]](using cf: Composer[F, G]): Optic[S, T, A, B, G] =
      cf.to(self)

  // ---- Generic Optic extensions -------------------------------------
  //
  // Grouped by the carrier capability each extension requires.
  // Read the extension's `(using …)` clause to see which carriers
  // can call which method.

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

    // Intentionally NOT `inline`: the body constructs a fresh `Optic`
    // anonymous class, and `inline` would duplicate that class definition
    // at every call site (E197 warning). Since the body already allocates
    // a new Optic, inlining wouldn't eliminate that allocation -- so we
    // keep the method as a normal `def` and avoid the bytecode bloat.
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

  /** `getOption` over an `Affine`-carrier optic — the canonical read for [[Optional]] and
    * [[AffineFold]]. Pattern-matches the Affine directly, so the miss branch allocates nothing
    * beyond the already-produced `Affine.Miss` and the hit branch wraps the focus in `Some`.
    *
    * Distinct from [[Optic.foldMap]] in that it does not require a `Monoid[A]`: a miss produces
    * `None` rather than `Monoid.empty`. For full Optional values this is exactly Monocle's
    * `Optional.getOption`.
    *
    * @group Operations
    */
  extension [S, T, A, B](o: Optic[S, T, A, B, Affine])

    inline def getOption(s: S): Option[A] =
      o.to(s).fold(_ => None, (_, a) => Some(a))
