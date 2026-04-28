package dev.constructive.eo
package generics

import scala.compiletime.testing.typeCheckErrors
import scala.language.implicitConversions

import dev.constructive.eo.generics.samples.{Employee, NestedWrapper, NotACaseClass, Person, Widget}
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
  *
  * '''2026-04-29 consolidation.''' 8 → 1 named composite block; each row of the D6 catalogue is
  * still asserted, with a `// covers: ...` reverse-index. Every snippet is still a literal
  * (compiletime constraint), only the spec frame collapses.
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

  // covers: empty varargs rejected with arity message + spec-prefix grep,
  //   non-case-class source rejected via Hearth's CaseClass.parse with spec prefix,
  //   non-field selector (method call) rejected at position 0 with Nested-paths hint,
  //   non-field selector at a non-first position reports correct position index,
  //   unknown field selector lists known fields and the offending name,
  //   duplicate selectors list every offending position with the "appear at most once" hint,
  //   nested path selector falls through to the single-field-accessor diagnostic
  //   with the `_.a.b` Future-Considerations recommendation,
  //   three-way duplicate lists every offending position
  "lens[S] macro error catalogue (D6 rows): every diagnostic surfaces with the right prefix + tail" >> {
    val empty = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Person; lens[Person]()"
    ).map(_.message)
    val emptyOk =
      anyMatches(empty, "lens[dev.constructive.eo.generics.samples.Person]") &&
        anyMatches(empty, "requires at least one field selector")

    val notCase = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.NotACaseClass;"
        + " lens[NotACaseClass](_.count)"
    ).map(_.message)
    val notCaseOk = anyMatches(notCase, "lens[dev.constructive.eo.generics.samples.NotACaseClass]")

    val nonFieldP0 = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Person;"
        + " lens[Person](_.name.toUpperCase)"
    ).map(_.message)
    val nonFieldP0Ok =
      anyMatches(nonFieldP0, "lens[dev.constructive.eo.generics.samples.Person]") &&
        anyMatches(nonFieldP0, "selector at position 0 must be a single-field accessor") &&
        anyMatches(nonFieldP0, "Nested paths")

    val nonFieldP1 = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Employee;"
        + " lens[Employee](_.id, _.name.toUpperCase)"
    ).map(_.message)
    val nonFieldP1Ok =
      anyMatches(nonFieldP1, "selector at position 1 must be a single-field accessor")

    // `Widget` exposes a non-constructor member `bogus`. `_.bogus` type-checks (it's a plain
    // method), so the macro reaches its known-fields check and produces the "not a field"
    // diagnostic. Wording is identical to the single-selector macro (R2 semantic parity).
    val unknown = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Widget;"
        + " lens[Widget](_.name, _.bogus)"
    ).map(_.message)
    val unknownOk =
      anyMatches(unknown, "'bogus' is not a field of dev.constructive.eo.generics.samples.Widget") &&
        anyMatches(unknown, "Known fields: name, size")

    val dup2 = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Employee;"
        + " lens[Employee](_.id, _.name, _.id)"
    ).map(_.message)
    val dup2Ok =
      anyMatches(dup2, "duplicate field selector 'id'") &&
        anyMatches(dup2, "positions 0, 2") &&
        anyMatches(dup2, "Each field may appear at most once")

    // `_.inner.count` is a two-step selector chain. `extractFieldName` requires the Select's
    // receiver to be the lambda parameter (an `Ident`), so a nested chain falls through to the
    // "single-field accessor" diagnostic that advertises nested-path support as a Future
    // Considerations item.
    val nested = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.NestedWrapper;"
        + " lens[NestedWrapper](_.inner.count)"
    ).map(_.message)
    val nestedOk =
      anyMatches(nested, "lens[dev.constructive.eo.generics.samples.NestedWrapper]") &&
        anyMatches(nested, "selector at position 0 must be a single-field accessor") &&
        anyMatches(nested, "Nested paths") &&
        anyMatches(nested, "`_.a.b`")

    val dup3 = typeCheckErrors(
      "import dev.constructive.eo.generics.lens; import dev.constructive.eo.generics.samples.Employee;"
        + " lens[Employee](_.name, _.name, _.id, _.name)"
    ).map(_.message)
    val dup3Ok =
      anyMatches(dup3, "duplicate field selector 'name'") && anyMatches(dup3, "positions 0, 1, 3")

    (emptyOk must beTrue)
      .and(notCaseOk must beTrue)
      .and(nonFieldP0Ok must beTrue)
      .and(nonFieldP1Ok must beTrue)
      .and(unknownOk must beTrue)
      .and(dup2Ok must beTrue)
      .and(nestedOk must beTrue)
      .and(dup3Ok must beTrue)
  }
