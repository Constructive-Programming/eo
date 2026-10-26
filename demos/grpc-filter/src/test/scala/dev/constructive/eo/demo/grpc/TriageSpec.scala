package dev.constructive.eo.demo.grpc

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray, WriterConfig}
import dev.constructive.eo.jsoniter.JsonSlice
import dev.constructive.eo.laws.OptionalLaws
import dev.constructive.eo.laws.discipline.OptionalTests
import dev.constructive.eo.optics.Optic.*
import io.github.martinhh.derived.scalacheck.{deriveArbitrary, deriveCogen}
import org.scalacheck.{Arbitrary, Cogen, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** Differential: reading a message through the optics must judge it exactly as decoding it does. */
class TriageSpec extends Specification with ScalaCheck with Discipline:

  import Issues.given

  private val issues = Issues(seed = 11).take(3000).toVector
  private val messages = issues.map(writeToArray(_))

  /** Message `i`, read in place between its neighbours' bytes. */
  private def between(i: Int): JsonSlice =
    val (before, message, after) = (messages(i - 1), messages(i), messages(i + 1))
    JsonSlice(before ++ message ++ after, before.length, message.length)

  private def agree(policy: Policy)(message: Array[Byte]) =
    Triage.optics(policy)(JsonSlice(message)) === Triage.decoding(policy)(JsonSlice(message))

  // Derived like the issues: any sets of severities, tiers, modules and plans, any vote threshold.
  given Arbitrary[Policy] = deriveArbitrary

  "an issue has 64 fields" >> (issues.head.productArity === 64)

  "the optics judge every message as decoding it does" >> {
    "under the default policy, on 3 000 issues the size the stream's are" >>
      forall(messages)(agree(Policy.default))
    "under any policy, on 300 of them" >>
      Prop.forAll((policy: Policy) => forall(messages.take(300))(agree(policy)))
    // From size 2 up: below that the derived enums have no case left to draw.
    "under any policy, on any issue of any size" >>
      Prop
        .forAll((policy: Policy, issue: Issue) => agree(policy)(writeToArray(issue)))
        .set(minTestsOk = 500, minSize = 2, maxSize = 40)
  }

  "every kind is both kept and dropped, so every branch of the policy is exercised" >> {
    val verdicts = messages.map(m => Triage.optics(Policy.default)(JsonSlice(m)))
    forall(Kind.values.toSeq) { kind =>
      verdicts.filter(_.kind.contains(kind)).map(_.keep).distinct must contain(exactly(true, false))
    }
  }

  "the detail the optics slice out is the detail's own JSON" >>
    forall(issues.zip(messages)) { (issue, message) =>
      Wire
        .detail
        .getOption(JsonSlice(message))
        .map(d => readFromArray[Detail](d.bytes)) must beSome(
        issue.detail
      )
    }

  "the detail is a view of the message's own array, not a copy: it sees the array change" >>
    forall(messages.take(300)) { message =>
      val array = message.clone
      val detail = Wire.detail.getOption(JsonSlice(array))
      val kept = detail.map(_.bytes) // a copy, in an array of its own
      java.util.Arrays.fill(array, '0'.toByte)
      (detail.map(_.bytes.forall(_ == '0')) must beSome(true))
        .and(kept.map(_.exists(_ != '0')) must beSome(true))
    }

  "`detail andThen severity` is one optic from a message to its severity, with plain `andThen`" >> {
    val severityOf: Focus[JsonSlice, Severity] = Wire.detail.andThen(Wire.severity)
    val bumped = Severity.values.last
    forall(issues.zip(messages)) { (issue, message) =>
      val slice = JsonSlice(message)
      issue.detail match
        case incident: Detail.Incident =>
          val written = Issue.decode(severityOf.replace(bumped)(slice))
          (severityOf.getOption(slice) must beSome(incident.severity))
            .and(written.map(_.detail) must beSome(incident.copy(severity = bumped): Detail))
            .and((written.map(_.copy(detail = incident)) == Some(issue)) must beTrue)
        // The leaf trusts the classification, but another kind has no `severity` to find.
        case _ => severityOf.getOption(slice) must beNone
    }
  }

  "a message read in place, between its neighbours' bytes, is judged as the message alone" >>
    forall(1 until 300) { i =>
      val (message, inPlace) = (messages(i), between(i))
      (Triage.optics(Policy.default)(inPlace) === Triage.optics(Policy.default)(JsonSlice(message)))
        .and(
          Triage.decoding(Policy.default)(inPlace) === Triage.decoding(Policy.default)(
            JsonSlice(message)
          )
        )
    }

  "whitespace between the tokens changes nothing" >> {
    val pretty = issues.take(200).map(writeToArray(_, WriterConfig.withIndentionStep(2)))
    forall(pretty)(agree(Policy.default))
  }

  "messages the policy can't judge are dropped, unclassified" >> {
    val request = messages(issues.indexWhere(_.detail.isInstanceOf[Detail.Request]))
    def edited(from: String, to: String) =
      JsonSlice(new String(request, "UTF-8").replace(from, to).getBytes("UTF-8"))
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
      val junk = JsonSlice("{\"id\":".getBytes("UTF-8"))
      Triage.optics(Policy.default)(junk) === unclassified
      Triage.decoding(Policy.default)(junk) === unclassified
    }
  }

  // The model's optics, which the baseline and these tests read through, are lawful.
  given Arbitrary[Severity] = deriveArbitrary
  given Arbitrary[Tier] = deriveArbitrary
  given Arbitrary[Plan] = deriveArbitrary
  given Cogen[Severity] = deriveCogen
  given Cogen[Tier] = deriveCogen
  given Cogen[Plan] = deriveCogen
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

  // So is the wire's chain from a message to a leaf, read in place between its neighbours' bytes.
  // Severities drawn directly: the derived enum generator can't draw at size 0, where a property
  // starts, and these foci are leaves.
  checkAll(
    "Wire.detail.andThen(Wire.severity)",
    new OptionalTests[JsonSlice, Severity]:
      val laws = new OptionalLaws[JsonSlice, Severity]:
        val optional = Wire.detail.andThen(Wire.severity)
    .optional(using
      Arbitrary(Gen.choose(1, messages.size - 2).map(between)),
      Arbitrary(Gen.oneOf(Severity.values.toSeq)),
    ),
  )
