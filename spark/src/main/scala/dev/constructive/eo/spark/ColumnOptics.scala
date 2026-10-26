package dev.constructive.eo
package spark

import org.apache.spark.sql.functions.when
import org.apache.spark.sql.types.StructType

import optics.{GetReplaceLens, Lens, Optional}

/** Optics over symbolic Spark columns, built from core constructors only.
  *
  * '''Partiality lives in the complement, not in a new carrier.''' A symbolic `to` runs when the
  * query is BUILT, before any row exists, so a partial focus has two kinds of miss and core
  * `Optional` (carrier `Affine`, complement `X = (T, S)`) holds both:
  *
  *   - a plan-time miss (the schema has no such field) is `Left(source)` — `getOption` is `None`
  *     and `modify` hands the column back unchanged;
  *   - a per-row miss stays a plan-time HIT whose focus is NULL on the missing rows; `reverseGet`
  *     reads the source back out of the complement and guards the rebuild with
  *     `when(present, rebuild).otherwise(source)`, so missing rows come out exactly as they went in
  *     — the Optional laws, read per row.
  */
object ColumnOptics:

  /** Struct-field lens: `get` is `getField`, `replace` is `withField`.
    *
    * Lawful on NON-NULL struct values only: `withField` on a NULL struct yields NULL, so put-get
    * fails there. Use [[nullableField]] for a nullable struct position.
    */
  def field[S, A](name: String): GetReplaceLens[Col[S], Col[S], Col[A], Col[A]] =
    Lens[Col[S], Col[A]](
      s => Col(s.column.getField(name)),
      (s, a) => Col(s.column.withField(name, a.column)),
    )

  /** A per-row partial focus: `focus` is read unconditionally (NULL where absent) and the rebuild
    * only fires on rows where `present` holds.
    */
  def guarded[S, A](
      present: Col[S] => Col[Boolean],
      focus: Col[S] => Col[A],
      rebuild: (Col[S], Col[A]) => Col[S],
  ): Optional[Col[S], Col[S], Col[A], Col[A]] =
    Optional[Col[S], Col[S], Col[A], Col[A]](
      s => Right(focus(s)),
      (s, a) => Col(when(present(s).column, rebuild(s, a).column).otherwise(s.column)),
    )

  /** Field of a possibly-NULL struct: rows whose struct is NULL pass through untouched. */
  def nullableField[S, A](name: String): Optional[Col[S], Col[S], Col[A], Col[A]] =
    val l = field[S, A](name)
    guarded[S, A](s => Col(s.column.isNotNull), l.get, (s, a) => l.replace(a)(s))

  /** Schema-checked field: a plan-time miss when `schema` has no field `name`. */
  def fieldOf[S, A](schema: StructType, name: String): Optional[Col[S], Col[S], Col[A], Col[A]] =
    val l = field[S, A](name)
    Optional[Col[S], Col[S], Col[A], Col[A]](
      s => Either.cond(schema.fieldNames.contains(name), l.get(s), s),
      (s, a) => l.replace(a)(s),
    )
