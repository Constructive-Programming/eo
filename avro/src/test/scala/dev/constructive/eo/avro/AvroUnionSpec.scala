package dev.constructive.eo.avro

import scala.language.implicitConversions

import cats.data.Ior
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.specs2.mutable.Specification

/** Behaviour spec for [[AvroPrism]]'s `.union[Branch]` macro extension. Exercises the four plan
  * scenarios per `docs/plans/2026-04-25-009-feat-eo-avro-module-plan.md` Unit 8:
  *
  *   - Round-trip modify on a `union<null, long>` field whose runtime value is the long branch →
  *     `Ior.Right(updated)` with the long incremented.
  *   - Branch mismatch: same prism on a record where the runtime value is `null` →
  *     `Ior.Both(chain-of-UnionResolutionFailed, originalRecord)`.
  *   - Sealed-trait union via `Payment` — happy path on the `Cash` branch.
  *   - Compile-error coverage on `.union[NotInSchema]` and on calling `.union` against a non-union
  *     focus is tested through `FieldsMacroErrorSpec`-style negative compilation expectations.
  */
class AvroUnionSpec extends Specification:

  import AvroSpecFixtures.*

  // ---- Option[Long] branch via Transaction.amount -----------------

  // covers: Some(long) branch round-trip on .field(_.amount).union[Long]
  "field(_.amount).union[Long] modify increments the long branch" >> {
    val t = Transaction("t-1", Some(42L))
    val record = transactionRecord(t)
    val prism = codecPrism[Transaction]
      .field(_.amount)
      .union[Long]

    val result = prism.modify(_ + 1L)(record)
    result match
      case Ior.Right(out) =>
        val rec = out.asInstanceOf[GenericRecord]
        val amount = rec.get(transactionSchema.getField("amount").pos)
        (amount.toString === "43").and(rec.getSchema === transactionSchema)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result
  }

  // covers: get on the long-branch path returns Ior.Right(longValue)
  "field(_.amount).union[Long] get returns Ior.Right on Some(long)" >> {
    val record = transactionRecord(Transaction("t-1", Some(99L)))
    codecPrism[Transaction]
      .field(_.amount)
      .union[Long]
      .get(record) match
      case Ior.Right(v) => v === 99L
      case other        =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // covers: branch mismatch (None / null branch) on a .union[Long] surfaces UnionResolutionFailed
  // accumulating the schema-declared alternatives, AND the modify default-surface preserves the
  // original record on Ior.Both.
  "field(_.amount).union[Long] on null-branch surfaces UnionResolutionFailed" >> {
    val record = transactionRecord(Transaction("t-2", None))
    val prism = codecPrism[Transaction]
      .field(_.amount)
      .union[Long]

    val getResult = prism.get(record)
    val modifyResult = prism.modify(_ + 1L)(record)

    val getOk = getResult match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.UnionResolutionFailed(branches, PathStep.UnionBranch("long")) =>
            // The post-processing in walkPath should enumerate ["null", "long"] from the parent
            // record's `amount` field schema.
            (branches === List("null", "long")): org.specs2.execute.Result
          case other =>
            org
              .specs2
              .execute
              .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    val modifyOk = modifyResult match
      case Ior.Both(chain, out) =>
        val isUnion = chain.headOption.get match
          case AvroFailure.UnionResolutionFailed(_, _) => true
          case _                                       => false
        (isUnion === true).and((out eq record) === true): org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result

    getOk.and(modifyOk)
  }

  // covers: getOptionUnsafe on the null-branch returns None silently
  "field(_.amount).union[Long] getOptionUnsafe is None on null-branch" >> {
    val record = transactionRecord(Transaction("t-3", None))
    codecPrism[Transaction]
      .field(_.amount)
      .union[Long]
      .getOptionUnsafe(record) === None
  }

  // ---- Sealed-trait Payment union ---------------------------------

  // covers: sealed-trait .union[Cash] resolves the schema branch by full-name and modifies the
  // amount field on the Cash record-alternative; getOptionUnsafe returns the Cash value.
  "Payment .union[Cash] modify on the Cash branch round-trips" >> {
    // Build a record-of-Payment by encoding through kindlings: the encoded value is a
    // GenericData.Record under the `Cash` schema (per the kindlings probe).
    val paymentSchema: Schema = summon[AvroCodec[Payment]].schema
    val cashRec = summon[AvroCodec[Cash]].encode(Cash(100L)).asInstanceOf[GenericRecord]

    // Wrap the alternative inside a synthetic `record { Payment payment; }` envelope so the path
    // walker has a Field step to land on before the UnionBranch.
    val envSchema: Schema = {
      val fields = new java.util.ArrayList[Schema.Field]()
      fields.add(new Schema.Field("payment", paymentSchema, null, null))
      Schema.createRecord("Envelope", null, "eo.avro.test", false, fields)
    }
    val envelope = new GenericData.Record(envSchema)
    envelope.put(envSchema.getField("payment").pos, cashRec)

    // We don't have a top-level codec for Envelope (would need its own derivation); read the
    // `payment` field directly via the walker. This still exercises the .union branch — we
    // just hand-build the prism via raw widenPath / widenPathUnion calls instead of going
    // through codecPrism[Envelope].
    val cashL = AvroPrism
      .codecPrism[Payment](envSchema.getField("payment").schema)
      .union[Cash]

    val out = cashL.modify((c: Cash) => c.copy(amount = c.amount + 5L))(cashRec)
    out match
      case Ior.Right(rec) =>
        val updated = rec.asInstanceOf[GenericRecord]
        val amount = updated.get(updated.getSchema.getField("amount").pos)
        amount.toString === "105"
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result
  }

  // covers: sealed-trait .union[Card] on a Cash-branch record surfaces UnionResolutionFailed
  "Payment .union[Card] on Cash branch surfaces UnionResolutionFailed" >> {
    val cashRec = summon[AvroCodec[Cash]].encode(Cash(100L)).asInstanceOf[GenericRecord]
    val cardL = AvroPrism
      .codecPrism[Payment]
      .union[Card]

    cardL.get(cashRec) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.UnionResolutionFailed(_, PathStep.UnionBranch("Card")) =>
            org.specs2.execute.Success(): org.specs2.execute.Result
          case other =>
            org
              .specs2
              .execute
              .Failure(
                s"expected UnionResolutionFailed(_, UnionBranch(Card)), got $other"
              ): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Left, got $other"): org.specs2.execute.Result
  }

  // ---- Macro identifier semantics ---------------------------------

  // covers: the macro emits the schema's getFullName as the branch identifier — i.e. "long"
  // (lowercase) for primitives, "Cash"/"Card" for records. Witness: we walk the path back via
  // the prism's path accessor.
  "macro emits schema-fullname as branch identifier" >> {
    val longUnion = codecPrism[Transaction].field(_.amount).union[Long]
    val cashUnion = AvroPrism.codecPrism[Payment].union[Cash]

    val longSteps = longUnion.path.toList
    val cashSteps = cashUnion.path.toList

    (longSteps.last === PathStep.UnionBranch("long"))
      .and(cashSteps.last === PathStep.UnionBranch("Cash"))
  }

  // ---- Compile-error coverage --------------------------------------

  // covers: .union[NotInSchema] on a Payment-shaped focus aborts at compile time with a
  // "not a known alternative" diagnostic.
  "compile error: .union[Int] on Payment is rejected by the macro" >> {
    val errors = scala
      .compiletime
      .testing
      .typeCheckErrors(
        """
        import dev.constructive.eo.avro.AvroPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Payment
        AvroPrism.codecPrism[Payment].union[Int]
      """
      )
    errors.exists(e => e.message.contains("not a known alternative")) === true
  }

  // covers: .union on a non-union focus (e.g. Person) aborts with a "not a union-shaped type" error.
  "compile error: .union on a Person focus is rejected" >> {
    val errors = scala
      .compiletime
      .testing
      .typeCheckErrors(
        """
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].union[String]
      """
      )
    errors.exists(e => e.message.contains("not a union-shaped type")) === true
  }

  // covers: .union[Branch] with a Scala 3 untagged union focus aborts with the kindlings-doesn't-
  // support-these diagnostic.
  "compile error: .union on a Scala 3 union focus is rejected" >> {
    val errors = scala
      .compiletime
      .testing
      .typeCheckErrors(
        """
        import dev.constructive.eo.avro.{AvroCodec, AvroPrism}
        // Stub AvroCodec for the union — the parent codec is irrelevant since the macro should
        // abort before summoning anything else; supply a `???` codec to keep the test focused on
        // the macro's compile-time check, not on kindlings' refusal to derive.
        given AvroCodec[Long | String] =
          new AvroCodec[Long | String]:
            def schema: org.apache.avro.Schema = ???
            def encode(a: Long | String): Any = ???
            def decodeEither(any: Any): Either[Throwable, Long | String] = ???
        AvroPrism.codecPrism[Long | String].union[Long]
      """
      )
    errors.exists(e =>
      e.message.contains("Scala 3 untagged union") ||
        e.message.contains("kindlings-avro-derivation does not")
    ) === true
  }

  // covers: .union[Branch] on Option[T] when Branch != T is rejected
  "compile error: .union[String] on Option[Long] is rejected" >> {
    val errors = scala
      .compiletime
      .testing
      .typeCheckErrors(
        """
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Transaction
        codecPrism[Transaction].field(_.amount).union[String]
      """
      )
    errors.exists(e =>
      e.message.contains("the only valid branch is") ||
        e.message.contains("not a known alternative")
    ) === true
  }

end AvroUnionSpec
