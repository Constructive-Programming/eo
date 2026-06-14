package dev.constructive.eo
package schemes

import scala.language.implicitConversions

import org.specs2.mutable.Specification

import optics.Lens
import schemes.samples.{Bin, BinF}

// Top-level (outer-accessor-safe) wrapper so the composition case can put a core Lens *above* the
// scheme Lens.
final case class Box(t: Bin)

/** Step 3: [[Schemes.paraLens]] — the paramorphism promoted to a writable [[Lens]]. `get` is a
  * subterm-retaining fold; `enplace` is the caller-supplied coherent put. The point of the spike: a
  * recursion scheme that is a genuine, lawful Lens, composing with core's Lenses.
  *
  * The fixture is the "leftmost leaf" lens: `get` folds down the left spine (a paramorphism that
  * keeps only the left child's result), `enplace` rewrites that same leaf. A coherent pair, so the
  * three Lens laws hold.
  */
class ParaLensSpec extends Specification:

  // get = leftmost leaf value, via a paramorphism (BranchF keeps the *left* child's fold result).
  private val leftmostAlg: BinF[(Bin, Int)] => Int =
    case BinF.LeafF(n)           => n
    case BinF.BranchF((_, l), _) => l

  // enplace = rewrite the leftmost leaf — the coherent inverse direction.
  private def setLeftmost(s: Bin, v: Int): Bin = s match
    case Bin.Leaf(_)      => Bin.Leaf(v)
    case Bin.Branch(l, r) => Bin.Branch(setLeftmost(l, v), r)

  private val leftmost = Schemes.paraLens[BinF, Bin, Int](leftmostAlg)(setLeftmost)

  private val tree: Bin =
    Bin.Branch(Bin.Branch(Bin.Leaf(1), Bin.Leaf(2)), Bin.Leaf(3))

  "get is the paramorphism: the leftmost leaf" >> {
    leftmost.get(tree) === 1
  }

  "Lens law — get-put (replacing with what you read is a no-op)" >> {
    leftmost.replace(leftmost.get(tree))(tree) === tree
  }

  "Lens law — put-get (reading what you wrote returns it)" >> {
    leftmost.get(leftmost.replace(99)(tree)) === 99
  }

  "Lens law — put-put (a second write wins)" >> {
    leftmost.replace(2)(leftmost.replace(1)(tree)) === leftmost.replace(2)(tree)
  }

  "modify lifts a function over the focus" >> {
    leftmost.modify(_ + 10)(tree) === Bin.Branch(Bin.Branch(Bin.Leaf(11), Bin.Leaf(2)), Bin.Leaf(3))
  }

  "composes with a core Lens on the fused Tuple2 path: Box → leftmost leaf" >> {
    val boxRoot = Lens[Box, Bin](_.t, (b, t) => b.copy(t = t))
    val composed = boxRoot.andThen(leftmost) // a Lens[Box, Int] focusing the leftmost leaf
    (composed.get(Box(tree)) === 1)
      .and(composed.replace(7)(Box(tree)) === Box(setLeftmost(tree, 7)))
  }
