package dev.constructive.eo.demo.uplc.validators

import scalus.cardano.onchain.plutus.prelude.*
import scalus.cardano.onchain.plutus.v3.*
import scalus.compiler.{Compile, Options}
import scalus.uplc.PlutusV3
import scalus.uplc.builtin.Builtins.sha2_256
import scalus.uplc.builtin.Data.{FromData, ToData}
import scalus.uplc.builtin.{ByteString, Data}

// On-chain code for the UPLC → JVM demo's oracle corpus. Every object below is
// compiled to UPLC by the Scalus compiler plugin; the demo's suite then runs each
// script, applied to hand-built V3 script contexts, through BOTH the Scalus CEK
// machine and the JVM class the demo emits, and demands the two agree.
//
// The validators deliberately cover different shapes of generated UPLC: Data
// decoding and list search (HelloOwner, Preimage), a crypto builtin (Preimage),
// interval inspection (TimeLock), folds with integer arithmetic (Multisig),
// tight self-recursion (Fibonacci), and enum pattern matching (Htlc).

/** Spending requires the owner's signature and the redeemer `"Hello, World!"`. */
@Compile
object HelloOwner extends Validator:

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val owner = datum.getOrFail("Datum not found").to[PubKeyHash]
    require(tx.signatories.contains(owner), "Must be signed")
    require(redeemer.to[String] == "Hello, World!", "Invalid redeemer")

/** Datum `(sha2_256(preimage), pkh)`: the redeemer must be the preimage, signed by `pkh`. */
@Compile
object Preimage extends Validator:

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val (hash, pkh) = datum.getOrFail("Expected datum").to[(ByteString, ByteString)]
    val preimage = redeemer.toByteString
    tx.signatories.find(_.hash == pkh).orFail("Not signed")
    require(sha2_256(preimage) == hash, "Wrong preimage")

case class Lock(beneficiary: PubKeyHash, deadline: PosixTime) derives FromData, ToData

/** Funds unlock for the beneficiary once the validity range starts at or after the deadline. */
@Compile
object TimeLock extends Validator:

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val lock = datum.getOrFail("No datum").to[Lock]
    require(tx.isSignedBy(lock.beneficiary), "Not signed by beneficiary")
    require(tx.validFromOrFail("No lower bound") >= lock.deadline, "Too early")

case class Quorum(signers: List[PubKeyHash], required: BigInt) derives FromData, ToData

/** m-of-n multisig: counts how many listed signers signed the transaction. */
@Compile
object Multisig extends Validator:

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val quorum = datum.getOrFail("No datum").to[Quorum]
    val signed =
      quorum.signers.foldLeft(BigInt(0))((n, key) => if tx.isSignedBy(key) then n + 1 else n)
    require(signed >= quorum.required, "Not enough signatures")

/** A puzzle lock: the redeemer `n` must satisfy `fib(n) == datum`. Pure integer recursion. */
@Compile
object Fibonacci extends Validator:

  def fib(n: BigInt, a: BigInt, b: BigInt): BigInt =
    if n <= 0 then a else fib(n - 1, b, a + b)

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val n = redeemer.to[BigInt]
    val expected = datum.getOrFail("No datum").to[BigInt]
    require(fib(n, 0, 1) == expected, "Wrong answer")

case class Contract(owner: PubKeyHash, receiver: PubKeyHash, hash: ByteString, timeout: PosixTime)
    derives FromData,
      ToData

enum HtlcAction derives FromData, ToData:
  case Claim(preimage: ByteString)
  case Refund

/** Hash-time-locked contract: the receiver claims with the preimage before the timeout, the owner
  * refunds after it.
  */
@Compile
object Htlc extends Validator:

  override inline def spend(datum: Option[Data], redeemer: Data, tx: TxInfo, ref: TxOutRef): Unit =
    val contract = datum.getOrFail("No datum").to[Contract]
    redeemer.to[HtlcAction] match
      case HtlcAction.Claim(preimage) =>
        require(tx.isSignedBy(contract.receiver), "Not signed by receiver")
        require(tx.validToOrFail("No upper bound") <= contract.timeout, "Too late")
        require(sha2_256(preimage) == contract.hash, "Wrong preimage")
      case HtlcAction.Refund =>
        require(tx.isSignedBy(contract.owner), "Not signed by owner")
        require(tx.validFromOrFail("No lower bound") > contract.timeout, "Too early")

/** The compiled scripts, ready to be applied to a script context. */
object CompiledValidators:

  private given Options = Options.release

  lazy val helloOwner = PlutusV3.compile(HelloOwner.validate)
  lazy val preimage = PlutusV3.compile(Preimage.validate)
  lazy val timeLock = PlutusV3.compile(TimeLock.validate)
  lazy val multisig = PlutusV3.compile(Multisig.validate)
  lazy val fibonacci = PlutusV3.compile(Fibonacci.validate)
  lazy val htlc = PlutusV3.compile(Htlc.validate)
