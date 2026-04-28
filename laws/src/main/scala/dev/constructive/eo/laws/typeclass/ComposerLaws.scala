package dev.constructive.eo
package laws
package typeclass

import _root_.dev.constructive.eo.data.Affine.given
import _root_.dev.constructive.eo.data.{Affine, Forgetful}

import optics.Optic
import optics.Optic.*

/** Laws for `Composer.chainViaTuple2` — multi-hop carrier coercion. Two laws: C1 path independence
  * (two intermediates → same modify output) and C2 preserves `get` on `Accessor`-bearing chains.
  */

/** C1 — path independence. An Iso → Affine via Tuple2 vs via Either should be modify-equivalent. */
trait ComposerPathIndependenceLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  // Spelled out explicitly to sidestep the `iso.morph[Affine]` ambiguity.
  private def viaTuple2: Optic[S, S, A, A, Affine] =
    Affine.tuple2affine.to(Composer.forgetful2tuple.to(iso))

  private def viaEither: Optic[S, S, A, A, Affine] =
    Affine.either2affine.to(Composer.forgetful2either.to(iso))

  def pathIndependence(s: S, f: A => A): Boolean =
    viaTuple2.modify(f)(s) == viaEither.modify(f)(s)

/** C2 — chain preserves `get` whenever both ends have an `Accessor`. */
trait ComposerPreservesGetLaws[S, A, F[_, _], G[_, _], H[_, _]]:
  def optic: Optic[S, S, A, A, F]
  def fToG: Composer[F, G]
  def gToH: Composer[G, H]

  given accessorF: _root_.dev.constructive.eo.data.Accessor[F]
  given accessorH: _root_.dev.constructive.eo.data.Accessor[H]

  def preservesGet(s: S): Boolean =
    gToH.to(fToG.to(optic)).get(s) == optic.get(s)
