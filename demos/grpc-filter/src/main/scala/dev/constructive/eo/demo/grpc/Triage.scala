package dev.constructive.eo.demo.grpc

import scala.concurrent.duration.{Duration, FiniteDuration}

import cats.effect.syntax.all.*
import cats.effect.{Async, Ref}
import cats.syntax.all.*
import dev.constructive.eo.jsoniter.JsonSlice
import dev.constructive.eo.optics.Optic.*
import fs2.{Chunk, Pipe}

/** What triage made of one message: its kind, if it has one this demo knows, and whether the policy
  * keeps it.
  */
final case class Verdict(kind: Option[Kind], keep: Boolean)

/** How a stream was triaged: messages and bytes seen; per kind, how many were seen and kept; and
  * the time spent judging them, apart from reading and writing sockets. `unclassified` counts the
  * messages without a kind: no `detail`, or one of a kind this demo doesn't know (from a newer
  * producer, say). The policy drops those.
  */
final case class Tally(
    messages: Long,
    bytes: Long,
    seen: Map[Kind, Long],
    kept: Map[Kind, Long],
    unclassified: Long,
    judging: FiniteDuration,
):

  def keptMessages: Long = kept.values.sum

  /** A chunk of messages, their verdicts, and how long they took. */
  def add(judged: Chunk[(Chunk[Byte], Verdict)], took: FiniteDuration): Tally =
    judged.foldLeft(copy(judging = judging + took)) {
      case (tally, (message, verdict)) =>
        def bump(counts: Map[Kind, Long], kind: Kind) = counts.updated(kind, counts(kind) + 1)
        tally.copy(
          messages = tally.messages + 1,
          bytes = tally.bytes + message.size,
          seen = verdict.kind.fold(tally.seen)(bump(tally.seen, _)),
          kept = verdict.kind.filter(_ => verdict.keep).fold(tally.kept)(bump(tally.kept, _)),
          unclassified = tally.unclassified + verdict.kind.fold(1L)(_ => 0L),
        )
    }

  def show: String =
    val rows = Kind.values.toList.map(k => f"  ${k.toString}%-10s${seen(k)}%10d${kept(k)}%10d")
    (f"  ${"kind"}%-10s${"seen"}%10s${"kept"}%10s" :: rows :::
      List(f"  ${"none"}%-10s$unclassified%10d${0}%10d")).mkString("\n")

object Tally:

  val empty: Tally =
    val none = Kind.values.map(_ -> 0L).toMap
    Tally(0, 0, none, none, 0, Duration.Zero)

/** Classifying and filtering a stream of gRPC-framed issues. */
object Triage:

  /** A message's verdict, read through [[Wire]]'s optics: one scan of the message for its `detail`,
    * then the few leaves the policy wants from the detail's bytes. Nothing else is decoded, and the
    * message is read where it lies.
    */
  def optics(policy: Policy)(message: JsonSlice): Verdict =
    import Wire.given
    Wire.detail.getOption(message) match
      case Some(detail) => Verdict(Wire.kind.getOption(detail), policy.keep(detail))
      case None         => Verdict(None, keep = false)

  /** The same verdict the usual way, as a baseline: decode the whole `Issue`, then ask the policy
    * about its `Detail` through [[Model]]'s optics. A message that doesn't decode (a kind this
    * model doesn't know fails the whole message) has no kind and is dropped.
    */
  def decoding(policy: Policy)(message: JsonSlice): Verdict =
    import Model.given
    Issue.decode(message).map(_.detail) match
      case Some(detail) => Verdict(Some(Kind.of(detail)), policy.keep(detail))
      case None         => Verdict(None, keep = false)

  /** The filter: raw bytes of gRPC frames in; the frames of the messages `verdict` keeps out, each
    * message forwarded byte for byte. Every chunk of verdicts, and how long it took, goes into
    * `tally`. Messages are judged and forwarded where they lie in the incoming chunks: none is
    * copied, except one that straddles two chunks.
    */
  def filter[F[_]: Async](
      verdict: JsonSlice => Verdict,
      tally: Ref[F, Tally]
  ): Pipe[F, Byte, Byte] =
    _.through(GrpcFrames.splitFrames())
      .chunks
      .evalMap { messages =>
        Async[F]
          .delay(messages.map(message => (message, verdict(Wire.json(message)))))
          .timed
          .flatMap { (took, judged) =>
            tally.update(_.add(judged, took)).as(judged.collect { case (m, Verdict(_, true)) => m })
          }
      }
      .unchunks
      .through(GrpcFrames.joinFrames)
