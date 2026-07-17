package dev.constructive.eo
package schemes
package laws

import data.PSVec
import optics.Optic.*
import optics.Plated

/** Law equations for the recursion schemes in [[Schemes]].
  *
  * First citizen: the '''hylo fusion law''' — `Schemes.hylo`'s scaladoc claims the fused refold is
  * "equal to `ana(…).cross(cata(alg))` on the same computation (the hylo law), but without
  * materializing the structure". This trait turns that claim into a checkable contract: an instance
  * supplies a coalgebra, the `S`-algebra the materializing side folds, and the seed-level fused
  * algebra claimed to correspond to it; [[hyloFusion]] verifies the consequence on every generated
  * seed. The seed expansion is *derived* from the coalgebra (`coalg(_)._1`), so the only coherence
  * an instance asserts is the `alg` ↔ `fusedAlg` correspondence — an incoherent pair fails the law,
  * which is the point.
  *
  * `equals` is used for the comparison, so `A` must have structural equality (every case class /
  * enum and the primitives do).
  *
  * More scheme laws are expected to land here as the zoo grows — para / apo / histo / futu fusion,
  * cata-compose (`cata(f) ∘ cata(g)` deforestation), ana-compose, and the `Plated`-coalgebra
  * coherence (`childrenVec` deconstructs exactly what the coalgebra's builder constructs).
  */
trait HyloLaws[Seed, S, A](using val P: Plated[S]):

  /** The coalgebra under test — a seed yields its child seeds plus the node builder. */
  def coalg: Schemes.Coalg[Seed, S]

  /** The `S`-algebra folded by the materializing `ana.cross(cata)` side. */
  def alg: (S, PSVec[A]) => A

  /** The seed-level algebra claimed to correspond to [[alg]] over the nodes [[coalg]] builds. */
  def fusedAlg: (Seed, PSVec[A]) => A

  /** `hylo(coalg(_)._1, fusedAlg).get(seed) == ana(coalg).cross(cata(alg)).get(seed)` — the fused
    * refold computes exactly what build-then-fold computes, with no intermediate `S`.
    */
  def hyloFusion(seed: Seed): Boolean =
    val expand: Seed => PSVec[Seed] = s => coalg(s)._1
    Schemes.hylo(expand, fusedAlg).get(seed) ==
      Schemes.ana(coalg).cross(Schemes.cata(alg)).get(seed)
