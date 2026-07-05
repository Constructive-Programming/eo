package dev.constructive.eo.avro

import cats.data.Chain
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{
  GenericData,
  GenericDatumWriter,
  GenericRecord,
  GenericRecordBuilder,
}
import org.apache.avro.io.EncoderFactory

/** Shared ADT fixtures + kindlings-derived codecs for the avro-test specs.
  *
  * Mirrors the role of `dev.constructive.eo.circe.JsonSpecFixtures`: a single source of truth for
  * the toy-record schemas and their derived codecs that every behaviour spec re-uses.
  *
  * Kindlings' `AvroEncoder` / `AvroDecoder` / `AvroSchemaFor` instances are summoned via
  * `*.derived` per-typeclass; cats-eo-avro's [[AvroCodec]] wrapper picks them up through
  * [[AvroCodec.derived]] and exposes one combined typeclass to the prism layer.
  */
object AvroSpecFixtures:

  // ---- ADTs ----------------------------------------------------------

  /** Two SAME-typed fields — the fixture that catches positional/by-name write confusion (a
    * positional write of a reordered full-cover NamedTuple silently swaps these).
    */
  case class FullName(first: String, last: String)

  object FullName:

    given AvroEncoder[FullName] = AvroEncoder.derived
    given AvroDecoder[FullName] = AvroDecoder.derived
    given AvroSchemaFor[FullName] = AvroSchemaFor.derived

  /** Two-field case class — minimal product type for the prism specs. */
  case class Person(name: String, age: Int)

  object Person:

    given AvroEncoder[Person] = AvroEncoder.derived
    given AvroDecoder[Person] = AvroDecoder.derived
    given AvroSchemaFor[Person] = AvroSchemaFor.derived

  /** Three-field case class used by the traversal specs as the per-element shape inside a
    * [[Basket]]. Mirrors `JsonSpecFixtures.Order` — `name` for the leaf-traversal hooks,
    * `(name, price)` for the multi-field traversal hooks, `qty` as the un-focused sibling whose
    * preservation across modify is the witness for "non-focused fields untouched".
    */
  case class Order(name: String, price: Double, qty: Int)

  object Order:

    given AvroEncoder[Order] = AvroEncoder.derived
    given AvroDecoder[Order] = AvroDecoder.derived
    given AvroSchemaFor[Order] = AvroSchemaFor.derived

  /** Owner + array of [[Order]]s — the array-walking shape consumed by the traversal specs. The
    * `items: List[Order]` derives a `Schema.Type.ARRAY` via kindlings.
    */
  case class Basket(owner: String, items: List[Order])

  object Basket:

    given AvroEncoder[Basket] = AvroEncoder.derived
    given AvroDecoder[Basket] = AvroDecoder.derived
    given AvroSchemaFor[Basket] = AvroSchemaFor.derived

  /** The reader schema used by the parse-helper tests. Pulled off the codec at fixture-creation
    * time so spec bodies don't repeat the `.schema` dance.
    */
  lazy val personSchema: Schema = summon[AvroCodec[Person]].schema

  /** Reader schema for [[Basket]] — used by the traversal specs to construct stump records that
    * exercise the missing-prefix path.
    */
  lazy val basketSchema: Schema = summon[AvroCodec[Basket]].schema

  /** Reader schema for [[Order]] — used to assemble custom array elements when a spec wants a
    * specific malformed shape.
    */
  lazy val orderSchema: Schema = summon[AvroCodec[Order]].schema

  /** Two-field record carrying a union-shaped `amount` slot. Mirrors the "transaction" shape from
    * the Unit 8 plan: kindlings derives `amount` as `union<null, long>` (the standard Avro encoding
    * for `Option[Long]`), exercising the `.union[Branch]` macro on a `Some` branch and surfacing
    * `UnionResolutionFailed` when the runtime value is on the `null` branch and the user asks for
    * `.union[Long]`.
    */
  case class Transaction(id: String, amount: Option[Long])

  object Transaction:

    given AvroEncoder[Transaction] = AvroEncoder.derived
    given AvroDecoder[Transaction] = AvroDecoder.derived
    given AvroSchemaFor[Transaction] = AvroSchemaFor.derived

  /** Reader schema for [[Transaction]] — exposed for tests that need to build malformed records
    * directly (e.g. the union-mismatch case).
    */
  lazy val transactionSchema: Schema = summon[AvroCodec[Transaction]].schema

  /** Sealed-trait sum used by the `.union[Branch]` happy-path tests. Mirrors the probe ADT — two
    * record-shaped subclasses, deliberately top-level so the kindlings macros aren't tripped by
    * outer accessors.
    */
  sealed trait Payment

  object Payment:

    given AvroEncoder[Payment] = AvroEncoder.derived
    given AvroDecoder[Payment] = AvroDecoder.derived
    given AvroSchemaFor[Payment] = AvroSchemaFor.derived

    given AvroEncoder[Cash] = AvroEncoder.derived
    given AvroDecoder[Cash] = AvroDecoder.derived
    given AvroSchemaFor[Cash] = AvroSchemaFor.derived

    given AvroEncoder[Card] = AvroEncoder.derived
    given AvroDecoder[Card] = AvroDecoder.derived
    given AvroSchemaFor[Card] = AvroSchemaFor.derived

  case class Cash(amount: Long) extends Payment
  case class Card(number: String) extends Payment

  /** Envelope-shaped record with a MID-RECORD union field: `payment` (the [[Payment]] union) sits
    * between scalar fields on both sides, so the byte-span specs get a real prefix (`id`, `seq`)
    * and a real suffix (`note`) around the sliced / grafted span. Mirrors the Kafka "envelope +
    * passthrough payload" shape the slice/graft surface was built for.
    */
  case class WireEnvelope(id: String, seq: Long, payment: Payment, note: String)

  object WireEnvelope:

    given AvroEncoder[WireEnvelope] = AvroEncoder.derived
    given AvroDecoder[WireEnvelope] = AvroDecoder.derived
    given AvroSchemaFor[WireEnvelope] = AvroSchemaFor.derived

  /** Reader schema for [[WireEnvelope]] — used by the byte-span specs to re-encode / re-decode. */
  lazy val envelopeSchema: Schema = summon[AvroCodec[WireEnvelope]].schema

  /** Union whose branches carry MULTIPLE fields — the fixture for `.union[Branch].fields(...)`
    * composition (a single-field branch can't feed `.fields`, which needs arity ≥ 2).
    */
  sealed trait Contact

  object Contact:

    given AvroEncoder[Contact] = AvroEncoder.derived
    given AvroDecoder[Contact] = AvroDecoder.derived
    given AvroSchemaFor[Contact] = AvroSchemaFor.derived

    given AvroEncoder[Email] = AvroEncoder.derived
    given AvroDecoder[Email] = AvroDecoder.derived
    given AvroSchemaFor[Email] = AvroSchemaFor.derived

    given AvroEncoder[Phone] = AvroEncoder.derived
    given AvroDecoder[Phone] = AvroDecoder.derived
    given AvroSchemaFor[Phone] = AvroSchemaFor.derived

  case class Email(user: String, domain: String) extends Contact
  case class Phone(country: String, number: String) extends Contact

  /** Record with a union field whose branches are multi-field — hosts the Fields-under-union
    * composition tests (`.field(_.contact).union[Email].fields(_.user, _.domain)`).
    */
  case class Directory(id: String, contact: Contact)

  object Directory:

    given AvroEncoder[Directory] = AvroEncoder.derived
    given AvroDecoder[Directory] = AvroDecoder.derived
    given AvroSchemaFor[Directory] = AvroSchemaFor.derived

  lazy val directorySchema: Schema = summon[AvroCodec[Directory]].schema

  def directoryRecord(d: Directory): GenericRecord =
    summon[AvroCodec[Directory]].encode(d).asInstanceOf[GenericRecord]

  /** Scala 3 untagged-union branches (issue #37). Two record-shaped members with distinct
    * full-names so `.union[Ping]` / `.union[Pong]` resolve by `getFullName` off the branch codec,
    * exactly as the sealed-trait path does — kindlings derives the same `union<Ping, Pong>` schema
    * for the `A | B` field as for a sealed trait.
    */
  case class Ping(seq: Long)
  case class Pong(label: String)

  object Ping:

    given AvroEncoder[Ping] = AvroEncoder.derived
    given AvroDecoder[Ping] = AvroDecoder.derived
    given AvroSchemaFor[Ping] = AvroSchemaFor.derived

  object Pong:

    given AvroEncoder[Pong] = AvroEncoder.derived
    given AvroDecoder[Pong] = AvroDecoder.derived
    given AvroSchemaFor[Pong] = AvroSchemaFor.derived

  /** Record with an untagged-union-typed field `payload: Ping | Pong` — the real #37 shape:
    * `codecPrism[Beacon].field(_.payload).union[Ping]` used to abort at compile time on the
    * `OrType` focus. Kindlings derives a codec for the union field directly.
    */
  case class Beacon(id: String, payload: Ping | Pong)

  object Beacon:

    given AvroEncoder[Beacon] = AvroEncoder.derived
    given AvroDecoder[Beacon] = AvroDecoder.derived
    given AvroSchemaFor[Beacon] = AvroSchemaFor.derived

  lazy val beaconSchema: Schema = summon[AvroCodec[Beacon]].schema

  def beaconRecord(b: Beacon): GenericRecord =
    summon[AvroCodec[Beacon]].encode(b).asInstanceOf[GenericRecord]

  /** Array-of-union record — hosts the `.entries.each.union[Cash]` composition (per-element union
    * branch focus) including under non-canonical block framing.
    */
  case class Ledger(owner: String, entries: List[Payment])

  object Ledger:

    given AvroEncoder[Ledger] = AvroEncoder.derived
    given AvroDecoder[Ledger] = AvroDecoder.derived
    given AvroSchemaFor[Ledger] = AvroSchemaFor.derived

  lazy val ledgerSchema: Schema = summon[AvroCodec[Ledger]].schema

  def ledgerRecord(l: Ledger): GenericRecord =
    summon[AvroCodec[Ledger]].encode(l).asInstanceOf[GenericRecord]

  /** Build a kindlings-encoded record for an [[WireEnvelope]]. */
  def envelopeRecord(e: WireEnvelope): GenericRecord =
    summon[AvroCodec[WireEnvelope]].encode(e).asInstanceOf[GenericRecord]

  /** Encode a single VALUE (not necessarily a record) to Avro binary under `schema` — the
    * counterpart to [[toBinary]] for the fragment-fidelity assertions, where the sliced span is a
    * bare `long` / union-branch record rather than a top-level record.
    */
  def toBinaryValue(value: Any, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    val writer = new GenericDatumWriter[Any](schema)
    writer.write(value, encoder)
    encoder.flush()
    out.toByteArray

  /** Decode a single VALUE from Avro binary under `schema` — inverse of [[toBinaryValue]]. */
  def fromBinaryValue(bytes: Array[Byte], schema: Schema): Any =
    val reader = new org.apache.avro.generic.GenericDatumReader[Any](schema)
    reader.read(null, org.apache.avro.io.DecoderFactory.get().binaryDecoder(bytes, null))

  /** Build a kindlings-encoded Transaction record. */
  def transactionRecord(t: Transaction): GenericRecord =
    summon[AvroCodec[Transaction]].encode(t).asInstanceOf[GenericRecord]

  /** Build a kindlings-encoded `IndexedRecord` for a `Person`. The encoder produces an
    * `org.apache.avro.generic.GenericData.Record` whose positional slots line up with the schema's
    * field order.
    */
  def personRecord(p: Person): GenericRecord =
    summon[AvroCodec[Person]].encode(p).asInstanceOf[GenericRecord]

  /** Build a kindlings-encoded record for an [[Order]]. */
  def orderRecord(o: Order): GenericRecord =
    summon[AvroCodec[Order]].encode(o).asInstanceOf[GenericRecord]

  /** Build a kindlings-encoded record for a [[Basket]]. */
  def basketRecord(b: Basket): GenericRecord =
    summon[AvroCodec[Basket]].encode(b).asInstanceOf[GenericRecord]

  /** Build a basket-shaped root record `{ owner: "Alice", items: [...] }` whose `items` array is
    * filled with the supplied raw element values. Mirrors `JsonSpecFixtures.basketRoot` — used by
    * the traversal specs whose scenarios all share this "wrap a sequence of element values as a
    * Basket payload" shape.
    *
    * The `items` field is allocated as a [[GenericData.Array]] under the basket schema's array
    * field type; raw `IndexedRecord` elements (or even `Utf8`-shaped strings) can be stuffed in
    * directly to test the per-element failure paths.
    */
  def basketRoot(elems: Seq[AnyRef]): GenericRecord =
    val itemsField = basketSchema.getField("items")
    val arr = new GenericData.Array[AnyRef](elems.size, itemsField.schema)
    elems.foreach(arr.add)
    val rec = new GenericData.Record(basketSchema)
    rec.put(basketSchema.getField("owner").pos, "Alice")
    rec.put(itemsField.pos, arr)
    rec

  /** Encode a record to its binary wire representation under the supplied schema. Used by the
    * parse-helper round-trip tests.
    */
  def toBinary(record: GenericRecord, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    val writer = new GenericDatumWriter[GenericRecord](schema)
    writer.write(record, encoder)
    encoder.flush()
    out.toByteArray

  /** Build a record by name + value pairs against the supplied schema, useful when the spec wants a
    * "broken" record for failure-path tests.
    */
  def buildRecord(schema: Schema)(fields: (String, AnyRef)*): GenericRecord =
    val b = new GenericRecordBuilder(schema)
    fields.foreach { case (n, v) => b.set(n, v) }
    b.build()

  /** Build an `org.apache.avro.generic.GenericData.Array` from a Scala iterable. The walker's
    * `NotAnArray` / `IndexOutOfRange` paths trigger on this runtime type.
    */
  def buildArray[A](elemSchema: Schema, elems: Iterable[A]): GenericData.Array[A] =
    val arr = new GenericData.Array[A](elems.size, Schema.createArray(elemSchema))
    elems.foreach(arr.add)
    arr

  /** Convenience: an empty `Chain[AvroFailure]` for assertions that compare against
    * `Ior.Right(_)`-shaped happy-path expectations.
    */
  val emptyChain: Chain[AvroFailure] = Chain.empty[AvroFailure]

end AvroSpecFixtures
