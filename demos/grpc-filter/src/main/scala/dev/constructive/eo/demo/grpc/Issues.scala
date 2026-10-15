package dev.constructive.eo.demo.grpc

import io.github.martinhh.derived.scalacheck.deriveArbitrary
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

/** The stream's content: ScalaCheck generators for issues, derived from the model by
  * scalacheck-derived, one per case class and enum, from their fields and cases. A third of the
  * issues are of each kind. Only the leaves are written by hand: text, numbers JSON can carry, and
  * the two opaque types.
  */
object Issues:

  private def words(spaced: String): Vector[String] = spaced.split(' ').toVector

  private val word = Gen.oneOf(
    words(
      "checkout latency timeout retry customer invoice cache deploy rollback queue partition " +
        "replica token session webhook refund ledger balance report export import search index " +
        "shard throttle quota alert page dashboard metric trace span error warning config flag " +
        "release build"
    )
  )

  private val unusual = Gen.oneOf(
    words(
      "\"quoted\" back\\slash line\nbreak tab\there </script> \u0001control " +
        "café Grüße naïve 漢字 日本語 🚀 emoji👍"
    )
  )

  /** The modules a stack frame can be in. */
  val modules: Vector[Module] =
    words("payments ledger checkout search auth notifications catalog inventory shipping analytics")
      .map(Module(_))

  /** Up to `size` words. About one in twenty is text JSON must escape (quotes, backslashes, line
    * breaks) or non-ASCII, so whatever reads the bytes meets them.
    */
  given Arbitrary[String] = Arbitrary(
    Gen
      .sized(n => Gen.choose(0, n))
      .flatMap(Gen.listOfN(_, Gen.frequency(19 -> word, 1 -> unusual)))
      .map(_.mkString(" "))
  )

  /** JSON has no NaN or infinities. */
  given Arbitrary[Double] = Arbitrary(Gen.choose(0.0, 1e6))

  given Arbitrary[Module] = Arbitrary(Gen.oneOf(modules))
  given Arbitrary[Votes] = Arbitrary(Gen.choose(0, 200).map(Votes(_)))
  given Arbitrary[Detail] = deriveArbitrary
  given Arbitrary[Issue] = deriveArbitrary

  /** The size the stream's issues are drawn at: up to this many words in a text, and elements in a
    * list. About 4 KB of JSON per issue.
    */
  val Size = 7

  /** Issues drawn from `seed`, at [[Size]]: the same seed gives the same issues. Each issue gets a
    * seed of its own (`slide`): consecutive seeds (`next`) would draw overlapping values.
    */
  def apply(seed: Long): Iterator[Issue] =
    val parameters = Gen.Parameters.default.withSize(Size)
    Iterator
      .iterate(Seed(seed))(_.slide)
      .map(Arbitrary.arbitrary[Issue].pureApply(parameters, _))
