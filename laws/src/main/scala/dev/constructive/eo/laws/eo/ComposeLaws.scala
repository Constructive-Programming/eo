package dev.constructive.eo
package laws
package eo

import _root_.dev.constructive.eo.data.{Affine, Direct}
import cats.Monoid
import dev.constructive.eo.accessor.Accessor
import dev.constructive.eo.compose.*
import dev.constructive.eo.forgetful.ForgetfulFunctor

import optics.Optic
import optics.Optic.*

// Laws governing `Optic.andThen`. Two layers:
//
//   1. Capability-keyed compose-coherence (`Composed*Laws`) — the composition equation stated ONCE
//      per read/write capability ([[CanGet]] / [[CanGetOption]] / [[CanFold]] / [[CanReverseGet]]),
//      so a single law serves every family that admits that capability AND their cross-family
//      composites. These are keyed by capability only, never by carrier: the three legs (outer,
//      inner, composed) are supplied as capability instances, so the same trait witnesses
//      Lens ∘ Lens, Iso ∘ Getter, Prism ∘ Lens, … alike.
//
//   2. The per-family pair laws (`LensComposeLaws` / `IsoComposeLaws` / `PrismComposeLaws` /
//      `OptionalComposeLaws`) now DELEGATE their shared members to layer 1 by mixing in the matching
//      capability trait and wiring each leg from their concrete optic fields. Only the genuinely
//      family-specific members remain spelled out here (`composedReplace` for Lens, the write-seam
//      `composedModifyIdentity` for Optional). This removes the byte-for-byte duplication the four
//      traits used to carry (Lens/Iso shared `composedGet`; Prism/Optional shared
//      `composedGetOption`; Iso/Prism shared `composedReverseGet`).
//
// Binary-compat note: the shared members are now inherited rather than declared on each per-family
// trait, and the traits gained new abstract capability-leg members. MiMa is disabled for the 0.x
// line (`tlMimaPreviousVersions := Set.empty`), so this reshaping is free; downstream law authors
// who instantiated the per-family traits keep the same optic-valued `outer` / `inner` surface.

// ===== Layer 1: capability-keyed compose-coherence =====

/** Compose-coherence for total read. For `composed = outer.andThen(inner)`,
  * `composed.get(s) == inner.get(outer.get(s))` — reading through the whole chain equals reading
  * the outer focus, then the inner. Keyed by [[CanGet]] on all three legs (Lens / Iso / Getter and
  * their total-read composites).
  */
trait ComposedGetLaws[S, A, B]:
  /** Outer leg's read capability. */
  def getOuter: CanGet[S, A]

  /** Inner leg's read capability. */
  def getInner: CanGet[A, B]

  /** Composed chain's read capability. */
  def getComposed: CanGet[S, B]

  /** `(o ∘ i).get(s) == i.get(o.get(s))`. */
  def composedGet(s: S): Boolean =
    getComposed.get(s) == getInner.get(getOuter.get(s))

/** Compose-coherence for partial read. For `composed = outer.andThen(inner)`,
  * `composed.getOption(s) == outer.getOption(s).flatMap(inner.getOption)` — the Kleisli law over
  * `Option`. Keyed by [[CanGetOption]] on all three legs (Prism / Optional / AffineFold and their
  * partial-read composites).
  */
trait ComposedGetOptionLaws[S, A, B]:
  /** Outer leg's partial-read capability. */
  def getOptionOuter: CanGetOption[S, A]

  /** Inner leg's partial-read capability. */
  def getOptionInner: CanGetOption[A, B]

  /** Composed chain's partial-read capability. */
  def getOptionComposed: CanGetOption[S, B]

  /** `(o ∘ i).getOption(s) == o.getOption(s).flatMap(i.getOption)`. */
  def composedGetOption(s: S): Boolean =
    getOptionComposed.getOption(s) ==
      getOptionOuter.getOption(s).flatMap(getOptionInner.getOption)

/** Compose-coherence for folding. For `composed = outer.andThen(inner)`,
  * `composed.foldMap(f)(s) == outer.foldMap(a => inner.foldMap(f)(a))(s)` — folding the whole chain
  * equals folding each outer focus through the inner fold. Keyed by [[CanFold]] on all three legs
  * (every readable family; the primary surface of Fold and Traversal).
  */
