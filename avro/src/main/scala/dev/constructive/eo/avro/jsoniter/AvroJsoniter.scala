package dev.constructive.eo.avro.jsoniter

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import com.github.plokhotnyuk.jsoniter_scala.core.{
  readFromArrayReentrant,
  writeToArray,
  JsonReader,
  JsonValueCodec,
  JsonWriter
}
import dev.constructive.eo.avro.{AvroBinaryCursor, AvroCodec, AvroPrism, AvroTraversal}
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.jsoniter.JsoniterPrism
import dev.constructive.eo.optics.{Getter, MendTearPrism, Optic, Prism}
import java.nio.ByteBuffer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericEnumSymbol, GenericFixed, IndexedRecord}

/** Structural Avro ↔ JSON-'''bytes''' bridge: move between Avro generic runtime values / payload
  * bytes and UTF-8 JSON byte arrays through jsoniter-scala's streaming `JsonWriter` / `JsonReader`,
  * '''without''' a typed case class — and without a JSON AST — in the middle. The AST-free sibling
  * of `dev.constructive.eo.avro.circe.AvroJson`: same walks, same conventions both ways, but the
  * JSON side is `Array[Byte]` instead of `io.circe.Json`, so circe never touches the classpath.
  * Lives inside `cats-eo-avro` with jsoniter-scala-core as an `Optional` dependency (the rendering
  * runs through its `JsonWriter`) — add `com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core`
  * to use this package.
  *
  * ==Rendering conventions (record → JSON bytes)==
  *
  * [[avroToJson]] mirrors `AvroJson.avroToJson` case for case, so the two bridges render
  * parse-equivalent documents for the same record:
  *   - `IndexedRecord` → object (field name from `getSchema.getFields`, recurse on value, schema
  *     declaration order);
  *   - `java.util.Map` → object (stringify keys, recurse on values, entry iteration order);
  *   - `java.util.List` → array (recurse);
  *   - `CharSequence` (incl. `org.apache.avro.util.Utf8`) → JSON string;
  *   - `Integer` / `Long` → JSON number; `Float` / `Double` → JSON number via jsoniter's
  *     shortest-round-trip rendering, non-finite values → `null` (matching circe's
  *     `fromFloatOrNull` / `fromDoubleOrNull`);
  *   - `Boolean` → JSON boolean; a resolved `null` union branch → `null`;
  *   - `GenericEnumSymbol` → JSON string;
  *   - `ByteBuffer` / `GenericFixed` → array of signed byte ints (circe's `Encoder[Array[Byte]]`
  *     convention, same as `AvroJson`);
  *   - any other runtime type → JSON string of `value.toString` — a lenient last resort (the walk
  *     is total), not a convention to rely on.
  *
  * Unions are resolved at the value level (the runtime value IS the branch), so dispatch on the
  * runtime type needs no union special-casing.
  *
  * ==Codec diagonals==
  *
  * The typed prism family mirrors `AvroJson`'s, with [[JsoniterBytes]] in the `Json` slot: the
  * fundamental diagonal [[valuePrism]] tears a generic runtime value into a typed `A` and mends any
  * generic value out as JSON bytes; [[bytesPrism]] and [[recordPrism]] pre-compose its input slots
  * via `tearFrom` / `mendFrom`. The [[AvroBytes]] / [[JsoniterBytes]] aliases (see the package
  * object) keep the two `Array[Byte]` roles apart in the signatures.
  *
  * ==Drilled cursors (`.json` and `.avro` faces)==
  *
  * The full `AvroPrism` cursor sugar — `.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` /
  * `.each` / Dynamic selection — reaches this bridge through the [[json]] extensions on `AvroPrism`
  * / `AvroTraversal` (drill first, flip last, like `.record`): reads yield the typed focus, writes
  * render the whole modified document as JSON bytes. [[render]] is the focus-as-standalone-JSON
  * terminal for the read side.
  *
  * The reverse cursor is the [[avro]] extension on `JsoniterPrism`: drill a JSON document with the
  * jsoniter sugar and the '''drilled focus itself''' converts — reads structurally parse the
  * focused slice under the focus codec's schema and yield its Avro binary encoding, writes accept
  * Avro binary, render it structurally, and splice the JSON slice back. Both directions are
  * schema-directed walks; no typed value is ever materialised.
  *
  * ==Parsing conventions (JSON bytes → record)==
  *
  * [[record]]'s `getOption` (and the `.avro` face's read) is the '''strict''' schema-directed
  * inverse — a streaming `JsonReader` walk, no AST — mirroring `AvroJson`'s parsing conventions: a
  * record object's key set must equal the schema's field names exactly; `enum` requires a schema
  * symbol; `fixed` the exact byte length; `bytes` / `fixed` the signed-byte-int array rendering; a
  * `union` is matched '''first branch that parses wins''' (the branch slice is captured raw once
  * and re-attempted per branch — a streaming reader cannot backtrack). Two deliberate
  * strictness-only divergences from the AST parse: numeric syntax is token-level (`1.0` misses an
  * `int` schema where circe's `toInt` accepts it), and a duplicate object key misses (circe's
  * `JsonObject` silently de-duplicates before `AvroJson` ever sees it).
  *
  * ==Non-goals (deliberate)==
  *
  * Same as `AvroJson`: the bridge sees only the '''runtime''' Avro value, never the logical type —
  * an `Instant` stored as timestamp-millis renders as a JSON number, not an ISO-8601 string.
  *
  * @groupname prism Bidirectional prism (JSON bytes ↔ record)
  * @groupprio prism 0
  * @groupname diagonal Codec diagonals (tearFrom / mendFrom of valuePrism)
  * @groupprio diagonal 1
  * @groupname base Structural walk
  * @groupprio base 2
  * @groupname optic Read optic (Avro bytes → JSON bytes)
  * @groupprio optic 3
  */
