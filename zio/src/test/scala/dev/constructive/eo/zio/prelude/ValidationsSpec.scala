package dev.constructive.eo
package zio
package prelude

import _root_.zio.Chunk
import _root_.zio.prelude.{Validation, ZValidation}
import org.specs2.mutable.Specification

class ValidationsSpec extends Specification:

  val okV: Validation[String, Int] = Validation.succeed(21)

  val bad: Validation[String, Int] = Validation.validate(
    Validation.fail("first"),
    Validation.fail("second"),
  ) match
    case ZValidation.Failure(w, es) => ZValidation.Failure(w, es)
    case s                          => sys.error(s"unexpected $s")

  // Non-empty logs: the whole reason `success` is an Optional rather than a Prism is that the log
  // must ride along, so every claim about it needs a fixture where dropping it is observable.
  val loggedOk: ZValidation[String, String, Int] =
    ZValidation.Success(Chunk("warn-a", "warn-b"), 21)

  val loggedBad: ZValidation[String, String, Int] =
    ZValidation.Failure(Chunk("warn-a"), _root_.zio.NonEmptyChunk("first", "second"))

  "Validations.success" should {
    "read and rewrite the success value" >> {
      val s = Validations.success[Nothing, String, Int]
      (s.getOption(okV) === Some(21)).and(s.modify(_ * 2)(okV) === Validation.succeed(42))
    }
    "pass writes through on failure" >> {
      Validations.success[Nothing, String, Int].modify(_ * 2)(bad) === bad
    }
    "preserve a non-empty log across writes" >> {
      val out = Validations.success[String, String, Int].modify(_ * 2)(loggedOk)
      (out === ZValidation.Success(Chunk("warn-a", "warn-b"), 42))
        .and(out.getLog === Chunk("warn-a", "warn-b"))
    }
  }

  "Validations.eachFailure" should {
    "translate every accumulated error — optic mapError" >> {
      val upper = Validations.eachFailure[Nothing, String, String, Int].modify(_.toUpperCase)(bad)
      upper match
        case ZValidation.Failure(_, es) => es.toChunk.toList === List("FIRST", "SECOND")
        case s                          => ko(s"unexpected $s")
    }
    "preserve a non-empty log while translating errors" >> {
      Validations.eachFailure[String, String, String, Int].modify(_.toUpperCase)(loggedBad) ===
        ZValidation.Failure(Chunk("warn-a"), _root_.zio.NonEmptyChunk("FIRST", "SECOND"))
    }
    "fold every accumulated error" >> {
      Validations.eachFailure[String, String, String, Int].foldMap(identity)(loggedBad) ===
        "firstsecond"
    }
    "have zero foci on success" >> {
      (Validations.eachFailure[Nothing, String, String, Int].foldMap(identity)(okV) === "")
        .and(
          Validations.eachFailure[Nothing, String, String, Int].modify(_.toUpperCase)(okV) === okV
        )
        .and(
          Validations.eachFailure[String, String, String, Int].modify(_.toUpperCase)(loggedOk) ===
            loggedOk
        )
    }
  }
