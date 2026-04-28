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

  // covers: codecPrism[S] modify happy round-trip (default ↔ Unsafe parity),
  //   modifyUnsafe on undecodable record passes through unchanged,
  //   get on happy input returns Ior.Right;
  //   schema-violating record (age slot holding a String against an Int-expected schema) —
  //   .get surfaces Ior.Left(DecodeFailed),
  //   .modify surfaces Ior.Both(DecodeFailed, originalRecord)
  "codecPrism[S] modify happy round-trip + DecodeFailed on schema-violating record" >> forAll {
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

      // ---- Schema-violating record: DecodeFailed on get + Ior.Both on modify (absorbed) ----
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
      val violGetOk = pPrism.get(viol) match
        case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.DecodeFailed])
        case _               => false
      val violModifyOk = pPrism.modify(identity[Person])(viol) match
        case Ior.Both(chain, out) =>
          chain.headOption.exists(_.isInstanceOf[AvroFailure.DecodeFailed]) && (out eq viol)
        case _ => false

      parity && correctness && getOk && violGetOk && violModifyOk
  }

  // ---- One-level drill (.field) ------------------------------------
  //
  // 2026-04-29 consolidation: 2 .field tests (happy + missing path) → 1 composite.

  // covers: field(_.name) happy modify (default ↔ Unsafe parity), get returns Ior.Right(focus),
  //   getOptionUnsafe returns Some(focus), siblings preserved (age intact after modify);
  //   missing-path get returns Ior.Left(chain-of-one PathMissing),
  //   missing-path modify returns Ior.Both(PathMissing, inputRecord),
  //   modifyUnsafe on missing-path returns input unchanged,
  //   getOptionUnsafe on missing-path returns None
  "field(_.name): happy modify (parity) + sibling preservation + missing-path PathMissing" >> forAll {
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
      val out = unsafe.asInstanceOf[GenericRecord]
      val ageOk = out.get("age").asInstanceOf[Int] == p.age

      // ---- Missing-path branch (constants) ----
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
      val nameLfix = codecPrism[Person](personSchema).field(_.name)

      val missGetOk = nameLfix.get(partial) match
        case Ior.Left(chain) =>
          chain.length == 1L &&
          chain.headOption.contains(AvroFailure.PathMissing(PathStep.Field("name")))
        case _ => false
      val missModifyOk = nameLfix.modify(_.toUpperCase)(partial) match
        case Ior.Both(chain, out) =>
          chain.headOption.contains(AvroFailure.PathMissing(PathStep.Field("name"))) &&
          (out eq partial)
        case _ => false
      val missUnsafeMod = nameLfix.modifyUnsafe(_.toUpperCase)(partial) eq partial
      val missUnsafeGet = nameLfix.getOptionUnsafe(partial) == None

      // ---- selectDynamic sugar parity (absorbed standalone test) ----
      // covers: codecPrism[Person].name behaves identically to .field(_.name),
      //   getOptionUnsafe through sugar, placeUnsafe / place(default) / transfer surfaces
      val sugared = codecPrism[Person].name
      val explicit = codecPrism[Person].field(_.name)
      val sugarFn: String => String = _.toUpperCase
      val sugarParityModify = recordsEqual(
        sugared.modifyUnsafe(sugarFn)(record),
        explicit.modifyUnsafe(sugarFn)(record),
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

      parity && correct && getOk && unsafeGetOk && ageOk &&
      missGetOk && missModifyOk && missUnsafeMod && missUnsafeGet &&
      sugarParityModify && sugarGet && sugarPlaceUnsafe && sugarPlaceDefault && sugarTransfer
  }

  // ---- Binary + JSON dual surface -----------------------------------
  //
  // 2026-04-29 consolidation: the Array[Byte] and Avro JSON String input tests share the
  // exact same shape — happy parse+modify, bad-input surfaces a Parse-Failed variant in
  // the chain, getOptionUnsafe returns None on bad input. Collapsed 2 → 1.

  // covers: Array[Byte] input — happy parse + modify (Ior.Right with the modified record),
  //   bad bytes surface BinaryParseFailed in the chain (Ior.Left),
  //   getOptionUnsafe on bad bytes returns None;
  //   String input (Avro JSON wire format) — happy parse + modify (Ior.Right),
  //   bad JSON surfaces JsonParseFailed in the chain (Ior.Left),
  //   getOptionUnsafe on bad JSON returns None
  "Binary + JSON dual-input surfaces: parse + modify, bad input surfaces *ParseFailed" >> {
    val p = Person("Alice", 30)
    val record = personRecord(p)
    val nameL = codecPrism[Person].field(_.name)

    val goodBytes = toBinary(record, personSchema)
    val badBytes: Array[Byte] = Array(0.toByte)
    val happyBytesOk = nameL.modify((s: String) => s.toUpperCase)(goodBytes) match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false
    val badBytesOk = nameL.modify((s: String) => s)(badBytes) match
      case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryParseFailed])
      case _               => false
    val unsafeBytesNone = nameL.getOptionUnsafe(badBytes) === None

    val goodJson = """{"name":"Alice","age":30}"""
    val badJson = "not json at all"
    val happyJsonOk = nameL.modify((s: String) => s.toUpperCase)(goodJson) match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false
    val badJsonOk = nameL.modify((s: String) => s)(badJson) match
      case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.JsonParseFailed])
      case _               => false
    val unsafeJsonNone = nameL.getOptionUnsafe(badJson) === None

    (happyBytesOk === true).and(badBytesOk === true).and(unsafeBytesNone)
      .and(happyJsonOk === true).and(badJsonOk === true).and(unsafeJsonNone)
  }


  /** Compare two records via apache-avro's structural compare. The public Eq[IndexedRecord] given
    * (per Gap-5 / OQ-avro-9) lives on [[AvroWalk]]; here we go through `GenericData.compare`
    * directly to keep the spec independent of Eq machinery.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      org.apache.avro.generic.GenericData.get().compare(a, b, a.getSchema) == 0

end AvroPrismSpec
