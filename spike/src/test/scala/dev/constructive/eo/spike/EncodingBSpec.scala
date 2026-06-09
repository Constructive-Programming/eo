package dev.constructive.eo.spike

import dev.constructive.eo.spike.EffectfulDemo.{GameF, given}
import org.specs2.mutable.Specification

/** Stage 2: encoding B (typed descriptor) on the same effectful example as encoding A.
  *
  * B uses a typed, named coalgebra/algebra over the `GameF` pattern functor — exactly
  * arbo's `optionsCoalgebra` / `optionsAlgebra` shape — and (via the `Traverse`-adapter in
  * `Schemes.hyloM_B`) desugars to the encoding-A machine, so it inherits A's stack-safety
  * and monad-generality. The cost of B over A is purely defining `GameF` + its
  * `Functor`/`Traverse` (reused here from `EffectfulDemo`); the payoff is the typed fold.
  */
class EncodingBSpec extends Specification:

  private type Eth[A] = Either[String, A]

  // Encoding B: typed coalgebra (Seed => M[F[Seed]]) + typed, named algebra (F[A] => A).
  private def coalgB(n: Int): Eth[GameF[Int]] =
    if n == 13 then Left("blocked at 13")
    else if n <= 1 then Right(GameF.Tip(1))
    else Right(GameF.Branch(s"n$n", List(n - 1, n - 2)))

  private val algB: GameF[Int] => Int =
    case GameF.Tip(s)        => s
    case GameF.Branch(_, ks) => ks.sum

  private def runB(n: Int): Eth[Int] =
    Schemes.hyloM_B[Eth, GameF, Int, Int](coalgB, algB)(n)

  private val okSeeds = List(0, 1, 2, 3, 4, 5, 6, 7, 10)

  "encoding B agrees with encoding A on the effectful example (success path)" >> {
    okSeeds.forall(n => runB(n) == EffectfulDemo.eo(n)) must beTrue
  }

  "encoding B threads the effect (deep-ish failure) identically to A" >> {
    (runB(14) == EffectfulDemo.eo(14)) && (runB(14) == Left("blocked at 13")) must beTrue
  }

  // B inherits A's stack-safety (it desugars to `hyloM`): a GameF spine at depth 10^6.
  private def spineCoalgB(n: Int): Eth[GameF[Int]] =
    if n <= 0 then Right(GameF.Tip(0))
    else Right(GameF.Branch("", List(n - 1)))
  private val spineAlgB: GameF[Int] => Int =
    case GameF.Tip(s)        => s
    case GameF.Branch(_, ks) => ks.head + 1

  "encoding B is stack-safe at depth 10^6 (inherited from the encoding-A machine)" >> {
    (Schemes.hyloM_B[Eth, GameF, Int, Int](spineCoalgB, spineAlgB)(1_000_000) == Right(1_000_000)) must beTrue
  }
