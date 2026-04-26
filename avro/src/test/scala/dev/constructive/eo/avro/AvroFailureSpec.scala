package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.{Chain, Ior}
import org.apache.avro.generic.GenericRecord
import org.specs2.mutable.Specification
import vulcan.AvroError

/** Observable-identity behaviour spec for [[AvroFailure]] and the [[AvroFailure.parseInputIor]] /
  * [[AvroFailure.parseInputUnsafe]] dual-input parse helpers.
  *
  * Mirrors `JsonFailureSpec`'s consolidation density: one composite block per case + one block per
  * parse-helper code path.
  */
class AvroFailureSpec extends Specification:

  import AvroSpecFixtures.*

  // ---- Per-case constructibility + message format -----------------

  // covers: PathMissing constructible, message includes step identifier
  "AvroFailure.PathMissing constructible + message includes step" >> {
    val f: AvroFailure = AvroFailure.PathMissing(PathStep.Field("name"))
    (f.message must contain("path missing"))
      .and(f.message must contain("Field(name)"))
  }

  // covers: NotARecord constructible, distinctive message
  "AvroFailure.NotARecord constructible + distinctive message" >> {
    val f: AvroFailure = AvroFailure.NotARecord(PathStep.Field("x"))
    f.message must contain("expected Avro record")
  }

  // covers: NotAnArray constructible, distinctive message
  "AvroFailure.NotAnArray constructible + distinctive message" >> {
    val f: AvroFailure = AvroFailure.NotAnArray(PathStep.Index(3))
    f.message must contain("expected Avro array")
  }

  // covers: IndexOutOfRange constructible, message includes size
  "AvroFailure.IndexOutOfRange constructible + message includes size" >> {
    val f: AvroFailure = AvroFailure.IndexOutOfRange(PathStep.Index(7), 3)
    f.message must contain("size=3")
  }

  // covers: DecodeFailed constructible, message wraps the underlying AvroError
  "AvroFailure.DecodeFailed constructible + wraps cause message" >> {
    val cause = AvroError("missing record field 'name'")
    val f: AvroFailure = AvroFailure.DecodeFailed(PathStep.Field("name"), cause)
    (f.message must contain("decode failed"))
      .and(f.message must contain(cause.message))
  }

  // covers: BinaryParseFailed constructible, message wraps the underlying Throwable
  "AvroFailure.BinaryParseFailed constructible + wraps Throwable message" >> {
    val cause = new RuntimeException("binary went sideways")
    val f: AvroFailure = AvroFailure.BinaryParseFailed(cause)
    (f.message must contain("binary"))
      .and(f.message must contain("binary went sideways"))
  }

  // covers: UnionResolutionFailed constructible, message includes branches
  "AvroFailure.UnionResolutionFailed constructible + message includes branches" >> {
    val f: AvroFailure = AvroFailure.UnionResolutionFailed(
      List("null", "long", "string"),
      PathStep.UnionBranch("long"),
    )
    (f.message must contain("union resolution"))
      .and(f.message must contain("long"))
      .and(f.message must contain("string"))
  }

  // covers: BadEnumSymbol constructible, message includes invalid symbol + valid set
  "AvroFailure.BadEnumSymbol constructible + message includes valid set" >> {
    val f: AvroFailure =
      AvroFailure.BadEnumSymbol("MAGENTA", List("RED", "GREEN", "BLUE"), PathStep.Field("color"))
    (f.message must contain("MAGENTA"))
      .and(f.message must contain("RED"))
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

end AvroFailureSpec
