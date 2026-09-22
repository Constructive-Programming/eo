package dev.constructive.eo.avro.vulcan

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

import _root_.vulcan.Codec as VCodec
import dev.constructive.eo.avro.{AvroCodec, AvroWalk}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

/** A compile-time-derived whole-record builder: `A ⇒ GenericData.Record`, leaf by leaf, with no
  * codec composition on the hot path (issue #95).
  *
  * '''The use case.''' Building a fresh generic record from a typed value on a hot path — an ingest
  * side, a replay, a batch flush. The full vulcan `Codec[A].encode` pays its composition once per
  * FIELD, per LEVEL: a `FreeApplicative.analyze`, an `Either` + `Chain.one` per field, and a
  * `put(name, value)` hash probe — and a nested sub-record field redoes all of it inside
  * `Codec[Sub].encode`, which is what the filer measured as ~384–468 B/field on their real
  * ClickInfo. A hand-built `.put(pos, value)` builder avoids all of it but costs one
  * hand-maintained line per leaf — the exact complaint the filer opened the issue with.
  *
  * '''What a derived builder is.''' [[AvroVulcan.recordBuilder]] walks `A`'s case fields at COMPILE
  * time (the [[WholeRecordBuilder.RecordShape]] IR) and emits one runtime assembly call;
  * construction resolves every case field's schema slot by NAME (all-or-nothing, issue #105's
  * doctrine) and validates every arm against the schema it will write into — so `toRecord` itself
  * is nothing but positional puts: `new GenericData.Record(schema)`, then per field either the
  * value itself (a primitive leaf), a recursive sub-record build (a nested case class — the
  * recursion the filer's positional prototype lacked, which is what made nested shapes pay vulcan's
  * per-sub-record composition), `null` (a `None`), or the field type's own leaf codec (everything
  * the fast arms don't cover). Schema-only fields (computed/derived columns the case class doesn't
  * hold) stay at their in-record default.
  *
  * '''Allocation is the gate, and it is hand-built-equal.''' Per record: the `GenericData.Record`
  * values array plus one boxed value per primitive leaf — exactly what the hand-built builder
  * allocates; the plans and slots are construction-time. ns/op stays within a small multiple of the
  * hand-built form (one erasure-level dispatch per non-primitive leaf; the primitive bulk is a
  * tight positional loop) and far below the codec composition it replaces — the `benchmarks`
  * `ClickRecordBench` measures all of it side by side.
  *
  * '''Construction is total.''' Every way assembly can fail — a case field no schema column answers
  * for, two case fields claiming one column, an arm disagreeing with its schema field's shape, a
  * non-record schema — comes back as the [[Exception]] half of the `recordBuilder` / `derive`
  * result, naming the field and the record, BEFORE any record is built. `toRecord` itself is total
  * for values matching `A` (a codec-leaf arm that fails encode still throws, per `AvroCodec`'s
  * total-encode convention — that is a codec-definition bug, not a construction condition).
  *
  * '''The one behavioural difference from `Codec[A].encode`,''' stated because a wire-compat claim
  * without it would be false precision: a schema-only (computed/derived) column. The codec fills it
  * during encode; the builder leaves the slot at its in-record value (null on a fresh
  * `GenericData.Record`) — identical to the hand-built `.put` builder the issue benchmarked, and
  * round-trip-safe through the codec's own decode (which reconstructs `A` from the fields it
  * knows).
  *
  * @param schema
  *   the record schema the builder writes into — the codec's own schema object, so a record built
  *   by the builder and a record decoded by the codec share one identity.
  */
final class WholeRecordBuilder[A] private[avro] (
    val schema: Schema,
    level: WholeRecordBuilder.RecordLevel,
):

  /** Build the whole record from `a`. Total for values matching `A`; every case field's slot is
    * written, schema-only columns keep their in-record default.
    */
  def toRecord(a: A): GenericData.Record = level.build(a)

  /** The [[AvroCodec]] with THIS builder as its encode and the in-scope vulcan codec as its decode
    * over THIS builder's schema — the one-line replacement for the hand-written
    * `given AvroCodec[A] with { def encode(a) = buildRecord(a) … }` the issue's recommendation
    * asked for. Decode errors surface as `Left`; encode is total (see [[AvroVulcan]] for the error
    * mapping).
    */
  def asAvroCodec(using c: VCodec[A]): AvroCodec[A] =
    val v = c
    new AvroCodec[A]:
      val schema: Schema = WholeRecordBuilder.this.schema
      def encode(a: A): Any = toRecord(a)
      def decodeEither(any: Any): Either[Throwable, A] =
        v.decode(any, WholeRecordBuilder.this.schema).left.map(_.throwable)

