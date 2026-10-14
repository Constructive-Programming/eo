package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import dev.constructive.eo.demo.uplc.runtime.{Dispatch, ProgramInfo}
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.tree.{ClassNode, LdcInsnNode}
import org.objectweb.asm.{Label, MethodVisitor, Type}
import scalus.uplc.builtin.ByteString
import scalus.uplc.{Constant, DefaultFun, DefaultUni}

/** How generated code holds a value. `Obj` is the uniform boxed representation every UPLC value can
  * take — a Scalus `Constant`, or a runtime closure / thunk / partial builtin / constructor; the
  * others are unboxed, the JVM classes Scalus' own builtins compute with.
  */
enum Rep(val jvm: Class[?]):
  case Obj extends Rep(classOf[AnyRef])
  case Int extends Rep(classOf[BigInt])
  case Bool extends Rep(classOf[Boolean])
  case Bytes extends Rep(classOf[ByteString])
  case Str extends Rep(classOf[String])
  case Data extends Rep(classOf[scalus.uplc.builtin.Data])

  val asm: Type = Type.getType(jvm)

  /** The runtime's conversions from and to the boxed representation, `Rt.box<Rep>` / `Rt.un<Rep>`.
    */
  lazy val (box, un) = (s"box$this", s"un$this")

object Rep:

  def of(ty: Ty): Rep = ty match
    case Some(DefaultUni.Integer)    => Int
    case Some(DefaultUni.Bool)       => Bool
    case Some(DefaultUni.ByteString) => Bytes
    case Some(DefaultUni.String)     => Str
    case Some(DefaultUni.Data)       => Data
    case _                           => Obj

  def of(jvm: Class[?]): Option[Rep] = values.find(_.jvm == jvm)

/** A ⇒ B: lowers the typed expression to JVM bytecode — the `f` in `iso.modify(f)`. B is ASM's own
  * in-memory class, `ClassNode`:
  *
  * {{{
  * public final class UplcProgram implements runtime.Dispatch {
  *   static final ProgramInfo INFO;   // the flat program, embedded and re-decoded at class init
  *   static final Object[] K;         // its constants, by node id
  *   static final UplcProgram INSTANCE;
  *   public Object run();                               // the program
  *   private static Object m<i>(Object[] env, Object…); // one per lambda chain / delay
  *   public Object call0/1/2/N(int id, Object[] env, …) // closure dispatch: switch on id
  * }
  * }}}
  *
  *   - Closure conversion. Every materialised lambda chain / `delay` becomes a static method plus a
  *     capture array holding exactly its free variables, in [[SourceProgram.captures]] order — the
  *     order the runtime reads back to discharge closures.
  *   - Representation. Typed values stay unboxed in locals and on the stack and are boxed only
  *     where they meet dynamic code. A saturated builtin with a direct implementation
  *     ([[Semantics.direct]]) is a plain call on its unboxed arguments.
  *   - Method size. Once a method has inlined enough code, the next `let` body moves to a method of
  *     its own: the JVM caps a method at 64 KB of bytecode.
  */
