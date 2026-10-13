package dev.constructive.eo.demo.uplc

import scalus.uplc.{Constant, DefaultFun, DefaultUni}

/** Static types reconstructed for an untyped UPLC term — the "hybrid" in hybrid typing.
  *
  * The four scalar types are what the typer can prove locally: constants, saturated builtins with a
  * monomorphic result, `let`-bound values of those, and conditionals whose branches agree. They get
  * an unboxed JVM representation. `Dyn` is everything else (functions, constructors, data, lists,
  * values flowing through lambda parameters) and lives in the uniform boxed representation.
  * `Bottom` types `error`, so a branch that can only fail does not widen its sibling.
  */
enum Ty:
  case Integer, ByteString, String, Bool, Dyn, Bottom

  def join(that: Ty): Ty =
    if this == that then this
    else if this == Ty.Bottom then that
    else if that == Ty.Bottom then this
    else Ty.Dyn

object Ty:

  def ofUni(uni: DefaultUni): Ty = uni match
    case DefaultUni.Integer    => Ty.Integer
    case DefaultUni.ByteString => Ty.ByteString
    case DefaultUni.String     => Ty.String
    case DefaultUni.Bool       => Ty.Bool
    case _                     => Ty.Dyn

  def ofConstant(c: Constant): Ty = ofUni(c.tpe)

/** A — the typed, re-sugared program: UPLC read back as a Scala-like expression tree.
  *
  * The typer recognises the idioms UPLC compilers (Plinth, Scalus, Aiken, …) emit for constructs
  * UPLC lacks — `let` as an immediately applied lambda, `if` as a forced `ifThenElse` over delayed
  * branches, direct calls of `let`-bound functions — and records them as such. Variables stay de
  * Bruijn indices; nodes that can become runtime closures keep the pre-order id (`origin`) of the
  * UPLC node they came from, so a closure can be discharged back to source.
  */
final case class TypedProgram(source: SourceProgram, body: Expr)

final case class Binding(name: String, rhs: Expr)

enum Expr:

  /** A variable some enclosing lambda or `let` binds (de Bruijn, 1 = innermost). */
  case Ref(index: Int, binderTy: Ty)

  /** A variable no enclosing binder covers: evaluating it fails, exactly as on the CEK machine. */
  case Unbound(index: Int)

  /** A maximal lambda chain `λp1 … λpn. body`, rooted at UPLC node `origin`. */
  case Lambda(origin: Int, params: List[String], body: Expr)

  /** `delay body`, rooted at UPLC node `origin`. */
  case Delayed(origin: Int, body: Expr)

  /** `(λx1 … λxn. body) a1 … an`: every right-hand side is evaluated in the OUTER scope, in order.
    * `origin` is the UPLC lambda the binders came from (used if the body must move to a method).
    */
  case Let(origin: Int, bindings: List[Binding], body: Expr, resultTy: Ty)

  /** Dynamic application of an arbitrary function value. */
  case Apply(fn: Expr, arg: Expr)

  /** `case (constr k a1 … an) [… fn …]`: evaluate the arguments, then `fn`, then apply. */
  case Spread(args: List[Expr], fn: Expr)

  /** Saturated call of a `let`-bound lambda chain (UPLC node `origin`): no closure dispatch. */
  case CallKnown(fn: Ref, origin: Int, args: List[Expr])

  case Force(body: Expr)
  case Lit(index: Int, value: Constant)

  /** An unsaturated builtin used as a value. */
  case BuiltinRef(fun: DefaultFun)

  /** A builtin applied to exactly its forces and arguments. */
  case Call(fun: DefaultFun, args: List[Expr], resultTy: Ty)

  /** `force (ifThenElse c (delay a) (delay b))` and the other selector builtins over delayed
    * branches: only the selected branch is evaluated.
    */
  case Choose(fun: DefaultFun, scrutinee: Expr, branches: List[Expr], resultTy: Ty)

  case Fail
  case Construct(tag: Long, args: List[Expr])
  case Match(scrutinee: Expr, branches: List[Expr], resultTy: Ty)

  def ty: Ty = this match
    case Ref(_, t)          => t
    case Let(_, _, _, t)    => t
    case Call(_, _, t)      => t
    case Choose(_, _, _, t) => t
    case Match(_, _, t)     => t
    case Lit(_, c)          => Ty.ofConstant(c)
    case Fail | Unbound(_)  => Ty.Bottom
    case Lambda(_, _, _) | Delayed(_, _) | Apply(_, _) | Spread(_, _) | CallKnown(_, _, _) |
        Force(_) | BuiltinRef(_) | Construct(_, _) =>
      Ty.Dyn

  /** Renders the expression as Scala-like source — for humans; nothing parses it back. */
  def show: String = Expr.block(this, 0, 0)

