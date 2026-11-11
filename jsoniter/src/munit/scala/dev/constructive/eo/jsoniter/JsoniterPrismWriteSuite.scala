package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*

/** Phase-2 write spec for `JsoniterPrism` (munit: runs on every platform). Exercises:
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
import JsoniterPathFixtures.prism

class JsoniterPrismWriteSuite extends munit.FunSuite:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make
  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")
  private def str(b: Array[Byte]): String = new String(b, "UTF-8")

  private val sample = bytes(
    """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,2,3]}}"""
  )

  private val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    prism[Long]("$.payload.user.id")

  private val emailP: Optic[Array[Byte], Array[Byte], String, String, Affine] =
    prism[String]("$.payload.user.email")

  test(".replace: same-length scalar — splices, surrounding bytes preserved") {
    assertEquals(
      str(idP.replace(99L)(sample)),
      """{"payload":{"user":{"id":99,"email":"alice@example.com"},"items":[1,2,3]}}""",
    )
  }

  test(".replace: longer scalar — array grows, surrounding bytes preserved") {
    assertEquals(
      str(idP.replace(1234567L)(sample)),
      """{"payload":{"user":{"id":1234567,"email":"alice@example.com"},"items":[1,2,3]}}""",
    )
  }

  test(".replace: shorter scalar — array shrinks, surrounding bytes preserved") {
    assertEquals(
      str(idP.replace(7L)(sample)),
      """{"payload":{"user":{"id":7,"email":"alice@example.com"},"items":[1,2,3]}}""",
    )
  }

  test(".replace on String — quotes preserved") {
    assertEquals(
      str(emailP.replace("bob@x.org")(sample)),
      """{"payload":{"user":{"id":42,"email":"bob@x.org"},"items":[1,2,3]}}""",
    )
  }

  test(".modify: transforms focus via codec round-trip") {
    assertEquals(
      str(idP.modify(_ * 10)(sample)),
      """{"payload":{"user":{"id":420,"email":"alice@example.com"},"items":[1,2,3]}}""",
    )
  }

  test(".replace: miss path — write-back is no-op") {
    val absentP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.user.absent")
    assertEquals(absentP.replace(99L)(sample).toSeq, sample.toSeq)
  }

  test("round-trip — read after write yields the new value") {
    idP.to(idP.replace(99L)(sample)) match
      case h: Affine.Hit[idP.X, Long] => assertEquals(h.b, 99L)
      case _: Affine.Miss[idP.X]      => fail("expected a Hit")
  }

  test(".modify(identity): byte-equivalent output for canonical encodings") {
    assertEquals(idP.modify(identity[Long])(sample).toSeq, sample.toSeq)
    assertEquals(emailP.modify(identity[String])(sample).toSeq, sample.toSeq)
  }

  test(".replace at array index — splices the indexed element") {
    val secondP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
      prism[Long]("$.payload.items[1]")
    assertEquals(
      str(secondP.replace(20L)(sample)),
      """{"payload":{"user":{"id":42,"email":"alice@example.com"},"items":[1,20,3]}}""",
    )
  }
