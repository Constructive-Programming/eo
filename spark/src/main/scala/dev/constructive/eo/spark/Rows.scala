package dev.constructive.eo
package spark

import scala.reflect.ClassTag

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.StructType

import data.PSVec
import optics.{Optional, Plated}

/** Optics over untyped Spark [[org.apache.spark.sql.Row]]s — what `collect()` on a `DataFrame`
  * returns. Plain values, no session.
  *
  *   - '''Navigation''' ([[field]] by name, [[at]] by position) — typed `Optional`s: a hit needs
  *     the field to exist AND hold a non-NULL value of type `A` (checked by `ClassTag`, boxed
  *     primitives included). Misses pass writes through untouched; writes keep the row's schema.
  *   - '''Whole-tree rewrites''' — [[platedRow]], whose children are the struct-valued fields.
  *
  * `Row` equality compares VALUES only (not the schema), the same as Spark's own.
  */
object Rows:

  /** The value of field `name` as an `A`. Needs a schema-bearing row (`GenericRowWithSchema`, what
    * a `DataFrame` collects to); a row without a schema is a miss.
    */
  def field[A](name: String)(using ClassTag[A]): Optional[Row, Row, A, A] =
    focus[A](r => schemaOf(r).map(_.fieldNames.indexOf(name)).filter(_ >= 0))

  /** The value at position `i` as an `A`. */
  def at[A](i: Int)(using ClassTag[A]): Optional[Row, Row, A, A] =
    focus[A](r => Option.when(i >= 0 && i < r.length)(i))

  /** [[optics.Plated]] over nested rows — children are the struct-valued (`Row`) fields, in field
    * order; rebuilding keeps every other value and the schema.
    */
  given platedRow: Plated[Row] = Plated.fromChildrenVec(children, rebuild)

  private def focus[A](index: Row => Option[Int])(using
      ct: ClassTag[A]
  ): Optional[Row, Row, A, A] =
    Optional[Row, Row, A, A](
      r => index(r).flatMap(i => ct.unapply(r.get(i))).toRight(r),
      (r, a) => index(r).filter(i => ct.unapply(r.get(i)).isDefined).fold(r)(updated(r, _, a)),
    )

  private def schemaOf(r: Row): Option[StructType] = (r.schema: StructType | Null) match
    case null          => None
    case s: StructType => Some(s)

  private def updated(r: Row, i: Int, v: Any): Row =
    rowLike(r, r.toSeq.updated(i, v))

  private def rowLike(r: Row, values: Seq[Any]): Row = schemaOf(r) match
    case Some(s) => new GenericRowWithSchema(values.toArray, s)
    case None    => Row.fromSeq(values)

  private def nestedIndices(r: Row): IndexedSeq[Int] =
    (0 until r.length).filter(i => r.get(i).isInstanceOf[Row])

  private def children(r: Row): PSVec[Row] =
    PSVec.from(nestedIndices(r).map(i => r.getAs[Row](i)))

  private def rebuild(r: Row, cs: PSVec[Row]): Row =
    val idx = nestedIndices(r)
    if idx.isEmpty then r
    else rowLike(r, idx.indices.foldLeft(r.toSeq)((vs, k) => vs.updated(idx(k), cs(k))))

end Rows
