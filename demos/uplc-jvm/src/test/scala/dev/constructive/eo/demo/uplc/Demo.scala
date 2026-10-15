package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.validators.{CompiledValidators, Scenarios}
import java.nio.file.{Files, Paths}
import org.objectweb.asm.util.{Textifier, TraceMethodVisitor}
import scalus.uplc.eval.Result
import scalus.uplc.{DeBruijn, DeBruijnedProgram}

/** The round trip, stage by stage — what the docs page (`site/docs/demos/uplc-jvm.md`) shows.
  *
  * {{{sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo [dir-for-the-class-file]"}}}
  *
  * Timings live in the JMH project (`demos/uplc-jvm-bench`), not here.
  */
object Demo:

  def main(args: Array[String]): Unit =
    // let x = 6 in let y = x * x in x + y, as UPLC compilers write it
    val letArithmetic =
      "[(lam x [(lam y [(builtin addInteger) x y]) [(builtin multiplyInteger) x x]]) (con integer 6)]"
    section("A small program, all the way round", letArithmetic)
    val script = parse(letArithmetic)
    val typed = UplcJvm.toTyped.get(script)
    println(s"\ntoTyped.get:\n${typed.body.show}")
    val cls = UplcJvm.lowering.get(typed)
    println(s"\nlowering.get, the method `run`:\n${bytecode(cls, "run")}")
    val classFile = Oracle.classFile(script)
    println(s"\ncompiling.getOption: ${classFile.bytes.length} bytes of class file")
    println(
      s"and back — toBytecode.reverseGet(that) == script: ${UplcJvm.toBytecode.reverseGet(classFile) == script}"
    )
    args
      .headOption
      .foreach(dir => Files.write(Paths.get(dir, "UplcProgram.class"), classFile.bytes))
    compare(script, classFile)

    // A program whose value is a closure: it comes back as a term, captures substituted.
    val closure = "[(lam x (lam y x)) (con integer 5)]"
    section("A closure comes back as a term", closure)
    compare(parse(closure), Oracle.classFile(parse(closure)))

    section("A Scalus-compiled validator, read back (Fibonacci)", "")
    val fib = Oracle.script(CompiledValidators.fibonacci.program)
    println(s"${fib.bytes.length} bytes of script\n\n${UplcJvm.toTyped.get(fib).body.show}")
    println(s"\n${shape(fib)}")

    section("Every validator", "")
    Scenarios
      .accepting
      .foreach(s => println(f"${s.name}%-11s ${shape(Oracle.script(s.script.program))}"))

  private def section(title: String, uplc: String): Unit =
    println(s"\n=== $title ===\n${if uplc.isEmpty then "" else s"\n(program 1.1.0 $uplc)"}")

  /** Sizes: script bytes, closure body methods in the class, class-file bytes. */
  private def shape(script: CborScript): String =
    val cls = UplcJvm.toJvmClass.get(script)
    val bodies = cls.tree.methods.asScala.count(_.name.startsWith("m"))
    val classFile = Oracle.classFile(script).bytes.length
    f"script ${script.bytes.length}%4d bytes   class: $bodies%2d closure bodies   class file ${classFile}%5d bytes"

  /** One method of a class, as ASM prints bytecode. */
  private def bytecode(cls: JvmClass, method: String): String =
    val text = new Textifier
    cls
      .tree
      .methods
      .asScala
      .filter(_.name == method)
      .foreach(_.accept(new TraceMethodVisitor(text)))
    text.getText.asScala.mkString

  private def parse(uplc: String): CborScript = Oracle.script(uplc)

  /** Runs the class, discharges its value, and puts the CEK machine's answer next to it. */
  private def compare(script: CborScript, classFile: JvmClassScript): Unit =
    val cek = Semantics.vm.evaluateDeBruijnedTerm(DeBruijnedProgram.fromCbor(script.bytes).term)
    Exec.run(classFile) match
      case Result.Success(t, _, _, _) =>
        println(s"\nrun + discharge: ${DeBruijn.fromDeBruijnTerm(t).show}")
        println(s"CEK machine:     ${cek.show}")
        println(s"α-equal:         ${t.α_==(DeBruijn.deBruijnTerm(cek))}")
      case Result.Failure(e, _, _, _) => println(s"\nrun failed: ${e.getMessage}")
