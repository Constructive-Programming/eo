package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for the bytes-in/bytes-out surface: `modifyBytes` / `placeBytes` (parse → optic
  * → re-encode), `sliceBytes` (byte-span extraction, [[AvroFragment]]) and `graftBytes` (fragment
  * splicing) on [[AvroPrism]] — Phase 1 of the zero-copy plan.
  *
  * The load-bearing witnesses:
  *
  *   - `modifyBytes` ≡ re-encode-of-`modify`-of-parse, byte-for-byte (encode determinism);
  *   - self-graft identity `graft(bytes, slice(bytes)) == bytes` — proves the located span is
  *     exactly `prefix ++ (index) ++ value ++ suffix`;
  *   - cross-payload graft ≡ `placeBytes` of the decoded fragment, byte-for-byte — the emit-path
  *     equivalence that lets a passthrough pipeline skip decode+re-encode entirely.
  */
class AvroBytesSpec extends Specification with ScalaCheck:

  import AvroSpecFixtures.*

  private given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
    yield Person(n, a)
  )

  // ---- modifyBytes / placeBytes -------------------------------------

  // covers: modifyBytes ≡ toBinary(modify(parse(bytes))) byte-for-byte, output decodes to the
  //   expected native value, modifyBytesUnsafe parity, placeBytes / placeBytesUnsafe same shape;
  //   bad bytes → Ior.Left(BinaryParseFailed) on the default tier, input pass-through (eq) on the
  //   Unsafe tier
  "modifyBytes/placeBytes ≡ parse→optic→re-encode byte-for-byte + bad bytes surface BinaryParseFailed" >> forAll {
    (p: Person) =>
      val nameL = codecPrism[Person].field(_.name)
      val bytes = toBinary(personRecord(p), personSchema)
      val expectedModify = toBinary(personRecord(p.copy(name = p.name.toUpperCase)), personSchema)
      val expectedPlace = toBinary(personRecord(p.copy(name = "Carol")), personSchema)

      val modifyOk = nameL.modifyBytes(bytes)(_.toUpperCase) match
        case Ior.Right(out) => java.util.Arrays.equals(out, expectedModify)
        case _              => false
      val modifyUnsafeOk =
        java.util.Arrays.equals(nameL.modifyBytesUnsafe(bytes)(_.toUpperCase), expectedModify)
      val decodesOk = codecPrism[Person].get(expectedModify) match
        case Ior.Right(q) => q == p.copy(name = p.name.toUpperCase)
        case _            => false

      val placeOk = nameL.placeBytes(bytes, "Carol") match
        case Ior.Right(out) => java.util.Arrays.equals(out, expectedPlace)
        case _              => false
      val placeUnsafeOk = java.util.Arrays.equals(nameL.placeBytesUnsafe(bytes, "Carol"), expectedPlace)

      val badBytes: Array[Byte] = Array(0.toByte)
      val badOk = nameL.modifyBytes(badBytes)(identity) match
        case Ior.Left(chain) =>
          chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryParseFailed])
        case _ => false
      val badUnsafeOk = nameL.modifyBytesUnsafe(badBytes)(identity) eq badBytes

      modifyOk && modifyUnsafeOk && decodesOk && placeOk && placeUnsafeOk && badOk && badUnsafeOk
  }

  // covers: re-encoding under a root schema the encoded record can't satisfy surfaces
  //   BinaryEncodeFailed (the encode mirror of BinaryParseFailed); the Unsafe tier passes the
  //   input bytes through unchanged
  "placeBytes surfaces BinaryEncodeFailed when the root schema refuses the encoded record" >> {
    // Root schema declares age: string; the kindlings encoder produces age: Int — the writer
    // trips a ClassCastException at the encode boundary, after parse and place both succeed.
    val stringAgeSchema =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "name",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      fields.add(
        new org.apache.avro.Schema.Field(
          "age",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val viol = buildRecord(stringAgeSchema)("name" -> "Alice", "age" -> "thirty")
    val bytes = toBinary(viol, stringAgeSchema)
    val rootP = codecPrism[Person](stringAgeSchema)

    val defaultOk = rootP.placeBytes(bytes, Person("Bob", 1)) match
      case Ior.Left(chain) =>
        chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryEncodeFailed]) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
    val unsafeOk = (rootP.placeBytesUnsafe(bytes, Person("Bob", 1)) eq bytes) === true

    defaultOk.and(unsafeOk)
  }

  // ---- sliceBytes -----------------------------------------------------

  // covers: scalar mid-record slice — fragment schema is the field schema, no branch ordinal,
  //   re-encoding the decoded fragment value under the fragment schema reproduces the slice
  //   byte-for-byte; self-graft identity (prefix + slice + suffix == original);
  //   union-branch slice — branch index stripped (fragment re-encodes as the bare branch record),
  //   branch ordinal reported, self-graft identity (prefix + synthesized index + slice + suffix
  //   == original); sliceBytesUnsafe parity on both shapes
  "sliceBytes: fragment fidelity + self-graft identity on scalar and union-branch focuses" >> {
    val e = WireEnvelope("env-1", 7L, Cash(100L), "keep-me")
    val bytes = toBinary(envelopeRecord(e), envelopeSchema)

    // ---- scalar mid-record focus: seq (long between id and payment) ----
    val seqL = codecPrism[WireEnvelope].field(_.seq)
    val seqOk = seqL.sliceBytes(bytes) match
      case Ior.Right(frag) =>
        val schemaOk = frag.schema.getType === org.apache.avro.Schema.Type.LONG
        val ordinalOk = frag.branchOrdinal === None
        val fidelity =
          java.util.Arrays.equals(frag.bytes, toBinaryValue(java.lang.Long.valueOf(7L), frag.schema)) === true
        val selfGraft = seqL.graftBytes(bytes, frag.bytes) match
          case Ior.Right(out) => java.util.Arrays.equals(out, bytes) === true
          case other          =>
            org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
        val unsafeParity =
          seqL.sliceBytesUnsafe(bytes).exists(f => java.util.Arrays.equals(f.bytes, frag.bytes)) === true
        schemaOk.and(ordinalOk).and(fidelity).and(selfGraft).and(unsafeParity)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    // ---- union-branch focus: payment.union[Cash] ----
    val cashL = codecPrism[WireEnvelope].field(_.payment).union[Cash]
    val paymentSchema = envelopeSchema.getField("payment").schema
    val cashOk = cashL.sliceBytes(bytes) match
      case Ior.Right(frag) =>
        val schemaOk = frag.schema.getFullName === "Cash"
        // The ordinal is Cash's position in the union schema, and the index bytes are STRIPPED:
        // the fragment re-encodes byte-for-byte as a bare Cash record (no union framing).
        val cashOrdinal = paymentSchema.getIndexNamed("Cash").intValue
        val ordinalOk = frag.branchOrdinal === Some(cashOrdinal)
        val decoded = fromBinaryValue(frag.bytes, frag.schema)
        val fidelity =
          java.util.Arrays.equals(frag.bytes, toBinaryValue(decoded, frag.schema)) === true
        val decodedOk =
          decoded.asInstanceOf[GenericRecord].get("amount").asInstanceOf[Long] === 100L
        val selfGraft = cashL.graftBytes(bytes, frag.bytes) match
          case Ior.Right(out) => java.util.Arrays.equals(out, bytes) === true
          case other          =>
            org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
        schemaOk.and(ordinalOk).and(fidelity).and(decodedOk).and(selfGraft)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    // ---- union-TYPED field focused WITHOUT .union[Branch]: index bytes are part of the span ----
    val paymentL = codecPrism[WireEnvelope].field(_.payment)
    val unionFieldOk = paymentL.sliceBytes(bytes) match
      case Ior.Right(frag) =>
        (frag.branchOrdinal === None)
          .and(frag.schema.getType === org.apache.avro.Schema.Type.UNION)
          .and(
            java.util.Arrays.equals(frag.bytes, toBinaryValue(cashRecordOf(100L), frag.schema)) === true
          )
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    seqOk.and(cashOk).and(unionFieldOk)
  }

  // ---- graftBytes -----------------------------------------------------

  // covers: cross-payload graft equals placeBytes of the decoded fragment byte-for-byte (the
  //   decode-free emit-path equivalence); mid-record graft preserves the fields AFTER the focused
  //   field (note survives); union-focused graft onto a DIFFERENT runtime branch synthesizes the
  //   prism's branch index (Card → Cash switch); graftBytesUnsafe parity
  "graftBytes: graft(output, slice(input)) ≡ place(decode(fragment)) + union branch switching" >> {
    val input = WireEnvelope("in", 1L, Cash(100L), "in-note")
    val output = WireEnvelope("out", 2L, Card("4111"), "out-note")
    val inputBytes = toBinary(envelopeRecord(input), envelopeSchema)
    val outputBytes = toBinary(envelopeRecord(output), envelopeSchema)

    val cashL = codecPrism[WireEnvelope].field(_.payment).union[Cash]
    val paymentL = codecPrism[WireEnvelope].field(_.payment)

    val frag = cashL.sliceBytes(inputBytes) match
      case Ior.Right(f) => f
      case other        => sys.error(s"slice failed: $other")

    // Output currently sits on the Card branch; grafting through .union[Cash] must synthesize
    // Cash's index — byte-identical to routing the decoded value through placeBytes.
    val expected = paymentL.placeBytes(outputBytes, Cash(100L)) match
      case Ior.Right(bs) => bs
      case other         => sys.error(s"placeBytes failed: $other")

    val graftOk = cashL.graftBytes(outputBytes, frag.bytes) match
      case Ior.Right(out) => java.util.Arrays.equals(out, expected) === true
      case other          =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result
    val unsafeOk =
      java.util.Arrays.equals(cashL.graftBytesUnsafe(outputBytes, frag.bytes), expected) === true

    // Decode the grafted payload: payload switched, every other field (incl. the suffix field
    // `note`, AFTER the focused union) carries the OUTPUT's values.
    val decodedOk = codecPrism[WireEnvelope].get(cashL.graftBytesUnsafe(outputBytes, frag.bytes)) match
      case Ior.Right(env) => env === WireEnvelope("out", 2L, Cash(100L), "out-note")
      case other          =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    graftOk.and(unsafeOk).and(decodedOk)
  }

  // ---- Failure paths ---------------------------------------------------

  // covers: PathStep.Index in the prism path → UnsupportedSpanStep on slice AND graft (default
  //   tier), input pass-through / None on the Unsafe tier; truncated bytes → BinaryParseFailed;
  //   union-branch slice on the wrong runtime branch → UnionResolutionFailed (strict terminal);
  //   missing field in the root schema → PathMissing
  "sliceBytes/graftBytes failure paths: UnsupportedSpanStep, BinaryParseFailed, UnionResolutionFailed" >> {
    val basket = Basket("Alice", List(Order("apple", 1.5, 2)))
    val basketBytes = toBinary(basketRecord(basket), basketSchema)
    val itemL = codecPrism[Basket].field(_.items).at(0)

    val indexSliceOk = itemL.sliceBytes(basketBytes) match
      case Ior.Left(chain) =>
        chain.headOption.contains(AvroFailure.UnsupportedSpanStep(PathStep.Index(0))) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
    val indexGraftOk = itemL.graftBytes(basketBytes, Array(0.toByte)) match
      case Ior.Left(chain) =>
        chain.headOption.contains(AvroFailure.UnsupportedSpanStep(PathStep.Index(0))) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
    val indexUnsafeOk = (itemL.sliceBytesUnsafe(basketBytes) === None)
      .and((itemL.graftBytesUnsafe(basketBytes, Array(0.toByte)) eq basketBytes) === true)

    val personBytes = toBinary(personRecord(Person("Alice", 30)), personSchema)
    val truncated = java.util.Arrays.copyOf(personBytes, personBytes.length - 1)
    val truncatedOk = codecPrism[Person].field(_.age).sliceBytes(truncated) match
      case Ior.Left(chain) =>
        chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryParseFailed]) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    val noneTx = transactionRecord(Transaction("t-1", None))
    val noneBytes = toBinary(noneTx, transactionSchema)
    val wrongBranchOk =
      codecPrism[Transaction].field(_.amount).union[Long].sliceBytes(noneBytes) match
        case Ior.Left(chain) =>
          chain.headOption.get match
            case AvroFailure.UnionResolutionFailed(branches, PathStep.UnionBranch("long")) =>
              branches === List("null", "long")
            case other =>
              org
                .specs2
                .execute
                .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result
        case other =>
          org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    // PathMissing is schema-driven for the byte walker: the prism's ROOT schema must lack the
    // field (the walker never sees the record, only bytes + schema).
    val ageOnlySchema =
      val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
      fields.add(
        new org.apache.avro.Schema.Field(
          "age",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val ageOnlyBytes = toBinary(buildRecord(ageOnlySchema)("age" -> Integer.valueOf(30)), ageOnlySchema)
    val missingOk = codecPrism[Person](ageOnlySchema).field(_.name).sliceBytes(ageOnlyBytes) match
      case Ior.Left(chain) =>
        chain.headOption.contains(AvroFailure.PathMissing(PathStep.Field("name"))) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    indexSliceOk
      .and(indexGraftOk)
      .and(indexUnsafeOk)
      .and(truncatedOk)
      .and(wrongBranchOk)
      .and(missingOk)
  }

  /** The runtime union value for `payment = Cash(amount)` — a bare Cash record, as the writer
    * sees it when encoding the union-typed field.
    */
  private def cashRecordOf(amount: Long): IndexedRecord =
    summon[AvroCodec[Cash]].encode(Cash(amount)).asInstanceOf[IndexedRecord]

end AvroBytesSpec
