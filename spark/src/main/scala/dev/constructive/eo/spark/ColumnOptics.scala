package dev.constructive.eo
package spark

import org.apache.spark.sql.functions
import org.apache.spark.sql.functions.when
import org.apache.spark.sql.types.StructType

import optics.{GetReplaceLens, Getter, Lens, Modify, Optional}

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

  // ---- Containers: element-wise writes are Modify, reads are aggregates ------------------------
  //
  // A symbolic array cannot hand out its elements while the query is being BUILT (its length is a
  // per-row fact), so `MultiFocus`'s Traversal does not apply: element-wise writes are a core
  // `Modify` compiled to Spark's higher-order `transform` / `transform_values`, and element-wise
  // reads are `Getter`s into one aggregate. An effectful `modifyA` cannot run inside a Spark
  // expression at all.

  /** Every element of an array column: `modify(f)` is `transform(array, f)`. NULL arrays stay NULL.
    */
  def each[C[x] <: Iterable[x], A]: Modify[Col[C[A]], Col[C[A]], Col[A], Col[A]] =
    Modify[Col[C[A]], Col[C[A]], Col[A], Col[A]](f =>
      s => Col(functions.transform(s.column, x => f(Col(x)).column))
    )

  /** Every value of a map column: `modify(f)` is `transform_values(map, (_, v) => f(v))`; keys and
    * entry order survive.
    */
  def values[K, V]: Modify[Col[Map[K, V]], Col[Map[K, V]], Col[V], Col[V]] =
    Modify[Col[Map[K, V]], Col[Map[K, V]], Col[V], Col[V]](f =>
      s => Col(functions.transform_values(s.column, (_, v) => f(Col(v)).column))
    )

  /** Fold an array column to one value per row: `aggregate(array, zero, combine)` — the symbolic
    * `foldLeft`. Pass a lawful monoid's `empty` / `combine` (e.g. `lit(0)` / `+`) for a `foldMap`.
    * NULL arrays aggregate to NULL.
    */
  def aggregate[C[x] <: Iterable[x], A, M](zero: Col[M])(
      combine: (Col[M], Col[A]) => Col[M]
  ): Getter[Col[C[A]], Col[M]] =
    Getter[Col[C[A]], Col[M]](s =>
      Col(functions.aggregate(s.column, zero.column, (acc, x) => combine(Col(acc), Col(x)).column))
    )

  /** An array or map column's element count (NULL for a NULL container). */
  def size[C]: Getter[Col[C], Col[Int]] =
    Getter[Col[C], Col[Int]](s => Col(functions.size(s.column)))

  /** Whether some element satisfies `p`. */
  def exists[C[x] <: Iterable[x], A](p: Col[A] => Col[Boolean]): Getter[Col[C[A]], Col[Boolean]] =
    Getter[Col[C[A]], Col[Boolean]](s => Col(functions.exists(s.column, x => p(Col(x)).column)))

  /** Whether every element satisfies `p`. */
  def forall[C[x] <: Iterable[x], A](p: Col[A] => Col[Boolean]): Getter[Col[C[A]], Col[Boolean]] =
    Getter[Col[C[A]], Col[Boolean]](s => Col(functions.forall(s.column, x => p(Col(x)).column)))
