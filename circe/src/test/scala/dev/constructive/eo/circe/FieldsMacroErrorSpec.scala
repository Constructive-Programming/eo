package dev.constructive.eo.circe

import scala.compiletime.testing.typeCheckErrors

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec
import org.specs2.mutable.Specification

/** Compile-error catalogue for `.fields` / traversal `.fields` (D10). Every row of the plan's D10
  * table has an exact-message test here, exercised via `scala.compiletime.testing.typeCheckErrors`.
  *
  * The catalogue covers:
  *   - Empty varargs / single-selector varargs (arity ≥ 2 rule).
  *   - Non-case-class parent type.
  *   - Selector-is-not-a-field (e.g. `_.name.toUpperCase`).
  *   - Nested-path selectors (`_.address.street`).
  *   - Unknown field name.
  *   - Duplicate selectors.
  *   - NamedTuple codec unreachable (no given `Codec.AsObject[NT]` in scope).
  *
  * '''2026-04-29 consolidation.''' 9 → 1 named composite block; every D10 row still asserts its
  * exact-substring diagnostic. The compiletime `typeCheckErrors` literal-string requirement forces
  * each snippet to stay distinct, only the spec-frame collapses.
  */
class FieldsMacroErrorSpec extends Specification:

  import JsonSpecFixtures.*
  import FieldsMacroErrorSpec.*

  // covers: empty varargs -> "requires at least two field selectors",
  //   single-selector -> arity message + ".field(_.x)" suggestion,
  //   selector invoking a method -> "selector at position 0 must be a single-field accessor",
  //   nested path selector -> single-field-accessor + "Nested paths" hint,
  //   unknown field name -> "'nope' is not a field of" + "Known fields:",
  //   duplicate selectors -> "duplicate field selector 'name'" + "positions 0, 1",
  //   non-case-class parent -> "has no case fields",
  //   NamedTuple codec unreachable -> "no given Encoder" or "no given Decoder",
  //   JsonTraversal.fields arity-1 -> "JsonTraversal.fields" + arity message
  "`.fields` D10 catalogue: every row's exact diagnostic surfaces" >> {
    val empty = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields()
      """)
    val emptyOk = empty.exists(_.message.contains("requires at least two field selectors"))

    val single = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.name)
      """)
    val singleOk = single.exists(e =>
      e.message.contains("requires at least two field selectors") &&
        e.message.contains(".field(_.x)")
    )

    val method = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.name.toUpperCase, _.age)
      """)
    val methodOk =
      method.exists(_.message.contains("selector at position 0 must be a single-field accessor"))

    val nested = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.address.street, _.name)
      """)
    val nestedOk = nested.exists(e =>
      e.message.contains("must be a single-field accessor") &&
        e.message.contains("Nested paths")
    )

    val unknown = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.name, _.nope)
      """)
    val unknownOk = unknown.exists(e =>
      e.message.contains("'nope' is not a field of") && e.message.contains("Known fields:")
    )

    val dup = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.name, _.name)
      """)
    val dupOk = dup.exists(e =>
      e.message.contains("duplicate field selector 'name'") &&
        e.message.contains("positions 0, 1")
    )

    val notCase = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.NotCase
        given io.circe.Encoder[NotCase] = ???
        given io.circe.Decoder[NotCase] = ???
        codecPrism[NotCase].fields(_.name, _.age)
      """)
    val notCaseOk = notCase.exists(_.message.contains("has no case fields"))

    val noCodec = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.NoCodec
        codecPrism[NoCodec].fields(_.a, _.b)
      """)
    val noCodecOk = noCodec.exists(e =>
      e.message.contains("no given Encoder") || e.message.contains("no given Decoder")
    )

    val travArity = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.FieldsMacroErrorSpec.Basket
        codecPrism[Basket].items.each.fields(_.name)
      """)
    val travArityOk = travArity.exists(e =>
      e.message.contains("JsonTraversal.fields") &&
        e.message.contains("requires at least two field selectors")
    )

    (emptyOk === true)
      .and(singleOk === true)
      .and(methodOk === true)
      .and(nestedOk === true)
      .and(unknownOk === true)
      .and(dupOk === true)
      .and(notCaseOk === true)
      .and(noCodecOk === true)
      .and(travArityOk === true)
  }

object FieldsMacroErrorSpec:

  // Common ADTs live in `JsonSpecFixtures`; the spec-specific
  // `NotCase` / `NoCodec` types stay here.

  // Non-case-class sample — no case fields. Used by the "non-case-class"
  // negative test. Encoder/Decoder are `???`'d in the test's source
  // string since compilation aborts before any runtime reference.
  class NotCase(val name: String, val age: Int)

  // A case class WITH case fields but where the synthesised NamedTuple
  // over a sub-selection has no Codec in scope — exercises the
  // "no given Encoder[NT] / Decoder[NT]" diagnostic without polluting
  // the general import scope.
  case class NoCodec(a: Int, b: String)

  object NoCodec:
    given Codec.AsObject[NoCodec] = KindlingsCodecAsObject.derive
