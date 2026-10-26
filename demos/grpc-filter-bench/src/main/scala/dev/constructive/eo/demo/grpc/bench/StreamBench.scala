package dev.constructive.eo.demo.grpc
package bench

import scala.compiletime.uninitialized

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray
import dev.constructive.eo.jsoniter.JsonSlice
import fs2.{Chunk, Stream}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** The whole filter, per message, in memory: gRPC frames arriving in 256 KiB pieces (as the demo
  * reads its socket) are cut into messages, judged, tallied, and the kept ones framed again. No
  * sockets, so what differs between the two `judge`s is what each costs in a running pipeline.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@OperationsPerInvocation(StreamBench.Messages)
class StreamBench:

  @Param(Array("optics", "decode"))
  var judge: String = uninitialized

  private var verdict: JsonSlice => Verdict = uninitialized

  private val pieces: Vector[Chunk[Byte]] =
    val framed = Stream
      .emits(
        Issues(seed = 7).take(StreamBench.Messages).map(i => Chunk.array(writeToArray(i))).toSeq
      )
      .through(GrpcFrames.joinFrames)
      .compile
      .to(Array)
    framed.grouped(256 * 1024).map(Chunk.array(_)).toVector

  @Setup def prepare(): Unit =
    verdict =
      if judge == "optics" then Triage.optics(Policy.default) else Triage.decoding(Policy.default)

  @Benchmark def filter: Long =
    Ref
      .of[IO, Tally](Tally.empty)
      .flatMap(tally =>
        Stream
          .emits(pieces)
          .flatMap(Stream.chunk)
          .through(Triage.filter(verdict, tally))
          .compile
          .count
      )
      .unsafeRunSync()

object StreamBench:
  final val Messages = 1024
