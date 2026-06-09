package dev.constructive.eo.spike

import cats.Eval
import dev.constructive.eo.data.PSVec
import org.specs2.mutable.Specification

/** Closes the last gap vs droste: one generic `hyloM` threads an ARBITRARY lawful `Monad[M]`
  * stack-safely (via the law-required stack-safe `tailRecM`), not just a special monad.
  * Demonstrated for two unrelated monads — `Eval` and `Either` — both at depth 10⁶. (droste's
  * `hyloM` is stack-safe only when `M` itself is; this is stack-safe for any `M`.)
  */
class GeneralEffectSpec extends Specification:

  private val deep = 1_000_000

  // --- M = Eval (deferred effect) ---
  private val evalSpine: Schemes.CoalgM[Eval, Int, Int] = n =>
    if n <= 0 then Eval.now((PSVec.empty[Int], (_: PSVec[Int]) => 0))
    else Eval.now((PSVec.singleton(n - 1), (ks: PSVec[Int]) => ks(0) + 1))

  "generic hyloM threads Eval stack-safely at depth 10^6" >> {
    (Schemes.hyloM(evalSpine)(deep).value == deep) must beTrue
  }

  // --- M = Either (fail-able effect) — same generic hyloM ---
  private def eitherSpine(blockAt: Int): Schemes.CoalgM[[X] =>> Either[String, X], Int, Int] = n =>
    if n == blockAt then Left(s"blocked at $blockAt")
    else if n <= 0 then Right((PSVec.empty[Int], (_: PSVec[Int]) => 0))
    else Right((PSVec.singleton(n - 1), (ks: PSVec[Int]) => ks(0) + 1))

  "generic hyloM threads Either stack-safely at depth 10^6 (success)" >> {
    (Schemes.hyloM(eitherSpine(blockAt = -1))(deep) == Right(deep)) must beTrue
  }

  "generic hyloM threads a deep Either failure (500k down) correctly" >> {
    (Schemes.hyloM(eitherSpine(blockAt = 500_000))(deep) == Left("blocked at 500000")) must beTrue
  }

  "generic hyloM agrees with the Either-specialised machine at shallow depth" >> {
    (0 to 50).forall { n =>
      val general = Schemes.hyloM(eitherSpine(blockAt = -1))(n)
      val special = Schemes.hyloEither((m: Int) =>
        if m <= 0 then Right((PSVec.empty[Int], (_: PSVec[Int]) => 0))
        else Right((PSVec.singleton(m - 1), (ks: PSVec[Int]) => ks(0) + 1))
      )(n)
      general == special
    } must beTrue
  }
