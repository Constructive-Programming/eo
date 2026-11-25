package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import java.util.ArrayList
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroPrism]]'s record face ([[AvroRecordPrism]], via `.record`) — the
  * cursor-backed Prism from `IndexedRecord` to a native type, with field-drilling sugar.
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

  // covers: codecPrism[S] modify happy round-trip, get on happy input returns Ior.Right;
  //   schema-violating record (age slot holding a String against an Int-expected schema) —
  //   .get surfaces Ior.Left(DecodeFailed),
  //   .modify surfaces Ior.Both(DecodeFailed, originalRecord)
  "codecPrism[S] modify happy round-trip + DecodeFailed on schema-violating record" >> forAll {
    (p: Person) =>
      val record = personRecord(p)
      val expected = personRecord(p.copy(name = p.name.toUpperCase))
      val default = codecPrism[Person]
        .record
        .modify((q: Person) => q.copy(name = q.name.toUpperCase))(record)

      val correctness = default match
        case Ior.Right(out) => recordsEqual(out, expected)
        case _              => false
      val getOk = codecPrism[Person].record.get(record) match
        case Ior.Right(p2) => p2 == p
        case _             => false
      // ---- Schema-violating record: DecodeFailed on get + Ior.Both on modify (absorbed) ----
      val violSchema =
        val fields = new ArrayList[Schema.Field]()
        fields.add(
          new Schema.Field(
            "name",
            org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
            null,
            null,
          )
        )
        fields.add(
          new Schema.Field(
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
      val pPrism = codecPrism[Person].record
      val violGetOk = pPrism.get(viol) match
        case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.DecodeFailed])
        case _               => false
      val violModifyOk = pPrism.modify(identity[Person])(viol) match
        case Ior.Both(chain, out) =>
          chain.headOption.exists(_.isInstanceOf[AvroFailure.DecodeFailed]) && (out eq viol)
        case _ => false

      correctness && getOk && violGetOk && violModifyOk
  }

  // ---- One-level drill (.field) ------------------------------------
  //
  // 2026-04-29 consolidation: 2 .field tests (happy + missing path) → 1 composite.

  // covers: field(_.name) happy modify, get returns Ior.Right(focus),
  //   getOption returns Some(focus), siblings preserved (age intact after modify);
  //   missing-path get returns Ior.Left(chain-of-one PathMissing),
  //   missing-path modify returns Ior.Both(PathMissing, inputRecord),
  //   getOption on missing-path returns None
  "field(_.name): happy modify + sibling preservation + missing-path PathMissing" >> forAll {
    (p: Person) =>
      val nameL = codecPrism[Person].field(_.name).record
      val record = personRecord(p)
      val mFn: String => String = _ + "-x"
      val expected = personRecord(p.copy(name = p.name + "-x"))

      // CPD-OFF — pre-existing specs2 Ior-match checks; touched by *Unsafe removal (#101).
      val default = nameL.modify(mFn)(record)
      val correct = default match
        case Ior.Right(out) => recordsEqual(out, expected)
        case _              => false
      val getOk = nameL.get(record) match
        case Ior.Right(s) => s == p.name
        case _            => false
      val optGetOk = nameL.getOption(record) == Some(p.name)
      val ageOk =
        default.toOption.exists(_.asInstanceOf[GenericRecord].get("age").asInstanceOf[Int] == p.age)
      // ---- Missing-path branch (constants) ----
      val ageOnlySchema =
        val fields = new ArrayList[Schema.Field]()
        fields.add(
          new Schema.Field(
            "age",
            org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
            null,
            null,
          )
        )
        org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
      val partial = buildRecord(ageOnlySchema)("age" -> Integer.valueOf(30))
      val nameLfix = codecPrism[Person].field(_.name).record

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
      val missOptGet = nameLfix.getOption(partial) == None

      // ---- selectDynamic sugar parity (absorbed standalone test) ----
      // covers: codecPrism[Person].name behaves identically to .field(_.name),
      //   getOption through sugar, place / transfer surfaces
      val sugared = codecPrism[Person].name.record
      val explicit = codecPrism[Person].field(_.name).record
      val sugarFn: String => String = _.toUpperCase
      val sugarParityModify =
        (sugared.modify(sugarFn)(record), explicit.modify(sugarFn)(record)) match
          case (Ior.Right(a), Ior.Right(b)) => recordsEqual(a, b)
          case _                            => false
      val sugarGet = sugared.getOption(record) == Some(p.name)
      val sugarPlaceDefault = sugared.place("Carol")(record) match
        case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "Carol")))
        case _              => false
      val sugarTransfer = sugared.transfer((s: String) => s.toUpperCase)(record)("alice") match
        case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
        case _              => false

      correct && getOk && optGetOk && ageOk &&
      missGetOk && missModifyOk && missOptGet &&
      sugarParityModify && sugarGet && sugarPlaceDefault && sugarTransfer
  }

  // ---- Binary + JSON dual surface -----------------------------------
  //
  // 2026-04-29 consolidation: the Array[Byte] and Avro JSON String input tests share the
  // exact same shape — happy parse+modify, bad-input surfaces a Parse-Failed variant in
  // the chain. Collapsed 2 → 1.

  // covers: Array[Byte] input — happy parse + modify (Ior.Right with the modified record),
  //   bad bytes surface BinaryParseFailed in the chain (Ior.Left);
  //   String input (Avro JSON wire format) — happy parse + modify (Ior.Right),
  //   bad JSON surfaces JsonParseFailed in the chain (Ior.Left)
  "Binary + JSON dual-input surfaces: parse + modify, bad input surfaces *ParseFailed" >> {
    val p = Person("Alice", 30)
    val record = personRecord(p)
    val nameL = codecPrism[Person].field(_.name).record

    val goodBytes = toBinary(record, personSchema)
    val badBytes: Array[Byte] = Array(0.toByte)
    val happyBytesOk = nameL.modify((s: String) => s.toUpperCase)(goodBytes) match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false
    val badBytesOk = nameL.modify((s: String) => s)(badBytes) match
      case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryParseFailed])
      case _               => false

    val goodJson = """{"name":"Alice","age":30}"""
    val badJson = "not json at all"
    val happyJsonOk = nameL.modify((s: String) => s.toUpperCase)(goodJson) match
      case Ior.Right(out) => recordsEqual(out, personRecord(p.copy(name = "ALICE")))
      case _              => false
    val badJsonOk = nameL.modify((s: String) => s)(badJson) match
      case Ior.Left(chain) => chain.headOption.exists(_.isInstanceOf[AvroFailure.JsonParseFailed])
      case _               => false

    (happyBytesOk === true)
      .and(badBytesOk === true)
      .and(happyJsonOk === true)
      .and(badJsonOk === true)
  }

  /** Compare two records via apache-avro's structural compare. The public Eq[IndexedRecord] given
    * (per Gap-5 / OQ-avro-9) lives on [[AvroWalk]]; here we go through `GenericData.compare`
    * directly to keep the spec independent of Eq machinery.
    */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      org.apache.avro.generic.GenericData.get().compare(a, b, a.getSchema) == 0

  // CPD-ON

end AvroPrismSpec
