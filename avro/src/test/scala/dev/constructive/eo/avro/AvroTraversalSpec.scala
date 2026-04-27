package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.{Chain, Ior}
import org.apache.avro.generic.{GenericData, IndexedRecord}
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroTraversal]] — the array-walking traversal introduced in Unit 6.
  *
  * Mirrors `dev.constructive.eo.circe.JsonTraversalSpec` block-for-block (six named cases): one
  * happy-path block with default ↔ Unsafe parity, an empty-array case, a missing-prefix case, two
  * per-element-failure blocks (single + multi-element accumulation), and a place / transfer block.
  *
  * The Avro side surfaces failures structurally through [[AvroFailure]] rather than circe's parsed
  * `JsonFailure`; otherwise the assertions are byte-for-byte the same shape.
  */
class AvroTraversalSpec extends Specification:

  import AvroSpecFixtures.*

  /** Build a basket whose items array holds `elems`, then run the per-element name traversal. */
  private def runItemsModify(elems: AnyRef*): Ior[Chain[AvroFailure], IndexedRecord] =
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)(basketRoot(elems))

  /** Compare two records via apache-avro's structural compare. */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

  // covers: return Ior.Right(updatedRecord) when every element succeeds, modify happy
  // === Ior.Right(modifyUnsafe happy), apply f to each raw IndexedRecord on the happy
  // path (transform), return Ior.Right(Vector) when every element decodes (getAll),
  // getAll happy === Ior.Right(getAllUnsafe happy)
  "AvroTraversal happy path: modify+transform+getAll, default↔Unsafe parity" >> {
    val basket = Basket(
      "Alice",
      List(Order("x", 1.0, 1), Order("y", 2.0, 2), Order("z", 3.0, 3)),
    )
    val record = basketRecord(basket)
    val expected = basketRecord(
      basket.copy(items = List(Order("X", 1.0, 1), Order("Y", 2.0, 2), Order("Z", 3.0, 3)))
    )

    val ior = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(record)
    val unsafe = codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(record)
    val parityM = ior match
      case Ior.Right(out) => recordsEqual(out, unsafe)
      case _              => false
    val correctM = recordsEqual(unsafe, expected)

    val getAllIor = codecPrism[Basket].items.each.name.getAll(record)
    val getAllUnsafe = codecPrism[Basket].items.each.name.getAllUnsafe(record)
    val parityGA = getAllIor === Ior.Right(getAllUnsafe)
    val correctGA = getAllUnsafe === Vector("x", "y", "z")

    (parityM === true).and(correctM === true).and(parityGA).and(correctGA)
  }

  // covers: return Ior.Right(input) on an empty array (no elements to iterate),
  // yield Chain.empty on happy path even at size zero
  "AvroTraversal empty array: modify returns Ior.Right(input)" >> {
    val basket = Basket("Alice", List.empty)
    val record = basketRecord(basket)
    codecPrism[Basket].items.each.name.modify(_.toUpperCase)(record) match
      case Ior.Right(out) => recordsEqual(out, record) === true
      case other          =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result
  }

  // covers: return Ior.Left on a missing prefix field (modify), return Ior.Left
  // on a missing prefix (getAll), modifyUnsafe on broken prefix returns input
  "AvroTraversal missing prefix: default → Ior.Left, Unsafe → input unchanged" >> {
    // Build a stump record whose schema lacks `items` entirely.
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

    val mResult = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(stump)
    val mOk = mResult match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("items")))
      case _ =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $mResult"): org.specs2.execute.Result

    val gaOk = codecPrism[Basket].items.each.name.getAll(stump).isLeft === true
    val unsafeOk =
      (codecPrism[Basket].items.each.name.modifyUnsafe(_.toUpperCase)(stump) eq stump) === true

    mOk.and(gaOk).and(unsafeOk)
  }

  // covers: return Ior.Both when one element's suffix misses (element left
  // unchanged, failure recorded), yield a single-element Chain for one per-element
  // failure
  "AvroTraversal single per-element failure: Ior.Both(chain-of-one, partial output)" >> {
    val good = orderRecord(Order("x", 1.0, 1))
    // A record whose schema lacks the `name` field — the per-element walk surfaces PathMissing.
    val malformedSchema =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "qty",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Order", null, "eo.avro.test", false, fields)
    val malformed = new GenericData.Record(malformedSchema)
    malformed.put(0, Integer.valueOf(99))

    val result = runItemsModify(good, malformed, orderRecord(Order("z", 3.0, 3)))
    result match
      case Ior.Both(chain, _) =>
        (chain.length === 1L)
          .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result
  }

  // covers: accumulate multiple per-element failures across the array
  "AvroTraversal accumulates multiple per-element failures across the array" >> {
    val nameless =
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
      r.put(0, Integer.valueOf(1))
      r

    val result = runItemsModify(nameless, nameless, orderRecord(Order("z", 3.0, 3)))
    result match
      case Ior.Both(chain, _) =>
        val missingName = AvroFailure.PathMissing(PathStep.Field("name"))
        (chain.length === 2L)
          .and(chain.toList.forall(_ == missingName) === true)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result
  }

  // covers: place overwrites every element's focused field with a constant,
  // placeUnsafe overwrites every element, transfer lifts C => A into element
  // broadcaster, transferUnsafe lifts C => A, place on a missing prefix returns
  // Ior.Left
  "place / placeUnsafe / transfer / transferUnsafe — default↔Unsafe parity + missing prefix" >> {
    val basket = Basket("Alice", List(Order("x", 1.0, 1), Order("y", 2.0, 2)))
    val record = basketRecord(basket)
    val expectedZ = basketRecord(
      basket.copy(items = List(Order("Z", 1.0, 1), Order("Z", 2.0, 2)))
    )

    val place = codecPrism[Basket].items.each.name.place("Z")(record)
    val placeUnsafe = codecPrism[Basket].items.each.name.placeUnsafe("Z")(record)
    val placeOk = (place match
      case Ior.Right(out) => recordsEqual(out, expectedZ)
      case _              => false
    ) && recordsEqual(placeUnsafe, expectedZ)

    val upcase: String => String = _.toUpperCase
    val expectedZZZ = basketRecord(
      basket.copy(items = List(Order("ZZZ", 1.0, 1), Order("ZZZ", 2.0, 2)))
    )
    val transfer = codecPrism[Basket].items.each.name.transfer(upcase)(record)("zzz")
    val transferUnsafe = codecPrism[Basket].items.each.name.transferUnsafe(upcase)(record)("zzz")
    val transferOk = (transfer match
      case Ior.Right(out) => recordsEqual(out, expectedZZZ)
      case _              => false
    ) && recordsEqual(transferUnsafe, expectedZZZ)

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

    val placeMissing = codecPrism[Basket].items.each.name.place("Z")(stump).isLeft === true

    (placeOk === true).and(transferOk === true).and(placeMissing)
  }

end AvroTraversalSpec
