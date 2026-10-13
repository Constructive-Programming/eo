package dev.constructive.eo.demo.uplc

import scala.collection.mutable

import scalus.uplc.{Constant, DefaultFun, DefaultUni}

/** A ⇒ B: lowers the typed expression to JVM-shaped code — the `f` in `iso.modify(f)`.
  *
  *   - Closure conversion. Every materialised lambda chain / `delay` becomes a static method plus a
  *     capture array holding exactly its free variables, in [[SourceProgram.captures]] order. The
  *     runtime reads the same order back to discharge closures, so the layout is fixed by the
  *     source, not by this pass.
  *   - Representation selection. Typed values stay unboxed (`BigInt`, `boolean`, `ByteString`,
  *     `String`) in locals and on the stack and are boxed only where they meet dynamic code;
  *     builtins with a typed fast path become direct static calls.
  *   - Local numbering. `let` binders live in numbered locals of their method.
  */
object Lower:

  def lower(program: TypedProgram): JvmProgram = new Lowering(program.source).run(program.body)

  /** Builtins with a typed fast path: those the runtime's `Rt` defines a method for, under the
    * builtin's UPLC name, on the unboxed representations of the builtin's (Scalus) type.
    */
  val fastPaths: Set[DefaultFun] =
    val defined = runtime.Rt.getClass.getMethods.iterator.map(_.getName).toSet
    DefaultFun.values.iterator.filter(f => defined(Semantics.name(f))).toSet

private enum Loc:
  case InLocal(slot: Int, rep: Rep)
  case InEnv(index: Int)

  /** Bound in the program but not captured by this method — never referenced from it. */
  case Absent

/** Local numbering for one method, which also counts the expression nodes inlined into it. */
final private class Frame(private var next: Int):

  var size: Int = 0

  def alloc(): Int =
    val slot = next
    next += 1
    slot

