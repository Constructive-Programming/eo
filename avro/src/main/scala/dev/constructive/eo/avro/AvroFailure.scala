package dev.constructive.eo.avro

import scala.util.control.NonFatal

import cats.Eq
import cats.data.{Chain, Ior}
import java.io.ByteArrayInputStream
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord, IndexedRecord}
import org.apache.avro.io.DecoderFactory

/** Structured failure surfaced by the default Ior-bearing surface of [[AvroPrism]].
  *
  * Every case carries a [[PathStep]] so the walk that produced the failure can point at the
  * specific cursor position that refused. The default enum `toString` keeps the structural
  * representation for testability; [[message]] gives a human-readable diagnostic.
  *
  * Mirrors `dev.constructive.eo.circe.JsonFailure` case-for-case, plus the schema-driven cases that
  * Avro adds: [[UnionResolutionFailed]], [[BadEnumSymbol]]. The two tied carriers differ on exactly
  * the schema-aware concept set; that's why eo-avro ships its own ADT rather than a shared one with
  * eo-circe.
  *
  * '''Gap-1 (per the eo-avro plan).''' Like [[PathStep]], this ADT is a deliberate divergence from
  * `JsonFailure`. Sharing would force Avro-specific cases into eo-circe.
  */
enum AvroFailure:

  /** Named field absent from its parent record at `step` (or absent map key). */
  case PathMissing(step: PathStep)

  /** Parent wasn't a record at `step` (so the walker couldn't look up a field). */
  case NotARecord(step: PathStep)

  /** Parent wasn't an array at `step` (so the walker couldn't index). */
  case NotAnArray(step: PathStep)

  /** Index was outside `[0, size)` at `step`. `size` is the actual array length. */
  case IndexOutOfRange(step: PathStep, size: Int)

  /** Codec refused at `step`. `step` is `PathStep.Field("")` at root-level decode failures on a
    * path-empty prism. The wrapped [[Throwable]] is whatever the kindlings decoder threw —
    * typically an `AvroRuntimeException`, an `AvroTypeException`, or a `ClassCastException` when
    * the runtime payload doesn't line up with the schema.
    */
  case DecodeFailed(step: PathStep, cause: Throwable)

  /** Input `Array[Byte]` didn't parse as an Avro binary record under the supplied schema. Surfaced
    * only by the `Avro | Array[Byte]` overloads; when the caller passes a parsed
    * [[org.apache.avro.generic.IndexedRecord]] directly this case cannot fire.
    */
  case BinaryParseFailed(cause: Throwable)

  /** Walker reached a `union<...>` value but none of the candidate branches matched the runtime
    * type. `branches` is the list of schema-declared alternatives; `step` carries the
    * [[PathStep.UnionBranch]] the walker was attempting to resolve. Schema-driven; no JSON
    * analogue.
    */
  case UnionResolutionFailed(branches: List[String], step: PathStep)

  /** Walker reached an enum value whose runtime symbol is not a member of the schema's declared
    * symbol set. Schema-driven; no JSON analogue.
    */
  case BadEnumSymbol(symbol: String, valid: List[String], step: PathStep)

  /** Human-readable diagnostic. Kept separate from `toString` so the default enum representation
    * remains useful for structural inspection / pattern-matching-in-tests.
    */
  def message: String = this match
    case PathMissing(s)              => s"path missing at $s"
    case NotARecord(s)               => s"expected Avro record at $s"
    case NotAnArray(s)               => s"expected Avro array at $s"
    case IndexOutOfRange(s, n)       => s"index out of range at $s (size=$n)"
    case DecodeFailed(s, c)          => s"decode failed at $s: ${c.getMessage}"
    case BinaryParseFailed(c)        => s"input bytes didn't parse as Avro binary: ${c.getMessage}"
    case UnionResolutionFailed(b, s) =>
      s"union resolution failed at $s (branches: ${b.mkString(", ")})"
    case BadEnumSymbol(sym, valid, s) =>
      s"bad enum symbol '$sym' at $s (valid: ${valid.mkString(", ")})"

