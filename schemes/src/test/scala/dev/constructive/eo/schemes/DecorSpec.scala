package dev.constructive.eo
package schemes

import org.specs2.mutable.Specification

import schemes.samples.BinF

/** Unit checks for the decoration data ([[Attr]] / [[Coattr]]) — construction, projection, and
  * structural equality over a real pattern functor. The zoo members that consume them (`histo` /
  * `futu`) carry the behavioural coverage.
  */
class DecorSpec extends Specification:

  // A decorated branch: results 1 and 3 at the leaves, 4 at the root.
  private val decorated: Attr[BinF, Int] =
    Attr(4, BinF.BranchF(Attr(1, BinF.LeafF(1)), Attr(3, BinF.LeafF(3))))

  "Attr (cofree-without-laziness)" should {

    "project its head" in {
      decorated.head === 4
    }

    "expose each child's full history through tail" in {
      decorated.tail === BinF.BranchF(Attr(1, BinF.LeafF(1)), Attr(3, BinF.LeafF(3)))
    }

    "forget == head" in {
      Attr.forget(decorated) === 4
    }
  }

  "Coattr (free-without-suspension)" should {

    "distinguish a seed from a prebuilt layer" in {
      val seed: Coattr[BinF, Int] = Coattr.Pure(7)
      val layer: Coattr[BinF, Int] = Coattr.Roll(BinF.BranchF(Coattr.Pure(1), Coattr.Pure(2)))
      (seed !== layer).and(seed === Coattr.Pure(7))
    }

    "nest prebuilt layers arbitrarily deep" in {
      val two: Coattr[BinF, Int] =
        Coattr.Roll(BinF.BranchF(Coattr.Roll(BinF.LeafF(1)), Coattr.Pure(9)))
      two === Coattr.Roll(BinF.BranchF(Coattr.Roll(BinF.LeafF(1)), Coattr.Pure(9)))
    }
  }
