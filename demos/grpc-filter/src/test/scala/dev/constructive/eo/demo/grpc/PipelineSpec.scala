package dev.constructive.eo.demo.grpc

import scala.concurrent.duration.*
import scala.language.implicitConversions

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.syntax.all.*
import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray
import fs2.{Chunk, Stream}
import org.scalacheck.{Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** The filter end to end: in memory, cut into arbitrary pieces, and over real sockets. */
class PipelineSpec extends Specification with ScalaCheck:

  private val policy = Policy.default
  private val issues = Issues(seed = 5).take(400).toVector
  private val messages = issues.map(writeToArray(_))

  /** A Request whose kind no one here knows: the filter reads on past it, and drops it. */
  private val task =
    val request = messages(issues.indexWhere(_.detail.isInstanceOf[Detail.Request]))
    new String(request, "UTF-8")
      .replace("\"type\":\"Request\"", "\"type\":\"Task\"")
      .getBytes("UTF-8")

  private val upstream = messages :+ task

  /** What the filter must forward: the messages the decoded issues say to keep, as they are. */
  private val expected = upstream.filter(Triage.decoding(policy)(_).keep).map(_.toList)

  private def run[A](io: IO[A]): A = io.timeout(60.seconds).unsafeRunSync()

  private def checkTally(tally: Tally) =
    (tally.messages === upstream.size.toLong)
      .and(tally.bytes === upstream.map(_.length.toLong).sum)
      .and(tally.keptMessages === expected.size.toLong)
      .and(tally.unclassified === 1L)
      .and(tally.seen.values.sum === issues.size.toLong)

  "in memory, however the bytes are cut, the filter forwards exactly the kept messages" >>
    Prop
      .forAll(Gen.nonEmptyListOf(Gen.choose(1, 20_000))) { sizes =>
        val bytes = Stream.emits(upstream).through(GrpcFrames.joinFrames).compile.to(Array)
        val (tally, out) = run(Ref.of[IO, Tally](Tally.empty).flatMap { tally =>
          Stream
            .emits(GrpcFramesSpec.pieces(bytes, sizes))
            .flatMap(Stream.chunk)
            .covary[IO]
            .through(Triage.filter(Triage.optics(policy), tally))
            .through(GrpcFrames.splitFrames())
            .map(_.toList)
            .compile
            .toVector
            .product(tally.get)
            .map(_.swap)
        })
        (out === expected).and(checkTally(tally))
      }
      .set(minTestsOk = 20)

  "over sockets: upstream service → filter → downstream service" >> {
    val (tally, received) = run(
      (
        Net.source[IO](Stream.emits(upstream)),
        Net.sink[IO, Vector[List[Byte]]](Vector.empty)(_ :+ _.toList),
      ).tupled.use {
        case (source, (sink, received)) =>
          Net.filter[IO](source, sink, Triage.optics(policy)).product(received)
      }
    )
    (received === expected).and(checkTally(tally))
  }

  "a malformed frame fails the filter's stream" >> {
    val compressed = Chunk.array(Array[Byte](1, 0, 0, 0, 1, 42))
    run(Ref.of[IO, Tally](Tally.empty).flatMap { tally =>
      Stream
        .chunk(compressed)
        .covary[IO]
        .through(Triage.filter(Triage.optics(policy), tally))
        .compile
        .drain
        .attempt
    }) must beLeft(GrpcFrames.Malformed("compressed flag 1: no message encoding negotiated"))
  }
