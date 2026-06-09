package dev.constructive.eo.spike

import org.specs2.mutable.Specification

/** Proves eo's closure encoding handles the **effectful** fused unfold→fold: the eo
  * version agrees with both droste's `hyloM` and the hand-written recursion, on both the
  * success path (Fibonacci sums) and the failure path (the blocked seed short-circuits to
  * `Left` identically). Refutes the earlier mistaken "eo is pure-only" claim.
  */
class EffectfulDemoSpec extends Specification:

  private val okSeeds: List[Int] = List(0, 1, 2, 3, 4, 5, 6, 7, 10)
  // fib-style: f(n)=f(n-1)+f(n-2), f(0)=f(1)=1 → 1,1,2,3,5,8,13,21,...
  private val expected: Map[Int, Int] =
    Map(0 -> 1, 1 -> 1, 2 -> 2, 3 -> 3, 4 -> 5, 5 -> 8, 6 -> 13, 7 -> 21, 10 -> 89)

  "all three effectful unfolds agree on the success path" >> {
    okSeeds.forall { n =>
      val d = EffectfulDemo.droste(n)
      val e = EffectfulDemo.eo(n)
      val h = EffectfulDemo.hand(n)
      d == Right(expected(n)) && e == d && h == d
    } must beTrue
  }

  "the effect (failure at the blocked seed) propagates identically in all three" >> {
    // seed 14 expands toward 13, which is blocked → Left in every implementation
    val d = EffectfulDemo.droste(14)
    val e = EffectfulDemo.eo(14)
    val h = EffectfulDemo.hand(14)
    (d == Left("blocked at 13")) && e == d && h == d must beTrue
  }
