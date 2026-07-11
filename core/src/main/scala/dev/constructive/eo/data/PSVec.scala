package dev.constructive.eo
package data

import scala.annotation.tailrec

import cats.{Applicative, Eval, Foldable, Functor, Traverse}

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

  /** Materialise as a `List` — the read-side bridge for `Plated.children` / `universe` and the
    * carriers (circe / avro) that reconstruct from an ordered sequence.
    */
  def toList: List[B] =
    val b = List.newBuilder[B]
    val n = length
    @tailrec def loop(i: Int): Unit =
      if i < n then
        b += apply(i)
        loop(i + 1)
    loop(0)
    b.result()

  /** Materialise as a fresh `Array[AnyRef]`. `Slice` overrides with `System.arraycopy` (intrinsic)
    * so the common rebuild path in `Traversal.pEach`'s `from` is one memcpy.
    */
  def toAnyRefArray: Array[AnyRef] =
    val n = length
    val a = new Array[AnyRef](n)
    @tailrec def loop(i: Int): Unit =
      if i < n then
        a(i) = apply(i).asInstanceOf[AnyRef]
        loop(i + 1)
    loop(0)
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
        @tailrec def loop(i: Int): Boolean =
          if i >= length then true
          else if apply(i) != other.apply(i) then false
          else loop(i + 1)
        loop(0)
    case _ => false

  override def hashCode(): Int =
    @tailrec def loop(i: Int, h: Int): Int =
      if i < length then
        val e = apply(i)
        loop(i + 1, h * 31 + (if e == null then 0 else e.hashCode))
      else h
    loop(0, 1)

  // Cold / debug-only path — `mkString` says exactly "comma-join the elements"; no hand-rolled
  // loop needed here (the hot paths above stay index-based for allocation control).
  override def toString(): String = toList.mkString("PSVec(", ", ", ")")

/** Constructors for [[PSVec]] — the primary entry points are `empty`, `singleton`, and `unsafeWrap`
  * (zero-copy from an `Array[AnyRef]`). The three `Slice` / `Single` / `Empty` subclasses are
  * internal to the `dev.constructive.eo.data` package and stable across release lines only at the
  * aggregate `PSVec` supertype level.
  */
object PSVec:

  // Cats instances — live here (the PSVec companion) because implicit scope for
  // `Functor[PSVec]` / `Foldable[PSVec]` / `Traverse[PSVec]` anchors on PSVec's own
  // companion, NOT on MultiFocusK's (where these were once parked behind
  // `import data.MultiFocus.given`).

  /** `cats.Functor` — index-based map into one fresh backing array (0/1-element results normalise
    * through [[unsafeWrap]] to `Empty` / `Single`).
    *
    * @group Instances
    */
  given pSVecFunctor: Functor[PSVec] with

    def map[A, B](fa: PSVec[A])(f: A => B): PSVec[B] =
      val n = fa.length
      if n == 0 then PSVec.empty[B]
      else
        val arr = new Array[AnyRef](n)
        @tailrec def loop(i: Int): Unit =
          if i < n then
            arr(i) = f(fa(i)).asInstanceOf[AnyRef]
            loop(i + 1)
        loop(0)
        PSVec.unsafeWrap[B](arr)

  /** `cats.Foldable` — index-based `foldLeft`, `Eval`-deferred `foldRight` (stack-safe), O(1)
    * `size`.
    *
    * @group Instances
    */
  given pSVecFoldable: Foldable[PSVec] with

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      val n = fa.length
      @tailrec def loop(i: Int, acc: B): B =
        if i < n then loop(i + 1, f(acc, fa(i)))
        else acc
      loop(0, b)

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] =
      val n = fa.length
      def loop(i: Int): Eval[B] =
        if i >= n then lb
        else f(fa(i), Eval.defer(loop(i + 1)))
      Eval.defer(loop(0))

    override def size[A](fa: PSVec[A]): Long = fa.length.toLong

  /** `cats.Traverse` — the instance Traversal's `.modifyA` / `.all` route through when the carrier
    * is `MultiFocus[PSVec]`. Effects run left-to-right in index order, results land in one
    * pre-sized backing array threaded through `G.map2` (no per-element vector rebuild).
    *
    * @group Instances
    */
  given pSVecTraverse: Traverse[PSVec] with

    def traverse[G[_]: Applicative, A, B](fa: PSVec[A])(f: A => G[B]): G[PSVec[B]] =
      val G = Applicative[G]
      val n = fa.length
      if n == 0 then G.pure(PSVec.empty[B])
      else
        @tailrec def loop(i: Int, acc: G[Array[AnyRef]]): G[Array[AnyRef]] =
          if i < n then
            val idx = i
            val gb = f(fa(idx))
            loop(
              i + 1,
              G.map2(acc, gb) { (a, b) =>
                a(idx) = b.asInstanceOf[AnyRef]
                a
              },
            )
          else acc
        val acc = loop(0, G.pure(new Array[AnyRef](n)))
        G.map(acc)(arr => PSVec.unsafeWrap[B](arr))

    def foldLeft[A, B](fa: PSVec[A], b: B)(f: (B, A) => B): B =
      pSVecFoldable.foldLeft(fa, b)(f)

    def foldRight[A, B](fa: PSVec[A], lb: Eval[B])(
        f: (A, Eval[B]) => Eval[B]
    ): Eval[B] = pSVecFoldable.foldRight(fa, lb)(f)

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

  /** Two-element vector — builds the backing array directly, no `List` / `Iterable` intermediate.
    * The common arity for binary-tree children (recursion-scheme coalgebras, `Node(l, r)`), where
    * `PSVec.of(l, r)` replaces the wasteful `fromIterable(List(l, r))`.
    */
  def of[B](b0: B, b1: B): PSVec[B] =
    val a = new Array[AnyRef](2)
    a(0) = b0.asInstanceOf[AnyRef]
    a(1) = b1.asInstanceOf[AnyRef]
    new Slice[B](a, 0, 2)

  /** Wrap an `Array[AnyRef]` verbatim as a PSVec. Shares ownership with the caller. Returns the
    * most specialised variant for the array's length so callers need not special-case empty /
    * singleton themselves.
    */
  def unsafeWrap[B](arr: Array[AnyRef]): PSVec[B] =
    arr.length match
      case 0 => Empty
      case 1 => new Single[B](arr(0).asInstanceOf[B])
      case _ => new Slice[B](arr, 0, arr.length)

  /** Build a PSVec from any `Iterable` — one backing-array allocation, specialised at length 0 / 1.
    * The bridge for `Plated` carriers whose children arrive as a `List` / `Vector` / circe object
    * values rather than already as a focus vector.
    */
  def fromIterable[B](xs: Iterable[B]): PSVec[B] =
    val n = xs.size
    if n == 0 then Empty
    else
      val a = new Array[AnyRef](n)
      val it = xs.iterator
      @tailrec def loop(i: Int): Unit =
        if it.hasNext then
          a(i) = it.next().asInstanceOf[AnyRef]
          loop(i + 1)
      loop(0)
      unsafeWrap(a)