final private class Lowering(source: SourceProgram):

  /** Expression nodes one method may inline before the next `let` body moves to a method of its
    * own. The JVM caps a method at 64 KB of bytecode; long `let` chains — the shape compilers give
    * a script's top-level definitions — would otherwise blow through it.
    */
  private val OutlineAfter = 1500

  private val methods = mutable.ArrayBuffer.empty[Option[JvmMethod]]
  private val lambdaMethods = mutable.HashMap.empty[Int, Int]

  private def reserve(): Int =
    methods += None
    methods.size - 1

  def run(body: Expr): JvmProgram =
    val entry = reserve()
    val code = coerce(expr(body, Vector.empty, new Frame(0)), Rep.Obj)
    methods(entry) = Some(JvmMethod(entry, MethodKind.Entry, code))
    JvmProgram(source, methods.iterator.flatten.toVector)

  private def load(scope: Vector[Loc], index: Int): Code = scope(scope.size - index) match
    case Loc.InLocal(slot, rep) => Code.Local(slot, rep)
    case Loc.InEnv(k)           => Code.Captured(k)
    case Loc.Absent             =>
      // Captures are the free variables of the source term, and every variable a nested
      // closure needs is free in its parent too — so this is unreachable.
      throw new IllegalStateException(s"variable $index is not captured")

  /** Converts between representations. Mismatched scalars go through a box so that an ill-typed
    * program fails at run time with the same evaluation failure the CEK machine reports.
    */
  private def coerce(code: Code, to: Rep): Code =
    if code.rep == to then code
    else
      code match
        case Code.Fail(reason, _)       => Code.Fail(reason, to)
        case Code.If(c, t, f, _)        => Code.If(c, coerce(t, to), coerce(f, to), to)
        case Code.Bind(slot, rhs, body) => Code.Bind(slot, rhs, coerce(body, to))
        case _ if to == Rep.Obj         => Code.Box(code, code.rep)
        case _ if code.rep == Rep.Obj   => Code.Unbox(code, to)
        case _                          => Code.Unbox(Code.Box(code, code.rep), to)

  private def obj(e: Expr, scope: Vector[Loc], frame: Frame): Code =
    coerce(expr(e, scope, frame), Rep.Obj)

  private def as(e: Expr, rep: Rep, scope: Vector[Loc], frame: Frame): Code =
    coerce(expr(e, scope, frame), rep)

  private def expr(e: Expr, scope: Vector[Loc], frame: Frame): Code =
    frame.size += 1
    node(e, scope, frame)

  private def node(e: Expr, scope: Vector[Loc], frame: Frame): Code = e match
    case Expr.Ref(i, _)     => load(scope, i)
    case Expr.Unbound(i)    => Code.Fail(s"open term: variable $i is not bound", Rep.Obj)
    case Expr.Fail          => Code.Fail("error", Rep.Obj)
    case Expr.BuiltinRef(f) => Code.BuiltinValue(f)

    case Expr.Lit(_, Constant.Bool(b)) => Code.BoolLit(b)
    case Expr.Lit(node, value)         => Code.Literal(node, Rep.of(Some(value.tpe)))

    case Expr.Lambda(origin, params, body) => makeLambda(origin, params.size, body, scope)
    case Expr.Delayed(origin, body)        => makeThunk(origin, body, scope)

    case Expr.Let(origin, bindings, body, _) =>
      // Right-hand sides see the outer scope only, and run in order.
      val rhss = bindings.map(b => expr(b.rhs, scope, frame))
      if frame.size > OutlineAfter then
        // The body becomes the body method of the let's own lambda, called directly.
        val (loads, inner) = closureScope(origin, scope)
        val id = reserve()
        val params = Vector.tabulate[Loc](rhss.size)(p => Loc.InLocal(1 + p, Rep.Obj))
        val code = coerce(expr(body, inner ++ params, new Frame(1 + rhss.size)), Rep.Obj)
        methods(id) = Some(JvmMethod(id, MethodKind.LambdaBody(origin, rhss.size), code))
        Code.CallBody(id, loads, rhss.map(coerce(_, Rep.Obj)).toVector)
      else
        val bound = rhss.map(rhs => (frame.alloc(), rhs))
        val inner = scope ++ bound.map((slot, rhs) => Loc.InLocal(slot, rhs.rep))
        bound.foldRight(expr(body, inner, frame)) {
          case ((slot, rhs), acc) =>
            Code.Bind(slot, rhs, acc)
        }

    case Expr.Apply(fn, arg)   => Code.Apply(obj(fn, scope, frame), obj(arg, scope, frame))
    case Expr.Spread(args, fn) =>
      Code.Spread(args.map(obj(_, scope, frame)).toVector, obj(fn, scope, frame))
    case Expr.Force(body) => Code.Force(obj(body, scope, frame))

    case Expr.CallKnown(fn, origin, args) =>
      Code.Invoke(
        lambdaMethods(origin),
        load(scope, fn.index),
        args.map(obj(_, scope, frame)).toVector
      )

    case Expr.Call(fun, args, _) if Lower.fastPaths(fun) =>
      val (argTys, result) = Semantics.signature(fun)
      val ops = args.zip(argTys).map((a, t) => as(a, Rep.of(t), scope, frame))
      Code.Prim(fun, ops.toVector, Rep.of(result))
    case Expr.Call(fun, args, _) => Code.Delegate(fun, args.map(obj(_, scope, frame)).toVector)

    case Expr.Choose(fun, scrut, branches, ty) =>
      val rep = Rep.of(ty)
      val arms = branches.map(as(_, rep, scope, frame)).toVector
      if fun == DefaultFun.IfThenElse && scrut.ty.contains(DefaultUni.Bool) then
        Code.If(as(scrut, Rep.Bool, scope, frame), arms(0), arms(1), rep)
      else Code.Choose(fun, obj(scrut, scope, frame), arms, rep)

    case Expr.Construct(tag, args) => Code.Construct(tag, args.map(obj(_, scope, frame)).toVector)

    case Expr.Match(scrut, branches, ty) =>
      if Semantics.caseOnBuiltins && scrut.ty.contains(DefaultUni.Bool) then
        val rep = Rep.of(ty)
        val cond = as(scrut, Rep.Bool, scope, frame)
        branches.map(as(_, rep, scope, frame)) match
          case List(whenFalse, whenTrue) => Code.If(cond, whenTrue, whenFalse, rep)
          case List(whenFalse)           =>
            Code.If(cond, Code.Fail("case on True needs a second branch", rep), whenFalse, rep)
          case _ =>
            Code.Bind(
              frame.alloc(),
              cond,
              Code.Fail("case on a boolean needs 1 or 2 branches", rep)
            )
      else if Semantics.caseOnBuiltins && scrut.ty.contains(DefaultUni.Integer) then
        val rep = Rep.of(ty)
        Code.IntSwitch(
          as(scrut, Rep.Int, scope, frame),
          branches.map(as(_, rep, scope, frame)).toVector,
          rep,
        )
      else
        val s = obj(scrut, scope, frame)
        val arms = branches.map {
          case Expr.Lambda(origin, params, body) =>
            Arm.Direct(makeLambda(origin, params.size, body, scope))
          case other => Arm.Value(obj(other, scope, frame))
        }
        Code.CaseOf(s, arms.toVector)

  /** The capture loads for a closure rooted at `origin`, and the scope its body sees them in. */
  private def closureScope(origin: Int, scope: Vector[Loc]): (Vector[Code], Vector[Loc]) =
    val captures = source.captures(origin)
    if source.depths(origin) != scope.size then
      throw new IllegalStateException(
        s"node $origin: scope depth ${scope.size} != source depth ${source.depths(origin)}"
      )
    val loads = captures.toVector.map(j => coerce(load(scope, j), Rep.Obj))
    val inner = captures.zipWithIndex.foldLeft(Vector.fill[Loc](scope.size)(Loc.Absent)) {
      case (acc, (j, k)) => acc.updated(scope.size - j, Loc.InEnv(k))
    }
    (loads, inner)

  private def makeLambda(origin: Int, arity: Int, body: Expr, scope: Vector[Loc]): Code.MakeLambda =
    val (loads, inner) = closureScope(origin, scope)
    val id = reserve()
    lambdaMethods(origin) = id
    val params = Vector.tabulate[Loc](arity)(p => Loc.InLocal(1 + p, Rep.Obj))
    val code = coerce(expr(body, inner ++ params, new Frame(1 + arity)), Rep.Obj)
    methods(id) = Some(JvmMethod(id, MethodKind.LambdaBody(origin, arity), code))
    Code.MakeLambda(id, origin, arity, loads)

  private def makeThunk(origin: Int, body: Expr, scope: Vector[Loc]): Code =
    val (loads, inner) = closureScope(origin, scope)
    val id = reserve()
    val code = coerce(expr(body, inner, new Frame(1)), Rep.Obj)
    methods(id) = Some(JvmMethod(id, MethodKind.ThunkBody(origin), code))
    Code.MakeThunk(id, origin, loads)
