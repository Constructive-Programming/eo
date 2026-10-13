package dev.constructive.eo.demo.uplc

import scala.collection.mutable

import java.util.IdentityHashMap
import scalus.uplc.{DeBruijnedProgram, Term}

/** A decoded flat UPLC program — Scalus' own `Term` tree — with a PRE-ORDER numbering of its nodes.
  *
  * The numbering is the contract between compile time and run time. The compiler stamps node ids
  * into the typed expression (A) and from there into the generated class; the runtime re-decodes
  * the same flat bytes (they are embedded in the class) and gets the same numbering back. A closure
  * therefore only has to carry an `Int` to be turned back into its source term — which is what
  * discharging a result (and comparing it with the CEK machine's) needs. Constants are looked up by
  * node id too.
  *
  * Every node gets an id: the node itself first, then its children left to right (`LamAbs` body;
  * `Apply` function then argument; `Constr` arguments; `Case` scrutinee then branches). A lambda
  * chain `λx.λy.b` therefore occupies consecutive ids — the inner lambda of a chain rooted at `o`
  * is `o + 1`, which partial applications rely on. `Term` compares structurally, so [[id]] goes by
  * object identity: the decoder builds every node afresh.
  */
final class SourceProgram private (
    val flat: Array[Byte],
    val nodes: IArray[Term],
    val depths: IArray[Int],
    ids: IdentityHashMap[Term, Integer],
):

  def root: Term = nodes(0)

  /** The pre-order id of a node of this program. */
  def id(node: Term): Int = ids.get(node).nn

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

object SourceProgram:

  /** Decodes flat bytes. Throws whatever Scalus' decoder throws on malformed input — callers that
    * cannot vouch for their bytes go through [[UplcJvm.compile]], the one place that converts it.
    */
  def decode(flat: Array[Byte]): SourceProgram =
    val nodes = mutable.ArrayBuffer.empty[Term]
    val depths = mutable.ArrayBuffer.empty[Int]
    val ids = new IdentityHashMap[Term, Integer]

    def number(term: Term, depth: Int): Unit =
      ids.put(term, nodes.length)
      nodes += term
      depths += depth
      term match
        case Term.LamAbs(_, body, _) => number(body, depth + 1)
        case Term.Apply(fn, arg, _)  =>
          number(fn, depth)
          number(arg, depth)
        case Term.Force(body, _)        => number(body, depth)
        case Term.Delay(body, _)        => number(body, depth)
        case Term.Constr(_, args, _)    => args.foreach(number(_, depth))
        case Term.Case(scrut, cases, _) =>
          number(scrut, depth)
          cases.foreach(number(_, depth))
        case _: Term.Var | _: Term.Const | _: Term.Builtin | _: Term.Error => ()

    number(DeBruijnedProgram.fromFlatEncoded(flat).term, 0)
    new SourceProgram(flat.clone(), IArray.from(nodes), IArray.from(depths), ids)

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
