package dev.constructive.eo.spike

import dev.constructive.eo.data.PSVec
import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.{DirectGetter, Getter, Plated}
import dev.constructive.eo.spike.samples.Expr
import org.specs2.mutable.Specification

/** A non-recursive carrier holding an `Expr`, to demonstrate that `cata`-as-`Getter`
  * composes onto an outer optic via `andThen` (top-level: no macro, but keeps it clean). */
final case class Wrapped(label: String, expr: Expr)

/** Recursion schemes AS eo optics: cata is a composable `Getter` (driven by a generics-derived
  * `Plated[Expr]`); ana is a `Review`. The headline is composition — `outerGetter.andThen(cata)` —
  * which is the eo-distinctive capability droste's free-standing schemes cannot offer.
  */
class OpticsSpec extends Specification:

  private given Plated[Expr] = plate[Expr]

  private val evalAlg: (Expr, PSVec[Double]) => Double = (node, kids) =>
    node match
      case Expr.Lit(v)    => v
      case Expr.Neg(_)    => -kids(0)
      case Expr.Add(_, _) => kids(0) + kids(1)
      case Expr.Mul(_, _) => kids(0) * kids(1)

  // Add(1, Mul(2, 3)) = 7
  private val expr: Expr = Expr.Add(Expr.Lit(1.0), Expr.Mul(Expr.Lit(2.0), Expr.Lit(3.0)))

  "cata is a Getter that evaluates an Expr" >> {
    val evalG: DirectGetter[Expr, Double] = Optics.cata(evalAlg)
    (evalG.get(expr) == 7.0) must beTrue
  }

  "cata-as-Getter COMPOSES onto an outer Getter via andThen (the eo differentiator)" >> {
    val exprG: DirectGetter[Wrapped, Expr] = Getter(_.expr)
    val composed: DirectGetter[Wrapped, Double] = exprG.andThen(Optics.cata(evalAlg))
    (composed.get(Wrapped("x", expr)) == 7.0) must beTrue
  }

  "ana is a Review; cata ∘ ana evaluates a built structure (both as optics)" >> {
    // seed n builds a right-nested Add of (n+1) ones, evaluating to n+1.
    val buildCoalg: Schemes.CoalgA[Int, Expr] = n =>
      if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(1.0))
      else (PSVec.fromIterable(List(0, n - 1)), (ks: PSVec[Expr]) => Expr.Add(ks(0), ks(1)))
    val built: Expr = Optics.ana(buildCoalg).reverseGet(3)
    (Optics.cata(evalAlg).get(built) == 4.0) must beTrue
  }

  "cata-as-Getter is stack-safe at depth 10^6 (Plated-driven machine)" >> {
    var e: Expr = Expr.Lit(0.0)
    var i = 0
    while i < 1_000_000 do
      e = Expr.Neg(e)
      i += 1
    val depthAlg: (Expr, PSVec[Int]) => Int = (node, kids) =>
      node match
        case Expr.Lit(_)    => 0
        case Expr.Neg(_)    => kids(0) + 1
        case Expr.Add(_, _) => math.max(kids(0), kids(1)) + 1
        case Expr.Mul(_, _) => math.max(kids(0), kids(1)) + 1
    (Optics.cata(depthAlg).get(e) == 1_000_000) must beTrue
  }
