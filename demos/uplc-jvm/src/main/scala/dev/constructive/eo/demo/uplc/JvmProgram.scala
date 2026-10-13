package dev.constructive.eo.demo.uplc

import scalus.uplc.DefaultFun

/** JVM representation of a value on the operand stack or in a local.
  *
  * `Obj` is the uniform boxed representation every UPLC value can take: a Scalus `Constant` for
  * constants, a runtime closure / thunk / partial builtin / constructor object otherwise. The
  * others are the unboxed representations of the typer's scalar types.
  */
enum Rep(val descriptor: String):
  case Obj extends Rep("Ljava/lang/Object;")
  case Int extends Rep("Lscala/math/BigInt;")
  case Bool extends Rep("Z")
  case Bytes extends Rep("Lscalus/uplc/builtin/ByteString;")
  case Str extends Rep("Ljava/lang/String;")

object Rep:

  def of(ty: Ty): Rep = ty match
    case Ty.Integer         => Rep.Int
    case Ty.Bool            => Rep.Bool
    case Ty.ByteString      => Rep.Bytes
    case Ty.String          => Rep.Str
    case Ty.Dyn | Ty.Bottom => Rep.Obj

/** A typed fast path: a builtin compiled to a direct static call on unboxed operands. */
final case class PrimOp(method: String, args: List[Rep], result: Rep)

object PrimOp:

  private def int2(m: String, r: Rep) = PrimOp(m, List(Rep.Int, Rep.Int), r)
  private def bytes2(m: String, r: Rep) = PrimOp(m, List(Rep.Bytes, Rep.Bytes), r)

  val table: Map[DefaultFun, PrimOp] = Map(
    DefaultFun.AddInteger -> int2("addInteger", Rep.Int),
    DefaultFun.SubtractInteger -> int2("subtractInteger", Rep.Int),
    DefaultFun.MultiplyInteger -> int2("multiplyInteger", Rep.Int),
    DefaultFun.DivideInteger -> int2("divideInteger", Rep.Int),
    DefaultFun.QuotientInteger -> int2("quotientInteger", Rep.Int),
    DefaultFun.RemainderInteger -> int2("remainderInteger", Rep.Int),
    DefaultFun.ModInteger -> int2("modInteger", Rep.Int),
    DefaultFun.EqualsInteger -> int2("equalsInteger", Rep.Bool),
    DefaultFun.LessThanInteger -> int2("lessThanInteger", Rep.Bool),
    DefaultFun.LessThanEqualsInteger -> int2("lessThanEqualsInteger", Rep.Bool),
    DefaultFun.AppendByteString -> bytes2("appendByteString", Rep.Bytes),
    DefaultFun.EqualsByteString -> bytes2("equalsByteString", Rep.Bool),
    DefaultFun.LessThanByteString -> bytes2("lessThanByteString", Rep.Bool),
    DefaultFun.LessThanEqualsByteString -> bytes2("lessThanEqualsByteString", Rep.Bool),
    DefaultFun.LengthOfByteString -> PrimOp("lengthOfByteString", List(Rep.Bytes), Rep.Int),
    DefaultFun.IndexByteString -> PrimOp("indexByteString", List(Rep.Bytes, Rep.Int), Rep.Int),
    DefaultFun.AppendString -> PrimOp("appendString", List(Rep.Str, Rep.Str), Rep.Str),
    DefaultFun.EqualsString -> PrimOp("equalsString", List(Rep.Str, Rep.Str), Rep.Bool),
  )

/** B — the lowered program, shaped for the JVM: one static method per closure body, closures
  * converted (each captures exactly its free variables, in [[SourceProgram.captures]] order),
  * locals allocated to slots, every boxing / unboxing explicit. `methods(0)` is the entry point.
  */
final case class JvmProgram(source: SourceProgram, methods: Vector[JvmMethod]):

  /** Renders every method as pseudo-code: slots, captures, boxing and fast paths spelled out. */
  def show: String = methods.map(JvmProgram.method).mkString("\n")

