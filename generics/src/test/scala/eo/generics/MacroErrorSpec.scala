package eo
package generics

import scala.compiletime.testing.typeCheckErrors
import scala.language.implicitConversions

import eo.generics.samples.{Employee, NestedWrapper, NotACaseClass, Person, Widget}

import org.specs2.mutable.Specification

/** Spec covering the D6 error-message catalogue for the varargs `lens[S](...)` macro.
  *
  * Every row of the table in `docs/plans/2026-04-23-003-feat-generics-multi-field-lens-plan.md`
  * gets an exact-substring assertion below, exercised through
  * `scala.compiletime.testing.typeCheckErrors` so the diagnostic comes from the macro at compile
  * time rather than from a failed runtime call.
  *
  * All messages share the `lens[${Type.prettyPrint[S]}]:` prefix for grep-ability, matching the
  * wording the single-selector macro already used.
  *
  * `typeCheckErrors` requires a statically-known String literal at the call site, so the snippet
  * below is inlined per case rather than pulled through a helper.
  */
class MacroErrorSpec extends Specification:

  // Suppress unused-import warnings on the sample types — they're
  // genuinely used, but only inside the typeCheckErrors string
  // literals, which `-Wunused` can't see into. Referencing each one
  // once here makes the warnings vanish without weakening coverage.
  private val _samplesUsed: (Person, Employee, NotACaseClass, Widget, NestedWrapper) =
    null.asInstanceOf

  private def anyMatches(msgs: List[String], substring: String): Boolean =
    msgs.exists(_.contains(substring))

  "empty varargs is rejected" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Person; lens[Person]()"
    ).map(_.message)
    anyMatches(msgs, "lens[eo.generics.samples.Person]") must beTrue
    anyMatches(msgs, "requires at least one field selector") must beTrue
  }

  "non-case-class source type is rejected via Hearth's CaseClass.parse" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.NotACaseClass;"
        + " lens[NotACaseClass](_.count)"
    ).map(_.message)
    anyMatches(msgs, "lens[eo.generics.samples.NotACaseClass]") must beTrue
  }

  "non-field selector (method call rather than field access) is rejected" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Person;"
        + " lens[Person](_.name.toUpperCase)"
    ).map(_.message)
    anyMatches(msgs, "lens[eo.generics.samples.Person]") must beTrue
    anyMatches(msgs, "selector at position 0 must be a single-field accessor") must beTrue
    anyMatches(msgs, "Nested paths") must beTrue
  }

  "non-field selector at a non-first position reports the correct position" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Employee;"
        + " lens[Employee](_.id, _.name.toUpperCase)"
    ).map(_.message)
    anyMatches(msgs, "selector at position 1 must be a single-field accessor") must beTrue
  }

  "unknown field selector is rejected with the list of known fields" >> {
    // `Widget` exposes a non-constructor member `bogus`. `_.bogus`
    // type-checks (it's just a plain method), so the macro reaches
    // its known-fields check and produces the "not a field"
    // diagnostic. The wording is identical to the single-selector
    // macro (R2 semantic parity).
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Widget;"
        + " lens[Widget](_.name, _.bogus)"
    ).map(_.message)
    anyMatches(msgs, "'bogus' is not a field of eo.generics.samples.Widget") must beTrue
    anyMatches(msgs, "Known fields: name, size") must beTrue
  }

  "duplicate selectors are rejected with their positions" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Employee;"
        + " lens[Employee](_.id, _.name, _.id)"
    ).map(_.message)
    anyMatches(msgs, "duplicate field selector 'id'") must beTrue
    anyMatches(msgs, "positions 0, 2") must beTrue
    anyMatches(msgs, "Each field may appear at most once") must beTrue
  }

  "nested path selector is rejected with the nested-paths hint" >> {
    // `_.inner.count` is a two-step selector chain. `extractFieldName`
    // requires the Select's receiver to be the lambda parameter
    // (an `Ident`), so a nested chain falls through to the
    // "single-field accessor" diagnostic that advertises nested-path
    // support as a Future Considerations item.
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.NestedWrapper;"
        + " lens[NestedWrapper](_.inner.count)"
    ).map(_.message)
    anyMatches(msgs, "lens[eo.generics.samples.NestedWrapper]") must beTrue
    anyMatches(msgs, "selector at position 0 must be a single-field accessor") must beTrue
    anyMatches(msgs, "Nested paths") must beTrue
    anyMatches(msgs, "`_.a.b`") must beTrue
  }

  "three-way duplicate selector lists every offending position" >> {
    val msgs = typeCheckErrors(
      "import eo.generics.lens; import eo.generics.samples.Employee;"
        + " lens[Employee](_.name, _.name, _.id, _.name)"
    ).map(_.message)
    anyMatches(msgs, "duplicate field selector 'name'") must beTrue
    anyMatches(msgs, "positions 0, 1, 3") must beTrue
  }
