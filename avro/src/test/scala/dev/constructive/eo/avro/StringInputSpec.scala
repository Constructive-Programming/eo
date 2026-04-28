package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.apache.avro.generic.{GenericData, GenericRecord, IndexedRecord}
import org.specs2.mutable.Specification

/** Behaviour spec for the `IndexedRecord | Array[Byte] | String` triple-input overloads on every
  * public Avro optic surface. The `String` arm is the Avro JSON wire format, parsed by
  * apache-avro's `JsonDecoder` against the prism's cached reader schema.
  *
  * Mirrors `dev.constructive.eo.circe.StringInputSpec` block-for-block — one composite block per
  * carrier (AvroPrism / AvroFieldsPrism / AvroTraversal) covering happy-path,
  * `Ior.Left(JsonParseFailed)` on bad input, and synthetic-empty-record / empty-vector fallbacks on
  * the Unsafe surface; one extra block for record-input parity (the widened union still routes
  * record input through the no-op arm).
  *
  * '''Avro JSON wire format quirks.'''
  *   - For `Person(name, age)` the wire form is the unsurprising `{"name":"Alice","age":30}` (the
  *     schema has no unions).
  *   - For `Basket(owner, items: List[Order])` the wire form quotes both sub-records and the
  *     enclosing array — `{"owner":"Alice","items":[{"name":"x","price":1.0,"qty":1},…]}`.
  *
  * '''JsonDecoder failure-class survey.'''
  *   - genuinely bad JSON (`"not json at all"`) → `org.apache.avro.AvroTypeException`;
  *   - JSON-but-wrong-shape (missing required field) → `AvroTypeException` ("Expected field …");
  *   - unknown field → silently dropped by apache-avro on read, so the parser succeeds and the test
  *     asserts on the resulting `Ior.Right`. (See [[AvroFailure.decodeJsonString]] doc.)
  */
