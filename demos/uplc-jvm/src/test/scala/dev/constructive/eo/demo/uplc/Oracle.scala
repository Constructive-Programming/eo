package dev.constructive.eo.demo.uplc

import scala.util.control.NonFatal

import scalus.uplc.eval.{CekMachine, NoBudgetSpender, NoLogger, Result}
import scalus.uplc.{Constant, DeBruijnedProgram, Term}

/** The differential harness: one flat program, two evaluators, one verdict. */
object Oracle:

  /** What the Scalus CEK machine makes of a flat program (then applied to `args`). */
  def cek(flat: Array[Byte], args: Seq[Constant] = Nil): Either[String, Term] =
    val program = DeBruijnedProgram.fromFlatEncoded(flat)
    val applied = args.foldLeft(program.term)((f, a) => Term.Apply(f, Term.Const(a)))
    val machine = new CekMachine(
      Semantics.vm.machineParams,
      NoBudgetSpender,
      NoLogger,
      Semantics.runtime,
      Semantics.caseOnBuiltins,
      caseOnDataEnabled = Semantics.caseOnData,
    )
    // The oracle's own failures are its verdict, whatever their exception class.
    try Right(Exec.onLargeStack(machine.evaluateTerm(applied)))
    catch case NonFatal(e) => Left(s"${e.getClass.getSimpleName}: ${e.getMessage}")

  enum Verdict:
    case BothSucceed(term: Term)
    case BothFail(jvm: String, cek: String)
    case Disagree(detail: String)

  /** Runs `flat` (applied to `args`) through the generated class and the CEK machine. A class the
    * JVM's verifier rejects fails the comparison with a `VerifyError`.
    */
  def compare(flat: Array[Byte], args: Seq[Constant] = Nil): Verdict =
    (Exec.run(UplcJvm.translate(flat), args), cek(flat, args)) match
      case (Result.Success(jvm, _, _, _), Right(expected)) =>
        if jvm.α_==(expected) then Verdict.BothSucceed(jvm)
        else Verdict.Disagree(s"JVM: ${jvm.show}\nCEK: ${expected.show}")
      case (Result.Failure(jvm, _, _, _), Left(expected)) =>
        Verdict.BothFail(jvm.getMessage, expected)
      case (Result.Success(jvm, _, _, _), Left(expected)) =>
        Verdict.Disagree(s"JVM succeeded with ${jvm.show}\nCEK failed: $expected")
      case (Result.Failure(jvm, _, _, _), Right(expected)) =>
        Verdict.Disagree(s"JVM failed: ${jvm.getMessage}\nCEK succeeded with ${expected.show}")
