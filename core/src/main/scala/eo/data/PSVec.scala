package eo
package data

/** Lightweight array-backed focus vector for [[PowerSeries]]. Stores an `Array[AnyRef]` plus an
  * `(offset, length)` view, so [[slice]] is a pointer update — no element copy, no new array.
  *
  * This replaces the former `ArraySeq[B]` focus storage purely so that
  * `PowerSeries.assoc.composeFrom` can hand each inner reassembly a zero-copy view of the
  * underlying flat focus array. ArraySeq's `slice` would otherwise allocate a fresh backing
  * array per inner call — `O(N)` extra allocation in a 3-hop chain over N elements, which is
  * the bulk of what [[PowerSeries]] pays above the naive `copy`/`map` baseline.
  *
  * Elements are stored type-erased as `AnyRef` and narrowed on read (the same pattern
  * [[ObjArrBuilder]] uses); callers must uphold the `B <: AnyRef`-shaped contract the optic
  * machinery already maintains.
  *
  * Equality is value-based: two vectors of the same length whose elements pairwise `==` compare
  * equal, regardless of underlying offsets. Needed so the `PowerSeries` case-class `equals`
  * discipline (law-suite sanity checks) keeps working after the carrier reshape.
  *
  * @param arr
  *   backing storage — shared across all slice derivatives of the same vector
  * @param offset
  *   first element read is at `arr(offset)`
  * @param length
  *   number of elements visible through this view
  */
final class PSVec[B] private[data] (
    private[data] val arr: Array[AnyRef],
    val offset: Int,
    val length: Int,
):

  /** Read the first element. Unchecked: UB if [[length]] == 0. */
  inline def head: B = arr(offset).asInstanceOf[B]

  /** Indexed access. Unchecked: UB if `i < 0 || i >= length`. */
  inline def apply(i: Int): B = arr(offset + i).asInstanceOf[B]

  def isEmpty: Boolean = length == 0

  /** Cheap slice: returns a view over the same backing array with an updated `(offset, length)`.
    * Out-of-range bounds are clamped to `[0, length]`, matching `ArraySeq.slice`'s behaviour.
    */
  def slice(from: Int, until: Int): PSVec[B] =
    val lo = math.max(0, from)
    val hi = math.min(length, math.max(lo, until))
    new PSVec(arr, offset + lo, hi - lo)

  override def equals(that: Any): Boolean = that match
    case other: PSVec[?] =>
      if length != other.length then false
      else
        var i  = 0
        var eq = true
        while eq && i < length do
          eq = arr(offset + i) == other.arr(other.offset + i)
          i += 1
        eq
    case _ => false

  override def hashCode(): Int =
    var h = 1
    var i = 0
    while i < length do
      val e = arr(offset + i)
      h = h * 31 + (if e == null then 0 else e.hashCode)
      i += 1
    h

  override def toString(): String =
    val sb = new StringBuilder("PSVec(")
    var i = 0
    while i < length do
      if i > 0 then sb.append(", ")
      sb.append(arr(offset + i))
      i += 1
    sb.append(")").toString

object PSVec:

  private val emptyArr: Array[AnyRef] = new Array[AnyRef](0)

  /** Wrap an `Array[AnyRef]` verbatim as a PSVec. Shares ownership with the caller. */
  def unsafeWrap[B](arr: Array[AnyRef]): PSVec[B] = new PSVec(arr, 0, arr.length)

  /** Zero-length shared view. */
  def empty[B]: PSVec[B] = new PSVec[B](emptyArr, 0, 0)

  /** Single-element view, freshly allocated. Useful for the `Lens`/`Prism`/`Optional` → PowerSeries
    * composers, which each focus exactly one element.
    */
  def singleton[B](b: B): PSVec[B] =
    val arr = new Array[AnyRef](1)
    arr(0)  = b.asInstanceOf[AnyRef]
    new PSVec(arr, 0, 1)
