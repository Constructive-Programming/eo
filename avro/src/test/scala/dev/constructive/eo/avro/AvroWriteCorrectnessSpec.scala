package dev.constructive.eo.avro

import scala.language.implicitConversions

import dev.constructive.eo.optics.Optic.*
import java.io.ByteArrayOutputStream
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord, IndexedRecord}
import org.apache.avro.io.EncoderFactory
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Write-side correctness of the byte faces — born from the 2026-07-04 category-theory / wire
  * format expert review. Every example here started RED against the reviewed code:
  *
  *   - `.fields` byte writes encoded the NamedTuple under the PARENT schema through avro's
  *     positional `GenericDatumWriter`: partial covers silently no-opped (writer indexed past the
  *     NT arity → swallowed exception → pass-through) and reordered same-typed full covers silently
  *     SWAPPED field values. Fixed by the by-name overlay (decode parent slice → overlay NT fields
  *     by name, mirroring the record face's `writeFields` → re-encode parent).
  *   - `.record.replace` (the generic Either-carrier extension) routed through
  *     `from(Right(a)) = reverseGet(a)`, reconstructing the focus STANDALONE — a drilled prism
  *     returned an empty null-filled record. Fixed by the sibling-preserving member `replace`.
  *   - negative-count (byte-sized) array block framing made traversal writes silently pass through
  *     while reads succeeded. Fixed by re-framing: the write rebuilds the array region canonically
  *     (single positive-count block) with the focus splices applied.
  *   - an adversarial `Long.MinValue` block count negated to itself and produced a confident empty
  *     element walk on corrupt bytes. Fixed by rejecting it as `BinaryParseFailed`.
  *
  * Plus the Optional-law property block for the byte prism (hand-rolled: the discipline
  * `OptionalTests` compare `S` with `==`, which is reference equality on `Array[Byte]`), and the
  * schema-discipline pins: the byte walk trusts the prism's reader schema absolutely, so payloads
  * written under a DIFFERENT schema Miss (or, for same-typed reorders, read the wrong field) — the
  * documented exact-writer-schema requirement.
  */
class AvroWriteCorrectnessSpec extends Specification with ScalaCheck:

  import AvroSpecFixtures.*
  import hearth.kindlings.avroderivation.{AvroDecoder, AvroEncoder, AvroSchemaFor}

  type NameQty = NamedTuple.NamedTuple[("name", "qty"), (String, Int)]
  given AvroEncoder[NameQty] = AvroEncoder.derived
  given AvroDecoder[NameQty] = AvroDecoder.derived
  given AvroSchemaFor[NameQty] = AvroSchemaFor.derived

  type LastFirst = NamedTuple.NamedTuple[("last", "first"), (String, String)]
  given AvroEncoder[LastFirst] = AvroEncoder.derived
  given AvroDecoder[LastFirst] = AvroDecoder.derived
  given AvroSchemaFor[LastFirst] = AvroSchemaFor.derived

  type NamePrice = NamedTuple.NamedTuple[("name", "price"), (String, Double)]
  given AvroEncoder[NamePrice] = AvroEncoder.derived
  given AvroDecoder[NamePrice] = AvroDecoder.derived
  given AvroSchemaFor[NamePrice] = AvroSchemaFor.derived

  type UserDomain = NamedTuple.NamedTuple[("user", "domain"), (String, String)]
  given AvroEncoder[UserDomain] = AvroEncoder.derived
  given AvroDecoder[UserDomain] = AvroDecoder.derived
  given AvroSchemaFor[UserDomain] = AvroSchemaFor.derived

  private def encodeVia[A](a: A)(using codec: AvroCodec[A]): Array[Byte] =
    toBinary(codec.encode(a).asInstanceOf[GenericRecord], codec.schema)

  // ---- F1: .fields byte-face writes ----------------------------------

  // covers: partial-cover .fields byte write actually writes (was: silent no-op via
  //   parent-schema positional encode → AIOOBE → pass-through)
  "byte-face .fields partial cover: modify writes the selected fields, siblings survive" >> {
    val order = Order("tea", 2.5, 3)
    val bytes = encodeVia(order)
    val L = codecPrism[Order].fields(_.name, _.qty)
    val out = L.modify(nt => (name = nt.name.toUpperCase, qty = nt.qty + 1))(bytes)
    codecPrism[Order].getOption(out) === Some(Order("TEA", 2.5, 4))
  }

  // covers: reordered full-cover .fields byte write goes by NAME, not writer-schema position
  //   (was: same-typed fields silently SWAPPED)
  "byte-face .fields reordered full cover: values land on their NAMED fields" >> {
    val fn = FullName("John", "Doe")
    val bytes = encodeVia(fn)
    val L = codecPrism[FullName].fields(_.last, _.first)
    val out = L.replace((last = "Smith", first = "Jane"))(bytes)
    codecPrism[FullName].getOption(out) === Some(FullName("Jane", "Smith"))
  }

  // covers: per-element .each.fields byte write (was: every element silently kept)
  "byte-face .each.fields: per-element multi-field write applies to every element" >> {
    val basket = Basket("ann", List(Order("tea", 2.5, 1), Order("mate", 4.0, 2)))
    val bytes = toBinary(basketRecord(basket), basketSchema)
    val T = codecPrism[Basket].items.each.fields(_.name, _.price)
    val out = T.modify(nt => (name = nt.name.toUpperCase, price = nt.price * 2))(bytes)
    codecPrism[Basket].getOption(out) === Some(
      Basket("ann", List(Order("TEA", 5.0, 1), Order("MATE", 8.0, 2)))
    )
  }

  // ---- F2: .record.replace ---------------------------------------------

  // covers: the record face's replace preserves siblings (was: generic extension routed through
  //   from(Right(a)) = reverseGet(a) → empty null-filled record)
  ".record.replace on a drilled prism replaces the focus and preserves siblings" >> {
    val rec = personRecord(Person("Alice", 30))
    val out = codecPrism[Person].field(_.name).record.replace("Bob")(rec)
    (out.get(personSchema.getField("name").pos).toString === "Bob")
      .and(out.get(personSchema.getField("age").pos).asInstanceOf[Int] === 30)
  }

  // ---- F6: negative-count (blocked) array framing ----------------------

  /** Encode via avro's blockingBinaryEncoder — the spec-legal framing that prefixes array blocks
    * with byte sizes (negative counts), as produced by avro-c / fastavro-with-block-size.
    */
  private def toBlockedBinary(record: IndexedRecord, schema: org.apache.avro.Schema): Array[Byte] =
    val out = new ByteArrayOutputStream()
    val encoder = new EncoderFactory().configureBlockSize(64).blockingBinaryEncoder(out, null)
    val writer = new GenericDatumWriter[IndexedRecord](schema)
    writer.write(record, encoder)
    encoder.flush()
    out.toByteArray

  // covers: traversal writes on byte-sized block framing re-frame the array region canonically
  //   and apply (was: reads succeeded, writes silently passed through)
  "negative-count array framing: traversal writes re-frame and apply" >> {
    val basket = Basket("ann", List(Order("tea", 2.5, 1), Order("mate", 4.0, 2)))
    val blocked = toBlockedBinary(basketRecord(basket), basketSchema)
    val canonical = toBinary(basketRecord(basket), basketSchema)
    // Framing witness: the fixture really is non-canonical, or this test proves nothing.
    val framingOk = java.util.Arrays.equals(blocked, canonical) === false

    val namesT = codecPrism[Basket].items.each.name
    val readOk = namesT.record.getAllUnsafe(blocked) === Vector("tea", "mate")
    val out = namesT.modify(_.toUpperCase)(blocked)
    val writeOk = codecPrism[Basket].getOption(out) === Some(
      Basket("ann", List(Order("TEA", 2.5, 1), Order("MATE", 4.0, 2)))
    )
    framingOk.and(readOk).and(writeOk)
  }

  // ---- Post-re-review combination axes (reframe × overlay × union) -----

  // covers: .each.fields on a NON-canonical array — the reframe path AND the by-name field
  //   overlay together (each individually pinned above; this exercises the combination)
  "byte-face .each.fields on blocked framing: reframe + overlay both apply" >> {
    val basket = Basket("ann", List(Order("tea", 2.5, 1), Order("mate", 4.0, 2)))
    val blocked = toBlockedBinary(basketRecord(basket), basketSchema)
    val T = codecPrism[Basket].items.each.fields(_.name, _.price)
    val out = T.modify(nt => (name = nt.name.toUpperCase, price = nt.price + 1.0))(blocked)
    codecPrism[Basket].getOption(out) === Some(
      Basket("ann", List(Order("TEA", 3.5, 1), Order("MATE", 5.0, 2)))
    )
  }

  // covers: Fields focus UNDER a .union step — the union index re-synthesis (from branchOrdinal)
  //   and the by-name parent overlay interacting on both faces
  "byte-face .union[Branch].fields: index re-synthesis + overlay, siblings survive" >> {
    val dir = Directory("d-1", Email("alice", "example.com"))
    val bytes = toBinary(directoryRecord(dir), directorySchema)
    val L = codecPrism[Directory].field(_.contact).union[Email].fields(_.user, _.domain)
    val out = L.modify(nt => (user = nt.user.toUpperCase, domain = nt.domain))(bytes)
    val byteOk = codecPrism[Directory].getOption(out) ===
      Some(Directory("d-1", Email("ALICE", "example.com")))
    // The record face over the same drilled path agrees.
    val recOk = codecPrism[Directory]
      .field(_.contact)
      .union[Email]
      .fields(_.user, _.domain)
      .record
      .getOptionUnsafe(out)
      .contains((user = "ALICE", domain = "example.com"))
    byteOk.and(recOk)
  }

  // covers: reframe of an array whose ELEMENTS are unions (List[Payment]) on blocked framing —
  //   each element's opaque index+value bytes are decoded/re-encoded whole via the branch codec
  //   and rebuilt into a canonical block. (Per-element `.union[B]` narrowing is not an
  //   AvroTraversal extension, so the element-level branch-index re-synthesis in reframeArray is
  //   unreachable via the public API and stays as a defensive mirror of spliceAll.)
  "byte-face .each on a union-element array under blocked framing: reframe applies per element" >> {
    val ledger = Ledger("ann", List(Cash(100L), Card("4111"), Cash(250L)))
    val blocked = toBlockedBinary(ledgerRecord(ledger), ledgerSchema)
    val T = codecPrism[Ledger].field(_.entries).each
    val out = T.modify {
      case Cash(a) => Cash(a + 1L)
      case Card(n) => Card(n.reverse)
    }(blocked)
    codecPrism[Ledger].getOption(out) === Some(
      Ledger("ann", List(Cash(101L), Card("1114"), Cash(251L)))
    )
  }

  // covers: Long.MinValue block count (negates to itself) is a structured parse failure, not a
  //   confident empty element walk
  "array block count Long.MinValue surfaces BinaryParseFailed" >> {
    val out = new ByteArrayOutputStream()
    val enc = EncoderFactory.get().binaryEncoder(out, null)
    enc.writeString("ann") // Basket.owner
    enc.writeLong(Long.MinValue) // items block count — adversarial
    enc.writeLong(0L) // "byte size" long the negative arm consumes
    enc.writeLong(0L) // terminator-shaped garbage
    enc.flush()
    val bytes = out.toByteArray

    AvroBinaryCursor.locateElements(
      bytes,
      basketSchema,
      Array(PathStep.Field("items")),
      Array(PathStep.Field("name")),
    ) match
      case Left(f)  => f.isInstanceOf[AvroFailure.BinaryParseFailed] === true
      case Right(r) =>
        org.specs2.execute.Failure(s"expected BinaryParseFailed, got $r"): org.specs2.execute.Result
  }

  // ---- Optional laws for the byte prism (canonical fixtures) -----------

  private given Arbitrary[Person] = Arbitrary(
    for
      n <- Gen.alphaStr.map(_.take(10)).suchThat(_.nonEmpty)
      a <- Gen.choose(0, 120)
    yield Person(n, a)
  )

  // covers: get-put (modify identity), put-get, put-put on the byte-carried Optional, on
  //   canonical (kindlings-encoded) payloads — the quotient the class docs state. Hand-rolled:
  //   discipline's OptionalTests compare S with ==, which is reference equality on Array[Byte].
  "byte prism Optional laws on canonical payloads: get-put / put-get / put-put" >> forAll {
    (p: Person, x: Int, y: Int) =>
      val ageL = codecPrism[Person].field(_.age)
      val s = toBinary(personRecord(p), personSchema)

      val getPut = java.util.Arrays.equals(ageL.modify(identity[Int])(s), s)
      val putGet = ageL.getOption(ageL.replace(x)(s)).contains(x)
      val putPut = java
        .util
        .Arrays
        .equals(
          ageL.replace(y)(ageL.replace(x)(s)),
          ageL.replace(y)(s),
        )
      val composeModify = java
        .util
        .Arrays
        .equals(
          ageL.modify((_: Int) + y)(ageL.modify((_: Int) + x)(s)),
          ageL.modify((v: Int) => v + x + y)(s),
        )
      getPut && putGet && putPut && composeModify
  }

  // ---- F4: schema discipline (documented limitation, pinned) -----------

  // covers: a payload written under a STRUCTURALLY different schema Misses — reads are None and
  //   writes pass through by reference. The byte walk performs no writer/reader resolution; the
  //   exact-writer-schema requirement is documented on AvroPrism / AvroBinaryCursor.
  "schema drift, incompatible shape: byte walk Misses, writes pass through (documented)" >> {
    // Writer schema: (extra: long, name: string, age: int) — the walk under Person's schema
    // reads `extra`'s varint bytes as `name`'s string length and derails.
    val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
    fields.add(
      new org.apache.avro.Schema.Field(
        "extra",
        org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG),
        null,
        null,
      )
    )
    fields.add(
      new org.apache.avro.Schema.Field(
        "name",
        org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
        null,
        null,
      )
    )
    val v1Schema =
      org.apache.avro.Schema.createRecord("PersonV1", null, "eo.avro.test", false, fields)
    val v1Bytes = toBinary(
      buildRecord(v1Schema)("extra" -> java.lang.Long.valueOf(Long.MaxValue), "name" -> "Alice"),
      v1Schema,
    )

    val nameL = codecPrism[Person].field(_.name)
    (nameL.getOption(v1Bytes) === None)
      .and((nameL.modify(_.toUpperCase)(v1Bytes) eq v1Bytes) === true)
  }

  // covers: same-typed field REORDER between writer and reader is undetectable from the bytes —
  //   the walk reads the wrong field with full confidence. This pin exists to document WHY the
  //   exact-writer-schema requirement is absolute, not merely "mostly Misses".
  "schema drift, same-typed reorder: byte walk reads the WRONG field (why the rule is absolute)" >> {
    // Writer laid the record out as (last, first); the prism's reader schema says (first, last).
    val fields = new java.util.ArrayList[org.apache.avro.Schema.Field]()
    fields.add(
      new org.apache.avro.Schema.Field(
        "last",
        org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
        null,
        null,
      )
    )
    fields.add(
      new org.apache.avro.Schema.Field(
        "first",
        org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
        null,
        null,
      )
    )
    val reordered =
      org.apache.avro.Schema.createRecord("FullName", null, "eo.avro.test", false, fields)
    val bytes = toBinary(buildRecord(reordered)("last" -> "Doe", "first" -> "John"), reordered)

    codecPrism[FullName].field(_.first).getOption(bytes) === Some("Doe")
  }

end AvroWriteCorrectnessSpec
