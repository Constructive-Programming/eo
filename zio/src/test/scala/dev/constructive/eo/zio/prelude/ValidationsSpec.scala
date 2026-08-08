package dev.constructive.eo
package zio
package prelude

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

  "Validations.success" should {
    "read and rewrite the success value" >> {
      val s = Validations.success[Nothing, String, Int]
      (s.getOption(okV) === Some(21)).and(s.modify(_ * 2)(okV) === Validation.succeed(42))
    }
    "pass writes through on failure" >> {
      Validations.success[Nothing, String, Int].modify(_ * 2)(bad) === bad
    }
  }

  "Validations.eachFailure" should {
    "translate every accumulated error — optic mapError" >> {
      val upper = Validations.eachFailure[Nothing, String, String, Int].modify(_.toUpperCase)(bad)
      upper match
        case ZValidation.Failure(_, es) => es.toChunk.toList === List("FIRST", "SECOND")
        case s                          => ko(s"unexpected $s")
    }
    "have zero foci on success" >> {
      (Validations.eachFailure[Nothing, String, String, Int].foldMap(identity)(okV) === "")
        .and(
          Validations.eachFailure[Nothing, String, String, Int].modify(_.toUpperCase)(okV) === okV
        )
    }
  }
