package dev.constructive.eo.demo.grpc

import scala.util.control.NoStackTrace

import cats.syntax.all.*
import fs2.{Chunk, Pipe, Pull, RaiseThrowable, Stream}

/** gRPC's message framing, as fs2 pipes over raw bytes. The gRPC over HTTP/2 spec frames each
  * message as a Length-Prefixed-Message: one byte saying whether the message is compressed, its
  * length as a 4-byte big-endian unsigned integer, then the message. Over HTTP/2 a stream of such
  * frames is the body of a gRPC call; in this demo they come straight off a TCP socket, in whatever
  * pieces the socket reads them.
  */
object GrpcFrames:

  /** The compressed flag and the length. */
  val HeaderBytes = 5

  /** gRPC's default limit on a received message: 4 MiB. */
  val DefaultMaxMessageBytes: Int = 4 * 1024 * 1024

  /** The bytes are not gRPC frames this decoder can read. It fails the stream, like the socket
    * errors around it: a gRPC peer answers a malformed frame by ending the call.
    */
  final case class Malformed(reason: String) extends RuntimeException(reason) with NoStackTrace

  /** Raw bytes → one array per message: the message's bytes, uncompressed, without its header.
    * Messages may straddle the incoming chunks any way at all. Fails with [[Malformed]] on a
    * compressed message (no message encoding is negotiated here), on one longer than
    * `maxMessageBytes`, or when the bytes end inside a frame.
    */
  def splitFrames[F[_]: RaiseThrowable](
      maxMessageBytes: Int = DefaultMaxMessageBytes
  ): Pipe[F, Byte, Array[Byte]] =
    def go(buffered: Chunk[Byte], rest: Stream[F, Byte]): Pull[F, Array[Byte], Unit] =
      split(buffered, maxMessageBytes) match
        case Left(reason)            => Pull.raiseError[F](Malformed(reason))
        case Right((messages, left)) =>
          Pull.output(messages) >> rest.pull.uncons.flatMap {
            case Some((chunk, tail))  => go(left ++ chunk, tail)
            case None if left.isEmpty => Pull.done
            case None                 =>
              Pull.raiseError[F](Malformed(s"the bytes end inside a frame, ${left.size} bytes in"))
          }
    in => go(Chunk.empty, in).stream

  /** Messages → their frames, uncompressed. The messages' bytes are not copied. */
  def joinFrames[F[_]]: Pipe[F, Array[Byte], Byte] =
    _.mapChunks(_.flatMap(frame))

  /** One message's frame: not compressed, its length, the message. */
  def frame(message: Array[Byte]): Chunk[Byte] =
    val length = message.length
    Chunk(0, length >>> 24, length >>> 16, length >>> 8, length).map(_.toByte) ++
      Chunk.array(message)

  /** Every whole frame at the front of `bytes`, as messages, and the bytes after them. */
  private def split(
      bytes: Chunk[Byte],
      maxMessageBytes: Int,
  ): Either[String, (Chunk[Array[Byte]], Chunk[Byte])] =
    (List.empty[Array[Byte]], bytes).tailRecM { (messages, rest) =>
      unframe(rest, maxMessageBytes).map {
        case Some((message, next)) => Left((message :: messages, next))
        case None                  => Right((Chunk.from(messages.reverse), rest))
      }
    }

  /** The message in the frame at the front of `bytes`, and the bytes after it; `None` until the
    * whole frame is there.
    */
  private def unframe(
      bytes: Chunk[Byte],
      maxMessageBytes: Int,
  ): Either[String, Option[(Array[Byte], Chunk[Byte])]] =
    if bytes.size < HeaderBytes then Right(None)
    else
      val length = (1 to 4).foldLeft(0L)((n, i) => n << 8 | (bytes(i) & 0xffL))
      if bytes(0) != 0 then Left(s"compressed flag ${bytes(0)}: no message encoding negotiated")
      else if length > maxMessageBytes then Left(s"a $length-byte message: over $maxMessageBytes")
      else
        Right(Option.when(bytes.size - HeaderBytes >= length) {
          val (message, rest) = bytes.drop(HeaderBytes).splitAt(length.toInt)
          (message.toArray, rest)
        })
