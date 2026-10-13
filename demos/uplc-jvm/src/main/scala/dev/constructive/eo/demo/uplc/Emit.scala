package dev.constructive.eo.demo.uplc

import java.lang.reflect.Modifier
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.{ClassWriter, Label, MethodVisitor, Type}

/** B ⇒ T: writes the lowered program as ONE class file.
  *
  * {{{
  * public final class UplcProgram implements runtime.Dispatch {
  *   static final ProgramInfo INFO;   // the flat program, embedded and re-decoded at class init
  *   static final Object[] K, KU;     // its constants, boxed / unboxed
  *   static final UplcProgram INSTANCE;
  *   public static Object entry();                      // the program
  *   private static Object m<i>(Object[] env, Object…); // one per lambda chain / delay
  *   public Object call0/1/2/N(int id, Object[] env, …) // closure dispatch: switch on id
  * }
  * }}}
  *
  * The flat program travels inside the class (as Latin-1 string constants), so the class file is a
  * self-contained artifact: the runtime needs it to map closures back to source terms.
  */
object Emit:

  val ClassName = "dev/constructive/eo/demo/uplc/generated/UplcProgram"
  private val Runtime = "dev/constructive/eo/demo/uplc/runtime/"
  private val Rt = Runtime + "Rt"
  private val Dispatch = Runtime + "Dispatch"
  private val Info = Runtime + "ProgramInfo"
  private val InfoDesc = s"L$Info;"
  private val ObjArr = "[Ljava/lang/Object;"
  private val Obj = "java/lang/Object"

  /** Descriptor of each `Rt` entry point, read off the compiled runtime so it cannot drift. */
  private lazy val rtDescriptors: Map[String, String] =
    Class
      .forName("dev.constructive.eo.demo.uplc.runtime.Rt")
      .getMethods
      .iterator
      .filter(m => Modifier.isStatic(m.getModifiers))
      .map(m => m.getName -> Type.getMethodDescriptor(m))
      .toMap

  /** Largest constant-pool string chunk: 16k chars ≤ 32k bytes of modified UTF-8 (limit 65535). */
  private val Chunk = 16384

  def classFile(program: JvmProgram): Array[Byte] =
    val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS):
      // Every reference type the generated code merges is either identical on both paths or
      // `Object`; never load classes to find out.
      override def getCommonSuperClass(a: String, b: String): String = Obj
    cw.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, ClassName, null, Obj, Array(Dispatch))
    for (name, desc) <- List(
        "INFO" -> InfoDesc,
        "K" -> ObjArr,
        "KU" -> ObjArr,
        "INSTANCE" -> s"L$ClassName;"
      )
    do cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, name, desc, null, null).visitEnd()

    staticInit(cw, program.source.flat)
    constructor(cw)
    program.methods.foreach(m => new MethodEmitter(cw, m).emit())
    dispatch(cw, program.methods)
    accessors(cw, program.methods.head)
    cw.visitEnd()
    cw.toByteArray

  private def methodName(m: JvmMethod): String = m.kind match
    case MethodKind.Entry => "entry"
    case _                => s"m${m.id}"

  private def push(mv: MethodVisitor, n: Int): Unit =
    if n >= -1 && n <= 5 then mv.visitInsn(ICONST_0 + n)
    else if n >= Byte.MinValue && n <= Byte.MaxValue then mv.visitIntInsn(BIPUSH, n)
    else if n >= Short.MinValue && n <= Short.MaxValue then mv.visitIntInsn(SIPUSH, n)
    else mv.visitLdcInsn(Integer.valueOf(n))

  private def rt(mv: MethodVisitor, name: String): Unit =
    mv.visitMethodInsn(INVOKESTATIC, Rt, name, rtDescriptors(name), false)

  private def staticInit(cw: ClassWriter, flat: Array[Byte]): Unit =
    val mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null)
    mv.visitCode()
    val text = String(flat.map(b => (b & 0xff).toChar))
    val chunks = text.grouped(Chunk).toVector
    push(mv, chunks.size)
    mv.visitTypeInsn(ANEWARRAY, "java/lang/String")
    chunks.zipWithIndex.foreach { (chunk, i) =>
      mv.visitInsn(DUP)
      push(mv, i)
      mv.visitLdcInsn(chunk)
      mv.visitInsn(AASTORE)
    }
    rt(mv, "loadProgram")
    mv.visitFieldInsn(PUTSTATIC, ClassName, "INFO", InfoDesc)
    for (field, getter) <- List("K" -> "boxed", "KU" -> "unboxed") do
      mv.visitFieldInsn(GETSTATIC, ClassName, "INFO", InfoDesc)
      mv.visitMethodInsn(INVOKEVIRTUAL, Info, getter, s"()$ObjArr", false)
      mv.visitFieldInsn(PUTSTATIC, ClassName, field, ObjArr)
    mv.visitTypeInsn(NEW, ClassName)
    mv.visitInsn(DUP)
    mv.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", "()V", false)
    mv.visitFieldInsn(PUTSTATIC, ClassName, "INSTANCE", s"L$ClassName;")
    mv.visitInsn(RETURN)
    mv.visitMaxs(0, 0)
    mv.visitEnd()

  private def constructor(cw: ClassWriter): Unit =
    val mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
    mv.visitCode()
    mv.visitVarInsn(ALOAD, 0)
    mv.visitMethodInsn(INVOKESPECIAL, Obj, "<init>", "()V", false)
    mv.visitInsn(RETURN)
    mv.visitMaxs(0, 0)
    mv.visitEnd()

  /** `Dispatch.run()` / `Dispatch.info()`: how the runtime reaches the class without reflection. */
  private def accessors(cw: ClassWriter, entry: JvmMethod): Unit =
    val run = cw.visitMethod(ACC_PUBLIC, "run", "()Ljava/lang/Object;", null, null)
    run.visitCode()
    run.visitMethodInsn(INVOKESTATIC, ClassName, methodName(entry), entry.descriptor, false)
    run.visitInsn(ARETURN)
    run.visitMaxs(0, 0)
    run.visitEnd()
    val info = cw.visitMethod(ACC_PUBLIC, "info", s"()$InfoDesc", null, null)
    info.visitCode()
    info.visitFieldInsn(GETSTATIC, ClassName, "INFO", InfoDesc)
    info.visitInsn(ARETURN)
    info.visitMaxs(0, 0)
    info.visitEnd()

  /** `callK(int id, Object[] env, …)`: a `lookupswitch` from method id to closure body. */
  private def dispatch(cw: ClassWriter, methods: Vector[JvmMethod]): Unit =
    def lambdas(p: Int => Boolean) = methods.filter {
      case JvmMethod(_, MethodKind.LambdaBody(_, a), _) => p(a)
      case _                                            => false
    }
    val thunks = methods.filter(_.kind.isInstanceOf[MethodKind.ThunkBody])
    // (name, descriptor, targets, how to push the target's arguments)
    val tables: List[(String, String, Vector[JvmMethod], (MethodVisitor, JvmMethod) => Unit)] =
      List(
        ("call0", s"(I$ObjArr)Ljava/lang/Object;", thunks, (_, _) => ()),
        (
          "call1",
          s"(I${ObjArr}Ljava/lang/Object;)Ljava/lang/Object;",
          lambdas(_ == 1),
          (mv, _) => mv.visitVarInsn(ALOAD, 3),
        ),
        (
          "call2",
          s"(I${ObjArr}Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
          lambdas(_ == 2),
          (mv, _) =>
            mv.visitVarInsn(ALOAD, 3)
            mv.visitVarInsn(ALOAD, 4),
        ),
        (
          "callN",
          s"(I$ObjArr$ObjArr)Ljava/lang/Object;",
          lambdas(_ => true),
          (mv, m) =>
            val arity = m.kind match
              case MethodKind.LambdaBody(_, a) => a
              case _                           => 0
            (0 until arity).foreach { i =>
              mv.visitVarInsn(ALOAD, 3)
              push(mv, i)
              mv.visitInsn(AALOAD)
            },
        ),
      )
    tables.foreach { (name, desc, targets, args) =>
      val mv = cw.visitMethod(ACC_PUBLIC, name, desc, null, null)
      mv.visitCode()
      val labels = targets.map(_ => new Label)
      val default = new Label
      mv.visitVarInsn(ILOAD, 1)
      mv.visitLookupSwitchInsn(default, targets.map(_.id).toArray, labels.toArray)
      targets.zip(labels).foreach { (m, label) =>
        mv.visitLabel(label)
        mv.visitVarInsn(ALOAD, 2)
        args(mv, m)
        mv.visitMethodInsn(INVOKESTATIC, ClassName, methodName(m), m.descriptor, false)
        mv.visitInsn(ARETURN)
      }
      mv.visitLabel(default)
      mv.visitVarInsn(ILOAD, 1)
      rt(mv, "badDispatch")
      mv.visitInsn(ATHROW)
      mv.visitMaxs(0, 0)
      mv.visitEnd()
    }

  /** Emits one method's body. */
  final private class MethodEmitter(cw: ClassWriter, method: JvmMethod):

    private val access = method.kind match
      case MethodKind.Entry => ACC_PUBLIC | ACC_STATIC
      case _                => ACC_PRIVATE | ACC_STATIC

    private val mv = cw.visitMethod(access, methodName(method), method.descriptor, null, null)

    def emit(): Unit =
      mv.visitCode()
      code(method.body)
      mv.visitInsn(ARETURN)
      mv.visitMaxs(0, 0)
      mv.visitEnd()

    private def internalName(rep: Rep): String = rep match
      case Rep.Int   => "scala/math/BigInt"
      case Rep.Bytes => "scalus/uplc/builtin/ByteString"
      case Rep.Str   => "java/lang/String"
      case _         => Obj

    private def boxName(rep: Rep): String = rep match
      case Rep.Int   => "Integer"
      case Rep.Bool  => "Bool"
      case Rep.Bytes => "Bytes"
      case Rep.Str   => "String"
      case Rep.Obj   => throw new IllegalArgumentException("Obj is already boxed")

    private def objects(items: Vector[Code]): Unit =
      push(mv, items.size)
      mv.visitTypeInsn(ANEWARRAY, Obj)
      items.zipWithIndex.foreach { (item, i) =>
        mv.visitInsn(DUP)
        push(mv, i)
        code(item)
        mv.visitInsn(AASTORE)
      }

    private def env(captures: Vector[Code]): Unit =
      if captures.isEmpty then rt(mv, "noCaptures") else objects(captures)

    private def table(cases: Vector[Code]): Unit =
      val end = new Label
      val default = new Label
      val labels = cases.map(_ => new Label)
      mv.visitTableSwitchInsn(0, cases.size - 1, default, labels*)
      cases.zip(labels).foreach { (c, label) =>
        mv.visitLabel(label)
        code(c)
        mv.visitJumpInsn(GOTO, end)
      }
      mv.visitLabel(default)
      push(mv, -1)
      rt(mv, "badDispatch")
      mv.visitInsn(ATHROW)
      mv.visitLabel(end)

    private def invokeBody(id: Int, arity: Int): Unit =
      val desc = "(" + ObjArr + "Ljava/lang/Object;" * arity + ")Ljava/lang/Object;"
      mv.visitMethodInsn(INVOKESTATIC, ClassName, s"m$id", desc, false)

    private def code(c: Code): Unit = c match
      case Code.Local(slot, rep) =>
        mv.visitVarInsn(if rep == Rep.Bool then ILOAD else ALOAD, slot)

      case Code.Captured(i) =>
        mv.visitVarInsn(ALOAD, 0)
        push(mv, i)
        mv.visitInsn(AALOAD)

      case Code.Literal(ordinal, rep) =>
        mv.visitFieldInsn(GETSTATIC, ClassName, if rep == Rep.Obj then "K" else "KU", ObjArr)
        push(mv, ordinal)
        mv.visitInsn(AALOAD)
        if rep != Rep.Obj then mv.visitTypeInsn(CHECKCAST, internalName(rep))

      case Code.BoolLit(b) => mv.visitInsn(if b then ICONST_1 else ICONST_0)

      case Code.Box(inner, from) =>
        code(inner)
        rt(mv, "box" + boxName(from))

      case Code.Unbox(inner, to) =>
        code(inner)
        rt(mv, "un" + boxName(to))

      case Code.Bind(slot, rhs, body) =>
        code(rhs)
        mv.visitVarInsn(if rhs.rep == Rep.Bool then ISTORE else ASTORE, slot)
        code(body)

      case Code.If(cond, whenTrue, whenFalse, _) =>
        val otherwise = new Label
        val end = new Label
        code(cond)
        mv.visitJumpInsn(IFEQ, otherwise)
        code(whenTrue)
        mv.visitJumpInsn(GOTO, end)
        mv.visitLabel(otherwise)
        code(whenFalse)
        mv.visitLabel(end)

      case Code.IntSwitch(scrut, cases, rep) =>
        code(scrut)
        push(mv, cases.size)
        rt(mv, "integerCaseIndex")
        if cases.isEmpty then
          mv.visitInsn(POP)
          code(Code.Fail("unreachable: empty integer case", rep))
        else table(cases)

      case Code.MakeLambda(id, origin, arity, captures) =>
        mv.visitFieldInsn(GETSTATIC, ClassName, "INSTANCE", s"L$ClassName;")
        push(mv, id)
        push(mv, origin)
        push(mv, arity)
        env(captures)
        rt(mv, "lambda")

      case Code.MakeThunk(id, origin, captures) =>
        mv.visitFieldInsn(GETSTATIC, ClassName, "INSTANCE", s"L$ClassName;")
        push(mv, id)
        push(mv, origin)
        env(captures)
        rt(mv, "thunk")

      case Code.Apply(fn, arg) =>
        code(fn)
        code(arg)
        rt(mv, "apply")

      case Code.Spread(args, fn) =>
        objects(args)
        code(fn)
        rt(mv, "spread")

      case Code.Force(inner) =>
        code(inner)
        rt(mv, "force")

      case Code.Invoke(id, arity, closure, args) =>
        code(closure)
        rt(mv, "envOf")
        args.foreach(code)
        invokeBody(id, arity)

      case Code.CallBody(id, arity, captures, args) =>
        env(captures)
        args.foreach(code)
        invokeBody(id, arity)

      case Code.Prim(op, args) =>
        args.foreach(code)
        rt(mv, op.method)

      case Code.Delegate(fun, args) =>
        push(mv, fun.ordinal)
        objects(args)
        rt(mv, "call")

      case Code.BuiltinValue(fun) =>
        push(mv, fun.ordinal)
        rt(mv, "builtin")

      case Code.Choose(fun, scrut, branches, _) =>
        push(mv, fun.ordinal)
        code(scrut)
        rt(mv, "chooseIndex")
        table(branches)

      case Code.Construct(tag, args) =>
        mv.visitLdcInsn(java.lang.Long.valueOf(tag))
        objects(args)
        rt(mv, "construct")

      case Code.CaseOf(scrut, arms, scrutSlot, fieldsSlot) =>
        code(scrut)
        mv.visitVarInsn(ASTORE, scrutSlot)
        mv.visitVarInsn(ALOAD, scrutSlot)
        push(mv, arms.size)
        rt(mv, "caseIndex")
        mv.visitVarInsn(ALOAD, scrutSlot)
        rt(mv, "caseFields")
        mv.visitVarInsn(ASTORE, fieldsSlot)
        val end = new Label
        val default = new Label
        val labels = arms.map(_ => new Label)
        if arms.isEmpty then
          mv.visitInsn(POP)
          code(Code.Fail("unreachable: case without branches", Rep.Obj))
        else
          mv.visitTableSwitchInsn(0, arms.size - 1, default, labels*)
          arms.zip(labels).foreach { (arm, label) =>
            mv.visitLabel(label)
            arm match
              case Arm.Value(value) =>
                code(value)
                mv.visitVarInsn(ALOAD, fieldsSlot)
                rt(mv, "applyAll")
              case Arm.Direct(lam) =>
                val generic = new Label
                mv.visitVarInsn(ALOAD, fieldsSlot)
                mv.visitInsn(ARRAYLENGTH)
                push(mv, lam.arity)
                mv.visitJumpInsn(IF_ICMPNE, generic)
                env(lam.captures)
                (0 until lam.arity).foreach { i =>
                  mv.visitVarInsn(ALOAD, fieldsSlot)
                  push(mv, i)
                  mv.visitInsn(AALOAD)
                }
                invokeBody(lam.method, lam.arity)
                mv.visitJumpInsn(GOTO, end)
                mv.visitLabel(generic)
                code(lam)
                mv.visitVarInsn(ALOAD, fieldsSlot)
                rt(mv, "applyAll")
            mv.visitJumpInsn(GOTO, end)
          }
          mv.visitLabel(default)
          push(mv, -1)
          rt(mv, "badDispatch")
          mv.visitInsn(ATHROW)
        mv.visitLabel(end)

      case Code.Fail(reason, _) =>
        mv.visitLdcInsn(reason)
        rt(mv, "fail")
        mv.visitInsn(ATHROW)
