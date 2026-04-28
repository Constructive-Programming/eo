package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.{GenericData, IndexedRecord}
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroFieldsTraversal]] — the multi-field array traversal produced by
  * `AvroTraversal.fields(_.a, _.b, ...)` (Unit 6).
  *
  * Mirrors `dev.constructive.eo.circe.JsonFieldsTraversalSpec` block-for-block (five named cases):
  * one happy-path block with default ↔ Unsafe parity + qty preserved, two atomicity blocks (single
  * element + cross-element accumulation), one empty-array / missing-prefix block, and one place /
  * placeUnsafe block.
  */
class AvroFieldsTraversalSpec extends Specification:

  import AvroSpecFixtures.*
  import AvroFieldsTraversalSpec.*
  import AvroFieldsTraversalSpec.given

  /** Build a basket whose `items` array holds `elems`, then run the multi-field traversal
    * `.modify(f)`.
    */
  private def runFieldsModify(
      elems: Seq[AnyRef],
      f: NamePrice => NamePrice = identity,
  ) =
    codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(f)(basketRoot(elems))

  /** Compare two records via apache-avro's structural compare. */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

  // ---- Happy path --------------------------------------------------

  // covers: modify (*Unsafe) upper-case name + double price on every element,
  // modify (default) returns Ior.Right on happy path, leaves non-focused fields
  // byte-identical (qty), getAll (default) returns every element's NamedTuple,
  // *Unsafe agreement: modify === Ior.Right(modifyUnsafe) on happy inputs
  "AvroFieldsTraversal happy path: modify+getAll, default↔Unsafe parity, qty preserved" >> {
    val basket = Basket(
      "Alice",
      List(
        Order("x", 1.0, qty = 7),
        Order("y", 2.0, qty = 2),
        Order("z", 3.0, qty = 3),
      ),
    )
    val record = basketRecord(basket)
    val f: NamePrice => NamePrice =
      nt => (name = nt.name.toUpperCase, price = nt.price * 2)

    val expected = basketRecord(
      basket.copy(items =
        List(
          Order("X", 2.0, qty = 7),
          Order("Y", 4.0, qty = 2),
          Order("Z", 6.0, qty = 3),
        )
      )
    )

    val ior = codecPrism[Basket].items.each.fields(_.name, _.price).modify(f)(record)
    val unsafe = codecPrism[Basket].items.each.fields(_.name, _.price).modifyUnsafe(f)(record)
    val parity = ior match
      case Ior.Right(out) => recordsEqual(out, unsafe)
      case _              => false
    val correct = recordsEqual(unsafe, expected)

    val getAll = codecPrism[Basket].items.each.fields(_.name, _.price).getAll(record)
    val getAllOk = getAll.toOption must beSome.which { vec =>
      (vec.length === 3)
        .and(vec(0).name === "x")
        .and(vec(0).price === 1.0)
        .and(vec(2).name === "z")
        .and(vec(2).price === 3.0)
    }

    // Sibling field qty preserved on the Unsafe surface.
    val items = unsafe.asInstanceOf[org.apache.avro.generic.GenericRecord].get("items")
    val firstItem =
      items.asInstanceOf[java.util.List[?]].get(0).asInstanceOf[IndexedRecord]
    val qtyOk = firstItem.get(firstItem.getSchema.getField("qty").pos) === 7

    (parity === true).and(correct === true).and(getAllOk).and(qtyOk)
  }

  // ---- Per-element atomicity (D4) ---------------------------------

  // covers: element with ONE focused field missing — chain-of-one PathMissing(price); the
  //   broken element's slots are NOT atomically touched (modify produces no partial NT) but
  //   the surviving good elements still apply the modify;
  //   element with BOTH focused fields missing contributes TWO PathMissing entries to the
  //   chain (one per missing field), confirming per-element accumulation
  "atomicity: per-element failures (single-field miss + both-missing accumulation)" >> {
    val good = orderRecord(Order("x", 1.0, qty = 1))
    val brokenElem =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "name",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      fields.add(
        new org.apache.avro.Schema.Field(
          "qty",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      val s = org.apache.avro.Schema.createRecord("Order", null, "eo.avro.test", false, fields)
      val r = new GenericData.Record(s)
      r.put(0, "y")
      r.put(1, Integer.valueOf(2))
      r

    val third = orderRecord(Order("z", 3.0, qty = 3))
    val singleOk = runFieldsModify(
      Seq(good, brokenElem, third),
      nt => (name = nt.name.toUpperCase, price = nt.price * 2),
    ) match
      case Ior.Both(chain, _) =>
        (chain.length === 1L)
          .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("price")))
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result

    def emptyOrder =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "qty",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      val s = org.apache.avro.Schema.createRecord("Order", null, "eo.avro.test", false, fields)
      val r = new GenericData.Record(s)
      r.put(0, Integer.valueOf(2))
      r

    val bothOk = runFieldsModify(Seq(good, emptyOrder)) match
      case Ior.Both(chain, _) =>
        (chain.length === 2L)
          .and(chain.toList.contains(AvroFailure.PathMissing(PathStep.Field("name"))) === true)
          .and(chain.toList.contains(AvroFailure.PathMissing(PathStep.Field("price"))) === true)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result

    singleOk.and(bothOk)
  }

  // ---- Empty array / missing prefix --------------------------------

  // covers: empty array — modify returns Ior.Right(input), missing prefix —
  // modify returns Ior.Left (nothing to iterate)
  // covers: empty array — modify returns Ior.Right(record),
  //   missing prefix — modify returns Ior.Left (nothing to iterate);
  //   place / placeUnsafe overwrite every element's focused fields with a constant
  //   (default ↔ Unsafe parity)
  "empty / missing prefix + place / placeUnsafe overwrites" >> {
    val emptyB = Basket("Alice", List.empty)
    val emptyRec = basketRecord(emptyB)
    val emptyOk = codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(nt => (name = nt.name, price = nt.price))(emptyRec) match
      case Ior.Right(out) => recordsEqual(out, emptyRec)
      case _              => false

    val stumpSchema =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "owner",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Basket", null, "eo.avro.test", false, fields)
    val stump = new GenericData.Record(stumpSchema)
    stump.put(0, "Alice")
    val missingOk = codecPrism[Basket]
      .items
      .each
      .fields(_.name, _.price)
      .modify(nt => (name = nt.name, price = nt.price))(stump)
      .isLeft === true

    val basket = Basket("Alice", List(Order("a", 0.5, qty = 1), Order("b", 0.6, qty = 2)))
    val nt: NamePrice = (name = "Z", price = 99.0)
    val expectedZ = basketRecord(
      basket.copy(items = List(Order("Z", 99.0, qty = 1), Order("Z", 99.0, qty = 2)))
    )
    val place =
      codecPrism[Basket].items.each.fields(_.name, _.price).place(nt)(basketRecord(basket))
    val placeUnsafe =
      codecPrism[Basket].items.each.fields(_.name, _.price).placeUnsafe(nt)(basketRecord(basket))
    val placeOk = place match
      case Ior.Right(out) => recordsEqual(out, expectedZ)
      case _              => false

    (emptyOk === true).and(missingOk)
      .and(placeOk === true).and(recordsEqual(placeUnsafe, expectedZ) === true)
  }

end AvroFieldsTraversalSpec

object AvroFieldsTraversalSpec:

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]

  given AvroEncoder[NamePrice] = AvroEncoder.derived
  given AvroDecoder[NamePrice] = AvroDecoder.derived
  given AvroSchemaFor[NamePrice] = AvroSchemaFor.derived

end AvroFieldsTraversalSpec
