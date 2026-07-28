package dev.constructive.eo.avro.circe

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

import dev.constructive.eo.avro.{AvroBinaryCursor, AvroBytes, AvroCodec, AvroPrism, AvroTraversal}
import dev.constructive.eo.circe.JsonPrism
import dev.constructive.eo.data.{Affine, MultiFocus, PSVec}
import dev.constructive.eo.optics.{Getter, MendTearPrism, Optic, Prism}
import io.circe.Json
import java.nio.ByteBuffer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericEnumSymbol, GenericFixed, IndexedRecord}

/** Structural Avro ↔ circe bridge: move between an Avro generic runtime value and a circe
  * [[io.circe.Json]] document '''without''' a typed case class in the middle. The Avro mirror of
  * `dev.constructive.eo.circe`. Lives inside `cats-eo-avro` with circe as an `Optional` dependency
  * — the API surface names `io.circe.Json`, so any caller already depends on circe directly; add
  * `circe-core` to use this package.
  *
  * The bidirectional entry points are [[record]], a lawful `Prism[Json, IndexedRecord]` per schema
  * — `getOption` is the strict schema-guided parse (Json → record, misses on any shape the schema
  * does not pin), `reverseGet` the total structural walk [[avroToJson]] (record → Json) — and its
  * polymorphic diagonal family rooted at [[valuePrism]] — [[pPrism]], [[bytesPrism]],
  * [[recordPrism]] and [[pRecord]] pre-compose its inputs via `tearFrom` / `mendFrom`, tearing
  * generic values / payload bytes / records into a typed `A` and mending back out as `Json`.
  *
  * ==Rendering conventions (record → Json)==
  *
  * [[avroToJson]] is a purely structural walk of the Avro value model:
  *   - `IndexedRecord` → object (field name from `getSchema.getFields`, recurse on value, schema
  *     declaration order);
  *   - `java.util.Map` → object (stringify keys, recurse on values) — matches
  *     `Encoder[Map[String, ?]]`;
  *   - `java.util.List` → array (recurse);
  *   - `CharSequence` (incl. `org.apache.avro.util.Utf8`) → `Json.fromString`;
  *   - `Integer` / `Long` → `Json.fromLong` (`Json.fromInt(i) == Json.fromLong(i.toLong)`);
  *   - `Double` → `Json.fromDoubleOrNull` (matches circe's `Encoder[Double]`);
  *   - `Float` → `Json.fromFloatOrNull` (matches circe's `Encoder[Float]`). NB widening
  *     float→double first would change the value (`0.1f` → `0.10000000149…`) and break `JsonNumber`
  *     equality, so the float branch must not;
  *   - `Boolean` → `Json.fromBoolean`; a resolved `null` union branch → `Json.Null`;
  *   - `GenericEnumSymbol` → `Json.fromString`;
  *   - `ByteBuffer` / `GenericFixed` → array of signed byte ints (circe's `Encoder[Array[Byte]]`
  *     convention);
  *   - any other runtime type → `Json.fromString(value.toString)` — a lenient last resort (the walk
  *     is total), not a convention to rely on.
  *
  * Unions are resolved at the value level (the runtime value IS the branch), so dispatch on the
  * runtime type needs no union special-casing.
  *
  * ==Parsing conventions (Json → record, the prism's `getOption`)==
  *
  * The parse inverts the walk under the schema, and is '''strict''' so the prism stays lawful
  * (`getOption(j).map(reverseGet)` must reproduce `j`, so nothing may be guessed, defaulted, or
  * dropped):
  *   - a record object's key set must equal the schema's field names exactly — an extra key would
  *     be silently dropped on re-render, a missing one has NO schema default applied; both miss;
  *   - `int` / `long` parse via `JsonNumber.toInt` / `toLong` — non-integral or out-of-range
  *     numbers miss; `float` / `double` accept any JSON number (`toFloat` / `toDouble`);
  *   - `enum` requires a string among the schema's symbols; `fixed` requires the exact byte length;
  *     `bytes` / `fixed` parse the signed-byte-int array rendering;
  *   - a `union` is matched '''first branch that parses wins''' (`Json.Null` only ever matches a
  *     `null` branch). For the ubiquitous `["null", X]` this is exact; a union whose branches
  *     overlap structurally (e.g. `["int", "long"]`) resolves to the first — and the prism laws
  *     only hold up to that choice.
  *
  * ==Non-goals (deliberate)==
  *
  * The bridge sees only the '''runtime''' Avro value, never the logical type or a case-class field
  * type — so a source whose circe encoder does a '''non-structural''' transform is not reproducible
  * structurally and the bridge is not a drop-in there. Two representative cases:
  *   - '''logical types''': an `Instant` stored as timestamp-millis is a `long` at runtime; the
  *     bridge renders `Json.fromLong`, not the ISO-8601 string a `Encoder[Instant]` would emit;
  *   - '''stringified numerics''': an encoder that renders a `long` as a decimal '''string''' has
  *     no structural counterpart — the bridge renders `Json.fromLong`.
  *
  * These are the caller's concern (post-process the `Json`, or decode the typed value): the walk is
  * defined by the wire shape, not the intended semantic type.
  *
  * ==Drilled cursors (`.json` and `.avro` faces)==
  *
  * The full `AvroPrism` cursor sugar — `.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` /
  * `.each` / Dynamic selection — reaches this bridge through the [[json]] extensions on `AvroPrism`
  * / `AvroTraversal` (drill first, flip last, like `.record`): reads yield the typed focus, writes
  * render the whole modified document as `Json`. [[render]] is the focus-as-standalone-`Json`
  * terminal for the read side. The reverse cursor is the [[avro]] extension on
  * `dev.constructive.eo.circe.JsonPrism`: the drilled focus itself converts — subtree ↔ Avro
  * binary, both directions the structural walks above. Same face family as
  * `dev.constructive.eo.avro.jsoniter`, landing on the AST instead of bytes; the `.avro` face is
  * why `cats-eo-circe` is a second `Optional` dependency next to circe-core.
  *
  * @groupname prism Bidirectional prism (Json ↔ record)
  * @groupprio prism 0
  * @groupname diagonal Codec diagonals (tearFrom / mendFrom of valuePrism)
  * @groupprio diagonal 1
  * @groupname base Structural walk
  * @groupprio base 2
  * @groupname optic Read optic (bytes → Json)
  * @groupprio optic 3
  */
