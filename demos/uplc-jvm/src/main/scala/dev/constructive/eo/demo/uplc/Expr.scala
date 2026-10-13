package dev.constructive.eo.demo.uplc

import scalus.uplc.{Constant, DefaultFun, DefaultUni, TypeScheme}

/** A static type reconstructed for an untyped UPLC term — the "hybrid" in hybrid typing: the Plutus
  * builtin type the typer could prove locally (constants, saturated builtins with a monomorphic
  * result, `let`-bound values of those, conditionals whose branches agree), or `None` for
  * everything else (functions, constructors, values flowing through lambda parameters). Integers,
  * booleans, bytestrings and strings get an unboxed JVM representation; every other value lives in
  * the uniform boxed one.
  */
type Ty = Option[DefaultUni]

object Ty:

  def of(t: TypeScheme): Ty = t match
    case TypeScheme.Type(uni) => Some(uni)
    case _                    => None

  /** What the branches of a conditional agree on. A branch that can only fail does not widen its
    * siblings.
    */
  def join(branches: List[Expr]): Ty =
    branches.filterNot(_.fails).map(_.ty).distinct match
      case List(t) => t
      case _       => None

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

  /** The constant at UPLC node `node`. */
  case Lit(node: Int, value: Constant)

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
    case Lit(_, c)          => Some(c.tpe)
    case _                  => None

  /** Can only fail: `error`, an unbound variable, or a `let` whose body is one of those. */
  def fails: Boolean = this match
    case Fail | Unbound(_)  => true
    case Let(_, _, body, _) => body.fails
    case _                  => false

  /** Renders the expression as Scala-like source — for humans; nothing parses it back. */
  def show: String = Expr.block(this, 0, 0)

object Expr:

  private def scalaType(ty: Ty): String = ty match
    case Some(DefaultUni.Integer)    => "BigInt"
    case Some(DefaultUni.ByteString) => "ByteString"
    case Some(DefaultUni.String)     => "String"
    case Some(DefaultUni.Bool)       => "Boolean"
    case _                           => "Value"

  private def pad(indent: Int): String = "  " * indent

  private def name(depth: Int, index: Int): String = s"v${depth - index}"

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
    case BuiltinRef(fun) => s"builtin.${Semantics.name(fun)}"
    case Fail            => "error()"

    case Lambda(_, params, body) =>
      val names = params.indices.map(k => s"v${depth + k}: Value").mkString(", ")
      s"($names) =>\n${pad(indent + 1)}${block(body, depth + params.size, indent + 1)}"

    case Delayed(_, body)     => s"Lazy(${render(body, depth, indent)})"
    case Apply(fn, arg)       => s"${render(fn, depth, indent)}(${render(arg, depth, indent)})"
    case Spread(as, fn)       => s"${render(fn, depth, indent)}(${args(as, depth, indent)})"
    case CallKnown(fn, _, as) => s"${render(fn, depth, indent)}(${args(as, depth, indent)})"
    case Force(body)          => s"${render(body, depth, indent)}.force"
    case Call(fun, as, _)     => s"${Semantics.name(fun)}(${args(as, depth, indent)})"
    case Construct(tag, as)   => s"Constr$tag(${args(as, depth, indent)})"
    case Choose(DefaultFun.IfThenElse, cond, List(t, f), _) => ifThenElse(cond, t, f, depth, indent)
    case Match(cond, List(f, t), _) if cond.ty.contains(DefaultUni.Bool) =>
      ifThenElse(cond, t, f, depth, indent)
    case Choose(fun, scrut, branches, _) =>
      s"${Semantics.name(fun)}(${render(scrut, depth, indent)})(${args(branches, depth, indent + 1)})"
    case Match(scrut, branches, _) =>
      val arms = branches.zipWithIndex.map {
        case (b, k) =>
          s"${pad(indent + 1)}case $k => ${block(b, depth, indent + 2)}"
      }
      s"${render(scrut, depth, indent)} match\n${arms.mkString("\n")}"
