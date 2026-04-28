package dev.constructive.eo.avro

import scala.compiletime.testing.typeCheckErrors

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.specs2.mutable.Specification

/** Compile-error catalogue for `AvroPrism.fields` (D10). Every row of the plan's D10 table has an
  * exact-message test here, exercised via `scala.compiletime.testing.typeCheckErrors`.
  *
  * Mirrors `dev.constructive.eo.circe.FieldsMacroErrorSpec` row-for-row; the error messages are
  * deliberately substring-matched (rather than equality-checked) so message-tweaks don't ripple
  * into a wave of test updates as long as the diagnostic anchors stay intact.
  *
  * The catalogue covers:
  *   - Empty varargs / single-selector varargs (arity ≥ 2 rule).
  *   - Non-case-class parent type.
  *   - Selector-is-not-a-field (e.g. `_.name.toUpperCase`).
  *   - Nested-path selectors (`_.a.b`).
  *   - Unknown field name.
  *   - Duplicate selectors.
  *   - NamedTuple AvroCodec unreachable (no given `AvroCodec[NT]` in scope).
  *
  * '''2026-04-29 consolidation.''' 8 → 1 named composite block; every D10 row still asserts.
  */
class FieldsMacroErrorSpec extends Specification:

  import FieldsMacroErrorSpec.*

  // covers: empty varargs -> arity message,
  //   single-selector -> arity + ".field(_.x)" suggestion,
  //   selector-invokes-method -> "selector at position 0 must be a single-field accessor",
  //   nested path selector -> single-field-accessor + "Nested paths" hint,
  //   unknown field -> "'nope' is not a field of" + "Known fields:",
  //   duplicate selectors -> "duplicate field selector 'name'" + "positions 0, 1",
  //   non-case-class parent -> "has no case fields",
  //   NamedTuple codec unreachable -> "no given AvroCodec" or kindlings schema-derivation diagnostic
  "`.fields` D10 catalogue: every row's exact diagnostic surfaces" >> {
    val empty = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields()
      """)
    val emptyOk = empty.exists(_.message.contains("requires at least two field selectors"))

    val single = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name)
      """)
    val singleOk = single.exists(e =>
      e.message.contains("requires at least two field selectors") &&
        e.message.contains(".field(_.x)")
    )

    val method = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name.toUpperCase, _.age)
      """)
    val methodOk =
      method.exists(_.message.contains("selector at position 0 must be a single-field accessor"))

    val nested = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.Outer
        codecPrism[Outer].fields(_.inner.value, _.tag)
      """)
    val nestedOk = nested.exists(e =>
      e.message.contains("must be a single-field accessor") &&
        e.message.contains("Nested paths")
    )

    val unknown = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name, _.nope)
      """)
    val unknownOk = unknown.exists(e =>
      e.message.contains("'nope' is not a field of") && e.message.contains("Known fields:")
    )

    val dup = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name, _.name)
      """)
    val dupOk = dup.exists(e =>
      e.message.contains("duplicate field selector 'name'") &&
        e.message.contains("positions 0, 1")
    )

    val notCase = typeCheckErrors("""
        import dev.constructive.eo.avro.{AvroCodec, codecPrism}
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.NotCase
        given AvroCodec[NotCase] = ???
        codecPrism[NotCase].fields(_.name, _.age)
      """)
    val notCaseOk = notCase.exists(_.message.contains("has no case fields"))

    // `NoCodec` has a hand-written `AvroCodec[NoCodec]` (so the parent prism is constructible)
    // but one of its fields is `Opaque` — a type for which no `AvroSchemaFor` (and no other
    // kindlings typeclasses) are derivable. The macro asks for `AvroCodec[NamedTuple[("a","b"),
    // (Int, Opaque)]]`; `AvroCodec.derived` chains into kindlings' `AvroEncoder` /
    // `AvroDecoder` / `AvroSchemaFor`, kindlings' schema-derivation walks the NT's field types,
    // and bottoms out on `Opaque`. Either the cats-eo-side or the kindlings-side diagnostic is
    // acceptable here.
    val noCodec = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.NoCodec
        codecPrism[NoCodec].fields(_.a, _.b)
      """)
    val noCodecOk = noCodec.exists(e =>
      e.message.contains("no given AvroCodec") ||
        e.message.contains("was not handled by any schema derivation rule") ||
        e.message.contains("AvroSchemaFor[dev.constructive.eo.avro.FieldsMacroErrorSpec.Opaque]")
    )

    (emptyOk === true)
      .and(singleOk === true)
      .and(methodOk === true)
      .and(nestedOk === true)
      .and(unknownOk === true)
      .and(dupOk === true)
      .and(notCaseOk === true)
      .and(noCodecOk === true)
  }

end FieldsMacroErrorSpec

object FieldsMacroErrorSpec:

  // Common ADTs (Person) live in `AvroSpecFixtures`; the spec-specific
  // `NotCase` / `NoCodec` / `Outer` types stay here.

  // Non-case-class sample — no case fields. Used by the "non-case-class"
  // negative test. AvroCodec is `???`'d in the test's source string since
  // compilation aborts before any runtime reference.
  class NotCase(val name: String, val age: Int)

  // A nested case class for the "nested path selector" negative test.
  case class Inner(value: String)

  object Inner:
    given AvroEncoder[Inner] = AvroEncoder.derived
    given AvroDecoder[Inner] = AvroDecoder.derived
    given AvroSchemaFor[Inner] = AvroSchemaFor.derived

  case class Outer(inner: Inner, tag: String)

  object Outer:
    given AvroEncoder[Outer] = AvroEncoder.derived
    given AvroDecoder[Outer] = AvroDecoder.derived
    given AvroSchemaFor[Outer] = AvroSchemaFor.derived

  // An opaque holder for which we deliberately do NOT derive any kindlings typeclasses, so
  // `AvroCodec[Opaque]` is unsummonable.
  class Opaque

  // A case class with one derivable field (`a: Int` — kindlings handles primitives) and one
  // non-derivable field (`b: Opaque`). The macro user supplies a hand-written
  // `AvroCodec[NoCodec]` so the PARENT prism is constructible; kindlings can't auto-derive an
  // `AvroCodec[NamedTuple[("a","b"), (Int, Opaque)]]` because `AvroCodec[Opaque]` doesn't
  // exist — that's the failure the macro reports.
  case class NoCodec(a: Int, b: Opaque)

  object NoCodec:

    given AvroCodec[NoCodec] =
      new AvroCodec[NoCodec]:
        def schema: org.apache.avro.Schema = ???
        def encode(a: NoCodec): Any = ???
        def decodeEither(any: Any): Either[Throwable, NoCodec] = ???

end FieldsMacroErrorSpec
