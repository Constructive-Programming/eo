package dev.constructive.eo
package data

import scala.annotation.tailrec

/** Minimal grow-on-demand `Array[Any]` builder. Used by [[MultiFocus]]'s `mfAssocPSVec` and
  * [[optics.Traversal.pEach]] to accumulate focus elements without `ArrayBuffer.toArray`'s final
  * copy. No `ClassTag` needed; the cast in [[freezeAsPSVec]] is sound (generic B erases to Object).
  */
final private[eo] class ObjArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[Any] = new Array[Any](math.max(initialCapacity, 1))
  private var len: Int = 0

  def size: Int = len

  def append(x: Any): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  /** Append without the grow-check; caller must pre-size at construction. Used on the
    * `mfAssocPSVec` always-hit fast path where the total is known upfront.
    */
  inline def unsafeAppend(x: Any): Unit =
    arr(len) = x
    len += 1

  def appendAllFromPSVec[A](src: PSVec[A]): Unit =
    src match
      case PSVec.Empty        => ()
      case s: PSVec.Single[?] => append(s.b)
      case s: PSVec.Slice[?]  =>
        val n = s.length
        if len + n > arr.length then grow(len + n)
        System.arraycopy(s.arr, s.offset, arr, len, n)
        len += n

  private def grow(minCap: Int): Unit =
    @tailrec def doubleTo(cap: Int): Int =
      if cap < minCap then doubleTo(cap * 2)
      else cap
    val newCap = doubleTo(arr.length * 2)
    val newArr = new Array[Any](newCap)
    System.arraycopy(arr, 0, newArr, 0, len)
    arr = newArr

  /** Return the accumulated storage as a [[PSVec]]. `PSVec.unsafeWrap` normalises 0/1-element
    * results into `Empty` / `Single` (no backing array).
    */
  def freezeAsPSVec[A]: PSVec[A] = PSVec.unsafeWrap[A](freezeArr)

  /** Return the raw `Array[Any]` exactly-sized (one arraycopy to truncate when needed). Used by
    * `MultiFocus.AssocSndZ` for parallel-array per-element leftovers.
    */
  def freezeArr: Array[Any] =
    if len == arr.length then arr
    else
      val trimmed = new Array[Any](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
