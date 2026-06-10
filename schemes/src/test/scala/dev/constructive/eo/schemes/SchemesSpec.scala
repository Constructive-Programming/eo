package dev.constructive.eo
package schemes

import io.circe.Json
import org.specs2.mutable.Specification

import data.PSVec
import optics.{Getter, Plated, Review, Unfold}
import optics.Optic.* // cross, andThen, get

import generics.plate
import circe.platedJson
import schemes.samples.{Expr, Wrapped}

class SchemesSpec extends Specification:

  private given Plated[Expr] = plate[Expr]

  // ----- algebras / expansions -----

  private val eval: (Expr, PSVec[Double]) => Double = (node, kids) =>
    node match
      case Expr.Lit(v)    => v
      case Expr.Neg(_)    => -kids(0)
      case Expr.Add(_, _) => kids(0) + kids(1)
      case Expr.Mul(_, _) => kids(0) * kids(1)

  // seed n expands to two child seeds (0, n-1) — a right-nested binary spine; n<=0 is a leaf.
  private val expandFib: Int => PSVec[Int] = n =>
    if n <= 0 then PSVec.empty[Int] else PSVec.of(0, n - 1)

  // bundled coalgebra for ana: each seed's child seeds + how to assemble the Expr node.
  // Builds a right-nested Add of (n+1) ones → evaluates to n+1.
  private val buildExprCoalg: Schemes.Coalg[Int, Expr] = n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(1.0))
    else (PSVec.of(0, n - 1), (ks: PSVec[Expr]) => Expr.Add(ks(0), ks(1)))

  // the same computation FUSED (folds to Double directly; no Expr built) — hylo's split form.
  private val fusedFib: (Int, PSVec[Double]) => Double = (n, rs) =>
    if n <= 0 then 1.0 else rs(0) + rs(1)

  private val expr: Expr = Expr.Add(Expr.Lit(1.0), Expr.Mul(Expr.Lit(2.0), Expr.Lit(3.0)))

  "cata is a Getter that folds an Expr to a value" >> {
    val evalG: Getter[Expr, Double] = Schemes.cata(eval)
    (evalG.get(expr) == 7.0) must beTrue
  }

  "cata folds to a result type other than S (A != S)" >> {
    val size: (Expr, PSVec[Int]) => Int = (_, kids) => 1 + kids.toList.sum
    (Schemes.cata(size).get(expr) == 5) must beTrue // Add, Lit, Mul, Lit, Lit
  }

  "cata consumes a pure algebra carried as an Unfold citizen (incl. one built by composition)" >> {
    val sizeAlg: Unfold[Int, Int, PSVec] =
      Unfold.algebra[Int, Int, PSVec](kids => 1 + kids.toList.sum)
    (Schemes.cata[Expr, Int](sizeAlg).get(expr) == 5) must beTrue

    // an algebra assembled by optic composition: Review post-processes each layer's result,
    // so the engine consumes `2 * max(sum(kids), 1)` without that lambda being written anywhere
    val doubled: Unfold[Int, Int, PSVec] =
      Review[Int, Int](_ * 2).andThen(Unfold.algebra[Int, Int, PSVec](_.toList.sum.max(1)))
    // Lit leaf: 2*max(0,1) = 2; Neg node: 2*max(2,1) = 4
    (Schemes.cata[Expr, Int](doubled).get(Expr.Neg(Expr.Lit(1.0))) == 4) must beTrue
  }

  "cata-as-Getter composes onto an outer Getter via andThen" >> {
    val composed: Getter[Wrapped, Double] =
      Getter[Wrapped, Expr](_.expr).andThen(Schemes.cata(eval))
    (composed.get(Wrapped("x", expr)) == 7.0) must beTrue
  }

  "ana is a Review that builds an Expr from a seed" >> {
    val built: Expr = Schemes.ana(buildExprCoalg).reverseGet(3)
    (Schemes.cata(eval).get(built) == 4.0) must beTrue
  }

  "ana.cross(cata) composes build->read (the materializing hylo) via core `cross`" >> {
    val refold =
      Schemes.ana(buildExprCoalg).cross(Schemes.cata(eval)) // Optic[Int,Unit,Double,Unit,Direct]
    (refold.get(3) == 4.0) must beTrue
  }

  "fused hylo folds a seed to a value (no intermediate Expr) and agrees with cata∘ana" >> {
    val h: Getter[Int, Double] = Schemes.hylo(expandFib, fusedFib)
    val viaCross = Schemes.ana(buildExprCoalg).cross(Schemes.cata(eval)).get(3)
    (h.get(3) == 4.0) && (viaCross == 4.0) must beTrue
  }

  "the fused hylo Getter composes further into the pipeline" >> {
    val toStr: Getter[Int, String] =
      Schemes.hylo(expandFib, fusedFib).andThen(Getter[Double, String](_.toString))
    (toStr.get(3) == "4.0") must beTrue
  }

  // The combine indexes children positionally (kids(0) = left, kids(1) = right), so the engine's
  // post-order out-array fill MUST preserve child order. Every other algebra here is commutative
  // (Add/Mul/sum), which would not catch a transposed-children regression — this one is not.
  "cata preserves left-to-right child order (non-commutative algebra)" >> {
    // reinterpret Add as subtraction, Mul as division — both order-sensitive.
    val sub: (Expr, PSVec[Double]) => Double = (node, kids) =>
      node match
        case Expr.Lit(v)    => v
        case Expr.Neg(_)    => -kids(0)
        case Expr.Add(_, _) => kids(0) - kids(1)
        case Expr.Mul(_, _) => kids(0) / kids(1)
    val minus = Expr.Add(Expr.Lit(10.0), Expr.Lit(3.0)) // 10 - 3 = 7, NOT 3 - 10 = -7
    val div = Expr.Mul(Expr.Lit(12.0), Expr.Lit(4.0)) // 12 / 4 = 3, NOT 4 / 12
    ((Schemes.cata(sub).get(minus) == 7.0) && (Schemes.cata(sub).get(div) == 3.0)) must beTrue
  }

  // All sample ADT nodes are arity <= 2; this exercises the n-ary (width 3) expand path through the
  // engine's per-node out-array, with positional weights that expose any mis-ordering.
  "fused hylo folds a WIDE node (3 child seeds), order-sensitive combine" >> {
    val expandWide: Int => PSVec[Int] =
      n => if n <= 0 then PSVec.empty[Int] else PSVec.fromIterable(List(0, 0, 0))
    val combineWide: (Int, PSVec[Int]) => Int =
      (n, rs) => if n <= 0 then 1 else rs(0) + 10 * rs(1) + 100 * rs(2)
    // n=1 → three leaves (each 1) → 1 + 10 + 100 = 111
    (Schemes.hylo(expandWide, combineWide).get(1) == 111) must beTrue
  }

  // ----- circe Plated[Json] (real downstream target) -----

  "cata works over circe's Plated[Json] (sum every number in a document)" >> {
    val sumNumbers: (Json, PSVec[Int]) => Int =
      (j, kids) => j.asNumber.flatMap(_.toInt).getOrElse(0) + kids.toList.sum
    val doc = Json.obj(
      "a" -> Json.fromInt(1),
      "b" -> Json.arr(Json.fromInt(2), Json.fromInt(3)),
      "c" -> Json.fromString("ignored"),
    )
    (Schemes.cata(sumNumbers).get(doc) == 6) must beTrue
  }

  "ana builds a nested circe Json from a seed" >> {
    val buildJsonCoalg: Schemes.Coalg[Int, Json] = n =>
      if n <= 0 then (PSVec.empty[Int], (_: PSVec[Json]) => Json.fromInt(0))
      else (PSVec.singleton(n - 1), (ks: PSVec[Json]) => Json.arr(ks(0)))
    val built = Schemes.ana(buildJsonCoalg).reverseGet(2)
    (built == Json.arr(Json.arr(Json.fromInt(0)))) must beTrue
  }

  // ----- stack-safety (the win over a hand-written one-off) -----

  "cata is stack-safe over a depth-10^6 Neg spine" >> {
    var e: Expr = Expr.Lit(0.0)
    var i = 0
    while i < 1_000_000 do
      e = Expr.Neg(e)
      i += 1
    val depth: (Expr, PSVec[Int]) => Int = (node, kids) =>
      node match
        case Expr.Lit(_) => 0
        case _           => kids(0) + 1
    (Schemes.cata(depth).get(e) == 1_000_000) must beTrue
  }

  "fused hylo is stack-safe at depth 10^6 (no intermediate S built)" >> {
    val expandSpine: Int => PSVec[Int] =
      n => if n <= 0 then PSVec.empty[Int] else PSVec.singleton(n - 1)
    val depthAlg: (Int, PSVec[Int]) => Int = (n, rs) => if n <= 0 then 0 else rs(0) + 1
    (Schemes.hylo(expandSpine, depthAlg).get(1_000_000) == 1_000_000) must beTrue
  }

  "ana's unfold loop is stack-safe (built S is O(depth) heap, not JVM stack)" >> {
    val buildNegCoalg: Schemes.Coalg[Int, Expr] = n =>
      if n <= 0 then (PSVec.empty[Int], (_: PSVec[Expr]) => Expr.Lit(0.0))
      else (PSVec.singleton(n - 1), (ks: PSVec[Expr]) => Expr.Neg(ks(0)))
    val deep: Expr = Schemes.ana(buildNegCoalg).reverseGet(100_000)
    val depth: (Expr, PSVec[Int]) => Int = (node, kids) =>
      node match
        case Expr.Lit(_) => 0
        case _           => kids(0) + 1
    (Schemes.cata(depth).get(deep) == 100_000) must beTrue
  }
