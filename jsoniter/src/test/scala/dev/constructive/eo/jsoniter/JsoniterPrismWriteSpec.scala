package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.specs2.mutable.Specification

import dev.constructive.eo.data.Affine
import dev.constructive.eo.data.Affine.given
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

/** Phase-2 write spec for `JsoniterPrism`. Exercises:
  *
  *   - `.replace` over a scalar focus (Long, String); same-length and longer/shorter encodings
  *   - `.modify` to transform the focused value
  *   - Miss path: write-back is no-op (returns original bytes)
  *   - Round-trip identity: read after write yields the new value
  *   - `.modify(identity)` produces byte-equivalent output (canonical encodings)
  *
  * `.modify` / `.replace` light up via `ForgetfulFunctor[Affine]` (in `Affine.given`); no new
  * typeclass shipping needed — the splice lives in the optic's `from` closure.
  */
class JsoniterPrismWriteSpec extends Specification:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")
  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  private val sample = bytes(
    """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,2,3]}}"""
  )

  "JsoniterPrism .replace: same-length scalar — splices, surrounding bytes preserved" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val out = idP.replace(99L)(sample)
    str(out) === """{"payload":{"user":{"id":99,"email":"alice@example.com"},"items":[1,2,3]}}"""
  }

  "JsoniterPrism .replace: longer scalar — array grows, surrounding bytes preserved" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val out = idP.replace(1234567L)(sample)
    str(out) === """{"payload":{"user":{"id":1234567,"email":"alice@example.com"},"items":[1,2,3]}}"""
  }

  "JsoniterPrism .replace: shorter scalar — array shrinks, surrounding bytes preserved" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val out = idP.replace(7L)(sample)
    str(out) === """{"payload":{"user":{"id":7,"email":"alice@example.com"},"items":[1,2,3]}}"""
  }

  "JsoniterPrism .replace on String — quotes preserved" >> {
    val emailP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      JsoniterPrism[String]("$.payload.user.email")
    val out = emailP.replace("bob@x.org")(sample)
    str(out) === """{"payload":{"user":{"id":42,"email":"bob@x.org"},"items":[1,2,3]}}"""
  }

  "JsoniterPrism .modify: transforms focus via codec round-trip" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val out = idP.modify(_ * 10)(sample)
    str(out) === """{"payload":{"user":{"id":420,"email":"alice@example.com"},"items":[1,2,3]}}"""
  }

  "JsoniterPrism .replace: miss path — write-back is no-op" >> {
    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.absent")
    val out = absentP.replace(99L)(sample)
    out.toSeq === sample.toSeq
  }

  "JsoniterPrism: round-trip — read after write yields the new value" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val written = idP.replace(99L)(sample)
    idP.to(written) match
      case h: Affine.Hit[idP.X, Long]  => h.b === 99L
      case _: Affine.Miss[idP.X, Long] => (false === true)
  }

  "JsoniterPrism .modify(identity): byte-equivalent output for canonical encodings" >> {
    val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.user.id")
    val emailP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
      JsoniterPrism[String]("$.payload.user.email")

    (idP.modify(identity[Long])(sample).toSeq === sample.toSeq)
      .and(emailP.modify(identity[String])(sample).toSeq === sample.toSeq)
  }

  "JsoniterPrism .replace at array index — splices the indexed element" >> {
    val secondP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      JsoniterPrism[Long]("$.payload.items[1]")
    val out = secondP.replace(20L)(sample)
    str(out) === """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,20,3]}}"""
  }
