package dev.constructive.eo
package spark

import java.util.ArrayList
import org.apache.spark.types.variant.VariantUtil.Type
import org.apache.spark.types.variant.{Variant, VariantBuilder}
import org.apache.spark.unsafe.types.VariantVal

import data.PSVec
import optics.{Optional, PickMendPrism, Plated, Prism, Traversal}

/** Optics over Spark 4's semi-structured `VARIANT` values — [[VariantVal]], what a `Row` holds for
  * a `VARIANT` column (`parse_json`, `try_variant_get`'s input). The eo playbook for JSON-like
  * trees (circe / zio-json / kyo kits) on Spark's binary encoding:
  *
  *   - '''Scalar prisms''' ([[string]], [[long]], [[double]], [[boolean]]).
  *   - '''Navigation''' ([[field]], [[at]], [[each]]) — sibling-preserving; misses (wrong node
  *     kind, absent key, out-of-range index) pass writes through untouched.
  *   - '''Whole-tree rewrites''' — [[platedVariant]].
  *
  * '''Canonical encoding.''' `VariantVal` equality is byte equality of (value, metadata), and one
  * logical variant has many encodings (dictionary order, unused keys). Every value this kit HANDS
  * OUT or WRITES is re-encoded through a fresh `VariantBuilder` — one encoding per logical value —
  * so byte equality is structural equality on everything the kit produced. A write rebuilds the
  * whole document (the binary format has no in-place update). Inputs read but not written keep
  * their bytes; [[canonical]] normalises one explicitly.
  *
  * Object keys are unique and stored sorted, so [[field]] has no duplicate-key caveat.
  */
