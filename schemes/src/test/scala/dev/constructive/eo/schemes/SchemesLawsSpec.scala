package dev.constructive.eo
package schemes

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import data.PSVec
import optics.Plated
import optics.Optic.* // cross

import generics.plate
import schemes.samples.Expr

class SchemesLawsSpec extends Specification with ScalaCheck:

  private given Plated[Expr] = plate[Expr]

  private val eval: (Expr, PSVec[Double]) => Double = (node, kids) =>
    node match
      case Expr.Lit(v)    => v
      case Expr.Neg(_)    => -kids(0)
      case Expr.Add(_, _) => kids(0) + kids(1)
      case Expr.Mul(_, _) => kids(0) * kids(1)

  private def refEval(e: Expr): Double = e match
    case Expr.Lit(v)    => v
    case Expr.Neg(a)    => -refEval(a)
    case Expr.Add(l, r) => refEval(l) + refEval(r)
    case Expr.Mul(l, r) => refEval(l) * refEval(r)

  private val genExpr: Gen[Expr] =
    def go(d: Int): Gen[Expr] =
      val lit = Gen.choose(0.0, 9.0).map(Expr.Lit(_))
      if d <= 0 then lit
      else
        Gen.oneOf(
          lit,
          go(d - 1).map(Expr.Neg(_)),
          for l <- go(d - 1); r <- go(d - 1) yield Expr.Add(l, r),
          for l <- go(d - 1); r <- go(d - 1) yield Expr.Mul(l, r),
        )
    Gen.choose(0, 4).flatMap(go)

  "cata(eval) matches a reference recursive eval (∀ Expr)" >> forAll(genExpr) { e =>
    Schemes.cata(eval).get(e) == refEval(e)
  }

  // Non-commutative algebra (Add→subtraction, Mul→division) quantified over random trees: catches a
  // transposed-children regression in the engine that the commutative `eval` above cannot.
  private val sub: (Expr, PSVec[Double]) => Double = (node, kids) =>
    node match
      case Expr.Lit(v)    => v
      case Expr.Neg(_)    => -kids(0)
      case Expr.Add(_, _) => kids(0) - kids(1)
      case Expr.Mul(_, _) => kids(0) / kids(1)

  private def refSub(e: Expr): Double = e match
    case Expr.Lit(v)    => v
    case Expr.Neg(a)    => -refSub(a)
    case Expr.Add(l, r) => refSub(l) - refSub(r)
    case Expr.Mul(l, r) => refSub(l) / refSub(r)

  "cata(sub) matches an order-sensitive reference eval (∀ Expr, left-to-right child order)" >>
    forAll(genExpr) { e =>
      val a = Schemes.cata(sub).get(e)
      val b = refSub(e)
      a == b || (a.isNaN && b.isNaN) // division by a zero Lit can yield NaN on both sides
    }

  // seed n builds a right-nested Add of (n+1) ones; fusedFib computes the same sum directly.
  private val buildExpr: Schemes.Coalg[Int, Expr] = n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(1.0))
    else (PSVec.fromIterable(List(0, n - 1)), (ks: PSVec[Expr]) => Expr.Add(ks(0), ks(1)))

  private val fusedFib: Schemes.Coalg[Int, Double] = n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Double]) => 1.0)
    else (PSVec.fromIterable(List(0, n - 1)), (rs: PSVec[Double]) => rs(0) + rs(1))

  "hylo law: fused hylo == ana.cross(cata) == cata ∘ ana (∀ seed)" >> forAll(Gen.choose(0, 12)) {
    n =>
      val fused = Schemes.hylo(fusedFib).get(n)
      val crossed = Schemes.ana(buildExpr).cross(Schemes.cata(eval)).get(n)
      val materialized = Schemes.cata(eval).get(Schemes.ana(buildExpr).reverseGet(n))
      fused == crossed && crossed == materialized
  }