trait ComposedFoldMapLaws[S, A, B]:
  /** Outer leg's fold capability. */
  def foldOuter: CanFold[S, A]

  /** Inner leg's fold capability. */
  def foldInner: CanFold[A, B]

  /** Composed chain's fold capability. */
  def foldComposed: CanFold[S, B]

  /** `(o ∘ i).foldMap(f)(s) == o.foldMap(a => i.foldMap(f)(a))(s)`. */
  def composedFoldMap[M](f: B => M)(s: S)(using Monoid[M]): Boolean =
    foldComposed.foldMap(f)(s) ==
      foldOuter.foldMap((a: A) => foldInner.foldMap(f)(a))(s)

/** Compose-coherence for building. For `composed = outer.andThen(inner)`,
  * `composed.reverseGet(c) == outer.reverseGet(inner.reverseGet(c))` — building through the chain
  * equals building the inner focus, then re-homing it via the outer. Keyed by [[CanReverseGet]] on
  * all three legs (Iso / Prism / Review and their build composites).
  */
trait ComposedReverseGetLaws[S, A, B]:
  /** Outer leg's build capability. */
  def reverseOuter: CanReverseGet[S, A]

  /** Inner leg's build capability. */
  def reverseInner: CanReverseGet[A, B]

  /** Composed chain's build capability. */
  def reverseComposed: CanReverseGet[S, B]

  /** `(o ∘ i).reverseGet(c) == o.reverseGet(i.reverseGet(c))`. */
  def composedReverseGet(c: B): Boolean =
    reverseComposed.reverseGet(c) == reverseOuter.reverseGet(reverseInner.reverseGet(c))

/** Associativity of `andThen`: the two bracketings of a three-optic chain behave identically. The
  * spec materialises both `leftNested = (a ∘ b) ∘ c` and `rightNested = a ∘ (b ∘ c)` because the
  * law trait can't call `.andThen` itself (the carrier's leftover `X` is abstract here — the same
  * reason [[dev.constructive.eo.laws.typeclass.AssociativeFunctorLaws]] takes a materialised
  * `composed`).
  *
  *   - `associativeModify` holds for any `ForgetfulFunctor[F]` carrier.
  *   - `associativeGet` additionally requires `Accessor[F]` (a total-read carrier).
  *
  * No `andThen` identity unit law (`o ∘ id == o == id ∘ o`) is stated: core ships no identity-optic
  * constructor (no `Iso.id` / `Optic.id`), so there is nothing lawful to compose against. Add the
  * unit law here if such a constructor lands.
  */
trait ComposeAssociativityLaws[S, A, F[_, _]]:
  /** `(a ∘ b) ∘ c`, materialised by the spec. */
  def leftNested: Optic[S, S, A, A, F]

  /** `a ∘ (b ∘ c)`, materialised by the spec. */
  def rightNested: Optic[S, S, A, A, F]

  /** `ForgetfulFunctor` evidence driving `modify` on the shared carrier. */
  given functor: ForgetfulFunctor[F]

  /** `((a ∘ b) ∘ c).modify(f)(s) == (a ∘ (b ∘ c)).modify(f)(s)`. */
  def associativeModify(s: S, f: A => A): Boolean =
    leftNested.modify(f)(s) == rightNested.modify(f)(s)

  /** `((a ∘ b) ∘ c).get(s) == (a ∘ (b ∘ c)).get(s)` — only where the carrier reads totally. */
  def associativeGet(s: S)(using Accessor[F]): Boolean =
    leftNested.get(s) == rightNested.get(s)

// ===== Layer 2: per-family pair laws (rewired onto Layer 1) =====

/** C1/C2 — Lens ∘ Lens composition laws. `composedGet` is inherited from [[ComposedGetLaws]];
  * `composedReplace` is the Lens-specific write-seam law.
  */
trait LensComposeLaws[S, A, B] extends ComposedGetLaws[S, A, B]:
  /** Outer optic. */
  def outer: Optic[S, S, A, A, Tuple2]

  /** Inner optic. */
  def inner: Optic[A, A, B, B, Tuple2]

  private def composedOptic: Optic[S, S, B, B, Tuple2] = outer.andThen(inner)

  final def getOuter: CanGet[S, A] = s => outer.get(s)
  final def getInner: CanGet[A, B] = a => inner.get(a)
  final def getComposed: CanGet[S, B] = s => composedOptic.get(s)

  /** C2 — `(o ∘ i).replace(b)(s) == o.replace(i.replace(b)(o.get(s)))(s)`. */
  def composedReplace(s: S, b: B): Boolean =
    composedOptic.replace(b)(s) ==
      outer.replace(inner.replace(b)(outer.get(s)))(s)

