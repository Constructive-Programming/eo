package dev.constructive.eo
package spark

import org.apache.spark.sql.types.{ArrayType, DataType, MapType, Metadata, StructField, StructType}

import data.{ModifyF, PSVec}
import generics.{lens, plate, prism}
import optics.Optic.*
import optics.{Optic, Optional, PickMendPrism, Plated, Prism}

/** Optics over Spark SCHEMAS — the mutually recursive `DataType` / `StructField` tree (`StructType`
  * / `ArrayType` / `MapType` nodes, `StructField` entries, atomic leaves). Plain values, no
  * session: the kit for schema evolution, validation, and inspection
  * (`field("address").andThen(nullable("zip"))`, `everyField.andThen(fieldNullable).replace(true)`,
  * a whole-schema `Plated.transform` widening every `IntegerType` to `LongType` and normalising
  * field names in one pass, or an effectful `Plated.transformM` validating types, …).
  *
  *   - '''Constructor prisms''' ([[struct]], [[array]], [[map]], plus [[dataType]] and
  *     [[structField]] on [[Node]]) — one per node kind.
  *   - '''`StructField` lenses''' ([[fieldName]], [[fieldType]], [[fieldNullable]],
  *     [[fieldMetadata]]) — derived by `eo-generics` for positional / whole-tree rewrites.
  *   - '''Navigation''' ([[field]], [[nullable]], [[metadata]], [[element]], [[mapKey]],
  *     [[mapValue]]) — sibling-preserving `Optional`s; misses (wrong node kind, no such field) pass
  *     writes through untouched. By-name navigation never focuses the field's name, so no write can
  *     rename a field out from under the optic that found it (positional rewrites go through
  *     [[platedNode]] / [[everyField]]).
  *   - '''Whole-tree rewrites''' — [[platedDataType]] (`DataType`-only) and [[platedNode]]
  *     (Multiplate over `DataType | StructField`, derived by `generics.plate`), plus [[everyField]]
  *     (`Plated.everywhereIn[Node, DataType].andThen(structField)`) to rewrite every `StructField`
  *     at every depth of a `DataType`.
  *
  * (Not `DataTypes`: that name is Spark's own Java factory in `org.apache.spark.sql.types`.)
  *
  * `StructType` is an array of fields, so duplicate names are representable; [[field]],
  * [[nullable]], and [[metadata]] read AND write the '''first''' match (what keeps them lawful
  * Optionals there).
  */
object Schemas:

  /** A node of the mutually recursive Spark schema tree — either a `DataType` or a `StructField`.
    */
  type Node = DataType | StructField

  /** Prism onto the `DataType` branch of [[Node]]. */
  val dataType: Optic[Node, Node, DataType, DataType, Either] =
    prism[Node, DataType]

  /** Prism onto the `StructField` branch of [[Node]]. */
  val structField: Optic[Node, Node, StructField, StructField, Either] =
    prism[Node, StructField]

  /** Lens onto a `StructField`'s `name`. */
  val fieldName = lens[StructField](_.name)

  /** Lens onto a `StructField`'s `dataType`. */
  val fieldType = lens[StructField](_.dataType)

  /** Lens onto a `StructField`'s `nullable` flag. */
  val fieldNullable = lens[StructField](_.nullable)

  /** Lens onto a `StructField`'s `metadata`. */
  val fieldMetadata = lens[StructField](_.metadata)

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
    atNamedField(name, fieldType)

  /** The nullability of a struct's field `name` (first match). */
  def nullable(name: String): Optional[DataType, DataType, Boolean, Boolean] =
    atNamedField(name, fieldNullable)

  /** The metadata of a struct's field `name` (first match). */
  def metadata(name: String): Optional[DataType, DataType, Metadata, Metadata] =
    atNamedField(name, fieldMetadata)

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

  /** Multiplate over the mutually recursive `DataType | StructField` tree, derived by
    * `generics.plate`: `StructType` steps into its `Array[StructField]`, each `StructField` into
    * its `dataType`, `ArrayType` into `elementType`, `MapType` into `keyType` and `valueType`, and
    * atomic `DataType`s are leaves.
    */
  given platedNode: Plated[Node] =
    plate[StructType | ArrayType | MapType | DataType | StructField]

  /** Every `StructField` at every nesting depth of a `DataType` (including structs nested inside
    * `ArrayType` and `MapType`) — `Plated.everywhereIn[Node, DataType].andThen(structField)`.
    * Compose with [[fieldNullable]], [[fieldName]], [[fieldMetadata]], or [[fieldType]] to rewrite
    * all fields across a schema in one pass.
    */
  val everyField: Optic[DataType, DataType, StructField, StructField, ModifyF] =
    Plated.everywhereIn[Node, DataType].andThen(structField)

  /** [[optics.Plated]] over `DataType` alone — immediate children are a struct's field types (1 hop
    * across `StructField`), an array's element type, and a map's key and value types. Delegates the
    * `DataType`-child branches (`ArrayType`, `MapType`, atomic leaves) to the macro-derived
    * [[platedNode]] and steps through [[fieldType]] on `StructType`.
    */
  given platedDataType: Plated[DataType] = Plated.fromChildrenVec(children, rebuild)

  private def atNamedField[A](
      name: String,
      lens: optics.SimpleLens[StructField, A, ?],
  ): Optional[DataType, DataType, A, A] =
    Optional[DataType, DataType, A, A](
      {
        case s: StructType => s.fields.find(_.name == name).map(lens.get).toRight(s)
        case other         => Left(other)
      },
      (s, a) =>
        s match
          case st: StructType =>
            val i = st.fields.indexWhere(_.name == name)
            if i < 0 then st else StructType(st.fields.updated(i, lens.replace(a)(st.fields(i))))
          case other => other,
    )

  private def children(t: DataType): PSVec[DataType] = t match
    case s: StructType => PSVec.from(s.fields.iterator.map(_.dataType))
    case other         => platedNode.childrenVec(other).asInstanceOf[PSVec[DataType]]

  private def rebuild(t: DataType, cs: PSVec[DataType]): DataType = t match
    case s: StructType =>
      StructType(Array.tabulate(s.fields.length)(i => fieldType.replace(cs(i))(s.fields(i))))
    case other => platedNode.rebuild(other, cs).asInstanceOf[DataType]

end Schemas
