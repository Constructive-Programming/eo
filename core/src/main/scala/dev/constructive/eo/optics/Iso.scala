package dev.constructive.eo
package optics

import cats.{Functor, Invariant}

import data.{Affine, Direct}

/** Monomorphic `Iso` — an alias for the CONCRETE [[BijectionIso]], not for the generic
  * `Optic[S, S, A, A, Direct]`. Because it names the concrete class, ascribing
  * `val nameI: Iso[Person, (String, Int)] = …` keeps code on the hot, fused paths — the fused
  * compose overloads and capability mixins stay statically visible. [[Lens]] aliases its sealed
  * base [[PLens]] the same way; `Prism` has two unrelated concrete classes and stays prose
  * shorthand.
  */
type Iso[S, A] = BijectionIso[S, S, A, A]

/** Constructor for `Iso` — a bijective single-focus optic, backed by `Direct`. The [[Iso]] type
  * alias above names the concrete [[BijectionIso]] for the monomorphic case, so ascribing it is
  * safe; [[Iso.apply]] returns that class carrying the fused compose overloads and capability
  * mixins. Consuming signatures should still demand a capability (`CanGet[S, A]`,
  * `CanReverseGet[S, A]`, …) rather than name `Iso`. An iso encodes a data-shape bijection.
  * `Direct[X, A] = A` carries no leftover, so every Iso operation reduces to plain function
  * application.
  */
object Iso:

  /** Construct an Iso from forward `get: S => A` and reverse `reverseGet: B => T`. Polymorphic; use
    * `S = T = A = B` for the monomorphic case.
    *
    * @group Constructors
    *
    * @example
    *   {{{
    * case class Person(age: Int, name: String)
    * val pairIso = Iso[(Int, String), (Int, String), Person, Person](
    *   t => Person(t._1, t._2),
    *   p => (p.age, p.name),
    * )
    *   }}}
    */
  def apply[S, T, A, B](f: S => A, g: B => T) =
    BijectionIso[S, T, A, B](f, g)

/** Concrete Optic subclass for an isomorphism. Stores `get` / `reverseGet` directly so the hot path
  * skips the `Accessor[Direct]` / `ReverseAccessor[Direct]` typeclass dispatches the generic
  * extensions would perform — same storage shape as Monocle's `Iso`. Returned by [[Iso.apply]] so
  * hand-written isos pick up the fused path automatically.
  */
final class BijectionIso[S, T, A, B](
    read: S => A,
    build: B => T,
) extends SingleFocus[S, T, A, B, Direct, Invariant],
      CanGet[S, A],
      CanReverseGet[T, B]:
  type X = Nothing
  def get(s: S): A = read(s)
  def reverseGet(b: B): T = build(b)
  def to(s: S): Direct[X, A] = Direct(read(s))
  def from(d: Direct[X, B]): T = build(d.value)

  // CPD-OFF — one `CanModifyFP` contract, a fused body per carrier: the signature repeats in every
  // concrete optic that implements it.
  inline def modify(f: A => B): S => T =
    s => reverseGet(f(get(s)))

  /** Fused effectful modify: read, then map `reverseGet` over the effect. */
  def modifyF[G[_]](f: A => G[B])(using G: Invariant[G] & Functor[G]): S => G[T] =
    s => G.map(f(read(s)))(build)
  // CPD-ON

  override inline def replace(b: B): S => T =
    val t = reverseGet(b)
    _ => t

  // CPD-OFF — the fused `andThen` matrix: one overload per inner optic, each a carrier-specific
  // kernel. Measured against the generic `AssociativeFunctor` / `Morph` route (AndThenBench, see the
  // 2.0 CHANGELOG): never more B/op per call, and only they keep a chain in concrete classes — the
  // generic route allocates per level (lens∘lens depth 6: 528 vs 120 B/op). The signatures and
  // the `Optional.fused(…)` shape repeat across them by construction.
  /** Fused `Iso.andThen(Iso)` — composes `get`s and `reverseGet`s directly. `inline` so each
    * compose site splices distinct lambdas, keeping a deep `iso.andThen(iso)…` chain under C2's
    * recursive-inline cap (see [[Getter.andThen]] / [[PLens.andThen]]).
    */
  inline def andThen[C, D](inner: BijectionIso[A, B, C, D]): BijectionIso[S, T, C, D] =
    new BijectionIso(
      read = s => inner.get(get(s)),
      build = d => reverseGet(inner.reverseGet(d)),
    )

  /** Fused `Iso.andThen(Lens)` — threads the iso around the inner lens. */
  def andThen[C, D](inner: PLens[A, B, C, D]): GetReplaceLens[S, T, C, D] =
    new GetReplaceLens(
      read = s => inner.get(get(s)),
      enplace = (s, d) => reverseGet(inner.enplace(get(s), d)),
    )

  /** Fused `Iso.andThen(Prism)` — on inner miss, outer.reverseGet lifts the leftover. */
  def andThen[C, D](inner: MendTearPrism[A, B, C, D]): MendTearPrism[S, T, C, D] =
    new MendTearPrism(
      tear = s => Optional.liftMiss(inner.tear(get(s)))(reverseGet),
      mend = d => reverseGet(inner.mend(d)),
    )

  /** Fused `Iso.andThen(PickMendPrism)` — a monomorphic iso keeps the prism's `Option`-shaped fast
    * path: the composite is again a [[PickMendPrism]], picking through `get` and mending through
    * `reverseGet`, with no `Either` built.
    */
  def andThen[C, D](inner: PickMendPrism[A, C, D])(using
      ev: A =:= B,
      evT: T =:= S,
  ): PickMendPrism[S, C, D] =
    new PickMendPrism(
      pick = s => inner.pick(get(s)),
      mend = d => evT(reverseGet(ev(inner.mend(d)))),
    )

  /** Fused `Iso.andThen(Optional)` — iso is transparent; result is `Optional`. */
  def andThen[C, D](inner: Optional[A, B, C, D]): Optional[S, T, C, D] =
    Optional.underTotal(inner)(
      get,
      (_, b) => reverseGet(b),
      (_, m) => m,
      (m, d) => reverseGet(inner.rebuild(m, d))
    )

  // CPD-ON
