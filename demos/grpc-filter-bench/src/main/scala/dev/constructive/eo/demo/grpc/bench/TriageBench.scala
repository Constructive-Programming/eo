package dev.constructive.eo.demo.grpc
package bench

import scala.compiletime.uninitialized

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray}
import dev.constructive.eo.optics.Optic.*
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

/** One message triaged, two ways, over a pool of messages small enough to stay in cache (as a
  * message just read from a socket is):
  *
  *   - `decode` — decode the whole `Issue`, then run the policy on its `Detail`.
  *   - `optics` — slice out the detail with eo-jsoniter, then run the same policy on its bytes.
  *   - `detail` — only the slicing: the one scan through the message.
  *
  * `layout` is where `detail` sits: `late`, where the codec writes it (61st of 64 fields, about 90%
  * of the way in), or `first`, moved to the front. Decoding reads every byte either way; the scan
  * reads the bytes in front of the field.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@OperationsPerInvocation(TriageBench.Pool)
class TriageBench:

  @Param(Array("late", "first"))
  var layout: String = uninitialized

  private val policy = Policy.default
  private var messages: Array[Array[Byte]] = uninitialized

  @Setup def prepare(): Unit =
    val issues = Issues(seed = 7).take(TriageBench.Pool).toArray
    messages = issues.map { issue =>
      val message = writeToArray(issue)
      val laidOut = if layout == "first" then detailFirst(message) else message
      require(readFromArray[Issue](laidOut) == issue, "the layout changed the message")
      laidOut
    }

  /** The message with `detail` moved to the front: the same JSON object, its fields reordered. */
  private def detailFirst(message: Array[Byte]): Array[Byte] =
    val json = new String(message, UTF_8)
    val detail = s"\"detail\":${new String(Wire.detail.getOption(message).get.bytes, UTF_8)}"
    s"{$detail,${json.drop(1).replace(s",$detail", "")}".getBytes(UTF_8)

  @Benchmark def decode(bh: Blackhole): Unit =
    import Model.given
    messages.foreach(m => bh.consume(policy.keep(readFromArray[Issue](m).detail)))

  @Benchmark def optics(bh: Blackhole): Unit =
    import Wire.given
    messages.foreach(m => bh.consume(Wire.detail.getOption(m).exists(policy.keep(_))))

  @Benchmark def detail(bh: Blackhole): Unit =
    messages.foreach(m => bh.consume(Wire.detail.getOption(m)))

object TriageBench:
  final val Pool = 256
