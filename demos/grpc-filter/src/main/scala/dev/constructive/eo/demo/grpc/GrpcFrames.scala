package dev.constructive.eo.demo.grpc

import scala.annotation.tailrec
import scala.util.control.NoStackTrace

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
  def decode[F[_]: RaiseThrowable](
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

  /** Messages → their frames, uncompressed, one chunk of bytes per chunk of messages. */
  def encode[F[_]]: Pipe[F, Array[Byte], Byte] =
    _.mapChunks(messages => Chunk.array(frames(messages)))

  /** One message's frame. */
  def frame(message: Array[Byte]): Array[Byte] = frames(Chunk.singleton(message))

  /** Every whole frame at the front of `bytes`, as messages, and the bytes after them. */
  private def split(
      bytes: Chunk[Byte],
      maxMessageBytes: Int,
  ): Either[String, (Chunk[Array[Byte]], Chunk[Byte])] =
    @tailrec def loop(
        rest: Chunk[Byte],
        messages: List[Array[Byte]],
    ): Either[String, (Chunk[Array[Byte]], Chunk[Byte])] =
      def done = Right((Chunk.from(messages.reverse), rest))
      if rest.size < HeaderBytes then done
      else
        val length =
          (rest(1) & 0xffL) << 24 | (rest(2) & 0xffL) << 16 | (rest(3) & 0xffL) << 8 | (rest(
            4
          ) & 0xffL)
        if rest(0) != 0 then Left(s"compressed flag ${rest(0)}: no message encoding negotiated")
        else if length > maxMessageBytes then Left(s"a $length-byte message: over $maxMessageBytes")
        else if rest.size - HeaderBytes < length then done
        else
          val (message, next) = rest.drop(HeaderBytes).splitAt(length.toInt)
          loop(next, message.toArray :: messages)
    loop(bytes, Nil)

  private def frames(messages: Chunk[Array[Byte]]): Array[Byte] =
    val out = new Array[Byte](messages.foldLeft(0)(_ + HeaderBytes + _.length))
    messages.foldLeft(0) { (at, message) =>
      val length = message.length
      out(at) = 0
      out(at + 1) = (length >>> 24).toByte
      out(at + 2) = (length >>> 16).toByte
      out(at + 3) = (length >>> 8).toByte
      out(at + 4) = length.toByte
      System.arraycopy(message, 0, out, at + HeaderBytes, length)
      at + HeaderBytes + length
    }
    out