object AvroJsoniter:

  /** The bidirectional bridge — `AvroJson.record` without the AST: a
    * `Prism[JsoniterBytes, IndexedRecord]` for `schema`. Reading (`getOption` / `to`) is the strict
    * schema-directed streaming parse — see ''Parsing conventions''; it misses (`Left` of the
    * untouched JSON bytes) on anything the schema does not pin. Writing (`reverseGet`) is the total
    * structural walk [[avroToJson]].
    * @group prism
    */
  def record(
      schema: Schema
  ): MendTearPrism[JsoniterBytes, JsoniterBytes, IndexedRecord, IndexedRecord] =
    Prism[Array[Byte], IndexedRecord](
      json => jsonToRecord(json, schema).toRight(json),
      avroToJson,
    )

  /** Strict schema-directed structural parse of a JSON document into a generic record —
    * [[record]]'s `getOption`. `None` on anything the schema does not pin; no typed case class and
    * no AST in the middle.
    * @group prism
    */
  def jsonToRecord(json: JsoniterBytes, schema: Schema): Option[IndexedRecord] =
    parseSlice(json, schema) match
      case Some(r: IndexedRecord) => Some(r)
      case _                      => None

  /** The fundamental codec diagonal — the prisms below are this one with their '''input''' slots
    * pre-composed via `MendTearPrism.tearFrom` / `mendFrom`. Tears an Avro '''generic runtime
    * value''' into a typed `A` via the codec's decode; a miss surrenders the '''structural
    * JSON-bytes view''' of the value (the [[avroToJson]] walk generalised to any value) instead of
    * the raw input, so a payload that is valid Avro but not a valid `A` still lands somewhere
    * inspectable. The mend renders any generic value back as JSON bytes — the same structural walk.
    * `AvroJson.valuePrism` with [[JsoniterBytes]] in the `Json` slot.
    * @group diagonal
    */
  def valuePrism[A](using codec: AvroCodec[A]): MendTearPrism[Any, JsoniterBytes, A, Any] =
    Prism.pPrism[Any, JsoniterBytes, A, Any](
      value => codec.decodeEither(value).left.map(_ => valueToJson(value)),
      valueToJson,
    )

  /** Typed-both-ways byte diagonal — tear Avro '''payload bytes''' ([[AvroBytes]]) into a typed
    * `A`, mend `A` out as JSON bytes through the codec's encode, so
    * `modify(f: A => A): AvroBytes => JsoniterBytes` works in one hop. The counterpart of
    * `AvroJson.bytesPrism`; same parse contract as [[bytesToJson]] (position-based under
    * `codec.schema`, no writer/reader resolution).
    * @group diagonal
    */
  def bytesPrism[A](using codec: AvroCodec[A]): MendTearPrism[AvroBytes, JsoniterBytes, A, A] =
    valuePrism[A].tearFrom(parse(codec.schema)).mendFrom(codec.encode)

  /** Record-sourced diagonal — for streams already resolved to generic records (e.g. the output of
    * `ConfluentWire.recordReader`): tear an `IndexedRecord` into a typed `A`, mend an
    * `IndexedRecord` out as JSON bytes.
    * @group diagonal
    */
  def recordPrism[A](using
      AvroCodec[A]
  ): MendTearPrism[IndexedRecord, JsoniterBytes, A, IndexedRecord] =
    valuePrism[A].tearFrom((r: IndexedRecord) => r).mendFrom((r: IndexedRecord) => r)

  /** The whole substance of the bridge: the recursive structural walk of an Avro generic record,
    * rendered to UTF-8 JSON bytes via jsoniter's `JsonWriter`. Allocates no typed case class and no
    * AST; field order is the schema's field declaration order.
    * @group base
    */
  def avroToJson(record: IndexedRecord): JsoniterBytes =
    valueToJson(record)

  /** The read optic: [[avroToJson]] composed onto a bytes → record read
    * [[dev.constructive.eo.optics.Getter]] — a total `Getter[Array[Byte], Array[Byte]]` from Avro
    * payload bytes to JSON document bytes. The AST-free counterpart of `AvroJson.bytesToJson`.
    *
    * The `schema` must be the exact writer schema the bytes were encoded under: the parse is
    * position-based and does no writer/reader resolution, so a mismatched schema silently misreads.
    * For a mixed-schema stream, resolve writer → reader first (e.g.
    * `dev.constructive.eo.avro.ConfluentWire.recordReader`) and walk the resolved record with
    * [[avroToJson]].
    * @group optic
    */
  def bytesToJson(schema: Schema): Getter[AvroBytes, JsoniterBytes] =
    new Getter(parseRecord(schema)).andThen(new Getter(avroToJson))

  /** Focus-as-JSON terminal: render a typed focus as a '''standalone JSON document''' through the
    * codec's encode + the structural walk. Compose it after any drilled optic to read just the
    * focused field as JSON bytes:
    *
    * {{{
    *   codecPrism[Person].field(_.address).andThen(AvroJsoniter.render[Address])
    *     .getOption(avroBytes)                       // Option[JsoniterBytes] of the address alone
    * }}}
    *
    * For the whole-document face (drilled writes included) use the [[json]] extension instead.
    * @group optic
    */
  def render[A](using codec: AvroCodec[A]): Getter[A, JsoniterBytes] =
    new Getter(a => valueToJson(codec.encode(a)))

  /** [[avroToJson]] generalised to any Avro generic runtime value — [[valuePrism]]'s mend, its
    * tear's miss fallback, and the `.avro` face's write-side render.
    */
  private[jsoniter] def valueToJson(value: Any): JsoniterBytes =
    writeToArray[Any](value)(using anyCodec)

  /** Position-based binary parse to a generic value under a single schema — [[bytesPrism]]'s
    * `tearFrom`. Routed through `AvroBinaryCursor` like `AvroJson.parse`, so the reader comes from
    * the shared per-thread cache instead of a closure-held instance that every thread using the
    * optic would share.
    */
  private[jsoniter] def parse(schema: Schema): AvroBytes => Any =
    bytes =>
      AvroBinaryCursor
        .leaves
        .read(bytes, 0, bytes.length, schema, schema, threadLocalStorage = true)

  /** Parse Avro binary payload bytes to a generic `IndexedRecord` under `schema` — the
    * parse-to-generic-record step behind [[bytesToJson]] and the `.json` faces. Same shared
    * per-thread reader cache as [[parse]].
    */
  private[jsoniter] def parseRecord(schema: Schema): AvroBytes => IndexedRecord =
    bytes =>
      AvroBinaryCursor
        .records
        .read(bytes, 0, bytes.length, schema, schema, threadLocalStorage = true)

  /** Write-only `JsonValueCodec` hosting the walk — the adapter jsoniter's `writeToArray` needs.
    * The decode side is unreachable by construction (nothing in this object reads).
    */
  private val anyCodec: JsonValueCodec[Any] = new JsonValueCodec[Any]:
    def encodeValue(value: Any, out: JsonWriter): Unit = writeValue(value, out)
    def decodeValue(in: JsonReader, default: Any): Any =
      in.decodeError("AvroJsoniter renders only; no JSON → value decode")
    def nullValue: Any = null.asInstanceOf[Any]

  private def writeRecord(record: IndexedRecord, out: JsonWriter): Unit =
    val fields = record.getSchema.getFields
    out.writeObjectStart()
    (0 until fields.size).foreach { i =>
      out.writeKey(fields.get(i).name)
      writeValue(record.get(i), out)
    }
    out.writeObjectEnd()

  /** Dispatch on the Avro runtime type — mirrors `AvroJson.valueToJson`. Order matters: structured
    * / enum / fixed cases precede `CharSequence` (`Utf8` is also a `CharSequence`).
    */
  private def writeValue(value: Any, out: JsonWriter): Unit = value match
    case null                   => out.writeNull()
    case r: IndexedRecord       => writeRecord(r, out)
    case m: java.util.Map[?, ?] =>
      out.writeObjectStart()
      m.forEach { (k, v) =>
        out.writeKey(k.toString)
        writeValue(v, out)
      }
      out.writeObjectEnd()
    case l: java.util.List[?] =>
      out.writeArrayStart()
      l.forEach(v => writeValue(v, out))
      out.writeArrayEnd()
    case e: GenericEnumSymbol[?] => out.writeVal(e.toString)
    case f: GenericFixed         => writeBytesField(f.bytes, out)
    case b: ByteBuffer           => writeBytesField(byteBufferBytes(b), out)
    case s: CharSequence         => out.writeVal(s.toString)
    case b: java.lang.Boolean    => out.writeVal(b.booleanValue)
    case i: java.lang.Integer    => out.writeVal(i.intValue)
    case l: java.lang.Long       => out.writeVal(l.longValue)
    case f: java.lang.Float      =>
      // jsoniter's writeVal(Float) throws on non-finite; circe's fromFloatOrNull yields Null
      if f.isNaN || f.isInfinite then out.writeNull() else out.writeVal(f.floatValue)
    case d: java.lang.Double =>
      if d.isNaN || d.isInfinite then out.writeNull() else out.writeVal(d.doubleValue)
    case other => out.writeVal(other.toString)

  /** circe's `Encoder[Array[Byte]]` convention: a JSON array of signed byte values. */
  private def writeBytesField(bytes: Array[Byte], out: JsonWriter): Unit =
    out.writeArrayStart()
    bytes.foreach(b => out.writeVal(b.toInt))
    out.writeArrayEnd()

  /** Read a `ByteBuffer`'s remaining bytes without disturbing its position. */
  private def byteBufferBytes(bb: ByteBuffer): Array[Byte] =
    val dup = bb.duplicate()
    val bytes = new Array[Byte](dup.remaining())
    dup.get(bytes)
    bytes

  // ---- JSON bytes → Avro (the strict streaming parse) ----------------

  /** Parse a JSON slice under `schema` into a generic Avro runtime value — `Some(null)` is a
    * legitimate hit (a `null` schema / union branch), `None` the strict miss. Whole-slice
    * strictness (no trailing input) comes from the read's end-of-input check.
    *
    * MUST be the reentrant read: [[readUnion]] recurses through here mid-parse, and the pooled
    * `readFromArray` would hand the nested parse the OUTER call's thread-local reader, corrupting
    * its position.
    */
  private[jsoniter] def parseSlice(json: Array[Byte], schema: Schema): Option[Any] =
    try Some(readFromArrayReentrant[Any](json)(using new SchemaCodec(schema)))
    catch case NonFatal(_) => None

  /** Schema-directed `JsonValueCodec` — the adapter that lets `readFromArray` drive the
    * [[readValue]] walk (and, symmetrically, `writeToArray` the [[writeValue]] walk).
    */
  final private class SchemaCodec(schema: Schema) extends JsonValueCodec[Any]:
    def decodeValue(in: JsonReader, default: Any): Any = readValue(in, schema)
    def encodeValue(value: Any, out: JsonWriter): Unit = writeValue(value, out)
    def nullValue: Any = null.asInstanceOf[Any]

  /** Schema-directed streaming inverse of [[writeValue]] — `in.decodeError` (an exception caught by
    * [[parseSlice]]) is the strict miss.
    */
  private def readValue(in: JsonReader, schema: Schema): Any =
    schema.getType match
      case Schema.Type.RECORD => readRecord(in, schema)
      case Schema.Type.UNION  => readUnion(in, schema)
      case Schema.Type.ARRAY  => readArray(in, schema.getElementType)
      case Schema.Type.MAP    => readMap(in, schema.getValueType)
      case Schema.Type.ENUM   =>
        // NB a null `default` makes jsoniter treat JSON null as a decode error — the strictness
        // we want (null only ever matches a NULL schema / union branch)
        val s = in.readString(null.asInstanceOf[String])
        if schema.getEnumSymbols.contains(s) then new GenericData.EnumSymbol(schema, s)
        else in.decodeError(s"not a symbol of ${schema.getFullName}")
      case Schema.Type.FIXED =>
        val bytes = readByteArray(in)
        if bytes.length == schema.getFixedSize then new GenericData.Fixed(schema, bytes)
        else in.decodeError(s"fixed ${schema.getFullName} needs ${schema.getFixedSize} bytes")
      case Schema.Type.BYTES   => ByteBuffer.wrap(readByteArray(in))
      case Schema.Type.STRING  => in.readString(null.asInstanceOf[String])
      case Schema.Type.INT     => Int.box(in.readInt())
      case Schema.Type.LONG    => Long.box(in.readLong())
      case Schema.Type.FLOAT   => Float.box(in.readFloat())
      case Schema.Type.DOUBLE  => Double.box(in.readDouble())
      case Schema.Type.BOOLEAN => Boolean.box(in.readBoolean())
      case Schema.Type.NULL    =>
        // readNullOrError refuses a null default, so consume "null" against a sentinel and
        // return the actual null the Avro model wants
        if in.isNextToken('n'.toByte) then
          in.readNullOrError(NullSentinel, "expected JSON null")
          nullAny
        else in.decodeError("expected JSON null")

  private object NullSentinel

  private def nullAny: Any = null.asInstanceOf[Any]

  /** Strict exact-cover record parse: every key must be a schema field (any order), no duplicate,
    * none missing.
    */
  private def readRecord(in: JsonReader, schema: Schema): IndexedRecord =
    if !in.isNextToken('{'.toByte) then in.decodeError("expected JSON object")
    val fields = schema.getFields
    val rec = new GenericData.Record(schema)
    val seen = new Array[Boolean](fields.size)
    @tailrec def loop(count: Int): Int =
      val field = schema.getField(in.readKeyAsString())
      if (field eq null) || seen(field.pos) then
        in.decodeError(s"not exactly the fields of ${schema.getFullName}")
      seen(field.pos) = true
      rec.put(field.pos, readValue(in, field.schema))
      if in.isNextToken(','.toByte) then loop(count + 1) else count + 1
    val count =
      if in.isNextToken('}'.toByte) then 0
      else
        in.rollbackToken()
        val n = loop(0)
        if !in.isCurrentToken('}'.toByte) then in.objectEndOrCommaError()
        n
    if count != fields.size then
      in.decodeError(s"expected exactly the ${fields.size} fields of ${schema.getFullName}")
    rec

  /** First branch that parses wins (mirrors `AvroJson`): the slice is captured raw ONCE, then each
    * branch attempts it on a fresh reader — a streaming reader cannot backtrack across branches.
    */
  private def readUnion(in: JsonReader, schema: Schema): Any =
    val raw = in.readRawValAsBytes()
    schema.getTypes.asScala.iterator.map(parseSlice(raw, _)).collectFirst {
      case Some(v) => v
    } match
      case Some(v) => v
      case None    => in.decodeError("no union branch parses")

  private def readArray(in: JsonReader, elem: Schema): java.util.ArrayList[Any] =
    if !in.isNextToken('['.toByte) then in.decodeError("expected JSON array")
    val list = new java.util.ArrayList[Any]()
    if in.isNextToken(']'.toByte) then list
    else
      in.rollbackToken()
      @tailrec def loop(): Unit =
        list.add(readValue(in, elem))
        if in.isNextToken(','.toByte) then loop()
      loop()
      if in.isCurrentToken(']'.toByte) then list else in.arrayEndOrCommaError()

  private def readMap(in: JsonReader, value: Schema): java.util.LinkedHashMap[String, Any] =
    if !in.isNextToken('{'.toByte) then in.decodeError("expected JSON object")
    val map = new java.util.LinkedHashMap[String, Any]()
    if in.isNextToken('}'.toByte) then map
    else
      in.rollbackToken()
      @tailrec def loop(): Unit =
        map.put(in.readKeyAsString(), readValue(in, value))
        if in.isNextToken(','.toByte) then loop()
      loop()
      if in.isCurrentToken('}'.toByte) then map else in.objectEndOrCommaError()

  /** Inverse of [[writeBytesField]]: a JSON array of signed byte ints (each integral, in
    * `[-128, 127]` — `readByte` enforces both) back to raw bytes.
    */
  private def readByteArray(in: JsonReader): Array[Byte] =
    if !in.isNextToken('['.toByte) then in.decodeError("expected JSON array of byte values")
    if in.isNextToken(']'.toByte) then Array.emptyByteArray
    else
      in.rollbackToken()
      val buf = scala.collection.mutable.ArrayBuffer.empty[Byte]
      @tailrec def loop(): Unit =
        buf += in.readByte()
        if in.isNextToken(','.toByte) then loop()
      loop()
      if in.isCurrentToken(']'.toByte) then buf.toArray else in.arrayEndOrCommaError()

