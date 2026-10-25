package dev.constructive.eo
package spark

import scala.collection.Factory
import scala.collection.immutable.ArraySeq
import scala.compiletime.{constValueTuple, erasedValue, summonInline}
import scala.deriving.Mirror
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoders.*
import org.apache.spark.sql.catalyst.encoders.{AgnosticEncoder, Codec}
import org.apache.spark.sql.types.Metadata

/** Scala 3 derivation of Spark's `AgnosticEncoder`s — the encoder layer Spark Classic and Spark
  * Connect share — in place of Spark's `TypeTag`-based `Encoders.product`, which Scala 3 cannot
  * call. An `AgnosticEncoder[T]` IS an `org.apache.spark.sql.Encoder[T]`.
  *
  * Covered: primitives, `String`, `Array[Byte]`, `BigInt` / `BigDecimal`, `java.time` `LocalDate` /
  * `Instant` / `LocalDateTime`, `Option`, `List` / `Vector` / `Seq`, `Map`, and top-level case
  * classes (no outer pointer). Sum types (enums / sealed traits) have no Spark struct encoding and
  * are deliberately not derived.
  *
  * A case class is NOT a Spark `ProductEncoder`: decoding one makes the Connect client look its
  * constructor up through `ScalaReflection`, whose initialiser builds the Scala 2
  * runtime-reflection universe — which cannot start on Scala 3.8+'s stdlib ("class Array does not
  * have a member apply"). Instead it is a `TransformingEncoder` over a `RowEncoder` of its fields,
  * and the codec rebuilds the value through its `Mirror`: same schema, no reflection.
  *
  * Collections dodge the same trap: Spark decodes a Scala `Iterable` / `Map` by resolving the
  * collection's companion through `currentMirror`. Its two reflection-free paths are
  * `immutable.ArraySeq` (arrays) and `java.util.Map` (maps, built as a `HashMap`), so every
  * collection here is a `TransformingEncoder` over one of those.
  */
