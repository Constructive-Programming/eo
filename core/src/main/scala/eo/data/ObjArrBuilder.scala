package eo
package data

import scala.collection.immutable.ArraySeq

/** Minimal grow-on-demand `Array[AnyRef]` builder, used by [[PowerSeries]] and
  * [[optics.Traversal.pEach]] to accumulate focus elements without paying
  * `ArrayBuffer.toArray`'s final copy. Doubles capacity on overflow; publishes via
  * [[freezeAs]] as an `ArraySeq` that wraps the builder's own array (one truncation copy only
  * when the buffer didn't fill exactly).
  *
  * No `ClassTag` required: `Array[AnyRef]` is legal storage for every generic `B` on the JVM
  * (generic parameters erase to `Object`), and the final cast inside [[freezeAs]] is always
  * safe for reference-typed `B`.
  */
private[eo] final class ObjArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[AnyRef] = new Array[AnyRef](math.max(initialCapacity, 1))
  private var len: Int           = 0

  def size: Int = len

  def append(x: AnyRef): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  def appendAllFromArraySeq[A](src: ArraySeq[A]): Unit =
    val n = src.length
    if len + n > arr.length then grow(len + n)
    var i = 0
    while i < n do
      arr(len + i) = src(i).asInstanceOf[AnyRef]
      i += 1
    len += n

  def appendAllFromPSVec[A](src: PSVec[A]): Unit =
    val n = src.length
    if len + n > arr.length then grow(len + n)
    System.arraycopy(src.arr, src.offset, arr, len, n)
    len += n

  private def grow(minCap: Int): Unit =
    var newCap = arr.length * 2
    while newCap < minCap do newCap *= 2
    val newArr = new Array[AnyRef](newCap)
    System.arraycopy(arr, 0, newArr, 0, len)
    arr = newArr

  /** Return the accumulated array exactly-sized. No-copy when the builder filled its internal
    * array exactly; one arraycopy to truncate when it didn't.
    */
  def freezeAs[A]: ArraySeq[A] =
    ArraySeq.unsafeWrapArray(freezeArr).asInstanceOf[ArraySeq[A]]

  /** Return the accumulated storage as a [[PSVec]]. Preferred over [[freezeAs]] for PowerSeries
    * focus storage since `PSVec` supports zero-copy slicing.
    */
  def freezeAsPSVec[A]: PSVec[A] = PSVec.unsafeWrap[A](freezeArr)

  /** Like [[freezeAs]] but returns the raw `Array[AnyRef]`. Used when the caller wants to keep
    * the array as primitive reference storage — e.g. `PowerSeries.AssocSndZ` which stores
    * per-element leftovers as parallel arrays rather than an `ArraySeq`.
    */
  def freezeArr: Array[AnyRef] =
    if len == arr.length then arr
    else
      val trimmed = new Array[AnyRef](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
