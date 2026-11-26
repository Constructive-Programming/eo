package dev.constructive.eo.avro

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.deriving.Mirror
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import java.lang.ref.{ReferenceQueue, WeakReference}
import java.nio.ByteBuffer
import java.time.{Instant, LocalDate, LocalTime}
import java.util.concurrent.ConcurrentHashMap
import java.util.{List as JList, Map as JMap, UUID}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericEnumSymbol, GenericFixed, IndexedRecord}

/** SENTINEL SYNTHESIS for the differential codec probe (issue #100).
  *
  * `AvroWalk.fieldNameAt` resolves `.field(_.x)` by NAME when the codec names the whole case-field
  * list onto distinct schema fields, else by declaration POSITION. Three codec shapes defeat both
  * rungs, because neither the names nor the arity carry a recoverable signal: a field list that
  * renames beyond recognition AND reorders, two same-physical-type columns renamed and reversed,
  * and compensating arity (one field dropped, one computed column added, counts agree). A fourth
  * shape defeats the NAME rung specifically: a schema column that bears a case field's name but
  * holds a different value (a derived public id, a stale legacy column).
  *
  * Nothing schema-derived reaches them — not the record's name, not its parsing fingerprint —
  * because the residual pair is two columns of the same physical type inside ONE record, so both
  * candidate readings carry the same fullname and the same fingerprint by construction. The only
  * mechanism that does is a DIFFERENTIAL PROBE of the codec itself: encode two sentinel values of
  * the parent case class that differ in exactly the target field, diff the resulting
  * `IndexedRecord` slots, and read which slot actually moved. That is ground truth, not inference.
  *
  * '''The probe never becomes a new failure surface.''' Every step answers `Option`, and `None`
  * means "no verdict" — the caller falls back to the name/position rule it would have used anyway.
  * A field whose type has no [[AvroProbe]] instance (an opaque or refined type, a recursive ADT, a
  * custom `AvroCodec`) makes the whole parent probe unavailable at COMPILE time
  * ([[AvroProbe.Slots]] is derived all-or-nothing, so its absence is a failed implicit search, not
  * an error), and a codec that rejects the sentinel throws inside `encode` and is caught at that
  * boundary. So the worst case of this machinery is the behaviour that shipped before it.
  *
  * '''Two verdict rules, one for each caller.'''
  *
  *   - `.field(_.x)` has the field's OWN codec, so it demands the strict rule: exactly one changed
  *     slot, and that slot must hold the field codec's encoding of the perturbed value in the
  *     perturbed record AND of the baseline value in the baseline record. That is what separates
  *     the field's home from a DERIVED column that moves with it (`public_id = "pub-" + id`, a
  *     digest of the payload): the derived column moves too, but it does not hold the field's
  *     value.
  *   - `.fields(...)` only has the NamedTuple codec, so it cannot build the expected value and uses
  *     the weaker rule: exactly one changed slot. Provably safe — on a positionally 1:1 codec, the
  *     only slot that can move is the field's own, so the rule cannot re-aim a call site that is
  *     already correct — and it still reaches a renamed-and-reordered list.
  *
  * A field the codec DROPS moves no slot (or moves only a derived column that does not hold its
  * value), so both rules abstain and resolution stays where it was. The probe cannot invent a
  * column that the codec never wrote.
  *
  * The probe runs at optic CONSTRUCTION, once per `(codec, case field)` — the verdict is cached in
  * a weak-keyed table, so a `def`-shaped optic that rebuilds its prism per call pays one lookup,
  * not two encodes. Zero per-operation cost, like the rest of the resolution machinery.
  */
trait AvroProbe[A]:

  /** Two DISTINCT values of `A`, or `None` when `A` has no synthesisable sentinel. Implementations
    * must never throw: a type that cannot be probed abstains by returning `None`.
    */
  def pair: Option[(A, A)]

