package dev.constructive.eo
package spark

import scala.annotation.publicInBinary
import scala.collection.Factory
import scala.collection.immutable.ArraySeq
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
  * and the codec calls its constructor directly: same schema, no reflection. The derivation is a
  * Hearth macro (`SparkEncodersMacro`), as eo-generics' are.
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

  /** Encoder for a top-level case class: one struct field per constructor parameter, named by the
    * parameter, nullable exactly when the field's own encoder is. Every field type needs an
    * `AgnosticEncoder` in scope.
    */
  inline def derived[T]: AgnosticEncoder[T] = ${ SparkEncodersMacro.derivedImpl[T] }

  /** The case-class encoder the macro emits: a `TransformingEncoder` over a `RowEncoder` of the
    * fields, through `codec`. It builds Spark's `EncoderField`s here rather than in the expansion:
    * their Scala 2 existential `AgnosticEncoder[_]` raises a compatibility warning wherever a macro
    * constructs one.
    */
  @publicInBinary
  private[spark] def product[T](
      ct: ClassTag[T],
      fields: List[(String, AgnosticEncoder[?])],
      codec: Codec[T, Row],
  ): AgnosticEncoder[T] =
    val row = RowEncoder(fields.map { (name, e) =>
      EncoderField(name, e, e.nullable, Metadata.empty, None, None)
    })
    TransformingEncoder[T, Row](ct, row, () => codec, nullable = true)

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
