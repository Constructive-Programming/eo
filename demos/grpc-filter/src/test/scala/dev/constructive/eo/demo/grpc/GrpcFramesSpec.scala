package dev.constructive.eo.demo.grpc

import scala.annotation.tailrec
import scala.language.implicitConversions

import fs2.{Chunk, Fallible, Stream}
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** gRPC framing over bytes that arrive in arbitrary pieces. */
class GrpcFramesSpec extends Specification with ScalaCheck:

  import GrpcFramesSpec.{join, pieces}

  /** Bytes, in pieces, through [[GrpcFrames.splitFrames]]: the messages, or the failure. */
  def split(
      chunks: List[Chunk[Byte]],
      max: Int = GrpcFrames.DefaultMaxMessageBytes,
  ): Either[Throwable, List[List[Byte]]] =
    Stream
      .emits(chunks)
      .flatMap(Stream.chunk)
      .covary[Fallible]
      .through(GrpcFrames.splitFrames(max))
      .compile
      .toList
      .map(_.map(_.toList))

  def malformed(bytes: Int*)(reason: String) =
    split(List(Chunk.array(bytes.map(_.toByte).toArray)), max = 10) must beLeft(
      GrpcFrames.Malformed(reason)
    )

  private val messages = Gen.listOf(Gen.containerOf[Array, Byte](Arbitrary.arbitrary[Byte]))
  private val sizes = Gen.nonEmptyListOf(Gen.oneOf(Gen.choose(1, 8), Gen.choose(1, 300)))

  "messages come back whole however the bytes are cut" >> Prop.forAll(messages, sizes) {
    (messages, sizes) =>
      split(pieces(join(messages), sizes)) must beRight(messages.map(_.toList))
  }

  "a frame is a zero, the length big-endian, then the message" >> {
    GrpcFrames.frame(Chunk.array(Array[Byte](7, 8, 9))).toList === List[Byte](0, 0, 0, 0, 3, 7, 8,
      9)
  }

  "a message inside one chunk is a view of that chunk's array, also after one that straddles" >> {
    val messages = List("first", "second", "third").map(_.getBytes("UTF-8"))
    val framed = join(messages)
    // The first chunk ends inside the first message; the other two lie inside the second chunk.
    val (head, tail) = framed.splitAt(7)
    val out = Stream
      .emits(List(Chunk.array(head), Chunk.array(tail)))
      .flatMap(Stream.chunk)
      .through(GrpcFrames.splitFrames[Fallible]())
      .compile
      .toList
      .toOption
      .get
    (out.map(m => new String(m.toArray, "UTF-8")) === List("first", "second", "third"))
      .and(out.tail.forall(_.values eq tail) must beTrue)
  }

  "no bytes, no messages; an empty message is a message" >> {
    split(Nil) must beRight(Nil)
    split(List(GrpcFrames.frame(Chunk.empty))) must beRight(List(Nil))
  }

  "a message at the size limit passes; one byte more fails the stream" >> {
    split(List(GrpcFrames.frame(Chunk.array(new Array[Byte](10)))), max = 10) must beRight
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

  /** Messages through [[GrpcFrames.joinFrames]]: their frames, back to back. */
  def join(messages: Seq[Array[Byte]]): Array[Byte] =
    Stream.emits(messages.map(Chunk.array(_))).through(GrpcFrames.joinFrames).compile.to(Array)

  /** `bytes` in pieces of `sizes` (cycled), as a socket might deliver them. */
  def pieces(bytes: Array[Byte], sizes: List[Int]): List[Chunk[Byte]] =
    @tailrec def go(at: Int, next: LazyList[Int], acc: List[Chunk[Byte]]): List[Chunk[Byte]] =
      if at >= bytes.length then acc.reverse
      else
        val size = next.head.min(bytes.length - at)
        go(at + size, next.tail, Chunk.array(bytes, at, size) :: acc)
    go(0, LazyList.continually(sizes).flatten, Nil)
