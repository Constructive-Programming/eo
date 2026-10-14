package dev.constructive.eo.demo.uplc

import scala.util.control.NonFatal

import io.bullet.borer.Cbor
import scalus.uplc.eval.{CekMachine, NoBudgetSpender, NoLogger, Result}
import scalus.uplc.{Constant, DeBruijnedProgram, Program, ProgramFlatCodec, Term}

/** The differential harness: one script, two evaluators, one verdict. */
object Oracle:

  def script(uplc: String): CborScript =
    CborScript.of(Program.parseUplc(s"(program 1.1.0 $uplc)").fold(e => sys.error(e), identity))

  /** A de Bruijn term as it is: Scalus' encoder would re-derive its indices from names. */
  def script(term: Term): CborScript =
    val flat = ProgramFlatCodec.unsafeEncodeFlat(Program((1, 1, 0), term))
    CborScript.parse(Cbor.encode(flat).toByteArray).toOption.get

  /** What the Scalus CEK machine makes of a script (then applied to `args`). */
  def cek(script: CborScript, args: Seq[Constant] = Nil): Either[String, Term] =
    val program = DeBruijnedProgram.fromCbor(script.bytes)
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

  /** Runs `script` (applied to `args`) through the generated class and the CEK machine. A class the
    * JVM's verifier rejects fails the comparison with a `VerifyError`.
    */
  def compare(script: CborScript, args: Seq[Constant] = Nil): Verdict =
    val classFile = UplcJvm.classFile.get(script)
    // The optics' laws, on every program: the script comes back out of its class, and the class
    // file survives ASM reading it and writing it again.
    if UplcJvm.classFile.reverseGet(classFile) != script ||
      UplcJvm.writing.get(UplcJvm.writing.reverseGet(classFile)) != classFile
    then Verdict.Disagree("the class file does not round-trip")
    else compare(classFile, script, args)

  private def compare(classFile: JvmClassScript, script: CborScript, args: Seq[Constant]) =
    (Exec.run(classFile, args), cek(script, args)) match
      case (Result.Success(jvm, _, _, _), Right(expected)) =>
        if jvm.α_==(expected) then Verdict.BothSucceed(jvm)
        else Verdict.Disagree(s"JVM: ${jvm.show}\nCEK: ${expected.show}")
      case (Result.Failure(jvm, _, _, _), Left(expected)) =>
        Verdict.BothFail(jvm.getMessage, expected)
      case (Result.Success(jvm, _, _, _), Left(expected)) =>
        Verdict.Disagree(s"JVM succeeded with ${jvm.show}\nCEK failed: $expected")
      case (Result.Failure(jvm, _, _, _), Right(expected)) =>
        Verdict.Disagree(s"JVM failed: ${jvm.getMessage}\nCEK succeeded with ${expected.show}")
