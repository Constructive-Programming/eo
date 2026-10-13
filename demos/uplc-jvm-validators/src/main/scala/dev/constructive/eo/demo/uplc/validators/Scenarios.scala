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

/** A validator together with the script context of a transaction it accepts. */
final case class Scenario(name: String, script: PlutusV3[?], context: Data)

object Scenarios:
  import CompiledValidators.*
  import Parties.*

  def fib(n: Int): BigInt =
    (1 to n).foldLeft((BigInt(0), BigInt(1)))((p, _) => (p._2, p._1 + p._2))._1

  /** The Fibonacci puzzle solved for `n`: `n` levels of integer recursion. */
  def fibonacciAt(n: Int): Scenario =
    Scenario(s"fibonacci($n)", fibonacci, Contexts.spending(fib(n).toData, BigInt(n).toData))

  private val secret = ByteString.fromString("open sesame")
  private val htlcSecret = ByteString.fromString("htlc secret")
  private val timeout = 1_800_000_000_000L

  /** One accepting transaction per validator. */
  lazy val accepting: List[Scenario] = List(
    Scenario(
      "helloOwner",
      helloOwner,
      Contexts.spending(alice.toData, "Hello, World!".toData, List(alice)),
    ),
    Scenario(
      "preimage",
      preimage,
      Contexts.spending((Builtins.sha2_256(secret), bob.hash).toData, secret.toData, List(bob)),
    ),
    Scenario(
      "timeLock",
      timeLock,
      Contexts.spending(
        Lock(carol, 1_700_000_000_000L).toData,
        Data.unit,
        List(carol),
        Interval.after(1_700_000_000_001L),
      ),
    ),
    Scenario(
      "multisig",
      multisig,
      Contexts.spending(Quorum(PList(alice, bob, carol), 2).toData, Data.unit, List(alice, carol)),
    ),
    fibonacciAt(30).copy(name = "fibonacci"),
    Scenario(
      "htlc",
      htlc,
      Contexts.spending(
        Contract(alice, bob, Builtins.sha2_256(htlcSecret), timeout).toData,
        HtlcAction.Claim(htlcSecret).toData,
        List(bob),
        Interval.before(timeout - 1),
      ),
    ),
  )
