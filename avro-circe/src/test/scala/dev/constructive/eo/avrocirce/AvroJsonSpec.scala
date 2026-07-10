package dev.constructive.eo.avrocirce

import scala.jdk.CollectionConverters.*

import io.circe.Json
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroJson]] — the structural Avro → circe read bridge.
  *
  * The generic records are built directly with the apache-avro generic API (`Schema.Parser` +
  * `GenericData`), so the spec needs no codec / kindlings dependency: each rendering convention is
  * pinned against a hand-built `Json` for the same value, and [[AvroJson.bytesToJson]] is proved to
  * agree with the base [[AvroJson.avroToJson]] across a binary round-trip.
  */
class AvroJsonSpec extends Specification with ScalaCheck:

  // ---- Schema + sub-schemas -----------------------------------------

  private val envSchema: Schema = new Schema.Parser().parse(
    """{"type":"record","name":"Envelope","namespace":"dev.constructive.eo.avrocirce.test","fields":[
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

  private val tagsSchema: Schema      = envSchema.getField("tags").schema
  private val eventDataSchema: Schema = envSchema.getField("event_data").schema
  private val payloadSchema: Schema   = eventDataSchema.getTypes.get(1)

  // ---- Model + generators -------------------------------------------

  private final case class Payload(kind: String, amount: Long, note: Option[String])

  private final case class Env(
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
      kind   <- genStr
      amount <- Gen.long
      note   <- Gen.option(genStr)
    yield Payload(kind, amount, note)

  private given Arbitrary[Env] = Arbitrary(
    for
      source   <- genStr
      clickId  <- Gen.option(genStr)
      count    <- Gen.long
      level    <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
      ratio    <- Gen.chooseNum(-1e9, 1e9)
      flavor   <- Gen.chooseNum(-1e6, 1e6).map(_.toFloat)
      active   <- Gen.oneOf(true, false)
      tags     <- Gen.listOf(genStr).map(_.take(6))
      metadata <- Gen
        .listOf(Gen.zip(Gen.alphaNumStr.map(_.take(8)).suchThat(_.nonEmpty), genStr))
        .map(_.take(4).toMap)
      payload  <- Gen.option(genPayload)
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
    val r    = new GenericData.Record(envSchema)
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
      "source"     -> Json.fromString(e.source),
      "click_id"   -> e.clickId.fold(Json.Null)(Json.fromString),
      "count"      -> Json.fromLong(e.count),
      "level"      -> Json.fromLong(e.level.toLong),
      "ratio"      -> Json.fromDoubleOrNull(e.ratio),
      "flavor"     -> Json.fromFloatOrNull(e.flavor),
      "active"     -> Json.fromBoolean(e.active),
      "tags"       -> Json.fromValues(e.tags.map(Json.fromString)),
      "metadata"   -> Json.fromFields(e.metadata.map((k, v) => k -> Json.fromString(v))),
      "event_data" -> e.eventData.fold(Json.Null)(p =>
        Json.obj(
          "kind"   -> Json.fromString(p.kind),
          "amount" -> Json.fromLong(p.amount),
          "note"   -> p.note.fold(Json.Null)(Json.fromString),
        )
      ),
    )

  private def toBinary(r: GenericRecord): Array[Byte] =
    val out     = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    val writer  = new GenericDatumWriter[GenericRecord](envSchema)
    writer.write(r, encoder)
    encoder.flush()
    out.toByteArray

  // ---- Properties ----------------------------------------------------

  // covers: record→object, string, optional-null/present union, long, int→long, double, float,
  //   boolean, array, map→object, nested optional record recursion — each rendered structurally
  "avroToJson renders an Envelope structurally identical to a hand-built Json" >> prop {
    (e: Env) => AvroJson.avroToJson(record(e)) === expected(e)
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

  // ---- Leaf renderings with no source coverage in the property schema

  // covers: enum → fromString; bytes (ByteBuffer) → array of signed byte ints; fixed → same
  "enum, bytes and fixed leaf renderings" >> {
    val schema = new Schema.Parser().parse(
      """{"type":"record","name":"Leaves","namespace":"dev.constructive.eo.avrocirce.test","fields":[
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
