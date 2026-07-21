package dev.constructive.eo
package schemes

/** Law definitions for the recursion schemes in [[Schemes]] — separate from `cats-eo-laws` because
  * these laws quantify over `schemes` types (`Schemes.Coalg`, `PSVec`, the fold machinery), which
  * the core law module deliberately does not depend on. Same discipline pattern as
  * [[dev.constructive.eo.laws]]: one `*Laws` trait of law equations here, its ScalaCheck/specs2
  * `RuleSet` bundle under [[laws.discipline]], wired by overriding `laws` and `checkAll`-ed from
  * your suite with your own generators.
  *
  * Coverage so far is the '''hylo fusion law ONLY''' ([[HyloLaws]] / `discipline.HyloTests`):
  * `hylo(expand, fused)` computes exactly what `ana(coalg).cross(cata(alg))` computes, with no
  * intermediate structure. More scheme laws are expected to land here as the zoo grows — para / apo
  * / histo / futu fusion, cata-compose and ana-compose deforestation, and `Plated`-coalgebra
  * coherence; see the [[HyloLaws]] scaladoc for the roadmap.
  */
package object laws
