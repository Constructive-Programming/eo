package dev.constructive.eo
package data

/** Grow-on-demand primitive `Array[Int]` builder. Used by `MultiFocus.mfAssocPSVec` to accumulate
  * per-element focus counts without `Integer` boxing and without `ArrayBuffer.toArray`'s final
  * copy.
  */
final private[eo] class IntArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[Int] = new Array[Int](math.max(initialCapacity, 1))
  private var len: Int = 0

  def size: Int = len

  def append(x: Int): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  /** Append without the grow-check; caller must pre-size at construction. */
  inline def unsafeAppend(x: Int): Unit =
    arr(len) = x
    len += 1

  private def grow(minCap: Int): Unit =
    var newCap = arr.length * 2
    while newCap < minCap do newCap *= 2
    val newArr = new Array[Int](newCap)
    System.arraycopy(arr, 0, newArr, 0, len)
    arr = newArr

  /** Return the accumulated array exactly-sized (one arraycopy to truncate when needed). */
  def freeze: Array[Int] =
    if len == arr.length then arr
    else
      val trimmed = new Array[Int](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
