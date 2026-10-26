package dev.constructive.eo
package spark

import org.apache.spark.sql.types.{ArrayType, DataType, MapType, StructField, StructType}

import data.PSVec
import optics.{Optional, PickMendPrism, Plated, Prism}

/** Optics over Spark SCHEMAS — the `DataType` tree (`StructType` / `ArrayType` / `MapType` nodes,
  * atomic leaves). Plain values, no session: the kit for schema evolution and inspection
  * (`field("address").andThen(nullable("zip"))`, a whole-schema `Plated.transform` widening every
  * `IntegerType` to `LongType`, …).
  *
  *   - '''Constructor prisms''' ([[struct]], [[array]], [[map]]) — one per node kind.
  *   - '''Navigation''' ([[field]], [[nullable]], [[element]], [[mapKey]], [[mapValue]]) —
  *     sibling-preserving `Optional`s; misses (wrong node kind, no such field) pass writes through
  *     untouched. A field's NAME is never a focus, so no write can rename one out from under the
  *     optic that found it.
  *   - '''Whole-tree rewrites''' — [[platedDataType]].
  *
  * (Not `DataTypes`: that name is Spark's own Java factory in `org.apache.spark.sql.types`.)
  *
  * `StructType` is an array of fields, so duplicate names are representable; [[field]] and
  * [[nullable]] read AND write the '''first''' match (what keeps them lawful Optionals there).
  */
object Schemas:

  /** Prism onto a `StructType` node. */
  val struct: PickMendPrism[DataType, StructType, StructType] =
    Prism.optional({ case s: StructType => Some(s); case _ => None }, s => s)

  /** Prism onto an `ArrayType` node. */
  val array: PickMendPrism[DataType, ArrayType, ArrayType] =
    Prism.optional({ case a: ArrayType => Some(a); case _ => None }, a => a)

  /** Prism onto a `MapType` node. */
  val map: PickMendPrism[DataType, MapType, MapType] =
    Prism.optional({ case m: MapType => Some(m); case _ => None }, m => m)

  /** The type of a struct's field `name` (first match) — nullability and metadata survive writes.
    */
  def field(name: String): Optional[DataType, DataType, DataType, DataType] =
    structField(name)(_.dataType, (f, t) => f.copy(dataType = t))

  /** The nullability of a struct's field `name` (first match). */
  def nullable(name: String): Optional[DataType, DataType, Boolean, Boolean] =
    structField(name)(_.nullable, (f, n) => f.copy(nullable = n))

  /** An array's element type — `containsNull` survives writes. */
  val element: Optional[DataType, DataType, DataType, DataType] =
    Optional[DataType, DataType, DataType, DataType](
      { case a: ArrayType => Right(a.elementType); case other => Left(other) },
      (s, t) =>
        s match
          case a: ArrayType => a.copy(elementType = t)
          case other        => other,
    )

  /** A map's key type. */
  val mapKey: Optional[DataType, DataType, DataType, DataType] =
    Optional[DataType, DataType, DataType, DataType](
      { case m: MapType => Right(m.keyType); case other => Left(other) },
      (s, t) =>
        s match
          case m: MapType => m.copy(keyType = t)
          case other      => other,
    )

  /** A map's value type — `valueContainsNull` survives writes. */
  val mapValue: Optional[DataType, DataType, DataType, DataType] =
    Optional[DataType, DataType, DataType, DataType](
      { case m: MapType => Right(m.valueType); case other => Left(other) },
      (s, t) =>
        s match
          case m: MapType => m.copy(valueType = t)
          case other      => other,
    )

  /** [[optics.Plated]] over the schema tree — children are a struct's field types, an array's
    * element type, a map's key and value types (atomic types have none); rebuilding keeps field
    * names, nullability, metadata, and the `containsNull` flags.
    */
  given platedDataType: Plated[DataType] = Plated.fromChildrenVec(children, rebuild)

  private def structField[A](name: String)(
      read: StructField => A,
      write: (StructField, A) => StructField,
  ): Optional[DataType, DataType, A, A] =
    Optional[DataType, DataType, A, A](
      {
        case s: StructType => s.fields.find(_.name == name).map(read).toRight(s)
        case other         => Left(other)
      },
      (s, a) =>
        s match
          case st: StructType =>
            val i = st.fields.indexWhere(_.name == name)
            if i < 0 then st else StructType(st.fields.updated(i, write(st.fields(i), a)))
          case other => other,
    )

  private val noChildren: PSVec[DataType] = PSVec.empty[DataType]

  private def children(t: DataType): PSVec[DataType] = t match
    case s: StructType => PSVec.from(s.fields.toSeq.map(_.dataType))
    case a: ArrayType  => PSVec.singleton(a.elementType)
    case m: MapType    => PSVec.from(Seq(m.keyType, m.valueType))
    case _             => noChildren

  private def rebuild(t: DataType, cs: PSVec[DataType]): DataType = t match
    case s: StructType =>
      StructType(s.fields.indices.map(i => s.fields(i).copy(dataType = cs(i))).toArray)
    case a: ArrayType => a.copy(elementType = cs(0))
    case m: MapType   => m.copy(keyType = cs(0), valueType = cs(1))
    case other        => other

end Schemas
