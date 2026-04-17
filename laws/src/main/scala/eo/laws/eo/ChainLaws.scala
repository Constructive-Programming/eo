package eo
package laws
package eo

import optics.Optic
import optics.Optic.*
import _root_.eo.data.{Affine, Forgetful}
import _root_.eo.data.Forgetful.given
import _root_.eo.data.Affine.given

// Laws governing `Composer.chain` — the mechanism that lifts a series
// of pairwise `Composer`s into a multi-hop carrier coercion.
//
//   F1. Chain path independence — when two distinct intermediate
//       carriers reach the same end carrier, both `modify` paths
//       produce identical output.
//   F2. Chain preserves `get` — for any chain whose carriers all
//       expose `Accessor`, the `get` extension agrees with the
//       uncomposed optic's `get`.

/** F1 — `Composer.chain` path independence.
  *
  * From `Forgetful` there are two direct composers (→ Tuple2,
  * → Either) and from each of those a direct composer to `Affine`.
  * So an `Iso` can land in `Affine` via two distinct chains. They
  * should be modify-equivalent — otherwise `Composer.chain` would be
  * making the caller's choice of intermediary observable.
  */
trait ChainPathIndependenceLaws[S, A]:
  def iso: Optic[S, S, A, A, Forgetful]

  // Using the composers explicitly sidesteps the implicit-resolution
  // ambiguity that `iso.morph[Affine]` would hit (Scala can't pick
  // between Tuple2 and Either for the intermediate G).
  private def viaTuple2: Optic[S, S, A, A, Affine] =
    Affine.tuple2affine.to(Composer.forgetful2tuple.to(iso))

  private def viaEither: Optic[S, S, A, A, Affine] =
    Affine.either2affine.to(Composer.forgetful2either.to(iso))

  def chainPathIndependence(s: S, f: A => A): Boolean =
    viaTuple2.modify(f)(s) == viaEither.modify(f)(s)

/** F2 — `Composer.chain` preserves `get`.
  *
  * A 2-hop chain `F → G → H` preserves `get` whenever all three
  * carriers have an `Accessor`. Currently core only ships `Accessor`
  * instances for `Forgetful` and `Tuple2`, so the only non-degenerate
  * witness is `Forgetful → Tuple2 → Tuple2` with an identity composer
  * at the second hop — testable with a locally-declared identity
  * `Composer` in the spec. The trait itself is fully generic and
  * will gain more witnesses as new `Accessor` instances are added.
  */
trait ChainAccessorLaws[S, A, F[_, _], G[_, _], H[_, _]]:
  def optic: Optic[S, S, A, A, F]
  def fToG: Composer[F, G]
  def gToH: Composer[G, H]

  // The `get` extension on both ends needs an Accessor in scope.
  // Middle-step Accessor isn't required by the law statement but
  // is useful for sanity-checking intermediates if anyone extends
  // this spec.
  given accessorF: _root_.eo.data.Accessor[F]
  given accessorH: _root_.eo.data.Accessor[H]

  def chainPreservesGet(s: S): Boolean =
    gToH.to(fToG.to(optic)).get(s) == optic.get(s)