object Lower:

  val ClassName = "dev/constructive/eo/demo/uplc/generated/UplcProgram"

  def lower(program: TypedProgram): ClassNode = new Lowering(program.source).run(program.body)

  /** The script a class embeds: the string constants its static initialiser assembles. */
  def script(node: ClassNode): CborScript =
    val init = node.methods.asScala.filter(_.name == "<clinit>")
    CborScript.fromChunks(init.flatMap(_.instructions.asScala).collect {
      case ldc: LdcInsnNode if ldc.cst.isInstanceOf[String] => ldc.cst.toString
    })

  // Descriptors and internal names, made once: ASM hashes every string it is handed.
  private[uplc] val Obj = Rep.Obj.asm
  private[uplc] val Objs = Type.getType(classOf[Array[AnyRef]])
  private[uplc] val Info = Type.getType(classOf[ProgramInfo])
  private[uplc] val Builtins = Type.getType(Semantics.Builtins.getClass)

  private[uplc] val (self, objs, objName, info) =
    (s"L$ClassName;", Objs.getDescriptor, Obj.getInternalName, Info.getInternalName)

  private[uplc] val (builtins, builtinsModule) = (Builtins.getInternalName, Builtins.getDescriptor)
  private[uplc] val Rt = "dev/constructive/eo/demo/uplc/runtime/Rt"

  /** `Rt`'s entry points, name ⇒ descriptor (each name is unique). */
  private[uplc] val rt = java.util.HashMap[String, String]()
  Class.forName(Rt.replace('/', '.')).getMethods.foreach(m => rt.put(m.getName, desc(m)))

  private def desc(m: java.lang.reflect.Method) = Type.getMethodDescriptor(m)

  /** A builtin's direct implementation (see [[Semantics.direct]]), as generated code calls it. */
  final case class Direct(owner: String, name: String, desc: String, args: List[Rep], result: Rep)

  private[uplc] val direct: IArray[Option[Direct]] = IArray.from(DefaultFun.values.map { f =>
    Semantics.direct(f).map { m =>
      val reps = m.getParameterTypes.toList.map(Rep.of(_).get)
      val owner = Type.getInternalName(m.getDeclaringClass)
      Direct(owner, m.getName, desc(m), reps, Rep.of(m.getReturnType).get)
    }
  })

  /** `m<id>(Object[] env, Object × arity)`. */
  private[uplc] val descriptors = IArray.tabulate(Typer.MaxChain + 1)(n =>
    Type.getMethodDescriptor(Obj, (Objs +: Seq.fill(n)(Obj))*)
  )

/** Where a variable lives in the method being written. */
private enum Loc:
  case Slot(slot: Int, rep: Rep)
  case Env(index: Int)

  /** A pure value (a builtin, maybe forced): computed where it is read, never if it is not. */
  case Pure(value: Expr)

  /** Bound in the program but not captured by this method — never referenced from it. */
  case Absent

/** One method being written: its next free local slot, and how many expression nodes it has inlined
  * so far. Every representation fits one slot.
  */
final private class Gen(mv: MethodVisitor, var next: Int) extends InstructionAdapter(ASM9, mv):
  var size: Int = 0

  /** Pops the top of the stack into a fresh local. */
  def local(t: Type): Int =
    store(next, t)
    next += 1
    next - 1

