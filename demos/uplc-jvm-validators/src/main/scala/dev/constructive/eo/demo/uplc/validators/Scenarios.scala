package dev.constructive.eo.demo.uplc.validators

import scalus.cardano.onchain.plutus.prelude.{List as PList, Option as POption}
import scalus.cardano.onchain.plutus.v1.Value
import scalus.cardano.onchain.plutus.v2.TxOut
import scalus.cardano.onchain.plutus.v3.*
import scalus.uplc.PlutusV3
import scalus.uplc.builtin.Data.toData
import scalus.uplc.builtin.{Builtins, ByteString, Data}

// Off-chain fixtures shared by the demo's specs, its walkthrough and its benchmarks: the parties,
// a V3 spending-context builder, and one transaction each validator accepts.

/** Key and script hashes are 28 bytes, transaction ids 32. */
object Parties:
  def hash(size: Int, byte: Int): ByteString = ByteString.fill(size, byte.toByte)
  val alice: PubKeyHash = PubKeyHash(hash(28, 0xa1))
  val bob: PubKeyHash = PubKeyHash(hash(28, 0xb0))
  val carol: PubKeyHash = PubKeyHash(hash(28, 0xca))

object Contexts:

  val ownRef: TxOutRef = TxOutRef(TxId(Parties.hash(32, 0x01)), 0)

  /** The script context of a transaction spending one script-locked output. */
  def spending(
      datum: Data,
      redeemer: Data,
      signatories: Seq[PubKeyHash] = Nil,
      validRange: Interval = Interval.always,
  ): Data =
    val ownInput = TxInInfo(
      ownRef,
      TxOut(
        Address(Credential.ScriptCredential(Parties.hash(28, 0x5c)), POption.None),
        Value.lovelace(10_000_000),
      ),
    )
    ScriptContext(
      txInfo = TxInfo(
        inputs = PList(ownInput),
        fee = 188021,
        validRange = validRange,
        signatories = PList.from(signatories),
        id = TxId(Parties.hash(32, 0xfe)),
      ),
      redeemer = redeemer,
      scriptInfo = ScriptInfo.SpendingScript(ownRef, POption.Some(datum)),
    ).toData

/** A validator, the script context of a transaction, and whether the validator must accept it. */
final case class Scenario(name: String, script: PlutusV3[?], context: Data, accepts: Boolean = true)

object Scenarios:
  import CompiledValidators.*
  import Contexts.spending as tx
  import Parties.*

  def fib(n: Int): BigInt =
    (1 to n).foldLeft((BigInt(0), BigInt(1)))((p, _) => (p._2, p._1 + p._2))._1

  /** The Fibonacci puzzle solved for `n`: `n` levels of integer recursion. */
  def fibonacciAt(n: Int): Scenario =
    Scenario(s"fibonacci($n)", fibonacci, tx(fib(n).toData, BigInt(n).toData))

  private def bytes(s: String) = ByteString.fromString(s)
  private val (owner, hello, unit) = (alice.toData, "Hello, World!".toData, Data.unit)
  private val (secret, htlcSecret) = (bytes("open sesame"), bytes("htlc secret"))
  private val image = (Builtins.sha2_256(secret), bob.hash).toData
  private val (deadline, timeout) = (1_700_000_000_000L, 1_800_000_000_000L)
  private val (lock, locked) = (Lock(carol, deadline).toData, Interval.after(deadline - 1))
  private val quorum = Quorum(PList(alice, bob, carol), 2).toData
  private val contract = Contract(alice, bob, Builtins.sha2_256(htlcSecret), timeout).toData
  private val (claim, refund) = (HtlcAction.Claim(htlcSecret).toData, HtlcAction.Refund.toData)
  private val guess = HtlcAction.Claim(bytes("guess")).toData
  private val (inTime, late) = (Interval.before(timeout - 1), Interval.before(timeout + 1))
  private val (after, early) = (Interval.after(timeout + 1), Interval.after(timeout - 1))

  /** Transactions the validators must accept or reject. */
  lazy val all: List[Scenario] = List(
    Scenario("helloOwner", helloOwner, tx(owner, hello, List(alice))),
    Scenario("helloOwner: not signed", helloOwner, tx(owner, hello, List(bob)), false),
    Scenario("helloOwner: wrong greeting", helloOwner, tx(owner, "Hi".toData, List(alice)), false),
    Scenario("preimage", preimage, tx(image, secret.toData, List(bob))),
    Scenario("preimage: wrong", preimage, tx(image, bytes("barley").toData, List(bob)), false),
    Scenario("preimage: not signed", preimage, tx(image, secret.toData, List(alice)), false),
    Scenario("timeLock", timeLock, tx(lock, unit, List(carol), Interval.after(deadline))),
    Scenario("timeLock: too early", timeLock, tx(lock, unit, List(carol), locked), false),
    Scenario("timeLock: no lower bound", timeLock, tx(lock, unit, List(carol)), false),
    Scenario("multisig", multisig, tx(quorum, unit, List(alice, carol))),
    Scenario("multisig: three signers", multisig, tx(quorum, unit, List(carol, bob, alice))),
    Scenario("multisig: one signer", multisig, tx(quorum, unit, List(bob)), false),
    fibonacciAt(30).copy(name = "fibonacci"),
    fibonacciAt(400), // an 84-digit answer, 400 frames deep
    Scenario("fibonacci: wrong", fibonacci, tx((fib(30) + 1).toData, BigInt(30).toData), false),
    Scenario("htlc", htlc, tx(contract, claim, List(bob), inTime)),
    Scenario("htlc: claim too late", htlc, tx(contract, claim, List(bob), late), false),
    Scenario("htlc: wrong preimage", htlc, tx(contract, guess, List(bob), inTime), false),
    Scenario("htlc: refund", htlc, tx(contract, refund, List(alice), after)),
    Scenario("htlc: refund too early", htlc, tx(contract, refund, List(alice), early), false),
  )

  /** One accepting transaction per validator: what the walkthrough and the benchmarks run. */
  lazy val accepting: List[Scenario] = all.filter(_.accepts).distinctBy(_.script)
