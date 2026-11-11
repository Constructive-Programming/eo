package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

/** Read-only spike for `JsoniterPrism` (munit: runs on every platform). Exercises:
  *
  *   - path scanner over object/array/scalar/nested cases
  *   - decode-failure surfacing as `Affine.Miss`
  *   - `.foldMap` over the Affine carrier (read-only escape: hit decodes via the codec, miss
  *     returns the Monoid identity), confirming the standard cats-eo extension methods light up
  *     over `Optic[..., Affine]` without any new typeclass shipping
  *   - parser error paths
  *
  * Phase 2 (read-write splice) is out of scope here; those tests live in a future
  * `JsoniterPrismWriteSpec` once splice mechanics are implemented.
  */
import JsoniterPathFixtures.prism

class JsoniterPrismSuite extends munit.FunSuite:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  private val sample = bytes(
    """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,2,3]}}"""
  )

  // ----- path parser ----------------------------------------------------

  test("PathParser: empty / leading-$ / between-step / $-only / nested") {
    assert(PathParser.parse("").isLeft)
    assert(PathParser.parse(".foo").isLeft)
    assert(PathParser.parse("$.foo bar").isLeft)
    assertEquals(PathParser.parse("$"), Right(Nil))
    assertEquals(
      PathParser.parse("$.payload.items[2].id"),
      Right(
        List(
          PathStep.Field("payload"),
          PathStep.Field("items"),
          PathStep.Index(2),
          PathStep.Field("id"),
        )
      ),
    )
  }

  // ----- path scanner ---------------------------------------------------

  test("JsonPathScanner: object dive / missing key / array index / OOB / empty path / whitespace") {
    val deepSpan = JsonPathScanner.find(sample, PathParser.parse("$.payload.user.id").toOption.get)
    assert(deepSpan.isHit)
    assertEquals(new String(sample.slice(deepSpan.start, deepSpan.end), "UTF-8"), "42")

    assert(
      !JsonPathScanner.find(sample, PathParser.parse("$.payload.user.name").toOption.get).isHit
    )

    val arrSpan = JsonPathScanner.find(sample, PathParser.parse("$.payload.items[1]").toOption.get)
    assertEquals(new String(sample.slice(arrSpan.start, arrSpan.end), "UTF-8"), "2")

    assert(!JsonPathScanner.find(sample, PathParser.parse("$.payload.items[7]").toOption.get).isHit)

    val wholeSpan = JsonPathScanner.find(sample, Nil)
    assertEquals(wholeSpan.start, 0)
    assertEquals(wholeSpan.end, sample.length)

    val padded = bytes("""  {  "a"  :  {  "b"  :  7  }  }  """)
    val pSpan = JsonPathScanner.find(padded, PathParser.parse("$.a.b").toOption.get)
    assertEquals(new String(padded.slice(pSpan.start, pSpan.end), "UTF-8"), "7")
  }

  // ----- JsoniterPrism --------------------------------------------------

  test("JsoniterPrism on Affine: hit decodes / miss passes through / decode-failure → Miss") {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.id")
    idP.to(sample) match
      case h: Affine.Hit[idP.X, Long] => assertEquals(h.b, 42L)
      case _: Affine.Miss[idP.X]      => fail("expected a Hit")

    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.missing")
    absentP.to(sample) match
      case m: Affine.Miss[absentP.X] =>
        assertEquals(m.fst.asInstanceOf[Array[Byte]].toSeq, sample.toSeq)
      case _: Affine.Hit[absentP.X, Long] => fail("expected a Miss")

    // Field 'email' is a String; decoding it as Long throws → Miss.
    val mistypedP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.email")
    assert(mistypedP.to(sample).isInstanceOf[Affine.Miss[?]])
  }

  test("JsoniterPrism: from is identity in phase-1 (Hit and Miss both pass bytes through)") {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.id")
    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.missing")
    assertEquals(idP.from(idP.to(sample)).toSeq, sample.toSeq)
    assertEquals(absentP.from(absentP.to(sample)).toSeq, sample.toSeq)
  }

  // covers: JsoniterPrism.scala fromPath — the ONLY string-path constructor returns the failure in
  //   the type (there is no throwing twin left to reach for), and the wildcard refusal keeps its
  //   JsoniterTraversal pointer. A path read from config is the motivating caller.
  test("JsoniterPrism: fromPath reports a bad path as Left, never as a throw") {
    val malformedMsg = "invalid JSONPath 'not-a-path': path must start with '$' (got 'n')"
    val wildcardMsg =
      "JsoniterPrism path '$.payload.items[*].id' contains '[*]' — use JsoniterTraversal for wildcard paths"
    assertEquals(JsoniterPrism.fromPath[Long]("not-a-path").left.toOption, Some(malformedMsg))
    assertEquals(
      JsoniterPrism.fromPath[Long]("$.payload.items[*].id").left.toOption,
      Some(wildcardMsg),
    )
    val good = JsoniterPrism.fromPath[Long]("$.payload.user.id")
    assert(good.isRight)
    assertEquals(good.toOption.get.foldMap(identity[Long])(sample), 42L)
  }

  test("JsoniterPrism: .foldMap over the Affine carrier — hit reads, miss returns Monoid.empty") {
    import cats.instances.string.given

    val emailP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      prism[String]("$.payload.user.email")
    val missingP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      prism[String]("$.does.not.exist")

    assertEquals(emailP.foldMap(identity[String])(sample), "alice@example.com")
    assertEquals(missingP.foldMap(identity[String])(sample), "")
  }
