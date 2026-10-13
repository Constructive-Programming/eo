package dev.constructive.eo.demo.uplc

import scala.collection.mutable

import dev.constructive.eo.demo.uplc.runtime.{Dispatch, ProgramInfo}
import java.util.Base64
import org.cojen.maker.{ClassMaker, MethodMaker, Variable}

/** B ⇒ T: writes the lowered program as ONE class file, with [Cojen
  * Maker](https://github.com/cojen/Maker): code is written against variables and method names; the
  * library resolves overloads, allocates locals and computes the stack maps.
  *
  * {{{
  * public final class UplcProgram implements runtime.Dispatch {
  *   static final ProgramInfo INFO;   // the flat program, embedded and re-decoded at class init
  *   static final Object[] K, KU;     // its constants by node id, boxed / unboxed
  *   static final UplcProgram INSTANCE;
  *   private static Object entry();                     // the program
  *   private static Object m<i>(Object[] env, Object…); // one per lambda chain / delay
  *   public Object call0/1/2/N(int id, Object[] env, …) // closure dispatch: switch on id
  * }
  * }}}
  *
  * The flat program travels inside the class (as Base64 string constants), so the class file is a
  * self-contained artifact: the runtime needs it to map closures back to source terms.
  */
object Emit:

  val ClassName = "dev.constructive.eo.demo.uplc.generated.UplcProgram"

  /** The runtime's entry points, called through `Rt`'s static forwarders. */
  private lazy val Rt: Class[?] = Class.forName("dev.constructive.eo.demo.uplc.runtime.Rt")

  private val Obj = classOf[AnyRef]
  private val Objs = classOf[Array[AnyRef]]

  /** Largest constant-pool string is 65535 bytes; Base64 text is one byte per char. */
  private val Chunk = 65535

  def classFile(program: JvmProgram): Array[Byte] =
    val cm = ClassMaker.beginExternal(ClassName).public_().final_().implement(classOf[Dispatch])
    cm.addField(classOf[ProgramInfo], "INFO").private_().static_().final_()
    cm.addField(Objs, "K").private_().static_().final_()
    cm.addField(Objs, "KU").private_().static_().final_()
    cm.addField(cm, "INSTANCE").public_().static_().final_()
    cm.addConstructor().public_().invokeSuperConstructor()
    staticInit(cm, program.source.flat)

    // Declared before any body is written, so bodies can call each other by name.
    val makers =
      program.methods.map(m => cm.addMethod(Obj, name(m), params(m)*).private_().static_())
    program.methods.zip(makers).foreach((m, mm) => new Body(mm).emit(m.body))

    val lambdas = program.methods.collect {
      case m @ JvmMethod(_, MethodKind.LambdaBody(_, arity), _) => (m, arity)
    }
    val thunks =
      program.methods.collect { case m @ JvmMethod(_, MethodKind.ThunkBody(_), _) => (m, 0) }
    def ofArity(n: Int) = lambdas.filter(_._2 == n)
    dispatch(cm, "call0", Nil, thunks)((_, _) => Nil)
    dispatch(cm, "call1", List(Obj), ofArity(1))((mm, _) => List(mm.param(2)))
    dispatch(cm, "call2", List(Obj, Obj), ofArity(2))((mm, _) => List(mm.param(2), mm.param(3)))
    dispatch(cm, "callN", List(Objs), lambdas)((mm, arity) =>
      (0 until arity).map(mm.param(2).aget(_))
    )

    val run = cm.addMethod(Obj, "run").public_()
    run.return_(run.invoke("entry"))
    val info = cm.addMethod(classOf[ProgramInfo], "info").public_()
    info.return_(info.field("INFO"))
    cm.finishBytes()

  private def name(m: JvmMethod): String = m.kind match
    case MethodKind.Entry => "entry"
    case _                => s"m${m.id}"

  private def params(m: JvmMethod): List[Class[?]] = m.kind match
    case MethodKind.Entry                => Nil
    case MethodKind.LambdaBody(_, arity) => Objs :: List.fill(arity)(Obj)
    case MethodKind.ThunkBody(_)         => List(Objs)

  private def staticInit(cm: ClassMaker, flat: Array[Byte]): Unit =
    val mm = cm.addClinit()
    val chunks = Base64.getEncoder.encodeToString(flat).grouped(Chunk).toVector
    val parts = mm.new_(classOf[Array[String]], chunks.size)
    chunks.zipWithIndex.foreach((chunk, i) => parts.aset(i, chunk))
    val info = mm.`var`(classOf[ProgramInfo]).invoke("fromChunks", parts)
    mm.field("INFO").set(info)
    mm.field("K").set(info.invoke("boxed"))
    mm.field("KU").set(info.invoke("unboxed"))
    mm.field("INSTANCE").set(mm.new_(cm)): Unit

  /** `callK(int id, Object[] env, …)`: a switch from method id to closure body. `args` loads the
    * target's arguments, given its arity.
    */
  private def dispatch(
      cm: ClassMaker,
      name: String,
      extra: List[Class[?]],
      targets: Vector[(JvmMethod, Int)]
  )(
      args: (MethodMaker, Int) => Seq[Variable]
  ): Unit =
    val mm = cm.addMethod(Obj, name, (classOf[Int] :: Objs :: extra)*).public_()
    val labels = targets.map(_ => mm.label())
    val default = mm.label()
    mm.param(0).switch_(default, targets.map(_._1.id).toArray, labels*)
    targets.zip(labels).foreach {
      case ((m, arity), label) =>
        label.here()
        mm.return_(mm.invoke(Emit.name(m), (mm.param(1) +: args(mm, arity))*))
    }
    default.here()
    mm.`var`(Rt).invoke("badDispatch", mm.param(0)).throw_()

  /** Writes one method body. Each [[Code]] node becomes a variable holding its value; the order the
    * nodes are visited in is the order their code runs in.
    */
  final private class Body(mm: MethodMaker):

    private val rt = mm.`var`(Rt)

    private val locals =
      mutable.HashMap.from[Int, Variable]((0 until mm.paramCount).map(i => i -> mm.param(i)))

    def emit(body: Code): Unit = mm.return_(code(body))

    extension (v: Variable) private def :=(value: Variable): Unit = v.set(value): Unit

    private def objects(items: Vector[Code]): Variable =
      val array = mm.new_(Objs, items.size)
      items.zipWithIndex.foreach((item, i) => array.aset(i, code(item)))
      array

    private def env(captures: Vector[Code]): Variable =
      if captures.isEmpty then rt.invoke("noCaptures") else objects(captures)

    /** Branch `i` for `index == i`; any other index is a compiler bug. */
    private def select[A](index: Variable, arms: Vector[A], rep: Rep)(
        arm: (A, Variable) => Unit
    ): Variable =
      val result = mm.`var`(rep.jvm)
      val end = mm.label()
      val default = mm.label()
      val labels = arms.map(_ => mm.label())
      index.switch_(default, arms.indices.toArray, labels*)
      arms.zip(labels).foreach { (a, label) =>
        label.here()
        arm(a, result)
        end.goto_()
      }
      default.here()
      rt.invoke("badDispatch", -1).throw_()
      end.here()
      result

    private def code(c: Code): Variable = c match
      case Code.Local(slot, _)         => locals(slot)
      case Code.Captured(i)            => locals(0).aget(i)
      case Code.Literal(node, Rep.Obj) => mm.field("K").aget(node)
      case Code.Literal(node, rep)     => mm.field("KU").aget(node).cast(rep.jvm)
      case Code.BoolLit(b)             => mm.`var`(classOf[Boolean]).set(b)
      case Code.Box(inner, _)          => rt.invoke("box", code(inner))
      case Code.Unbox(inner, to)       => rt.invoke(s"un$to", code(inner))
      case Code.Bind(slot, rhs, body)  =>
        locals(slot) = code(rhs)
        code(body)

      case Code.If(cond, whenTrue, whenFalse, rep) =>
        val result = mm.`var`(rep.jvm)
        code(cond).ifTrue(() => result := code(whenTrue), () => result := code(whenFalse))
        result

      case Code.IntSwitch(scrut, cases, rep) =>
        val index = rt.invoke("integerCaseIndex", code(scrut), cases.size)
        select(index, cases, rep)((c, result) => result := code(c))

      case Code.MakeLambda(id, origin, arity, captures) =>
        rt.invoke("lambda", mm.field("INSTANCE"), id, origin, arity, env(captures))
      case Code.MakeThunk(id, origin, captures) =>
        rt.invoke("thunk", mm.field("INSTANCE"), id, origin, env(captures))

      case Code.Apply(fn, arg)   => rt.invoke("apply", code(fn), code(arg))
      case Code.Spread(args, fn) =>
        val values = objects(args)
        rt.invoke("applyAll", code(fn), values)
      case Code.Force(inner) => rt.invoke("force", code(inner))

      case Code.Invoke(id, closure, args) =>
        val closureEnv = rt.invoke("envOf", code(closure))
        mm.invoke(s"m$id", (closureEnv +: args.map(code))*)
      case Code.CallBody(id, captures, args) =>
        val bodyEnv = env(captures)
        mm.invoke(s"m$id", (bodyEnv +: args.map(code))*)

      case Code.Prim(fun, args, _)  => rt.invoke(Semantics.name(fun), args.map(code)*)
      case Code.Delegate(fun, args) => rt.invoke("call", fun.ordinal, objects(args))
      case Code.BuiltinValue(fun)   => rt.invoke("builtin", fun.ordinal)

      case Code.Choose(fun, scrut, branches, rep) =>
        val index = rt.invoke("chooseIndex", fun.ordinal, code(scrut))
        select(index, branches, rep)((c, result) => result := code(c))

      case Code.Construct(tag, args) => rt.invoke("construct", tag, objects(args))

      case Code.CaseOf(scrut, arms) =>
        val value = code(scrut)
        val index = rt.invoke("caseIndex", value, arms.size)
        val fields = rt.invoke("caseFields", value)
        select(index, arms, Rep.Obj) {
          case (Arm.Value(branch), result) => result := rt.invoke("applyAll", code(branch), fields)
          case (Arm.Direct(lam), result)   =>
            // The field count matches the chain's arity: call its body method directly.
            fields
              .alength()
              .ifEq(
                lam.arity,
                () =>
                  val args = (0 until lam.arity).map(i => fields.aget(i))
                  result := mm.invoke(s"m${lam.method}", (env(lam.captures) +: args)*)
                ,
                () => result := rt.invoke("applyAll", code(lam), fields),
              )
        }

      case Code.Fail(reason, rep) =>
        rt.invoke("fail", reason).throw_()
        mm.`var`(rep.jvm) // unreachable; stands for the value the failed expression never has
