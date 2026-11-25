package dev.constructive.eo
package optics

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

import cats.Traverse
import dev.constructive.eo.data.{ObjArrBuilder, PSVec}

/** The hidden representation of [[Children]]. Kept in its own object so the opaque type's
  * underlying type never refers back to the opaque type itself (which the compiler rejects as a
  * cyclic reference when the definition is loaded from a downstream module).
  */
private[eo] object ChildrenRepr:

  type Repr[S] =
    scala.collection.Seq[S] | Array[Any] | PSVec[S] | Option[S] | Single[S] | Concat[S] |
      Flatten[S] | TraverseOf[S] | OwnedArray[S]

  final class Single[S](val value: S)
  final class Concat[S](val left: Repr[S], val right: Repr[S])
  final class Flatten[S](val children: () => Iterator[Repr[S]])
  final class TraverseOf[S](val elements: () => List[S])
  final class OwnedArray[S](val array: Array[Any])

/** The immediate children of a [[Plated]] node, as a reusable, non-strict view.
  *
  * The public surface is deliberately small — `iterator`, `knownSize`, `toList`, `toArray`,
  * `toAnyArray`, `size`, `isEmpty`, `foreach` — and the representation is hidden. The common shapes
  * (`List`, another `Seq`, `Array`, `PSVec`, `Option`) are stored as themselves, so `toList` shares
  * a node's own `List[S]` field instead of copying it: exactly the hand-written
  * `case Many(args) => args`. Only `Single` / `Concat` / `Flatten` / `TraverseOf` (and the
  * owned-array hand-off) allocate a wrapper.
  *
  * The derived `Plated` instances and the hand-written carriers construct values through the
  * factories; patterns are never matched outside this file.
  */
opaque type Children[S] = ChildrenRepr.Repr[S]