/** Sentinel synthesis, and the differential probe built on it. See the [[AvroProbe]] scaladoc. */
object AvroProbe extends AvroProbeLowPriority:

  def apply[A](using probe: AvroProbe[A]): AvroProbe[A] = probe

  /** The two sentinels of `A`, or `None`. */
  def pair[A](using probe: AvroProbe[A]): Option[(A, A)] = probe.pair

  /** One sentinel pair per CASE FIELD of `A`, in declaration order, plus the constructor that turns
    * a per-field value array back into an `A` — everything the probe needs to build a baseline
    * instance and a perturbed one that differs in exactly one field.
    *
    * Derived all-or-nothing: if any case field's type has no [[AvroProbe]] instance the derivation
    * does not resolve, the macro emits `None`, and resolution falls back to name/position. An EMPTY
    * `pairs` array is the runtime abstention of the same kind (a field whose probe answers `None`).
    */
  trait Slots[A]:

    /** One `(baseline, perturbed)` pair per case field, in DECLARATION order; empty ⇒ unavailable.
      */
    def pairs: Array[(Any, Any)]

    /** Build an `A` from one value per case field, in declaration order. */
    def build(values: Array[Any]): A

  object Slots:

    /** The `Mirror`-driven derivation. Its `FieldProbes` parameter is what makes the derivation
      * all-or-nothing at COMPILE time: a single unsynthesisable field drops the whole instance from
      * implicit scope instead of failing later, at a `.field` call site, in a way the user cannot
      * attribute.
      */
    given derived[A](using
        m: Mirror.ProductOf[A],
        fields: FieldProbes[m.MirroredElemTypes],
    ): Slots[A] =
      new Slots[A]:
        // Lazy: a probe that never needs the sentinels (a cache hit) never builds them.
        lazy val pairs: Array[(Any, Any)] = fields.pairs.getOrElse(EmptyPairs)

        def build(values: Array[Any]): A = m.fromProduct(Tuple.fromArray(values))

  /** Per-element sentinel pairs for a product's field tuple. INTERNAL MACHINERY — it exists so that
    * `Slots.derived` and `AvroProbe.product` can be all-or-nothing at compile time; it is not part
    * of the supported surface and its shape may change.
    */
  sealed trait FieldProbes[T <: Tuple]:
    def pairs: Option[Array[(Any, Any)]]

  object FieldProbes:

    given empty: FieldProbes[EmptyTuple] with
      val pairs: Option[Array[(Any, Any)]] = Some(Array.empty[(Any, Any)])

    given cons[H, T <: Tuple](using h: AvroProbe[H], t: FieldProbes[T]): FieldProbes[H *: T] with

      def pairs: Option[Array[(Any, Any)]] =
        (h.pair, t.pairs) match
          case (Some(head), Some(tail)) =>
            val out = new Array[(Any, Any)](tail.length + 1)
            out(0) = head
            System.arraycopy(tail, 0, out, 1, tail.length)
            Some(out)
          case _ => None

  /** One representative value per alternative of a sum. Same role as [[FieldProbes]] for products;
    * same "internal machinery" caveat.
    */
  sealed trait AltProbes[T <: Tuple]:
    def values: Option[Array[Any]]

  object AltProbes:

    given empty: AltProbes[EmptyTuple] with
      val values: Option[Array[Any]] = Some(Array.empty[Any])

    given cons[H, T <: Tuple](using h: AvroProbe[H], t: AltProbes[T]): AltProbes[H *: T] with

      def values: Option[Array[Any]] =
        (h.pair, t.values) match
          case (Some(head), Some(tail)) =>
            val out = new Array[Any](tail.length + 1)
            out(0) = head._1
            System.arraycopy(tail, 0, out, 1, tail.length)
            Some(out)
          case _ => None

  // ---- leaf sentinels -------------------------------------------------
  //
  // Distinctness is what the probe needs; a length-preserving pair for `String` is a bonus, because a
  // computed column that derives from a field's LENGTH then does not move at all.

  /** A leaf's two sentinels, fixed at construction. */
  private def of[A](a: A, b: A): AvroProbe[A] =
    new AvroProbe[A]:
      val pair: Option[(A, A)] = Some((a, b))

  given booleanProbe: AvroProbe[Boolean] = of(true, false)
  given byteProbe: AvroProbe[Byte] = of(11.toByte, 12.toByte)
  given shortProbe: AvroProbe[Short] = of(101.toShort, 102.toShort)
  given intProbe: AvroProbe[Int] = of(1001, 1002)
  given longProbe: AvroProbe[Long] = of(1000000001L, 1000000002L)
  given floatProbe: AvroProbe[Float] = of(1001.5f, 1002.5f)
  given doubleProbe: AvroProbe[Double] = of(1001.5, 1002.5)
  given charProbe: AvroProbe[Char] = of('a', 'b')
  given stringProbe: AvroProbe[String] = of("eo-probe-0", "eo-probe-1")
  given bytesProbe: AvroProbe[Array[Byte]] = of(Array[Byte](11), Array[Byte](12))

  given byteBufferProbe: AvroProbe[ByteBuffer] =
    of(ByteBuffer.wrap(Array[Byte](11)), ByteBuffer.wrap(Array[Byte](12)))

  given bigDecimalProbe: AvroProbe[BigDecimal] = of(BigDecimal(1001), BigDecimal(1002))

  given uuidProbe: AvroProbe[UUID] =
    of(
      UUID.fromString("00000000-0000-0000-0000-000000001001"),
      UUID.fromString("00000000-0000-0000-0000-000000001002"),
    )

  given instantProbe: AvroProbe[Instant] =
    of(Instant.ofEpochMilli(1000000001L), Instant.ofEpochMilli(1000000002L))

  given localDateProbe: AvroProbe[LocalDate] =
    of(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2))

  given localTimeProbe: AvroProbe[LocalTime] = of(LocalTime.of(1, 0), LocalTime.of(2, 0))

  // ---- containers -----------------------------------------------------
  //
  // Single-element collections: the probe needs two DISTINCT values, not interesting contents, and a
  // one-element collection keeps the sentinel's own structure out of the way.

  given optionProbe[A](using p: AvroProbe[A]): AvroProbe[Option[A]] =
    new AvroProbe[Option[A]]:
      def pair: Option[(Option[A], Option[A])] = p.pair.map((a, b) => (Some(a), Some(b)))

  given listProbe[A](using p: AvroProbe[A]): AvroProbe[List[A]] =
    new AvroProbe[List[A]]:
      def pair: Option[(List[A], List[A])] = p.pair.map((a, b) => (List(a), List(b)))

  given vectorProbe[A](using p: AvroProbe[A]): AvroProbe[Vector[A]] =
    new AvroProbe[Vector[A]]:
      def pair: Option[(Vector[A], Vector[A])] = p.pair.map((a, b) => (Vector(a), Vector(b)))

  given seqProbe[A](using p: AvroProbe[A]): AvroProbe[Seq[A]] =
    new AvroProbe[Seq[A]]:
      def pair: Option[(Seq[A], Seq[A])] = p.pair.map((a, b) => (Seq(a), Seq(b)))

  given setProbe[A](using p: AvroProbe[A]): AvroProbe[Set[A]] =
    new AvroProbe[Set[A]]:
      def pair: Option[(Set[A], Set[A])] = p.pair.map((a, b) => (Set(a), Set(b)))

  given arraySeqProbe[A](using
      p: AvroProbe[A],
      ct: scala.reflect.ClassTag[A]
  ): AvroProbe[ArraySeq[A]] =
    new AvroProbe[ArraySeq[A]]:
      def pair: Option[(ArraySeq[A], ArraySeq[A])] =
        p.pair.map((a, b) => (ArraySeq(a), ArraySeq(b)))

  given mapProbe[K, V](using k: AvroProbe[K], v: AvroProbe[V]): AvroProbe[Map[K, V]] =
    new AvroProbe[Map[K, V]]:
      def pair: Option[(Map[K, V], Map[K, V])] =
        (k.pair, v.pair) match
          case (Some((k1, k2)), Some((v1, v2))) => Some((Map(k1 -> v1), Map(k2 -> v2)))
          case _                                => None

  given eitherProbe[A, B](using a: AvroProbe[A], b: AvroProbe[B]): AvroProbe[Either[A, B]] =
    new AvroProbe[Either[A, B]]:
      def pair: Option[(Either[A, B], Either[A, B])] =
        (a.pair, b.pair) match
          case (Some((a1, _)), Some((_, b2))) => Some((Left(a1), Right(b2)))
          case _                              => None

  // ---- the probe ------------------------------------------------------

  // The two verdict entry points differ only in the RULE they hand `verdict`; the guard, cache and
  // sentinel path they share is the point of the pair.
  /** Strict verdict for `.field(_.x)`: `declIdx`'s schema field name, established by encoding two
    * sentinels of `codec`'s type and requiring the changed slot to hold `fieldCodec`'s encoding of
    * the perturbed value (and of the baseline value in the baseline record). `None` whenever the
    * sentinels cannot be built, the codec refuses them, no slot — or more than one — qualifies, or
    * the answer is not a field of `parent`. The caller then resolves by name/position as before.
    */
  private[avro] def fieldName[A](
      codec: AvroCodec[A],
      fieldCodec: AvroCodec[?],
      slots: => Option[Slots[A]],
      declIdx: Int,
      parent: Schema,
  ): Option[String] =
    verdict(codec, declIdx, strict = true, parent, slots) { (s, pairs) =>
      val (baseValue, pertValue) = pairs(declIdx)
      for
        expectedBase <- encodeValue(fieldCodec, baseValue)
        expectedPert <- encodeValue(fieldCodec, pertValue)
        name <- run(codec, s, pairs, declIdx, Some((expectedBase, expectedPert)))
      yield name
    }

  /** Weaker verdict for `.fields(...)`, which has no per-field codec to build an expected value
    * from: the schema field name of `declIdx`, accepted only when EXACTLY ONE slot moved. Sound
    * because a positionally 1:1 codec — the population a wrong re-aim would break — can only move
    * the field's own slot, so the rule never disagrees with a resolution that is already correct.
    */
  private[avro] def fieldNameByChange[A](
      codec: AvroCodec[A],
      slots: => Option[Slots[A]],
      declIdx: Int,
      parent: Schema,
  ): Option[String] =
    verdict(codec, declIdx, strict = false, parent, slots)((s, pairs) =>
      run(codec, s, pairs, declIdx, None)
    )

  /** The one path both verdict rules run: the parent guard, the cached verdict, the sentinels, then
    * the rule.
    *
    * The parent guard is a TYPE test first because `Schema.getField` THROWS `AvroRuntimeException`
    * on a non-record — it is not a null-returning lookup — and a union / primitive / array parent
    * has no field names at all, so abstaining is the only correct answer there.
    *
    * The trailing filter keeps a cached verdict only when the record it will be WALKED against
    * really carries that field. The probe reads the codec's own record; the walk reads the schema
    * at the optic's path. They are the same schema for any consistent codec, and this is what keeps
    * a hand-built inconsistent one from re-aiming a call site at a name the walk cannot reach.
    */
  // `verdict` and `withPairs` are two steps of ONE pipeline (guard + cache, then sentinels +
  // bounds); their `Option` plumbing is shared on purpose.
  private def verdict[A](
      codec: AvroCodec[A],
      declIdx: Int,
      strict: Boolean,
      parent: Schema,
      slots: => Option[Slots[A]],
  )(rule: (Slots[A], Array[(Any, Any)]) => Option[String]): Option[String] =
    if parent.getType != Schema.Type.RECORD then None
    else
      cached(codec, declIdx, strict)(withPairs(slots, declIdx)(rule))
        .filter(name => parent.getField(name) != null)

  /** Run `rule` on the parent's sentinel pairs, or abstain when the synthesiser is absent, yields
    * none, or `declIdx` falls outside them.
    */
  private def withPairs[A](
      slots: => Option[Slots[A]],
      declIdx: Int,
  )(rule: (Slots[A], Array[(Any, Any)]) => Option[String]): Option[String] =
    slots.flatMap { s =>
      pairsOf(s).flatMap { pairs =>
        if declIdx < 0 || declIdx >= pairs.length then None else rule(s, pairs)
      }
    }

  /** One verdict per `(parent codec, case field, rule)`.
    *
    * Deliberately NOT keyed on the sentinels or on the field codec: the verdict is a property of
    * the PARENT codec — which slot it writes this case field into — and both of those only gate
    * whether the probe can establish it. A call that abstained because its own field codec could
    * not encode the sentinel therefore caches the abstention for later callers: a false NEGATIVE,
    * which costs the probe's improvement and never produces a wrong name.
    */
  private def cached[A](
      codec: AvroCodec[A],
      declIdx: Int,
      strict: Boolean,
  )(compute: => Option[String]): Option[String] =
    val key = new ProbeQuery(codec, declIdx, strict)
    val hit = answers.get(key)
    if hit != null then hit
    else
      val answer = compute
      // Drain first: a purge costs nothing on the hot path because it only runs on a cache MISS,
      // which is once per (codec, case field) for the lifetime of that codec.
      purgeStale()
      val raced =
        answers.putIfAbsent(new ProbeWeakKey(codec, staleAnswers, declIdx, strict), answer)
      if raced == null then answer else raced

  /** Encode the baseline and the perturbed instance, then diff. Every step is `Option`-shaped so a
    * refusal anywhere abstains instead of propagating.
    */
  private def run[A](
      codec: AvroCodec[A],
      slots: Slots[A],
      pairs: Array[(Any, Any)],
      declIdx: Int,
      expected: Option[(Any, Any)],
  ): Option[String] =
    val base = new Array[Any](pairs.length)
    val pert = new Array[Any](pairs.length)
    @tailrec def fill(i: Int): Unit =
      if i < pairs.length then
        val (b, p) = pairs(i)
        base(i) = b
        pert(i) = if i == declIdx then p else b
        fill(i + 1)
    fill(0)
    for
      v1 <- buildQuietly(slots, base)
      v2 <- buildQuietly(slots, pert)
      r1 <- encodeRecord(codec, v1)
      r2 <- encodeRecord(codec, v2)
      name <- diff(r1, r2, expected)
    yield name

  /** Force the sentinels, treating an EMPTY array as "no verdict" — the runtime half of the
    * compile-time all-or-nothing: a parent whose fields all have probes, but whose PROBE for some
    * field answers `None` (a recursive ADT, a sum with one alternative), yields no pairs.
    *
    * `Slots` is a supported extension point, so a hand-written one may throw while building its
    * sentinels; that abstains here rather than escaping into optic construction.
    */
  private def pairsOf[A](slots: Slots[A]): Option[Array[(Any, Any)]] =
    try
      val pairs = slots.pairs
      if pairs.isEmpty then None else Some(pairs)
    catch case NonFatal(_) => None

  /** `Slots.build` is the one step the probe does not own — a hand-written `Slots` for an opaque
    * type is a supported extension point — so a throw there abstains rather than escaping into
    * optic construction.
    */
  private def buildQuietly[A](slots: Slots[A], values: Array[Any]): Option[A] =
    try Some(slots.build(values))
    catch case NonFatal(_) => None

  /** CODEC BOUNDARY. kindlings' encoders throw on a value their schema cannot carry (and a
    * validating `vulcan.Codec` throws on one its validation rejects), which is exactly the "the
    * codec refuses the sentinel" case: abstain, never surface it. A parent whose encode is not a
    * record has no slots to diff at all.
    */
  private def encodeRecord(codec: AvroCodec[?], value: Any): Option[IndexedRecord] =
    try
      codec.asInstanceOf[AvroCodec[Any]].encode(value) match
        case rec: IndexedRecord if rec.getSchema.getType == Schema.Type.RECORD => Some(rec)
        case _                                                                 => None
    catch case NonFatal(_) => None

  /** CODEC BOUNDARY — see [[encodeRecord]]. */
  private def encodeValue(codec: AvroCodec[?], value: Any): Option[Any] =
    try Some(codec.asInstanceOf[AvroCodec[Any]].encode(value))
    catch case NonFatal(_) => None

  /** Exactly one slot may qualify: more than one is an ambiguous signal (a field written to two
    * columns, a derived column that happens to hold the sentinel), and zero is no signal at all.
    */
  private def diff(
      r1: IndexedRecord,
      r2: IndexedRecord,
      expected: Option[(Any, Any)],
  ): Option[String] =
    val fields = r1.getSchema.getFields
    val n = fields.size
    if r2.getSchema.getFields.size != n then None
    else
      @tailrec def scan(k: Int, hits: Int, name: String | Null): (Int, String | Null) =
        if k >= n then (hits, name)
        else
          val a = r1.get(k)
          val b = r2.get(k)
          if sameValue(a, b) then scan(k + 1, hits, name)
          else if qualifies(a, b, expected) then scan(k + 1, hits + 1, fields.get(k).name)
          else scan(k + 1, hits, name)
      scan(0, 0, null) match
        case (1, hit) => if hit == null then None else Some(hit)
        case _        => None

  private def qualifies(a: Any, b: Any, expected: Option[(Any, Any)]): Boolean =
    expected match
      case Some((base, pert)) => sameValue(a, base) && sameValue(b, pert)
      case None               => true

  /** STRUCTURAL equality over Avro's runtime value space. `Utf8` only compares equal to another
    * `Utf8`, `GenericData.Record` compares by identity, and a generic array's `equals` delegates to
    * its elements' — so `==` would report a difference for every record-shaped slot and a match for
    * none of the string ones. Numbers compare by CLASS as well as value: an `Int` sentinel must not
    * match a `Long` column, or a derived count could claim a field.
    */
  private def sameValue(a: Any, b: Any): Boolean =
    a match
      case null            => b == null
      case x: CharSequence =>
        b match
          case y: CharSequence => x.toString == y.toString
          case _               => false
      case x: Array[Byte] =>
        b match
          case y: Array[Byte] => java.util.Arrays.equals(x, y)
          case y: ByteBuffer  => ByteBuffer.wrap(x) == y
          case _              => false
      case x: ByteBuffer =>
        b match
          case y: ByteBuffer  => x == y
          case y: Array[Byte] => x == ByteBuffer.wrap(y)
          case _              => false
      case x: IndexedRecord =>
        b match
          case y: IndexedRecord => sameRecord(x, y)
          case _                => false
      case x: GenericEnumSymbol[?] =>
        b match
          case y: GenericEnumSymbol[?] => x.toString == y.toString
          case _                       => false
      case x: GenericFixed =>
        b match
          case y: GenericFixed => java.util.Arrays.equals(x.bytes, y.bytes)
          case _               => false
      case x: JList[?] =>
        b match
          case y: JList[?] => sameList(x, y)
          case _           => false
      case x: JMap[?, ?] =>
        b match
          case y: JMap[?, ?] => sameMap(x, y)
          case _             => false
      case x: java.lang.Number =>
        b match
          case y: java.lang.Number => x.getClass == y.getClass && x == y
          case _                   => false
      case x => x == b

  private def sameRecord(a: IndexedRecord, b: IndexedRecord): Boolean =
    val sa = a.getSchema
    val sb = b.getSchema
    val n = sa.getFields.size
    sa.getFullName == sb.getFullName && sb.getFields.size == n && sameElements(a, b, n)

  private def sameList(a: JList[?], b: JList[?]): Boolean =
    val n = a.size
    n == b.size && sameElements(a, b, n)

  /** Element-wise structural comparison of the first `n` elements of two Avro indexables — the one
    * loop behind [[sameRecord]] and [[sameList]].
    */
  private def sameElements(a: Any, b: Any, n: Int): Boolean =
    @tailrec def loop(i: Int): Boolean =
      i >= n || (sameValue(slotAt(a, i), slotAt(b, i)) && loop(i + 1))
    loop(0)

  /** The `i`-th element of either indexable shape a diffed slot can be: a record's field slot, or a
    * generic array's element.
    */
  private def slotAt(value: Any, i: Int): Any =
    value match
      case rec: IndexedRecord => rec.get(i)
      case lst: JList[?]      => lst.get(i)
      case _                  => null

  private def sameMap(a: JMap[?, ?], b: JMap[?, ?]): Boolean =
    val left = a.asInstanceOf[JMap[Any, Any]].asScala
    val right = b.asInstanceOf[JMap[Any, Any]].asScala
    left.size == right.size && left.forall { (k, v) =>
      right.get(k) match
        case Some(other) => sameValue(v, other)
        case None        => false
    }

  private val EmptyPairs: Array[(Any, Any)] = Array.empty[(Any, Any)]

  // ---- verdict cache --------------------------------------------------
  //
  // Same design as `AvroWalk.normalisedNameIndex`'s: identity-keyed (a codec is not `equals`-shaped),
  // WEAK so a codec built dynamically is collectable, and the value holds no reference back to the
  // codec. One entry per `(codec, case field, verdict rule)` for the life of the codec.

  private val answers: ConcurrentHashMap[ProbeKey, Option[String]] =
    new ConcurrentHashMap[ProbeKey, Option[String]]()

  private val staleAnswers: ReferenceQueue[AnyRef] = new ReferenceQueue[AnyRef]()

  private def purgeStale(): Unit =
    @tailrec def drain(): Unit =
      val stale = staleAnswers.poll()
      if stale != null then
        answers.remove(stale.asInstanceOf[ProbeKey]): Unit
        drain()
    drain()

  // The identity-keyed cache face MIRRORS `AvroWalk.SchemaId`'s, deliberately: same identity-hash
  // contract, same cleared-weak-key removal rule. Two caches, one shape to audit.
  sealed private trait ProbeKey:
    def codecRef: AnyRef | Null
    def declIdx: Int
    def strict: Boolean
    def idHash: Int

    final override def hashCode(): Int = idHash

    final override def equals(other: Any): Boolean =
      other match
        case that: ProbeKey =>
          // Identity FIRST so a cleared weak key can still be removed by the purge — its referent is
          // gone, so the reference comparison below would refuse to match even itself.
          (this eq that) || {
            val mine = codecRef
            mine != null && (mine eq that.codecRef) && declIdx == that.declIdx && strict == that.strict
          }
        case _ => false

  end ProbeKey

  /** Throwaway lookup key — a plain object, so a cache HIT costs no `WeakReference` registration.
    */
  final private class ProbeQuery(codec: AnyRef, val declIdx: Int, val strict: Boolean)
      extends ProbeKey:
    val codecRef: AnyRef = codec
    val idHash: Int = System.identityHashCode(codec)

  /** The stored key: weak, so the entry dies with the codec. */
  final private class ProbeWeakKey(
      codec: AnyRef,
      queue: ReferenceQueue[AnyRef],
      val declIdx: Int,
      val strict: Boolean,
  ) extends WeakReference[AnyRef](codec, queue),
        ProbeKey:
    val idHash: Int = System.identityHashCode(codec)
    def codecRef: AnyRef | Null = get()

