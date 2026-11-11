// CPD-OFF: deliberate twin of a specs2 spec the platform cannot run (specs2 has no Scala Native
// artifact / the original reads JVM-only stack traces) — duplication is the point.
package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*
import java.nio.ByteBuffer

/** Scala Native smoke twin of the jsoniter specs (specs2 has no Native artifact): the byte-cursor
  * read / write splice, and the `JsonSlice` paths that used JDK-9+ APIs before they were made
  * portable (range equality, bounds check, absolute buffer copy).
  */
class NativeJsoniterSuite extends munit.FunSuite:

  given longCodec: JsonValueCodec[Long] = JsonCodecMaker.make

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")
  private val sample = bytes("""{"payload":{"user":{"id":42,"email":"a@b.c"},"items":[1,2,3]}}""")

  private val idP: Optic[Array[Byte], Array[Byte], Long, Long, Affine] =
    JsoniterPrism.fromPath[Long]("$.payload.user.id").fold(msg => fail(msg), identity)

  test("prism reads a hit, passes a miss through, and treats a mistyped focus as a miss") {
    idP.to(sample) match
      case h: Affine.Hit[idP.X, Long] => assertEquals(h.b, 42L)
      case _: Affine.Miss[idP.X]      => fail("expected a hit")
    val absent = JsoniterPrism.fromPath[Long]("$.payload.user.missing").fold(m => fail(m), identity)
    assert(absent.to(sample).isInstanceOf[Affine.Miss[?]])
    val mistyped = JsoniterPrism.fromPath[Long]("$.payload.user.email").fold(m => fail(m), identity)
    assert(mistyped.to(sample).isInstanceOf[Affine.Miss[?]])
  }

  test("prism write splices the new value into the bytes and nothing else") {
    val out = idP.replace(7L)(sample)
    assertEquals(
      new String(out, "UTF-8"),
      """{"payload":{"user":{"id":7,"email":"a@b.c"},"items":[1,2,3]}}"""
    )
    assertEquals(new String(idP.modify(_ + 1)(sample), "UTF-8").contains("\"id\":43"), true)
  }

  test("JsonSlice: equality and hashCode are by content, not by array position") {
    val a = JsonSlice(bytes("xx{\"a\":1}yy"), 2, 7)
    val b = JsonSlice(bytes("{\"a\":1}"))
    assertEquals(a, b)
    assertEquals(a.hashCode, b.hashCode)
    assertNotEquals(a, JsonSlice(bytes("{\"a\":2}")))
    assertNotEquals(a, JsonSlice(bytes("{\"a\":1}x")))
  }

  test("JsonSlice: an out-of-range slice fails fast with IndexOutOfBoundsException") {
    intercept[IndexOutOfBoundsException](JsonSlice(new Array[Byte](4), 3, 2))
    intercept[IndexOutOfBoundsException](JsonSlice(new Array[Byte](4), -1, 2))
    intercept[IndexOutOfBoundsException](JsonSlice(new Array[Byte](4), 0, -1))
    assertEquals(JsonSlice(new Array[Byte](4), 4, 0).length, 0)
  }

  test("JsonSlice from a heap and a read-only ByteBuffer: views / copies, position untouched") {
    val doc = bytes("""{"id":42}""")
    val heap = ByteBuffer.wrap(bytes("..") ++ doc, 2, doc.length)
    assertEquals(JsonSlice(heap), JsonSlice(doc))
    val ro = ByteBuffer.wrap(doc).asReadOnlyBuffer()
    ro.position(0)
    val s = JsonSlice(ro)
    assertEquals(s, JsonSlice(doc))
    assertEquals(ro.position(), 0)
  }
// CPD-ON
