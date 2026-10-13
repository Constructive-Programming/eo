package dev.constructive.eo.demo.uplc

import scala.util.control.NonFatal

import java.io.{PrintWriter, StringWriter}
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.CheckClassAdapter
import scalus.uplc.eval.{CekMachine, NoBudgetSpender, NoLogger}
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

  /** ASM's bytecode verifier over a generated class; empty when it is well-formed. */
  def verify(classFile: Array[Byte]): String =
    val out = new StringWriter
    CheckClassAdapter.verify(
      new ClassReader(classFile),
      getClass.getClassLoader,
      false,
      new PrintWriter(out)
    )
    out.toString

  enum Verdict:
    case BothSucceed(term: Term)
    case BothFail(jvm: String, cek: String)
    case Disagree(detail: String)

  /** Runs `flat` (applied to `args`) through the generated class and the CEK machine. */
  def compare(flat: Array[Byte], args: Seq[Constant] = Nil): Verdict =
    val classFile = UplcJvm.translate(flat)
    val problems = verify(classFile)
    if problems.nonEmpty then Verdict.Disagree(s"class file fails verification:\n$problems")
    else
      (Exec.run(classFile, args), cek(flat, args)) match
        case (Exec.Outcome.Success(jvm, _), Right(expected)) =>
          if jvm.α_==(expected) then Verdict.BothSucceed(jvm)
          else Verdict.Disagree(s"JVM: ${jvm.show}\nCEK: ${expected.show}")
        case (Exec.Outcome.Failure(jvm, _), Left(expected)) => Verdict.BothFail(jvm, expected)
        case (Exec.Outcome.Success(jvm, _), Left(expected)) =>
          Verdict.Disagree(s"JVM succeeded with ${jvm.show}\nCEK failed: $expected")
        case (Exec.Outcome.Failure(jvm, _), Right(expected)) =>
          Verdict.Disagree(s"JVM failed: $jvm\nCEK succeeded with ${expected.show}")
