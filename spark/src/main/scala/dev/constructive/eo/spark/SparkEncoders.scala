package dev.constructive.eo
package spark

import scala.compiletime.{constValueTuple, erasedValue, summonInline}
import scala.deriving.Mirror
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
  * and the codec rebuilds the value through its `Mirror`: same schema, no reflection.
  */
object SparkEncoders:

  given AgnosticEncoder[Boolean] = PrimitiveBooleanEncoder
  given AgnosticEncoder[Int] = PrimitiveIntEncoder
  given AgnosticEncoder[Long] = PrimitiveLongEncoder
  given AgnosticEncoder[Double] = PrimitiveDoubleEncoder
  given AgnosticEncoder[String] = StringEncoder

  given [A](using e: AgnosticEncoder[A]): AgnosticEncoder[Option[A]] = OptionEncoder(e)

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
