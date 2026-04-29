package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.specs2.mutable.Specification

import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

/** Read-only spike for `JsoniterPrism`. Exercises:
  *
  *   - path scanner over object/array/scalar/nested cases
  *   - decode-failure surfacing as `Affine.Miss`
  *   - `.foldMap` over the Affine carrier (read-only escape: hit decodes via the codec,
  *     miss returns the Monoid identity), confirming the standard cats-eo extension methods
  *     light up over `Optic[..., Affine]` without any new typeclass shipping
  *   - parser error paths
  *
  * Phase 2 (read-write splice) is out of scope here; those tests live in a future
  * `JsoniterPrismWriteSpec` once splice mechanics are implemented.
  */
class JsoniterPrismSpec extends Specification:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

  private val sample = bytes(
    """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,2,3]}}"""
  )

  // ----- path parser ----------------------------------------------------

  "PathParser: empty / leading-$ / between-step / $-only / nested" >> {
    val emptyOk = PathParser.parse("").isLeft
    val leadingOk = PathParser.parse(".foo").isLeft
    val mixedOk = PathParser.parse("$.foo bar").isLeft
    val justDollarOk = PathParser.parse("$") === Right(Nil)
    val nestedOk = PathParser.parse("$.payload.items[2].id") === Right(
      List(
        PathStep.Field("payload"),
        PathStep.Field("items"),
        PathStep.Index(2),
        PathStep.Field("id"),
      )
    )
    (emptyOk === true)
      .and(leadingOk === true)
      .and(mixedOk === true)
      .and(justDollarOk)
      .and(nestedOk)
  }

  // ----- path scanner ---------------------------------------------------

  "JsonPathScanner: object dive / missing key / array index / OOB / empty path / whitespace" >> {
    val deepSteps = PathParser.parse("$.payload.user.id").toOption.get
    val deepSpan = JsonPathScanner.find(sample, deepSteps)
    val deepOk = (deepSpan.isHit === true)
      .and(new String(sample.slice(deepSpan.start, deepSpan.end), "UTF-8") === "42")

    val missSteps = PathParser.parse("$.payload.user.name").toOption.get
    val missOk = JsonPathScanner.find(sample, missSteps).isHit === false

    val arrSteps = PathParser.parse("$.payload.items[1]").toOption.get
    val arrSpan = JsonPathScanner.find(sample, arrSteps)
    val arrOk = new String(sample.slice(arrSpan.start, arrSpan.end), "UTF-8") === "2"

    val oobSteps = PathParser.parse("$.payload.items[7]").toOption.get
    val oobOk = JsonPathScanner.find(sample, oobSteps).isHit === false

    val wholeSpan = JsonPathScanner.find(sample, Nil)
    val wholeOk = (wholeSpan.start === 0).and(wholeSpan.end === sample.length)

    val padded = bytes("""  {  "a"  :  {  "b"  :  7  }  }  """)
    val pSteps = PathParser.parse("$.a.b").toOption.get
    val pSpan = JsonPathScanner.find(padded, pSteps)
    val paddedOk = new String(padded.slice(pSpan.start, pSpan.end), "UTF-8") === "7"

    deepOk.and(missOk).and(arrOk).and(oobOk).and(wholeOk).and(paddedOk)
  }

  // ----- JsoniterPrism --------------------------------------------------

  "JsoniterPrism on Affine: hit decodes / miss passes through / decode-failure → Miss" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")

    val hitOk = idP.to(sample) match
      case h: Affine.Hit[idP.X, Long]  => h.b === 42L
      case _: Affine.Miss[idP.X, Long] => (false === true) // expected Hit

    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.missing")
    val missOk = absentP.to(sample) match
      case m: Affine.Miss[absentP.X, Long] => m.fst === sample
      case _: Affine.Hit[absentP.X, Long]  => (false === true)

    // Field 'email' is a String; decoding it as Long throws → Miss.
    val mistypedP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.email")
    val mistypedOk = mistypedP.to(sample) match
      case _: Affine.Miss[mistypedP.X, Long] => true === true
      case _: Affine.Hit[mistypedP.X, Long]  => (false === true)

    hitOk.and(missOk).and(mistypedOk)
  }

  "JsoniterPrism: from is identity in phase-1 (Hit and Miss both pass bytes through)" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.missing")

    (idP.from(idP.to(sample)) === sample)
      .and(absentP.from(absentP.to(sample)) === sample)
  }

  "JsoniterPrism: rejects malformed path at construction" >> {
    JsoniterPrism[Long]("not-a-path") must throwAn[IllegalArgumentException]
  }

  "JsoniterPrism: .foldMap over the Affine carrier — hit reads, miss returns Monoid.empty" >> {
    import cats.instances.string.given

    val emailP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      JsoniterPrism[String]("$.payload.user.email")
    val missingP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      JsoniterPrism[String]("$.does.not.exist")

    (emailP.foldMap(identity[String])(sample) === "alice@example.com")
      .and(missingP.foldMap(identity[String])(sample) === "")
  }
