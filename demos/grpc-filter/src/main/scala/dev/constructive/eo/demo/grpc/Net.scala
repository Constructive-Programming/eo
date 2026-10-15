package dev.constructive.eo.demo.grpc

import cats.effect.syntax.all.*
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.io.net.{Network, Socket}

/** What the downstream service received: messages and bytes, how many decoded as issues, and how
  * many of those the policy keeps when it reads the decoded issue.
  */
final case class Received(messages: Long, bytes: Long, issues: Long, kept: Long):

  def add(message: Array[Byte], policy: Policy): Received =
    import Model.given
    val detail = Issue.decode(message).map(_.detail)
    Received(
      messages + 1,
      bytes + message.length,
      issues + detail.size,
      kept + detail.count(policy.keep(_)),
    )

object Received:
  val empty: Received = Received(0, 0, 0, 0)

/** The demo's three parties, each on its own TCP socket on the loopback interface: an upstream
  * service streaming issues, the filter, and a downstream service that receives what the filter
  * keeps.
  */
object Net:

  private val loopback = SocketAddress(ip"127.0.0.1", port"0")

  /** A socket's bytes, up to 256 KiB per read. fs2's `reads` asks for 8 KiB at a time, and every
    * read is a trip through the event loop: on loopback that, not the filter, was the bottleneck.
    */
  def reads[F[_]](socket: Socket[F]): Stream[F, Byte] =
    Stream.repeatEval(socket.read(256 * 1024)).unNoneTerminate.unchunks

  /** The upstream service: to each client that connects, it streams `messages` as gRPC frames and
    * then closes its side. Gives the address it listens on.
    */
  def source[F[_]: {Network, Async}](
      messages: Stream[F, Array[Byte]]
  ): Resource[F, GenSocketAddress] =
    Network[F].bind(loopback).flatMap { server =>
      server
        .accept
        .map(client =>
          messages.through(GrpcFrames.joinFrames).through(client.writes) ++
            Stream.exec(client.endOfOutput)
        )
        .parJoinUnbounded
        .compile
        .drain
        .background
        .as(server.address)
    }

  /** The downstream service: accepts one client and folds every message it sends into `z` with `f`.
    * Gives the address it listens on, and the result once the client is done.
    */
  def sink[F[_]: {Network, Async}, A](z: A)(
      f: (A, Array[Byte]) => A
  ): Resource[F, (GenSocketAddress, F[A])] =
    Network[F].bind(loopback).flatMap { server =>
      server
        .accept
        .head
        .flatMap(reads(_).through(GrpcFrames.splitFrames()))
        .fold(z)(f)
        .compile
        .lastOrError
        .background
        .map(outcome => (server.address, outcome.flatMap(_.embedError)))
    }

  /** The filter: connects to both, triages the upstream's stream with `verdict` and writes the
    * frames it keeps downstream. Gives the tally once the upstream's stream ends. Reading, judging
    * and writing each run on their own fiber, with a few chunks buffered between them, so the
    * sockets wait on the judging only when it is the slowest of the three.
    */
  def filter[F[_]: {Network, Async}](
      upstream: GenSocketAddress,
      downstream: GenSocketAddress,
      verdict: Array[Byte] => Verdict,
  ): F[Tally] =
    (Network[F].connect(upstream), Network[F].connect(downstream)).tupled.use { (in, out) =>
      Ref.of[F, Tally](Tally.empty).flatMap { tally =>
        reads(in)
          .prefetchN(4)
          .through(Triage.filter(verdict, tally))
          .prefetchN(4)
          .through(out.writes)
          .compile
          .drain >>
          out.endOfOutput >> tally.get
      }
    }
