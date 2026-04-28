package dev.constructive.eo
package data

/** Lightweight array-backed focus vector underlying `MultiFocus[PSVec]`. Three variants so empty
  * and singleton focus vectors don't pay a backing-array allocation:
  *
  *   - [[PSVec.Empty]] — shared singleton; Prism / Affine miss branches allocate nothing.
  *   - [[PSVec.Single]] — element stored inline (~16 B vs a backing-array view's ~40 B).
  *   - [[PSVec.Slice]] — arbitrary `(arr, offset, length)` view. [[slice]] is a zero-copy pointer
  *     update — what `mfAssocPSVec.composeFrom` relies on for O(1) per-element reassembly.
  *
  * Equality is value-based across variants.
  */
sealed trait PSVec[+B]:
  def length: Int

  /** Indexed access. Unchecked: UB if `i < 0 || i >= length`. */
  def apply(i: Int): B

  /** Read the first element. Unchecked: UB if [[length]] == 0. */
  def head: B

  /** Zero-copy view `[from, until)`, clamped to this vector's bounds. */
  def slice(from: Int, until: Int): PSVec[B]

  inline def isEmpty: Boolean = length == 0

  /** Materialise as a fresh `Array[AnyRef]`. `Slice` overrides with `System.arraycopy` (intrinsic)
    * so the common rebuild path in `Traversal.pEach`'s `from` is one memcpy.
    */
  def toAnyRefArray: Array[AnyRef] =
    val n = length
    val a = new Array[AnyRef](n)
    var i = 0
    while i < n do
      a(i) = apply(i).asInstanceOf[AnyRef]
      i += 1
    a

  /** Like [[toAnyRefArray]] but MAY share the backing array zero-copy when a dense Slice covers its
    * full range. Callers MUST treat the result as immutable. Used by consumers that also won't
    * mutate, e.g. [[optics.Traversal.pEach]]'s `from` → `ArraySeq.unsafeWrapArray`). Default is the
    * safe copy; only `Slice` overrides to share.
    */
  def unsafeShareableArray: Array[AnyRef] = toAnyRefArray

  override def equals(that: Any): Boolean = that match
    case other: PSVec[?] =>
      if length != other.length then false
      else
        var i = 0
        var eq = true
        while eq && i < length do
          eq = apply(i) == other.apply(i)
          i += 1
        eq
    case _ => false

  override def hashCode(): Int =
    var h = 1
    var i = 0
    while i < length do
      val e = apply(i)
      h = h * 31 + (if e == null then 0 else e.hashCode)
      i += 1
    h

  override def toString(): String =
    val sb = new StringBuilder("PSVec(")
    var i = 0
    while i < length do
      if i > 0 then sb.append(", ")
      sb.append(apply(i))
      i += 1
    sb.append(")").toString

/** Constructors for [[PSVec]] — the primary entry points are `empty`, `singleton`, and `unsafeWrap`
  * (zero-copy from an `Array[AnyRef]`). The three `Slice` / `Single` / `Empty` subclasses are
  * internal to the `dev.constructive.eo.data` package and stable across release lines only at the
  * aggregate `PSVec` supertype level.
  */
object PSVec:

  /** Zero-element shared singleton; Prism / Affine miss branches allocate nothing. */
  case object Empty extends PSVec[Nothing]:
    def length: Int = 0

    override val toAnyRefArray: Array[AnyRef] = new Array[AnyRef](0)

    def apply(i: Int): Nothing =
      throw new IndexOutOfBoundsException(s"PSVec.Empty.apply($i)")

    def head: Nothing =
      throw new NoSuchElementException("PSVec.Empty.head")

    def slice(from: Int, until: Int): PSVec[Nothing] = Empty

  /** Single-element vector — stores the element inline, no backing array. */
  final class Single[+B](val b: B) extends PSVec[B]:
    def length: Int = 1

    def apply(i: Int): B =
      if i == 0 then b
      else throw new IndexOutOfBoundsException(s"PSVec.Single.apply($i)")

    def head: B = b

    def slice(from: Int, until: Int): PSVec[B] =
      val lo = math.max(0, from)
      val hi = math.min(1, math.max(lo, until))
      if lo == 0 && hi == 1 then this else Empty

    override def toAnyRefArray: Array[AnyRef] =
      val a = new Array[AnyRef](1)
      a(0) = b.asInstanceOf[AnyRef]
      a

  /** Array-backed view with offset + length. [[slice]] is a pointer update over the shared backing
    * array — the zero-copy reassembly path `mfAssocPSVec` depends on.
    */
  final class Slice[+B] private[data] (
      private[data] val arr: Array[AnyRef],
      private[data] val offset: Int,
      val length: Int,
  ) extends PSVec[B]:
    def apply(i: Int): B = arr(offset + i).asInstanceOf[B]
    def head: B = arr(offset).asInstanceOf[B]

    def slice(from: Int, until: Int): PSVec[B] =
      val lo = math.max(0, from)
      val hi = math.min(length, math.max(lo, until))
      val n = hi - lo
      if n == 0 then Empty
      else if n == 1 then new Single[B](arr(offset + lo).asInstanceOf[B])
      else new Slice[B](arr, offset + lo, n)

    override def toAnyRefArray: Array[AnyRef] =
      val a = new Array[AnyRef](length)
      System.arraycopy(arr, offset, a, 0, length)
      a

    override def unsafeShareableArray: Array[AnyRef] =
      if offset == 0 && length == arr.length then arr
      else toAnyRefArray

  /** Zero-length shared vector. */
  def empty[B]: PSVec[B] = Empty

  /** Single-element vector, stored inline (no backing array). */
  def singleton[B](b: B): PSVec[B] = new Single(b)

  /** Wrap an `Array[AnyRef]` verbatim as a PSVec. Shares ownership with the caller. Returns the
    * most specialised variant for the array's length so callers need not special-case empty /
    * singleton themselves.
    */
  def unsafeWrap[B](arr: Array[AnyRef]): PSVec[B] =
    arr.length match
      case 0 => Empty
      case 1 => new Single[B](arr(0).asInstanceOf[B])
      case _ => new Slice[B](arr, 0, arr.length)
