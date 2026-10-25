package dev.constructive.eo
package spark

import org.apache.spark.sql.functions.{col, struct}
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

    /** The whole row as one struct column. */
    def rowCol: Col[S] = Col(struct(ds.columns.toSeq.map(n => col(s"`${n.replace("`", "``")}`"))*))

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
