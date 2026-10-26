package dev.constructive.eo
package spark

import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.functions.{explode, lit, struct}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset, Encoder}

/** Lifting column optics onto a whole `Dataset`: a row IS a struct column, so any optic over
  * `Col[S]` edits every row as ONE projection — `select(edit(struct(columns*)) AS r).select(r.*)`.
  * Catalyst removes the wrapping struct (the specs pin the optimized plan), so this is the
  * projection one would write by hand, and it never leaves Spark's columnar execution.
  *
  * The `…All` operations CONSUME through a capability (`CanGet` / `CanModify` on `Col[S]`), per the
  * library's construct-via-optic / consume-via-capability rule: give the optic as a given, or pass
  * it explicitly with `(using myOptic)`.
  */
object DatasetOptics:

  private val RowAlias = "__eo_row"

  extension [S](ds: Dataset[S])

    /** The whole row as one struct column, bound to THIS Dataset's attributes (`ds.col`), so it
      * stays unambiguous in a join with a Dataset that has the same column names.
      */
    def rowCol: Col[S] =
      Col(struct(ds.columns.toSeq.map(n => ds.col(s"`${n.replace("`", "``")}`").as(n))*))

    /** Rebuild every row through `edit` in one projection; untyped result. */
    def editRows(edit: Col[S] => Col[S]): DataFrame =
      ds.select(edit(rowCol).column.as(RowAlias)).select(s"$RowAlias.*")

    /** `modify(f)` on every row through the given optic. */
    def modifyAll[A](
        f: Col[A] => Col[A]
    )(using m: CanModify[Col[S], Col[A]], e: Encoder[S]): Dataset[S] =
      editRows(m.modify(f)).as[S](using e)

    /** `replace(a)` on every row through the given optic. */
    def replaceAll[A](a: Col[A])(using m: CanModify[Col[S], Col[A]], e: Encoder[S]): Dataset[S] =
      editRows(m.replace(a)).as[S](using e)

    /** The focus of every row, as its own Dataset. */
    def getAll[A](using g: CanGet[Col[S], Col[A]], e: Encoder[A]): Dataset[A] =
      ds.select(g.get(rowCol).column).as[A](using e)

    /** Aggregate the focus over the whole Dataset with an aggregate function (`sum`, `max`,
      * `collect_list`, …) — the Dataset-level `foldMap`. Runs a Spark action.
      */
    def foldAll[A, B](agg: Col[A] => Col[B])(using g: CanGet[Col[S], Col[A]], e: Encoder[B]): B =
      ds.select(agg(g.get(rowCol)).column).as[B](using e).head()

    // ---- Consumers: Dataset operations that are not optics, keyed by capabilities ----------

    /** The rows where a partial focus HITS (a non-NULL focus); a plan-time miss keeps none. */
    def filterHits[A](using o: CanGetOption[Col[S], Col[A]]): Dataset[S] =
      o.getOption(rowCol).fold(ds.filter(lit(false)))(c => ds.filter(c.column.isNotNull))

    /** Inner equi-join on keys read through capabilities — `l.get(left) === r.get(right)`. The
      * pairs decode with eo's tuple encoder (Spark's own needs Scala 2 reflection).
      */
    def joinOn[R, K](right: Dataset[R])(using
        l: CanGet[Col[S], Col[K]],
        r: CanGet[Col[R], Col[K]],
        es: AgnosticEncoder[S],
        er: AgnosticEncoder[R],
    ): Dataset[(S, R)] =
      val (left, rightRow) = (rowCol, right.rowCol)
      ds.join(right, l.get(left).column === r.get(rightRow).column, "inner")
        .select(left.column.as("_1"), rightRow.column.as("_2"))
        .as[(S, R)](using SparkEncoders.tuple2(es, er))

    /** Group by a key focus and aggregate a value focus (`sum`, `count`, `collect_list`, …). */
    def groupAggregate[K, A, B](agg: Col[A] => Col[B])(using
        k: CanGet[Col[S], Col[K]],
        a: CanGet[Col[S], Col[A]],
        ek: AgnosticEncoder[K],
        eb: AgnosticEncoder[B],
    ): Dataset[(K, B)] =
      val row = rowCol
      ds.groupBy(k.get(row).column.as("_1"))
        .agg(agg(a.get(row)).column.as("_2"))
        .as[(K, B)](using SparkEncoders.tuple2(ek, eb))

    /** Every element of an array focus, one per output row — the Dataset-level `Fold` (read-only:
      * putting the elements back would need a shuffle).
      */
    def explodeAll[C[x] <: Iterable[x], A](using
        g: CanGet[Col[S], Col[C[A]]],
        e: Encoder[A]
    ): Dataset[A] =
      val exploded = ds.select(explode(g.get(rowCol).column).as(RowAlias))
      // A struct element is one column holding the fields; a case-class encoder wants them flat.
      val flat = exploded.schema(RowAlias).dataType match
        case _: StructType => exploded.select(s"$RowAlias.*")
        case _             => exploded
      flat.as[A](using e)

    // ---- Eager bridge -----------------------------------------------------------------------

    /** `modify(f)` through an EAGER optic on every decoded row — a typed `Dataset.map`. The escape
      * hatch for edits a column expression cannot express (arbitrary Scala, `modifyA`-shaped
      * logic): it leaves Catalyst, decodes and re-encodes every row, and the closure (optic
      * included: optics are `Serializable`) runs on the server, so the session needs the Scala 3
      * runtime and the application's classes ([[ScalaRuntime.addTo]]). Prefer the symbolic
      * operations above whenever they can say it.
      *
      * Define `f` (and the optic) somewhere the SERVER can load: a lambda is serialized as a
      * reference to the class whose method implements it, so one written inside, say, a test class
      * drags that class (and its framework) onto the server.
      */
    def mapThrough[A](f: A => A)(using m: CanModify[S, A], e: Encoder[S]): Dataset[S] =
      val g = m.modify(f)
      ds.map(s => g(s))(using e)
