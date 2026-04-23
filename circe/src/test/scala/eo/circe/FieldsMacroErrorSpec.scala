package eo.circe

import scala.compiletime.testing.typeCheckErrors

import io.circe.Codec

import hearth.kindlings.circederivation.KindlingsCodecAsObject

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
  */
class FieldsMacroErrorSpec extends Specification:

  import FieldsMacroErrorSpec.*

  // ---- Arity --------------------------------------------------------

  "`.fields` requires at least two selectors" should {

    "empty varargs -> compile error with the arity message" >> {
      val errors = typeCheckErrors("""
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields()
      """)
      errors.exists(e => e.message.contains("requires at least two field selectors")) === true
    }

    "single-selector varargs -> suggests .field(_.x) instead" >> {
      val errors = typeCheckErrors("""
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
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
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.name.toUpperCase, _.age)
      """)
      errors.exists(e =>
        e.message.contains("selector at position 0 must be a single-field accessor")
      ) === true
    }

    "nested path selector -> same message (recommends chaining .field)" >> {
      val errors = typeCheckErrors("""
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
        codecPrism[Person].fields(_.address.street, _.name)
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
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
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
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Person
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
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.NotCase
        given io.circe.Encoder[NotCase] = ???
        given io.circe.Decoder[NotCase] = ???
        codecPrism[NotCase].fields(_.name, _.age)
      """)
    errors.exists(e => e.message.contains("has no case fields")) === true
  }

  // ---- NamedTuple codec unreachable --------------------------------

  "`.fields` aborts when NamedTuple codec is not summonable" in {
    // `NoCodec` has Encoder/Decoder but the NamedTuple synthesised
    // over its fields does NOT have a matching codec in scope.
    val errors = typeCheckErrors("""
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.NoCodec
        codecPrism[NoCodec].fields(_.a, _.b)
      """)
    errors.exists(e =>
      e.message.contains("no given Encoder") ||
        e.message.contains("no given Decoder")
    ) === true
  }

  // ---- Traversal mirror --------------------------------------------
  //
  // One spot-check on the traversal side so the JsonTraversal.fields
  // message prefix is exercised too.

  "`JsonTraversal.fields` rejects arity-1 varargs" in {
    val errors = typeCheckErrors("""
        import eo.circe.codecPrism
        import eo.circe.FieldsMacroErrorSpec.Basket
        codecPrism[Basket].items.each.fields(_.name)
      """)
    errors.exists(e =>
      e.message.contains("JsonTraversal.fields") &&
        e.message.contains("requires at least two field selectors")
    ) === true
  }

object FieldsMacroErrorSpec:

  case class Address(street: String, zip: Int)

  object Address:
    given Codec.AsObject[Address] = KindlingsCodecAsObject.derive

  case class Person(name: String, age: Int, address: Address)

  object Person:
    given Codec.AsObject[Person] = KindlingsCodecAsObject.derive

  case class Order(name: String)

  object Order:
    given Codec.AsObject[Order] = KindlingsCodecAsObject.derive

  case class Basket(owner: String, items: Vector[Order])

  object Basket:
    given Codec.AsObject[Basket] = KindlingsCodecAsObject.derive

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
