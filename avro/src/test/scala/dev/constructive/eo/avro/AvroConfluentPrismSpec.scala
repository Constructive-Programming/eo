package dev.constructive.eo.avro

import cats.data.Ior
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroPrism.confluent]] (issue #38) — the Confluent-framed read face.
  *
  * Covers the byte-exact classification path: strip the 5-byte header, resolve the writer schema by
  * id through an injected [[ConfluentWire.SchemaById]] hook, and — when the writer fingerprint
  * equals the reader's — decode the body and apply the optic. Drift, unresolvable ids, and unframed
  * input all surface on the Ior channel.
  */
class AvroConfluentPrismSpec extends Specification:

  import AvroSpecFixtures.*

  // Registry: id 1 → the Person reader schema (byte-exact), id 2 → an unrelated writer schema
  // (Transaction) whose fingerprint differs, id 99 → unknown (hook throws).
  private val registry: ConfluentWire.SchemaById =
    case 1  => personSchema
    case 2  => transactionSchema
    case id => throw new NoSuchElementException(s"no schema for id $id")

  private def framed(schemaId: Int, p: Person): Array[Byte] =
    ConfluentWire.attach(schemaId, toBinary(personRecord(p), personSchema))

  "byte-exact frame (writer fp == reader fp): whole-record get + drilled field get" >> {
    val bytes = framed(1, Person("Ada", 36))

    val wholeOk = codecPrism[Person].confluent(registry).get(bytes) match
      case Ior.Right(p) => p === Person("Ada", 36)
      case other        =>
        org.specs2.execute.Failure(s"expected Right(Person), got $other"): org.specs2.execute.Result

    val fieldOk = codecPrism[Person].field(_.name).confluent(registry).get(bytes) match
      case Ior.Right(n) => n === "Ada"
      case other        =>
        org.specs2.execute.Failure(s"expected Right(name), got $other"): org.specs2.execute.Result

    val unsafeOk = codecPrism[Person].confluent(registry).getOptionUnsafe(bytes) === Some(
      Person("Ada", 36)
    )

    wholeOk.and(fieldOk).and(unsafeOk)
  }

  "schema drift (writer fp != reader fp): SchemaMismatch, not a misread" >> {
    // Frame a Person body under id 2 — the registry maps id 2 to the Transaction schema, whose
    // fingerprint differs from Person's, so the gate must refuse rather than walk the bytes.
    val bytes = ConfluentWire.attach(2, toBinary(personRecord(Person("Ada", 36)), personSchema))
    val prism = codecPrism[Person].confluent(registry)

    val mismatchOk = prism.get(bytes) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.SchemaMismatch(2, w, r) =>
            (w !== r): org.specs2.execute.Result
          case other =>
            org
              .specs2
              .execute
              .Failure(s"expected SchemaMismatch(2, _, _), got $other"): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    mismatchOk.and(prism.getOptionUnsafe(bytes) === None)
  }

  "unresolvable schema id: hook throw becomes SchemaResolutionFailed" >> {
    val bytes = framed(99, Person("Ada", 36))
    codecPrism[Person].confluent(registry).get(bytes) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.SchemaResolutionFailed(99, _) => true === true
          case other                                     =>
            org
              .specs2
              .execute
              .Failure(
                s"expected SchemaResolutionFailed(99, _), got $other"
              ): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
  }

  "unframed input: NotConfluentFramed before the hook is ever called" >> {
    // Raw Avro body with no 5-byte header. A throwing registry proves the hook isn't consulted.
    val body = toBinary(personRecord(Person("Ada", 36)), personSchema)
    val boom: ConfluentWire.SchemaById = _ => throw new AssertionError("hook must not be called")
    codecPrism[Person].confluent(boom).get(body) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.NotConfluentFramed(_) => true === true
          case other                             =>
            org
              .specs2
              .execute
              .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result
  }

  "reader schema roundtrips its own fingerprint (sanity: identical schema → byte-exact gate opens)" >> {
    // A distinct-but-structurally-identical reader Schema instance still fingerprints equal to the
    // writer, so the gate keys on canonical form, not object identity.
    val readerCopy: Schema = new Schema.Parser().parse(personSchema.toString)
    val bytes = framed(1, Person("Ada", 36))
    AvroPrism.codecPrism[Person](readerCopy).confluent(registry).get(bytes) match
      case Ior.Right(p) => p === Person("Ada", 36)
      case other        =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

end AvroConfluentPrismSpec
