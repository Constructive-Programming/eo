package dev.constructive.eo
package kyo
package schema

import _root_.kyo.*
import _root_.kyo.Structure.Value

import data.PSVec
import optics.{Optional, PickMendPrism, Plated, Prism, Traversal}

/** Optics over kyo-schema's untyped [[Structure.Value]] tree — the format-agnostic analog of the
  * circe module's `Json` surface. `Structure.Value` is what any `Schema[A]` encodes to before a
  * codec turns it into bytes, so one navigation kit covers every wire format.
  *
  *   - '''Constructor prisms''' ([[StructureValues.str]], [[StructureValues.record]], …): one
  *     `Option`-shaped prism per `Value` case, the `jsonString` / `jsonObject` analogs.
  *   - '''Navigation''' ([[StructureValues.field]], [[StructureValues.at]],
  *     [[StructureValues.key]], [[StructureValues.each]]) — sibling-preserving `Optional`s /
  *     `Traversal` into records, sequences, and maps, plus the one circe can't have:
  *     [[StructureValues.variant]], sum navigation on the untyped side.
  *   - '''Whole-tree rewrites''' — [[StructureValues.platedValue]] makes `Value` a recursive
  *     self-traversal for `Plated.transform` / `rewrite` / `universe`.
  *   - '''Typed ↔ untyped''' — [[valuePrism(Schema)]], the third sibling of [[prism(Schema)]] /
  *     [[stringPrism(Schema)]]: a Prism between the untyped tree and `A` via the public
  *     `Structure.encode` / `Structure.decode` seam.
  *
  * The kit composes with the byte faces already here: RC6 ships `Schema[Structure.Value]`
  * (`Structure.valueSchema`), so `Structure.valueSchema.prism[Json] andThen
  * StructureValues.field("total")` edits one field inside encoded bytes with no typed value ever
  * materialised — under any codec.
  */