object AvroJson:

  /** The bidirectional bridge: a `Prism[Json, IndexedRecord]` for `schema`. Reading (`getOption` /
    * `to`) is the strict schema-guided parse — see ''Parsing conventions''; it misses (`Left` of
    * the untouched `Json`) on anything the schema does not pin. Writing (`reverseGet`) is the total
    * structural walk [[avroToJson]] — use it directly for the record → Json direction.
    * @group prism
    */
  def record(schema: Schema): MendTearPrism[Json, Json, IndexedRecord, IndexedRecord] =
    Prism[Json, IndexedRecord](
      json => jsonToRecord(json, schema).toRight(json),
      avroToJson,
    )

  /** The fundamental codec diagonal — every prism below is this one with its inputs pre-composed
    * via `MendTearPrism.tearFrom` / `mendFrom`. Tears an Avro '''generic runtime value''' into a
    * typed `A` via the codec's decode; a miss surrenders the '''structural Json view''' of the
    * value (the [[avroToJson]] walk generalised to any value) instead of the raw input, so a
    * payload that is valid Avro but not a valid `A` still lands somewhere inspectable. The mend
    * renders any generic value back as `Json` — the same structural walk.
    *
    * The family below varies the two '''input''' slots only: what the tear consumes (generic value
    * / payload bytes / record) and what the mend accepts (generic value / `A` / `IndexedRecord`).
    * The outputs — typed focus `A`, Json fallback — are fixed here.
    * @group diagonal
    */
  def valuePrism[A](using codec: AvroCodec[A]): MendTearPrism[Any, Json, A, Any] =
    Prism.pPrism[Any, Json, A, Any](
      value => codec.decodeEither(value).left.map(_ => valueToJson(value)),
      valueToJson,
    )

  /** [[valuePrism]] torn from a binary parse — tear Avro '''payload bytes''' into a typed `A`, mend
    * a generic record out as `Json`.
    *
    * Same parse contract as [[bytesToJson]]: position-based under `codec.schema`, no writer/reader
    * resolution — malformed or schema-mismatched bytes throw rather than miss. For a mixed-schema
    * stream use the writer-schema overload of [[bytesPrism]] (or `ConfluentWire.resolvingBytes`).
    * @group diagonal
    */
  def pPrism[A](using codec: AvroCodec[A]): MendTearPrism[AvroBytes, Json, A, IndexedRecord] =
    valuePrism[A]
      .tearFrom(AvroBinaryCursor.leaves.parser(codec.schema))
      .mendFrom((r: IndexedRecord) => r)

  /** Typed-both-ways byte diagonal — [[pPrism]] with the mend routed through the codec's encode, so
    * `modify(f: A => A): AvroBytes => Json` works in one hop with no generic record at the call
    * site.
    * @group diagonal
    */
  def bytesPrism[A](using codec: AvroCodec[A]): MendTearPrism[AvroBytes, Json, A, A] =
    valuePrism[A].tearFrom(AvroBinaryCursor.leaves.parser(codec.schema)).mendFrom(codec.encode)

  /** [[bytesPrism]] for a stream written under a '''different''' (but compatible) writer schema:
    * the tear resolves writer → `codec.schema` (Avro schema resolution — field reordering,
    * defaults, promotions) before decoding.
    * @group diagonal
    */
  def bytesPrism[A](writer: Schema)(using
      codec: AvroCodec[A]
  ): MendTearPrism[AvroBytes, Json, A, A] =
    valuePrism[A]
      .tearFrom(AvroBinaryCursor.leaves.parser(writer, codec.schema))
      .mendFrom(codec.encode)

  /** Record-sourced diagonal — for streams already resolved to generic records (e.g. the output of
    * `ConfluentWire.recordReader`): tear an `IndexedRecord` into a typed `A`, mend `A` out as
    * `Json`.
    * @group diagonal
    */
  def recordPrism[A](using codec: AvroCodec[A]): MendTearPrism[IndexedRecord, Json, A, A] =
    valuePrism[A].tearFrom((r: IndexedRecord) => r).mendFrom(codec.encode)

  /** Untyped byte diagonal — no codec, no case class: [[bytesPrism]] instantiated at
    * `IndexedRecord` via a trivial per-schema codec (encode = identity, decode = runtime-type
    * check). For registry / dynamic-schema consumers; effectively [[bytesToJson]] upgraded to a
    * writable prism.
    * @group diagonal
    */
  def pRecord(schema: Schema): MendTearPrism[AvroBytes, Json, IndexedRecord, IndexedRecord] =
    bytesPrism[IndexedRecord](using recordCodec(schema))

  /** The trivial `AvroCodec[IndexedRecord]` that lets [[pRecord]] reuse the typed family. */
  private def recordCodec(schema0: Schema): AvroCodec[IndexedRecord] =
    new AvroCodec[IndexedRecord]:
      def schema: Schema = schema0
      def encode(r: IndexedRecord): Any = r
      def decodeEither(any: Any): Either[Throwable, IndexedRecord] = any match
        case r: IndexedRecord => Right(r)
        case other            => Left(new IllegalArgumentException(s"not an IndexedRecord: $other"))

  /** The whole substance of the write side: a recursive structural walk of an Avro generic record
    * into a circe [[io.circe.Json]] object. Allocates no typed case class; field order is the
    * schema's field declaration order. Also [[record]]'s `reverseGet`.
    * @group base
    */
  def avroToJson(record: IndexedRecord): Json =
    val fields = record.getSchema.getFields
    Json.fromFields(
      (0 until fields.size).map(i => fields.get(i).name -> valueToJson(record.get(i)))
    )

  /** Dispatch on the Avro runtime type. Order matters: structured / enum / fixed cases precede
    * `CharSequence` (`Utf8` is also a `CharSequence`).
    */
  private[circe] def valueToJson(value: Any): Json = value match
    case null             => Json.Null
    case r: IndexedRecord => avroToJson(r)
    // entry ITERATION order preserved: mapping through an intermediate scala Map would re-hash
    // (reordering entries, deduping stringified-equal keys) — order is visible to the prism's
    // round-trip law, where Avro's binary map encoding is order-sensitive
    case m: java.util.Map[?, ?] =>
      Json.fromFields(m.asScala.iterator.map((k, v) => k.toString -> valueToJson(v)).toList)
    case l: java.util.List[?]    => Json.fromValues(l.asScala.map(valueToJson))
    case e: GenericEnumSymbol[?] => Json.fromString(e.toString)
    case f: GenericFixed         => bytesFieldToJson(f.bytes)
    case b: ByteBuffer           => bytesFieldToJson(AvroBinaryCursor.byteBufferBytes(b))
    case s: CharSequence         => Json.fromString(s.toString)
    case b: java.lang.Boolean    => Json.fromBoolean(b)
    case i: java.lang.Integer    => Json.fromLong(i.toLong)
    case l: java.lang.Long       => Json.fromLong(l)
    case f: java.lang.Float      => Json.fromFloatOrNull(f)
    case d: java.lang.Double     => Json.fromDoubleOrNull(d)
    case other                   => Json.fromString(other.toString)

  /** circe's `Encoder[Array[Byte]]` convention: a JSON array of signed byte values. */
  private def bytesFieldToJson(bytes: Array[Byte]): Json =
    Json.fromValues(bytes.map(b => Json.fromInt(b.toInt)))

  // ---- Json → Avro (the prism's read side) ---------------------------

  /** Strict schema-guided parse of a JSON object into a generic record — [[record]]'s `getOption`.
    * `None` on a non-object, a key set differing from the schema's field names, or any field value
    * the field schema rejects.
    */
  private def jsonToRecord(json: Json, schema: Schema): Option[IndexedRecord] =
    json.asObject.flatMap { obj =>
      val fields = schema.getFields
      // exact cover: every schema field present (checked per field below) and no extras
      // (same count + all present ⇒ same key set; circe's JsonObject has de-duplicated keys)
      if obj.size != fields.size then None
      else
        fields.asScala.foldLeft(Option(new GenericData.Record(schema))) { (acc, field) =>
          acc.flatMap(rec =>
            obj(field.name)
              .flatMap(jsonToValue(_, field.schema))
              .map { value => rec.put(field.pos, value); rec }
          )
        }
    }

  /** Schema-directed inverse of [[valueToJson]]: `None` is the prism miss. `Some(null)` is a
    * legitimate hit (a `null` schema / union branch).
    */
  private[circe] def jsonToValue(json: Json, schema: Schema): Option[Any] =
    schema.getType match
      case Schema.Type.RECORD => jsonToRecord(json, schema)
      case Schema.Type.UNION  =>
        // first branch that parses wins; Json.Null only ever matches the NULL branch
        schema.getTypes.asScala.iterator.map(jsonToValue(json, _)).collectFirst {
          case Some(v) =>
            v
        }
      case Schema.Type.ARRAY =>
        json
          .asArray
          .flatMap(
            _.foldLeft(Option(new java.util.ArrayList[Any]())) { (acc, elem) =>
              acc.flatMap(list =>
                jsonToValue(elem, schema.getElementType).map { v => list.add(v); list }
              )
            }
          )
      case Schema.Type.MAP =>
        json
          .asObject
          .flatMap(
            _.toIterable.foldLeft(Option(new java.util.LinkedHashMap[String, Any]())) {
              case (acc, (key, value)) =>
                acc.flatMap(map =>
                  jsonToValue(value, schema.getValueType).map { v => map.put(key, v); map }
                )
            }
          )
      case Schema.Type.ENUM =>
        json
          .asString
          .filter(schema.getEnumSymbols.contains)
          .map(new GenericData.EnumSymbol(schema, _))
      case Schema.Type.FIXED =>
        jsonBytes(json)
          .filter(_.length == schema.getFixedSize)
          .map(new GenericData.Fixed(schema, _))
      case Schema.Type.BYTES   => jsonBytes(json).map(ByteBuffer.wrap)
      case Schema.Type.STRING  => json.asString
      case Schema.Type.INT     => json.asNumber.flatMap(_.toInt).map(Int.box)
      case Schema.Type.LONG    => json.asNumber.flatMap(_.toLong).map(Long.box)
      case Schema.Type.FLOAT   => json.asNumber.map(n => Float.box(n.toFloat))
      case Schema.Type.DOUBLE  => json.asNumber.map(n => Double.box(n.toDouble))
      case Schema.Type.BOOLEAN => json.asBoolean.map(Boolean.box)
      case Schema.Type.NULL    => Option.when(json.isNull)(null)

  /** Inverse of [[bytesFieldToJson]]: a JSON array of signed byte ints (each in `[-128, 127]`,
    * integral) back to raw bytes; `None` on any other shape.
    */
  private def jsonBytes(json: Json): Option[Array[Byte]] =
    json
      .asArray
      .flatMap(
        _.foldLeft(Option(Vector.empty[Byte])) { (acc, elem) =>
          acc.flatMap(bytes => elem.asNumber.flatMap(_.toByte).map(bytes :+ _))
        }.map(_.toArray)
      )

  /** The read optic: the base [[avroToJson]] composed onto a bytes → record read
    * [[dev.constructive.eo.optics.Getter]]. The parse step reads payload bytes to a generic
    * `IndexedRecord` (no typed decode), and eo's fused `Getter.andThen(Getter)` maps the structural
    * walk over it, yielding a total `Getter[AvroBytes, Json]`.
    *
    * The `schema` must be the exact writer schema the bytes were encoded under: the parse is
    * position-based and does no writer/reader resolution, so a mismatched schema silently misreads.
    * For a mixed-schema stream, resolve writer→reader first (e.g. `ConfluentWire.recordReader` /
    * `ConfluentWire.resolvingBytes`) and walk the resolved record / bytes.
    * @group optic
    */
  def bytesToJson(schema: Schema): Getter[AvroBytes, Json] =
    new Getter(AvroBinaryCursor.records.parser(schema)).andThen(new Getter(avroToJson))

  /** Focus-as-JSON terminal: render a typed focus as a standalone [[io.circe.Json]] through the
    * codec's encode + the structural walk. Compose it after any drilled optic to read just the
    * focused field as `Json`:
    *
    * {{{
    *   codecPrism[Person].field(_.address).andThen(AvroJson.render[Address])
    *     .getOption(avroBytes)                       // Option[Json] of the address alone
    * }}}
    *
    * For the whole-document face (drilled writes included) use the [[json]] extension instead.
    * @group optic
    */
  def render[A](using codec: AvroCodec[A]): Getter[A, Json] =
    new Getter(a => valueToJson(codec.encode(a)))

