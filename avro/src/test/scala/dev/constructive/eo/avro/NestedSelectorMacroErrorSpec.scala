package dev.constructive.eo.avro

import scala.compiletime.testing.typeCheckErrors
import scala.language.implicitConversions

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.specs2.mutable.Specification

// Fixture ADTs at top level (the eo-generics convention for macro-facing test types). The SHAPE is
// the hazard: the inner record and the outer record BOTH carry a field named `y`, so a selector
// that walks one hop too far (`_.inner.y`) parses as the bare name `y` and lands on the OUTER
// record's `y` — a well-typed, lawful optic aimed at the wrong field.
final case class NInner(x: String, y: String)

object NInner:
  given AvroEncoder[NInner] = AvroEncoder.derived
  given AvroDecoder[NInner] = AvroDecoder.derived
  given AvroSchemaFor[NInner] = AvroSchemaFor.derived

final case class NOuter(inner: NInner, y: String):
  // A no-arg member that is NOT a case field — the second half of the selector rung. `_.hashCode`
  // will not do: it is a Java no-arg method, so it eta-expands to `Apply(Select(_, …), Nil)` and is
  // already caught by the single-field-accessor rule.
  def derivedTag: String = y.toUpperCase

object NOuter:
  given AvroEncoder[NOuter] = AvroEncoder.derived
  given AvroDecoder[NOuter] = AvroDecoder.derived
  given AvroSchemaFor[NOuter] = AvroSchemaFor.derived

final case class NBasket(items: List[NInner])

object NBasket:
  given AvroEncoder[NBasket] = AvroEncoder.derived
  given AvroDecoder[NBasket] = AvroDecoder.derived
  given AvroSchemaFor[NBasket] = AvroSchemaFor.derived

/** A NESTED selector must not compile — issue #95's second, independent hazard.
  *
  * `MacroSelectors.extractFieldName` matched `Lambda(_, Select(_, name))` with ANY receiver, so
  * `.field(_.inner.y)` parsed as the single name `"y"` and resolved it on the PARENT record. On
  * `NOuter` above that reads and writes the outer `y` while the call site plainly says "the inner
  * one" — silent corruption on a perfectly 1:1, kindlings-derived codec, with no schema divergence
  * involved at all. The macro's own "nested paths … chain them" abort was unreachable for exactly
  * the shape it was written for.
  *
  * The fix is in the extractor (`extractFieldName` now requires the `Select` receiver to be the
  * lambda parameter), so the same row holds for every cursor macro that shares it — eo-circe's
  * `JsonPrism` / `JsonTraversal` and eo-jsoniter's `JsoniterPrism` / `JsoniterTraversal` carry the
  * mirror of this spec.
  *
  * The second half of the rung is the known-field check: a single-hop selector that names something
  * that is not a case field of the parent (`_.hashCode`) used to be passed through as a literal
  * schema-field name and silently Missed at runtime. It is now a compile error too.
  */
class NestedSelectorMacroErrorSpec extends Specification:

  private def anyMatches(msgs: List[String], fragment: String): Boolean =
    msgs.exists(_.contains(fragment))

  "a nested `.field(_.a.b)` selector is a compile error naming the chained form" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.avro.{codecPrism, NOuter}
        codecPrism[NOuter].field(_.inner.y)
      """).map(_.message)

    (anyMatches(msgs, "AvroPrism.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
      .and(anyMatches(msgs, ".field(_.a).field(_.b)") must beTrue)
  }

  "a nested selector on a TRAVERSAL suffix is a compile error too" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.avro.{codecPrism, NBasket}
        codecPrism[NBasket].field(_.items).each.field(_.x.length)
      """).map(_.message)

    (anyMatches(msgs, "AvroTraversal.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
  }

  "a single-hop selector that is not a case field is a compile error, not a runtime miss" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.avro.{codecPrism, NOuter}
        codecPrism[NOuter].field(_.derivedTag)
      """).map(_.message)

    (anyMatches(msgs, "is not a case field of") must beTrue)
      .and(anyMatches(msgs, "Known fields: inner, y") must beTrue)
  }

  "the CHAINED form is what actually reaches the inner field" >> {
    val bytes = AvroCodec
      .encodeValue(NOuter(NInner("INNER_X", "INNER_Y"), "OUTER_Y"))
      .fold(f => throw new RuntimeException(f.toString), identity)

    (codecPrism[NOuter].field(_.inner).field(_.y).getOption(bytes) must beSome("INNER_Y"))
      .and(codecPrism[NOuter].field(_.y).getOption(bytes) must beSome("OUTER_Y"))
  }

end NestedSelectorMacroErrorSpec
