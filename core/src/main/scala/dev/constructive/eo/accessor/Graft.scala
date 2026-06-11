package dev.constructive.eo
package accessor

import data.{Fst, Snd}

/** Build-channel injection for carriers with a *finished* arm — the vocabulary a generic
  * recursion-scheme driver needs to feed a decoration's build seam without knowing the concrete
  * variants.
  *
  * [[done]] injects an already-finished payload: the consumer must treat it as final (an apo graft
  * places it in the result slot as-is; a futu unroll expands the prebuilt layer without consulting
  * the coalgebra again). [[step]] injects a focus alongside its leftover context — the keep-going
  * arm.
  *
  * The payload *meaning* of `done` is pinned per optic value via the existential `X` (`Fst[X]`),
  * not by this capability — see the `Decor` family in `cats-eo-schemes`.
  *
  * @tparam F
  *   the carrier
  */
trait Graft[F[_, _]]:

  /** Inject an already-finished payload — no further building for this slot. */
  def done[X, B](fst: Fst[X]): F[X, B]

  /** Inject a focus `b` alongside its leftover context — keep building. */
  def step[X, B](snd: Snd[X], b: B): F[X, B]