object JvmProgram:

  private def method(m: JvmMethod): String =
    val header = m.kind match
      case MethodKind.Entry                     => "entry():"
      case MethodKind.LambdaBody(origin, arity) =>
        val params = (1 to arity).map(p => s"s$p").mkString(", ")
        s"m${m.id}(env, $params):   // lambda chain at UPLC node $origin"
      case MethodKind.ThunkBody(origin) => s"m${m.id}(env):   // delay at UPLC node $origin"
    s"$header\n  ${block(m.body, 1)}"

  private def pad(indent: Int): String = "  " * indent

  private def rep(r: Rep): String = r match
    case Rep.Obj   => "Value"
    case Rep.Int   => "BigInt"
    case Rep.Bool  => "Boolean"
    case Rep.Bytes => "ByteString"
    case Rep.Str   => "String"

  private def list(cs: Vector[Code], indent: Int): String = cs.map(expr(_, indent)).mkString(", ")

  /** A statement position: bindings render as one line each. */
  private def block(c: Code, indent: Int): String = c match
    case Code.Bind(slot, rhs, body) =>
      s"s$slot: ${rep(rhs.rep)} = ${expr(rhs, indent)}\n${pad(indent)}${block(body, indent)}"
    case other => expr(other, indent)

  private def expr(c: Code, indent: Int): String = c match
    case Code.Local(slot, _)      => s"s$slot"
    case Code.Captured(i)         => s"env[$i]"
    case Code.Literal(i, Rep.Obj) => s"K[$i]"
    case Code.Literal(i, r)       => s"KU[$i]: ${rep(r)}"
    case Code.BoolLit(b)          => b.toString
    case Code.Box(inner, _)       => s"box(${expr(inner, indent)})"
    case Code.Unbox(inner, to)    => s"unbox[${rep(to)}](${expr(inner, indent)})"
    case bind: Code.Bind => s"{\n${pad(indent + 1)}${block(bind, indent + 1)}\n${pad(indent)}}"
    case Code.If(cond, t, f, _) =>
      s"if ${expr(cond, indent)} then ${block(t, indent + 1)}\n${pad(indent)}else ${block(f, indent + 1)}"
    case Code.IntSwitch(scrut, cases, _) =>
      val arms =
        cases.zipWithIndex.map((b, k) => s"${pad(indent + 1)}case $k => ${block(b, indent + 2)}")
      s"switch ${expr(scrut, indent)}\n${arms.mkString("\n")}"
    case Code.MakeLambda(m, _, arity, caps) =>
      s"closure(m$m, arity $arity, env [${list(caps, indent)}])"
    case Code.MakeThunk(m, _, caps) => s"thunk(m$m, env [${list(caps, indent)}])"
    case Code.Apply(f, a)           => s"apply(${expr(f, indent)}, ${expr(a, indent)})"
    case Code.Spread(args, f)       => s"applyAll(${expr(f, indent)}, [${list(args, indent)}])"
    case Code.Force(inner)          => s"force(${expr(inner, indent)})"
    case Code.Invoke(m, _, closure, args) =>
      s"m$m(envOf(${expr(closure, indent)}), ${list(args, indent)})"
    case Code.CallBody(m, _, caps, args) => s"m$m([${list(caps, indent)}], ${list(args, indent)})"
    case Code.Prim(op, args)             => s"${op.method}(${list(args, indent)})"
    case Code.Delegate(fun, args)        => s"scalus.${fun.toString}(${list(args, indent)})"
    case Code.BuiltinValue(fun)          => s"builtin(${fun.toString})"
    case Code.Choose(fun, scrut, branches, _) =>
      val arms =
        branches.zipWithIndex.map((b, k) => s"${pad(indent + 1)}case $k => ${block(b, indent + 2)}")
      s"choose ${fun.toString}(${expr(scrut, indent)})\n${arms.mkString("\n")}"
    case Code.Construct(tag, args)      => s"constr $tag(${list(args, indent)})"
    case Code.CaseOf(scrut, arms, _, _) =>
      val shown = arms.zipWithIndex.map {
        case (Arm.Value(v), k) =>
          s"${pad(indent + 1)}case $k => applyAll(${expr(v, indent + 2)}, fields)"
        case (Arm.Direct(l), k) =>
          s"${pad(indent + 1)}case $k => m${l.method}(env [${list(l.captures, indent + 2)}], fields…)"
      }
      s"case ${expr(scrut, indent)}\n${shown.mkString("\n")}"
    case Code.Fail(reason, _) => s"fail(\"$reason\")"

enum MethodKind:

  /** `static Object run()` — evaluates the whole program. */
  case Entry

  /** `static Object lam(Object[] env, Object p1, …, Object pn)` — body of a lambda chain. */
  case LambdaBody(origin: Int, arity: Int)

  /** `static Object thunk(Object[] env)` — body of a `delay`. */
  case ThunkBody(origin: Int)

