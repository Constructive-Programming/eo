package dev.constructive.eo.demo.grpc

import scala.language.implicitConversions
import scala.util.Random

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray, WriterConfig}
import dev.constructive.eo.laws.OptionalLaws
import dev.constructive.eo.laws.discipline.OptionalTests
import dev.constructive.eo.optics.Optic.*
import org.scalacheck.{Arbitrary, Cogen, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Differential: reading a message through the optics must judge it exactly as decoding it does. */
class TriageSpec extends Specification with ScalaCheck with Discipline:

  private val issues = Issues(seed = 11).take(3000).toVector
  private val messages = issues.map(writeToArray(_))

  private def agree(policy: Policy)(message: Array[Byte]) =
    Triage.optics(policy)(message) === Triage.decoding(policy)(message)

  private val policies =
    for
      urgent <- Gen.someOf(Severity.values.toSeq)
      tiers <- Gen.someOf(Tier.values.toSeq)
      modules <- Gen.someOf(Issues.modules)
      plans <- Gen.someOf(Plan.values.toSeq)
      minVotes <- Gen.choose(0, 210)
    yield Policy(urgent.toSet, tiers.toSet, modules.toSet, plans.toSet, Votes(minVotes))

  "an issue has 64 fields" >> (issues.head.productArity === 64)

  "the optics judge every message as decoding it does" >> {
    "under the default policy" >> forall(messages)(agree(Policy.default))
    "under any policy" >> Prop.forAll(policies)(policy => forall(messages.take(300))(agree(policy)))
  }

  "every kind is both kept and dropped, so every branch of the policy is exercised" >> {
    val verdicts = messages.map(Triage.optics(Policy.default))
    forall(Kind.values.toSeq) { kind =>
      verdicts.filter(_.kind.contains(kind)).map(_.keep).distinct must contain(exactly(true, false))
    }
  }

  "the detail the optics slice out is the detail's own JSON" >>
    forall(issues.zip(messages)) { (issue, message) =>
      Wire.detail.getOption(message).map(d => readFromArray[Detail](d.bytes)) must beSome(
        issue.detail
      )
    }

  "whitespace between the tokens changes nothing" >> {
    val pretty = issues.take(200).map(writeToArray(_, WriterConfig.withIndentionStep(2)))
    forall(pretty)(agree(Policy.default))
  }

  "messages the policy can't judge are dropped, unclassified" >> {
    val request = messages(issues.indexWhere(_.detail.isInstanceOf[Detail.Request]))
    def edited(from: String, to: String) =
      new String(request, "UTF-8").replace(from, to).getBytes("UTF-8")
    val unclassified = Verdict(None, keep = false)
    "a kind from a newer producer: the optics read on, a decoder rejects the message" >> {
      val task = edited("\"type\":\"Request\"", "\"type\":\"Task\"")
      Triage.optics(Policy.default)(task) === unclassified
      Triage.decoding(Policy.default)(task) === unclassified
    }
    "no detail" >> {
      Triage.optics(Policy.default)(edited("\"detail\":", "\"derail\":")) === unclassified
    }
    "not JSON" >> {
      val junk = "{\"id\":".getBytes("UTF-8")
      Triage.optics(Policy.default)(junk) === unclassified
      Triage.decoding(Policy.default)(junk) === unclassified
    }
  }

  // The model's optics, which the baseline and these tests read through, are lawful.
  given Arbitrary[Detail] = Arbitrary(Arbitrary.arbitrary[Long].map(s => Issues.detail(Random(s))))
  given Arbitrary[Severity] = Arbitrary(Gen.oneOf(Severity.values.toSeq))
  given Arbitrary[Tier] = Arbitrary(Gen.oneOf(Tier.values.toSeq))
  given Arbitrary[Plan] = Arbitrary(Gen.oneOf(Plan.values.toSeq))
  given Arbitrary[Module] = Arbitrary(Gen.oneOf(Issues.modules))
  given Arbitrary[Votes] = Arbitrary(Gen.choose(0, 300).map(Votes(_)))
  given Cogen[Severity] = Cogen[Int].contramap(_.ordinal)
  given Cogen[Tier] = Cogen[Int].contramap(_.ordinal)
  given Cogen[Plan] = Cogen[Int].contramap(_.ordinal)
  given Cogen[Module] = Cogen[String].contramap(_.name)
  given Cogen[Votes] = Cogen[Int].contramap(_.count)

  private def optional[A: Arbitrary: Cogen](name: String, focus: Focus[Detail, A]) =
    checkAll(
      s"Model.$name",
      new OptionalTests[Detail, A]:
        val laws = new OptionalLaws[Detail, A]:
          val optional = focus
      .optional,
    )

  optional("severity", Model.severity)
  optional("tier", Model.tier)
  optional("module", Model.module)
  optional("plan", Model.plan)
  optional("votes", Model.votes)
