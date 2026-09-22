package dev.constructive.eo.avro.vulcan

import scala.compiletime.testing.typeCheckErrors

import org.specs2.mutable.Specification

/** Compile-error catalogue for `AvroVulcan.recordBuilder` — the two refusals the macro owns (the
  * rest are construction-time, covered by [[WholeRecordBuilderSpec]]). Substring-matched like
  * `FieldsMacroErrorSpec`, so message tweaks don't ripple.
  */
class WholeRecordBuilderMacroErrorSpec extends Specification:

  "a leaf field whose type has no vulcan codec names the field and the fallback rule" in {
    val errs = typeCheckErrors(
      "import dev.constructive.eo.avro.vulcan.*\n"
        + "AvroVulcan.recordBuilder[WithNoCodec]"
    )
    (
      errs.exists(_.message.contains("has no given vulcan.Codec"))
        && errs.exists(_.message.contains("'opaque'"))
    ) must beTrue
  }

  "a non-case-class root refuses at compile time" in {
    val errs = typeCheckErrors(
      "import dev.constructive.eo.avro.vulcan.*\n"
        + "AvroVulcan.recordBuilder[List[Int]]"
    )
    errs.exists(_.message.contains("is not a case class")) must beTrue
  }