object AvroFailure:

  /** Structural equality — two [[AvroFailure]] values are equal iff they are the same case with the
    * same arguments. [[Throwable]] cases ([[DecodeFailed]] and [[BinaryParseFailed]]) fall back to
    * reference equality; tests that need to assert on the failure shape pattern-match the case
    * instead of comparing whole values.
    *
    * Required for `Eq[Chain[AvroFailure]]` to be summonable at specs2-`===` call sites.
    */
  given Eq[AvroFailure] = Eq.fromUniversalEquals

  /** Resolve an `IndexedRecord | Array[Byte]` input to a parsed `IndexedRecord`, threading parse
    * failures through the Ior channel. Used by every dual-input-accepting overload on [[AvroPrism]]
    * so the parse step is uniform (same failure shape, same message format).
    *
    * When `input` is already an [[IndexedRecord]], this is a pure `Ior.Right`. When it's an
    * `Array[Byte]`, parse failures arrive as `Ior.Left(Chain(AvroFailure.BinaryParseFailed(t)))`.
    *
    * The `String` overload (Avro JSON wire format) is intentionally absent at v0.1.0 (per
    * OQ-avro-3); Unit 10 will add it behind an opaque `AvroJsonInput` wrapper.
    *
    * @param schema
    *   the reader schema used to decode binary input. Ignored for record input.
    */
  private[avro] def parseInputIor(
      input: IndexedRecord | Array[Byte],
      schema: Schema,
  ): Ior[Chain[AvroFailure], IndexedRecord] =
    input match
      case r: IndexedRecord => Ior.Right(r)
      case bs: Array[Byte]  =>
        decodeBinary(bs, schema) match
          case Right(r) => Ior.Right(r)
          case Left(t)  => Ior.Left(Chain.one(AvroFailure.BinaryParseFailed(t)))

  /** Resolve an `IndexedRecord | Array[Byte]` input, dropping failures. For the `*Unsafe` escape
    * hatches: parsed-record input passes through; bad bytes produce a synthetic empty record built
    * from the supplied schema.
    *
    * There's no meaningful silent fallback for unparseable bytes — callers who need parse
    * diagnostics must use the Ior-bearing default surface.
    *
    * @param schema
    *   the reader schema; also used to synthesise the empty-record fallback. The fallback is a bare
    *   [[org.apache.avro.generic.GenericData.Record]] with all positional slots zero-initialised
    *   (`null` for nullable fields, default values for the rest).
    */
  private[avro] def parseInputUnsafe(
      input: IndexedRecord | Array[Byte],
      schema: Schema,
  ): IndexedRecord =
    input match
      case r: IndexedRecord => r
      case bs: Array[Byte]  =>
        decodeBinary(bs, schema) match
          case Right(r) => r
          case Left(_)  => new org.apache.avro.generic.GenericData.Record(schema)

  /** Use apache-avro's `GenericDatumReader` to parse a binary payload under the supplied schema.
    * Kindlings' `AvroEncoder` / `AvroDecoder` operate over already-parsed `Any` payloads, leaving
    * binary serialisation to the apache-avro layer. This helper is the eo-avro counterpart to that
    * boundary.
    *
    * Catches every NonFatal Throwable (apache-avro's binary decoder throws a wide variety —
    * `IOException`, `EOFException`, `AvroRuntimeException`) and surfaces them through the
    * structured-failure channel.
    */
  private def decodeBinary(
      bytes: Array[Byte],
      schema: Schema,
  ): Either[Throwable, IndexedRecord] =
    try
      val reader = new GenericDatumReader[GenericRecord](schema)
      val decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null)
      Right(reader.read(null, decoder))
    catch case NonFatal(t) => Left(t)
