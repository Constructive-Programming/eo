package dev.constructive.eo
package bench

import scala.compiletime.uninitialized

import dev.constructive.eo.avro.{AvroCodec, ConfluentWire}
import dev.constructive.eo.bench.fixture.*
import java.util.concurrent.TimeUnit
import org.apache.avro.generic.IndexedRecord
import org.openjdk.jmh.annotations.*

/** Decode-path reuse bench (#77): the per-thread `GenericDatumReader` + `BinaryDecoder` reuse vs
  * the pre-#77 fresh-allocation decode, on the 7-field [[ConversionDomain]] envelope.
  *
  * Pairs to compare:
  *   - [[freshDecodeRecord]] vs [[cachedDecodeRecord]] — the root decode with and without the
  *     engine reuse. Caveat: the cached path returns through `Either`, the fresh baseline is bare
  *     (the fixture's pre-#77 incantation), so B/op carries a small constant wrapper delta.
  *   - [[confluentRecordReader]] vs [[confluentRecordReaderFresh]] — the SAME
  *     `ConfluentWire.recordReader` consume path, `threadLocalStorage` on vs off. Identical
  *     wrappers both sides, so the B/op delta is exactly the reader + decoder reuse.
  *
  * B/op (`-prof gc`) is the load-bearing metric on the local box; ns/op advises. Run:
  *
  * {{{
  *   sbt "benchmarks/Jmh/run -f 1 -wi 2 -i 3 -t 1 -prof gc .*AvroDecodeReuseBench.*"
  * }}}
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class AvroDecodeReuseBench extends JmhDefaults:

  import ConversionDomain.*

  private type ET[A] = Either[Throwable, A]

  var envelopeBytes: Array[Byte] = uninitialized
  var framedBytes: Array[Byte] = uninitialized
  var consume: Array[Byte] => ET[IndexedRecord] = uninitialized
  var consumeFresh: Array[Byte] => ET[IndexedRecord] = uninitialized

  @Setup(Level.Iteration)
  def init(): Unit =
    envelopeBytes = encodeEnvelope(inputEnvelope)
    framedBytes = ConfluentWire.attach(1, envelopeBytes)
    consume = ConfluentWire.recordReader[ET](_ => Right(envelopeSchema), envelopeSchema)
    consumeFresh = ConfluentWire.recordReader[ET](
      _ => Right(envelopeSchema),
      envelopeSchema,
      threadLocalStorage = false,
    )

  /** Pre-#77 root decode, reproduced verbatim by the fixture: fresh `GenericDatumReader` + fresh
    * `BinaryDecoder` per call.
    */
  @Benchmark def freshDecodeRecord: IndexedRecord =
    toRecord(envelopeBytes, envelopeSchema, envelopeSchema)

  /** #77 root decode: per-thread reader per `(writer, reader)` pair + reusable decoder. */
  @Benchmark def cachedDecodeRecord: Either[?, IndexedRecord] =
    AvroCodec.decodeRecord(envelopeBytes, envelopeSchema)

  /** Per-message Confluent consume (strip + resolve-decode), engine reuse ON (the default). */
  @Benchmark def confluentRecordReader: ET[IndexedRecord] =
    consume(framedBytes)

  /** Same consume path, engine reuse opted out at construction — the pre-#77 allocation profile
    * behind an identical wrapper stack.
    */
  @Benchmark def confluentRecordReaderFresh: ET[IndexedRecord] =
    consumeFresh(framedBytes)