class StringInputSpec extends Specification:

  import AvroSpecFixtures.*
  import StringInputSpec.given

  // ---- AvroPrism + AvroFieldsPrism + record-pass-through (one composite) ----------
  //
  // 2026-04-29 consolidation: 4 String-input tests → 2 composites (one for prism+fields+
  // pass-through, one for traversal). Same code paths witnessed exhaustively per surface.

  // covers: AvroPrism String-input — happy parse + modify (Ior.Right + structural record equality),
  //   .get round-trips on well-formed String, .place via Ior, .transfer via Ior,
  //   bad-JSON surfaces Ior.Left(JsonParseFailed), modifyUnsafe on bad JSON returns synthetic
  //   empty record under personSchema, getOptionUnsafe on bad JSON returns None;
  //   AvroFieldsPrism (multi-field NamedTuple focus) — happy parse + modify (Ior.Right),
  //   bad-JSON surfaces Ior.Left(JsonParseFailed) on the multi-field surface;
  //   IndexedRecord input still routes cleanly through the widened (record | bytes | String)
  //   signature — parity with String input on the Person.field(_.name) modify
  "AvroPrism + AvroFieldsPrism String-input + record pass-through (one composite)" >> {
    val str = """{"name":"Alice","age":30}"""
    val expected = personRecord(Person("ALICE", 30))
    val nameL = codecPrism[Person].field(_.name)

    val modOk = nameL.modify((s: String) => s.toUpperCase)(str) match
      case Ior.Right(out) => recordsEqual(out, expected)
      case _              => false
    val getOk = nameL.get(str) match
      case Ior.Right(s) => s == "Alice"
      case _            => false
    val placeOk = nameL.place("Bob")(str) match
      case Ior.Right(out) => recordsEqual(out, personRecord(Person("Bob", 30)))
      case _              => false
    val transferOk = nameL.transfer((s: String) => s.toUpperCase)(str)("alice") match
      case Ior.Right(out) => recordsEqual(out, personRecord(Person("ALICE", 30)))
      case _              => false

    val badStr = "not json at all"
    val badIor = nameL.modify((s: String) => s.toUpperCase)(badStr)
    val badIorOk = badIor match
      case Ior.Left(chain) =>
        (chain.length == 1L) && chain.headOption.exists(_.isInstanceOf[AvroFailure.JsonParseFailed])
      case _ => false
    val unsafeBad = nameL.modifyUnsafe((s: String) => s.toUpperCase)(badStr)
    val unsafeBadOk = unsafeBad.getSchema == personSchema
    val unsafeNoneOk = nameL.getOptionUnsafe(badStr) == None

    val fp = codecPrism[Person].fields(_.name, _.age)
    val fieldsExpected = personRecord(Person("ALICE", 31))
    val fieldsOk = fp.modify(nt => (name = nt.name.toUpperCase, age = nt.age + 1))(str) match
      case Ior.Right(out) => recordsEqual(out, fieldsExpected)
      case _              => false
    val fieldsBadOk = fp.modify(identity)("{ malformed") match
      case Ior.Left(chain) =>
        chain.headOption.exists(_.isInstanceOf[AvroFailure.JsonParseFailed])
      case _ => false

    val p = Person("Alice", 30)
    val recordInput = personRecord(p)
    val fromRecord = nameL.modify((s: String) => s.toUpperCase)(recordInput)
    val fromString = nameL.modify((s: String) => s.toUpperCase)(str)
    val parity = (fromRecord, fromString) match
      case (Ior.Right(a), Ior.Right(b)) => recordsEqual(a, b)
      case _                            => false
    val passExpected = personRecord(p.copy(name = "ALICE"))
    val recordOk = fromRecord match
      case Ior.Right(out) => recordsEqual(out, passExpected)
      case _              => false

    (modOk === true)
      .and(getOk === true)
      .and(placeOk === true)
      .and(transferOk === true)
      .and(badIorOk === true)
      .and(unsafeBadOk === true)
      .and(unsafeNoneOk === true)
      .and(fieldsOk === true)
      .and(fieldsBadOk === true)
      .and(parity === true)
      .and(recordOk === true)
  }

  // covers: AvroTraversal String-input — happy parse + per-element modify (Ior.Right),
  //   bad-JSON surfaces Ior.Left(JsonParseFailed),
  //   getAllUnsafe on bad JSON returns Vector.empty
  "AvroTraversal String-input: parse+modify + Ior.Left(JsonParseFailed) + getAllUnsafe-empty on bad input" >> {
    val basket = Basket("Alice", List(Order("a", 1.0, 1), Order("b", 2.0, 2)))
    val str =
      """{"owner":"Alice","items":[{"name":"a","price":1.0,"qty":1},{"name":"b","price":2.0,"qty":2}]}"""
    val expected = basketRecord(
      basket.copy(items = List(Order("A", 1.0, 1), Order("B", 2.0, 2)))
    )

    val modOk = codecPrism[Basket].items.each.name.modify(_.toUpperCase)(str) match
      case Ior.Right(out) => recordsEqual(out, expected)
      case _              => false

    val badIor = codecPrism[Basket].items.each.name.modify(_.toUpperCase)("totally not json")
    val badIorOk = badIor match
      case Ior.Left(chain) =>
        chain.headOption.exists(_.isInstanceOf[AvroFailure.JsonParseFailed])
      case _ => false

    val unsafeEmpty = codecPrism[Basket].items.each.name.getAllUnsafe("not json")
    val unsafeEmptyOk = unsafeEmpty == Vector.empty

    (modOk === true).and(badIorOk === true).and(unsafeEmptyOk === true)
  }

  /** Compare two records via apache-avro's structural compare; sidesteps the Eq-given import. */
  private def recordsEqual(a: IndexedRecord, b: IndexedRecord): Boolean =
    a.getSchema == b.getSchema &&
      GenericData.get().compare(a, b, a.getSchema) == 0

  // Suppress unused-import lints for GenericRecord / GenericData when assertions skip them.
  private val _ = (classOf[GenericRecord], GenericData.get())

end StringInputSpec

object StringInputSpec:

  /** The NamedTuple type the macro synthesises for `Person.fields(_.name, _.age)`. Mirrors the
    * companion-object pattern in [[AvroFieldsPrismSpec]] so this spec is independent.
    */
  type NameAge = NamedTuple.NamedTuple[("name", "age"), (String, Int)]

  given AvroEncoder[NameAge] = AvroEncoder.derived
  given AvroDecoder[NameAge] = AvroDecoder.derived
  given AvroSchemaFor[NameAge] = AvroSchemaFor.derived
