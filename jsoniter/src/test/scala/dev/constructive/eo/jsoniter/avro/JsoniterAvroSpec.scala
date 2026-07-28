package dev.constructive.eo.jsoniter.avro

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.avro.jsoniter.AvroJsoniter
import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.Optic.*
import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}
import org.specs2.mutable.Specification

/** Behaviour of the `.avro` face — JSON bytes drilled with the `JsoniterPrism` cursor, whole
  * document out as Avro binary. The reverse of `AvroJsoniterSpec`'s `.json` face block; writes are
  * verified by reading the produced Avro bytes back through `codecPrism` /
  * `AvroJsoniter.bytesPrism`.
  */
class JsoniterAvroSpec extends Specification:

  import JsoniterAvroSpec.given

  val person = Person("x", 9)
  val bag = Bag("t", List(1, 2, 3))

  ".avro face" should {
    "read the typed focus off JSON bytes and modify the whole document out as Avro binary" in {
      val jsonBytes = writeToArray(person)
      val face = JsoniterPrism[Person].field(_.name).avro[Person]
      (face.getOption(jsonBytes) === Some("x"))
        .and(
          codecPrism[Person].field(_.name).getOption(face.modify(_.toUpperCase)(jsonBytes)) ===
            Some("X")
        )
    }

    "drill via Dynamic selection" in {
      val jsonBytes = writeToArray(person)
      codecPrism[Person]
        .age
        .getOption(JsoniterPrism[Person].age.avro[Person].replace(10)(jsonBytes)) === Some(10)
    }

    ".each folds and rewrites every element" in {
      val jsonBytes = writeToArray(bag)
      val face = JsoniterPrism[Bag].field(_.items).each.avro[Bag]
      (face.exists(_ == 3)(jsonBytes) === true)
        .and(
          AvroJsoniter.bytesPrism[Bag].getOption(face.modify(_ + 1)(jsonBytes)) ===
            Some(Bag("t", List(2, 3, 4)))
        )
    }

    "a path miss converts the document unchanged" in {
      val jsonBytes = writeToArray(bag)
      AvroJsoniter
        .bytesPrism[Bag]
        .getOption(
          JsoniterPrism[Bag].field(_.items).at(9).avro[Bag].replace(0)(jsonBytes)
        ) === Some(bag)
    }

    "jsonToAvro ∘ bytesToJson round-trips a document" in {
      val avroCodec = summon[AvroCodec[Person]]
      val jsonBytes = writeToArray(person)
      val avroBytes = JsoniterAvro.jsonToAvro[Person].get(jsonBytes)
      AvroJsoniter.bytesToJson(avroCodec.schema).get(avroBytes).toSeq === jsonBytes.toSeq
    }
  }

/** Codec fixtures at top level (kindlings derivation macro-facing convention). */
case class Person(name: String, age: Int)
case class Bag(tag: String, items: List[Int])

object JsoniterAvroSpec:
  given JsonValueCodec[Person] = JsonCodecMaker.make
  given JsonValueCodec[Bag] = JsonCodecMaker.make
  // leaf codecs for the drilled hops (.field/.each decode only the focused slice)
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[Int] = JsonCodecMaker.make
  given JsonValueCodec[List[Int]] = JsonCodecMaker.make
