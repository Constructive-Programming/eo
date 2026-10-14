package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.validators.{CompiledValidators, Scenarios}
import java.nio.file.{Files, Paths}
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.{Textifier, TraceMethodVisitor}
import scalus.uplc.eval.Result
import scalus.uplc.{DeBruijn, DeBruijnedProgram, Program}

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
    val flat = parse(letArithmetic)
    val typed = UplcJvm.iso.get(flat) //                  forth: S ⇒ A
    println(s"\nA — iso.get:\n${typed.body.show}")
    val lowered = Lower.lower(typed) //                    A ⇒ B
    println(s"\nB — Lower.lower, the method `run`:\n${bytecode(lowered, "run")}")
    val classFile = UplcJvm.iso.reverseGet(lowered) //     back: B ⇒ T
    println(s"\nT — iso.reverseGet: ${classFile.length} bytes of class file")
    args.headOption.foreach(dir => Files.write(Paths.get(dir, "UplcProgram.class"), classFile))
    compare(flat, classFile)

    // A program whose value is a closure: it comes back as a term, captures substituted.
    val closure = "[(lam x (lam y x)) (con integer 5)]"
    section("A closure comes back as a term", closure)
    compare(parse(closure), UplcJvm.translate(parse(closure)))

    section("A Scalus-compiled validator, read back (Fibonacci)", "")
    val fib = CompiledValidators.fibonacci.program.flatEncoded
    println(s"S: ${fib.length} bytes of flat UPLC\n\n${UplcJvm.iso.get(fib).body.show}")
    println(s"\n${shape(fib)}")

    section("Every validator: S and T", "")
    Scenarios
      .accepting
      .foreach(s => println(f"${s.name}%-11s ${shape(s.script.program.flatEncoded)}"))

  private def section(title: String, uplc: String): Unit =
    println(s"\n=== $title ===\n${if uplc.isEmpty then "" else s"\n(program 1.1.0 $uplc)"}")

  /** S, B and T sizes: flat bytes, closure body methods, class-file bytes. */
  private def shape(flat: Array[Byte]): String =
    val lowered = Lower.lower(UplcJvm.iso.get(flat))
    val bodies = lowered.methods.asScala.count(_.name.startsWith("m"))
    f"S: ${flat.length}%5d bytes   B: $bodies%3d closure bodies   T: ${UplcJvm.iso.reverseGet(lowered).length}%6d bytes"

  /** One method of B, as ASM prints bytecode. */
  private def bytecode(node: ClassNode, method: String): String =
    val text = new Textifier
    node.methods.asScala.filter(_.name == method).foreach(_.accept(new TraceMethodVisitor(text)))
    text.getText.asScala.mkString

  private def parse(uplc: String): Array[Byte] =
    Program.parseUplc(s"(program 1.1.0 $uplc)").fold(e => sys.error(e), _.flatEncoded)

  /** Runs the class, discharges its value, and puts the CEK machine's answer next to it. */
  private def compare(flat: Array[Byte], classFile: Array[Byte]): Unit =
    val cek = Semantics.vm.evaluateDeBruijnedTerm(DeBruijnedProgram.fromFlatEncoded(flat).term)
    Exec.run(classFile) match
      case Result.Success(t, _, _, _) =>
        println(s"\nrun + discharge: ${DeBruijn.fromDeBruijnTerm(t).show}")
        println(s"CEK machine:     ${cek.show}")
        println(s"α-equal:         ${t.α_==(DeBruijn.deBruijnTerm(cek))}")
      case Result.Failure(e, _, _, _) => println(s"\nrun failed: ${e.getMessage}")
