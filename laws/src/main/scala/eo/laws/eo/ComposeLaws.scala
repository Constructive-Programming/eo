package eo
package laws
package eo

import optics.Optic
import optics.Optic.*
import data.{Affine, Forgetful}
import data.Forgetful.given
import data.Affine.given

// Laws governing `Optic.andThen` for each pair of like-shaped optics
// (Lens ∘ Lens, Iso ∘ Iso, Prism ∘ Prism, Optional ∘ Optional).
//
// The composition laws all boil down to: the composed optic behaves
// as if you read/wrote the outer carrier, then applied the inner
// optic to that focus. For Kleisli-shaped carriers (Prism, Optional)
// the law becomes a flatMap equivalence over `Option`.
//
// C1/C2 (Lens), C3/C4 (Iso), C5 (Prism), plus the Optional variant
// are grouped in this one file because they share a dependency on
// `AssociativeFunctor` instances for the relevant carriers; keeping
// them together avoids duplicating the `given affineAssoc` /
// `given forgetfulAssoc` shadow-management blocks.

/** C1/C2 — Lens ∘ Lens composition laws. */
trait LensComposeLaws[S, A, B]:
  def outer: Optic[S, S, A, A, Tuple2]
  def inner: Optic[A, A, B, B, Tuple2]

  def composedGet(s: S): Boolean =
    outer.andThen(inner).get(s) == inner.get(outer.get(s))

  def composedReplace(s: S, b: B): Boolean =
    outer.andThen(inner).replace(b)(s) ==
      outer.replace(inner.replace(b)(outer.get(s)))(s)

/** C3/C4 — Iso ∘ Iso composition laws. */
trait IsoComposeLaws[S, A, B]:
  def outer: Optic[S, S, A, A, Forgetful]
  def inner: Optic[A, A, B, B, Forgetful]

  // Forgetful.assoc and Affine.assoc both go by the bare name `assoc`,
  // so the wildcard `data.*.given` imports at the top of the file let
  // them shadow each other for by-name implicit search. This alias
  // pins the Forgetful instance for just this trait's `andThen` calls.
  private given forgetfulAssoc[X, Y]
      : AssociativeFunctor[Forgetful, X, Y] = Forgetful.assoc

  def composedGet(s: S): Boolean =
    outer.andThen(inner).get(s) == inner.get(outer.get(s))

  def composedReverseGet(c: B): Boolean =
    outer.andThen(inner).reverseGet(c) ==
      outer.reverseGet(inner.reverseGet(c))

/** C5 — Prism ∘ Prism composition laws. */
trait PrismComposeLaws[S, A, B]:
  def outer: Optic[S, S, A, A, Either]
  def inner: Optic[A, A, B, B, Either]

  private def getOpt[X, Y](
      p: Optic[X, X, Y, Y, Either], x: X
  ): Option[Y] = p.to(x).toOption

  /** `(o ∘ i).getOption(s) == o.getOption(s).flatMap(i.getOption)` —
    * the Kleisli law for `Option`. */
  def composedGetOption(s: S): Boolean =
    getOpt(outer.andThen(inner), s) ==
      getOpt(outer, s).flatMap(a => getOpt(inner, a))

  /** `(o ∘ i).reverseGet(b) == o.reverseGet(i.reverseGet(b))`. */
  def composedReverseGet(b: B): Boolean =
    outer.andThen(inner).reverseGet(b) ==
      outer.reverseGet(inner.reverseGet(b))

/** Optional ∘ Optional composition laws. */
trait OptionalComposeLaws[S, A, B]:
  // Affine's AssociativeFunctor instance requires both Xs to be tuple
  // subtypes (matching how `Optional.apply` materialises
  // `type X = (T, S)`). The refinement surfaces that constraint to
  // the trait boundary.
  val outer: Optic[S, S, A, A, Affine] { type X <: Tuple }
  val inner: Optic[A, A, B, B, Affine] { type X <: Tuple }

  // Disambiguate `Affine.assoc` from the ambient `Forgetful.assoc`
  // wildcard import.
  private given affineAssoc[X <: Tuple, Y <: Tuple]
      : AssociativeFunctor[Affine, X, Y] = Affine.assoc

  private def getOpt[X, Y](
      p: Optic[X, X, Y, Y, Affine], x: X
  ): Option[Y] = p.to(x).affine.toOption.map(_._2)

  /** `(o ∘ i).getOption(s) == o.getOption(s).flatMap(i.getOption)`. */
  def composedGetOption(s: S): Boolean =
    getOpt(outer.andThen(inner), s) ==
      getOpt(outer, s).flatMap(a => getOpt(inner, a))

  /** `(o ∘ i).modify(identity)(s) == s`. */
  def composedModifyIdentity(s: S): Boolean =
    outer.andThen(inner).modify(identity[B])(s) == s
