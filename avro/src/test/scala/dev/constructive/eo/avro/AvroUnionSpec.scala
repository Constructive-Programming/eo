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
  //
  // 2026-04-29 consolidation: 4 Option[Long] branch tests → 1 composite.

  // covers: Some(long) branch round-trip on .field(_.amount).union[Long].modify (Ior.Right + schema preserved),
  //   .get on Some(long) returns Ior.Right(longValue),
  //   None / null-branch on .union[Long].get surfaces UnionResolutionFailed with schema-declared
  //     alternatives ["null", "long"],
  //   None / null-branch on .modify yields Ior.Both(UnionResolutionFailed, originalRecord),
  //   .getOptionUnsafe on the null-branch returns None silently
  "field(_.amount).union[Long]: Some round-trip + None branch surfaces UnionResolutionFailed" >> {
    val record1 = transactionRecord(Transaction("t-1", Some(42L)))
    val prism = codecPrism[Transaction].field(_.amount).union[Long]

    val modifyOk = prism.modify(_ + 1L)(record1) match
      case Ior.Right(out) =>
        val rec = out.asInstanceOf[GenericRecord]
        val amount = rec.get(transactionSchema.getField("amount").pos)
        (amount.toString === "43").and(rec.getSchema === transactionSchema)
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Right, got $other"): org.specs2.execute.Result

    val record2 = transactionRecord(Transaction("t-1", Some(99L)))
    val getSomeOk = prism.get(record2) match
      case Ior.Right(v) => v === 99L
      case other        =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val record3 = transactionRecord(Transaction("t-2", None))
    val getNoneOk = prism.get(record3) match
      case Ior.Left(chain) =>
        chain.headOption.get match
          case AvroFailure.UnionResolutionFailed(branches, PathStep.UnionBranch("long")) =>
            // The post-processing in walkPath should enumerate ["null", "long"] from the parent
            // record's `amount` field schema.
            branches === List("null", "long"): org.specs2.execute.Result
          case other =>
            org
              .specs2
              .execute
              .Failure(s"expected UnionResolutionFailed, got $other"): org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Ior.Left, got $other"): org.specs2.execute.Result

    val modifyNoneOk = prism.modify(_ + 1L)(record3) match
      case Ior.Both(chain, out) =>
        val isUnion = chain.headOption.get match
          case AvroFailure.UnionResolutionFailed(_, _) => true
          case _                                       => false
        (isUnion === true).and((out eq record3) === true): org.specs2.execute.Result
      case other =>
        org
          .specs2
          .execute
          .Failure(s"expected Ior.Both, got $other"): org.specs2.execute.Result

    val record4 = transactionRecord(Transaction("t-3", None))
    val unsafeNoneOk = prism.getOptionUnsafe(record4) === None

    modifyOk.and(getSomeOk).and(getNoneOk).and(modifyNoneOk).and(unsafeNoneOk)
  }

  // ---- Sealed-trait Payment union ---------------------------------

  // covers: sealed-trait .union[Cash] resolves the schema branch by full-name and modifies the
  // amount field on the Cash record-alternative; getOptionUnsafe returns the Cash value;
  // sealed-trait .union[Card] on a Cash-branch record surfaces UnionResolutionFailed with
  // PathStep.UnionBranch("Card");
  // macro emits the schema's getFullName as the branch identifier — "long" (lowercase) for
  // primitives, "Cash"/"Card" for records (witnessed by the path accessor).
  "Payment .union[Cash] / .union[Card]: happy modify + Cash→Card mismatch + macro emits fullname branch ids" >> {
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

    val cashHappy = cashL.modify((c: Cash) => c.copy(amount = c.amount + 5L))(cashRec) match
      case Ior.Right(rec) =>
        val updated = rec.asInstanceOf[GenericRecord]
        val amount = updated.get(updated.getSchema.getField("amount").pos)
        amount.toString === "105": org.specs2.execute.Result
      case other =>
        org.specs2.execute.Failure(s"expected Right, got $other"): org.specs2.execute.Result

    val cardL = AvroPrism.codecPrism[Payment].union[Card]
    val cashOnCardOk = cardL.get(cashRec) match
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

    // Macro identifier: "long" lowercase for primitives, "Cash"/"Card" record-fullname.
    val longUnion = codecPrism[Transaction].field(_.amount).union[Long]
    val cashUnion = AvroPrism.codecPrism[Payment].union[Cash]
    val branchIdsOk = (longUnion.path.toList.last === PathStep.UnionBranch("long"))
      .and(cashUnion.path.toList.last === PathStep.UnionBranch("Cash"))

    cashHappy.and(cashOnCardOk).and(branchIdsOk)
  }

  // ---- Compile-error coverage --------------------------------------
  //
  // 2026-04-29 consolidation: 4 compile-error specs → 1 composite with reverse-index.

  // covers: .union[NotInSchema] on a Payment-shaped focus -> "not a known alternative",
  //   .union on a non-union focus (Person) -> "not a union-shaped type",
  //   .union on a Scala 3 untagged union focus -> kindlings "Scala 3 untagged union" or
  //     "kindlings-avro-derivation does not" diagnostic,
  //   .union[String] on Option[Long] -> "the only valid branch is" or "not a known alternative"
  "compile errors: .union macro rejects non-union, wrong-branch, and Scala-3-union focuses" >> {
    val notInSchema = scala
      .compiletime
      .testing
      .typeCheckErrors("""
        import dev.constructive.eo.avro.AvroPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Payment
        AvroPrism.codecPrism[Payment].union[Int]
      """)
    val notInSchemaOk = notInSchema.exists(_.message.contains("not a known alternative"))

    val nonUnion = scala
      .compiletime
      .testing
      .typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Person
        codecPrism[Person].union[String]
      """)
    val nonUnionOk = nonUnion.exists(_.message.contains("not a union-shaped type"))

    // Stub AvroCodec for the union — the parent codec is irrelevant since the macro should
    // abort before summoning anything else; supply a `???` codec to keep the test focused on
    // the macro's compile-time check, not on kindlings' refusal to derive.
    val scala3Union = scala
      .compiletime
      .testing
      .typeCheckErrors("""
        import dev.constructive.eo.avro.{AvroCodec, AvroPrism}
        given AvroCodec[Long | String] =
          new AvroCodec[Long | String]:
            def schema: org.apache.avro.Schema = ???
            def encode(a: Long | String): Any = ???
            def decodeEither(any: Any): Either[Throwable, Long | String] = ???
        AvroPrism.codecPrism[Long | String].union[Long]
      """)
    val scala3UnionOk = scala3Union.exists(e =>
      e.message.contains("Scala 3 untagged union") ||
        e.message.contains("kindlings-avro-derivation does not")
    )

    val wrongOptionBranch = scala
      .compiletime
      .testing
      .typeCheckErrors("""
        import dev.constructive.eo.avro.codecPrism
        import dev.constructive.eo.avro.AvroSpecFixtures.Transaction
        codecPrism[Transaction].field(_.amount).union[String]
      """)
    val wrongOptionBranchOk = wrongOptionBranch.exists(e =>
      e.message.contains("the only valid branch is") ||
        e.message.contains("not a known alternative")
    )

    (notInSchemaOk === true)
      .and(nonUnionOk === true)
      .and(scala3UnionOk === true)
      .and(wrongOptionBranchOk === true)
  }

end AvroUnionSpec
