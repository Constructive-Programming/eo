package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.validators.{CompiledValidators, Scenarios}
import java.nio.file.{Files, Paths}
import org.objectweb.asm.tree.ClassNode
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
    val typed = UplcJvm.typed.get(script) //            S ⇒ A
    println(s"\nA — typed.get:\n${typed.body.show}")
    val lowered = UplcJvm.lowering.get(typed) //         A ⇒ B
    println(s"\nB — lowering.get, the method `run`:\n${bytecode(lowered, "run")}")
    val classFile = UplcJvm.writing.get(lowered) //      B ⇒ T
    println(s"\nT — writing.get: ${classFile.bytes.length} bytes of class file")
    println(
      s"and back — classFile.reverseGet(T) == S: ${UplcJvm.classFile.reverseGet(classFile) == script}"
    )
    args
      .headOption
      .foreach(dir => Files.write(Paths.get(dir, "UplcProgram.class"), classFile.bytes))
    compare(script, classFile)

    // A program whose value is a closure: it comes back as a term, captures substituted.
    val closure = "[(lam x (lam y x)) (con integer 5)]"
    section("A closure comes back as a term", closure)
    compare(parse(closure), UplcJvm.classFile.get(parse(closure)))

    section("A Scalus-compiled validator, read back (Fibonacci)", "")
    val fib = CborScript.of(CompiledValidators.fibonacci.program)
    println(s"S: ${fib.bytes.length} bytes of script\n\n${UplcJvm.typed.get(fib).body.show}")
    println(s"\n${shape(fib)}")

    section("Every validator: S and T", "")
    Scenarios
      .accepting
      .foreach(s => println(f"${s.name}%-11s ${shape(CborScript.of(s.script.program))}"))

  private def section(title: String, uplc: String): Unit =
    println(s"\n=== $title ===\n${if uplc.isEmpty then "" else s"\n(program 1.1.0 $uplc)"}")

  /** S, B and T sizes: script bytes, closure body methods, class-file bytes. */
  private def shape(script: CborScript): String =
    val lowered = UplcJvm.bytecode.get(script)
    val bodies = lowered.methods.asScala.count(_.name.startsWith("m"))
    val classFile = UplcJvm.writing.get(lowered).bytes.length
    f"S: ${script.bytes.length}%5d bytes   B: $bodies%3d closure bodies   T: $classFile%6d bytes"

  /** One method of B, as ASM prints bytecode. */
  private def bytecode(node: ClassNode, method: String): String =
    val text = new Textifier
    node.methods.asScala.filter(_.name == method).foreach(_.accept(new TraceMethodVisitor(text)))
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
