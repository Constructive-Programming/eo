package dev.constructive.eo
package schemes
package laws

import org.scalacheck.{Arbitrary, Gen}

import data.PSVec
import optics.Plated
import schemes.laws.discipline.HyloTests

enum HyloExpr:
  case Lit(v: Double)
  case Add(l: HyloExpr, r: HyloExpr)

/** Hylo fusion law across the engines' `OnStackLimit` (512): the 480..600 depth band drives
  * `unfoldFold`, `unfoldCoalg` and `foldInPlace` through their heap-machine fallback. munit, so it
  * runs on every platform (this was a specs2 spec; specs2 has no Scala Native artifact).
  */
class HyloLawsSuite extends munit.DisciplineSuite:

  private given Plated[HyloExpr] = Plated.fromChildren(
    {
      case HyloExpr.Add(l, r) => List(l, r)
      case HyloExpr.Lit(_)    => Nil
    },
    {
      case (HyloExpr.Add(_, _), l :: r :: Nil) => HyloExpr.Add(l, r)
      case (leaf, _)                           => leaf
    },
  )

  private given Arbitrary[Int] = Arbitrary(
    Gen.frequency(3 -> Gen.choose(0, 48), 2 -> Gen.choose(480, 600))
  )

  checkAll(
    "Schemes.hylo (right-nested Add eval, depth straddling OnStackLimit)",
    new HyloTests[Int, HyloExpr, Double]:
      val laws = new HyloLaws[Int, HyloExpr, Double]:
        val coalg: Schemes.Coalg[Int, HyloExpr] = n =>
          if n <= 0 then (PSVec.empty[Int], (_: PSVec[HyloExpr]) => HyloExpr.Lit(1.0))
          else (PSVec.of(0, n - 1), (ks: PSVec[HyloExpr]) => HyloExpr.Add(ks(0), ks(1)))
        val alg: (HyloExpr, PSVec[Double]) => Double = (node, kids) =>
          node match
            case HyloExpr.Lit(v)    => v
            case HyloExpr.Add(_, _) => kids(0) + kids(1)
        val fusedAlg: (Int, PSVec[Double]) => Double =
          (n, rs) => if n <= 0 then 1.0 else rs(0) + rs(1)
    .hylo,
  )
