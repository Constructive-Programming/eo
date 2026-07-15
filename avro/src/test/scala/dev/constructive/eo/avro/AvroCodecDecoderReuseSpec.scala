package dev.constructive.eo.avro

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericRecord, IndexedRecord}
import org.apache.avro.io.DecoderFactory
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
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
  *      correctness guard: a wrong decode in a serde library corrupts every consumer.
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

  "non-resolved decodeRecord matches the fresh reference decode (WriterEvent)" >> forAll(
    genWriterEvent
  ) { e =>
    val bytes = binaryOf(e)
    AvroCodec
      .decodeRecord(bytes, writerSchema)
      .exists(_ == freshDecode(bytes, writerSchema, writerSchema))
  }

  "non-resolved decodeRecord matches the fresh reference decode (union payload)" >> forAll(
    genTransaction
  ) { t =>
    val bytes = binaryOf(t)
    AvroCodec
      .decodeRecord(bytes, transactionSchema)
      .exists(_ == freshDecode(bytes, transactionSchema, transactionSchema))
  }

  "resolved decode matches the fresh reference (WriterEvent→ReaderEvent, field dropped)" >> forAll(
    genWriterEvent
  ) { e =>
    val bytes = binaryOf(e)
    AvroCodec
      .decodeResolvedRecord(bytes, writerSchema, readerEventSchema)
      .exists(_ == freshDecode(bytes, writerSchema, readerEventSchema))
  }

  "resolved decode matches the fresh reference (fields reordered, resolved by name)" >> forAll(
    genReorderWriter
  ) { w =>
    val bytes = binaryOf(w)
    AvroCodec
      .decodeResolvedRecord(bytes, reorderWriterSchema, reorderReaderSchema)
      .exists(_ == freshDecode(bytes, reorderWriterSchema, reorderReaderSchema))
  }

  "resolved decode matches the fresh reference (int→long promotion)" >> forAll(genPromoteWriter) {
    w =>
      val bytes = binaryOf(w)
      AvroCodec
        .decodeResolvedRecord(bytes, promoteWriterSchema, promoteReaderSchema)
        .exists(_ == freshDecode(bytes, promoteWriterSchema, promoteReaderSchema))
  }

  "resolved decode matches the fresh reference (field added with default)" >> forAll(genId) { id =>
    val rec = new GenericData.Record(addWriterSchema)
    rec.put("id", id)
    val bytes = AvroSpecFixtures.toBinary(rec, addWriterSchema)
    val cached = AvroCodec.decodeResolvedRecord(bytes, addWriterSchema, addReaderSchema)
    val reference = freshDecode(bytes, addWriterSchema, addReaderSchema)
    // Sanity: the reader default really did materialise, so this is a live added-field case.
    val defaultApplied = reference.get(addReaderSchema.getField("note").pos).toString == "n/a"
    cached.exists(_ == reference) && defaultApplied
  }

  // ---- 2. threadLocalStorage = false opt-out (fresh allocation per call) ----

  "resolved decode with threadLocalStorage = false matches the fresh reference decode" >> forAll(
    genWriterEvent
  ) { e =>
    val bytes = binaryOf(e)
    AvroCodec
      .decodeResolvedRecord(bytes, writerSchema, readerEventSchema, threadLocalStorage = false)
      .exists(_ == freshDecode(bytes, writerSchema, readerEventSchema))
  }

  // ---- 3. Concurrent + non-aliased ----

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