end AvroProbe

/** Lower priority than the leaf/container instances above, so an explicit `AvroProbe[Option[A]]` is
  * never ambiguous with the `Mirror.SumOf` derivation.
  */
private[avro] trait AvroProbeLowPriority:

  import AvroProbe.{AltProbes, FieldProbes}

  // The two `Mirror` derivations (product / sum) share the `given` head and the `pair` shape; the
  // bodies are the two different constructions.
  /** A case class: one sentinel pair per field, built through the `Mirror`. */
  given product[A](using
      m: Mirror.ProductOf[A],
      fp: FieldProbes[m.MirroredElemTypes],
  ): AvroProbe[A] =
    new AvroProbe[A]:
      def pair: Option[(A, A)] =
        fp.pairs.map { pairs =>
          val n = pairs.length
          val base = new Array[Any](n)
          val pert = new Array[Any](n)
          @tailrec def fill(i: Int): Unit =
            if i < n then
              val (b, p) = pairs(i)
              base(i) = b
              pert(i) = p
              fill(i + 1)
          fill(0)
          (m.fromProduct(Tuple.fromArray(base)), m.fromProduct(Tuple.fromArray(pert)))
        }

  /** A sum: one representative per alternative, so two DIFFERENT branches — a singleton enum case
    * yields one value, and two of them are still distinct.
    */
  given sum[A](using m: Mirror.SumOf[A], ap: AltProbes[m.MirroredElemTypes]): AvroProbe[A] =
    new AvroProbe[A]:
      def pair: Option[(A, A)] =
        ap.values match
          case Some(values) if values.length >= 2 =>
            Some((values(0).asInstanceOf[A], values(1).asInstanceOf[A]))
          case _ => None

end AvroProbeLowPriority
