package dev.constructive.eo.avro.circe

import scala.jdk.CollectionConverters.*

import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import io.circe.Json
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericRecord, IndexedRecord}
import org.apache.avro.io.EncoderFactory
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroJson]] — the structural Avro ↔ circe bridge.
  *
  * The generic records are built directly with the apache-avro generic API (`Schema.Parser` +
  * `GenericData`), so the spec needs no codec / kindlings dependency: each rendering convention is
  * pinned against a hand-built `Json` for the same value, [[AvroJson.bytesToJson]] is proved to
  * agree with the base [[AvroJson.avroToJson]] across a binary round-trip, and the
  * [[AvroJson.record]] prism's two round-trip laws are property-pinned (record equality via binary
  * encoding — `Utf8` vs `String` field values are wire-identical but not `equals`).
  */
class AvroJsonSpec extends Specification with ScalaCheck:

  // ---- Schema + sub-schemas -----------------------------------------

  private val envSchema: Schema = new Schema.Parser().parse(
    """{"type":"record","name":"Envelope","namespace":"dev.constructive.eo.avro.circe.test","fields":[
      |  {"name":"source","type":"string"},
      |  {"name":"click_id","type":["null","string"]},
      |  {"name":"count","type":"long"},
      |  {"name":"level","type":"int"},
      |  {"name":"ratio","type":"double"},
      |  {"name":"flavor","type":"float"},
      |  {"name":"active","type":"boolean"},
      |  {"name":"tags","type":{"type":"array","items":"string"}},
      |  {"name":"metadata","type":{"type":"map","values":"string"}},
      |  {"name":"event_data","type":["null",
      |    {"type":"record","name":"Payload","fields":[
      |      {"name":"kind","type":"string"},
      |      {"name":"amount","type":"long"},
      |      {"name":"note","type":["null","string"]}]}]}
      |]}""".stripMargin
  )

  private val tagsSchema: Schema = envSchema.getField("tags").schema
  private val eventDataSchema: Schema = envSchema.getField("event_data").schema
  private val payloadSchema: Schema = eventDataSchema.getTypes.get(1)

  // ---- Model + generators -------------------------------------------

  final private case class Payload(kind: String, amount: Long, note: Option[String])

  final private case class Env(
      source: String,
      clickId: Option[String],
      count: Long,
      level: Int,
      ratio: Double,
      flavor: Float,
      active: Boolean,
      tags: List[String],
      metadata: Map[String, String],
      eventData: Option[Payload],
  )

  private val genStr: Gen[String] =
    Gen.frequency(1 -> Gen.const(""), 9 -> Gen.alphaNumStr.map(_.take(16)))

  private val genPayload: Gen[Payload] =
    for
      kind <- genStr
      amount <- Gen.long
      note <- Gen.option(genStr)
    yield Payload(kind, amount, note)

  private given Arbitrary[Env] = Arbitrary(
    for
      source <- genStr
      clickId <- Gen.option(genStr)
      count <- Gen.long
      level <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
      ratio <- Gen.chooseNum(-1e9, 1e9)
      flavor <- Gen.chooseNum(-1e6, 1e6).map(_.toFloat)
      active <- Gen.oneOf(true, false)
      tags <- Gen.listOf(genStr).map(_.take(6))
      metadata <- Gen
        .listOf(Gen.zip(Gen.alphaNumStr.map(_.take(8)).suchThat(_.nonEmpty), genStr))
        .map(_.take(4).toMap)
      payload <- Gen.option(genPayload)
    yield Env(source, clickId, count, level, ratio, flavor, active, tags, metadata, payload)
  )

  // ---- Record + expected-Json builders ------------------------------

  private def payloadRecord(p: Payload): GenericRecord =
    val r = new GenericData.Record(payloadSchema)
    r.put("kind", p.kind)
    r.put("amount", p.amount)
    r.put("note", p.note.orNull)
    r

  private def record(e: Env): GenericRecord =
    val r = new GenericData.Record(envSchema)
    val tags = new GenericData.Array[String](e.tags.size, tagsSchema)
    e.tags.foreach(tags.add)
    val meta = new java.util.HashMap[String, String]()
    e.metadata.foreach((k, v) => meta.put(k, v))
    r.put("source", e.source)
    r.put("click_id", e.clickId.orNull)
    r.put("count", e.count)
    r.put("level", e.level)
    r.put("ratio", e.ratio)
    r.put("flavor", e.flavor)
    r.put("active", e.active)
    r.put("tags", tags)
    r.put("metadata", meta)
    r.put("event_data", e.eventData.map(payloadRecord).orNull)
    r

  private def expected(e: Env): Json =
    Json.obj(
      "source" -> Json.fromString(e.source),
      "click_id" -> e.clickId.fold(Json.Null)(Json.fromString),
      "count" -> Json.fromLong(e.count),
      "level" -> Json.fromLong(e.level.toLong),
      "ratio" -> Json.fromDoubleOrNull(e.ratio),
      "flavor" -> Json.fromFloatOrNull(e.flavor),
      "active" -> Json.fromBoolean(e.active),
      "tags" -> Json.fromValues(e.tags.map(Json.fromString)),
      "metadata" -> Json.fromFields(e.metadata.map((k, v) => k -> Json.fromString(v))),
      "event_data" -> e
        .eventData
        .fold(Json.Null)(p =>
          Json.obj(
            "kind" -> Json.fromString(p.kind),
            "amount" -> Json.fromLong(p.amount),
            "note" -> p.note.fold(Json.Null)(Json.fromString),
          )
        ),
    )

  private def toBinary(r: IndexedRecord): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    val writer = new GenericDatumWriter[IndexedRecord](envSchema)
    writer.write(r, encoder)
    encoder.flush()
    out.toByteArray

  // ---- Properties ----------------------------------------------------

  // covers: record→object, string, optional-null/present union, long, int→long, double, float,
  //   boolean, array, map→object, nested optional record recursion — each rendered structurally
  "avroToJson renders an Envelope structurally identical to a hand-built Json" >> prop { (e: Env) =>
    AvroJson.avroToJson(record(e)) === expected(e)
  }

  // covers: the bytesToJson read optic (parse-to-record ∘ structural walk, fused Getter.andThen)
  //   agrees with the base function across a binary round-trip (strings return as Utf8, still
  //   fromString; ints as Integer; longs as Long; maps Utf8-keyed — same rendering)
  "bytesToJson read optic agrees with avroToJson on the same bytes" >> prop { (e: Env) =>
    AvroJson.bytesToJson(envSchema).get(toBinary(record(e))) === AvroJson.avroToJson(record(e))
  }

  // covers: top-level object key order == schema field declaration order
  "keys of the rendered object equal the schema field names in order" >> prop { (e: Env) =>
    AvroJson.avroToJson(record(e)).asObject.map(_.keys.toList) ===
      Some(envSchema.getFields.asScala.map(_.name).toList)
  }

  // covers: an absent optional union branch renders as Json.Null (not omitted, not a nested object)
  "an absent optional union branch renders as Json.Null" >> prop { (e0: Env) =>
    val e = e0.copy(eventData = None)
    AvroJson.avroToJson(record(e)).asObject.flatMap(_("event_data")) === Some(Json.Null)
  }

  // ---- The record prism (Json ↔ IndexedRecord) -----------------------

  private val envPrism = AvroJson.record(envSchema)

  // covers: prism law reverseGet-then-getOption — the strict parse inverts the walk; record
  //   equality via binary encoding (Utf8/String and Integer boxing differences are wire-invisible)
  "record prism: getOption ∘ reverseGet round-trips every Env record" >> prop { (e: Env) =>
    val original = record(e)
    envPrism.getOption(envPrism.reverseGet(original)).map(r => toBinary(r).toSeq) ===
      Some(toBinary(original).toSeq)
  }

  // covers: prism law getOption-then-reverseGet — a walked (canonical) document survives
  //   parse + re-render byte-for-byte
  "record prism: reverseGet ∘ getOption reproduces a walked document" >> prop { (e: Env) =>
    val json = AvroJson.avroToJson(record(e))
    envPrism.getOption(json).map(envPrism.reverseGet) === Some(json)
  }

  // covers: strictness — the misses that keep the prism lawful (extra key, missing key,
  //   type mismatch, int overflow, Null under a non-nullable field)
  "record prism: strict misses" >> {
    val good = AvroJson.avroToJson(record(Env("s", None, 1L, 2, 0.5, 0.5f, true, Nil, Map(), None)))
    (envPrism.getOption(good.mapObject(_.add("extra", Json.True))) === None)
      .and(envPrism.getOption(good.mapObject(_.remove("count"))) === None)
      .and(envPrism.getOption(good.mapObject(_.add("count", Json.fromString("NaN")))) === None)
      .and(envPrism.getOption(good.mapObject(_.add("level", Json.fromLong(1L << 40)))) === None)
      .and(envPrism.getOption(good.mapObject(_.add("source", Json.Null))) === None)
      .and(envPrism.getOption(Json.fromString("not an object")) === None)
  }

  // covers: enum symbol / fixed length / bytes shape parse both ways
  "record prism: enum, bytes and fixed round-trip and reject malformed leaves" >> {
    val schema = new Schema.Parser().parse(
      """{"type":"record","name":"LeavesRT","namespace":"dev.constructive.eo.avro.circe.test","fields":[
        |  {"name":"color","type":{"type":"enum","name":"ColorRT","symbols":["RED","GREEN"]}},
        |  {"name":"blob","type":"bytes"},
        |  {"name":"tag","type":{"type":"fixed","name":"TagRT","size":3}}
        |]}""".stripMargin
    )
    val prism = AvroJson.record(schema)
    val json = Json.obj(
      "color" -> Json.fromString("GREEN"),
      "blob" -> Json.arr(Json.fromInt(1), Json.fromInt(-2)),
      "tag" -> Json.arr(Json.fromInt(-1), Json.fromInt(0), Json.fromInt(127)),
    )
    (prism.getOption(json).map(prism.reverseGet) === Some(json))
      .and(
        prism.getOption(json.mapObject(_.add("color", Json.fromString("BLUE")))) === None
      )
      .and(
        prism.getOption(
          json.mapObject(_.add("tag", Json.arr(Json.fromInt(1), Json.fromInt(2))))
        ) === None
      )
      .and(
        prism.getOption(json.mapObject(_.add("blob", Json.arr(Json.fromInt(200))))) === None
      )
  }

  // ---- valuePrism and its torn/mended diagonal family ----

  private val comboCodec = summon[dev.constructive.eo.avro.AvroCodec[Combo]]
  private val combo = Combo("x", 9L, active = true)
  private val comboRec = comboCodec.encode(combo).asInstanceOf[IndexedRecord]

  private def binary(rec: IndexedRecord, schema: Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[IndexedRecord](schema).write(rec, encoder)
    encoder.flush()
    out.toByteArray

  // covers: the fundamental diagonal — typed tear off a generic value, Json mend of any generic
  //   value; a codec-level decode miss surrenders the structural Json view, not the input
  "valuePrism: typed tear, Json mend, decode-miss falls back to structural Json" >> {
    val rejecting = new dev.constructive.eo.avro.AvroCodec[Combo]:
      def schema = comboCodec.schema
      def encode(c: Combo) = comboCodec.encode(c)
      def decodeEither(any: Any) = Left(new RuntimeException("rejected"))

    val p = AvroJson.valuePrism[Combo]
    (p.getOption(comboRec) === Some(combo))
      .and(p.reverseGet(comboRec) === AvroJson.avroToJson(comboRec))
      .and(
        AvroJson.valuePrism[Combo](using rejecting).tear(comboRec) ===
          Left(AvroJson.avroToJson(comboRec))
      )
  }

  // covers: each tearFrom/mendFrom variant of valuePrism — pPrism (bytes tear, record mend), bytesPrism
  //   (bytes tear, typed mend via encode), recordPrism (record tear, typed mend), pRecord
  //   (bytes tear, untyped record focus, no codec)
  "torn/mended diagonals: pPrism, bytesPrism, recordPrism, pRecord" >> {
    val bytes = binary(comboRec, comboCodec.schema)
    val upper = comboCodec.encode(combo.copy(name = "X")).asInstanceOf[IndexedRecord]

    (AvroJson.pPrism[Combo].getOption(bytes) === Some(combo))
      .and(AvroJson.pPrism[Combo].modify(_ => comboRec)(bytes) === AvroJson.avroToJson(comboRec))
      .and(
        AvroJson.bytesPrism[Combo].modify(c => c.copy(name = c.name.toUpperCase))(bytes) ===
          AvroJson.avroToJson(upper)
      )
      .and(AvroJson.recordPrism[Combo].getOption(comboRec) === Some(combo))
      .and(
        AvroJson.recordPrism[Combo].modify(c => c.copy(size = 10L))(comboRec) ===
          AvroJson.avroToJson(
            comboCodec.encode(combo.copy(size = 10L)).asInstanceOf[IndexedRecord]
          )
      )
      .and(
        AvroJson.pRecord(comboCodec.schema).getOption(bytes).map(AvroJson.avroToJson) ===
          Some(AvroJson.avroToJson(comboRec))
      )
  }

  // covers: the writer-schema overload resolves a field-reordered writer schema by name before
  //   the typed decode (the plain overload is position-based and would misread these bytes)
  "bytesPrism(writer): resolves a reordered writer schema" >> {
    val rs = comboCodec.schema
    val writer = Schema.createRecord(
      rs.getName,
      null,
      rs.getNamespace,
      false,
      rs.getFields.asScala.toList.reverse.map(f => new Schema.Field(f.name, f.schema)).asJava,
    )
    val wrec = new GenericData.Record(writer)
    wrec.put("name", combo.name)
    wrec.put("size", combo.size)
    wrec.put("active", combo.active)

    AvroJson.bytesPrism[Combo](writer).getOption(binary(wrec, writer)) === Some(combo)
  }

  // ---- Leaf renderings with no source coverage in the property schema

  // covers: enum → fromString; bytes (ByteBuffer) → array of signed byte ints; fixed → same
  "enum, bytes and fixed leaf renderings" >> {
    val schema = new Schema.Parser().parse(
      """{"type":"record","name":"Leaves","namespace":"dev.constructive.eo.avro.circe.test","fields":[
        |  {"name":"color","type":{"type":"enum","name":"Color","symbols":["RED","GREEN"]}},
        |  {"name":"blob","type":"bytes"},
        |  {"name":"tag","type":{"type":"fixed","name":"Tag","size":3}}
        |]}""".stripMargin
    )
    val record = new GenericData.Record(schema)
    record.put("color", new GenericData.EnumSymbol(schema.getField("color").schema, "GREEN"))
    record.put("blob", java.nio.ByteBuffer.wrap(Array[Byte](1, -2, 3)))
    record.put("tag", new GenericData.Fixed(schema.getField("tag").schema, Array[Byte](-1, 0, 127)))

    val json = AvroJson.avroToJson(record)
    (json.asObject.flatMap(_("color")) === Some(Json.fromString("GREEN")))
      .and(
        json.asObject.flatMap(_("blob")) ===
          Some(Json.arr(Json.fromInt(1), Json.fromInt(-2), Json.fromInt(3)))
      )
      .and(
        json.asObject.flatMap(_("tag")) ===
          Some(Json.arr(Json.fromInt(-1), Json.fromInt(0), Json.fromInt(127)))
      )
  }

end AvroJsonSpec

/** Top-level so kindlings derivation needs no outer accessor (same reason the fixture ADTs in
  * `AvroSpecFixtures` live in an object, not the spec class).
  */
case class Combo(name: String, size: Long, active: Boolean)

object Combo:
  given AvroEncoder[Combo] = AvroEncoder.derived
  given AvroDecoder[Combo] = AvroDecoder.derived
  given AvroSchemaFor[Combo] = AvroSchemaFor.derived