object WholeRecordBuilder:

  // ---- The compile-time IR (macro-emitted, hand-authorable) -------------------
  //
  // The `RecordShape` for a case class is the WHOLE derivation the macro emits: case fields in
  // declaration order, each classified into one of five arms. It is deliberately a plain runtime
  // VALUE (not a macro-only tree) so the assembly below is ordinary testable Scala, and so a shape
  // the macro cannot classify can be authored by hand.

  /** One record level: `typeName` for failure messages, case fields in declaration order. */
  final case class RecordShape(typeName: String, fields: List[FieldShape])

  /** One case field: its name and how `toRecord` obtains the datum it puts. */
  final case class FieldShape(name: String, kind: Kind)

  sealed trait Kind

  /** Primitive leaf written directly: the runtime value IS the Avro datum (Boolean / Int / Long /
    * Float / Double / String). Validated against the schema field type at construction.
    */
  final case class DirectKind(schemaType: Schema.Type) extends Kind

  /** `Option[X]`: `None` puts null (vulcan's own `OptionCodec`), `Some(v)` puts through the inner
    * arm against the non-null union branch.
    */
  final case class OptionKind(inner: Kind) extends Kind

  /** Nested case class: a sub-record level built by the same rule against the field's own record
    * schema — the recursion that makes nested shapes cost leaf-by-leaf, not codec-per-sub-record.
    */
  final case class RecordKind(sub: RecordShape) extends Kind

  /** A self-recursive ancestor already on the derivation path (`depth` levels up, 0 = the level
    * being built): resolved to that level at construction, so recursive case classes terminate.
    */
  final case class SelfKind(depth: Int) extends Kind

  /** Everything else — enums, bytes, logical types, collections, sums, value classes — encoded by
    * the field type's own vulcan codec, summoned at the derivation site and held here.
    */
  final case class CodecKind(codec: VCodec[Any]) extends Kind

  // ---- Runtime assembly ------------------------------------------------------
  //
  // Walks the `RecordShape` against the codec's schema: slots by name (AvroWalk.recordSlots,
  // all-or-nothing), each arm validated against the schema it will write into, sub-levels built
  // recursively. Everything happens HERE — once at builder construction, TOTAL (failures come back
  // as the Exception half, never thrown) — so `toRecord` is pure positional puts.

  /** Assemble a [[WholeRecordBuilder]] for `A` from `schema` (the codec's) and the compile-time
    * `shape`. Returns the failure — naming the field and the record — instead of a builder, when a
    * case field names no schema column, two case fields claim one, or an arm disagrees with its
    * schema field's shape. Never throws.
    */
  def derive[A](
      schema: Schema,
      shape: RecordShape,
      who: String
  ): Exception | WholeRecordBuilder[A] =
    if schema.getType != Schema.Type.RECORD then
      IllegalArgumentException(
        s"$who: the codec's schema is a ${schema.getType}, not a record — the builder mirrors a case"
          + " class onto record fields; use the codec for non-record shapes."
      )
    else
      buildLevel(schema, shape, who, parent = null) match
        case e: Exception       => e
        case level: RecordLevel => new WholeRecordBuilder[A](schema, level)

  /** One record level of the builder: resolved slots plus the per-field plans. Built shell-first (a
    * `SelfKind` arm captures the level being built), then sealed — `buildLevel` seals only on
    * success, so `build` never sees the empty arrays.
    */
  final private[avro] class RecordLevel(
      val schema: Schema,
      val parent: RecordLevel | Null,
  ):
    private var directs: Array[DirectSlot] = Array.empty
    private var unusual: Array[UnusualSlot] = Array.empty

    private[avro] def seal(d: Array[DirectSlot], u: Array[UnusualSlot]): Unit =
      directs = d
      unusual = u

    /** Build the record: the primitive bulk first (no dispatch — a positional put loop), then the
      * option / sub-record / codec arms. Positions are already resolved; every put is positional.
      */
    def build(a: Any): GenericData.Record =
      val record = new GenericData.Record(schema)
      val product = a.asInstanceOf[Product]
      putDirects(product, record, 0)
      putUnusual(product, record, 0)
      record

    @tailrec private def putDirects(product: Product, r: GenericData.Record, i: Int): Unit =
      if i < directs.length then
        val d = directs(i)
        r.put(d.slot, product.productElement(d.decl))
        putDirects(product, r, i + 1)

    @tailrec private def putUnusual(product: Product, r: GenericData.Record, i: Int): Unit =
      if i < unusual.length then
        val u = unusual(i)
        u.plan.put(r, product.productElement(u.decl))
        putUnusual(product, r, i + 1)

  final private[avro] case class DirectSlot(slot: Int, decl: Int)
  final private[avro] case class UnusualSlot(decl: Int, plan: FieldPlan)

  /** One non-primitive arm of a record level: how `toRecord` turns the field's value into the datum
    * it puts at `slot`. `put` receives the value as erased `Any` (the case field's value, already
    * boxed by `productElement`), so a level dispatches without per-type closures.
    */
  sealed private[avro] trait FieldPlan:
    def slot: Int
    def put(r: GenericData.Record, value: Any): Unit

  final private[avro] case class DirectPlan(slot: Int) extends FieldPlan:
    def put(r: GenericData.Record, value: Any): Unit = r.put(slot, value)

  /** `None` → null (vulcan's `OptionCodec`), `Some(v)` → the inner arm. The option field owns ONE
    * schema slot; both branches put there.
    */
  final private[avro] case class OptionPlan(slot: Int, inner: FieldPlan) extends FieldPlan:

    def put(r: GenericData.Record, value: Any): Unit =
      value match
        case None    => r.put(slot, null)
        case Some(v) => inner.put(r, v)

  final private[avro] case class SubRecordPlan(slot: Int, sub: RecordLevel) extends FieldPlan:
    def put(r: GenericData.Record, value: Any): Unit = r.put(slot, sub.build(value))

  /** The field type's own codec — resolved once at construction; encode errors throw (eo's total
    * encode convention, matching [[AvroVulcan.codec]]).
    */
  final private[avro] case class CodecPlan(slot: Int, codec: VCodec[Any]) extends FieldPlan:

    def put(r: GenericData.Record, value: Any): Unit =
      r.put(slot, codec.encode(value).fold(e => throw e.throwable, identity))

  private[avro] def buildLevel(
      schema: Schema,
      shape: RecordShape,
      who: String,
      parent: RecordLevel | Null,
  ): Exception | RecordLevel =
    val level = new RecordLevel(schema, parent)
    AvroWalk.recordSlots(schema, shape.fields.map(_.name), who) match
      case e: Exception      => e
      case slots: Array[Int] =>
        val fields = schema.getFields
        val directBuf = List.newBuilder[DirectSlot]
        val unusualBuf = List.newBuilder[UnusualSlot]
        // The first arm failure short-circuits the level; the shell is discarded with it.
        @tailrec def each(i: Int, rest: List[FieldShape]): Exception | Null =
          rest match
            case Nil    => null
            case f :: t =>
              planFor(f.kind, slots(i), f.name, fields.get(slots(i)).schema, level, who) match
                case e: Exception  => e
                case d: DirectPlan =>
                  directBuf += DirectSlot(d.slot, i)
                  each(i + 1, t)
                case plan: FieldPlan =>
                  unusualBuf += UnusualSlot(i, plan)
                  each(i + 1, t)
        each(0, shape.fields) match
          case e: Exception => e
          case null         =>
            level.seal(directBuf.result().toArray, unusualBuf.result().toArray)
            level

  /** The `FieldPlan` for one arm, validating the arm against the schema it will write into. */
  private def planFor(
      kind: Kind,
      slot: Int,
      name: String,
      schema: Schema,
      level: RecordLevel,
      who: String,
  ): Exception | FieldPlan =
    kind match
      case DirectKind(expected) =>
        unwrapNullable(schema, name, who) match
          case e: Exception  => e
          case inner: Schema =>
            if inner.getType != expected then typeMismatch(name, expected, inner.getType, who)
            else DirectPlan(slot)
      case RecordKind(sub) =>
        unwrapNullable(schema, name, who) match
          case e: Exception  => e
          case inner: Schema =>
            if inner.getType != Schema.Type.RECORD then
              typeMismatch(name, Schema.Type.RECORD, inner.getType, who)
            else
              buildLevel(inner, sub, s"$who → $name (${sub.typeName})", parent = level) match
                case e: Exception          => e
                case subLevel: RecordLevel => SubRecordPlan(slot, subLevel)
      case SelfKind(depth) =>
        climb(level, depth) match
          case e: Exception        => e
          case target: RecordLevel => SubRecordPlan(slot, target)
      case CodecKind(codec) =>
        CodecPlan(slot, codec)
      case OptionKind(innerKind) =>
        if schema.getType != Schema.Type.UNION || !hasNullBranch(schema) then
          notNullUnion(name, schema.getType, who)
        else
          val innerSchema = if schema.getTypes.size == 2 then nonNullBranch(schema) else schema
          innerPlanFor(innerKind, slot, name, innerSchema, schema, level, who) match
            case e: Exception     => e
            case inner: FieldPlan => OptionPlan(slot, inner)

  /** The inner arm of an `OptionPlan`. Primitive / nested-record inners require a 2-branch nullable
    * pair (they write the non-null branch's shape); codec / self / nested-option inners consume no
    * schema shape and accept wider unions.
    */
  private def innerPlanFor(
      kind: Kind,
      slot: Int,
      name: String,
      innerSchema: Schema,
      fieldSchema: Schema,
      level: RecordLevel,
      who: String,
  ): Exception | FieldPlan =
    kind match
      case DirectKind(expected) =>
        if fieldSchema.getTypes.size != 2 then pairMismatch(name, fieldSchema.getTypes.size, who)
        else if innerSchema.getType != expected then
          typeMismatch(name, expected, innerSchema.getType, who)
        else DirectPlan(slot)
      case RecordKind(sub) =>
        if fieldSchema.getTypes.size != 2 then pairMismatch(name, fieldSchema.getTypes.size, who)
        else if innerSchema.getType != Schema.Type.RECORD then
          typeMismatch(name, Schema.Type.RECORD, innerSchema.getType, who)
        else
          buildLevel(innerSchema, sub, s"$who → $name (${sub.typeName})", parent = level) match
            case e: Exception          => e
            case subLevel: RecordLevel => SubRecordPlan(slot, subLevel)
      case other => planFor(other, slot, name, innerSchema, level, who)

  /** The schema behind a plain (non-`Option`) arm: itself, or the non-null branch of a 2-branch
    * nullable pair (a non-`Option` case field under a nullable schema — the codec writes the value,
    * never null).
    */
  private def unwrapNullable(schema: Schema, name: String, who: String): Schema | Exception =
    if schema.getType != Schema.Type.UNION then schema
    else
      val branches = schema.getTypes
      if branches.size != 2 then multiBranch(name, branches.size, who)
      else
        val i =
          if branches.get(0).getType == Schema.Type.NULL then 1
          else if branches.get(1).getType == Schema.Type.NULL then 0
          else -1
        if i < 0 then noNullBranch(name, who)
        else branches.get(i)

  // ---- Failure factories (one message per refusal, returned — never thrown) ----

  private def typeMismatch(
      name: String,
      expected: Schema.Type,
      actual: Schema.Type,
      who: String,
  ): IllegalArgumentException =
    IllegalArgumentException(
      s"$who: case field '$name' needs a $expected schema field but schema field '$name' is a"
        + s" $actual — the schema must agree with the case-class shape; encode through the codec or"
        + " align the schema."
    )

  private def pairMismatch(name: String, branches: Int, who: String): IllegalArgumentException =
    IllegalArgumentException(
      s"$who: case field '$name' is an Option whose inner arm is a primitive leaf or a nested case"
        + s" class, but schema field '$name' is a $branches-branch union — the builder's fast arms"
        + " handle plain fields and 2-branch nullable pairs only; wider unions belong to the field's"
        + " own codec."
    )

  private def notNullUnion(
      name: String,
      actual: Schema.Type,
      who: String,
  ): IllegalArgumentException =
    IllegalArgumentException(
      s"$who: case field '$name' is Option[...] but schema field '$name' is a $actual, not a"
        + " null-union — vulcan pairs an optional case field with a null-union, and the builder"
        + " mirrors the codec's encode (None → null), not the schema default."
    )

  private def multiBranch(name: String, branches: Int, who: String): IllegalArgumentException =
    IllegalArgumentException(
      s"$who: schema field '$name' is a $branches-branch union — the builder's non-Option arms"
        + " handle plain fields and 2-branch nullable pairs only; wider unions belong to the field's"
        + " own codec."
    )

  private def noNullBranch(name: String, who: String): IllegalArgumentException =
    IllegalArgumentException(
      s"$who: schema field '$name' is a union without a null branch — the builder handles plain"
        + " fields and 2-branch nullable pairs only."
    )

  private def hasNullBranch(schema: Schema): Boolean =
    schema.getTypes.asScala.exists(_.getType == Schema.Type.NULL)

  private def nonNullBranch(schema: Schema): Schema =
    val branches = schema.getTypes
    if branches.get(0).getType == Schema.Type.NULL then branches.get(1) else branches.get(0)

  /** `depth` levels up the parent chain — 0 is `level` itself (a self-recursive field of the level
    * being built). The depth is compile-time-derived from the ancestor stack, so the failure below
    * is an invariant check, not an expected one.
    */
  @tailrec private def climb(level: RecordLevel, depth: Int): Exception | RecordLevel =
    if depth == 0 then level
    else
      level.parent match
        case p: RecordLevel => climb(p, depth - 1)
        case null           =>
          IllegalStateException(
            s"whole-record builder: self-reference depth $depth exceeds the level chain —"
              + " internal invariant broken"
          )