object Expr:

  private def scalaType(ty: Ty): String = ty match
    case Ty.Integer    => "BigInt"
    case Ty.ByteString => "ByteString"
    case Ty.String     => "String"
    case Ty.Bool       => "Boolean"
    case Ty.Dyn        => "Value"
    case Ty.Bottom     => "Nothing"

  private def pad(indent: Int): String = "  " * indent

  private def name(depth: Int, index: Int): String = s"v${depth - index}"

  /** UPLC spells builtins in camelCase (`addInteger`); Scalus' enum cases are capitalised. */
  private def builtin(fun: DefaultFun): String =
    val n = fun.toString
    n.take(1).toLowerCase + n.drop(1)

  private def constant(c: Constant): String = c match
    case Constant.Integer(v)    => v.toString
    case Constant.Bool(b)       => b.toString
    case Constant.String(s)     => "\"" + s + "\""
    case Constant.ByteString(b) => s"hex\"${b.toHex}\""
    case Constant.Unit          => "()"
    case other                  => other.toString

  /** A statement position (top level, a body, a branch): a `let` renders as its statements. */
  private def block(e: Expr, depth: Int, indent: Int): String = e match
    case Let(_, bindings, body, _) =>
      val defs = bindings.zipWithIndex.map {
        case (b, k) =>
          s"val v${depth + k}: ${scalaType(b.rhs.ty)} = ${render(b.rhs, depth, indent)}"
      }
      (defs :+ block(body, depth + bindings.size, indent)).mkString(s"\n${pad(indent)}")
    case other => render(other, depth, indent)

  private def args(es: List[Expr], depth: Int, indent: Int): String =
    es.map(render(_, depth, indent)).mkString(", ")

  private def ifThenElse(cond: Expr, t: Expr, f: Expr, depth: Int, indent: Int): String =
    s"if ${render(cond, depth, indent)} then ${block(t, depth, indent + 1)}\n${pad(indent)}else ${block(f, depth, indent + 1)}"

  /** An expression position: a `let` becomes a `{ … }` block. */
  private def render(e: Expr, depth: Int, indent: Int): String = e match
    case let: Let   => s"{\n${pad(indent + 1)}${block(let, depth, indent + 1)}\n${pad(indent)}}"
    case Ref(i, _)  => name(depth, i)
    case Unbound(i) => s"<unbound $i>"
    case Lit(_, c)  => constant(c)
    case BuiltinRef(fun) => s"builtin.${builtin(fun)}"
    case Fail            => "error()"

    case Lambda(_, params, body) =>
      val names = params.indices.map(k => s"v${depth + k}: Value").mkString(", ")
      s"($names) =>\n${pad(indent + 1)}${block(body, depth + params.size, indent + 1)}"

    case Delayed(_, body)     => s"Lazy(${render(body, depth, indent)})"
    case Apply(fn, arg)       => s"${render(fn, depth, indent)}(${render(arg, depth, indent)})"
    case Spread(as, fn)       => s"${render(fn, depth, indent)}(${args(as, depth, indent)})"
    case CallKnown(fn, _, as) => s"${render(fn, depth, indent)}(${args(as, depth, indent)})"
    case Force(body)          => s"${render(body, depth, indent)}.force"
    case Call(fun, as, _)     => s"${builtin(fun)}(${args(as, depth, indent)})"
    case Construct(tag, as)   => s"Constr$tag(${args(as, depth, indent)})"
    case Choose(DefaultFun.IfThenElse, cond, List(t, f), _) => ifThenElse(cond, t, f, depth, indent)
    case Match(cond, List(f, t), _) if cond.ty == Ty.Bool   => ifThenElse(cond, t, f, depth, indent)
    case Choose(fun, scrut, branches, _)                    =>
      s"${builtin(fun)}(${render(scrut, depth, indent)})(${args(branches, depth, indent + 1)})"
    case Match(scrut, branches, _) =>
      val arms = branches.zipWithIndex.map {
        case (b, k) =>
          s"${pad(indent + 1)}case $k => ${block(b, depth, indent + 2)}"
      }
      s"${render(scrut, depth, indent)} match\n${arms.mkString("\n")}"