end AvroJson

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroPrism]] — the circe sibling of the
  * jsoniter module's `.json` face, byte-identical in shape but landing on the [[io.circe.Json]]
  * AST: drill with the full cursor sugar (`.field(_.x)` / `.fields(...)` / `.at(i)` / `.union[B]` /
  * Dynamic selection) and flip last, like `.record`:
  *
  * {{{
  *   import dev.constructive.eo.avro.codecPrism
  *   import dev.constructive.eo.avro.circe.*
  *
  *   codecPrism[Person].name.json.modify(_.toUpperCase)(avroBytes)  // Json of the WHOLE doc,
  *                                                                  // name uppercased
  * }}}
  *
  * Reads (`.getOption`) still yield the typed focus `A`; every write (`.modify` / `.replace`)
  * rebuilds the record through the record face's single-walk writer and renders it via
  * [[AvroJson.avroToJson]]. Routing through `.record` is what makes `.at(i)` navigable (index steps
  * are unsupported by the byte-span locate) and gives single-walk writes. A path / branch / decode
  * Miss renders the document unchanged; genuinely malformed bytes throw at the eager parse.
  */
extension [A](p: AvroPrism[A])

  def json: Optic[AvroBytes, Json, A, A, Affine] =
    Optic
      .outerProfunctor[A, A, Affine]
      .dimap(p.record)(AvroBinaryCursor.records.parser(p.rootSchemaCached))(AvroJson.avroToJson)