end AvroJsoniter

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroPrism]] — the prism's `.record`
  * face with Avro bytes in and '''JSON document bytes out'''. Drill with the full cursor sugar
  * (`.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` / Dynamic selection) and flip last:
  *
  * {{{
  *   import dev.constructive.eo.avro.codecPrism
  *   import dev.constructive.eo.avro.jsoniter.*
  *
  *   codecPrism[Person].name.json.modify(_.toUpperCase)(avroBytes)  // JsoniterBytes of the WHOLE
  *                                                                  // doc, name uppercased
  * }}}
  *
  * Reads (`.getOption`) still yield the typed focus `A`; every write (`.modify` / `.replace`)
  * rebuilds the record through the record face's single-walk writer and renders it via
  * [[AvroJsoniter.avroToJson]] — no byte splice, no re-parse. Routing through `.record` (rather
  * than the byte-span face) is what makes `.at(i)` navigable here: index steps are unsupported by
  * the byte-span locate but fine on the record walk. A path / branch / decode '''Miss''' renders
  * the document unchanged; genuinely '''malformed bytes throw''' at the eager parse (the output
  * format changes, so there is no byte-face-style silent pass-through). Implemented as
  * `Optic.outerProfunctor.dimap`, so the generic capability surface applies at this terminal hop.
  */
