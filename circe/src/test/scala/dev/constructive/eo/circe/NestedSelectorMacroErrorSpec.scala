package dev.constructive.eo.circe

import scala.compiletime.testing.typeCheckErrors
import scala.language.implicitConversions

import hearth.kindlings.circederivation.KindlingsCodecAsObject
import io.circe.Codec
import org.specs2.mutable.Specification

/** The eo-circe mirror of `avro.NestedSelectorMacroErrorSpec` — the cursor macros share one
  * selector parser (`generics.MacroSelectors.extractFieldName`), so the hazard and its fix are
  * carrier-wide.
  *
  * `.field(_.inner.y)` used to parse as the bare name `"y"` and resolve it on the PARENT object,
  * because the parser matched a `Select` with any receiver. On the fixture below — where the outer
  * and inner objects both carry a `y` — that is a well-typed, lawful optic aimed at the wrong JSON
  * key, and the macro's own "nested paths … chain them" abort never fired.
  */
object NestedSelectorMacroErrorSpec:

  case class NInner(x: String, y: String)

  object NInner:
    given Codec.AsObject[NInner] = KindlingsCodecAsObject.derived

  case class NOuter(inner: NInner, y: String):
    // A no-arg member that is NOT a case field. `_.hashCode` will not do: it is a Java no-arg
    // method, so it eta-expands to `Apply(Select(_, …), Nil)` and the single-field-accessor rule
    // already catches it.
    def derivedTag: String = y.toUpperCase

  object NOuter:
    given Codec.AsObject[NOuter] = KindlingsCodecAsObject.derived

  case class NBasket(items: Vector[NInner])

  object NBasket:
    given Codec.AsObject[NBasket] = KindlingsCodecAsObject.derived

end NestedSelectorMacroErrorSpec

class NestedSelectorMacroErrorSpec extends Specification:

  private def anyMatches(msgs: List[String], fragment: String): Boolean =
    msgs.exists(_.contains(fragment))

  "a nested `.field(_.a.b)` selector is a compile error naming the chained form" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.NestedSelectorMacroErrorSpec.NOuter
        codecPrism[NOuter].field(_.inner.y)
      """).map(_.message)

    (anyMatches(msgs, "JsonPrism.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
      .and(anyMatches(msgs, ".field(_.a).field(_.b)") must beTrue)
  }

  "a nested selector on a TRAVERSAL suffix is a compile error too" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.NestedSelectorMacroErrorSpec.NBasket
        codecPrism[NBasket].field(_.items).each.field(_.x.length)
      """).map(_.message)

    (anyMatches(msgs, "JsonTraversal.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
  }

  "a single-hop selector that is not a case field is a compile error, not a runtime miss" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.circe.codecPrism
        import dev.constructive.eo.circe.NestedSelectorMacroErrorSpec.NOuter
        codecPrism[NOuter].field(_.derivedTag)
      """).map(_.message)

    (anyMatches(msgs, "is not a case field of") must beTrue)
      .and(anyMatches(msgs, "Known fields: inner, y") must beTrue)
  }

  "the CHAINED form is what actually reaches the inner field" >> {
    import NestedSelectorMacroErrorSpec.*
    import io.circe.syntax.*
    val json = NOuter(NInner("INNER_X", "INNER_Y"), "OUTER_Y").asJson

    (codecPrism[NOuter].field(_.inner).field(_.y).getOption(json) must beSome("INNER_Y"))
      .and(codecPrism[NOuter].field(_.y).getOption(json) must beSome("OUTER_Y"))
  }

end NestedSelectorMacroErrorSpec