object SparkEncoders:

  given AgnosticEncoder[Boolean] = PrimitiveBooleanEncoder
  given AgnosticEncoder[Int] = PrimitiveIntEncoder
  given AgnosticEncoder[Long] = PrimitiveLongEncoder
  given AgnosticEncoder[Double] = PrimitiveDoubleEncoder
  given AgnosticEncoder[String] = StringEncoder
  given AgnosticEncoder[Byte] = PrimitiveByteEncoder
  given AgnosticEncoder[Short] = PrimitiveShortEncoder
  given AgnosticEncoder[Float] = PrimitiveFloatEncoder
  given AgnosticEncoder[Array[Byte]] = BinaryEncoder
  given AgnosticEncoder[BigInt] = ScalaBigIntEncoder

  /** `DECIMAL(38, 18)`, Spark's default for Scala `BigDecimal`. */
  given AgnosticEncoder[BigDecimal] = DEFAULT_SCALA_DECIMAL_ENCODER

  given AgnosticEncoder[java.time.LocalDate] = LocalDateEncoder(false)
  given AgnosticEncoder[java.time.Instant] = InstantEncoder(false)
  given AgnosticEncoder[java.time.LocalDateTime] = LocalDateTimeEncoder

  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[Option[A]] = OptionEncoder(e)

  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[List[A]] = sequence(e)
  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[Vector[A]] = sequence(e)
  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[Seq[A]] = sequence(e)

  given [K, V](using k: AgnosticEncoder[K], v: AgnosticEncoder[V]): AgnosticEncoder[Map[K, V]] =
    TransformingEncoder[Map[K, V], java.util.Map[K, V]](
      ClassTag(classOf[Map[?, ?]]),
      MapEncoder[java.util.Map[K, V], K, V](
        ClassTag(classOf[java.util.Map[?, ?]]),
        k,
        v,
        v.nullable
      ),
      () => MapCodec[K, V](),
      nullable = true,
    )

  /** Tuples encode like case classes (fields `_1`, `_2`), not as Spark's tuple `ProductEncoder`,
    * whose decode needs the same dead `ScalaReflection` (see the scaladoc above).
    */
  given [A, B](using a: AgnosticEncoder[A], b: AgnosticEncoder[B]): AgnosticEncoder[(A, B)] =
    tuple2(a, b)

  /** The encoder of `(A, B)` from its components' encoders. */
  def tuple2[A, B](a: AgnosticEncoder[A], b: AgnosticEncoder[B]): AgnosticEncoder[(A, B)] =
    product(
      ClassTag(classOf[(?, ?)]),
      List(
        EncoderField("_1", a, a.nullable, Metadata.empty, None, None),
        EncoderField("_2", b, b.nullable, Metadata.empty, None, None),
      ),
      summon[Mirror.ProductOf[(A, B)]],
    )

  /** Encoder for a top-level case class: one struct field per constructor parameter, named by the
    * parameter, nullable exactly when the field's own encoder is.
    */
  inline def derived[T <: Product](using
      m: Mirror.ProductOf[T],
      ct: ClassTag[T]
  ): AgnosticEncoder[T] =
    val names = constValueTuple[m.MirroredElemLabels].productIterator.map(_.toString).toList
    val fields = names.zip(encoders[m.MirroredElemTypes]).map { (n, e) =>
      EncoderField(n, e, e.nullable, Metadata.empty, None, None)
    }
    product(ct, fields, m)

  private def product[T](
      ct: ClassTag[T],
      fields: List[EncoderField],
      m: Mirror.ProductOf[T],
  ): AgnosticEncoder[T] =
    TransformingEncoder[T, Row](ct, RowEncoder(fields), () => ProductCodec(m), nullable = true)

  private def sequence[C <: Iterable[A], A](e: AgnosticEncoder[A])(using
      f: Factory[A, C],
      ct: ClassTag[C],
  ): AgnosticEncoder[C] =
    TransformingEncoder[C, ArraySeq[A]](
      ct,
      IterableEncoder[ArraySeq[A], A](ClassTag(classOf[ArraySeq[?]]), e, e.nullable, false),
      () => SeqCodec[C, A](f),
      nullable = true,
    )

  private inline def encoders[Ts <: Tuple]: List[AgnosticEncoder[?]] =
    inline erasedValue[Ts] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[AgnosticEncoder[t]] :: encoders[ts]

  /** One level of a case class ↔ its `Row` of field values. Nested case classes are already decoded
    * by their own field encoders, so the row holds the final field values. A NULL struct travels
    * through the codec as `null` both ways (Spark's NULL, not a missed optic focus).
    */
  final private class ProductCodec[T](m: Mirror.ProductOf[T]) extends Codec[T, Row]:

    def encode(in: T): Row = (in: Any) match
      case p: Product => Row.fromSeq(p.productIterator.toSeq)
      case _          => null.asInstanceOf[Row]

    def decode(out: Row): T = (out: Row | Null) match
      case null   => null.asInstanceOf[T]
      case r: Row => m.fromProduct(RowProduct(r))

  final private class RowProduct(row: Row) extends Product:
    def productArity: Int = row.length
    def productElement(n: Int): Any = row.get(n)
    def canEqual(that: Any): Boolean = false

  /** A Scala collection ↔ the `ArraySeq` Spark decodes without reflection; NULL passes through. */
  final private class SeqCodec[C <: Iterable[A], A](f: Factory[A, C]) extends Codec[C, ArraySeq[A]]:

    def encode(in: C): ArraySeq[A] =
      if (in: C | Null) == null then null.asInstanceOf[ArraySeq[A]]
      else ArraySeq.unsafeWrapArray(in.toArray[Any]).asInstanceOf[ArraySeq[A]]

    def decode(out: ArraySeq[A]): C =
      if (out: ArraySeq[A] | Null) == null then null.asInstanceOf[C] else f.fromSpecific(out)

  /** A Scala `Map` ↔ the `java.util.Map` Spark decodes without reflection; NULL passes through. */
  final private class MapCodec[K, V] extends Codec[Map[K, V], java.util.Map[K, V]]:

    def encode(in: Map[K, V]): java.util.Map[K, V] =
      if (in: Map[K, V] | Null) == null then null.asInstanceOf[java.util.Map[K, V]] else in.asJava

    def decode(out: java.util.Map[K, V]): Map[K, V] =
      if (out: java.util.Map[K, V] | Null) == null then null.asInstanceOf[Map[K, V]]
      else out.asScala.toMap
