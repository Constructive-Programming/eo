package dev.constructive.eo.avro.jsoniter

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumWriter, IndexedRecord}
import org.apache.avro.io.EncoderFactory
import org.apache.avro.util.Utf8
import org.specs2.mutable.Specification

/** Behaviour of the structural Avro → JSON-bytes bridge. The rendering conventions under test are
  * `AvroJson`'s (see that scaladoc) — this spec pins the jsoniter rendering to exact JSON text so a
  * drift in either walk is visible.
  */
class AvroJsoniterSpec extends Specification:

  val schema: Schema = new Schema.Parser().parse(
    """{
      |  "type": "record", "name": "Everything", "fields": [
      |    {"name": "s",      "type": "string"},
      |    {"name": "i",      "type": "int"},
      |    {"name": "l",      "type": "long"},
      |    {"name": "f",      "type": "float"},
      |    {"name": "d",      "type": "double"},
      |    {"name": "b",      "type": "boolean"},
      |    {"name": "opt",    "type": ["null", "int"]},
      |    {"name": "e",      "type": {"type": "enum", "name": "Color", "symbols": ["RED", "GREEN"]}},
      |    {"name": "fx",     "type": {"type": "fixed", "name": "Fx", "size": 2}},
      |    {"name": "by",     "type": "bytes"},
      |    {"name": "arr",    "type": {"type": "array", "items": "int"}},
      |    {"name": "map",    "type": {"type": "map", "values": "string"}},
      |    {"name": "nested", "type": {"type": "record", "name": "Inner",
      |                                "fields": [{"name": "v", "type": "int"}]}}
      |  ]
      |}""".stripMargin
  )

  def fieldSchema(name: String): Schema = schema.getField(name).schema

  /** One value per rendering branch. Map is single-entry: `GenericDatumReader` reads maps into a
    * `HashMap`, so a multi-entry map has no guaranteed iteration order after the binary round-trip
    * in the `bytesToJson` example below.
    */
  def everything: GenericData.Record =
    val nested = new GenericData.Record(fieldSchema("nested"))
    nested.put("v", 7)
    val map = new java.util.LinkedHashMap[String, Any]()
    map.put("k", new Utf8("v"))
    val arr = new java.util.ArrayList[Any]()
    arr.add(1)
    arr.add(2)
    val rec = new GenericData.Record(schema)
    rec.put("s", new Utf8("héllo \"quoted\""))
    rec.put("i", 42)
    rec.put("l", 9007199254740993L)
    rec.put("f", 1.5f)
    rec.put("d", 2.25d)
    rec.put("b", true)
    rec.put("opt", null)
    rec.put("e", new GenericData.EnumSymbol(fieldSchema("e"), "GREEN"))
    rec.put("fx", new GenericData.Fixed(fieldSchema("fx"), Array[Byte](1, -128)))
    rec.put("by", ByteBuffer.wrap(Array[Byte](0, -1)))
    rec.put("arr", arr)
    rec.put("map", map)
    rec.put("nested", nested)
    rec

  def toAvroBinary(rec: IndexedRecord): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[IndexedRecord](rec.getSchema).write(rec, encoder)
    encoder.flush()
    out.toByteArray

  "avroToJson" should {
    "render every branch of the walk with AvroJson's conventions" in {
      new String(AvroJsoniter.avroToJson(everything), UTF_8) ===
        """{"s":"héllo \"quoted\"","i":42,"l":9007199254740993,"f":1.5,"d":2.25,"b":true,""" +
        """"opt":null,"e":"GREEN","fx":[1,-128],"by":[0,-1],"arr":[1,2],"map":{"k":"v"},""" +
        """"nested":{"v":7}}"""
    }

    "render non-finite floats and doubles as null (fromFloatOrNull convention)" in {
      val rec = everything
      rec.put("f", Float.NaN)
      rec.put("d", Double.PositiveInfinity)
      val json = new String(AvroJsoniter.avroToJson(rec), UTF_8)
      json must contain(""""f":null""")
      json must contain(""""d":null""")
    }
  }

  "bytesToJson" should {
    "agree with the direct walk after an Avro binary round-trip" in {
      val rec = everything
      new String(AvroJsoniter.bytesToJson(schema).get(toAvroBinary(rec)), UTF_8) ===
        new String(AvroJsoniter.avroToJson(rec), UTF_8)
    }
  }
