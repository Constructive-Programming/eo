package dev.constructive.eo.avro.jsoniter

import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
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

  import AvroJsoniterSpec.given

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

  // ---- valuePrism and its torn/mended diagonal family ----

  val comboCodec = summon[AvroCodec[Combo]]
  val combo = Combo("x", 9L, active = true)
  def comboRec: IndexedRecord = comboCodec.encode(combo).asInstanceOf[IndexedRecord]

  def str(bytes: Array[Byte]): String = new String(bytes, UTF_8)

  "valuePrism" should {
    "tear a generic value into a typed A; mend renders JSON bytes" in {
      val p = AvroJsoniter.valuePrism[Combo]
      (p.getOption(comboRec) === Some(combo))
        .and(str(p.reverseGet(comboRec)) === str(AvroJsoniter.avroToJson(comboRec)))
    }

    "surrender the structural JSON-bytes view on a decode miss" in {
      val rejecting = new AvroCodec[Combo]:
        def schema = comboCodec.schema
        def encode(c: Combo) = comboCodec.encode(c)
        def decodeEither(any: Any) = Left(new RuntimeException("rejected"))
      AvroJsoniter.valuePrism[Combo](using rejecting).tear(comboRec).left.map(str) ===
        Left(str(AvroJsoniter.avroToJson(comboRec)))
    }
  }

  "bytesPrism" should {
    "tear typed off Avro bytes and mend typed back out as JSON bytes" in {
      val bytes = toAvroBinary(comboRec)
      val upper = comboCodec.encode(combo.copy(name = "X")).asInstanceOf[IndexedRecord]
      (AvroJsoniter.bytesPrism[Combo].getOption(bytes) === Some(combo))
        .and(
          str(AvroJsoniter.bytesPrism[Combo].modify(_.copy(name = "X"))(bytes)) ===
            str(AvroJsoniter.avroToJson(upper))
        )
    }
  }

  "recordPrism" should {
    "tear typed off a resolved record and mend a record out as JSON bytes" in {
      (AvroJsoniter.recordPrism[Combo].getOption(comboRec) === Some(combo))
        .and(
          str(AvroJsoniter.recordPrism[Combo].modify(_ => comboRec)(comboRec)) ===
            str(AvroJsoniter.avroToJson(comboRec))
        )
    }
  }

  // ---- .json face: drilled cursor in, JSON document bytes out ----

  val bagCodec = summon[AvroCodec[Bag]]
  val bag = Bag("t", List(1, 2, 3))
  def bagRec(b: Bag): IndexedRecord = bagCodec.encode(b).asInstanceOf[IndexedRecord]

  ".json face" should {
    "read the typed focus and modify the whole document out as JSON bytes" in {
      val bytes = toAvroBinary(comboRec)
      val p = codecPrism[Combo].field(_.name).json
      val upper = comboCodec.encode(combo.copy(name = "X")).asInstanceOf[IndexedRecord]
      (p.getOption(bytes) === Some("x"))
        .and(str(p.modify(_.toUpperCase)(bytes)) === str(AvroJsoniter.avroToJson(upper)))
    }

    "drill via Dynamic selection and .fields" in {
      val bytes = toAvroBinary(comboRec)
      val nameAndActive = codecPrism[Combo].fields(_.name, _.active)
      (codecPrism[Combo].name.json.getOption(bytes) === Some("x"))
        .and(nameAndActive.json.getOption(bytes).map(_.toTuple) === Some(("x", true)))
    }

    ".at drills an element; .each folds and rewrites every element" in {
      val bytes = toAvroBinary(bagRec(bag))
      val bumped = bagRec(Bag("t", List(2, 3, 4)))
      (codecPrism[Bag].field(_.items).at(1).json.getOption(bytes) === Some(2))
        .and(
          str(codecPrism[Bag].field(_.items).each.json.modify(_ + 1)(bytes)) ===
            str(AvroJsoniter.avroToJson(bumped))
        )
    }

    "a path miss renders the document unchanged" in {
      val bytes = toAvroBinary(bagRec(bag))
      str(codecPrism[Bag].field(_.items).at(9).json.replace(0)(bytes)) ===
        str(AvroJsoniter.avroToJson(bagRec(bag)))
    }

    "render: read the drilled focus as a standalone JSON document" in {
      codecPrism[Combo]
        .field(_.name)
        .andThen(AvroJsoniter.render[String])
        .getOption(toAvroBinary(comboRec))
        .map(str) === Some("\"x\"")
    }
  }

object AvroJsoniterSpec:

  /** NamedTuple codec for the `.fields(_.name, _.active)` focus — pre-derived at object scope
    * (deriving inline inside the spec body trips a macro-expansion scoping error, same pattern as
    * `AvroFieldsTraversalSpec`).
    */
  type NameActive = NamedTuple.NamedTuple[("name", "active"), (String, Boolean)]

  given AvroEncoder[NameActive] = AvroEncoder.derived
  given AvroDecoder[NameActive] = AvroDecoder.derived
  given AvroSchemaFor[NameActive] = AvroSchemaFor.derived

end AvroJsoniterSpec

/** Codec fixtures at top level (kindlings derivation macro-facing convention). */
case class Combo(name: String, size: Long, active: Boolean)
case class Bag(tag: String, items: List[Int])
