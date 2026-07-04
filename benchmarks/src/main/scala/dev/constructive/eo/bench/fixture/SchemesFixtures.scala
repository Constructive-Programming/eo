package dev.constructive.eo
package bench
package fixture

import cats.Functor
import higherkindness.droste.data.Fix
import higherkindness.droste.{Algebra, Coalgebra}

import dev.constructive.eo.data.PSVec
import dev.constructive.eo.schemes.Schemes

/** Pattern functor for the native [[Bin]] tree (`Leaf(Int)` / `Node(Bin, Bin)`).
  *
  * droste requires this pattern-functor + `Fix` encoding to express recursion schemes; eo works on
  * the native `Bin` directly through its `Plated` instance, and the hand-wired baseline recurses on
  * `Bin` straight. The three encodings are what `SchemesBench` contrasts on the *same* fold
  * (leaf-sum) and the *same* build (a perfect binary tree of `2^depth` leaves).
  */
enum BinF[+A]:
  case LeafF(value: Int)
  case NodeF(left: A, right: A)

object SchemesFixtures:

  given binFunctor: Functor[BinF] with

    def map[A, B](fa: BinF[A])(f: A => B): BinF[B] =
      fa match
        case BinF.LeafF(v)    => BinF.LeafF(v)
        case BinF.NodeF(l, r) => BinF.NodeF(f(l), f(r))

  // ----- droste algebra / coalgebra (over BinF, on Fix[BinF]) ----------------

  val drosteSum: Algebra[BinF, Int] = Algebra {
    case BinF.LeafF(v)    => v
    case BinF.NodeF(l, r) => l + r
  }

  /** Build a perfect binary tree: depth `d` ⇒ `2^d` `Leaf(1)`s. */
  val drosteBuild: Coalgebra[BinF, Int] = Coalgebra { d =>
    if d <= 0 then BinF.LeafF(1) else BinF.NodeF(d - 1, d - 1)
  }

  // ----- eo algebra / coalgebra (over native Bin via Plated) -----------------

  val eoSum: (Bin, PSVec[Int]) => Int = (node, kids) =>
    node match
      case Bin.Leaf(v)    => v
      case Bin.Node(_, _) => kids(0) + kids(1)

  /** Seed expansion shared by eo's hylo/ana: depth `d` ⇒ two child seeds `(d-1, d-1)`; leaf at
    * `d <= 0`. `PSVec.of` builds the 2-vector directly (no `List` intermediate).
    */
  val eoExpand: Int => PSVec[Int] = d => if d <= 0 then PSVec.empty[Int] else PSVec.of(d - 1, d - 1)

  /** Fused hylo algebra — folds to `Int` directly, never building a `Bin`. */
  val eoHyloAlg: (Int, PSVec[Int]) => Int = (d, rs) => if d <= 0 then 1 else rs(0) + rs(1)

  /** ana coalgebra (bundled) — each seed's child seeds + how to assemble a native `Bin`. */
  val eoAnaCoalg: Schemes.Coalg[Int, Bin] = d =>
    if d <= 0 then (PSVec.empty[Int], (_: PSVec[Bin]) => Bin.Leaf(1))
    else (PSVec.of(d - 1, d - 1), (ks: PSVec[Bin]) => Bin.Node(ks(0), ks(1)))

  // ----- hand-wired recursion (the baseline you'd write without either lib) --

  def handSum(b: Bin): Int =
    b match
      case Bin.Leaf(v)    => v
      case Bin.Node(l, r) => handSum(l) + handSum(r)

  def handBuild(d: Int): Bin =
    if d <= 0 then Bin.Leaf(1) else Bin.Node(handBuild(d - 1), handBuild(d - 1))

  def handHylo(d: Int): Int =
    if d <= 0 then 1 else handHylo(d - 1) + handHylo(d - 1)

  // ----- prebuilt trees for the cata benches (built once, not measured) ------

  def balancedBin(d: Int): Bin = handBuild(d)

  def balancedFix(d: Int): Fix[BinF] =
    if d <= 0 then Fix(BinF.LeafF(1))
    else Fix(BinF.NodeF(balancedFix(d - 1), balancedFix(d - 1)))
