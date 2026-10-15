package dev.constructive.eo.demo.uplc
package bench

import scala.compiletime.uninitialized

import dev.constructive.eo.demo.uplc.runtime.{Dispatch, Rt}
import dev.constructive.eo.demo.uplc.validators.{Scenario, Scenarios}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import scalus.uplc.eval.{NoBudgetSpender, NoLogger}
import scalus.uplc.{Constant, DeBruijnedProgram, Program, Term}

/** A validator's script: it has a flat encoding, or the setup fails. */
def script(program: Program): CborScript = CborScript.of(program).fold(sys.error, identity)

/** Its class file: it fits in one, or the setup fails. */
def classFile(script: CborScript): JvmClassScript =
  UplcJvm.compile(UplcJvm.toJvmClass.get(script)).fold(sys.error, identity)

/** Shared state: one accepting transaction, ready for both evaluators.
  *
  *   - `cek` — Scalus' CEK machine evaluating the script applied to its context, the way the ledger
  *     runs it, minus budget accounting (the generated code meters nothing either).
  *   - `jvm` — the generated class, translated and loaded once in setup: evaluate the script, apply
  *     it to the context.
  */
abstract class ScriptState:

  protected def scenario: Scenario

  var applied: DeBruijnedProgram = uninitialized
  var compiled: Dispatch = uninitialized
  var context: Constant = uninitialized

  @Setup(Level.Trial)
  def prepare(): Unit =
    val s = scenario
    applied = (s.script.program $ s.context).deBruijnedProgram
    val classFile = bench.classFile(script(s.script.program))
    compiled = Exec.load(classFile)
    context = Constant.Data(s.context)
    // Both evaluators must accept the transaction, or the timings compare different work.
    val accepted =
      cek == Term.Const(Constant.Unit) && Exec.validate(classFile, s.context).isRight
    require(accepted, s"${s.name}: an evaluator rejects the benchmark transaction")

  @Benchmark def cek: Term = Semantics.vm.evaluateScript(applied, NoBudgetSpender, NoLogger)

  @Benchmark def jvm: AnyRef = Rt.apply(compiled.run(), context)

/** Each validator validating one accepting transaction. */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = Array("-Xss256m"))
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ValidatorBench extends ScriptState:

  @Param(Array("helloOwner", "preimage", "timeLock", "multisig", "fibonacci", "htlc"))
  var validator: String = uninitialized

  protected def scenario: Scenario = Scenarios.accepting.find(_.name == validator).get

/** The Fibonacci puzzle at growing `n`: how each evaluator scales with interpretive work. Slow
  * operations reach steady state late — with 3 × 1 s of warm-up the first measured iteration of
  * every fork was still being compiled — hence the longer warm-up.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = Array("-Xss256m"))
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 1)
class FibonacciBench extends ScriptState:

  @Param(Array("10", "100", "1000"))
  var n: Int = uninitialized

  protected def scenario: Scenario = Scenarios.fibonacciAt(n)

/** What the translation costs: flat bytes to class file, and to a loaded, initialised class. */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = Array("-Xss256m"))
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class TranslateBench:

  @Param(Array("helloOwner", "preimage", "timeLock", "multisig", "fibonacci", "htlc"))
  var validator: String = uninitialized

  var script: CborScript = uninitialized

  @Setup(Level.Trial)
  def prepare(): Unit =
    script = bench.script(Scenarios.accepting.find(_.name == validator).get.script.program)

  @Benchmark def translate: Option[JvmClassScript] = UplcJvm.toBytecode.getOption(script)

  @Benchmark def translateAndLoad: Option[Dispatch] =
    UplcJvm.toBytecode.getOption(script).map(Exec.load)