object StructureValues:

  /** Prism onto the `Str` case. */
  val str: PickMendPrism[Value, String, String] =
    Prism.optional({ case Value.Str(s) => Some(s); case _ => None }, Value.Str(_))

  /** Prism onto the `Bool` case. */
  val bool: PickMendPrism[Value, Boolean, Boolean] =
    Prism.optional({ case Value.Bool(b) => Some(b); case _ => None }, Value.Bool(_))

  /** Prism onto the `Integer` case (stored as `Long`). */
  val integer: PickMendPrism[Value, Long, Long] =
    Prism.optional({ case Value.Integer(i) => Some(i); case _ => None }, Value.Integer(_))

  /** Prism onto the `Decimal` case (stored as `Double`). */
  val decimal: PickMendPrism[Value, Double, Double] =
    Prism.optional({ case Value.Decimal(d) => Some(d); case _ => None }, Value.Decimal(_))

  /** Prism onto the `BigNum` case. */
  val bigNum: PickMendPrism[Value, BigDecimal, BigDecimal] =
    Prism.optional({ case Value.BigNum(n) => Some(n); case _ => None }, Value.BigNum(_))

  /** Prism onto the `Bytes` case. */
  val bytes: PickMendPrism[Value, Span[Byte], Span[Byte]] =
    Prism.optional({ case Value.Bytes(b) => Some(b); case _ => None }, Value.Bytes(_))

  /** Prism onto the `Instant` case. */
  val instant: PickMendPrism[Value, java.time.Instant, java.time.Instant] =
    Prism.optional({ case Value.Instant(i) => Some(i); case _ => None }, Value.Instant(_))

  /** Prism onto the `Duration` case. */
  val duration: PickMendPrism[Value, java.time.Duration, java.time.Duration] =
    Prism.optional({ case Value.Duration(d) => Some(d); case _ => None }, Value.Duration(_))

  /** Prism onto the `Null` case object (focus `Unit`). */
  val nul: PickMendPrism[Value, Unit, Unit] =
    Prism.optional({ case Value.Null => Some(()); case _ => None }, _ => Value.Null)

  /** Prism onto a `Sequence`'s elements. */
  val sequence: PickMendPrism[Value, Chunk[Value], Chunk[Value]] =
    Prism.optional({ case Value.Sequence(es) => Some(es); case _ => None }, Value.Sequence(_))

  /** Prism onto a `Record`'s ordered `(name, value)` fields. */
  val record: PickMendPrism[Value, Chunk[(String, Value)], Chunk[(String, Value)]] =
    Prism.optional({ case Value.Record(fs) => Some(fs); case _ => None }, Value.Record(_))

  /** Prism onto a `MapEntries`' ordered key-value pairs. */
  val mapEntries: PickMendPrism[Value, Chunk[(Value, Value)], Chunk[(Value, Value)]] =
    Prism.optional({ case Value.MapEntries(es) => Some(es); case _ => None }, Value.MapEntries(_))

  /** Optional into a `Record` field by name — the other fields (and their order) survive writes.
    * Misses (not a record, or no such field) pass through writes untouched. Schema-produced records
    * carry unique names; if duplicates ever appear, reads take the first and writes update all.
    */
  def field(name: String): Optional[Value, Value, Value, Value] =
    Optional[Value, Value, Value, Value](
      {
        case r @ Value.Record(fields) =>
          fields.collectFirst { case (n, v) if n == name => v }.toRight(r)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case Value.Record(fields) if fields.exists(_._1 == name) =>
            Value.Record(fields.map((n, v) => if n == name then (n, b) else (n, v)))
          case other => other,
    )

  /** Optional into a `Sequence` element by index — siblings survive writes, out-of-range or
    * non-sequence misses pass through.
    */
  def at(i: Int): Optional[Value, Value, Value, Value] =
    Optional[Value, Value, Value, Value](
      {
        case s @ Value.Sequence(es) => if es.isDefinedAt(i) then Right(es(i)) else Left(s)
        case other                  => Left(other)
      },
      (s, b) =>
        s match
          case Value.Sequence(es) if es.isDefinedAt(i) => Value.Sequence(es.updated(i, b))
          case other                                   => other,
    )

  /** Optional into a `MapEntries` value by key (compared with `Value`'s structural equality) —
    * reads take the first matching entry, writes update every matching entry, misses pass through.
    */
  def key(k: Value): Optional[Value, Value, Value, Value] =
    Optional[Value, Value, Value, Value](
      {
        case m @ Value.MapEntries(entries) =>
          entries.collectFirst { case (ek, ev) if ek == k => ev }.toRight(m)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case Value.MapEntries(entries) if entries.exists(_._1 == k) =>
            Value.MapEntries(entries.map((ek, ev) => if ek == k then (ek, b) else (ek, ev)))
          case other => other,
    )

  /** Traversal over every element of a `Sequence` — zero foci (write pass-through) on anything
    * else. Built on [[optics.Traversal.selfChildren]], whose variable-arity `PSVec` view is exactly
    * this shape: a sequence's elements, or none.
    */
  val each: Traversal[Value, Value, Value, Value] =
    Traversal.selfChildren[Value](
      { case Value.Sequence(es) => PSVec.from(es); case _ => emptyValueVec },
      (v, children) =>
        v match
          case Value.Sequence(_) => Value.Sequence(Chunk.from(children.toList))
          case other             => other,
    )

  /** Optional through a sum variant by name — the sum-navigation optic circe has no analog for.
    * Focuses the payload when the active variant matches `name`; other variants (and non-variants)
    * pass through writes untouched.
    *
    * Matches both spellings of a variant on the wire: the declared `VariantCase(name, payload)`
    * shape, and the single-field wrapper record `Record(Chunk((name, payload)))` that RC6's
    * `Structure.encode` actually emits for sealed traits / enums (kyo's own `Path.Variant` matches
    * only the former — an upstream inconsistency this optic papers over). Writes rebuild whichever
    * spelling was read.
    */
  def variant(name: String): Optional[Value, Value, Value, Value] =
    Optional[Value, Value, Value, Value](
      {
        case v @ Value.VariantCase(n, payload) => if n == name then Right(payload) else Left(v)
        case Value.Record(fields) if fields.size == 1 && fields(0)._1 == name =>
          Right(fields(0)._2)
        case other => Left(other)
      },
      (s, b) =>
        s match
          case Value.VariantCase(n, _) if n == name => Value.VariantCase(n, b)
          case Value.Record(fields) if fields.size == 1 && fields(0)._1 == name =>
            Value.Record(Chunk(name -> b))
          case other => other,
    )

  /** [[optics.Plated]] over the `Value` tree itself — the immediate children of a node are a
    * record's field values, a variant's payload, a sequence's elements, or a map's keys and values
    * interleaved (scalars have none); rebuilding keeps names, the variant tag, and key-value
    * pairing. `Plated.transform` / `rewrite` / `universe` then walk whole documents: redact every
    * string at any depth, round every decimal, collect every value under any schema.
    */
  given platedValue: Plated[Value] =
    Plated.fromChildrenVec(childValues, rebuildFromChildren)

  private val emptyValueVec: PSVec[Value] = PSVec.empty[Value]

  private def childValues(v: Value): PSVec[Value] = v match
    case Value.Record(fields)      => PSVec.from(fields.map(_._2))
    case Value.VariantCase(_, pv)  => PSVec.singleton(pv)
    case Value.Sequence(es)        => PSVec.from(es)
    case Value.MapEntries(entries) => PSVec.from(entries.flatMap((k, ev) => Seq(k, ev)))
    case _                         => emptyValueVec

  private def rebuildFromChildren(v: Value, children: PSVec[Value]): Value = v match
    case Value.Record(fields) =>
      Value.Record(Chunk.from(fields.indices.map(i => (fields(i)._1, children(i)))))
    case Value.VariantCase(n, _)   => Value.VariantCase(n, children(0))
    case Value.Sequence(_)         => Value.Sequence(Chunk.from(children.toList))
    case Value.MapEntries(entries) =>
      Value.MapEntries(Chunk.from(entries.indices.map(i => (children(2 * i), children(2 * i + 1)))))
    case other => other

end StructureValues

extension [A](self: Schema[A])

  /** Prism between the untyped `Structure.Value` tree and `A` — the typed ↔ untyped face beside
    * [[prism(Schema)]] (bytes) and [[stringPrism(Schema)]] (String), built on the public
    * `Structure.encode` / `Structure.decode` seam. Same laws and caveats as those byte faces:
    * decode-then-encode normalises the tree (defaults filled, one canonical shape), a failed decode
    * is a miss, and misses pass through writes untouched.
    *
    * Compose inward for the codecPrism idiom: `StructureValues.field("items") andThen
    * StructureValues.each andThen Schema.derived[Item].valuePrism` decodes only the leaves it
    * touches.
    */
  def valuePrism(using Frame): optics.MendTearPrism[Value, Value, A, A] =
    given Schema[A] = self
    Prism[Value, A](
      v => Structure.decode[A](v).foldError(Right(_), _ => Left(v)),
      a => Structure.encode(a),
    )
