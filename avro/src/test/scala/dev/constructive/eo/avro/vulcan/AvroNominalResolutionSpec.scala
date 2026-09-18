package dev.constructive.eo.avro.vulcan

import scala.language.implicitConversions

import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.specs2.mutable.Specification

/** The gate for schema-field resolution (`AvroWalk.fieldNameAt`) — issue #95.
  *
  * Issue #35 resolved `.field(_.x)` to the schema field at `x`'s DECLARATION INDEX, with no name
  * cross-check at all. That is sound only while the codec's schema is positionally 1:1 with the
  * case class — true by construction for every kindlings-derived codec, and NOT true for a
  * hand-written or `vulcan.Codec` field list, which can add a computed field, drop one, or reorder.
  * On those the optic targets the WRONG SLOT and produces valid wire bytes with wrong content: no
  * law sees it (a mis-targeted optic is a perfectly lawful `Optional` onto the wrong field), no
  * round-trip sees it, and no existing fixture could have seen it because every fixture in the
  * suite is derived.
  *
  * Every example below scores SLOT TRUTH — which schema slot a write actually touched — never
  * `copy(...)`-truth, which cannot tell a wrong slot from a stale derived one.
  *
  * The false-positive controls at the bottom are as load-bearing as the repros: issue #35's
  * motivating case (a name transform, which by definition destroys the literal Scala name) MUST
  * keep resolving by position, and `AvroFieldNamingSpec` is the snake_case half of that guard.
  */
