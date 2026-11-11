package dev.constructive.eo.jsoniter

import java.nio.ByteBuffer

/** The `JsonSlice` paths that used JDK-9+ APIs before they were made portable (range equality,
  * bounds check, absolute buffer copy), pinned on every platform — the Scala.js and Scala Native
  * javalibs carry none of those APIs.
  */
class JsonSlicePortabilitySuite extends munit.FunSuite:

  private def bytes(s: String): Array[Byte] = s.getBytes("UTF-8")

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
