package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.validators.CompiledValidators
import scalus.cardano.onchain.plutus.prelude.{List as PList, Option as POption}
import scalus.cardano.onchain.plutus.v1.Value
import scalus.cardano.onchain.plutus.v2.TxOut
import scalus.cardano.onchain.plutus.v3.*
import scalus.uplc.builtin.Data.toData
import scalus.uplc.builtin.{ByteString, Data}
import scalus.uplc.eval.{NoBudgetSpender, NoLogger}

/** A walk through the optic on a real validator, plus a rough timing.
  *
  * {{{sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo"}}}
  *
  * The timing is a single-threaded loop, not a JMH benchmark: read it as an order of magnitude.
  */
object Demo:

  def main(args: Array[String]): Unit =
    val script = CompiledValidators.fibonacci.program
    val flat = script.flatEncoded

    // S ⇒ A
    val typed = UplcJvm.iso.get(flat)
    println(s"S: ${flat.length} bytes of flat UPLC")
    println(
      s"A: the typed expression (first lines)\n${typed.body.show.linesIterator.take(24).mkString("\n")}\n…"
    )

    // A ⇒ B ⇒ T
    val lowered = Lower.lower(typed)
    val classFile = UplcJvm.iso.reverseGet(lowered)
    println(s"B: ${lowered.methods.size} JVM methods")
    println(s"T: ${classFile.length} bytes of class file")

    // Validate one transaction both ways, then time them.
    val n = 400
    val fib = (1 to n).foldLeft((BigInt(0), BigInt(1)))((p, _) => (p._2, p._1 + p._2))._1
    val ctx = context(fib.toData, BigInt(n).toData)
    val applied = (script $ ctx).deBruijnedProgram
    val cek = () => Semantics.vm.evaluateScript(applied, NoBudgetSpender, NoLogger)
    val loaded = Exec.load(classFile)
    val jit = () => runtime.Rt.apply(loaded.run(), scalus.uplc.Constant.Data(ctx))
    println(s"CEK: ${cek().show}, JVM: ${Exec.validate(classFile, ctx)}")
    // Both loops on one large-stack thread: the generated code recurses on the JVM stack.
    val (cekMs, jitMs) = Exec.onLargeStack((time(cek), time(jit)))
    println(f"fib($n) validation, CEK: $cekMs%.3f ms/run, JVM class: $jitMs%.3f ms/run")

  private def time(run: () => Any): Double =
    (1 to 200).foreach(_ => run())
    val rounds = 500
    val start = System.nanoTime()
    (1 to rounds).foreach(_ => run())
    (System.nanoTime() - start) / 1e6 / rounds

  private def context(datum: Data, redeemer: Data): Data =
    val ref = TxOutRef(TxId(ByteString.fill(32, 1)), 0)
    val input = TxInInfo(
      ref,
      TxOut(Address(Credential.ScriptCredential(ByteString.fill(28, 2)), POption.None), Value.zero),
    )
    ScriptContext(
      TxInfo(inputs = PList(input), id = TxId(ByteString.fill(32, 3))),
      redeemer,
      ScriptInfo.SpendingScript(ref, POption.Some(datum)),
    ).toData