/** JSON-carried face of a drilled [[dev.constructive.eo.avro.AvroTraversal]] — `.each`'s
  * multi-focus counterpart of the prism extension above: `.foldMap` / `.exists` read the typed
  * elements, `.modify` splices every element (byte face — the traversal's record face is the Ior
  * surface, not an `Optic`) and renders the whole spliced document as `Json`.
  */
extension [A](t: AvroTraversal[A])

  def json: Optic[AvroBytes, Json, A, A, MultiFocus[PSVec]] =
    Optic
      .outerProfunctor[A, A, MultiFocus[PSVec]]
      .dimap(t)(identity[AvroBytes])(AvroJson.bytesToJson(t.rootSchemaCached).get)

/** Avro-carried face of a drilled [[dev.constructive.eo.circe.JsonPrism]] — the reverse cursor,
  * mirror of the jsoniter `.avro` face: drill a `Json` document with the circe cursor sugar
  * (`.field(_.x)` / `.at(i)` / Dynamic selection) and flip last; the '''drilled focus itself''' is
  * the unit of conversion:
  *
  * {{{
  *   import dev.constructive.eo.circe.JsonPrism
  *   import dev.constructive.eo.avro.circe.*
  *
  *   val nameA = JsonPrism[Person].name.avro       // Optic[Json, Json,
  *                                                 //       AvroBytes, AvroBytes, Affine]
  *   nameA.getOption(jsonDoc)                      // Some(Avro binary of the name alone)
  *   nameA.replace(avroName)(jsonDoc)              // Json doc with the subtree spliced back
  * }}}
  *
  * Both directions are structural, schema-directed (the `AvroCodec[A]` is schema evidence only — no
  * typed `A` is ever materialised, matching `codecPrism`'s doctrine): reads take the focused
  * subtree raw ([[dev.constructive.eo.circe.JsonPrism.raw]]) and run the strict parse
  * [[AvroJson.jsonToValue]] before encoding to Avro binary — a subtree the schema does not pin is a
  * '''Miss''', exactly like a path miss; writes parse the Avro binary to a generic value, render it
  * with the structural walk, and splice the subtree back. A write whose Avro bytes do not parse
  * under the schema passes the document through unchanged (`from` has no failure channel).
  */
