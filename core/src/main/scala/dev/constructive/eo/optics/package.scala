package dev.constructive.eo

/** The concrete optic zoo: [[Optic]] (the one existential base trait) and its named shapes —
  * [[Lens]], [[Prism]], [[Iso]], [[Optional]], [[Traversal]], [[Getter]], [[Fold]], [[AffineFold]],
  * [[Review]], [[Modify]], [[Unfold]], [[Plated]] — each a thin class over a carrier from [[data]]
  * with fused `andThen` overloads so hot compositions skip the generic seam. Construct and compose
  * here; consume through the capability traits in [[dev.constructive.eo]].
  */
package object optics
