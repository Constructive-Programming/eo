package dev.constructive.eo.demo.uplc

import scala.collection.mutable

import scalus.uplc.{Constant, DeBruijnedProgram, DefaultFun, Term}

/** A decoded flat UPLC program together with a PRE-ORDER numbering of its nodes.
  *
  * The numbering is the contract between compile time and run time. The compiler stamps node ids
  * into the typed expression (A) and from there into the generated class; the runtime re-decodes
  * the same flat bytes (they are embedded in the class) and gets the same numbering back. A closure
  * therefore only has to carry an `Int` to be turned back into its source term — which is what
  * discharging a result (and comparing it with the CEK machine's) needs.
  *
  * Every node gets an id: the node itself first, then its children left to right (`LamAbs` body;
  * `Apply` function then argument; `Constr` arguments; `Case` scrutinee then branches). A lambda
  * chain `λx.λy.b` therefore occupies consecutive ids — the inner lambda of a chain rooted at `o`
  * is `o + 1`, which partial applications rely on.
  */
final class SourceProgram private (
    val flat: Array[Byte],
    val version: (Int, Int, Int),
    val root: Node,
    val nodes: IArray[Term],
    val depths: IArray[Int],
    val constants: IArray[Constant],
):

  private val captureCache = mutable.HashMap.empty[Int, IArray[Int]]

  /** The variables a closure created at node `origin` must capture, in capture-array order.
    *
    * These are the free de Bruijn indices of the node — relative to the node's own position — that
    * some enclosing binder actually binds, ascending. Indices past the program's binders are open:
    * they are never captured and fail only if evaluation reaches them. Compile time (which builds
    * the capture array) and run time (which reads it back while discharging) both ask this one
    * function, so the two can never disagree on the layout.
    */
  def captures(origin: Int): IArray[Int] =
    captureCache.synchronized {
      captureCache.getOrElseUpdate(
        origin,
        IArray.from(FreeVars.of(nodes(origin)).filter(_ <= depths(origin))),
      )
    }

/** Mirror of the UPLC term with every node carrying its pre-order id. Typing (A) is computed from
  * this tree: unlike Scalus' `Term`, ids survive the re-ordering that re-sugaring needs (a `let`
  * types its right-hand side before its body, although the body comes first in pre-order).
  */
enum Node:
  case Var(id: Int, index: Int)
  case Lam(id: Int, name: String, body: Node)
  case Apply(id: Int, fn: Node, arg: Node)
  case Force(id: Int, body: Node)
  case Delay(id: Int, body: Node)
  case Const(id: Int, index: Int, value: Constant)
  case Builtin(id: Int, fun: DefaultFun)
  case Error(id: Int)
  case Constr(id: Int, tag: Long, args: List[Node])
  case Case(id: Int, scrutinee: Node, branches: List[Node])

object SourceProgram:

  /** Decodes flat bytes. Throws whatever Scalus' decoder throws on malformed input — callers that
    * cannot vouch for their bytes go through [[UplcJvm.compile]], the one place that converts it.
    */
  def decode(flat: Array[Byte]): SourceProgram =
    val program = DeBruijnedProgram.fromFlatEncoded(flat)
    val nodes = mutable.ArrayBuffer.empty[Term]
    val depths = mutable.ArrayBuffer.empty[Int]
    val constants = mutable.ArrayBuffer.empty[Constant]

    def number(term: Term, depth: Int): Node =
      val id = nodes.length
      nodes += term
      depths += depth
      term match
        case Term.Var(name, _)          => Node.Var(id, name.index)
        case Term.LamAbs(name, body, _) => Node.Lam(id, name, number(body, depth + 1))
        case Term.Apply(fn, arg, _)     =>
          val f = number(fn, depth)
          Node.Apply(id, f, number(arg, depth))
        case Term.Force(body, _)  => Node.Force(id, number(body, depth))
        case Term.Delay(body, _)  => Node.Delay(id, number(body, depth))
        case Term.Const(value, _) =>
          constants += value
          Node.Const(id, constants.length - 1, value)
        case Term.Builtin(fun, _)       => Node.Builtin(id, fun)
        case Term.Error(_)              => Node.Error(id)
        case Term.Constr(tag, args, _)  => Node.Constr(id, tag.value, args.map(number(_, depth)))
        case Term.Case(scrut, cases, _) =>
          val s = number(scrut, depth)
          Node.Case(id, s, cases.map(number(_, depth)))

    val root = number(program.term, 0)
    new SourceProgram(
      flat.clone(),
      program.version,
      root,
      IArray.from(nodes),
      IArray.from(depths),
      IArray.from(constants),
    )

/** Free de Bruijn indices of a term, relative to the term's own position, ascending. */
object FreeVars:

  def of(term: Term): Vector[Int] =
    val acc = mutable.TreeSet.empty[Int]

    def go(t: Term, binders: Int): Unit = t match
      case Term.Var(name, _)    => if name.index > binders then acc += name.index - binders
      case Term.LamAbs(_, b, _) => go(b, binders + 1)
      case Term.Apply(f, a, _)  =>
        go(f, binders)
        go(a, binders)
      case Term.Force(b, _)        => go(b, binders)
      case Term.Delay(b, _)        => go(b, binders)
      case Term.Constr(_, args, _) => args.foreach(go(_, binders))
      case Term.Case(s, cases, _)  =>
        go(s, binders)
        cases.foreach(go(_, binders))
      case _: Term.Const | _: Term.Builtin | _: Term.Error => ()

    go(term, 0)
    acc.toVector
