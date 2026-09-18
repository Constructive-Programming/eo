package dev.constructive.eo.avro

import scala.language.implicitConversions

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericRecord, IndexedRecord}
import org.apache.avro.io.DecoderFactory
import org.scalacheck.Gen
import org.scalacheck.Prop.forAllNoShrink
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Decode-path reuse in [[AvroCodec]] — the per-thread `GenericDatumReader` cache and reusable
  * `BinaryDecoder` that replaced the fresh-allocate-per-decode code (`new GenericDatumReader(…)` +
  * `binaryDecoder(new ByteArrayInputStream(…), null)`). Two guards, in the order they matter:
  *
  *   1. '''Byte-for-byte identical decode.''' The cached path — and the `threadLocalStorage =
  *      false` opt-out — must produce a record equal to the OLD fresh-allocation reference decode
  *      (reproduced verbatim in [[freshDecode]]), across writer→reader evolution (field dropped,
  *      added-with-default, reordered, promoted) and union-typed payloads. This is the load-bearing
  *      correctness guard: a wrong decode in a serde library corrupts every consumer. The evolution
  *      shapes are DATA ([[reuseScenarios]]) rather than one example each, so every shape is put
  *      through both entry points.
  *   2. '''Thread-safe + non-aliased.''' Concurrent decodes on many threads stay correct, and a
  *      record decoded earlier on a thread is never mutated by a later decode on that thread (fresh
  *      datum, no `Utf8`/bytes aliasing).
  *
  * Fixtures (`WriterEvent` / `ReaderEvent`, `ReorderWriter` / `ReorderReader`, `PromoteWriter` /
  * `PromoteReader`) are shared with [[ConfluentReaderSpec]]; `Transaction` (union `<null,long>`)
  * comes from [[AvroSpecFixtures]]. The add-a-field-with-default pair is hand-parsed so the Avro
  * default is guaranteed present regardless of derivation behaviour.
  */