final case class JvmMethod(id: Int, kind: MethodKind, body: Code):

  def descriptor: String = kind match
    case MethodKind.Entry                => "()Ljava/lang/Object;"
    case MethodKind.LambdaBody(_, arity) =>
      "([Ljava/lang/Object;" + "Ljava/lang/Object;" * arity + ")Ljava/lang/Object;"
    case MethodKind.ThunkBody(_) => "([Ljava/lang/Object;)Ljava/lang/Object;"

/** How a dynamic `case` hands its fields to a branch. */
enum Arm:

  /** Evaluate the branch to a value, then apply it to the fields one by one. */
  case Value(code: Code)

  /** The branch is a lambda chain: when the field count matches its arity, call its body method
    * directly; otherwise build the closure and apply it like any other value.
    */
  case Direct(closure: Code.MakeLambda)

/** One node of lowered code. Each knows the representation it leaves on the stack. */
enum Code:
  case Local(slot: Int, slotRep: Rep)

  /** `env[index]` of the enclosing closure body. */
  case Captured(index: Int)

  /** Constant number `index` of the program, boxed (`Obj`) or unboxed. */
  case Literal(index: Int, literalRep: Rep)

  case BoolLit(value: Boolean)
  case Box(code: Code, from: Rep)
  case Unbox(code: Code, to: Rep)

  /** Evaluate `rhs` into `slot`, then evaluate `body`. */
  case Bind(slot: Int, rhs: Code, body: Code)

  case If(cond: Code, whenTrue: Code, whenFalse: Code, resultRep: Rep)

  /** `case` on an unboxed integer: branch `i` for scrutinee `i`, failure outside the branches. */
  case IntSwitch(scrutinee: Code, cases: Vector[Code], resultRep: Rep)

  case MakeLambda(method: Int, origin: Int, arity: Int, captures: Vector[Code])
  case MakeThunk(method: Int, origin: Int, captures: Vector[Code])
  case Apply(fn: Code, arg: Code)

  /** Evaluate `args` into an array, then `fn`, then apply `fn` to them all. */
  case Spread(args: Vector[Code], fn: Code)
  case Force(code: Code)

  /** Direct call of a known lambda chain's body method, reusing the environment of the closure
    * value `closure` evaluates to.
    */
  case Invoke(method: Int, arity: Int, closure: Code, args: Vector[Code])

  /** Direct call of an outlined `let` body: `method(env = captures, args…)`. */
  case CallBody(method: Int, arity: Int, captures: Vector[Code], args: Vector[Code])

  case Prim(op: PrimOp, args: Vector[Code])

  /** A saturated builtin call through the runtime (native selector or Scalus delegation). */
  case Delegate(fun: DefaultFun, args: Vector[Code])

  case BuiltinValue(fun: DefaultFun)

  /** A selector builtin over delayed branches: the runtime picks the branch index. */
  case Choose(fun: DefaultFun, scrutinee: Code, branches: Vector[Code], resultRep: Rep)

  case Construct(tag: Long, args: Vector[Code])

  /** Dynamic `case`: `scrutSlot` holds the scrutinee, `fieldsSlot` its fields. */
  case CaseOf(scrutinee: Code, arms: Vector[Arm], scrutSlot: Int, fieldsSlot: Int)

  /** Evaluation failure (UPLC `error`, or a scrutinee no branch can take). */
  case Fail(reason: String, resultRep: Rep)

  def rep: Rep = this match
    case Local(_, r)        => r
    case Literal(_, r)      => r
    case BoolLit(_)         => Rep.Bool
    case Unbox(_, r)        => r
    case Bind(_, _, body)   => body.rep
    case If(_, _, _, r)     => r
    case IntSwitch(_, _, r) => r
    case Prim(op, _)        => op.result
    case Choose(_, _, _, r) => r
    case Fail(_, r)         => r
    case Captured(_) | Box(_, _) | MakeLambda(_, _, _, _) | MakeThunk(_, _, _) | Apply(_, _) |
        Spread(_, _) | Force(_) | Invoke(_, _, _, _) | CallBody(_, _, _, _) | Delegate(_, _) |
        BuiltinValue(_) | Construct(_, _) | CaseOf(_, _, _, _) =>
      Rep.Obj
