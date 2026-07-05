package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.data.{Chain, Ior}
import org.apache.avro.{Schema, SchemaNormalization}

/** Confluent-framed read face of an [[AvroPrism]] — reachable only through [[AvroPrism.confluent]].
  *
  * Where the plain byte optic ([[AvroPrism]]) assumes its input bytes are already headerless and
  * encoded under exactly this prism's reader schema, this face consumes a full Confluent payload:
  *
  *   1. [[ConfluentWire.strip]] the 5-byte header → `(schemaId, body)`;
  *   2. resolve the writer schema for `schemaId` via the injected [[ConfluentWire.SchemaById]]
  *      hook;
  *   3. classify by Avro parsing-canonical-form fingerprint
  *      ([[org.apache.avro.SchemaNormalization.parsingFingerprint64]]): when the writer fingerprint
  *      equals the reader's, the body is byte-identical under both schemas, so the existing byte
  *      walk / record decode is exact — apply the optic directly;
  *   4. otherwise surface [[AvroFailure.SchemaMismatch]] rather than decode under the wrong schema.
  *
  * The resolving writer→reader `ResolvingDecoder` fallback for the mismatch case is deliberately
  * not shipped yet — this face covers the common byte-exact path (the same writer as the reader,
  * the overwhelmingly common Kafka-consumer shape) and refuses loudly on drift. `SchemaMismatch`
  * carries both fingerprints so a caller can route to their own resolving decode until eo grows
  * one.
  *
  * ponytail: read-only for now (`get` / `getOptionUnsafe`). Modify-and-re-frame would have to
  * decide which schema id to re-attach; add it when a caller actually needs framed writes.
  */
final class AvroConfluentPrism[A] private[avro] (
    private[avro] val focus: AvroFocus[A],
    private[avro] val readerSchema: Schema,
    private[avro] val schemaById: ConfluentWire.SchemaById,
):

  /** Reader fingerprint, computed once — the byte-exact gate compares every payload's writer
    * fingerprint against this.
    */
  private val readerFingerprint: Long =
    SchemaNormalization.parsingFingerprint64(readerSchema)

  /** Strip the Confluent header, resolve + fingerprint-gate the writer schema, and (on a byte-exact
    * match) decode the body under the reader schema and apply the optic. Every failure — bad frame,
    * unresolvable id, schema drift, parse / walk failure — arrives on the Ior channel.
    */
  def get(framed: Array[Byte]): Ior[Chain[AvroFailure], A] =
    ConfluentWire.strip(framed) match
      case Left(f)      => Ior.left(Chain.one(f))
      case Right(frame) =>
        resolveWriter(frame.schemaId) match
          case Left(f)       => Ior.left(Chain.one(f))
          case Right(writer) =>
            val writerFingerprint = SchemaNormalization.parsingFingerprint64(writer)
            if writerFingerprint == readerFingerprint then
              AvroFailure.parseInputIor(frame.body, readerSchema).flatMap(focus.readIor)
            else
              Ior.left(
                Chain.one(
                  AvroFailure.SchemaMismatch(frame.schemaId, writerFingerprint, readerFingerprint)
                )
              )

  /** Silent counterpart to [[get]] — `None` on any failure (bad frame, unresolvable id, schema
    * mismatch, or a parse / walk miss).
    */
  def getOptionUnsafe(framed: Array[Byte]): Option[A] =
    get(framed).right

  /** Run the injected hook, catching any throw into [[AvroFailure.SchemaResolutionFailed]]. */
  private def resolveWriter(schemaId: Int): Either[AvroFailure, Schema] =
    try Right(schemaById(schemaId))
    catch case NonFatal(t) => Left(AvroFailure.SchemaResolutionFailed(schemaId, t))

end AvroConfluentPrism
