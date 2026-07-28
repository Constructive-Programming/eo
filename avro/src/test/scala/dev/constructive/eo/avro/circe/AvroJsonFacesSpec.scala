package dev.constructive.eo.avro.circe

import dev.constructive.eo.avro.{codecPrism, AvroBinaryCursor, AvroCodec}
import dev.constructive.eo.circe.JsonPrism
import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import io.circe.{Decoder, Encoder, Json}
import java.io.ByteArrayOutputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumWriter, IndexedRecord}
import org.apache.avro.io.EncoderFactory
import org.specs2.mutable.Specification

/** Behaviour of the drilled cursor faces on the circe bridge — `.json` on `AvroPrism` /
  * `AvroTraversal` (Avro bytes drilled, whole document out as [[io.circe.Json]]) and the reverse
  * `.avro` on `JsonPrism` (Json drilled, the focused subtree converts to Avro binary). Mirrors the
  * jsoniter module's `AvroJsoniterSpec` face blocks; [[Combo]] is shared with `AvroJsonSpec`.
  */
class AvroJsonFacesSpec extends Specification:

  import AvroJsonFacesSpec.given

  val comboCodec = summon[AvroCodec[Combo]]
  val combo = Combo("x", 9L, active = true)
  def comboRec: IndexedRecord = comboCodec.encode(combo).asInstanceOf[IndexedRecord]
  def nameSchema: Schema = comboCodec.schema.getField("name").schema

  val bagCodec = summon[AvroCodec[Bag]]
  val bag = Bag("t", List(1, 2, 3))
  def bagRec(b: Bag): IndexedRecord = bagCodec.encode(b).asInstanceOf[IndexedRecord]

  def toAvroBinary(rec: IndexedRecord): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[IndexedRecord](rec.getSchema).write(rec, encoder)
    encoder.flush()
    out.toByteArray

  ".json face" should {
    "read the typed focus and modify the whole document out as Json" in {
      val bytes = toAvroBinary(comboRec)
      val p = codecPrism[Combo].field(_.name).json
      val upper = comboCodec.encode(combo.copy(name = "X")).asInstanceOf[IndexedRecord]
      (p.getOption(bytes) === Some("x"))
        .and(p.modify(_.toUpperCase)(bytes) === AvroJson.avroToJson(upper))
    }

    ".at drills an element; .each folds and rewrites every element" in {
      val bytes = toAvroBinary(bagRec(bag))
      val bumped = bagRec(Bag("t", List(2, 3, 4)))
      (codecPrism[Bag].field(_.items).at(1).json.getOption(bytes) === Some(2))
        .and(
          codecPrism[Bag].field(_.items).each.json.modify(_ + 1)(bytes) ===
            AvroJson.avroToJson(bumped)
        )
    }

    "a path miss renders the document unchanged; dynamic drilling works" in {
      val bytes = toAvroBinary(bagRec(bag))
      (codecPrism[Bag].field(_.items).at(9).json.replace(0)(bytes) ===
        AvroJson.avroToJson(bagRec(bag)))
        .and(
          codecPrism[Combo].name.json.getOption(toAvroBinary(comboRec)) === Some("x")
        )
    }

    "render: read the drilled focus as standalone Json" in {
      codecPrism[Combo]
        .field(_.name)
        .andThen(AvroJson.render[String])
        .getOption(toAvroBinary(comboRec)) === Some(Json.fromString("x"))
    }
  }

  ".avro face" should {
    def comboJson: Json = AvroJson.avroToJson(comboRec)

    "read the drilled focus as its Avro binary encoding" in {
      JsonPrism[Combo].field(_.name).avro.getOption(comboJson).map(_.toSeq) ===
        Some(AvroBinaryCursor.writeDatum("x", nameSchema).toSeq)
    }

    "write Avro binary back as a Json subtree splice (structural, no typed value)" in {
      val face = JsonPrism[Combo].field(_.name).avro
      face.replace(AvroBinaryCursor.writeDatum("Z", nameSchema))(comboJson) ===
        comboJson.mapObject(_.add("name", Json.fromString("Z")))
    }

    "drill via Dynamic selection" in {
      JsonPrism[Combo].name.avro.getOption(comboJson).map(_.toSeq) ===
        Some(AvroBinaryCursor.writeDatum("x", nameSchema).toSeq)
    }

    "miss when the subtree does not parse under the focus schema" in {
      val badSize = comboJson.mapObject(_.add("size", Json.fromString("oops")))
      val face = JsonPrism[Combo].field(_.size).avro
      (face.getOption(badSize) === None)
        .and(face.modify(identity)(badSize) === badSize)
    }
  }

object AvroJsonFacesSpec:

  // circe codecs for the JsonPrism drilling (root + each drilled focus type); circe-core has the
  // String/Long leaf instances already
  given Encoder[Combo] =
    Encoder.forProduct3("name", "size", "active")(c => (c.name, c.size, c.active))

  given Decoder[Combo] = Decoder.forProduct3("name", "size", "active")(Combo.apply)

end AvroJsonFacesSpec

/** Array fixture for the `.at` / `.each` faces (Combo is shared with `AvroJsonSpec`). */
case class Bag(tag: String, items: List[Int])
