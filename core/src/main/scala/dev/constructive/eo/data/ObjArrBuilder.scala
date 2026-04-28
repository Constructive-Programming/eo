package dev.constructive.eo
package data

/** Minimal grow-on-demand `Array[AnyRef]` builder. Used by [[MultiFocus]]'s `mfAssocPSVec` and
  * [[optics.Traversal.pEach]] to accumulate focus elements without `ArrayBuffer.toArray`'s final
  * copy. No `ClassTag` needed; the cast in [[freezeAsPSVec]] is sound (generic B erases to Object).
  */
final private[eo] class ObjArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[AnyRef] = new Array[AnyRef](math.max(initialCapacity, 1))
  private var len: Int = 0

  def size: Int = len

  def append(x: AnyRef): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  /** Append without the grow-check; caller must pre-size at construction. Used on the
    * `mfAssocPSVec` always-hit fast path where the total is known upfront.
    */
  inline def unsafeAppend(x: AnyRef): Unit =
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

  /** Return the accumulated storage as a [[PSVec]]. `PSVec.unsafeWrap` normalises 0/1-element
    * results into `Empty` / `Single` (no backing array).
    */
  def freezeAsPSVec[A]: PSVec[A] = PSVec.unsafeWrap[A](freezeArr)

  /** Return the raw `Array[AnyRef]` exactly-sized (one arraycopy to truncate when needed). Used by
    * `MultiFocus.AssocSndZ` for parallel-array per-element leftovers.
    */
  def freezeArr: Array[AnyRef] =
    if len == arr.length then arr
    else
      val trimmed = new Array[AnyRef](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
