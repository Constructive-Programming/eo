package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.Oracle.Verdict
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths}
import org.specs2.execute.{Result, Skipped}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment
import scalus.uplc.Program

/** The Plutus conformance suite (vendored, see `plutus-conformance/SOURCE.md`) as a differential
  * test: each program is flat-encoded, translated to a class file, run, and compared with what the
  * Scalus CEK machine computes for the same flat bytes.
  *
  * The `.expected` files are used for one thing only: to check the comparison is not vacuous. A
  * case the suite expects to succeed must succeed on both sides, and one it expects to fail must
  * fail on both — so a broken harness cannot pass by having both evaluators fail everything.
  */
class ConformanceSpec extends Specification:

  private val root: Path =
    Paths.get(getClass.getResource("/plutus-conformance/uplc/evaluation").toURI)

  private val cases: List[Path] =
    Files.walk(root).iterator.asScala.filter(_.toString.endsWith(".uplc")).toList.sortBy(_.toString)

  private def read(p: Path): String = new String(Files.readAllBytes(p), UTF_8).trim

  private def check(file: Path): Result =
    val expected = read(Paths.get(file.toString + ".expected"))
    if expected == "parse error" then Skipped("a parser test, not an evaluation")
    else
      Program.parseUplc(read(file)) match
        case Left(e)        => Skipped(s"Scalus cannot parse it: ${e.linesIterator.nextOption()}")
        case Right(program) =>
          CborScript.of(program) match
            case Left(e)       => Skipped(e)
            case Right(script) =>
              val expectFailure = expected == "evaluation failure"
              Oracle.compare(script) match
                case Verdict.Disagree(detail)                => failure(detail)
                case Verdict.BothSucceed(t) if expectFailure =>
                  failure(s"both evaluators succeeded with ${t.show}, the suite expects failure")
                case Verdict.BothFail(jvm, cek) if !expectFailure =>
                  failure(
                    s"both evaluators failed (JVM: $jvm; CEK: $cek), the suite expects $expected"
                  )
                case _ => success

  s"Plutus conformance (${cases.size} UPLC evaluation cases): the JVM class agrees with the CEK machine" >> {
    Fragment.foreach(cases)(file =>
      root.relativize(file).toString.stripSuffix(".uplc") >> check(file)
    )
  }
