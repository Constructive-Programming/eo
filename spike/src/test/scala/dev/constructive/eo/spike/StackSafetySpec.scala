package dev.constructive.eo.spike

import dev.constructive.eo.data.PSVec
import org.specs2.mutable.Specification

/** Stage 1 (U1c): the win that justifies a scheme over a hand-written recursion — eo's
  * synchronous-machine `hylo` completes a depth-10⁶ unfold (crossing well past any call-stack
  * limit) where the naive recursion `StackOverflow`s.
  */
class StackSafetySpec extends Specification:

  // Linear spine of the given depth; the combiner counts depth (node payload via capture).
  private val depthCoalg: Schemes.CoalgA[Int, Int] = n =>
    if n <= 0 then (PSVec.empty[Int], (_: PSVec[Int]) => 0)
    else (PSVec.singleton(n - 1), (ks: PSVec[Int]) => ks(0) + 1)

  private val deep = 1_000_000

  "eo stack-safe hylo completes a depth-10^6 unfold" >> {
    (Schemes.hylo(depthCoalg)(deep) == deep) must beTrue
  }

  "the naive (hand-written) recursion overflows at that depth — the baseline eo beats" >> {
    val overflowed =
      try
        Schemes.hyloNaive(depthCoalg)(deep)
        false
      catch case _: StackOverflowError => true
    overflowed must beTrue
  }

  "both agree on a shallow depth (correctness of the machine)" >> {
    (0 to 50).forall(n =>
      Schemes.hylo(depthCoalg)(n) == Schemes.hyloNaive(depthCoalg)(n)
    ) must beTrue
  }
