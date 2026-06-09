package dev.constructive.eo.spike

import dev.constructive.eo.data.PSVec
import org.specs2.mutable.Specification

/** The ultimate proof: **stack-safe AND effectful together** (the full arbo shape). A depth-10⁶
  * unfold whose expansion is a fail-able effect (`Either`, arbo's `M`) completes via eo's heap
  * machine where the naive effectful recursion overflows — and the effect (a deep `Left`) is
  * threaded correctly, which requires reaching that depth stack-safely.
  */
class EffectfulStackSafetySpec extends Specification:

  // Linear effectful spine; fails (Left) at `blockAt` (a negative value never blocks).
  private def spine(blockAt: Int): Schemes.CoalgEither[String, Int, Int] = n =>
    if n == blockAt then Left(s"blocked at $blockAt")
    else if n <= 0 then Right((PSVec.empty[Int], (_: PSVec[Int]) => 0))
    else Right((PSVec.singleton(n - 1), (ks: PSVec[Int]) => ks(0) + 1))

  private val deep = 1_000_000

  "eo stack-safe effectful hylo completes a depth-10^6 unfold (success path)" >> {
    (Schemes.hyloEither(spine(blockAt = -1))(deep) == Right(deep)) must beTrue
  }

  "the naive effectful recursion overflows at that depth — the baseline eo beats" >> {
    val overflowed =
      try
        Schemes.hyloEitherNaive(spine(blockAt = -1))(deep)
        false
      catch case _: StackOverflowError => true
    overflowed must beTrue
  }

  "a Left injected DEEP (500k levels down) is threaded correctly — requires reaching it stack-safely" >> {
    (Schemes.hyloEither(spine(blockAt = 500_000))(deep) == Left("blocked at 500000")) must beTrue
  }

  "eo and naive agree at shallow depth, success and failure" >> {
    val ok = (0 to 50).forall(n =>
      Schemes.hyloEither(spine(blockAt = -1))(n) == Schemes.hyloEitherNaive(spine(blockAt = -1))(n)
    )
    val fail = Schemes.hyloEither(spine(blockAt = 10))(40) ==
      Schemes.hyloEitherNaive(spine(blockAt = 10))(40)
    (ok && fail) must beTrue
  }
