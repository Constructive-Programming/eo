package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.Oracle.Verdict
import dev.constructive.eo.demo.uplc.validators.*
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import scalus.cardano.onchain.plutus.prelude.List as PList
import scalus.cardano.onchain.plutus.v3.*
import scalus.uplc.builtin.Data.toData
import scalus.uplc.builtin.{Builtins, ByteString, Data}
import scalus.uplc.{Constant, PlutusV3, Term}

/** Smart-contract validation, differential: validators written in Scala and compiled to UPLC by the
  * Scalus plugin run against V3 script contexts through the CEK machine and through the generated
  * class, for transactions they must accept and transactions they must reject.
  *
  * Each script is exercised two ways:
  *   - compiled ONCE from its on-chain CBOR form (`UplcJvm.onChain`, the `cborScript andThen iso`
  *     composition) and applied to the context at run time — how a node would use a JIT;
  *   - applied to the context first and translated as a whole, the context then being one large
  *     embedded `Data` constant.
  */
class ValidatorSpec extends Specification:

  import Parties.{alice, bob, carol}

  private def context(
      datum: Data,
      redeemer: Data,
      signatories: Seq[PubKeyHash] = Nil,
      validRange: Interval = Interval.always,
  ): Data = Contexts.spending(datum, redeemer, signatories, validRange)

  /** CIP-117: a V3 script validates iff it evaluates to unit. */
  private def validates(term: Term): Boolean = term match
    case Term.Const(Constant.Unit, _) => true
    case _                            => false

  private def check(script: PlutusV3[?], ctx: Data, accept: Boolean): Result =
    // 1. compile once (from CBOR, through the composed optic), apply at run time
    val classFile = UplcJvm.onChain.modify(Lower.lower)(script.program.cborEncoded)
    val jit = Exec.validate(classFile, ctx)
    // 2. the applied program, translated whole, against the CEK machine
    val applied = (script.program $ ctx).flatEncoded
    val verdict = Oracle.compare(applied)
    val oracleAccepts = verdict match
      case Verdict.BothSucceed(t) => validates(t)
      case _                      => false
    (verdict must not(beAnInstanceOf[Verdict.Disagree]))
      .and(oracleAccepts must beEqualTo(accept))
      .and(jit.isRight must beEqualTo(accept))

  "Scalus-compiled validators: the JVM class and the CEK machine reach the same verdict" >> {
    import CompiledValidators.*

    "HelloOwner" >> {
      val datum = alice.toData
      "accepts: signed by the owner, says hello" >> {
        check(helloOwner, context(datum, "Hello, World!".toData, List(alice)), accept = true)
      }
      "rejects: not signed" >> {
        check(helloOwner, context(datum, "Hello, World!".toData, List(bob)), accept = false)
      }
      "rejects: wrong greeting" >> {
        check(helloOwner, context(datum, "Hi".toData, List(alice)), accept = false)
      }
    }

    "Preimage" >> {
      val secret = ByteString.fromString("open sesame")
      val datum = (Builtins.sha2_256(secret), bob.hash).toData
      "accepts: right preimage, signed" >> {
        check(preimage, context(datum, secret.toData, List(bob)), accept = true)
      }
      "rejects: wrong preimage" >> {
        check(
          preimage,
          context(datum, ByteString.fromString("open barley").toData, List(bob)),
          accept = false
        )
      }
      "rejects: right preimage, not signed" >> {
        check(preimage, context(datum, secret.toData, List(alice)), accept = false)
      }
    }

    "TimeLock" >> {
      val datum = Lock(carol, deadline = 1_700_000_000_000L).toData
      "accepts: after the deadline, signed" >> {
        check(
          timeLock,
          context(datum, Data.unit, List(carol), Interval.after(1_700_000_000_001L)),
          accept = true
        )
      }
      "rejects: before the deadline" >> {
        check(
          timeLock,
          context(datum, Data.unit, List(carol), Interval.after(1_600_000_000_000L)),
          accept = false
        )
      }
      "rejects: no lower bound" >> {
        check(timeLock, context(datum, Data.unit, List(carol)), accept = false)
      }
    }

    "Multisig (2 of 3)" >> {
      val datum = Quorum(PList(alice, bob, carol), 2).toData
      "accepts: two signers" >> check(
        multisig,
        context(datum, Data.unit, List(alice, carol)),
        accept = true
      )
      "accepts: three signers" >> {
        check(multisig, context(datum, Data.unit, List(carol, bob, alice)), accept = true)
      }
      "rejects: one signer" >> check(multisig, context(datum, Data.unit, List(bob)), accept = false)
    }

    "Fibonacci (integer recursion)" >> {
      import Scenarios.fib
      "accepts: fib(30)" >> {
        check(fibonacci, context(fib(30).toData, BigInt(30).toData), accept = true)
      }
      "accepts: fib(400), a 84-digit integer, 400 frames deep" >> {
        check(fibonacci, context(fib(400).toData, BigInt(400).toData), accept = true)
      }
      "rejects: a wrong answer" >> {
        check(fibonacci, context((fib(30) + 1).toData, BigInt(30).toData), accept = false)
      }
    }

    "Htlc (enum pattern matching)" >> {
      val secret = ByteString.fromString("htlc secret")
      val timeout = 1_800_000_000_000L
      val datum =
        Contract(committer = alice, receiver = bob, Builtins.sha2_256(secret), timeout).toData
      val claim = HtlcAction.Claim(secret).toData
      val refund = HtlcAction.Refund.toData
      "accepts: receiver claims before the timeout" >> {
        check(htlc, context(datum, claim, List(bob), Interval.before(timeout - 1)), accept = true)
      }
      "rejects: claim after the timeout" >> {
        check(htlc, context(datum, claim, List(bob), Interval.before(timeout + 1)), accept = false)
      }
      "rejects: claim with a wrong preimage" >> {
        val wrong = HtlcAction.Claim(ByteString.fromString("guess")).toData
        check(htlc, context(datum, wrong, List(bob), Interval.before(timeout - 1)), accept = false)
      }
      "accepts: committer refunds after the timeout" >> {
        check(htlc, context(datum, refund, List(alice), Interval.after(timeout + 1)), accept = true)
      }
      "rejects: refund before the timeout" >> {
        check(
          htlc,
          context(datum, refund, List(alice), Interval.after(timeout - 1)),
          accept = false
        )
      }
    }
  }
