package dev.constructive.eo.demo.grpc

import scala.annotation.tailrec
import scala.language.implicitConversions

import fs2.{Chunk, Fallible, Stream}
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** gRPC framing over bytes that arrive in arbitrary pieces. */
class GrpcFramesSpec extends Specification with ScalaCheck:

  import GrpcFramesSpec.pieces

  def decode(
      chunks: List[Chunk[Byte]],
      max: Int = GrpcFrames.DefaultMaxMessageBytes,
  ): Either[Throwable, List[List[Byte]]] =
    Stream
      .emits(chunks)
      .flatMap(Stream.chunk)
      .covary[Fallible]
      .through(GrpcFrames.decode(max))
      .compile
      .toList
      .map(_.map(_.toList))

  def framed(messages: List[Array[Byte]]): Array[Byte] =
    Stream.emits(messages).through(GrpcFrames.encode).compile.to(Array)

  def malformed(bytes: Int*)(reason: String) =
    decode(List(Chunk.array(bytes.map(_.toByte).toArray)), max = 10) must beLeft(
      GrpcFrames.Malformed(reason)
    )

  private val messages = Gen.listOf(Gen.containerOf[Array, Byte](Arbitrary.arbitrary[Byte]))
  private val sizes = Gen.nonEmptyListOf(Gen.oneOf(Gen.choose(1, 8), Gen.choose(1, 300)))

  "messages come back whole however the bytes are cut" >> Prop.forAll(messages, sizes) {
    (messages, sizes) =>
      decode(pieces(framed(messages), sizes)) must beRight(messages.map(_.toList))
  }

  "a frame is a zero, the length big-endian, then the message" >> {
    GrpcFrames.frame(Array[Byte](7, 8, 9)).toList === List[Byte](0, 0, 0, 0, 3, 7, 8, 9)
  }

  "no bytes, no messages; an empty message is a message" >> {
    decode(Nil) must beRight(Nil)
    decode(List(Chunk.array(GrpcFrames.frame(Array.emptyByteArray)))) must beRight(List(Nil))
  }

  "a message at the size limit passes; one byte more fails the stream" >> {
    decode(List(Chunk.array(GrpcFrames.frame(new Array[Byte](10)))), max = 10) must beRight
    malformed(0, 0, 0, 0, 11)("a 11-byte message: over 10")
  }

  "the length is unsigned" >> malformed(0, 255, 255, 255, 255)("a 4294967295-byte message: over 10")

  "compressed messages fail the stream: no message encoding is negotiated" >> {
    malformed(1, 0, 0, 0, 1, 42)("compressed flag 1: no message encoding negotiated")
    malformed(2, 0, 0, 0, 1, 42)("compressed flag 2: no message encoding negotiated")
  }

  "bytes that end inside a frame fail the stream" >> {
    malformed(0, 0, 0)("the bytes end inside a frame, 3 bytes in")
    malformed(0, 0, 0, 0, 4, 1, 2)("the bytes end inside a frame, 7 bytes in")
  }

object GrpcFramesSpec:

  /** `bytes` in pieces of `sizes` (cycled), as a socket might deliver them. */
  def pieces(bytes: Array[Byte], sizes: List[Int]): List[Chunk[Byte]] =
    @tailrec def go(at: Int, next: LazyList[Int], acc: List[Chunk[Byte]]): List[Chunk[Byte]] =
      if at >= bytes.length then acc.reverse
      else
        val size = next.head.min(bytes.length - at)
        go(at + size, next.tail, Chunk.array(bytes, at, size) :: acc)
    go(0, LazyList.continually(sizes).flatten, Nil)
