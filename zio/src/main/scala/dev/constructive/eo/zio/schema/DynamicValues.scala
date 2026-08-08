package dev.constructive.eo
package zio
package schema

import scala.collection.immutable.ListMap

import _root_.zio.Chunk
import _root_.zio.schema.{DynamicValue, StandardType}

import data.PSVec
import optics.{Optional, PickMendPrism, Plated, Prism, Traversal}

/** Optics over zio-schema's untyped [[DynamicValue]] tree — the format-agnostic analog of the circe
  * module's `Json` surface, and the exact twin of the kyo module's `StructureValues`.
  * `DynamicValue` is what any `Schema[A]` encodes to before a codec turns it into bytes, so one
  * navigation kit covers every wire format zio-schema speaks.
  *
  *   - '''Constructor prisms''' ([[str]], [[record]], …): one `Option`-shaped prism per case, plus
  *     the generic [[primitive]] for the ~30 `StandardType`s not named here.
  *   - '''Navigation''' ([[field]], [[at]], [[key]], [[each]]) — sibling-preserving `Optional`s /
  *     `Traversal` into records, sequences, and dictionaries, plus [[variant]]: sum navigation on
  *     the untyped side, which zio-schema (unlike kyo RC6) gives an explicit `Enumeration` node.
  *   - '''Whole-tree rewrites''' — [[platedDynamicValue]] makes `DynamicValue` a recursive
  *     self-traversal for `Plated.transform` / `rewrite` / `universe`.
  *   - '''Typed ↔ untyped''' — [[dynamicPrism(Schema)]] (in [[SchemaOptics]]) composes inward:
  *     `field("items") andThen each andThen itemSchema.dynamicPrism` decodes only the leaves it
  *     touches.
  */
