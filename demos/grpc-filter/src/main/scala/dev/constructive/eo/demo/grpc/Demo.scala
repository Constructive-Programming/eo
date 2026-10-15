package dev.constructive.eo.demo.grpc

import scala.concurrent.duration.FiniteDuration

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray
import fs2.Stream

/** Runs the three parties on the loopback interface: once with the filter reading messages through
  * eo-jsoniter optics, once decoding every message, as a baseline. The upstream service repeats a
  * pool of 1 024 encoded issues, so encoding isn't what's measured, and each filter runs once
  * untimed first: the JIT compiler takes a few hundred thousand messages to finish its work.
  *
  * {{{
  * sbt "demoGrpcFilter/run 200000"
  * }}}
  */
object Demo extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    val count = args.headOption.flatMap(_.toIntOption).getOrElse(200_000)
    val pool = Issues(seed = 2026).take(1024).map(writeToArray(_)).toVector
    val policy = Policy.default
    val filters = List(
      "eo-jsoniter optics" -> Triage.optics(policy),
      "full decode (baseline)" -> Triage.decoding(policy),
    )

    def once(count: Int, name: String, verdict: Array[Byte] => Verdict): IO[String] =
      val messages = Stream.emits(pool).repeat.take(count.toLong).covary[IO]
      (Net.source(messages), Net.sink[IO, Received](Received.empty)(_.add(_, policy))).tupled.use {
        case (upstream, (downstream, received)) =>
          for
            started <- IO.monotonic
            tally <- Net.filter[IO](upstream, downstream, verdict)
            took <- IO.monotonic.map(_ - started)
            got <- received
          yield report(name, tally, took, got)
      }

    IO.println("Warming up: one untimed run of each filter…") >>
      filters.traverse_((name, verdict) => once(count, name, verdict)) >>
      filters
        .traverse_((name, verdict) => once(count, name, verdict).flatMap(IO.println(_)))
        .as(
          ExitCode.Success
        )

  private def report(name: String, tally: Tally, took: FiniteDuration, got: Received): String =
    val seconds = took.toNanos / 1e9
    val megabytes = tally.bytes / 1e6
    val judging = tally.judging.toNanos / 1e3 / tally.messages
    s"""|$name: ${tally.messages} messages, ${f"$megabytes%.1f"} MB in ${f"$seconds%.2f"} s:
        |  ${f"${tally.messages / seconds / 1000}%.0f"} k messages/s, ${f"${megabytes / seconds}%.0f"} MB/s
        |  judging them took ${f"$judging%.1f"} µs per message; the rest is sockets and framing
        |${tally.show}
        |  downstream received ${got.messages} messages (${tally.keptMessages} kept); ${got.issues} decode as issues, and the policy keeps ${got.kept} of them
        |""".stripMargin
