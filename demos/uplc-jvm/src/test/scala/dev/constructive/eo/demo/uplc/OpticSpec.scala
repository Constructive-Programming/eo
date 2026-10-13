package dev.constructive.eo.demo.uplc

import dev.constructive.eo.demo.uplc.validators.CompiledValidators
import org.specs2.mutable.Specification
import scalus.uplc.Program

/** The optic's shape, and what hybrid typing does to real code. */
class OpticSpec extends Specification:

  private def flat(uplc: String): Array[Byte] =
    Program.parseUplc(s"(program 1.1.0 $uplc)").fold(e => sys.error(e), _.flatEncoded)

  /** Every node of lowered code, depth first. */
  private def nodes(program: JvmProgram): Vector[Code] =
    def go(c: Code): Vector[Code] = c +: (c match
      case Code.Box(x, _)               => go(x)
      case Code.Unbox(x, _)             => go(x)
      case Code.Bind(_, rhs, body)      => go(rhs) ++ go(body)
      case Code.If(c0, t, f, _)         => go(c0) ++ go(t) ++ go(f)
      case Code.IntSwitch(s, cs, _)     => go(s) ++ cs.flatMap(go)
      case Code.MakeLambda(_, _, _, cs) => cs.flatMap(go)
      case Code.MakeThunk(_, _, cs)     => cs.flatMap(go)
      case Code.Apply(f, a)             => go(f) ++ go(a)
      case Code.Force(x)                => go(x)
      case Code.Invoke(_, f, as)        => go(f) ++ as.flatMap(go)
      case Code.CallBody(_, cs, as)     => cs.flatMap(go) ++ as.flatMap(go)
      case Code.Spread(as, f)           => as.flatMap(go) ++ go(f)
      case Code.Prim(_, as, _)          => as.flatMap(go)
      case Code.Delegate(_, as)         => as.flatMap(go)
      case Code.Choose(_, s, bs, _)     => go(s) ++ bs.flatMap(go)
      case Code.Construct(_, as)        => as.flatMap(go)
      case Code.CaseOf(s, arms)         =>
        go(s) ++ arms.flatMap {
          case Arm.Value(v)  => go(v)
          case Arm.Direct(l) => go(l)
        }
      case _ => Vector.empty)
    program.methods.flatMap(m => go(m.body))

  private def lowered(flatProgram: Array[Byte]): JvmProgram =
    Lower.lower(UplcJvm.iso.get(flatProgram))

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
    "keeps let-bound arithmetic unboxed end to end (one box, at the result)" >> {
      val code = nodes(
        lowered(
          flat(
            "[(lam x [(lam y [(builtin addInteger) x y]) [(builtin multiplyInteger) x x]]) (con integer 6)]"
          )
        )
      )
      (code.count(_.isInstanceOf[Code.Prim]) must beEqualTo(2))
        .and(code.count(_.isInstanceOf[Code.Box]) must beEqualTo(1))
        .and(code.count(_.isInstanceOf[Code.Unbox]) must beEqualTo(0))
    }
    "turns forced ifThenElse over delays into a JVM branch on a primitive boolean" >> {
      val code = nodes(
        lowered(
          flat(
            "(force [(force (builtin ifThenElse)) [(builtin lessThanInteger) (con integer 1) (con integer 2)] (delay (con integer 1)) (delay (con integer 2))])"
          )
        )
      )
      (code.collect { case i: Code.If => i } must haveSize(1))
        .and(code.count(_.isInstanceOf[Code.MakeThunk]) must beEqualTo(0))
    }
    "finds typed fast paths, direct calls and fused branches in Scalus-compiled validators" >> {
      val validators = List(
        CompiledValidators.helloOwner,
        CompiledValidators.preimage,
        CompiledValidators.timeLock,
        CompiledValidators.multisig,
        CompiledValidators.fibonacci,
        CompiledValidators.htlc,
      )
      val code = validators.flatMap(v => nodes(lowered(v.program.flatEncoded)))
      (code.count(_.isInstanceOf[Code.Prim]) must beGreaterThan(0))
        .and(code.count(_.isInstanceOf[Code.Invoke]) must beGreaterThan(0))
        .and(
          code.count(c =>
            c.isInstanceOf[Code.If] || c.isInstanceOf[Code.Choose]
          ) must beGreaterThan(0)
        )
    }
  }
