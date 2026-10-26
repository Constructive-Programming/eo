package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/** A JSON document held as a '''slice''' of a byte array: `length` bytes of `array` from `offset`
  * on. The source (and, through `.raw`, the focus) of the in-place optics [[JsoniterInPlacePrism]]
  * and [[JsoniterInPlaceTraversal]] — `JsoniterPrism.inPlace[A]`, `prism.inPlace`,
  * `traversal.inPlace` — which read a JSON value where it lies, without first copying it into an
  * array of its own: a message cut out of a network buffer, say.
  *
  * A slice is a '''view''', not a copy:
  *
  *   - Building one copies nothing, and an optic reads the array as it is when it runs. '''Don't'''
  *     '''mutate the array''' while the slice, or anything read from it, is in use.
  *   - A slice keeps the '''whole''' array reachable, however few bytes it covers: a 300-byte slice
  *     of a 256 KiB read pins the 256 KiB. Call [[toArray]] for anything kept long-term.
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

/** `JsonSlice(bytes, offset, length)` as a method on the array: the `length` bytes of `bytes` from
  * `offset` on — `(offset, length)`, as [[JsonSlice.apply]] takes them, not `(from, until)`.
  * Nothing is copied, and the same fail-fast bounds check applies. There is deliberately no
  * implicit conversion from `Array[Byte]`: a slice shares its array, so taking one is spelled out.
  *
  * @group Constructors
  */
extension (bytes: Array[Byte])
  def jsonSlice(offset: Int, length: Int): JsonSlice = JsonSlice(bytes, offset, length)

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

  /** The bytes between `buf`'s position and its limit, as a slice. The buffer's position, limit and
    * mark are left as they were.
    *
    *   - A buffer with an accessible array (`buf.hasArray`: a heap buffer that isn't read-only) is
    *     '''not copied''': the slice is a view of `buf.array` from
    *     `buf.arrayOffset + buf.position`, `buf.remaining` bytes long, with the view's rules —
    *     don't change the bytes while it's in use, and it keeps the whole array reachable.
    *   - A '''direct''' or '''read-only''' buffer exposes no array, so its remaining bytes are
    *     '''copied once''', into an array of the slice's own.
    *
    * @group Constructors
    */
  def apply(buf: ByteBuffer): JsonSlice =
    val length = buf.remaining
    if buf.hasArray then new JsonSlice(buf.array, buf.arrayOffset + buf.position, length)
    else
      val out = new Array[Byte](length)
      buf.get(buf.position, out) // absolute bulk get: the position doesn't move
      new JsonSlice(out, 0, length)

  /** A slice of a range the caller has already checked: a span the scanner found inside a slice. */
  private[jsoniter] def unchecked(bytes: Array[Byte], offset: Int, length: Int): JsonSlice =
    new JsonSlice(bytes, offset, length)