extension [A](p: AvroPrism[A])

  def json: Optic[AvroBytes, JsoniterBytes, A, A, Affine] =
    Optic
      .outerProfunctor[A, A, Affine]
      .dimap(p.record)(AvroJsoniter.parseRecord(p.rootSchemaCached))(AvroJsoniter.avroToJson)

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroTraversal]] — `.each`'s
  * multi-focus counterpart of the prism extension above: `.foldMap` / `.all` read the typed
  * elements, `.modify` splices every element (this one stays on the byte face — the traversal's
  * record face is the Ior diagnostic surface, not an `Optic`) and renders the whole spliced
  * document as JSON bytes. Reads on malformed bytes fold zero foci (byte-walk semantics); a write
  * on them throws at the render.
  */
extension [A](t: AvroTraversal[A])

  def json: Optic[AvroBytes, JsoniterBytes, A, A, MultiFocus[PSVec]] =
    Optic
      .outerProfunctor[A, A, MultiFocus[PSVec]]
      .dimap(t)(identity[AvroBytes])(AvroJsoniter.bytesToJson(t.rootSchemaCached).get)

/** Avro-carried face of a drilled [[dev.constructive.eo.jsoniter.JsoniterPrism]] — the reverse
  * cursor: drill a JSON document with the full jsoniter sugar (`.field(_.x)` / `.at(i)` / Dynamic
  * selection) and flip last; the '''drilled focus itself''' is the unit of conversion:
  *
  * {{{
  *   import dev.constructive.eo.jsoniter.JsoniterPrism
  *   import dev.constructive.eo.avro.jsoniter.*
  *
  *   val nameA = JsoniterPrism[Person].name.avro   // Optic[JsoniterBytes, JsoniterBytes,
  *                                                 //       AvroBytes, AvroBytes, Affine]
  *   nameA.getOption(jsonBytes)                    // Some(Avro binary of the name alone)
  *   nameA.replace(avroName)(jsonBytes)            // JSON doc with the slice spliced back
  * }}}
  *
  * Both directions are '''structural, schema-directed''' walks under the focus codec's schema (the
  * `AvroCodec[A]` is schema evidence only — no typed `A` is ever materialised, matching
  * `codecPrism`'s doctrine): reads capture the focused slice raw ([[JsoniterPrism.raw]]) and run
  * the strict streaming parse (see ''Parsing conventions'' on [[AvroJsoniter]]) before encoding to
  * Avro binary — a slice the schema does not pin is a '''Miss''', exactly like a path miss; writes
  * parse the Avro binary to a generic value, render it as a JSON slice, and splice. A write whose
  * Avro bytes do not parse under the schema passes the document through unchanged (`from` has no
  * failure channel).
  */