object Variants:

  /** Re-encode `v` canonically: same logical value, deterministic bytes. */
  def canonical(v: VariantVal): VariantVal = build(_.appendVariant(open(v)))

  /** A variant object. Keys must be distinct (the encoding stores each once, sorted). */
  def obj(fields: (String, VariantVal)*): VariantVal =
    build(writeObject(_, fields.toList.map((k, v) => (k, open(v)))))

  /** A variant array. */
  def arr(elements: VariantVal*): VariantVal = build(writeArray(_, elements.toList.map(open)))

  /** The variant NULL (a value, unlike SQL NULL). */
  val nullValue: VariantVal = build(_.appendNull())

  /** Prism onto a string scalar. */
  val string: PickMendPrism[VariantVal, String, String] =
    scalar(Type.STRING, _.getString, (b, s) => b.appendString(s))

  /** Prism onto an integral scalar (any of the variant's 1/2/4/8-byte integer encodings). */
  val long: PickMendPrism[VariantVal, Long, Long] =
    scalar(Type.LONG, _.getLong, (b, l) => b.appendLong(l))

  /** Prism onto a double scalar. */
  val double: PickMendPrism[VariantVal, Double, Double] =
    scalar(Type.DOUBLE, _.getDouble, (b, d) => b.appendDouble(d))

  /** Prism onto a boolean scalar. */
  val boolean: PickMendPrism[VariantVal, Boolean, Boolean] =
    scalar(Type.BOOLEAN, _.getBoolean, (b, x) => b.appendBoolean(x))

  /** Optional into an object's field `name`. */
  def field(name: String): Optional[VariantVal, VariantVal, VariantVal, VariantVal] =
    Optional[VariantVal, VariantVal, VariantVal, VariantVal](
      s =>
        objectFields(open(s))
          .flatMap(_.collectFirst { case (`name`, v) => standalone(v) })
          .toRight(s),
      (s, b) =>
        objectFields(open(s)) match
          case Some(fs) if fs.exists(_._1 == name) =>
            build(writeObject(_, fs.map((k, v) => if k == name then (k, open(b)) else (k, v))))
          case _ => s,
    )

  /** Optional into an array's element `i`. */
  def at(i: Int): Optional[VariantVal, VariantVal, VariantVal, VariantVal] =
    Optional[VariantVal, VariantVal, VariantVal, VariantVal](
      s => arrayElements(open(s)).filter(_.isDefinedAt(i)).map(es => standalone(es(i))).toRight(s),
      (s, b) =>
        arrayElements(open(s)) match
          case Some(es) if es.isDefinedAt(i) => build(writeArray(_, es.updated(i, open(b))))
          case _                             => s,
    )

  /** Traversal over every element of an array — zero foci (write pass-through) on anything else. */
  val each: Traversal[VariantVal, VariantVal, VariantVal, VariantVal] =
    Traversal.selfChildren[VariantVal](
      s => arrayElements(open(s)).fold(noChildren)(es => PSVec.from(es.map(standalone))),
      (s, cs) =>
        if arrayElements(open(s)).isEmpty then s
        else build(writeArray(_, cs.toList.map(open))),
    )

  /** [[optics.Plated]] over the variant tree — children are an object's field values (key order)
    * and an array's elements; scalars have none. Rebuilding keeps every key.
    */
  given platedVariant: Plated[VariantVal] = Plated.fromChildrenVec(children, rebuild)

  // ---- encoding ----------------------------------------------------------

  private val noChildren: PSVec[VariantVal] = PSVec.empty[VariantVal]

  private def open(v: VariantVal): Variant = new Variant(v.getValue, v.getMetadata)

  private def build(write: VariantBuilder => Unit): VariantVal =
    val b = new VariantBuilder(false)
    write(b)
    val r = b.result()
    new VariantVal(r.getValue, r.getMetadata)

  /** A nested variant (which shares its parent's buffers) as a canonical value of its own. */
  private def standalone(v: Variant): VariantVal = build(_.appendVariant(v))

  private def scalar[A](
      tpe: Type,
      read: Variant => A,
      write: (VariantBuilder, A) => Unit,
  ): PickMendPrism[VariantVal, A, A] =
    Prism.optional(
      s =>
        val v = open(s)
        Option.when(v.getType == tpe)(read(v))
      ,
      a => build(write(_, a)),
    )

  private def objectFields(v: Variant): Option[List[(String, Variant)]] =
    Option.when(v.getType == Type.OBJECT) {
      (0 until v.objectSize).toList.map { i =>
        val f = v.getFieldAtIndex(i)
        (f.key, f.value)
      }
    }

  private def arrayElements(v: Variant): Option[List[Variant]] =
    Option.when(v.getType == Type.ARRAY)(
      (0 until v.arraySize).toList.map(i => v.getElementAtIndex(i))
    )

  /** Keys are added in SORTED order, interleaved depth-first with the field values — exactly the
    * order `appendVariant` re-encodes a stored (sorted) object in, which is what makes every value
    * built here canonical regardless of the order the caller listed the fields in.
    */
  private def writeObject(b: VariantBuilder, fields: List[(String, Variant)]): Unit =
    val start = b.getWritePos
    val entries = new ArrayList[VariantBuilder.FieldEntry](fields.size)
    fields.sortBy(_._1).foreach { (k, v) =>
      entries.add(new VariantBuilder.FieldEntry(k, b.addKey(k), b.getWritePos - start))
      b.appendVariant(v)
    }
    b.finishWritingObject(start, entries)

  private def writeArray(b: VariantBuilder, elements: List[Variant]): Unit =
    val start = b.getWritePos
    val offsets = new ArrayList[Integer](elements.size)
    elements.foreach { v =>
      offsets.add(Integer.valueOf(b.getWritePos - start))
      b.appendVariant(v)
    }
    b.finishWritingArray(start, offsets)

  private def children(s: VariantVal): PSVec[VariantVal] =
    val v = open(s)
    objectFields(v)
      .map(fs => PSVec.from(fs.map((_, c) => standalone(c))))
      .orElse(arrayElements(v).map(es => PSVec.from(es.map(standalone))))
      .getOrElse(noChildren)

  private def rebuild(s: VariantVal, cs: PSVec[VariantVal]): VariantVal =
    val v = open(s)
    objectFields(v) match
      case Some(fs) =>
        build(writeObject(_, fs.indices.toList.map(i => (fs(i)._1, open(cs(i))))))
      case None =>
        arrayElements(v) match
          case Some(_) => build(writeArray(_, cs.toList.map(open)))
          case None    => s

end Variants
