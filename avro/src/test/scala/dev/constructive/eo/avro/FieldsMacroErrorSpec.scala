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
  */
class FieldsMacroErrorSpec extends Specification:

  import FieldsMacroErrorSpec.*

  // ---- Arity --------------------------------------------------------

  "`.fields` requires at least two selectors" should {

    "empty varargs -> compile error with the arity message" >> {
      val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields()
      """)
      errors.exists(e => e.message.contains("requires at least two field selectors")) === true
    }

    "single-selector varargs -> suggests .field(_.x) instead" >> {
      val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name)
      """)
      errors.exists(e =>
        e.message.contains("requires at least two field selectors") &&
          e.message.contains(".field(_.x)")
      ) === true
    }
  }

  // ---- Selector shape ----------------------------------------------

  "`.fields` rejects non-single-field selectors" should {

    "selector invoking a method -> \"not yet supported\" message with position" >> {
      val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name.toUpperCase, _.age)
      """)
      errors.exists(e =>
        e.message.contains("selector at position 0 must be a single-field accessor")
      ) === true
    }

    "nested path selector -> same message (recommends chaining .field)" >> {
      val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.Outer
        codecPrism[Outer].fields(_.inner.value, _.tag)
      """)
      errors.exists(e =>
        e.message.contains("must be a single-field accessor") &&
          e.message.contains("Nested paths")
      ) === true
    }
  }

  // ---- Unknown field -----------------------------------------------

  "`.fields` rejects unknown field names" in {
    val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name, _.nope)
      """)
    errors.exists(e =>
      e.message.contains("'nope' is not a field of") &&
        e.message.contains("Known fields:")
    ) === true
  }

  // ---- Duplicates --------------------------------------------------

  "`.fields` rejects duplicate selectors" in {
    val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].fields(_.name, _.name)
      """)
    errors.exists(e =>
      e.message.contains("duplicate field selector 'name'") &&
        e.message.contains("positions 0, 1")
    ) === true
  }

  // ---- Non-case-class ----------------------------------------------
  //
  // Using a class without case-class shape — `FieldsMacroErrorSpec.NotCase`
  // is declared without the `case` modifier; it has no `caseFields`.

  "`.fields` rejects non-case-class parent types" in {
    val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.{AvroCodec, codecPrism}
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.NotCase
        given AvroCodec[NotCase] = ???
        codecPrism[NotCase].fields(_.name, _.age)
      """)
    errors.exists(e => e.message.contains("has no case fields")) === true
  }

  // ---- NamedTuple codec unreachable --------------------------------

  "`.fields` aborts when NamedTuple codec is not summonable" in {
    // `NoCodec` has a hand-written `AvroCodec[NoCodec]` (so the parent prism is constructible)
    // but one of its fields is `Opaque` — a type for which no `AvroSchemaFor` (and no other
    // kindlings typeclasses) are derivable. The macro asks for `AvroCodec[NamedTuple[("a","b"),
    // (Int, Opaque)]]`; `AvroCodec.derived` chains into kindlings' `AvroEncoder` /
    // `AvroDecoder` / `AvroSchemaFor`, kindlings' schema-derivation walks the NT's field types,
    // and bottoms out on `Opaque` with a "schema for ... was not handled by any schema
    // derivation rule" error. The macro's "no given AvroCodec" wording is the cats-eo-side
    // diagnostic; kindlings' wording is the upstream surface — either is acceptable here, both
    // signal "the user needs to derive a codec for the synthesised NamedTuple".
    val errors = typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.FieldsMacroErrorSpec.NoCodec
        codecPrism[NoCodec].fields(_.a, _.b)
      """)
    errors.exists(e =>
      e.message.contains("no given AvroCodec") ||
        e.message.contains("was not handled by any schema derivation rule") ||
        e.message.contains("AvroSchemaFor[dev.constructive.eo.avro.FieldsMacroErrorSpec.Opaque]")
    ) === true
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
