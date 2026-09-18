package dev.constructive.eo.jsoniter

import scala.compiletime.testing.typeCheckErrors
import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.optics.Optic.*
import org.specs2.mutable.Specification

// Fixture ADTs at top level (the convention for macro-facing test types in this module). The outer
// and inner objects both carry a `y`, which is what turns a one-hop-too-far selector into a
// well-typed optic aimed at the wrong JSON key rather than a miss.
final case class NsInner(x: String, y: String)

final case class NsOuter(inner: NsInner, y: String):
  // A no-arg member that is NOT a case field. `_.hashCode` will not do: it is a Java no-arg method,
  // so it eta-expands to `Apply(Select(_, …), Nil)` and the single-field-accessor rule catches it.
  def derivedTag: String = y.toUpperCase

final case class NsBasket(items: List[NsInner])

object NsCodecs:
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[Int] = JsonCodecMaker.make
  given JsonValueCodec[NsInner] = JsonCodecMaker.make
  given JsonValueCodec[List[NsInner]] = JsonCodecMaker.make
  given JsonValueCodec[NsOuter] = JsonCodecMaker.make
  given JsonValueCodec[NsBasket] = JsonCodecMaker.make
end NsCodecs

/** The eo-jsoniter mirror of `avro.NestedSelectorMacroErrorSpec` — the cursor macros share one
  * selector parser (`generics.MacroSelectors.extractFieldName`), so the hazard and its fix are
  * carrier-wide. `.field(_.inner.y)` used to parse as the bare name `"y"` and resolve it on the
  * PARENT object.
  */
class NestedSelectorMacroErrorSpec extends Specification:

  import NsCodecs.given

  private def anyMatches(msgs: List[String], fragment: String): Boolean =
    msgs.exists(_.contains(fragment))

  "a nested `.field(_.a.b)` selector is a compile error naming the chained form" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.jsoniter.{JsoniterPrism, NsOuter}
        import dev.constructive.eo.jsoniter.NsCodecs.given
        JsoniterPrism[NsOuter].field(_.inner.y)
      """).map(_.message)

    (anyMatches(msgs, "JsoniterPrism.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
      .and(anyMatches(msgs, ".field(_.a).field(_.b)") must beTrue)
  }

  "a nested selector on a TRAVERSAL suffix is a compile error too" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.jsoniter.{JsoniterPrism, NsBasket}
        import dev.constructive.eo.jsoniter.NsCodecs.given
        JsoniterPrism[NsBasket].field(_.items).each.field(_.x.length)
      """).map(_.message)

    (anyMatches(msgs, "JsoniterTraversal.field") must beTrue)
      .and(anyMatches(msgs, "single-field accessor") must beTrue)
      .and(anyMatches(msgs, "Nested paths") must beTrue)
  }

  "a single-hop selector that is not a case field is a compile error, not a runtime miss" >> {
    val msgs = typeCheckErrors("""
        import dev.constructive.eo.jsoniter.{JsoniterPrism, NsOuter}
        import dev.constructive.eo.jsoniter.NsCodecs.given
        JsoniterPrism[NsOuter].field(_.derivedTag)
      """).map(_.message)

    (anyMatches(msgs, "is not a case field of") must beTrue)
      .and(anyMatches(msgs, "Known fields: inner, y") must beTrue)
  }

  "the CHAINED form is what actually reaches the inner field" >> {
    val bytes = writeToArray(NsOuter(NsInner("INNER_X", "INNER_Y"), "OUTER_Y"))

    (JsoniterPrism[NsOuter].field(_.inner).field(_.y).getOption(bytes) must beSome("INNER_Y"))
      .and(JsoniterPrism[NsOuter].field(_.y).getOption(bytes) must beSome("OUTER_Y"))
  }

end NestedSelectorMacroErrorSpec
