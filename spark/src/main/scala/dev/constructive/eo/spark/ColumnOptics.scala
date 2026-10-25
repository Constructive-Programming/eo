package dev.constructive.eo
package spark

import org.apache.spark.sql.functions
import org.apache.spark.sql.functions.{lit, when}
import org.apache.spark.sql.types.{DataType, StructType}
import org.apache.spark.unsafe.types.VariantVal

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

  // ---- Partial foci: guarded core Optionals (per-row miss = NULL focus, untouched rebuild) -----

  /** The value at key `k` of a map column. Rows whose map lacks `k` (or is NULL) are a miss and
    * pass writes through; a write edits that one entry in place (`transform_values`), so entry
    * order and every other entry survive. Reads use `try_element_at`, which is NULL-safe under ANSI
    * mode.
    */
  def key[K, V](k: Col[K]): Optional[Col[Map[K, V]], Col[Map[K, V]], Col[V], Col[V]] =
    guarded[Map[K, V], V](
      m => Col(functions.map_contains_key(m.column, k.column)),
      m => Col(functions.try_element_at(m.column, k.column)),
      (m, v) =>
        Col(
          functions
            .transform_values(m.column, (kk, vv) => when(kk === k.column, v.column).otherwise(vv))
        ),
    )

  /** [[optics.At]]-style access to key `k`: the focus is `Col[Option[V]]` (NULL = absent), so a
    * write can CREATE (`Some`, appended with `map_concat`), UPDATE (in place) or DELETE (`None`,
    * via `map_filter`) the entry. A miss only on a NULL map.
    *
    * Lawful for maps whose VALUES are never NULL: a present key holding NULL reads as absent, and
    * writing that read back deletes the entry (the same caveat as `Map[K, Option[V]]` flattening).
    */
  def atKey[K, V](
      k: Col[K]
  ): Optional[Col[Map[K, V]], Col[Map[K, V]], Col[Option[V]], Col[Option[V]]] =
    guarded[Map[K, V], Option[V]](
      m => Col(m.column.isNotNull),
      m => Col(functions.try_element_at(m.column, k.column)),
      (m, ov) =>
        Col(
          when(ov.column.isNull, functions.map_filter(m.column, (kk, _) => kk =!= k.column))
            .when(
              functions.map_contains_key(m.column, k.column),
              functions.transform_values(
                m.column,
                (kk, vv) => when(kk === k.column, ov.column).otherwise(vv)
              ),
            )
            .otherwise(functions.map_concat(m.column, functions.map(k.column, ov.column)))
        ),
    )

  /** Element `i` (0-based) of an array column. Rows whose array is shorter (or NULL) are a miss and
    * pass writes through; a write replaces that one position. Fails fast on a negative index.
    */
  def index[C[x] <: Iterable[x], A](i: Int): Optional[Col[C[A]], Col[C[A]], Col[A], Col[A]] =
    require(i >= 0, s"array index must be non-negative, got $i")
    guarded[C[A], A](
      a => Col(functions.size(a.column) > i),
      a => Col(functions.get(a.column, lit(i))),
      (a, x) => Col(functions.transform(a.column, (e, j) => when(j === i, x.column).otherwise(e))),
    )

  /** A column read AS another type: the focus is `try_cast(to)`, so rows that do not convert are a
    * miss and pass writes through; a write casts back to `back` (the source's own type).
    *
    * Lawful only on CANONICAL inputs — those `cast(cast(s, to), back) == s` holds for. `"007"`
    * reads as `7` and `modify(identity)` writes it back as `"7"` (the same contract as
    * `JsoniterPrism`'s canonical re-encoding).
    */
  def cast[S, A](to: DataType, back: DataType): Optional[Col[S], Col[S], Col[A], Col[A]] =
    guarded[S, A](
      s => Col(s.column.try_cast(to).isNotNull),
      s => Col(s.column.try_cast(to)),
      (_, a) => Col(a.column.cast(back)),
    )

  /** A JSON-text column read as a typed struct: the focus is `from_json(schema)`; a write
    * re-encodes with `to_json`. Rows that are not a JSON OBJECT matching `schema` — malformed text,
    * `5`, `[1]`, `null`, a field of the wrong type — are a miss and pass writes through.
    *
    * Plain `from_json` cannot tell those apart from a hit: in its default PERMISSIVE mode it
    * decodes them to a struct of NULL fields (not to NULL), so `"not json"` would read as present
    * and a write would replace it with `{}`. The decode therefore carries a corrupt-record column
    * (`columnNameOfCorruptRecord`), which Spark fills exactly on those rows, and drops it from the
    * focus. An object with ABSENT fields (`{}`) is a hit whose fields are NULL.
    *
    * Lawful only on canonical JSON (`to_json`'s key order and number forms), as for [[cast]].
    */
  def json[A](schema: StructType): Optional[Col[String], Col[String], Col[A], Col[A]] =
    require(
      !schema.fieldNames.contains(CorruptField),
      s"schema must not use the reserved $CorruptField"
    )
    val withCorrupt = schema.add(CorruptField, org.apache.spark.sql.types.StringType)
    val options = Map("columnNameOfCorruptRecord" -> CorruptField)
    def decoded(s: Col[String]) = functions.from_json(s.column, withCorrupt, options)
    guarded[String, A](
      s => Col(decoded(s).isNotNull && decoded(s).getField(CorruptField).isNull),
      s => Col(decoded(s).dropFields(CorruptField)),
      (_, a) => Col(functions.to_json(a.column)),
    )

  private val CorruptField = "__eo_corrupt"

  /** A JSON-text column read as a `VARIANT` (`try_parse_json`; unparseable rows are a miss),
    * written back with `to_json`. Lawful only on canonical JSON. Compose with [[variantGet]] to
    * read into the tree, or edit the variant client-side with [[Variants]].
    */
  val parseJson: Optional[Col[String], Col[String], Col[VariantVal], Col[VariantVal]] =
    guarded[String, VariantVal](
      s => Col(functions.try_parse_json(s.column).isNotNull),
      s => Col(functions.try_parse_json(s.column)),
      (_, v) => Col(functions.to_json(v.column)),
    )

  /** READ-only typed access into a `VARIANT` column by path (`$.a.b[0]`): `try_variant_get`, NULL
    * on a missing path or a value that does not convert to `targetType` (a Spark SQL type name such
    * as `"int"`). Spark has no variant UPDATE function, so this is a `Getter`, not an `Optional`;
    * client-side edits go through [[Variants]].
    */
  def variantGet[A](path: String, targetType: String): Getter[Col[VariantVal], Col[A]] =
    Getter[Col[VariantVal], Col[A]](v => Col(functions.try_variant_get(v.column, path, targetType)))