extension [A](p: JsoniterPrism[A])

  def avro(using
      codec: AvroCodec[A]
  ): Optic[JsoniterBytes, JsoniterBytes, AvroBytes, AvroBytes, Affine] =
    new AvroSliceFace(p.raw, codec.schema)

/** Implementation of the `.avro` face: re-focuses the drilled prism on its raw slice and converts
  * slice ↔ Avro binary at the seam. The leftover `X` is the raw prism's own (source bytes + span),
  * so writes splice without re-walking.
  */
final private class AvroSliceFace(
    private val rawPrism: JsoniterPrism[Array[Byte]],
    private val schema: Schema,
) extends Optic[JsoniterBytes, JsoniterBytes, AvroBytes, AvroBytes, Affine]:

  type X = rawPrism.X

  def to(json: JsoniterBytes): Affine[X, AvroBytes] =
    rawPrism.to(json) match
      case m: Affine.Miss[X]             => new Affine.Miss[X](m.fst)
      case h: Affine.Hit[X, Array[Byte]] =>
        AvroJsoniter.parseSlice(h.b, schema) match
          case Some(value) =>
            new Affine.Hit[X, AvroBytes](h.snd, AvroBinaryCursor.writeDatum(value, schema))
          case None => new Affine.Miss[X](h.snd._1)

  def from(aff: Affine[X, AvroBytes]): JsoniterBytes =
    aff match
      case m: Affine.Miss[X]           => m.fst
      case h: Affine.Hit[X, AvroBytes] =>
        try
          val value = AvroJsoniter.parse(schema)(h.b)
          rawPrism.from(new Affine.Hit[X, Array[Byte]](h.snd, AvroJsoniter.valueToJson(value)))
        // ponytail: silent pass-through on unparseable Avro bytes — from has no failure channel
        catch case NonFatal(_) => h.snd._1

end AvroSliceFace
