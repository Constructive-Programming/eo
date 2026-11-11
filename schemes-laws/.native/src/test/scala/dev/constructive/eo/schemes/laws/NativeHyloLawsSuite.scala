// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
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

/** Scala Native twin of `HyloLawsSpec` (specs2 has no Native artifact). The 480..600 depth band
  * straddles `Schemes.OnStackLimit`, so the heap-machine fallback runs on Native too.
  */
class NativeHyloLawsSuite extends munit.DisciplineSuite:

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

// CPD-ON
