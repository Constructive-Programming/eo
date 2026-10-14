package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.validators.{CompiledValidators, Scenarios}
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.{JumpInsnNode, MethodInsnNode}
import org.specs2.mutable.Specification
import scalus.uplc.Program

/** The optic's shape, and what hybrid typing does to real code. */
class OpticSpec extends Specification:

  private def flat(uplc: String): Array[Byte] =
    Program.parseUplc(s"(program 1.1.0 $uplc)").fold(e => sys.error(e), _.flatEncoded)

  /** Every method call in the lowered class, as `Owner.name`. */
  private def calls(flatProgram: Array[Byte]): Vector[String] =
    val methods = Lower.lower(UplcJvm.iso.get(flatProgram)).methods.asScala.toVector
    methods.flatMap(_.instructions.iterator.asScala.collect {
      case m: MethodInsnNode                              => s"${m.owner.split('/').last}.${m.name}"
      case j: JumpInsnNode if j.getOpcode == Opcodes.IFEQ => "if"
    })

  "the optic" >> {
    "translate is iso.modify(Lower.lower): get, lower, reverseGet" >> {
      val s = flat("[(lam x [(builtin addInteger) x (con integer 1)]) (con integer 41)]")
      UplcJvm.translate(s).toSeq must beEqualTo(
        UplcJvm.iso.reverseGet(Lower.lower(UplcJvm.iso.get(s))).toSeq
      )
    }
    "onChain = cborScript andThen iso: CBOR in, the same class file out" >> {
      val program = CompiledValidators.multisig.program
      UplcJvm.onChain.modify(Lower.lower)(program.cborEncoded).toSeq must beEqualTo(
        UplcJvm.translate(program.flatEncoded).toSeq
      )
    }
    "get reads UPLC back as a typed Scala-like expression" >> {
      val a = UplcJvm.iso.get(flat("[(lam x [(builtin multiplyInteger) x x]) (con integer 7)]"))
      a.body.show must beEqualTo("val v0: BigInt = 7\nmultiplyInteger(v0, v0)")
    }
  }

  "hybrid typing" >> {
    "keeps let-bound arithmetic unboxed end to end (the constant read once, one box at the result)" >> {
      val code = calls(
        flat(
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
        flat(
          "(force [(force (builtin ifThenElse)) [(builtin lessThanInteger) (con integer 1) (con integer 2)] (delay (con integer 1)) (delay (con integer 2))])"
        )
      )
      (code.count(_ == "if") must beEqualTo(1)).and(code.count(_ == "Rt.closure") must beEqualTo(0))
    }
    "finds direct builtin calls, direct closure calls and branches in Scalus-compiled validators" >> {
      val code = Scenarios.accepting.flatMap(s => calls(s.script.program.flatEncoded))
      (code.count(_.startsWith("AbstractBuiltins.")) must beGreaterThan(0))
        .and(code.count(_ == "Rt.envOf") must beGreaterThan(0))
        .and(code.count(c => c == "if" || c == "Rt.chooseIndex") must beGreaterThan(0))
    }
  }
