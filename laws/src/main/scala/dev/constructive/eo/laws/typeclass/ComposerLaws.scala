package dev.constructive.eo
package laws
package typeclass

import _root_.dev.constructive.eo.data.Affine.given
import _root_.dev.constructive.eo.data.{Affine, Forgetful}

import optics.Optic
import optics.Optic.*

/** Laws governing [[Composer.chain]] — the mechanism that lifts a series of pairwise `Composer`s
  * into a multi-hop carrier coercion. Promoted from the earlier
  * `dev.constructive.eo.laws.eo.ChainLaws` in Unit 7 so downstream projects adding a new carrier
  * can verify their `Composer` instances as first-class typeclass laws, not as optic-shape-specific
  * EO-laws.
  *
  * C1. Composer path independence — when two distinct intermediate carriers reach the same end
  * carrier, both `modify` paths produce identical output. C2. Composer preserves `get` — for a
  * chain whose carriers all expose `Accessor`, the `get` extension on the composed optic agrees
  * with the uncomposed optic's `get`.
  */

/** C1 — `Composer.chain` path independence.
  *
  * From `Forgetful` there are two direct composers (→ Tuple2, → Either) and from each of those a
  * direct composer to `Affine`. So an `Iso` can land in `Affine` via two distinct chains. They
  * should be modify-equivalent — otherwise `Composer.chain` would be making the caller's choice of
  * intermediary observable.
  */
trait ComposerPathIndependenceLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  // Using the composers explicitly sidesteps the implicit-resolution
  // ambiguity that `iso.morph[Affine]` would hit (Scala can't pick
  // between Tuple2 and Either for the intermediate G).
  private def viaTuple2: Optic[S, S, A, A, Affine] =
    Affine.tuple2affine.to(Composer.forgetful2tuple.to(iso))

  private def viaEither: Optic[S, S, A, A, Affine] =
    Affine.either2affine.to(Composer.forgetful2either.to(iso))

  def pathIndependence(s: S, f: A => A): Boolean =
    viaTuple2.modify(f)(s) == viaEither.modify(f)(s)

/** C2 — `Composer.chain` preserves `get`.
  *
  * A 2-hop chain `F → G → H` preserves `get` whenever all three carriers have an `Accessor`.
  * Currently core only ships `Accessor` instances for `Forgetful` and `Tuple2`, so the only
  * non-degenerate witness is `Forgetful → Tuple2 → Tuple2` with an identity composer at the second
  * hop — testable with a locally-declared identity `Composer` in the spec. The trait itself is
  * fully generic and will gain more witnesses as new `Accessor` instances are added.
  */
trait ComposerPreservesGetLaws[S, A, F[_, _], G[_, _], H[_, _]]:
  def optic: Optic[S, S, A, A, F]
  def fToG: Composer[F, G]
  def gToH: Composer[G, H]

  // The `get` extension on both ends needs an Accessor in scope.
  // Middle-step Accessor isn't required by the law statement but
  // is useful for sanity-checking intermediates if anyone extends
  // this spec.
  given accessorF: _root_.dev.constructive.eo.data.Accessor[F]
  given accessorH: _root_.dev.constructive.eo.data.Accessor[H]

  def preservesGet(s: S): Boolean =
    gToH.to(fToG.to(optic)).get(s) == optic.get(s)
