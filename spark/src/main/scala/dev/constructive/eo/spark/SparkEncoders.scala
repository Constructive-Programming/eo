package dev.constructive.eo
package spark

import scala.annotation.publicInBinary
import scala.reflect.ClassTag

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoders.*
import org.apache.spark.sql.catalyst.encoders.{AgnosticEncoder, Codec}
import org.apache.spark.sql.types.Metadata

/** Scala 3 derivation of Spark's `AgnosticEncoder`s — the encoder layer Spark Classic and Spark
  * Connect share — in place of Spark's `TypeTag`-based `Encoders.product`, which Scala 3 cannot
  * call. An `AgnosticEncoder[T]` IS an `org.apache.spark.sql.Encoder[T]`.
  *
  * Spike scope: primitives, `String`, `Option`, and top-level case classes (no outer pointer).
  *
  * A case class is NOT a Spark `ProductEncoder`: decoding one makes the Connect client look its
  * constructor up through `ScalaReflection`, whose initialiser builds the Scala 2
  * runtime-reflection universe — which cannot start on Scala 3.8+'s stdlib ("class Array does not
  * have a member apply"). Instead it is a `TransformingEncoder` over a `RowEncoder` of its fields,
  * and the codec calls its constructor directly: same schema, no reflection. The derivation is a
  * Hearth macro (`SparkEncodersMacro`), as eo-generics' are.
  */
object SparkEncoders:

  given AgnosticEncoder[Boolean] = PrimitiveBooleanEncoder
  given AgnosticEncoder[Int] = PrimitiveIntEncoder
  given AgnosticEncoder[Long] = PrimitiveLongEncoder
  given AgnosticEncoder[Double] = PrimitiveDoubleEncoder
  given AgnosticEncoder[String] = StringEncoder

  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[Option[A]] = OptionEncoder(e)

  /** Encoder for a top-level case class: one struct field per constructor parameter, named by the
    * parameter, nullable exactly when the field's own encoder is. Every field type needs an
    * `AgnosticEncoder` in scope.
    */
  inline def derived[T]: AgnosticEncoder[T] = ${ SparkEncodersMacro.derivedImpl[T] }

  /** The case-class encoder the macro emits: a `TransformingEncoder` over a `RowEncoder` of the
    * fields, through `encode` (field reads) and `decode` (a constructor call). It builds Spark's
    * `EncoderField`s here rather than in the expansion: their Scala 2 existential
    * `AgnosticEncoder[_]` raises a compatibility warning wherever a macro constructs one.
    */
  @publicInBinary
  private[spark] def product[T](
      ct: ClassTag[T],
      fields: List[(String, AgnosticEncoder[?])],
      encode: T => Row,
      decode: Row => T,
  ): AgnosticEncoder[T] =
    val row = RowEncoder(fields.map { (name, e) =>
      EncoderField(name, e, e.nullable, Metadata.empty, None, None)
    })
    TransformingEncoder[T, Row](ct, row, () => ProductCodec(encode, decode), nullable = true)

  /** One level of a case class ↔ its `Row` of field values. Nested case classes are already decoded
    * by their own field encoders, so the row holds the final field values. A NULL struct travels
    * through the codec as `null` both ways (Spark's NULL, not a missed optic focus).
    */
  final private class ProductCodec[T](enc: T => Row, dec: Row => T) extends Codec[T, Row]:

    def encode(in: T): Row =
      if (in: T | Null) == null then null.asInstanceOf[Row] else enc(in)

    def decode(out: Row): T =
      if (out: Row | Null) == null then null.asInstanceOf[T] else dec(out)
