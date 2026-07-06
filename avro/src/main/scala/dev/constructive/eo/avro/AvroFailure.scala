package dev.constructive.eo.avro

import cats.Eq

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

  /** Input `String` didn't parse as Avro JSON wire format under the supplied schema. Surfaced only
    * by the `Avro | Array[Byte] | String` overloads; record / bytes input cannot trigger this case.
    * The wrapped [[Throwable]] is whatever apache-avro's `JsonDecoder` / `GenericDatumReader` threw
    * — typically an `AvroTypeException` for shape mismatches, an `IOException` for malformed JSON,
    * or an `org.apache.avro.AvroRuntimeException` for misc apache-avro complaints.
    */
  case JsonParseFailed(cause: Throwable)

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

  /** The byte-offset locator ([[AvroBinaryCursor]]) reached a [[PathStep]] kind it cannot resolve
    * to a byte span — currently [[PathStep.Index]]: array elements sit inside length-prefixed
    * blocks, so a single element's span is not graftable without rewriting the block framing.
    * Surfaced only by `sliceBytes` / `graftBytes`; the decode-based surfaces (`get` / `modify` /
    * `at`) keep supporting Index steps.
    */
  case UnsupportedSpanStep(step: PathStep)

  /** Input bytes are not a Confluent-framed payload — shorter than the 5-byte header, or the magic
    * byte isn't `0x00`. Surfaced only by [[ConfluentWire.strip]]; `reason` names the specific check
    * that refused.
    */
  case NotConfluentFramed(reason: String)

  /** The [[ConfluentWire.SchemaById]] hook threw while resolving `schemaId` to a writer schema — a
    * registry miss, a network error, whatever the injected lookup raised. Surfaced only by
    * [[ConfluentWire.resolve]] / [[ConfluentWire.confluent]].
    */
  case SchemaResolutionFailed(schemaId: Int, cause: Throwable)

  /** A Confluent-framed payload's writer schema (id `schemaId`) is not byte-identical to the reader
    * schema — their Avro parsing-canonical-form fingerprints differ — so the direct byte walk would
    * misread it. [[ConfluentWire.resolve]] / [[ConfluentWire.confluent]] GATE (refuse) here rather
    * than hand back bytes that would misdecode. To TRANSLATE the drift instead of refusing, use the
    * resolving reader ([[ConfluentWire.reader]] / [[ConfluentWire.resolving]]), which resolves
    * writer→reader via Avro's `ResolvingDecoder`. Surfaced only by the gating surface.
    */
  case SchemaMismatch(schemaId: Int, writerFingerprint: Long, readerFingerprint: Long)

  /** Encoding a value back to Avro binary failed — the write-side counterpart of [[DecodeFailed]].
    * Surfaced by the fallible-build seam (`T = Either[Chain[AvroFailure], Array[Byte]]`), e.g.
    * [[AvroBridge]]'s `from`, when the codec's `Any` payload can't be written under the target
    * schema. The wrapped [[Throwable]] is whatever apache-avro's `GenericDatumWriter` threw.
    */
  case EncodeFailed(cause: Throwable)

  /** Avro writer→reader schema resolution refused — the writer schema (resolved by id) and the
    * reader schema aren't compatible, so `ResolvingDecoder` can't translate the payload. The
    * wrapped [[Throwable]] is whatever apache-avro's resolving `GenericDatumReader` threw
    * (typically an `AvroTypeException`). Surfaced by [[AvroCodec.decodeResolvedRecord]] /
    * `decodeResolvedValue` and the `ConfluentWire` resolving reader.
    */
  case ResolveFailed(cause: Throwable)

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
    case JsonParseFailed(c)          => s"input string didn't parse as Avro JSON: ${c.getMessage}"
    case UnionResolutionFailed(b, s) =>
      s"union resolution failed at $s (branches: ${b.mkString(", ")})"
    case BadEnumSymbol(sym, valid, s) =>
      s"bad enum symbol '$sym' at $s (valid: ${valid.mkString(", ")})"
    case UnsupportedSpanStep(s)        => s"byte-span location unsupported at $s"
    case NotConfluentFramed(r)         => s"not a Confluent-framed payload: $r"
    case SchemaResolutionFailed(id, c) =>
      s"could not resolve writer schema for id $id: ${c.getMessage}"
    case SchemaMismatch(id, w, r) =>
      s"writer schema (id $id, fingerprint $w) differs from reader schema (fingerprint $r);"
        + " a resolving writer→reader decode is required"
    case EncodeFailed(c)  => s"value didn't encode to Avro binary: ${c.getMessage}"
    case ResolveFailed(c) => s"writer→reader schema resolution failed: ${c.getMessage}"

object AvroFailure:

  /** Structural equality — two [[AvroFailure]] values are equal iff they are the same case with the
    * same arguments. [[Throwable]]-bearing cases ([[DecodeFailed]], [[BinaryParseFailed]],
    * [[JsonParseFailed]], [[SchemaResolutionFailed]], [[EncodeFailed]], [[ResolveFailed]]) fall
    * back to reference equality; tests that need to assert on the failure shape pattern-match the
    * case instead of comparing whole values.
    *
    * Required for `Eq[Chain[AvroFailure]]` to be summonable at specs2-`===` call sites.
    */
  given Eq[AvroFailure] = Eq.fromUniversalEquals

end AvroFailure

/** Carries an [[AvroFailure]] as a `Throwable`, so an effectful reader (e.g.
  * [[ConfluentWire.reader]] under a `MonadThrow[F]`) can `raiseError` the structured failure into
  * `F`'s error channel. The pure surface keeps returning `Either[AvroFailure, …]`; this is only the
  * bridge to an `F` that fails.
  */
final class AvroFailureException(val failure: AvroFailure) extends RuntimeException(failure.message)
