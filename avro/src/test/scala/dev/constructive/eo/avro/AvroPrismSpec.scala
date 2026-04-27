package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroPrism]] — the cursor-backed Prism from `IndexedRecord` to a native
  * type, with field-drilling sugar.
  *
  * Mirrors the post-consolidation `JsonPrismSpec`: one block per macro shape, default ↔ Unsafe
  * parity invariant. ScalaCheck `forAll` over a `Person` generator hits both surfaces with one
  * assertion.
  */
class AvroPrismSpec extends Specification with ScalaCheck:

  import AvroSpecFixtures.*

  // ---- Generators (light) ------------------------------------------

  private given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
    yield Person(n, a)
  )

  // ---- Root-level codecPrism[S] ------------------------------------

  // covers: round-trip modify via full decode/encode (default), round-trip modify (Unsafe),
  // pass through record that doesn't decode (Unsafe), get returns Ior.Right
  "codecPrism[S] modify round-trips on the happy path with default↔Unsafe parity" >> forAll {
    (p: Person) =>
      val record = personRecord(p)
      val expected = personRecord(p.copy(name = p.name.toUpperCase))
      val unsafe = codecPrism[Person]
        .modifyUnsafe((q: Person) => q.copy(name = q.name.toUpperCase))(record)
      val default = codecPrism[Person]
        .modify((q: Person) => q.copy(name = q.name.toUpperCase))(record)

      val parity = default match
        case Ior.Right(out) => recordsEqual(out, unsafe)
        case _              => false
      val correctness = recordsEqual(unsafe, expected)
      val getOk = codecPrism[Person].get(record) match
        case Ior.Right(p2) => p2 == p
        case _             => false

      parity && correctness && getOk
  }

  // ---- One-level drill (.field) ------------------------------------

  // covers: modify focused field in place (default + Unsafe parity), get returns Ior.Right(focus),
  // getOptionUnsafe returns Some(focus), siblings preserved after the modify
  "field(_.name) happy path: modify+get default↔Unsafe parity, siblings preserved" >> forAll {
    (p: Person) =>
      val nameL = codecPrism[Person].field(_.name)
      val record = personRecord(p)
      val mFn: String => String = _ + "-x"
      val expected = personRecord(p.copy(name = p.name + "-x"))

      val unsafe = nameL.modifyUnsafe(mFn)(record)
      val default = nameL.modify(mFn)(record)

      val parity = default match
        case Ior.Right(out) => recordsEqual(out, unsafe)
        case _              => false
      val correct = recordsEqual(unsafe, expected)
      val getOk = nameL.get(record) match
        case Ior.Right(s) => s == p.name
        case _            => false
      val unsafeGetOk = nameL.getOptionUnsafe(record) == Some(p.name)
      // Sibling preservation: age intact after modify
      val out = unsafe.asInstanceOf[GenericRecord]
      val ageOk = out.get("age").asInstanceOf[Int] == p.age

      parity && correct && getOk && unsafeGetOk && ageOk
  }

  // covers: get returns Ior.Left with PathMissing on missing path, getOptionUnsafe returns None,
  // modify on missing-path record returns Ior.Both(PathMissing, inputRecord),
  // modifyUnsafe on missing-path record returns input unchanged
  "field(_.name) on missing path: default↔Unsafe parity surfaces PathMissing" >> {
    // Build a record under a one-field schema (no `name`).
    val ageOnlySchema =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "age",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val partial = buildRecord(ageOnlySchema)("age" -> Integer.valueOf(30))
    val nameL = codecPrism[Person](personSchema).field(_.name)

    val getResult = nameL.get(partial)
    val modifyResult = nameL.modify(_.toUpperCase)(partial)
    val unsafeModify = nameL.modifyUnsafe(_.toUpperCase)(partial)
    val unsafeGet = nameL.getOptionUnsafe(partial)

    val getOk = getResult match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
      case _ =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $getResult"): org.specs2.execute.Result

    val modifyOk = modifyResult match
      case Ior.Both(chain, out) =>
        (chain.headOption.get === AvroFailure.PathMissing(PathStep.Field("name")))
          .and((out eq partial) === true)
      case _ =>
        org
          .specs2
          .execute
          .Failure(s"expected Ior.Both, got $modifyResult"): org.specs2.execute.Result

    getOk
      .and(modifyOk)
      .and((unsafeModify eq partial) === true)
      .and(unsafeGet === None)
  }

  // ---- Selectable field sugar (.name) ------------------------------

  // covers: behave identically to .field(_.name), getOptionUnsafe through sugar, place/transfer
  // surfaces (Unsafe + default), default surface returns Ior.Right
  "selectDynamic sugar `codecPrism[Person].name` matches .field(_.name)" >> forAll { (p: Person) =>
    val record = personRecord(p)
    val explicit = codecPrism[Person].field(_.name)
    val sugared = codecPrism[Person].name
    val mFn: String => String = _.toUpperCase

    val parityModify = recordsEqual(
      sugared.modifyUnsafe(mFn)(record),
      explicit.modifyUnsafe(mFn)(record),
    )
    val sugarGet = sugared.getOptionUnsafe(record) == Some(p.name)

    val sugarPlaceUnsafe = recordsEqual(
      sugared.placeUnsafe("Carol")(record),
      personRecord(p.copy(name = "Carol")),
    )
    val sugarPlaceDefault = sugared.place("Carol")(record) match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "Carol")))
      case _              => false

    val sugarTransfer = sugared.transfer((s: String) => s.toUpperCase)(record)("alice") match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false

    parityModify && sugarGet && sugarPlaceUnsafe && sugarPlaceDefault && sugarTransfer
  }

  // ---- Binary-input dual surface -----------------------------------

  // covers: modify on Array[Byte] input parses + applies (Ior.Right) on the happy path,
  // modify on bad bytes surfaces BinaryParseFailed in the chain, getOptionUnsafe on bad bytes
  // returns None
  "Array[Byte] input: parse + modify, bad bytes surface BinaryParseFailed" >> {
    val p = Person("Alice", 30)
    val record = personRecord(p)
    val goodBytes = toBinary(record, personSchema)
    val badBytes: Array[Byte] = Array(0.toByte)

    val nameL = codecPrism[Person].field(_.name)

    val happyResult = nameL.modify((s: String) => s.toUpperCase)(goodBytes)
    val happyOk = happyResult match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false

    val badResult = nameL.modify((s: String) => s)(badBytes)
    val badOk = badResult match
      case Ior.Left(chain) =>
        chain.headOption.get.isInstanceOf[AvroFailure.BinaryParseFailed]
      case _ => false

    val unsafeBad = nameL.getOptionUnsafe(badBytes)

    (happyOk === true)
      .and(badOk === true)
      .and(unsafeBad === None)
  }

  // ---- String-input dual surface (Avro JSON wire format) -----------

  // covers: modify on String input parses + applies (Ior.Right) on the happy path,
  // modify on bad JSON surfaces JsonParseFailed in the chain, getOptionUnsafe on bad JSON
  // returns None (synthetic-empty-record fallback can't decode the focus)
  "String input: parse + modify, bad JSON surfaces JsonParseFailed" >> {
    val p = Person("Alice", 30)
    val goodJson = """{"name":"Alice","age":30}"""
    val badJson = "not json at all"

    val nameL = codecPrism[Person].field(_.name)

    val happyResult = nameL.modify((s: String) => s.toUpperCase)(goodJson)
    val happyOk = happyResult match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false

    val badResult = nameL.modify((s: String) => s)(badJson)
    val badOk = badResult match
      case Ior.Left(chain) =>
        chain.headOption.get.isInstanceOf[AvroFailure.JsonParseFailed]
      case _ => false

    val unsafeBad = nameL.getOptionUnsafe(badJson)

    (happyOk === true)
      .and(badOk === true)
      .and(unsafeBad === None)
  }

  // ---- Codec-level decode failure ----------------------------------

  // covers: decode failure on get surfaces Ior.Left(DecodeFailed),
  // decode failure on modify surfaces Ior.Both(DecodeFailed, inputRecord)
  "decode failure surfaces DecodeFailed on get + Ior.Both on modify" >> {
    // Build a record where the `age` slot holds a String (schema-violating).
    // Using a schema with `age: string` so the codec sees a type mismatch
    // when decoding back to Person (which expects age: Int).
    val violSchema =
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
          "age",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val viol = buildRecord(violSchema)(
      "name" -> "Alice",
      "age" -> "thirty",
    )

    val pPrism = codecPrism[Person](personSchema)
    val getResult = pPrism.get(viol)
    val modifyResult = pPrism.modify(identity[Person])(viol)

    val getOk = getResult match
      case Ior.Left(chain) =>
        chain.headOption.get.isInstanceOf[AvroFailure.DecodeFailed]
      case _ => false

    val modifyOk = modifyResult match
      case Ior.Both(chain, out) =>
        chain.headOption.get.isInstanceOf[AvroFailure.DecodeFailed] && (out eq viol)
      case _ => false

    (getOk === true).and(modifyOk === true)
  }

  /** Compare two records via apache-avro's structural compare. The public Eq[IndexedRecord] given
    * (per Gap-5 / OQ-avro-9) lives on [[AvroWalk]]; here we go through `GenericData.compare`
    * directly to keep the spec independent of Eq machinery.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      org.apache.avro.generic.GenericData.get().compare(a, b, a.getSchema) == 0

end AvroPrismSpec
