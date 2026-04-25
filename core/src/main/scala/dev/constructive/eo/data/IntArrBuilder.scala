package dev.constructive.eo
package data

/** Grow-on-demand primitive `Array[Int]` builder. Used by [[PowerSeries.assoc]] to accumulate
  * per-element focus counts without boxing each `Int` into an `Integer` (which a `Tuple2[Int, …]`
  * would force) and without dragging in an `ArrayBuffer[Int]` that would pay a final `toArray`
  * copy.
  *
  * Doubles capacity on overflow; `freeze` returns the primitive array exactly-sized (one truncation
  * `arraycopy` only when the builder didn't fill its internal array exactly).
  */
final private[eo] class IntArrBuilder(initialCapacity: Int = 16):
  private var arr: Array[Int] = new Array[Int](math.max(initialCapacity, 1))
  private var len: Int = 0

  def size: Int = len

  def append(x: Int): Unit =
    if len == arr.length then grow(len + 1)
    arr(len) = x
    len += 1

  /** Append without the grow-check. Caller MUST have pre-sized the builder at construction time
    * with `initialCapacity` ≥ the total append count for this builder's lifetime, otherwise an
    * `ArrayIndexOutOfBoundsException` fires. Used on hot paths where the total is known upfront
    * (`PowerSeries.assoc`'s PSSingleton fast paths) — skips a branch + length read per call.
    */
  inline def unsafeAppend(x: Int): Unit =
    arr(len) = x
    len += 1

  private def grow(minCap: Int): Unit =
    var newCap = arr.length * 2
    while newCap < minCap do newCap *= 2
    val newArr = new Array[Int](newCap)
    System.arraycopy(arr, 0, newArr, 0, len)
    arr = newArr

  /** Return the accumulated primitive array exactly-sized. No copy when the builder filled its
    * internal array exactly; one arraycopy to truncate when it didn't.
    */
  def freeze: Array[Int] =
    if len == arr.length then arr
    else
      val trimmed = new Array[Int](len)
      System.arraycopy(arr, 0, trimmed, 0, len)
      trimmed
