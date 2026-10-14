package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.validators.Scenarios
import dev.constructive.eo.laws.IsoLaws
import dev.constructive.eo.laws.discipline.IsoTests
import dev.constructive.eo.optics.Iso
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.{JumpInsnNode, MethodInsnNode}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

/** The optics' laws, and what hybrid typing does to real code. */
class OpticSpec extends Specification with Discipline:

  import Oracle.script

  /** A program whose value is a closure, and every validator. The differential suites check the
    * round trips on every program they run (`Oracle.compare`), the conformance suite's included.
    */
  private val validators = Scenarios.accepting.map(s => script(s.script.program))
  private val scripts = script("[(lam x (lam y x)) (con integer 5)]") :: validators

  given Arbitrary[CborScript] = Arbitrary(Gen.oneOf(scripts))
  given Arbitrary[TypedProgram] = Arbitrary(Gen.oneOf(scripts).map(UplcJvm.toTyped.get))
  given Arbitrary[JvmClassScript] = Arbitrary(Gen.oneOf(scripts).map(UplcJvm.toBytecode.get))

  private def isoTests[S, A](optic: Iso[S, A]) =
    new IsoTests[S, A]:
      val laws = new IsoLaws[S, A]:
        val iso = optic

  checkAll("UplcJvm.toTyped: CborScript ≅ TypedProgram", isoTests(UplcJvm.toTyped).iso)
  checkAll("UplcJvm.toBytecode: CborScript ≅ JvmClassScript", isoTests(UplcJvm.toBytecode).iso)

  "typed reads UPLC back as a typed Scala-like expression" >> {
    val a = UplcJvm.toTyped.get(script("[(lam x [(builtin multiplyInteger) x x]) (con integer 7)]"))
    a.body.show must beEqualTo("val v0: BigInt = 7\nmultiplyInteger(v0, v0)")
  }

  /** Every method call in the lowered class, as `Owner.name`. */
  private def calls(script: CborScript): Vector[String] =
    val methods = UplcJvm.toJvmClass.get(script).tree.methods.asScala.toVector
    methods.flatMap(_.instructions.iterator.asScala.collect {
      case m: MethodInsnNode                              => s"${m.owner.split('/').last}.${m.name}"
      case j: JumpInsnNode if j.getOpcode == Opcodes.IFEQ => "if"
    })

  "hybrid typing" >> {
    "keeps let-bound arithmetic unboxed end to end (the constant read once, one box at the result)" >> {
      val code = calls(
        script(
          "[(lam x [(lam y [(builtin addInteger) x y]) [(builtin multiplyInteger) x x]]) (con integer 6)]"
        )
      )
      (code.filter(_.startsWith("AbstractBuiltins.")) must beEqualTo(
        Vector("AbstractBuiltins.multiplyInteger", "AbstractBuiltins.addInteger")
      ))
        .and(code.count(_.startsWith("Rt.box")) must beEqualTo(1))
        .and(code.filter(_.startsWith("Rt.un")) must beEqualTo(Vector("Rt.unInt")))
    }
    "turns forced ifThenElse over delays into a JVM branch on a primitive boolean" >> {
      val code = calls(
        script(
          "(force [(force (builtin ifThenElse)) [(builtin lessThanInteger) (con integer 1) (con integer 2)] (delay (con integer 1)) (delay (con integer 2))])"
        )
      )
      (code.count(_ == "if") must beEqualTo(1)).and(code.count(_ == "Rt.closure") must beEqualTo(0))
    }
    "finds direct builtin calls, direct closure calls and branches in Scalus-compiled validators" >> {
      val code = Scenarios.accepting.flatMap(s => calls(script(s.script.program)))
      (code.count(_.startsWith("AbstractBuiltins.")) must beGreaterThan(0))
        .and(code.count(_ == "Rt.envOf") must beGreaterThan(0))
        .and(code.count(c => c == "if" || c == "Rt.chooseIndex") must beGreaterThan(0))
    }
  }
