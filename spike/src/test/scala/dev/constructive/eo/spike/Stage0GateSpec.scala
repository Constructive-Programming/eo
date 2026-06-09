package dev.constructive.eo.spike

import io.circe.Json
import org.specs2.mutable.Specification

/** Stage-0 gate correctness: the hand-written, droste-interop, and eo encoding-A unfolds must
  * produce the SAME `Json` for the same seed. (The ceremony comparison that actually drives the
  * go/no-go decision is recorded in the verdict doc, not asserted here — this spec only proves the
  * three are equivalent so the comparison is apples-to-apples.)
  */
class Stage0GateSpec extends Specification:

  private val seeds: List[(Int, Int)] =
    List((0, 0), (0, 1), (1, 4), (0, 7), (3, 3), (0, 15))

  "the three unfolds agree on every seed" >> {
    seeds.forall {
      case (lo, hi) =>
        val h = DomainUnfold.hand(lo, hi)
        val d = DomainUnfold.droste((lo, hi))
        val e = DomainUnfold.eo((lo, hi))
        h == d && d == e
    } must beTrue
  }

  "a leaf seed produces a bare int" >> {
    (DomainUnfold.eo((5, 5)) == Json.fromInt(5)) must beTrue
  }

  "a two-element range produces a 2-array of leaves" >> {
    (DomainUnfold.droste((0, 1)) == Json.arr(Json.fromInt(0), Json.fromInt(1))) must beTrue
  }
