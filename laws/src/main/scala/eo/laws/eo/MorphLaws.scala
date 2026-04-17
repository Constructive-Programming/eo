package eo
package laws
package eo

import optics.Optic

/** Laws that pin down `Optic.morph` — EO's carrier-coercion extension.
  *
  * A1/I1. Morph preserves `modify`: reshaping the carrier does not
  * change what the optic does to the focus.
  *
  * A2. Morph preserves `get`: for any carriers `F` / `G` that both
  * expose `Accessor`, reshaping preserves the read path as well.
  *
  * These two laws are the foundation of EO's "write once, reuse
  * everywhere" claim about optics. If they failed, a user could not
  * safely convert a `Lens` to an `Optional` or a `Getter` to a `Fold`
  * without changing observable behaviour.
  */
trait MorphLaws[S, A, F[_, _], G[_, _]]:
  def optic: Optic[S, S, A, A, F]
  def morphed: Optic[S, S, A, A, G]

  /** A1 — morph preserves modify. */
  def morphPreservesModify(s: S, f: A => A)(using
      ForgetfulFunctor[F], ForgetfulFunctor[G]
  ): Boolean =
    morphed.modify(f)(s) == optic.modify(f)(s)

  /** A2 — morph preserves get (when both carriers have `Accessor`). */
  def morphPreservesGet(s: S)(using
      data.Accessor[F], data.Accessor[G]
  ): Boolean =
    morphed.get(s) == optic.get(s)