object DynamicValues:

  /** Prism onto a `Primitive` of the given `StandardType` — the generic face behind [[str]],
    * [[bool]], …. Matching is by `StandardType` equality (they are case objects), which pins the
    * payload type; the cast the match can't prove is exactly the one zio-schema's own decoder
    * performs.
    */
  def primitive[A](st: StandardType[A]): PickMendPrism[DynamicValue, A, A] =
    Prism.optional(
      {
        case DynamicValue.Primitive(v, pst) if pst == st => Some(v.asInstanceOf[A])
        case _                                           => None
      },
      a => DynamicValue.Primitive(a, st),
    )

  /** Prism onto a `String` primitive. */
  val str: PickMendPrism[DynamicValue, String, String] = primitive(StandardType.StringType)

  /** Prism onto a `Boolean` primitive. */
  val bool: PickMendPrism[DynamicValue, Boolean, Boolean] = primitive(StandardType.BoolType)

  /** Prism onto an `Int` primitive. */
  val int: PickMendPrism[DynamicValue, Int, Int] = primitive(StandardType.IntType)

  /** Prism onto a `Long` primitive. */
  val long: PickMendPrism[DynamicValue, Long, Long] = primitive(StandardType.LongType)

  /** Prism onto a `Double` primitive. */
  val double: PickMendPrism[DynamicValue, Double, Double] = primitive(StandardType.DoubleType)

  /** Prism onto a `Sequence`'s elements. */
  val sequence: PickMendPrism[DynamicValue, Chunk[DynamicValue], Chunk[DynamicValue]] =
    Prism.optional(
      { case DynamicValue.Sequence(es) => Some(es); case _ => None },
      DynamicValue.Sequence(_),
    )

  /** Prism onto a `Record`'s ordered name → value fields. Writes keep the record's `TypeId`;
    * `reverseGet` from bare fields uses `TypeId.Structural`.
    */
  val record: PickMendPrism[
    DynamicValue,
    ListMap[String, DynamicValue],
    ListMap[String, DynamicValue],
  ] =
    Prism.optional(
      { case DynamicValue.Record(_, vs) => Some(vs); case _ => None },
      vs => DynamicValue.Record(_root_.zio.schema.TypeId.Structural, vs),
    )

  /** Prism onto a `Dictionary`'s ordered key-value entries. */
  val dictionary: PickMendPrism[
    DynamicValue,
    Chunk[(DynamicValue, DynamicValue)],
    Chunk[(DynamicValue, DynamicValue)],
  ] =
    Prism.optional(
      { case DynamicValue.Dictionary(es) => Some(es); case _ => None },
      DynamicValue.Dictionary(_),
    )

  /** Optional into a `Record` field by name — the other fields (and their order, and the record's
    * `TypeId`) survive writes. Misses (not a record, or no such field) pass through writes
    * untouched.
    */
  def field(name: String): Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue] =
    Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue](
      {
        case r @ DynamicValue.Record(_, vs) => vs.get(name).toRight(r)
        case other                          => Left(other)
      },
      (s, b) =>
        s match
          case DynamicValue.Record(id, vs) if vs.contains(name) =>
            DynamicValue.Record(id, vs.map((n, v) => if n == name then (n, b) else (n, v)))
          case other => other,
    )

  /** Optional into a `Sequence` element by index — siblings survive writes, out-of-range or
    * non-sequence misses pass through.
    */
  def at(i: Int): Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue] =
    Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue](
      {
        case s @ DynamicValue.Sequence(es) => if es.isDefinedAt(i) then Right(es(i)) else Left(s)
        case other                         => Left(other)
      },
      (s, b) =>
        s match
          case DynamicValue.Sequence(es) if es.isDefinedAt(i) =>
            DynamicValue.Sequence(es.updated(i, b))
          case other => other,
    )

  /** Optional into a `Dictionary` value by key (structural `DynamicValue` equality) — reads take
    * the first matching entry, writes update every matching entry, misses pass through.
    */
  def key(k: DynamicValue): Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue] =
    Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue](
      {
        case m @ DynamicValue.Dictionary(entries) =>
          entries.collectFirst { case (ek, ev) if ek == k => ev }.toRight(m)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case DynamicValue.Dictionary(entries) if entries.exists(_._1 == k) =>
            DynamicValue.Dictionary(entries.map((ek, ev) => if ek == k then (ek, b) else (ek, ev)))
          case other => other,
    )

  /** Traversal over every element of a `Sequence` — zero foci (write pass-through) on anything
    * else.
    */
  val each: Traversal[DynamicValue, DynamicValue, DynamicValue, DynamicValue] =
    Traversal.selfChildren[DynamicValue](
      { case DynamicValue.Sequence(es) => PSVec.from(es); case _ => emptyVec },
      (v, children) =>
        v match
          case DynamicValue.Sequence(_) => DynamicValue.Sequence(Chunk.from(children.toList))
          case other                    => other,
    )

  /** Optional through a sum variant by case name — focuses the `Enumeration` payload when the
    * active case matches `name`; other cases (and non-variants) pass through writes untouched.
    * zio-schema gives sums a first-class node, so there is no wrapper-record spelling to paper over
    * (the kyo RC6 wart).
    */
  def variant(name: String): Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue] =
    Optional[DynamicValue, DynamicValue, DynamicValue, DynamicValue](
      {
        case v @ DynamicValue.Enumeration(_, (n, payload)) =>
          if n == name then Right(payload) else Left(v)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case DynamicValue.Enumeration(id, (n, _)) if n == name =>
            DynamicValue.Enumeration(id, (n, b))
          case other => other,
    )

  /** [[optics.Plated]] over the `DynamicValue` tree itself — children are a record's field values,
    * an enumeration's payload, a sequence's elements, a dictionary's keys and values interleaved, a
    * tuple's / both-value's two sides, or an option's / either's payload (scalars have none);
    * rebuilding keeps names, ids, the case tag, and key-value pairing. `Plated.transform` /
    * `rewrite` / `universe` then walk whole documents under any schema.
    */
  given platedDynamicValue: Plated[DynamicValue] =
    Plated.fromChildrenVec(children, rebuild)

  private val emptyVec: PSVec[DynamicValue] = PSVec.empty[DynamicValue]

  private def children(v: DynamicValue): PSVec[DynamicValue] = v match
    case DynamicValue.Record(_, vs)          => PSVec.from(vs.values.toSeq)
    case DynamicValue.Enumeration(_, (_, p)) => PSVec.singleton(p)
    case DynamicValue.Sequence(es)           => PSVec.from(es)
    case DynamicValue.Dictionary(entries)    => PSVec.from(entries.flatMap((k, ev) => Seq(k, ev)))
    case DynamicValue.Tuple(l, r)            => PSVec.from(Seq(l, r))
    case DynamicValue.BothValue(l, r)        => PSVec.from(Seq(l, r))
    case DynamicValue.SomeValue(p)           => PSVec.singleton(p)
    case DynamicValue.LeftValue(p)           => PSVec.singleton(p)
    case DynamicValue.RightValue(p)          => PSVec.singleton(p)
    case _                                   => emptyVec

  private def rebuild(v: DynamicValue, cs: PSVec[DynamicValue]): DynamicValue = v match
    case DynamicValue.Record(id, vs) =>
      val names = vs.keys.toIndexedSeq
      DynamicValue.Record(id, ListMap.from(names.indices.map(i => (names(i), cs(i)))))
    case DynamicValue.Enumeration(id, (n, _)) => DynamicValue.Enumeration(id, (n, cs(0)))
    case DynamicValue.Sequence(_)             => DynamicValue.Sequence(Chunk.from(cs.toList))
    case DynamicValue.Dictionary(entries)     =>
      DynamicValue.Dictionary(Chunk.from(entries.indices.map(i => (cs(2 * i), cs(2 * i + 1)))))
    case DynamicValue.Tuple(_, _)     => DynamicValue.Tuple(cs(0), cs(1))
    case DynamicValue.BothValue(_, _) => DynamicValue.BothValue(cs(0), cs(1))
    case DynamicValue.SomeValue(_)    => DynamicValue.SomeValue(cs(0))
    case DynamicValue.LeftValue(_)    => DynamicValue.LeftValue(cs(0))
    case DynamicValue.RightValue(_)   => DynamicValue.RightValue(cs(0))
    case other                        => other

end DynamicValues
