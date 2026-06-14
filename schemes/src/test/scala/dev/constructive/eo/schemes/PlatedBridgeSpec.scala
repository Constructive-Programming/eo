package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import optics.Plated
import schemes.samples.{Bin, BinF}

/** The Step-1 bridge: a typed pattern-functor [[Basis]] (the schemes' `S`↔`F` correspondence) feeds
  * core's [[optics.Plated.fromBasis]], so the same `Basis[BinF, Bin]` that drives `cata`/`ana` also
  * drives core's `Plated` recursion combinators (`children` / `universe` / `transform`). One
  * correspondence, both worlds. */
class PlatedBridgeSpec extends Specification:

  // The bridge: derive the core Plated straight from the schemes' Basis.
  private given Plated[Bin] = Plated.fromBasis[BinF, Bin]

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

  "fromBasis.children yields the immediate subterms in project order" >> {
    Plated.children(tree) === List(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))
  }

  "fromBasis.universe enumerates the whole tree (self first, pre-order)" >> {
    Plated.universe(tree) === List(
      tree,
      Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)),
      Bin.Leaf(1),
      Bin.Leaf(2),
      Bin.Leaf(3),
    )
  }

  "fromBasis.transform rewrites every node bottom-up" >> {
    val bumped = Plated.transform[Bin] {
      case Bin.Leaf(n) => Bin.Leaf(n + 10)
      case b           => b
    }(tree)
    bumped === Bin.Branch(Bin.Branch(Bin.Leaf(11), Bin.Leaf(12)), Bin.Leaf(13))
  }

  "fromBasis rebuild is identity (the embed∘project coherence the derivation rests on)" >> {
    Plated.transform[Bin](identity)(tree) === tree
  }

  "fromBasis.universe sum agrees with a cata leaf-sum over the same Basis" >> {
    val viaPlated = Plated
      .universe(tree)
      .collect { case Bin.Leaf(n) => n }
      .sum
    val viaCata = Schemes
      .cata[BinF, Bin, Int] {
        case BinF.LeafF(n)      => n
        case BinF.BranchF(l, r) => l + r
      }
      .get(tree)
    viaPlated === viaCata and (viaPlated === 6)
  }
