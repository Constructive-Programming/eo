package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.optics.Optic.*
import org.specs2.mutable.Specification

/** Phase-2 write spec for `JsoniterTraversal`. Exercises:
  *
  *   - `.modify` over a wildcard path splices EVERY element in one pass, siblings preserved
  *   - length-changing encodings (longer and shorter) shift the following spans correctly
  *   - `.modify(identity)` is byte-equivalent for canonical encodings
  *   - empty / missing arrays pass the input through by reference
  *   - elements whose decode refuses are dropped from the focus set and stay byte-untouched, while
  *     the decodable elements around them still splice (span↔foci alignment)
  *   - nested wildcard paths (`\$.outer[*].inner`) write each element's sub-field
  *
  * `.modify` lights up via `mfFunctor[PSVec]` — the splice lives in the optic's `from`.
  */
class JsoniterTraversalWriteSpec extends Specification:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")
  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  private val sample = bytes("""{"items":[1,2,3,4,5],"empty":[],"name":"alice"}""")

  "JsoniterTraversal .modify: every wildcard element spliced, siblings preserved" >> {
    val itemsT = JsoniterTraversal[Long]("$.items[*]")
    str(itemsT.modify(_ * 10)(sample)) ===
      """{"items":[10,20,30,40,50],"empty":[],"name":"alice"}"""
  }

  "JsoniterTraversal .modify: longer and shorter encodings shift subsequent spans" >> {
    val itemsT = JsoniterTraversal[Long]("$.items[*]")
    val longer = str(itemsT.modify(_ + 1000)(sample)) ===
      """{"items":[1001,1002,1003,1004,1005],"empty":[],"name":"alice"}"""
    val wide = bytes("""{"items":[100,200,300],"name":"bob"}""")
    val shorter = str(itemsT.modify(_ / 100)(wide)) === """{"items":[1,2,3],"name":"bob"}"""
    longer.and(shorter)
  }

  "JsoniterTraversal .modify(identity): byte-equivalent for canonical encodings" >> {
    val itemsT = JsoniterTraversal[Long]("$.items[*]")
    itemsT.modify(identity[Long])(sample).toSeq === sample.toSeq
  }

  "JsoniterTraversal .modify: empty / missing arrays pass through by reference" >> {
    val emptyT = JsoniterTraversal[Long]("$.empty[*]")
    val missingT = JsoniterTraversal[Long]("$.absent[*]")
    ((emptyT.modify(_ * 10)(sample) eq sample) === true)
      .and((missingT.modify(_ * 10)(sample) eq sample) === true)
  }

  "JsoniterTraversal .modify: undecodable elements stay untouched, the rest splice" >> {
    val mixed = bytes("""{"items":[1,"x",3],"name":"carol"}""")
    val itemsT = JsoniterTraversal[Long]("$.items[*]")
    str(itemsT.modify(_ * 2)(mixed)) === """{"items":[2,"x",6],"name":"carol"}"""
  }

  "JsoniterTraversal .modify: nested wildcard path writes each element's sub-field" >> {
    val nested = bytes("""{"outer":[{"inner":"a","keep":1},{"inner":"b","keep":2}]}""")
    val innerT = JsoniterTraversal[String]("$.outer[*].inner")
    str(innerT.modify(_.toUpperCase)(nested)) ===
      """{"outer":[{"inner":"A","keep":1},{"inner":"B","keep":2}]}"""
  }

  "JsoniterTraversal round-trip: read after write yields the new values" >> {
    import cats.instances.list.given
    val itemsT = JsoniterTraversal[Long]("$.items[*]")
    val written = itemsT.modify(_ * 10)(sample)
    itemsT.foldMap(List(_))(written) === List(10L, 20L, 30L, 40L, 50L)
  }

end JsoniterTraversalWriteSpec
