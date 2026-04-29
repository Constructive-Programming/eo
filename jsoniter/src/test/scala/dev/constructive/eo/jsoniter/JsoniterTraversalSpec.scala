package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.specs2.mutable.Specification

import dev.constructive.eo.data.MultiFocus
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

/** Phase-1.5 spike for `JsoniterTraversal`. Exercises:
  *
  *   - parser accepts `[*]` wildcard, preserves position in nested paths
  *   - scanner.findAll fans out across array elements; empty/missing arrays produce empty list
  *   - JsoniterTraversal[A].foldMap sums over hit elements, returns Monoid.empty on miss
  *   - JsoniterPrism rejects wildcard paths at construction (cross-check)
  *
  * Phase 2 (write-back splice — `.modify` re-encoding each element back into the buffer) is out
  * of scope here.
  */
class JsoniterTraversalSpec extends Specification:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  private val sample = bytes("""{"items":[1,2,3,4,5],"empty":[],"name":"alice"}""")

  "PathParser: accepts [*] wildcard" >> {
    val ok = PathParser.parse("$.items[*]") === Right(
      List(PathStep.Field("items"), PathStep.Wildcard)
    )
    val nestedOk = PathParser.parse("$.outer[*].inner") === Right(
      List(PathStep.Field("outer"), PathStep.Wildcard, PathStep.Field("inner"))
    )
    val rejectMalformedOk = PathParser.parse("$.items[*").isLeft
    ok.and(nestedOk).and(rejectMalformedOk === true)
  }

  "JsonPathScanner.findAll: fans out across array, empty array → Nil, missing → Nil" >> {
    val itemsSteps = PathParser.parse("$.items[*]").toOption.get
    val itemSpans = JsonPathScanner.findAll(sample, itemsSteps)
    val countOk = itemSpans.length === 5
    val firstOk = new String(sample.slice(itemSpans.head.start, itemSpans.head.end), "UTF-8") === "1"
    val lastOk = new String(sample.slice(itemSpans.last.start, itemSpans.last.end), "UTF-8") === "5"

    val emptyOk = JsonPathScanner.findAll(sample, PathParser.parse("$.empty[*]").toOption.get) === Nil
    val missingOk = JsonPathScanner.findAll(sample, PathParser.parse("$.absent[*]").toOption.get) === Nil

    countOk.and(firstOk).and(lastOk).and(emptyOk).and(missingOk)
  }

  "JsoniterTraversal: foldMap over array hits / empty / missing" >> {
    import cats.instances.long.given

    val itemsT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[List]] =
      JsoniterTraversal[Long]("$.items[*]")
    val emptyT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[List]] =
      JsoniterTraversal[Long]("$.empty[*]")
    val missingT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[List]] =
      JsoniterTraversal[Long]("$.absent[*]")

    (itemsT.foldMap(identity[Long])(sample) === 15L) // 1+2+3+4+5
      .and(emptyT.foldMap(identity[Long])(sample) === 0L)
      .and(missingT.foldMap(identity[Long])(sample) === 0L)
  }

  "JsoniterTraversal: from is identity in phase-1.5 (returns the original bytes)" >> {
    val itemsT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[List]] =
      JsoniterTraversal[Long]("$.items[*]")
    itemsT.from(itemsT.to(sample)) === sample
  }

  "JsoniterTraversal: silently skips spans that fail to decode" >> {
    import cats.instances.long.given
    // Mixed array — a string element among integers will fail Long decode and get dropped.
    val mixed = bytes("""{"vals":[1,"oops",3,4]}""")
    val valsT: Optic[Array[Byte], Array[Byte], Long, Long, MultiFocus[List]] =
      JsoniterTraversal[Long]("$.vals[*]")
    valsT.foldMap(identity[Long])(mixed) === 8L // 1 + 3 + 4 (the "oops" String drops)
  }

  "JsoniterPrism: rejects wildcard paths at construction (cross-check)" >> {
    JsoniterPrism[Long]("$.items[*]") must throwAn[IllegalArgumentException]
  }