object Children:

  /** A shared zero-length array: there is nothing for a caller to mutate. */
  private[eo] val EmptyArray: Array[Any] = new Array[Any](0)

  // ---- factories -----------------------------------------------------------------------------

  def empty[S]: Children[S] = Nil

  def single[S](value: S): Children[S] = new ChildrenRepr.Single(value)

  /** A node's own `List[S]` field — `toList` returns it as is. */
  def fromList[S](xs: List[S]): Children[S] = xs

  /** A `Vector` / `ArraySeq` / `Seq` field — `toList` materialises it. */
  def fromSeq[S](xs: scala.collection.Seq[S]): Children[S] = xs

  /** A node's own `Array[S]` field — iterated as is; `toList` / `toAnyArray` copy. Takes an erased
    * array so the engine's `Array[Any]` rebuild path needs no reified `Array[S]` cast.
    */
  def fromArray[S](xs: Array[?]): Children[S] = xs.asInstanceOf[Array[Any]]

  /** A `PSVec` (the carrier's own focus vector) — `toList` materialises it. */
  def fromPSVec[S](xs: PSVec[S]): Children[S] = xs

  def fromOption[S](xs: Option[S]): Children[S] = xs

  /** An array the caller owns and will not mutate through this view — `toAnyArray` returns it as
    * is. Used by the derived `rebuildArray` fast path and by fresh-array carriers.
    */
  def fromOwnedArray[S](xs: Array[Any]): Children[S] = new ChildrenRepr.OwnedArray(xs)

  def fromTraverse[F[_], A](fa: F[A])(using F: Traverse[F]): Children[A] =
    new ChildrenRepr.TraverseOf(() => foldToList(fa, F))

  def concat[S](left: Children[S], right: Children[S]): Children[S] =
    if isEmptyOf(left) then right
    else if isEmptyOf(right) then left
    else new ChildrenRepr.Concat(left, right)

  // CPD-OFF — the flatten factories differ only in the source collection's iterator.
  def flattenList[A, S](xs: List[A])(f: A => Children[S]): Children[S] =
    new ChildrenRepr.Flatten(() => xs.iterator.map(f))

  def flattenSeq[A, S](xs: scala.collection.Seq[A])(f: A => Children[S]): Children[S] =
    new ChildrenRepr.Flatten(() => xs.iterator.map(f))

  def flattenArray[A, S](xs: Array[A])(f: A => Children[S]): Children[S] =
    new ChildrenRepr.Flatten(() => new scala.collection.ArrayOps(xs).iterator.map(f))

  def flattenTraverse[F[_], A, S](fa: F[A])(f: A => Children[S])(using
      F: Traverse[F]
  ): Children[S] =
    new ChildrenRepr.Flatten(() => {
      val buf = ListBuffer.empty[Children[S]]
      F.foldLeft(fa, ())((_, a) => { buf += f(a); () })
      buf.iterator
    })
  // CPD-ON

  // ---- public read surface -------------------------------------------------------------------

  extension [S](c: Children[S])
    /** A fresh iterator over the children, in order. */
    def iterator: Iterator[S] = iteratorOf(c)

    /** The size when it is cheap, `-1` otherwise (the `IterableOnce` contract). */
    def knownSize: Int = knownSizeOf(c)

    /** The children as a `List`; shares a node's own `List[S]` field. */
    def toList: List[S] = toListOf(c)

    def toArray[B >: S: ClassTag]: Array[B] = toListOf(c).toArray

    /** A **fresh, caller-mutable** `Array[Any]` — the cata engine's currency. */
    def toAnyArray: Array[Any] = toAnyArrayOf(c)

    def size: Int =
      val n = knownSizeOf(c)
      if n >= 0 then n else iteratorOf(c).length

    def isEmpty: Boolean = isEmptyOf(c)

    def foreach[U](f: S => U): Unit = iteratorOf(c).foreach(f)

  /** The carrier's `PSVec` view. Identity for a `PSVec`-backed value; otherwise materialises. */
  private[eo] def toPSVec[S](c: Children[S]): PSVec[S] = c match
    case xs: PSVec[?] => xs.asInstanceOf[PSVec[S]]
    case _            => PSVec.unsafeWrap[S](toAnyArrayOf(c))

  // ---- implementation ------------------------------------------------------------------------

  private def foldToList[F[_], A](fa: F[A], F: Traverse[F]): List[A] =
    val buf = ListBuffer.empty[A]
    F.foldLeft(fa, ())((_, a) => { buf += a; () })
    buf.toList

  private def iteratorOf[S](c: ChildrenRepr.Repr[S]): Iterator[S] = c match
    case xs: List[?]                 => xs.iterator.asInstanceOf[Iterator[S]]
    case xs: scala.collection.Seq[?] => xs.iterator.asInstanceOf[Iterator[S]]
    case xs: Array[?]  => new scala.collection.ArrayOps(xs).iterator.asInstanceOf[Iterator[S]]
    case xs: PSVec[?]  => xs.iterator.asInstanceOf[Iterator[S]]
    case xs: Option[?] => xs.iterator.asInstanceOf[Iterator[S]]
    case s: ChildrenRepr.Single[?] => Iterator.single(s.value.asInstanceOf[S])
    case c: ChildrenRepr.Concat[?] =>
      iteratorOf(c.left).asInstanceOf[Iterator[S]] ++ iteratorOf(c.right).asInstanceOf[Iterator[S]]
    case f: ChildrenRepr.Flatten[?] =>
      f.children().flatMap(k => iteratorOf(k)).asInstanceOf[Iterator[S]]
    case t: ChildrenRepr.TraverseOf[?] =>
      t.elements().iterator.asInstanceOf[Iterator[S]]
    case a: ChildrenRepr.OwnedArray[?] =>
      a.array.iterator.asInstanceOf[Iterator[S]]

  private def knownSizeOf[S](c: ChildrenRepr.Repr[S]): Int = c match
    case _: List[?]                  => -1
    case xs: scala.collection.Seq[?] => xs.knownSize
    case xs: Array[?]                => xs.length
    case xs: PSVec[?]                => xs.length
    case xs: Option[?]               => if xs.isEmpty then 0 else 1
    case _: ChildrenRepr.Single[?]   => 1
    case c: ChildrenRepr.Concat[?]   =>
      val l = knownSizeOf(c.left)
      val r = knownSizeOf(c.right)
      if l >= 0 && r >= 0 then l + r else -1
    case _: ChildrenRepr.Flatten[?]    => -1
    case _: ChildrenRepr.TraverseOf[?] => -1
    case a: ChildrenRepr.OwnedArray[?] => a.array.length

  private def isEmptyOf[S](c: ChildrenRepr.Repr[S]): Boolean =
    knownSizeOf(c) == 0 || !iteratorOf(c).hasNext

  private def toListOf[S](c: ChildrenRepr.Repr[S]): List[S] = c match
    case xs: List[?]               => xs.asInstanceOf[List[S]]
    case s: ChildrenRepr.Single[?] => List(s.value.asInstanceOf[S])
    case c: ChildrenRepr.Concat[?] =>
      toListOf(c.left).asInstanceOf[List[S]] ::: toListOf(c.right)
    case t: ChildrenRepr.TraverseOf[?] => t.elements().asInstanceOf[List[S]]
    case _                             => iteratorOf(c).toList

  private def toAnyArrayOf[S](c: ChildrenRepr.Repr[S]): Array[Any] = c match
    case xs: List[?]                 => if xs.isEmpty then EmptyArray else xs.toArray[Any]
    case xs: scala.collection.Seq[?] => if xs.isEmpty then EmptyArray else xs.toArray[Any]
    case xs: Array[?]  => if xs.length == 0 then EmptyArray else Array.copyAs[Any](xs, xs.length)
    case xs: PSVec[?]  => xs.toAnyArray
    case xs: Option[?] =>
      xs match
        case Some(x) => Array[Any](x)
        case None    => EmptyArray
    case s: ChildrenRepr.Single[?] => Array[Any](s.value.asInstanceOf[S])
    case c: ChildrenRepr.Concat[?] =>
      concatArrays(toAnyArrayOf(c.left), toAnyArrayOf(c.right))
    case f: ChildrenRepr.Flatten[?] =>
      val buf = new ObjArrBuilder(8)
      f.children().foreach(kids => appendAll(buf, toAnyArrayOf(kids), 0))
      if buf.size == 0 then EmptyArray else buf.freezeArr
    case t: ChildrenRepr.TraverseOf[?] =>
      val elems = t.elements()
      if elems.isEmpty then EmptyArray else elems.toArray[Any]
    case a: ChildrenRepr.OwnedArray[?] => a.array

  private def concatArrays(left: Array[Any], right: Array[Any]): Array[Any] =
    val n = left.length + right.length
    if n == 0 then EmptyArray
    else
      val out = new Array[Any](n)
      System.arraycopy(left, 0, out, 0, left.length)
      System.arraycopy(right, 0, out, left.length, right.length)
      out

  @tailrec private def appendAll(buf: ObjArrBuilder, xs: Array[Any], i: Int): Unit =
    if i < xs.length then
      buf.append(xs(i))
      appendAll(buf, xs, i + 1)