class AvroNominalResolutionSpec extends Specification:

  import DivergentCodecs.*

  private val threeSchema = summon[AvroCodec[Three]].schema
  private val three = Three("A0", "B0", 7L)
  private val threeBytes = encodeBytes(three)

  // ---- cells 1-3: the reported bug, byte face -------------------------
  // schema {a, computed, b, c} vs case class {a, b, c}: one computed field shifts every later slot.

  "a computed schema field does not shift the READ onto the wrong slot" >> {
    codecPrism[Three].field(_.b).getOption(threeBytes) must beSome("B0")
  }

  "a computed schema field does not shift the WRITE onto the wrong slot" >> {
    val after = slots(codecPrism[Three].field(_.b).replace("B1")(threeBytes), threeSchema)
    // Exactly the intended slot moved. `computed` keeps its stale value — a splice optic never
    // recomputes a derived field, which is by design and is NOT a targeting failure.
    (after("b") === "B1")
      .and(after("a") === "A0")
      .and(after("c") === "7")
      .and(after("computed") === "A0|B0")
  }

  "a leaf PAST the divergence still round-trips (the wrong slot does not even decode)" >> {
    val after = slots(codecPrism[Three].field(_.c).replace(9L)(threeBytes), threeSchema)
    (codecPrism[Three].field(_.c).getOption(threeBytes) must beSome(7L))
      .and(after("c") === "9")
      .and(after("b") === "B0")
  }

  // ---- cell 4: the record face resolves through the same function -----

  "the record face resolves through the same resolver, read and write" >> {
    val rec = summon[AvroCodec[Three]].encode(three).asInstanceOf[IndexedRecord]
    val out = codecPrism[Three].field(_.b).record.modifyUnsafe(_ + "!")(rec)
    (codecPrism[Three].field(_.b).record.getOption(rec) must beSome("B0"))
      .and(String.valueOf(out.asInstanceOf[GenericRecord].get("b")) === "B0!")
      .and(String.valueOf(out.asInstanceOf[GenericRecord].get("computed")) === "A0|B0")
  }

  // ---- cell 5: `.fields` — one grouped call used to damage two slots --

  "`.fields` resolves every selector independently (one call used to damage two slots)" >> {
    import ThreeNt.given
    val read = codecPrism[Three].fields(_.a, _.b).getOption(threeBytes)
    val after = slots(
      codecPrism[Three].fields(_.a, _.b).replace((a = "A1", b = "B1"): ThreeNt.AB)(threeBytes),
      threeSchema,
    )
    (read must beSome((a = "A0", b = "B0"): ThreeNt.AB))
      .and(after("a") === "A1")
      .and(after("b") === "B1")
      .and(after("c") === "7")
  }

  // ---- cell 6: the traversal suffix (`.each.field`) --------------------

  "a traversal suffix resolves through the same resolver, read and write" >> {
    val basket = Basket(List(Inner("X0", 1L)))
    val basketSchema = summon[AvroCodec[Basket]].schema
    val basketBytes = encodeBytes(basket)
    val basketRec = summon[AvroCodec[Basket]].encode(basket).asInstanceOf[IndexedRecord]
    val optic = codecPrism[Basket].field(_.items).each.field(_.x)
    val out = optic.modify(_ + "@")(basketBytes)
    val item = recordOf(out, basketSchema)
      .get("items")
      .asInstanceOf[java.util.List[GenericRecord]]
      .get(0)
    (optic.record.getAllUnsafe(basketRec) === Vector("X0"))
      .and(String.valueOf(item.get("x")) === "X0@")
      // the inner codec's derived slot `c` was written as "X0!" and must be untouched
      .and(String.valueOf(item.get("c")) === "X0!")
  }

  "a nested record's own divergence is resolved one hop down" >> {
    val outerSchema = summon[AvroCodec[Outer]].schema
    val outerBytes = encodeBytes(Outer("O0", Inner("X0", 1L)))
    val optic = codecPrism[Outer].field(_.inner).field(_.x)
    val inner = recordOf(optic.replace("X1")(outerBytes), outerSchema)
      .get("inner")
      .asInstanceOf[GenericRecord]
    (optic.getOption(outerBytes) must beSome("X0"))
      .and(String.valueOf(inner.get("x")) === "X1")
      .and(String.valueOf(inner.get("c")) === "X0!")
  }

  // ---- cell 7: reversed field order, both String ----------------------
  // Equal arity, identical leaf types: no shape signal and no decode failure exist. Names only.

  "a reordered field list reads and writes the named slot, not the i-th one" >> {
    val pairSchema = summon[AvroCodec[Pair]].schema
    val bytes = encodeBytes(Pair("ALPHA0", "BETA0"))
    val after = slots(codecPrism[Pair].field(_.alpha).replace("ALPHA1")(bytes), pairSchema)
    (codecPrism[Pair].field(_.alpha).getOption(bytes) must beSome("ALPHA0"))
      .and(after("alpha") === "ALPHA1")
      .and(after("beta") === "BETA0")
  }

  // ---- cell 8: snake_case schema PLUS a computed field ----------------
  // No literal Scala name survives, so exact-name matching alone sends this to the wrong slot.

  "a snake_case schema with a computed field still resolves to the right slot" >> {
    val schema = summon[AvroCodec[SnakeComputed]].schema
    val bytes = encodeBytes(SnakeComputed("c0", 7L))
    val optic = codecPrism[SnakeComputed].field(_.landingPageId)
    val after = slots(optic.replace(8L)(bytes), schema)
    (optic.getOption(bytes) must beSome(7L))
      .and(after("landing_page_id") === "8")
      .and(after("click_id") === "c0")
      .and(after("computed") === "c0!")
  }

  // ---- FALSE-POSITIVE CONTROLS: issue #35's own population ------------
  // A name transform REMOVES the literal Scala name by definition, so no nominal rung can fire and
  // POSITION must stay in charge. Breaking either of these breaks what #35 shipped for.

  "FALSE-POSITIVE CONTROL: a hostile custom name transform still resolves by POSITION" >> {
    import HostileTransform.*
    val schema = summon[AvroCodec[Hostile]].schema
    val bytes = encodeBytes(Hostile("A0", 1L))
    val after = slots(codecPrism[Hostile].field(_.alpha).replace("A1")(bytes), schema)
    (codecPrism[Hostile].field(_.alpha).getOption(bytes) must beSome("A0"))
      .and(after("x_ahpla") === "A1")
      .and(after("x_ateb") === "1")
  }

  "FALSE-POSITIVE CONTROL: a vulcan rename with preserved order still resolves by POSITION" >> {
    val schema = summon[AvroCodec[NPair]].schema
    val bytes = encodeBytes(NPair("ALPHA0", "BETA0"))
    val after = slots(codecPrism[NPair].field(_.alpha).replace("ALPHA1")(bytes), schema)
    (codecPrism[NPair].field(_.alpha).getOption(bytes) must beSome("ALPHA0"))
      .and(after("alpha_name") === "ALPHA1")
      .and(after("beta_name") === "BETA0")
  }

end AvroNominalResolutionSpec