class AvroCodecDecoderReuseSpec extends Specification with ScalaCheck:

  private val writerSchema: Schema = summon[AvroCodec[WriterEvent]].schema
  private val readerEventSchema: Schema = summon[AvroCodec[ReaderEvent]].schema
  private val reorderWriterSchema: Schema = summon[AvroCodec[ReorderWriter]].schema
  private val reorderReaderSchema: Schema = summon[AvroCodec[ReorderReader]].schema
  private val promoteWriterSchema: Schema = summon[AvroCodec[PromoteWriter]].schema
  private val promoteReaderSchema: Schema = summon[AvroCodec[PromoteReader]].schema
  private val transactionSchema: Schema = AvroSpecFixtures.transactionSchema

  /** Writer with only `id`; reader adds `note` with a String default — Avro fills the default when
    * resolving the field-less writer bytes.
    */
  private val addWriterSchema: Schema = new Schema.Parser().parse(
    """{"type":"record","name":"Add","namespace":"eo.reuse.test",
      | "fields":[{"name":"id","type":"string"}]}""".stripMargin
  )

  private val addReaderSchema: Schema = new Schema.Parser().parse(
    """{"type":"record","name":"Add","namespace":"eo.reuse.test",
      | "fields":[{"name":"id","type":"string"},
      |           {"name":"note","type":"string","default":"n/a"}]}""".stripMargin
  )

  /** The OLD, fresh-allocation decode reproduced verbatim: a brand-new `GenericDatumReader` and a
    * brand-new `BinaryDecoder` over a `ByteArrayInputStream`, fresh datum. The reuse impl must
    * match this exactly.
    */
  private def freshDecode(bytes: Array[Byte], writer: Schema, reader: Schema): IndexedRecord =
    val r = new GenericDatumReader[GenericRecord](writer, reader)
    val d = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null)
    r.read(null, d)

  private def binaryOf[A](a: A)(using codec: AvroCodec[A]): Array[Byte] =
    AvroSpecFixtures.toBinaryValue(codec.encode(a), codec.schema)

  private val genId: Gen[String] =
    Gen.alphaNumStr.map(s => if s.isEmpty then "x" else s.take(12))

  private val genWriterEvent: Gen[WriterEvent] =
    for
      id <- genId
      legacy <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
    yield WriterEvent(id, legacy)

  private val genReorderWriter: Gen[ReorderWriter] =
    for
      alpha <- genId
      beta <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
      gamma <- Gen.oneOf(true, false)
    yield ReorderWriter(alpha, beta, gamma)

  private val genPromoteWriter: Gen[PromoteWriter] =
    for
      label <- genId
      count <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
    yield PromoteWriter(label, count)

  private val genTransaction: Gen[AvroSpecFixtures.Transaction] =
    for
      id <- genId
      amount <- Gen.option(Gen.chooseNum(Long.MinValue, Long.MaxValue))
    yield AvroSpecFixtures.Transaction(id, amount)

  // ---- 1. Byte-identical decode vs the fresh-allocation reference ----
  //
  // Seven near-identical properties collapsed into one: they differed ONLY in the (generator,
  // writer schema, reader schema) triple and in which entry point they called, so the triple is
  // now data and both entry points — the thread-local cache and the `threadLocalStorage = false`
  // opt-out — are checked on every scenario instead of one each.

  /** One writer→reader shape. `extra` is a scenario-specific sanity check on the reference decode,
    * so a scenario can assert that the evolution it names really happened.
    */
  final private case class Reuse(
      name: String,
      bytes: Gen[Array[Byte]],
      writer: Schema,
      reader: Schema,
      extra: IndexedRecord => Boolean,
  )

  private def always: IndexedRecord => Boolean = _ => true

  private val addWriterBytes: Gen[Array[Byte]] = genId.map { id =>
    val rec = new GenericData.Record(addWriterSchema)
    rec.put("id", id)
    AvroSpecFixtures.toBinary(rec, addWriterSchema)
  }

  private val reuseScenarios: List[Reuse] = List(
    Reuse("non-resolved", genWriterEvent.map(binaryOf(_)), writerSchema, writerSchema, always),
    Reuse(
      "non-resolved, union payload",
      genTransaction.map(binaryOf(_)),
      transactionSchema,
      transactionSchema,
      always,
    ),
    Reuse(
      "field dropped",
      genWriterEvent.map(binaryOf(_)),
      writerSchema,
      readerEventSchema,
      always,
    ),
    Reuse(
      "fields reordered, resolved by name",
      genReorderWriter.map(binaryOf(_)),
      reorderWriterSchema,
      reorderReaderSchema,
      always,
    ),
    Reuse(
      "int→long promotion",
      genPromoteWriter.map(binaryOf(_)),
      promoteWriterSchema,
      promoteReaderSchema,
      always,
    ),
    // The reader default must really materialise, else this is not a live added-field case.
    Reuse(
      "field added with default",
      addWriterBytes,
      addWriterSchema,
      addReaderSchema,
      r => r.get(addReaderSchema.getField("note").pos).toString == "n/a",
    ),
  )

  private val genScenario: Gen[(Reuse, Array[Byte])] =
    Gen.oneOf(reuseScenarios).flatMap(s => s.bytes.map(b => (s, b)))

  "every decode entry point reproduces the fresh-allocation reference, on every evolution shape" >>
    forAllNoShrink(genScenario) { (scenario, bytes) =>
      val reference = freshDecode(bytes, scenario.writer, scenario.reader)
      val cached =
        if scenario.writer == scenario.reader then AvroCodec.decodeRecord(bytes, scenario.writer)
        else AvroCodec.decodeResolvedRecord(bytes, scenario.writer, scenario.reader)
      val uncached = AvroBinaryCursor
        .records
        .read(
          bytes,
          0,
          bytes.length,
          scenario.writer,
          scenario.reader,
          threadLocalStorage = false,
        )
      (cached.exists(_ == reference) && (uncached == reference) && scenario.extra(
        reference
      )) :| scenario.name
    }

  // ---- 2. Concurrent + non-aliased ----

  "concurrent decodes across threads are correct and never alias an earlier record" >> {
    val events = (0 until 8).map(i => WriterEvent(s"id-$i", i)).toVector
    val payloads = events.map(binaryOf(_))
    val idPos = writerSchema.getField("id").pos
    val threadCount = 8
    val perThread = 300
    val errors = new ConcurrentLinkedQueue[String]()

    val threads = (0 until threadCount).map { ti =>
      new Thread(() =>
        var held: IndexedRecord | Null = null
        var heldId: String = ""
        (0 until perThread).foreach { k =>
          val idx = (ti + k) % events.size
          val expected = events(idx)
          AvroCodec.decodeRecord(payloads(idx), writerSchema) match
            case Right(rec) =>
              if rec.get(idPos).toString != expected.id then
                val _ =
                  errors.add(s"thread $ti iter $k: got ${rec.get(idPos)}, want ${expected.id}")
              val prev = held
              if prev != null && prev.get(idPos).toString != heldId then
                val _ =
                  errors.add(s"thread $ti iter $k: earlier record aliased to ${prev.get(idPos)}")
              held = rec
              heldId = expected.id
            case Left(f) =>
              val _ = errors.add(s"thread $ti iter $k: decode failed $f")
        }
      )
    }
    threads.foreach(_.start())
    threads.foreach(_.join())

    errors.isEmpty must beTrue
  }

end AvroCodecDecoderReuseSpec
