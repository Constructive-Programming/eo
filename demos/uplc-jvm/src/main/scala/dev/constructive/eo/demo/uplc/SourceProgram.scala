package dev.constructive.eo.demo.uplc

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

import scalus.uplc.{DeBruijnedProgram, Term}

/** A decoded script — Scalus' own `Term` tree — numbered in PRE-ORDER.
  *
  * The numbering is the contract between compile time and run time. The compiler stamps node ids
  * into the typed expression (A) and from there into the generated class; the runtime re-decodes
  * the same script (it is embedded in the class) and gets the same numbering back. A closure
  * therefore only has to carry an `Int` to be turned back into its source term — which is what
  * discharging a result (and comparing it with the CEK machine's) needs. Constants are looked up by
  * node id too.
  *
  * Every node gets an id: the node itself first, then its children left to right (`LamAbs` body;
  * `Apply` function then argument; `Constr` arguments; `Case` scrutinee then branches). A lambda
  * chain `λx.λy.b` therefore occupies consecutive ids — the inner lambda of a chain rooted at `o`
  * is `o + 1`, which partial applications rely on — and a node's next sibling comes right after its
  * subtree.
  */
final class SourceProgram private (
    val script: CborScript,
    val nodes: IArray[Term],
    val depths: IArray[Int],
    sizes: IArray[Int],
):

  /** The ids of node `n`'s children, in order. */
  def children(n: Int): List[Int] =
    val count = nodes(n) match
      case _: Term.LamAbs | _: Term.Force | _: Term.Delay => 1
      case _: Term.Apply                                  => 2
      case Term.Constr(_, args, _)                        => args.size
      case Term.Case(_, cases, _)                         => 1 + cases.size
      case _                                              => 0
    List.iterate(n + 1, count)(c => c + sizes(c))

  private val captureCache = TrieMap.empty[Int, IArray[Int]]

  /** The variables a closure created at node `origin` must capture, in capture-array order.
    *
    * These are the free de Bruijn indices of the node — relative to the node's own position — that
    * some enclosing binder actually binds, ascending. Indices past the program's binders are open:
    * they are never captured and fail only if evaluation reaches them. Compile time (which builds
    * the capture array) and run time (which reads it back while discharging) both ask this one
    * function, so the two can never disagree on the layout.
    */
  def captures(origin: Int): IArray[Int] = captureCache.getOrElseUpdate(
    origin,
    IArray.from(FreeVars.of(nodes(origin)).filter(_ <= depths(origin)))
  )

object SourceProgram:

  def decode(script: CborScript): SourceProgram =
    // Every node takes at least its 4-bit tag in the flat encoding: a bound on the node count.
    val bound = script.bytes.length * 2 + 1
    val (nodes, depths, sizes) =
      (new Array[Term](bound), new Array[Int](bound), new Array[Int](bound))

    /** Numbers `term` and its subtree from `id` on; returns the next free id. */
    def number(term: Term, depth: Int, id: Int): Int =
      nodes(id) = term
      depths(id) = depth
      val next = term match
        case Term.LamAbs(_, body, _)    => number(body, depth + 1, id + 1)
        case Term.Apply(fn, arg, _)     => number(arg, depth, number(fn, depth, id + 1))
        case Term.Force(body, _)        => number(body, depth, id + 1)
        case Term.Delay(body, _)        => number(body, depth, id + 1)
        case Term.Constr(_, args, _)    => args.foldLeft(id + 1)((i, a) => number(a, depth, i))
        case Term.Case(scrut, cases, _) =>
          cases.foldLeft(number(scrut, depth, id + 1))((i, c) => number(c, depth, i))
        case _: Term.Var | _: Term.Const | _: Term.Builtin | _: Term.Error => id + 1
      sizes(id) = next - id
      next

    val n = number(DeBruijnedProgram.fromCbor(script.bytes).term, 0, 0)
    def first[A](a: Array[A]) = IArray.unsafeFromArray(a.take(n))
    new SourceProgram(script, first(nodes), first(depths), first(sizes))

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
