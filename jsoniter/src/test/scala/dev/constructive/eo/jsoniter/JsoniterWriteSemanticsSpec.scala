package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.optics.Traversal
import org.specs2.mutable.Specification

/** Write-semantics pins for the JSON byte optics — born from the 2026-07-04 expert review.
  * These pin the DOCUMENTED contract so drift is caught:
  *
  *   - the Optional/Traversal laws hold up to CANONICAL RE-ENCODING of the focused slice: a
  *     non-canonical focus (`1e0`, `A` escapes, exotic number forms) is normalised by
  *     `modify(identity)` — the law is `modify(identity) == identity` on the canonical quotient,
  *     not on raw bytes. Bytes OUTSIDE the focused span (whitespace, sibling formatting) are
  *     never touched.
  *   - the Affine write decodes the current focus before splicing — `.replace` onto a span whose
  *     current value doesn't decode as `A` is a Miss pass-through, so templates must carry VALID
  *     placeholder encodings.
  *   - same-carrier composition (`JsoniterTraversal[List[A]].andThen(Traversal.each)`) is
  *     write-capable end to end — the review flagged it as advertised-but-untested.
  */
class JsoniterWriteSemanticsSpec extends Specification:

  given JsonValueCodec[Double] = JsonCodecMaker.make
  given JsonValueCodec[Long] = JsonCodecMaker.make
  given rowCodec: JsonValueCodec[List[Long]] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")
  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  // covers: modify(identity) NORMALISES a non-canonical focused slice — the canonical-quotient
  //   law caveat, pinned so it stays documented behaviour rather than silent surprise. Bytes
  //   outside the span (the sibling and its spacing) survive verbatim.
  "modify(identity) canonicalises the focused slice; bytes outside the span are untouched" >> {
    val nonCanonical = bytes("""{"id":1e0,  "name":"x"}""")
    val idP = JsoniterPrism[Double]("$.id")
    val out = idP.modify(identity[Double])(nonCanonical)
    (str(out) === """{"id":1.0,  "name":"x"}""")
      .and(java.util.Arrays.equals(out, nonCanonical) === false)
  }

  // covers: replace requires a DECODABLE current focus — an invalid placeholder is a Miss and
  //   the write passes through by reference (the documented template precondition)
  "replace onto an undecodable current focus: Miss pass-through by reference" >> {
    val template = bytes("""{"id":{},"name":"x"}""")
    val idP = JsoniterPrism[Double]("$.id")
    ((idP.replace(9.5)(template) eq template) === true)
      .and(str(idP.replace(9.5)(bytes("""{"id":0.0,"name":"x"}"""))) ===
        """{"id":9.5,"name":"x"}""")
  }

  // covers: same-carrier andThen — byte traversal composed with the core each-traversal reads
  //   AND writes through both layers (the mfAssocPSVec composition the JsoniterTraversal doc
  //   advertises; previously untested for writes)
  "JsoniterTraversal[List[A]].andThen(Traversal.each): composed read and write" >> {
    import cats.instances.long.given
    val doc = bytes("""{"rows":[[1,2],[3]],"tag":"t"}""")
    val rowsT = JsoniterTraversal[List[Long]]("$.rows[*]")
    val comp = rowsT.andThen(Traversal.each[List, Long])

    val readOk = comp.foldMap(identity[Long])(doc) === 6L
    val writeOk = str(comp.modify(_ * 10L)(doc)) === """{"rows":[[10,20],[30]],"tag":"t"}"""
    readOk.and(writeOk)
  }

end JsoniterWriteSemanticsSpec
