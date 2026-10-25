package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

import java.nio.charset.StandardCharsets

/** A JSON document held as a '''slice''' of a byte array: `length` bytes of `array` from `offset`
  * on. The source of the slice faces [[JsoniterPrism.onSlice]] and [[JsoniterTraversal.onSlice]],
  * which read a JSON value in place, without first copying it into an array of its own — a message
  * cut out of a network buffer, say.
  *
  * A slice is a '''view''': building one copies nothing, and it reads the array as it is when an
  * optic runs. Don't change the bytes while a slice of them is in use.
  *
  * Equality is by '''content''': two slices are equal when they hold the same bytes, wherever they
  * sit in whatever arrays. So a slice in the middle of a larger buffer equals the same bytes copied
  * out, and the optic laws can compare slices with `==`.
  *
  * @param array
  *   the backing array, shared, not copied
  * @param offset
  *   where the slice starts in `array`
  * @param length
  *   how many bytes the slice holds
  *
  * @group Optics
  */
final class JsonSlice private (val array: Array[Byte], val offset: Int, val length: Int):

  /** One past the slice's last byte in [[array]]. */
  def until: Int = offset + length

  /** A copy of the slice's bytes, in an array of their own. */
  def toArray: Array[Byte] = java.util.Arrays.copyOfRange(array, offset, until)

  override def equals(that: Any): Boolean = that match
    case s: JsonSlice =>
      java.util.Arrays.equals(array, offset, until, s.array, s.offset, s.until)
    case _ => false

  override def hashCode: Int =
    @tailrec def loop(i: Int, h: Int): Int =
      if i < until then loop(i + 1, 31 * h + array(i)) else h
    loop(offset, 1)

  /** The slice's bytes as UTF-8 text. */
  override def toString: String =
    s"JsonSlice(${new String(array, offset, length, StandardCharsets.UTF_8)})"

object JsonSlice:

  /** The `length` bytes of `bytes` from `offset` on. Nothing is copied.
    *
    * Fails fast with an `IndexOutOfBoundsException` when the range doesn't lie inside `bytes`
    * (`offset` or `length` negative, or `offset + length > bytes.length`) — the contract of
    * `java.util.Arrays.copyOfRange`, `ByteBuffer.wrap` and jsoniter-scala's own `readFromSubArray`.
    * Offsets are the caller's arithmetic over an array it holds, not data read off the wire, so a
    * wrong one is a bug at the call site; checking here means no optic ever runs on one.
    *
    * @group Constructors
    */
  def apply(bytes: Array[Byte], offset: Int, length: Int): JsonSlice =
    java.util.Objects.checkFromIndexSize(offset, length, bytes.length)
    new JsonSlice(bytes, offset, length)

  /** The whole of `bytes`, as a slice. Nothing is copied.
    *
    * @group Constructors
    */
  def apply(bytes: Array[Byte]): JsonSlice = new JsonSlice(bytes, 0, bytes.length)
