package dev.constructive.eo
package laws
package eo

import _root_.dev.constructive.eo.data.Affine.given
import _root_.dev.constructive.eo.data.Forgetful.given
import _root_.dev.constructive.eo.data.{Affine, Forgetful}

import optics.Optic
import optics.Optic.*

// Laws governing `Optic.andThen` for each pair of like-shaped optics. The composition laws boil
// down to: the composed optic reads/writes the outer carrier and applies the inner to the focus.
// Kleisli carriers (Prism, Optional) become a flatMap equivalence over Option. Grouped here to
// share the `given *Assoc` shadow-management.

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

  // Both `Forgetful.assoc` and `Affine.assoc` go by the bare name `assoc`; this alias pins the
  // Forgetful instance for this trait's `andThen` calls.
  private given forgetfulAssoc[X, Y]: AssociativeFunctor[Forgetful, X, Y] = Forgetful.assoc

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
      p: Optic[X, X, Y, Y, Either],
      x: X,
  ): Option[Y] = p.to(x).toOption

  /** `(o ∘ i).getOption(s) == o.getOption(s).flatMap(i.getOption)` — the Kleisli law for `Option`.
    */
  def composedGetOption(s: S): Boolean =
    getOpt(outer.andThen(inner), s) ==
      getOpt(outer, s).flatMap(a => getOpt(inner, a))

  /** `(o ∘ i).reverseGet(b) == o.reverseGet(i.reverseGet(b))`. */
  def composedReverseGet(b: B): Boolean =
    outer.andThen(inner).reverseGet(b) ==
      outer.reverseGet(inner.reverseGet(b))

/** Optional ∘ Optional composition laws. */
trait OptionalComposeLaws[S, A, B]:
  // X <: Tuple matches `Optional.apply`'s `type X = (T, S)` materialisation.
  val outer: Optic[S, S, A, A, Affine] { type X <: Tuple }
  val inner: Optic[A, A, B, B, Affine] { type X <: Tuple }

  // Disambiguate `Affine.assoc` from the ambient `Forgetful.assoc` wildcard import.
  private given affineAssoc[X <: Tuple, Y <: Tuple]: AssociativeFunctor[Affine, X, Y] = Affine.assoc

  private def getOpt[X, Y](
      p: Optic[X, X, Y, Y, Affine],
      x: X,
  ): Option[Y] = p.to(x).affine.toOption.map(_._2)

  /** `(o ∘ i).getOption(s) == o.getOption(s).flatMap(i.getOption)`. */
  def composedGetOption(s: S): Boolean =
    getOpt(outer.andThen(inner), s) ==
      getOpt(outer, s).flatMap(a => getOpt(inner, a))

  /** `(o ∘ i).modify(identity)(s) == s`. */
  def composedModifyIdentity(s: S): Boolean =
    outer.andThen(inner).modify(identity[B])(s) == s
