package eo
package data

/** Minimal grow-on-demand `Array[AnyRef]` builder, used by [[PowerSeries]] and
  * [[optics.Traversal.pEach]] to accumulate focus elements without paying `ArrayBuffer.toArray`'s
  * final copy. Doubles capacity on overflow; the `freeze*` methods each publish the accumulated
  * array in a different shape (raw `Array[AnyRef]`, or a [[PSVec]] view).
  *
  * No `ClassTag` required: `Array[AnyRef]` is legal storage for every generic `B` on the JVM
  * (generic parameters erase to `Object`), and the final cast inside [[freezeAsPSVec]] is always
  * safe for reference-typed `B`.
  */
final private[eo] class ObjArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[AnyRef] = new Array[AnyRef](math.max(initialCapacity, 1))
  private var len: Int = 0

  def size: Int = len

  def append(x: AnyRef): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  def appendAllFromPSVec[A](src: PSVec[A]): Unit =
    src match
      case PSVec.Empty        => ()
      case s: PSVec.Single[?] => append(s.b.asInstanceOf[AnyRef])
      case s: PSVec.Slice[?]  =>
        val n = s.length
        if len + n > arr.length then grow(len + n)
        System.arraycopy(s.arr, s.offset, arr, len, n)
        len += n

  private def grow(minCap: Int): Unit =
    var newCap = arr.length * 2
    while newCap < minCap do newCap *= 2
    val newArr = new Array[AnyRef](newCap)
    System.arraycopy(arr, 0, newArr, 0, len)
    arr = newArr

  /** Return the accumulated storage as a [[PSVec]]. The PowerSeries focus-storage shape — uses
    * `PSVec.unsafeWrap` so 0/1-element results are normalised into the `Empty` / `Single` variant
    * without a backing array.
    */
  def freezeAsPSVec[A]: PSVec[A] = PSVec.unsafeWrap[A](freezeArr)

  /** Return the raw `Array[AnyRef]` exactly-sized. No copy when the builder filled its internal
    * array exactly; one arraycopy to truncate when it didn't. Used by `PowerSeries.AssocSndZ`,
    * which stores per-element leftovers as parallel arrays directly.
    */
  def freezeArr: Array[AnyRef] =
    if len == arr.length then arr
    else
      val trimmed = new Array[AnyRef](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
