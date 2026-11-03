package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import dev.constructive.eo.avro.circe.AvroJson
import dev.constructive.eo.avro.jsoniter.AvroJsoniter
import io.circe.Json
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumWriter, IndexedRecord}
import org.openjdk.jmh.annotations.*

/** Schema-defaults bench (issue #156): the cost of the lenient `recordWithDefaults` read against
  * the strict `record` read, on both structural bridges (circe AST and jsoniter streaming).
  *
  * The fixture is an operator-facing amendment body — the shape the issue reports: a record whose
  * fields are mostly optional, three of them carrying an Avro-declared default (one of them a
  * nested record default), written with only the field that carries a value.
  *
  * Rows, in the pairs that matter:
  *   - [[circeStrictFull]] vs [[circeDefaultsFull]] — '''the regression guard'''. The same FULL
  *     body through the strict and the lenient prism. The lenient walk's only extra work on a field
  *     that is present is a `match` on the lookup circe already performs, so these must be equal
  *     (and `circeDefaultsFull` must not be slower). A body that omits nothing never asks for a
  *     default.
  *   - [[circeStrictSparse]] vs [[circeDefaultsSparse]] — the behaviour change: the sparse body
  *     that used to be a miss is now a hit, paid for by `GenericData.getDefaultValue` (cached per
  *     `Field` by apache-avro) plus one `deepCopy` per absent field.
  *   - [[circeWithDefaults]] — the total normalizer, i.e. parse-with-defaults + full re-render. The
  *     row a caller who wants to stay on the lawful strict prism pays.
  *
  * `jsoniter*` rows are the same six measurements on the AST-free twin; the setup asserts both
  * bridges agree byte-for-byte on both bodies, so a divergence shows up as a failed run, not as
  * silently different numbers.
  *
  * B/op (`-prof gc`) is the load-bearing metric: the strict path's allocation profile must not
  * move, and the lenient path's cost must be proportional to the ABSENT fields only. Run:
  *
  * {{{
  *   sbt "benchmarks/Jmh/run -f 3 -wi 3 -i 5 -t 1 -prof gc .*AvroJsonDefaultsBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroJsonDefaultsBench extends JmhDefaults:

  private val schema: Schema = new Schema.Parser().parse(
    """{"type":"record","name":"Amendment","namespace":"eo.bench","fields":[
      |  {"name":"id","type":"string"},
      |  {"name":"note","type":["null","string"],"default":null},
      |  {"name":"priority","type":"int","default":0},
      |  {"name":"active","type":"boolean","default":true},
      |  {"name":"tags","type":{"type":"array","items":"string"},"default":[]},
      |  {"name":"meta","type":{"type":"map","values":"long"},"default":{}},
      |  {"name":"owner","type":{"type":"record","name":"Owner","fields":[
      |      {"name":"team","type":"string","default":"core"},
      |      {"name":"tier","type":"int","default":1}]},
      |   "default":{"team":"core","tier":1}}]}""".stripMargin
  )

  private var fullJson: Json = uninitialized
  private var sparseJson: Json = uninitialized
  private var fullBytes: Array[Byte] = uninitialized
  private var sparseBytes: Array[Byte] = uninitialized

  private var circeStrict: Json => Option[IndexedRecord] = uninitialized
  private var circeLenient: Json => Option[IndexedRecord] = uninitialized
  private var jsoniterStrict: Array[Byte] => Option[IndexedRecord] = uninitialized
  private var jsoniterLenient: Array[Byte] => Option[IndexedRecord] = uninitialized

  @Setup(Level.Trial)
  def init(): Unit =
    fullJson = Json.obj(
      "id" -> Json.fromString("a-1"),
      "note" -> Json.fromString("bump the cap"),
      "priority" -> Json.fromInt(3),
      "active" -> Json.fromBoolean(false),
      "tags" -> Json.arr(Json.fromString("urgent"), Json.fromString("billing")),
      "meta" -> Json.obj("region" -> Json.fromLong(7L)),
      "owner" -> Json.obj("team" -> Json.fromString("core"), "tier" -> Json.fromInt(1)),
    )
    // the issue's body: only the field that carries a value
    sparseJson = Json.obj("id" -> Json.fromString("a-1"))
    fullBytes = fullJson.noSpaces.getBytes(UTF_8)
    sparseBytes = sparseJson.noSpaces.getBytes(UTF_8)

    circeStrict = AvroJson.record(schema).getOption
    circeLenient = AvroJson.recordWithDefaults(schema).getOption
    jsoniterStrict = AvroJsoniter.record(schema).getOption
    jsoniterLenient = AvroJsoniter.recordWithDefaults(schema).getOption

    // both bridges must agree on both bodies, in both modes, or the numbers below compare
    // different work — a divergence fails the run instead of drifting the table
    require(
      circeStrict(fullJson).map(render) == jsoniterStrict(fullBytes).map(render),
      "strict parse of the full body diverges between the bridges",
    )
    require(
      circeLenient(fullJson).map(render) == jsoniterLenient(fullBytes).map(render),
      "lenient parse of the full body diverges between the bridges",
    )
    require(
      circeLenient(sparseJson).map(render) == jsoniterLenient(sparseBytes).map(render),
      "lenient parse of the sparse body diverges between the bridges",
    )
    require(circeLenient(sparseJson).isDefined, "the sparse body must be a lenient hit")
    require(circeStrict(sparseJson).isEmpty, "the sparse body must stay a strict miss")

  /** Avro binary of a record — the one equality both bridges agree on (`Utf8` vs `String` field
    * values are wire-identical but not `equals`).
    */
  private def render(record: IndexedRecord): Seq[Byte] =
    val out = new java.io.ByteArrayOutputStream()
    val encoder = org.apache.avro.io.EncoderFactory.get().binaryEncoder(out, null)
    new GenericDatumWriter[IndexedRecord](record.getSchema).write(record, encoder)
    encoder.flush()
    out.toByteArray.toSeq

  // ---- circe (AST) ---------------------------------------------------

  /** The strict read on a full body — the profile that must not move. */
  @Benchmark def circeStrictFull: Option[IndexedRecord] = circeStrict(fullJson)

  /** The lenient read on the SAME full body: the absent-field branch is never taken, so this is the
    * regression guard for `recordWithDefaults`.
    */
  @Benchmark def circeDefaultsFull: Option[IndexedRecord] = circeLenient(fullJson)

  /** The new path: six defaults resolved (one of them a nested record) for one present field. */
  @Benchmark def circeDefaultsSparse: Option[IndexedRecord] = circeLenient(sparseJson)

  /** The old behaviour on the sparse body — a miss, for contrast. */
  @Benchmark def circeStrictSparse: Option[IndexedRecord] = circeStrict(sparseJson)

  /** The total normalizer: parse-with-defaults + full re-render. */
  @Benchmark def circeWithDefaults: Json = AvroJson.withDefaults(sparseJson, schema)

  // ---- jsoniter (AST-free) -------------------------------------------

  @Benchmark def jsoniterStrictFull: Option[IndexedRecord] = jsoniterStrict(fullBytes)

  @Benchmark def jsoniterDefaultsFull: Option[IndexedRecord] = jsoniterLenient(fullBytes)

  @Benchmark def jsoniterDefaultsSparse: Option[IndexedRecord] = jsoniterLenient(sparseBytes)

  @Benchmark def jsoniterStrictSparse: Option[IndexedRecord] = jsoniterStrict(sparseBytes)

  @Benchmark def jsoniterWithDefaults: Array[Byte] =
    AvroJsoniter.withDefaults(sparseBytes, schema)

end AvroJsonDefaultsBench
