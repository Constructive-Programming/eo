package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.{Chain, Ior}
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification

/** Observable-identity behaviour spec for [[AvroFailure]] and the [[AvroFailure.parseInputIor]] /
  * [[AvroFailure.parseInputUnsafe]] dual-input parse helpers.
  *
  * Mirrors `JsonFailureSpec`'s consolidation density: one composite block per case + one block per
  * parse-helper code path.
  *
  * '''2026-04-29 consolidation.''' 9 per-case "constructible + message" blocks → 1 composite. The
  * Eq case stays separate since it lives on a different code path. The parse helpers also stay
  * separate (their own code paths).
  */
class AvroFailureSpec extends Specification:

  import AvroSpecFixtures.*

  // ---- Per-case constructibility + message format -----------------

  // covers: PathMissing constructible w/ "path missing" + step identifier in message,
  //   NotARecord constructible w/ "expected Avro record",
  //   NotAnArray constructible w/ "expected Avro array",
  //   IndexOutOfRange constructible w/ "size=N",
  //   DecodeFailed constructible w/ "decode failed" + wrapped cause message,
  //   BinaryParseFailed constructible w/ "binary" tag + wrapped cause message,
  //   JsonParseFailed constructible w/ "Avro JSON" tag + wrapped cause message,
  //   UnionResolutionFailed constructible w/ "union resolution" + alternatives in message,
  //   BadEnumSymbol constructible w/ invalid symbol + valid-set listing in message
  "AvroFailure: every case constructible + message contains its diagnostic anchors" >> {
    val pm: AvroFailure = AvroFailure.PathMissing(PathStep.Field("name"))
    val pmOk = (pm.message must contain("path missing"))
      .and(pm.message must contain("Field(name)"))

    val nr: AvroFailure = AvroFailure.NotARecord(PathStep.Field("x"))
    val nrOk = nr.message must contain("expected Avro record")

    val na: AvroFailure = AvroFailure.NotAnArray(PathStep.Index(3))
    val naOk = na.message must contain("expected Avro array")

    val ix: AvroFailure = AvroFailure.IndexOutOfRange(PathStep.Index(7), 3)
    val ixOk = ix.message must contain("size=3")

    val dCause = new RuntimeException("missing record field 'name'")
    val df: AvroFailure = AvroFailure.DecodeFailed(PathStep.Field("name"), dCause)
    val dfOk = (df.message must contain("decode failed"))
      .and(df.message must contain(dCause.getMessage))

    val bCause = new RuntimeException("binary went sideways")
    val bf: AvroFailure = AvroFailure.BinaryParseFailed(bCause)
    val bfOk = (bf.message must contain("binary")).and(bf.message must contain(bCause.getMessage))

    val jCause = new RuntimeException("json went sideways")
    val jf: AvroFailure = AvroFailure.JsonParseFailed(jCause)
    val jfOk =
      (jf.message must contain("Avro JSON")).and(jf.message must contain(jCause.getMessage))

    val ur: AvroFailure = AvroFailure.UnionResolutionFailed(
      List("null", "long", "string"),
      PathStep.UnionBranch("long"),
    )
    val urOk = (ur.message must contain("union resolution"))
      .and(ur.message must contain("long"))
      .and(ur.message must contain("string"))

    val be: AvroFailure =
      AvroFailure.BadEnumSymbol("MAGENTA", List("RED", "GREEN", "BLUE"), PathStep.Field("color"))
    val beOk = (be.message must contain("MAGENTA")).and(be.message must contain("RED"))

    pmOk
      .and(nrOk)
      .and(naOk)
      .and(ixOk)
      .and(dfOk)
      .and(bfOk)
      .and(jfOk)
      .and(urOk)
      .and(beOk)
  }

  // ---- Eq instance --------------------------------------------------

  // covers: structural equality is total + agrees with ==
  "AvroFailure Eq is total + agrees with ==" >> {
    val eq = summon[cats.Eq[AvroFailure]]
    val a: AvroFailure = AvroFailure.PathMissing(PathStep.Field("x"))
    val b: AvroFailure = AvroFailure.PathMissing(PathStep.Field("x"))
    val c: AvroFailure = AvroFailure.PathMissing(PathStep.Field("y"))
    (eq.eqv(a, b) === true).and(eq.eqv(a, c) === false)
  }

  // ---- Parse helpers (Ior + Unsafe) ---------------------------------

  // covers: parseInputIor passes IndexedRecord through, parseInputIor decodes good
  // bytes, parseInputIor surfaces BinaryParseFailed for bad bytes, parseInputUnsafe
  // passes IndexedRecord through, parseInputUnsafe decodes good bytes, parseInputUnsafe
  // returns an empty record on bad bytes
  "parseInputIor + parseInputUnsafe handle record / good bytes / bad bytes" >> {
    val p = Person("Alice", 30)
    val record = personRecord(p)
    val schema = personSchema
    val goodBytes = toBinary(record, schema)
    // A truncated payload (one byte) is unparseable under the Person schema.
    val badBytes: Array[Byte] = Array(0.toByte)

    // ---- Ior path ------
    val iorRecord = AvroFailure.parseInputIor(record, schema)
    val iorGood = AvroFailure.parseInputIor(goodBytes, schema)
    val iorBad = AvroFailure.parseInputIor(badBytes, schema)

    // `IndexedRecord` has reference equality; compare by identity for record input.
    val iorRecordOk = iorRecord match
      case Ior.Right(r) => (r eq record) === true
      case other        =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    val iorGoodOk = iorGood match
      case Ior.Right(r) =>
        // Round-trip: decoded fields match. Cast to GenericRecord (the runtime type) so we
        // can read by name; IndexedRecord only exposes positional `get(i: Int)`.
        val gr = r.asInstanceOf[GenericRecord]
        (gr.get("name").toString === p.name)
          .and(gr.get("age").asInstanceOf[Int] === p.age)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    val iorBadOk = iorBad match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get.isInstanceOf[AvroFailure.BinaryParseFailed] === true)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    // ---- Unsafe path ------
    val unsafeRecord = AvroFailure.parseInputUnsafe(record, schema)
    val unsafeGood = AvroFailure.parseInputUnsafe(goodBytes, schema)
    val unsafeBad = AvroFailure.parseInputUnsafe(badBytes, schema)

    val unsafeRecordOk = (unsafeRecord eq record) === true
    val unsafeGoodOk = unsafeGood.asInstanceOf[GenericRecord].get("name").toString === p.name
    // Bad bytes should produce a bare empty record under the Person schema, not throw.
    val unsafeBadOk = unsafeBad.getSchema === schema

    iorRecordOk
      .and(iorGoodOk)
      .and(iorBadOk)
      .and(unsafeRecordOk)
      .and(unsafeGoodOk)
      .and(unsafeBadOk)

    // Suppress "unused chain helper" warnings.
    val _ = Chain.empty[AvroFailure]
  }

  // covers: parseInputIor decodes a well-formed Avro JSON String, parseInputIor surfaces
  // JsonParseFailed for malformed JSON, parseInputUnsafe decodes a well-formed JSON String,
  // parseInputUnsafe returns a synthetic empty record on bad JSON
  "parseInputIor + parseInputUnsafe handle good JSON / bad JSON" >> {
    val p = Person("Alice", 30)
    val schema = personSchema
    val goodJson = """{"name":"Alice","age":30}"""
    val badJson = "not json at all"

    // ---- Ior path ------
    val iorGood = AvroFailure.parseInputIor(goodJson, schema)
    val iorBad = AvroFailure.parseInputIor(badJson, schema)

    val iorGoodOk = iorGood match
      case Ior.Right(r) =>
        val gr = r.asInstanceOf[GenericRecord]
        (gr.get("name").toString === p.name)
          .and(gr.get("age").asInstanceOf[Int] === p.age)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    val iorBadOk = iorBad match
      case Ior.Left(chain) =>
        (chain.length === 1L)
          .and(chain.headOption.get.isInstanceOf[AvroFailure.JsonParseFailed] === true)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    // ---- Unsafe path ------
    val unsafeGood = AvroFailure.parseInputUnsafe(goodJson, schema)
    val unsafeBad = AvroFailure.parseInputUnsafe(badJson, schema)

    val unsafeGoodOk = unsafeGood.asInstanceOf[GenericRecord].get("name").toString === p.name
    val unsafeBadOk = unsafeBad.getSchema === schema

    iorGoodOk.and(iorBadOk).and(unsafeGoodOk).and(unsafeBadOk)
  }

end AvroFailureSpec