final private class Lowering(source: SourceProgram):

  import Lower.*

  /** Expression nodes one method may inline before the next `let` body moves to a method. */
  private val OutlineAfter = 1500

  private val cls = new ClassNode
  private val closures = mutable.ArrayBuffer.empty[(Int, Int)] // (method id, arity; 0 = delay)
  private val lambdaMethods = mutable.HashMap.empty[Int, Int] // origin -> method id

  def run(program: Expr): ClassNode =
    val dispatch = Array(Type.getInternalName(classOf[Dispatch]))
    cls.visit(V17, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, ClassName, null, objName, dispatch)
    for (name, desc) <- List("INFO" -> Info.getDescriptor, "K" -> objs, "INSTANCE" -> self) do
      cls.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, name, desc, null, null)
    method(ACC_PUBLIC, "<init>", "()V") { g ?=>
      g.visitVarInsn(ALOAD, 0)
      g.invokespecial(objName, "<init>", "()V", false)
    }
    method(ACC_STATIC, "<clinit>", "()V")(staticInit)
    method(ACC_PUBLIC, "run", Type.getMethodDescriptor(Obj))(genAs(program, Rep.Obj, Vector.empty))
    for (name, arity) <- List("call0" -> 0, "call1" -> 1, "call2" -> 2, "callN" -> -1) do
      val targets = closures.filter((_, a) => if arity < 0 then a > 0 else a == arity).toMap
      val extra = if arity < 0 then List(Objs) else List.fill(arity)(Obj)
      method(ACC_PUBLIC, name, Type.getMethodDescriptor(Obj, Type.INT_TYPE :: Objs :: extra*)) {
        g ?=>
          // callK(int id, Object[] env, …): a switch from method id to closure body.
          g.load(1, Type.INT_TYPE)
          switch(targets.keys.toVector.sorted) { id =>
            g.load(2, Objs)
            if arity >= 0 then (1 to arity).foreach(i => g.load(2 + i, Obj))
            else (0 until targets(id)).foreach(element(g.load(3, Objs), _))
            invokeBody(id, targets(id))
          }
      }
    cls

  /** Writes a method whose first free local slot is `next`. */
  private def method(access: Int, name: String, desc: String, next: Int = 1)(body: Gen ?=> Unit) =
    val g = Gen(cls.visitMethod(access, name, desc, null, null), next)
    g.visitCode()
    body(using g)
    g.visitInsn(if desc.endsWith("V") then RETURN else ARETURN)
    g.visitMaxs(0, 0)
    g.visitEnd()

  /** Loads the embedded script and its constants, and creates `INSTANCE`. */
  private def staticInit(using g: Gen): Unit =
    objects(source.script.chunks)(g.aconst(_))
    g.invokestatic(info, "fromChunks", Type.getMethodDescriptor(Info, Objs), false)
    g.dup()
    g.putstatic(ClassName, "INFO", Info.getDescriptor)
    g.invokevirtual(info, "constants", Type.getMethodDescriptor(Objs), false)
    g.putstatic(ClassName, "K", objs)
    g.visitTypeInsn(NEW, ClassName)
    g.dup()
    g.invokespecial(ClassName, "<init>", "()V", false)
    g.putstatic(ClassName, "INSTANCE", self)

  private def element(array: => Unit, i: Int)(using g: Gen): Unit =
    array
    g.iconst(i)
    g.visitInsn(AALOAD)

  /** An `Object[]` of `items`, evaluated in order. */
  private def objects[A](items: Iterable[A])(item: A => Unit)(using g: Gen): Unit =
    g.iconst(items.size)
    g.visitTypeInsn(ANEWARRAY, objName)
    items.iterator.zipWithIndex.foreach { (a, i) =>
      g.dup()
      g.iconst(i)
      item(a)
      g.visitInsn(AASTORE)
    }

  private def call(name: String)(using g: Gen): Unit = g.invokestatic(Rt, name, rt.get(name), false)

  /** A runtime call whose result is a boxed value. */
  private def obj(name: String)(using Gen): Rep =
    call(name)
    Rep.Obj

  private def invokeBody(id: Int, arity: Int)(using g: Gen): Unit =
    g.invokestatic(ClassName, cls.methods.get(id).name, descriptors(arity), false)

  private def fail(reason: String)(using g: Gen): Rep =
    g.aconst(reason)
    call("fail")
    g.athrow()
    Rep.Obj

  /** Converts between representations. Mismatched scalars go through a box, so an ill-typed program
    * fails at run time, as on the CEK machine.
    */
  private def convert(from: Rep, to: Rep)(using Gen): Unit =
    if from != to then
      if from != Rep.Obj then call(from.box)
      if to != Rep.Obj then call(to.un)

  private def load(scope: Vector[Loc], index: Int)(using g: Gen): Rep =
    scope(scope.size - index) match
      case Loc.Slot(slot, rep) =>
        g.load(slot, rep.asm)
        rep
      case Loc.Env(k) =>
        element(g.visitVarInsn(ALOAD, 0), k)
        Rep.Obj
      case Loc.Pure(value) => gen(value, Vector.empty)
      // Captures are the free variables of the source term, and every variable a nested closure
      // needs is free in its parent too — so this is unreachable.
      case Loc.Absent => throw new IllegalStateException(s"variable $index is not captured")

  private def values(es: Seq[Expr], scope: Vector[Loc])(using Gen): Unit =
    objects(es)(genAs(_, Rep.Obj, scope))

  /** The capture array for a closure rooted at `origin`. */
  private def env(origin: Int, scope: Vector[Loc])(using Gen): Unit =
    val captures = source.captures(origin)
    if captures.isEmpty then call("noCaptures")
    else objects(captures.toSeq)(j => convert(load(scope, j), Rep.Obj))

  /** Writes the body method of a closure rooted at `origin` with `arity` parameters (a `delay` has
    * none), registered with the dispatchers when it is a `closure`, and returns its id. The body
    * sees exactly the captured variables, then the parameters.
    */
  private def outline(origin: Int, arity: Int, e: Expr, scope: Vector[Loc], closure: Boolean): Int =
    val inner =
      source.captures(origin).zipWithIndex.foldLeft(Vector.fill[Loc](scope.size)(Loc.Absent)) {
        case (acc, (j, k)) => acc.updated(scope.size - j, Loc.Env(k))
      } ++ (1 to arity).map(Loc.Slot(_, Rep.Obj))
    val id = cls.methods.size
    if closure then
      closures += ((id, arity))
      lambdaMethods(origin) = id // before the body: a recursive body calls itself directly
    method(ACC_PRIVATE | ACC_STATIC, s"m$id", descriptors(arity), arity + 1)(
      genAs(e, Rep.Obj, inner)
    )
    id

  private def closure(id: Int, origin: Int, arity: Int, scope: Vector[Loc])(using g: Gen): Rep =
    g.getstatic(ClassName, "INSTANCE", self)
    g.iconst(id)
    g.iconst(origin)
    g.iconst(arity)
    env(origin, scope)
    obj("closure")

  /** Emits `e` in representation `rep`. */
  private def genAs(e: Expr, rep: Rep, scope: Vector[Loc])(using g: Gen): Unit = e match
    case Expr.Let(origin, bindings, body, _) if g.size > OutlineAfter =>
      // The body becomes the body method of the let's own lambda, called directly.
      env(origin, scope)
      bindings.foreach(b => genAs(b.rhs, Rep.Obj, scope))
      invokeBody(outline(origin, bindings.size, body, scope, closure = false), bindings.size)
      convert(Rep.Obj, rep)
    case Expr.Let(_, bindings, body, _) =>
      // Right-hand sides see the outer scope only, and run in order.
      val bound = bindings.map { b =>
        if pure(b.rhs, 0) then Loc.Pure(b.rhs)
        else
          val r = Rep.of(b.rhs.ty)
          genAs(b.rhs, r, scope)
          Loc.Slot(g.local(r.asm), r)
      }
      genAs(body, rep, scope ++ bound)

    case Expr.Choose(DefaultFun.IfThenElse, cond, List(t, f), _)
        if cond.ty.contains(DefaultUni.Bool) =>
      genAs(cond, Rep.Bool, scope)
      branch(genAs(t, rep, scope), genAs(f, rep, scope))
    case Expr.Choose(fun, scrut, branches, _) =>
      g.iconst(fun.ordinal)
      genAs(scrut, Rep.Obj, scope)
      g.iconst(branches.size)
      call("chooseIndex")
      switch(branches.indices)(i => genAs(branches(i), rep, scope))

    case Expr.Match(scrut, branches, _) if scalar(scrut, DefaultUni.Bool) =>
      genAs(scrut, Rep.Bool, scope)
      branches match
        case List(f, t) => branch(genAs(t, rep, scope), genAs(f, rep, scope))
        case List(f)    =>
          branch(fail("case on True needs a second branch"): Unit, genAs(f, rep, scope))
        case _ => fail("case on a boolean needs 1 or 2 branches"): Unit
    case Expr.Match(scrut, branches, _) if scalar(scrut, DefaultUni.Integer) =>
      genAs(scrut, Rep.Int, scope)
      g.iconst(branches.size)
      call("integerCaseIndex")
      switch(branches.indices)(i => genAs(branches(i), rep, scope))

    case _ => convert(gen(e, scope), rep)

  /** A builtin forced no more often than it takes type arguments: nothing to evaluate or fail. */
  @tailrec
  private def pure(e: Expr, forces: Int): Boolean = e match
    case Expr.Force(inner)  => pure(inner, forces + 1)
    case Expr.BuiltinRef(f) => forces <= Semantics.scheme(f).numTypeVars
    case _                  => false

  /** PV11 casing on a known scalar compiles to a JVM branch or switch. */
  private def scalar(scrut: Expr, uni: DefaultUni): Boolean =
    Semantics.caseOnBuiltins && scrut.ty.contains(uni)

  /** An `if` on the boolean on the stack. */
  private def branch(whenTrue: => Unit, whenFalse: => Unit)(using g: Gen): Unit =
    val (otherwise, end) = (new Label, new Label)
    g.ifeq(otherwise)
    whenTrue
    g.goTo(end)
    g.mark(otherwise)
    whenFalse
    g.mark(end)

  /** A switch on the int on the stack, one arm per key (ascending). The runtime only ever produces
    * one of the keys, so the last arm doubles as the default; with no keys it never returns.
    */
  private def switch(keys: Seq[Int])(arm: Int => Unit)(using g: Gen): Unit =
    val (labels, end) = (keys.map(_ => new Label).toArray, new Label)
    if keys.isEmpty then
      g.pop()
      fail("no branches")
    else g.lookupswitch(labels.last, keys.toArray, labels)
    keys.lazyZip(labels).foreach { (k, l) =>
      g.mark(l)
      arm(k)
      g.goTo(end)
    }
    g.mark(end)

  /** Emits `e` in its natural representation and returns it. */
  private def gen(e: Expr, scope: Vector[Loc])(using g: Gen): Rep =
    g.size += 1
    e match
      case Expr.Ref(i, _)  => load(scope, i)
      case Expr.Unbound(i) => fail(s"open term: variable $i is not bound")
      case Expr.Fail       => fail("error")

      case Expr.Lit(_, Constant.Bool(b)) =>
        g.iconst(if b then 1 else 0)
        Rep.Bool
      case Expr.Lit(node, _) =>
        // Constants live boxed in the class's table, and are unboxed where a typed use reads them.
        element(g.getstatic(ClassName, "K", objs), node)
        Rep.Obj

      case Expr.Lambda(origin, params, body) =>
        val id = outline(origin, params.size, body, scope, closure = true)
        closure(id, origin, params.size, scope)
      case Expr.Delayed(origin, body) =>
        closure(outline(origin, 0, body, scope, closure = true), origin, 0, scope)

      case Expr.Apply(fn, arg) =>
        genAs(fn, Rep.Obj, scope)
        genAs(arg, Rep.Obj, scope)
        obj("apply")
      case Expr.Force(body) =>
        genAs(body, Rep.Obj, scope)
        obj("force")
      case Expr.CallKnown(fn, origin, args) =>
        convert(load(scope, fn.index), Rep.Obj)
        call("envOf")
        args.foreach(genAs(_, Rep.Obj, scope))
        invokeBody(lambdaMethods(origin), args.size)
        Rep.Obj

      case Expr.BuiltinRef(fun) =>
        g.iconst(fun.ordinal)
        obj("builtin")
      case Expr.Call(fun, args, _) =>
        direct(fun.ordinal) match
          case Some(d) =>
            // Outside `Rt`, a direct implementation is a method of Scalus' `Builtins` object.
            if d.owner != Rt then g.getstatic(builtins, "MODULE$", builtinsModule)
            args.lazyZip(d.args).foreach(genAs(_, _, scope))
            if d.owner == Rt then g.invokestatic(Rt, d.name, d.desc, false)
            else g.invokevirtual(d.owner, d.name, d.desc, false)
            d.result
          case None =>
            g.iconst(fun.ordinal)
            values(args, scope)
            obj("call")

      case Expr.Construct(tag, args) =>
        g.lconst(tag)
        values(args, scope)
        obj("construct")

      case Expr.Match(scrut, branches, _)
          if !scalar(scrut, DefaultUni.Bool) && !scalar(scrut, DefaultUni.Integer) =>
        caseOf(scrut, branches, scope)
      case _ =>
        val rep = Rep.of(e.ty)
        genAs(e, rep, scope)
        rep

  /** A dynamic `case`: the runtime picks the branch and hands it the scrutinee's fields. A branch
    * that is a lambda chain of the right arity is called directly.
    */
  private def caseOf(scrut: Expr, branches: List[Expr], scope: Vector[Loc])(using g: Gen): Rep =
    genAs(scrut, Rep.Obj, scope)
    g.dup()
    call("caseFields")
    val fields = g.local(Objs)
    g.iconst(branches.size)
    call("caseIndex")
    switch(branches.indices) { i =>
      branches(i) match
        case Expr.Lambda(origin, params, lambdaBody) =>
          val arity = params.size
          val id = outline(origin, arity, lambdaBody, scope, closure = true)
          val (generic, end) = (new Label, new Label)
          g.load(fields, Objs)
          g.arraylength()
          g.iconst(arity)
          g.ificmpne(generic)
          env(origin, scope)
          (0 until arity).foreach(element(g.load(fields, Objs), _))
          invokeBody(id, arity)
          g.goTo(end)
          g.mark(generic)
          closure(id, origin, arity, scope)
          g.load(fields, Objs)
          call("applyAll")
          g.mark(end)
        case other =>
          genAs(other, Rep.Obj, scope)
          g.load(fields, Objs)
          call("applyAll")
    }
    Rep.Obj
