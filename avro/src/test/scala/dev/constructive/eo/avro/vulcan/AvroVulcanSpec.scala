package dev.constructive.eo.avro.vulcan

import scala.language.implicitConversions

import _root_.vulcan.Codec as VCodec
import cats.syntax.all.*
import dev.constructive.eo.avro.circe.AvroJson
import dev.constructive.eo.avro.{codecPrism, AvroCodec}
import org.apache.avro.generic.IndexedRecord
import org.specs2.mutable.Specification

// Top-level so the vulcan record codec and the derived prisms see a plain classfile.
case class Combo(name: String, size: Long, active: Boolean)

given VCodec[Combo] =
  VCodec.record(name = "Combo", namespace = "dev.constructive.eo.avro.vulcan") { fb =>
    (fb("name", _.name), fb("size", _.size), fb("active", _.active)).mapN(Combo.apply)
  }

class AvroVulcanSpec extends Specification:

  private val original = Combo("ada", 42L, active = true)

  "AvroVulcan.codec" should {

    "round-trip encode → decodeEither through the bridged codec" in {
      val bridged = AvroVulcan.codec[Combo]
      bridged.decodeEither(bridged.encode(original)) must beRight(original)
    }

    "surface decode failures as Left, never throw" in {
      AvroVulcan.codec[Combo].decodeEither("not a record") must beLeft
    }
  }

  "the vulcanAvroCodec given" should {

    "power AvroCodec-keyed entry points from a vulcan.Codec alone" in {
      // `codecPrism` demands `AvroCodec[Combo]`; only the vulcan codec is defined above —
      // evidence arrives through the bridge given (downstream: `import eo.avro.vulcan.given`).
      val bytes = AvroCodec.encodeValue(original).toOption.get
      codecPrism[Combo].getOption(bytes) must beSome(original)
    }

    "collapse the issue-#73 decode-or-throw sites onto the AvroJson diagonals" in {
      val rec = summon[AvroCodec[Combo]].encode(original).asInstanceOf[IndexedRecord]
      AvroJson.recordPrism[Combo].getOption(rec) must beSome(original)
    }
  }
