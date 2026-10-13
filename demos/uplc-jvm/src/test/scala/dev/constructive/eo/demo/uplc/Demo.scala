package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.validators.{CompiledValidators, Scenarios}
import java.nio.file.{Files, Paths}
import scalus.uplc.{DeBruijn, DeBruijnedProgram, Program}

/** The round trip, stage by stage — what the docs page (`site/docs/demos/uplc-jvm.md`) shows.
  *
  * {{{sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo [dir-for-the-class-file]"}}}
  *
  * Timings live in the JMH project (`demos/uplc-jvm-bench`), not here.
  */
object Demo:

  /** `let x = 6 in let y = x * x in x + y`, as UPLC compilers write it. */
  val letArithmetic: String =
    "(program 1.1.0 [(lam x [(lam y [(builtin addInteger) x y]) [(builtin multiplyInteger) x x]]) (con integer 6)])"

  /** A program whose value is a closure: it comes back as a term, captures substituted. */
  val closureResult: String = "(program 1.1.0 [(lam x (lam y x)) (con integer 5)])"

  def main(args: Array[String]): Unit =
    section("A small program, all the way round")
    val flat = parse(letArithmetic)
    println(letArithmetic)
    val typed = UplcJvm.iso.get(flat) //                  forth: S ⇒ A
    println(s"\nA — iso.get:\n${typed.body.show}")
    val lowered = Lower.lower(typed) //                    A ⇒ B
    println(s"\nB — Lower.lower:\n${lowered.show}")
    val classFile = UplcJvm.iso.reverseGet(lowered) //     back: B ⇒ T
    println(s"\nT — iso.reverseGet: ${classFile.length} bytes of class file")
    args.headOption.foreach(dir => Files.write(Paths.get(dir, "UplcProgram.class"), classFile))
    compare(flat, classFile)

    section("A closure comes back as a term")
    val closure = parse(closureResult)
    println(closureResult)
    compare(closure, UplcJvm.translate(closure))

    section("A Scalus-compiled validator, read back (Fibonacci)")
    val fib = CompiledValidators.fibonacci.program.flatEncoded
    val fibTyped = UplcJvm.iso.get(fib)
    println(s"S: ${fib.length} bytes of flat UPLC\n")
    println(fibTyped.body.show)
    val fibLowered = Lower.lower(fibTyped)
    println(
      s"\nB: ${fibLowered.methods.size} methods; T: ${UplcJvm.iso.reverseGet(fibLowered).length} bytes"
    )

    section("Every validator: S and T")
    Scenarios.accepting.foreach { s =>
      val flat = s.script.program.flatEncoded
      val lowered = Lower.lower(UplcJvm.iso.get(flat))
      println(
        f"${s.name}%-11s S: ${flat.length}%5d bytes   B: ${lowered.methods.size}%3d methods   T: ${UplcJvm.iso.reverseGet(lowered).length}%6d bytes"
      )
    }

  private def section(title: String): Unit = println(s"\n=== $title ===\n")

  private def parse(uplc: String): Array[Byte] =
    Program.parseUplc(uplc).fold(e => sys.error(e), _.flatEncoded)

  /** Runs the class, discharges its value, and puts the CEK machine's answer next to it. */
  private def compare(flat: Array[Byte], classFile: Array[Byte]): Unit =
    val cek = Semantics.vm.evaluateDeBruijnedTerm(DeBruijnedProgram.fromFlatEncoded(flat).term)
    Exec.run(classFile) match
      case Exec.Outcome.Success(t, _) =>
        println(s"\nrun + discharge: ${DeBruijn.fromDeBruijnTerm(t).show}")
        println(s"CEK machine:     ${cek.show}")
        println(s"α-equal:         ${t.α_==(DeBruijn.deBruijnTerm(cek))}")
      case Exec.Outcome.Failure(reason, _) => println(s"\nrun failed: $reason")
