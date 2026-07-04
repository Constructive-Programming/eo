package dev.constructive.eo.avro

import org.apache.avro.Schema

/** Confluent Schema Registry wire-format framing for binary Avro payloads: a 5-byte header — magic
  * byte `0x00` followed by the big-endian 4-byte schema id — then the Avro binary body.
  *
  * Registry-agnostic by design: no registry client, no new dependencies. [[strip]] / [[attach]]
  * only move the frame; resolving a schema id to a [[Schema]] is the caller's job, for which the
  * [[SchemaById]] alias is the hook (plug in a registry client, a static map, a cache — whatever
  * the deployment owns).
  */
object ConfluentWire:

  /** The Confluent magic byte — always `0x00` in the current wire format. */
  val Magic: Byte = 0x00

  /** Header length: 1 magic byte + 4 big-endian schema-id bytes. */
  val HeaderLength: Int = 5

  /** Registry hook — resolve a Confluent schema id to its [[Schema]]. Purely an alias so
    * signatures that need resolution stay registry-agnostic; this module never calls it.
    */
  type SchemaById = Int => Schema

  /** A stripped Confluent frame: the schema id and the Avro binary body.
    *
    * `body` is a COPY of the payload bytes, not a zero-copy offset view: `Array[Byte]` cannot
    * carry an offset, and every downstream consumer in this module ([[AvroPrism]]'s dual-input
    * surface, `sliceBytes` / `graftBytes`) takes whole arrays. The copy is one `arraycopy` of
    * `length - 5` bytes — noise next to any decode that follows. A true offset view is a concern
    * for the deferred byte-native optic surface.
    */
  final case class Framed(schemaId: Int, body: Array[Byte])

  /** Validate and strip the 5-byte Confluent header. Fails structurally
    * ([[AvroFailure.NotConfluentFramed]]) on inputs shorter than the header or whose magic byte
    * isn't `0x00`.
    */
  def strip(bytes: Array[Byte]): Either[AvroFailure, Framed] =
    if bytes.length < HeaderLength then
      Left(
        AvroFailure.NotConfluentFramed(
          s"payload is ${bytes.length} bytes, shorter than the $HeaderLength-byte header"
        )
      )
    else if bytes(0) != Magic then
      Left(AvroFailure.NotConfluentFramed(s"magic byte is 0x${"%02x".format(bytes(0))}, not 0x00"))
    else
      val schemaId =
        ((bytes(1) & 0xff) << 24) |
          ((bytes(2) & 0xff) << 16) |
          ((bytes(3) & 0xff) << 8) |
          (bytes(4) & 0xff)
      val body = new Array[Byte](bytes.length - HeaderLength)
      System.arraycopy(bytes, HeaderLength, body, 0, body.length)
      Right(Framed(schemaId, body))

  /** Frame an Avro binary body under `schemaId` — the inverse of [[strip]]:
    * `strip(attach(id, body)) == Right(Framed(id, body))` for every id / body.
    */
  def attach(schemaId: Int, body: Array[Byte]): Array[Byte] =
    val out = new Array[Byte](HeaderLength + body.length)
    out(0) = Magic
    out(1) = ((schemaId >>> 24) & 0xff).toByte
    out(2) = ((schemaId >>> 16) & 0xff).toByte
    out(3) = ((schemaId >>> 8) & 0xff).toByte
    out(4) = (schemaId & 0xff).toByte
    System.arraycopy(body, 0, out, HeaderLength, body.length)
    out

end ConfluentWire
