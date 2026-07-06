package dev.constructive.eo.avro

import dev.constructive.eo.optics.Optic.*
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** Behaviour spec for the Confluent-framed seam (issue #41 redesign of #38): the decode-agnostic
  * [[ConfluentWire.resolve]] primitive and the composable [[ConfluentWire.confluent]] byte Prism.
  *
  * The strip + writer-resolve + fingerprint-gate no longer welds a kindlings decode: `resolve`
  * hands back the byte-exact body (or the specific `AvroFailure`), and `confluent` is a
  * `Prism[Array[Byte], Array[Byte]]` (framed ↔ body) you drop BEFORE any byte optic.
  */
class ConfluentPrismSpec extends Specification:

  import AvroSpecFixtures.*

  // Registry: id 1 → the Person reader schema (byte-exact), id 2 → an unrelated writer schema
  // (Transaction) whose fingerprint differs, id 99 → unknown (hook throws).
  private val registry: ConfluentWire.SchemaById =
    case 1  => personSchema
    case 2  => transactionSchema
    case id => throw new NoSuchElementException(s"no schema for id $id")

  private def framed(schemaId: Int, p: Person): Array[Byte] =
    ConfluentWire.attach(schemaId, toBinary(personRecord(p), personSchema))

  // ---- ConfluentWire.resolve (decode-agnostic primitive) -----------

  "resolve: byte-exact → Right(body); drift/unresolvable/unframed → the specific AvroFailure" >> {
    val body = toBinary(personRecord(Person("Ada", 36)), personSchema)

    val exact = ConfluentWire.resolve(ConfluentWire.attach(1, body), registry, personSchema) match
      case Right(b) => b.sameElements(body) === true
      case other    =>
        org.specs2.execute.Failure(s"expected Right(body), got $other"): org.specs2.execute.Result

    // id 2 resolves to the Transaction schema — different fingerprint → SchemaMismatch, not a misread.
    val drift = ConfluentWire.resolve(ConfluentWire.attach(2, body), registry, personSchema) match
      case Left(AvroFailure.SchemaMismatch(2, w, r)) => (w !== r): org.specs2.execute.Result
      case other                                     =>
        org
          .specs2
          .execute
          .Failure(s"expected SchemaMismatch(2), got $other"): org.specs2.execute.Result

    val unresolvable =
      ConfluentWire.resolve(ConfluentWire.attach(99, body), registry, personSchema) match
        case Left(AvroFailure.SchemaResolutionFailed(99, _)) => true === true
        case other                                           =>
          org
            .specs2
            .execute
            .Failure(s"expected SchemaResolutionFailed(99), got $other"): org.specs2.execute.Result

    // Raw body, no header. A throwing registry proves the hook isn't reached before the frame check.
    val boom: ConfluentWire.SchemaById = _ => throw new AssertionError("hook must not be called")
    val unframed = ConfluentWire.resolve(body, boom, personSchema) match
      case Left(AvroFailure.NotConfluentFramed(_)) => true === true
      case other                                   =>
        org
          .specs2
          .execute
          .Failure(s"expected NotConfluentFramed, got $other"): org.specs2.execute.Result

    exact.and(drift).and(unresolvable).and(unframed)
  }

  // ---- ConfluentWire.confluent (composable byte Prism) -------------

  "confluent Prism: standalone getOption yields the resolved body; drift/unframed → None" >> {
    val body = toBinary(personRecord(Person("Ada", 36)), personSchema)
    val cf = ConfluentWire.confluent(registry, personSchema, frameId = 1)

    val exactOk =
      cf.getOption(ConfluentWire.attach(1, body)).map(_.sameElements(body)) === Some(true)
    val driftOk = cf.getOption(ConfluentWire.attach(2, body)) === None
    val unframedOk = cf.getOption(body) === None

    exactOk.and(driftOk).and(unframedOk)
  }

  "confluent Prism: reverseGet re-frames under frameId (strip ∘ reverseGet round-trips)" >> {
    val body = toBinary(personRecord(Person("Ada", 36)), personSchema)
    val cf = ConfluentWire.confluent(registry, personSchema, frameId = 7)
    ConfluentWire.strip(cf.reverseGet(body)) match
      case Right(ConfluentWire.Framed(7, b)) => b.sameElements(body) === true
      case other                             =>
        org
          .specs2
          .execute
          .Failure(s"expected Framed(7, body), got $other"): org.specs2.execute.Result
  }

  "confluent Prism composes BEFORE a byte optic: field read, whole-record read, and modify round-trip" >> {
    val cf = ConfluentWire.confluent(registry, personSchema, frameId = 1)
    val frame = framed(1, Person("Ada", 36))

    // framed → focused field
    val fieldRead = cf.andThen(codecPrism[Person].field(_.name)).getOption(frame) === Some("Ada")

    // framed → whole record
    val wholeRead = cf.andThen(codecPrism[Person]).getOption(frame) === Some(Person("Ada", 36))

    // framed → modify a field → back to a framed payload (exercises the compose write path:
    // strip → walk → re-encode body → reverseGet re-frames under frameId).
    val modified = cf.andThen(codecPrism[Person].field(_.name)).modify(_.toUpperCase)(frame)
    val modifyRoundTrip = cf.andThen(codecPrism[Person]).getOption(modified) === Some(
      Person("ADA", 36)
    )

    fieldRead.and(wholeRead).and(modifyRoundTrip)
  }

end ConfluentPrismSpec
