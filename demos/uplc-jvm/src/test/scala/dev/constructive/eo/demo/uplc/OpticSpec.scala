package dev.constructive.eo.demo.uplc

import scala.jdk.CollectionConverters.*
import scala.util.Try

import dev.constructive.eo.demo.uplc.validators.Scenarios
import dev.constructive.eo.laws.discipline.{IsoTests, PrismTests}
import dev.constructive.eo.laws.{IsoLaws, PrismLaws}
import dev.constructive.eo.optics.Iso
import java.util.concurrent.FutureTask
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.{JumpInsnNode, MethodInsnNode}
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline
import scalus.cardano.ledger.Word64
import scalus.uplc.{Constant, DefaultFun, NamedDeBruijn, Term}

/** The optics' laws, and what hybrid typing does to real code. */
class OpticSpec extends Specification with Discipline:

  import Oracle.script

  /** A program whose value is a closure, and every validator. The differential suites check the
    * round trips on every program they run (`Oracle.compare`), the conformance suite's included.
    */
  private val validators = Scenarios.accepting.map(s => script(s.script.program))
  private val scripts = script("[(lam x (lam y x)) (con integer 5)]") :: validators

  private def int(i: Int): Term = Term.Const(Constant.Integer(i))

  /** One constructor with 5000 computed fields and no `let`: one method, over the JVM's 64 KB. */
  private val tooLarge = script(
    Term.Constr(
      Word64(0),
      List.fill(5000)(Term.Apply(Term.Apply(Term.Builtin(DefaultFun.AddInteger), int(1)), int(2)))
    )
  )

  private def tooBig(compiled: Either[String, JvmClassScript]) =
    compiled.left.exists(_.startsWith("Method too large"))

  given Arbitrary[CborScript] = Arbitrary(Gen.oneOf(tooLarge :: scripts))
  given Arbitrary[TypedProgram] = Arbitrary(Gen.oneOf(scripts).map(UplcJvm.toTyped.get))
  given Arbitrary[JvmClassScript] = Arbitrary(Gen.oneOf(scripts).map(Oracle.classFile))
  given Cogen[JvmClassScript] = Cogen[Int].contramap(c => java.util.Arrays.hashCode(c.bytes))

  private def isoTests[S, A](optic: Iso[S, A]) =
    new IsoTests[S, A]:
      val laws = new IsoLaws[S, A]:
        val iso = optic

  checkAll("UplcJvm.toTyped: CborScript ≅ TypedProgram", isoTests(UplcJvm.toTyped).iso)
  checkAll(
    "UplcJvm.toBytecode: CborScript ⇀ JvmClassScript",
    new PrismTests[CborScript, JvmClassScript]:
      val laws = new PrismLaws[CborScript, JvmClassScript]:
        val prism = UplcJvm.toBytecode
    .prism,
  )

  "the compiler is a prism: a class too large for a class file has none" >> {
    (UplcJvm.toBytecode.getOption(tooLarge) must beNone)
      .and(tooBig(UplcJvm.compile(UplcJvm.toJvmClass.get(tooLarge))) must beTrue)
  }

  "scripts, classes and class files hand out copies: changing those changes nothing" >> {
    val s = scripts.head
    val (classFile, cls) = (Oracle.classFile(s), UplcJvm.toJvmClass.get(s))
    s.bytes(0) = 0
    classFile.bytes(10) = 0
    cls.tree.methods.clear()
    (UplcJvm.toBytecode.reverseGet(classFile) must beEqualTo(s))
      .and(UplcJvm.lowering.reverseGet(cls).script must beEqualTo(s))
      .and(UplcJvm.toBytecode.getOption(s) must beEqualTo(Some(classFile)))
  }

  /** `f`, on a thread with a stack of `bytes`. */
  private def onStack[A](bytes: Long)(f: => A): Either[Throwable, A] =
    val task = new FutureTask[A](() => f)
    val thread = new Thread(null, task, "stack", bytes)
    thread.start()
    thread.join()
    Try(task.get()).toEither

  /** Typed, lowered and compiled on a 1 MB stack — the JVM's default for a thread. Scalus' own flat
    * encoder and decoder recurse, so they get a large one.
    */
  private def compiledOnSmallStack(term: Term) =
    val source = onStack(1L << 30)(SourceProgram.decode(script(term))).fold(throw _, identity)
    onStack(1L << 20)(UplcJvm.compile(Lower.lower(Typer.typeProgram(source))))

  "compiling is stack safe: programs nested 100 000 deep" >> {
    def nest(times: Int, inner: Term)(wrap: Term => Term) =
      (1 to times).foldLeft(inner)((t, _) => wrap(t))
    "force (delay …), which the typer reads back as the constant inside: a class file" >> {
      val deep = nest(100000, int(0))(t => Term.Force(Term.Delay(t)))
      compiledOnSmallStack(deep).map(_.isRight) must beEqualTo(Right(true))
    }
    "headList (headList …), one method of direct calls: too large for a class file" >> {
      val headList = Term.Force(Term.Builtin(DefaultFun.HeadList))
      val deep = nest(100000, Term.Const(Constant.List(scalus.uplc.DefaultUni.Integer, Nil)))(
        Term.Apply(headList, _)
      )
      compiledOnSmallStack(deep).map(tooBig) must beEqualTo(Right(true))
    }
    "λx. λx. … x, a closure per 200 lambdas: their dispatch method is too large for a class file" >> {
      val deep = nest(100000, Term.Var(NamedDeBruijn("x", 100000)))(Term.LamAbs("x", _))
      compiledOnSmallStack(deep).map(tooBig) must beEqualTo(Right(true))
    }
  }

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
