package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.Oracle.Verdict
import dev.constructive.eo.demo.uplc.validators.{Scenario, Scenarios}
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment
import scalus.uplc.{Constant, Term}

/** Smart-contract validation, differential: validators written in Scala and compiled to UPLC by the
  * Scalus plugin run against V3 script contexts through the CEK machine and through the generated
  * class, for transactions they must accept and transactions they must reject.
  *
  * Each script is exercised two ways:
  *   - compiled ONCE from its on-chain form (`UplcJvm.toBytecode`) and applied to the context at
  *     run time — how a node would use a JIT;
  *   - applied to the context first and translated as a whole, the context then being one large
  *     embedded `Data` constant.
  */
class ValidatorSpec extends Specification:

  private def check(s: Scenario): Result =
    val classFile = Oracle.classFile(Oracle.script(s.script.program))
    val verdict = Oracle.compare(Oracle.script(s.script.program $ s.context))
    // CIP-117: a V3 script validates iff it evaluates to unit.
    val oracleAccepts = verdict match
      case Verdict.BothSucceed(Term.Const(Constant.Unit, _)) => true
      case _                                                 => false
    (verdict must not(beAnInstanceOf[Verdict.Disagree]))
      .and(oracleAccepts must beEqualTo(s.accepts))
      .and(Exec.validate(classFile, s.context).isRight must beEqualTo(s.accepts))

  "Scalus-compiled validators: the JVM class and the CEK machine reach the same verdict" >> {
    Fragment.foreach(Scenarios.all) { s =>
      s"${s.name} — ${if s.accepts then "accepts" else "rejects"}" >> check(s)
    }
  }