extension [A](p: JsonPrism[A])

  def avro(using codec: AvroCodec[A]): Optic[Json, Json, AvroBytes, AvroBytes, Affine] =
    new JsonAvroFace(p.raw, codec.schema)

/** Implementation of the `.avro` face: re-focuses the drilled prism on its raw subtree and converts
  * subtree ↔ Avro binary at the seam. `Snd[X]` retains the ORIGINAL subtree alongside the
  * sibling-preserving writer so a failed write can re-apply it (document unchanged) — unlike the
  * jsoniter face's span seam, circe's writer-only seam has no other route back to the source.
  */
final private class JsonAvroFace(
    private val rawPrism: JsonPrism[Json],
    private val schema: Schema,
) extends Optic[Json, Json, AvroBytes, AvroBytes, Affine]:

  type X = (Json, (Json, Json => Json))

  def to(json: Json): Affine[X, AvroBytes] =
    rawPrism.to(json) match
      case _: Affine.Miss[rawPrism.X]      => new Affine.Miss[X](json)
      case h: Affine.Hit[rawPrism.X, Json] =>
        AvroJson.jsonToValue(h.b, schema) match
          case Some(value) =>
            new Affine.Hit[X, AvroBytes]((h.b, h.snd), AvroBinaryCursor.writeDatum(value, schema))
          case None => new Affine.Miss[X](json)

  def from(aff: Affine[X, AvroBytes]): Json =
    aff match
      case m: Affine.Miss[X]           => m.fst
      case h: Affine.Hit[X, AvroBytes] =>
        val (slice, writer) = h.snd
        try writer(AvroJson.valueToJson(AvroBinaryCursor.leaves.parser(schema)(h.b)))
        // ponytail: silent pass-through on unparseable Avro bytes — from has no failure channel;
        // re-applying the retained original subtree reproduces the document unchanged
        catch case NonFatal(_) => writer(slice)

end JsonAvroFace