/** C3/C4 — Iso ∘ Iso composition laws. Both `composedGet` and `composedReverseGet` are inherited
  * from the capability layer.
  */
trait IsoComposeLaws[S, A, B] extends ComposedGetLaws[S, A, B] with ComposedReverseGetLaws[S, A, B]:
  /** Outer optic. */
  def outer: Optic[S, S, A, A, Direct]

  /** Inner optic. */
  def inner: Optic[A, A, B, B, Direct]

  // Both `Direct.assoc` and `Affine.assoc` go by the bare name `assoc`; this alias pins the
  // Direct instance for this trait's `andThen` calls.
  private given forgetfulAssoc[X, Y]: AssociativeFunctor[Direct, X, Y] = Direct.assoc

  private def composedOptic: Optic[S, S, B, B, Direct] = outer.andThen(inner)

  final def getOuter: CanGet[S, A] = s => outer.get(s)
  final def getInner: CanGet[A, B] = a => inner.get(a)
  final def getComposed: CanGet[S, B] = s => composedOptic.get(s)

  final def reverseOuter: CanReverseGet[S, A] = a => outer.reverseGet(a)
  final def reverseInner: CanReverseGet[A, B] = b => inner.reverseGet(b)
  final def reverseComposed: CanReverseGet[S, B] = b => composedOptic.reverseGet(b)

/** C5 — Prism ∘ Prism composition laws. Both `composedGetOption` and `composedReverseGet` are
  * inherited from the capability layer.
  */
trait PrismComposeLaws[S, A, B]
    extends ComposedGetOptionLaws[S, A, B]
    with ComposedReverseGetLaws[S, A, B]:
  /** Outer optic. */
  def outer: Optic[S, S, A, A, Either]

  /** Inner optic. */
  def inner: Optic[A, A, B, B, Either]

  private def composedOptic: Optic[S, S, B, B, Either] = outer.andThen(inner)

  final def getOptionOuter: CanGetOption[S, A] = s => outer.getOption(s)
  final def getOptionInner: CanGetOption[A, B] = a => inner.getOption(a)
  final def getOptionComposed: CanGetOption[S, B] = s => composedOptic.getOption(s)

  final def reverseOuter: CanReverseGet[S, A] = a => outer.reverseGet(a)
  final def reverseInner: CanReverseGet[A, B] = b => inner.reverseGet(b)
  final def reverseComposed: CanReverseGet[S, B] = b => composedOptic.reverseGet(b)

/** Optional ∘ Optional composition laws. `composedGetOption` is inherited from the capability
  * layer; `composedModifyIdentity` is the Optional-specific write-seam law.
  */
trait OptionalComposeLaws[S, A, B] extends ComposedGetOptionLaws[S, A, B]:
  /** Outer optic — the `X <: Tuple` refinement matches `Optional.apply`'s `type X = (T, S)`
    * materialisation.
    */
  val outer: Optic[S, S, A, A, Affine] { type X <: Tuple }

  /** Inner optic. */
  val inner: Optic[A, A, B, B, Affine] { type X <: Tuple }

  // Disambiguate `Affine.assoc` from the ambient `Direct.assoc` wildcard import.
  private given affineAssoc[X <: Tuple, Y <: Tuple]: AssociativeFunctor[Affine, X, Y] = Affine.assoc

  private def composedOptic: Optic[S, S, B, B, Affine] = outer.andThen(inner)

  final def getOptionOuter: CanGetOption[S, A] = s => outer.getOption(s)
  final def getOptionInner: CanGetOption[A, B] = a => inner.getOption(a)
  final def getOptionComposed: CanGetOption[S, B] = s => composedOptic.getOption(s)

  /** `(o ∘ i).modify(identity)(s) == s`. */
  def composedModifyIdentity(s: S): Boolean =
    composedOptic.modify(identity[B])(s) == s
