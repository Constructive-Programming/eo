package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import dev.constructive.eo.data.Affine
import dev.constructive.eo.optics.Optic.*
import java.util.{ArrayList, Arrays}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericRecord, IndexedRecord}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroPrism]]'s wire-form surfaces: the byte-carried default optic (`to` /
  * `.modify` / `.replace` splice on the binary payload), `sliceBytes` (byte-span extraction,
  * [[AvroFragment]]) and `graftBytes` (fragment splicing).
  *
  * The load-bearing witnesses:
  *
  *   - `.modify` / `.replace` on the wire form ≡ record-rebuild + re-encode, byte-for-byte (encode
  *     determinism) — the splice write is indistinguishable from the decode/re-encode path;
  *   - self-graft identity `graft(bytes, slice(bytes)) == bytes` — proves the located span is
  *     exactly `prefix ++ (index) ++ value ++ suffix`;
  *   - cross-payload graft ≡ `.replace` of the decoded fragment, byte-for-byte — the emit-path
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

  // ---- byte-carried default surface ----------------------------------

  // covers: AvroPrism IS Optic[Array[Byte], Array[Byte], A, A, Affine] — `to` Hits with the
  //   decoded focus; `.modify` / `.replace` splice byte-for-byte what a record rebuild +
  //   re-encode produces (siblings preserved by full byte equality, encode determinism);
  //   `.modify(identity)` is the identity on the wire form; the spliced payload decodes to the
  //   expected native value through the root prism
  "AvroPrism default surface: read/modify/replace on the wire form ≡ record rebuild byte-for-byte" >> forAll {
    (p: Person) =>
      val nameL = codecPrism[Person].field(_.name)
      val bytes = toBinary(personRecord(p), personSchema)
      val expectedModify = toBinary(personRecord(p.copy(name = p.name.toUpperCase)), personSchema)
      val expectedPlace = toBinary(personRecord(p.copy(name = "Carol")), personSchema)

      val readOk = nameL.to(bytes) match
        case h: Affine.Hit[nameL.X, String]  => h.b == p.name
        case _: Affine.Miss[nameL.X, String] => false

      val modifyOk = Arrays.equals(nameL.modify(_.toUpperCase)(bytes), expectedModify)
      val replaceOk = Arrays.equals(nameL.replace("Carol")(bytes), expectedPlace)
      val identityOk = Arrays.equals(nameL.modify(identity[String])(bytes), bytes)
      val decodesOk = codecPrism[Person]
        .getOption(nameL.modify(_.toUpperCase)(bytes))
        .contains(p.copy(name = p.name.toUpperCase))

      readOk && modifyOk && replaceOk && identityOk && decodesOk
  }

  // covers: Miss pass-through — unparseable bytes flow through `.modify` untouched (reference
  //   equality: from(Miss) returns the original array); unsupported index paths Miss the same
  //   way; the .record face keeps the Ior diagnostics for the same bad input (BinaryParseFailed)
  "AvroPrism Miss pass-through on bad bytes / index paths; .record keeps Ior diagnostics" >> {
    // A single 0x00 is a VALID prefix for the name field (empty string), so the truncation must
    // bite past it: focus age, whose walk hits EOF after skipping name.
    val ageL = codecPrism[Person].field(_.age)
    val badBytes: Array[Byte] = Array(0.toByte)
    val badOk = (ageL.modify(identity[Int])(badBytes) eq badBytes) === true

    val basket = Basket("ann", List(Order("tea", 2.5, 1)))
    val basketBytes = toBinary(basketRecord(basket), basketSchema)
    val itemL = codecPrism[Basket].field(_.items).at(0)
    val indexOk = (itemL.modify(identity[Order])(basketBytes) eq basketBytes) === true

    val diagOk = ageL.record.modify(identity[Int])(badBytes) match
      case Ior.Left(chain) =>
        chain.headOption.exists(_.isInstanceOf[AvroFailure.BinaryParseFailed]) === true
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    badOk.and(indexOk).and(diagOk)
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
      case Right(frag) =>
        val schemaOk = frag.schema.getType === org.apache.avro.Schema.Type.LONG
        val ordinalOk = frag.branchOrdinal === None
        val fidelity =
          java
            .util
            .Arrays
            .equals(frag.bytes, toBinaryValue(java.lang.Long.valueOf(7L), frag.schema)) === true
        val selfGraft = seqL.graftBytes(bytes, frag.bytes) match
          case Right(out) => Arrays.equals(out, bytes) === true
          case other      =>
            org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
        val unsafeParity =
          seqL
            .sliceBytesUnsafe(bytes)
            .exists(f => Arrays.equals(f.bytes, frag.bytes)) === true
        schemaOk.and(ordinalOk).and(fidelity).and(selfGraft).and(unsafeParity)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    // ---- union-branch focus: payment.union[Cash] ----
    val cashL = codecPrism[WireEnvelope].field(_.payment).union[Cash]
    val paymentSchema = envelopeSchema.getField("payment").schema
    val cashName = summon[AvroCodec[Cash]].schema.getFullName
    val cashOk = cashL.sliceBytes(bytes) match
      case Right(frag) =>
        val schemaOk = frag.schema.getFullName === cashName
        // The ordinal is Cash's position in the union schema, and the index bytes are STRIPPED:
        // the fragment re-encodes byte-for-byte as a bare Cash record (no union framing).
        val cashOrdinal = paymentSchema.getIndexNamed(cashName).intValue
        val ordinalOk = frag.branchOrdinal === Some(cashOrdinal)
        val decoded = fromBinaryValue(frag.bytes, frag.schema)
        val fidelity =
          Arrays.equals(frag.bytes, toBinaryValue(decoded, frag.schema)) === true
        val decodedOk =
          decoded.asInstanceOf[GenericRecord].get("amount").asInstanceOf[Long] === 100L
        val selfGraft = cashL.graftBytes(bytes, frag.bytes) match
          case Right(out) => Arrays.equals(out, bytes) === true
          case other      =>
            org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
        schemaOk.and(ordinalOk).and(fidelity).and(decodedOk).and(selfGraft)
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    // ---- union-TYPED field focused WITHOUT .union[Branch]: index bytes are part of the span ----
    val paymentL = codecPrism[WireEnvelope].field(_.payment)
    val unionFieldOk = paymentL.sliceBytes(bytes) match
      case Right(frag) =>
        (frag.branchOrdinal === None)
          .and(frag.schema.getType === org.apache.avro.Schema.Type.UNION)
          .and(
            java
              .util
              .Arrays
              .equals(frag.bytes, toBinaryValue(cashRecordOf(100L), frag.schema)) === true
          )
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    seqOk.and(cashOk).and(unionFieldOk)
  }

  // ---- graftBytes -----------------------------------------------------

  // covers: cross-payload graft equals `.replace` of the decoded fragment byte-for-byte (the
  //   decode-free emit-path equivalence); mid-record graft preserves the fields AFTER the focused
  //   field (note survives); union-focused graft onto a DIFFERENT runtime branch synthesizes the
  //   prism's branch index (Card → Cash switch); graftBytesUnsafe parity
  "graftBytes: graft(output, slice(input)) ≡ replace(decode(fragment)) + union branch switching" >> {
    val input = WireEnvelope("in", 1L, Cash(100L), "in-note")
    val output = WireEnvelope("out", 2L, Card("4111"), "out-note")
    val inputBytes = toBinary(envelopeRecord(input), envelopeSchema)
    val outputBytes = toBinary(envelopeRecord(output), envelopeSchema)

    val cashL = codecPrism[WireEnvelope].field(_.payment).union[Cash]
    val paymentL = codecPrism[WireEnvelope].field(_.payment)

    val frag = cashL.sliceBytes(inputBytes) match
      case Right(f)  => f
      case Left(err) => sys.error(s"slice failed: $err")

    // Output currently sits on the Card branch; grafting through .union[Cash] must synthesize
    // Cash's index — byte-identical to routing the decoded value through the union-typed field's
    // own `.replace` (which re-encodes index + value under the union schema).
    val expected = paymentL.replace(Cash(100L))(outputBytes)

    val graftOk = cashL.graftBytes(outputBytes, frag.bytes) match
      case Right(out) => Arrays.equals(out, expected) === true
      case other      =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
    val unsafeOk =
      Arrays.equals(cashL.graftBytesUnsafe(outputBytes, frag.bytes), expected) === true

    // Decode the grafted payload: payload switched, every other field (incl. the suffix field
    // `note`, AFTER the focused union) carries the OUTPUT's values.
    val decodedOk =
      codecPrism[WireEnvelope].record.get(cashL.graftBytesUnsafe(outputBytes, frag.bytes)) match
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
      case Left(failure) =>
        failure === AvroFailure.UnsupportedSpanStep(PathStep.Index(0))
      case other =>
        org.specs2.execute.Failure(s"expected Left, got $other"): org.specs2.execute.Result
    val indexGraftOk = itemL.graftBytes(basketBytes, Array(0.toByte)) match
      case Left(failure) =>
        failure === AvroFailure.UnsupportedSpanStep(PathStep.Index(0))
      case other =>
        org.specs2.execute.Failure(s"expected Left, got $other"): org.specs2.execute.Result
    val indexUnsafeOk = (itemL.sliceBytesUnsafe(basketBytes) === None)
      .and((itemL.graftBytesUnsafe(basketBytes, Array(0.toByte)) eq basketBytes) === true)

    val personBytes = toBinary(personRecord(Person("Alice", 30)), personSchema)
    val truncated = Arrays.copyOf(personBytes, personBytes.length - 1)
    val truncatedOk = codecPrism[Person].field(_.age).sliceBytes(truncated) match
      case Left(failure) =>
        failure.isInstanceOf[AvroFailure.BinaryParseFailed] === true
      case other =>
        org.specs2.execute.Failure(s"expected Left, got $other"): org.specs2.execute.Result

    val noneTx = transactionRecord(Transaction("t-1", None))
    val noneBytes = toBinary(noneTx, transactionSchema)
    val wrongBranchOk =
      codecPrism[Transaction].field(_.amount).union[Long].sliceBytes(noneBytes) match
        case Left(AvroFailure.UnionResolutionFailed(branches, PathStep.UnionBranch("long"))) =>
          branches === List("null", "long")
        case other =>
          org
            .specs2
            .execute
            .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result

    // Sibling strictTerminalUnion=true call sites (AvroPrism.to / .sliceBytesUnsafe) must refuse
    // the same wrong-branch payload as sliceBytes above, not just the sliceBytes call site.
    val toMissOk =
      codecPrism[Transaction].field(_.amount).union[Long].to(noneBytes) match
        case _: Affine.Miss[?, ?] => true
        case _                    => false
    val sliceUnsafeMissOk =
      codecPrism[Transaction].field(_.amount).union[Long].sliceBytesUnsafe(noneBytes) === None

    // PathMissing is schema-driven for the byte walker: the prism's ROOT schema must lack the
    // field (the walker never sees the record, only bytes + schema). `.field(_.name)` now resolves
    // by DECLARATION POSITION (issue #35), so on this drifted 1-field schema it would land on the
    // 0th schema field ("age"); to assert a genuine name-miss we navigate by explicit schema name
    // via the `.fieldNamed` escape hatch, which bypasses position resolution.
    val ageOnlySchema =
      val fields = new ArrayList[Schema.Field]()
      fields.add(
        new Schema.Field(
          "age",
          org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT),
          null,
          null,
        )
      )
      org.apache.avro.Schema.createRecord("Person", null, "eo.avro.test", false, fields)
    val ageOnlyBytes =
      toBinary(buildRecord(ageOnlySchema)("age" -> Integer.valueOf(30)), ageOnlySchema)
    // `codecPrism` no longer takes an explicit schema (the reader schema always matches the
    // codec's). To exercise the walker on a ROOT schema that drifted from the codec — bytes under a
    // narrower schema than `Person`'s — build the prism directly with the internal constructor.
    val ageOnlyPrism =
      new AvroPrism[Person](
        new AvroFocus.Leaf[Person](Array.empty[PathStep], summon[AvroCodec[Person]]),
        ageOnlySchema,
      )
    val missingOk =
      ageOnlyPrism.fieldNamed[String]("name").sliceBytes(ageOnlyBytes) match
        case Left(failure) =>
          failure === AvroFailure.PathMissing(PathStep.Field("name"))
        case other =>
          org.specs2.execute.Failure(s"expected Left, got $other"): org.specs2.execute.Result

    indexSliceOk
      .and(indexGraftOk)
      .and(indexUnsafeOk)
      .and(truncatedOk)
      .and(wrongBranchOk)
      .and(missingOk)
      .and(toMissOk === true)
      .and(sliceUnsafeMissOk)
  }

  // ---- byte-carried traversal ------------------------------------------

  // covers: AvroTraversal IS Optic[Array[Byte], Array[Byte], A, A, MultiFocus[PSVec]] — reads
  //   fold every element's focus straight off the wire; `.modify` splices every element
  //   byte-for-byte vs the record-rebuild + re-encode path (sibling fields preserved by full
  //   byte equality); `.modify(identity)` is the identity on the wire form; whole-walk failure
  //   passes the input through by reference; empty arrays read as empty and write as identity
  "AvroTraversal default surface: per-element read/modify on the wire form ≡ record rebuild" >> {
    val basket = Basket("ann", List(Order("tea", 2.5, 1), Order("mate", 4.0, 2)))
    val bytes = toBinary(basketRecord(basket), basketSchema)
    val namesT = codecPrism[Basket].items.each.name

    val readOk = namesT.foldMap(List(_))(bytes) === List("tea", "mate")

    val expected = toBinary(
      basketRecord(basket.copy(items = basket.items.map(o => o.copy(name = o.name.toUpperCase)))),
      basketSchema,
    )
    val modifyOk = Arrays.equals(namesT.modify(_.toUpperCase)(bytes), expected) === true
    val identityOk =
      Arrays.equals(namesT.modify(identity[String])(bytes), bytes) === true

    val badBytes: Array[Byte] = Array(2.toByte)
    val missOk = (namesT.modify(identity[String])(badBytes) eq badBytes) === true

    val emptyBasket = Basket("bo", Nil)
    val emptyBytes = toBinary(basketRecord(emptyBasket), basketSchema)
    val emptyReadOk = namesT.foldMap(List(_))(emptyBytes) === Nil
    val emptyWriteOk =
      Arrays.equals(namesT.modify(_.toUpperCase)(emptyBytes), emptyBytes) === true

    readOk.and(modifyOk).and(identityOk).and(missOk).and(emptyReadOk).and(emptyWriteOk)
  }

  /** The runtime union value for `payment = Cash(amount)` — a bare Cash record, as the writer sees
    * it when encoding the union-typed field.
    */
  private def cashRecordOf(amount: Long): IndexedRecord =
    summon[AvroCodec[Cash]].encode(Cash(amount)).asInstanceOf[IndexedRecord]

  // ---- Map-field skip (byte walker must skip a leading Map before a trailing sibling) --------

  // covers: reading/writing a field declared AFTER a `Map[String, Long]` field forces the byte
  //   walker to skip the whole map's block framing first (AvroBinaryCursor.skipMapItems /
  //   skipMapBlocks) — a populated (multi-entry) map exercises the per-item loop, an empty map
  //   exercises the immediate zero-count terminator
  "byte walker skips over a leading Map field to reach a trailing sibling: populated + empty" >> {
    val populated = TaggedCounts(Map("a" -> 1L, "b" -> 2L, "c" -> 3L), 42)
    val populatedBytes = toBinary(taggedCountsRecord(populated), taggedCountsSchema)
    val populatedReadOk =
      codecPrism[TaggedCounts].field(_.total).getOption(populatedBytes) === Some(42)
    val populatedWriteOk =
      codecPrism[TaggedCounts].getOption(
        codecPrism[TaggedCounts].field(_.total).replace(99)(populatedBytes)
      ) === Some(populated.copy(total = 99))

    val empty = TaggedCounts(Map.empty, 7)
    val emptyBytes = toBinary(taggedCountsRecord(empty), taggedCountsSchema)
    val emptyReadOk = codecPrism[TaggedCounts].field(_.total).getOption(emptyBytes) === Some(7)

    populatedReadOk.and(populatedWriteOk).and(emptyReadOk)
  }

  // ---- zigzag varint boundary lengths ------------------------------------

  // covers: zigZagLong / zigZagInt at their widest inputs — Long.MinValue needs the full 10-byte
  //   varint, Int.MinValue the full 5-byte varint (BinaryData.encodeLong/encodeInt's max width);
  //   these back the union-branch-index synthesis in splice/spliceAll/reframeArray
  "zigZagLong / zigZagInt: MinValue boundary lengths (10 / 5 bytes)" >> {
    (AvroBinaryCursor.zigZagLong(Long.MinValue).length === 10)
      .and(AvroBinaryCursor.zigZagInt(Int.MinValue).length === 5)
  }

end AvroBytesSpec
