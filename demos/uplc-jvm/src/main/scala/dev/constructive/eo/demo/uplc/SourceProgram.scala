package dev.constructive.eo.demo.uplc

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.SortedSet

import scalus.uplc.Term

/** A decoded script — Scalus' own `Term` tree — numbered in PRE-ORDER.
  *
  * The numbering is the contract between compile time and run time. The compiler stamps node ids
  * into the typed expression and from there into the generated class; the runtime re-decodes the
  * same script (it is embedded in the class) and gets the same numbering back. A closure therefore
  * only has to carry an `Int` to be turned back into its source term — which is what discharging a
  * result (and comparing it with the CEK machine's) needs. Constants are looked up by node id too.
  *
  * Every node gets an id: the node itself first, then its children left to right (`LamAbs` body;
  * `Apply` function then argument; `Constr` arguments; `Case` scrutinee then branches). A lambda
  * chain `λx.λy.b` therefore occupies consecutive ids — the inner lambda of a chain rooted at `o`
  * is `o + 1`, which partial applications rely on — and a node's next sibling comes right after its
  * subtree.
  */
final class SourceProgram private (
    val script: CborScript,
    val version: (Int, Int, Int),
    val nodes: IArray[Term],
    val depths: IArray[Int],
    sizes: IArray[Int],
):

  /** The ids of node `n`'s children, in order: each starts where the one before it ends, until the
    * end of `n`'s subtree.
    */
  def children(n: Int): List[Int] =
    List.unfold(n + 1)(c => Option.when(c < n + sizes(n))((c, c + sizes(c))))

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
    // The node's subtree is the ids from `origin` on; a variable there is free in the node when its
    // index reaches past the binders between the two.
    IArray.from((origin until origin + sizes(origin)).foldLeft(SortedSet.empty[Int]) { (free, n) =>
      nodes(n) match
        case Term.Var(name, _) =>
          val index = name.index - (depths(n) - depths(origin))
          if index >= 1 && index <= depths(origin) then free + index else free
        case _ => free
    })
  )

object SourceProgram:

  def decode(script: CborScript): SourceProgram =
    // A subtree's nodes, binder depths and subtree sizes in pre-order, ahead of the `count` nodes
    // after it: a right fold over the children.
    type Numbered = (List[Term], List[Int], List[Int], Int)
    val walk = new Walk
    def number(term: Term, depth: Int, rest: Numbered): Step[Numbered] = walk.nested {
      val inner = if term.isInstanceOf[Term.LamAbs] then depth + 1 else depth
      subterms(term)
        .foldRight(Step.Done(rest): Step[Numbered])((t, acc) => acc.flatMap(number(t, inner, _)))
        .map((ns, ds, ss, count) =>
          (term :: ns, depth :: ds, (count - rest._4 + 1) :: ss, count + 1)
        )
    }
    val program = script.program
    val (nodes, depths, sizes, _) = number(program.term, 0, (Nil, Nil, Nil, 0)).value
    new SourceProgram(
      script,
      program.version,
      IArray.from(nodes),
      IArray.from(depths),
      IArray.from(sizes),
    )

  /** A term's immediate subterms, in the order they are numbered. */
  def subterms(term: Term): List[Term] = term match
    case Term.LamAbs(_, body, _)                                       => List(body)
    case Term.Apply(fn, arg, _)                                        => List(fn, arg)
    case Term.Force(body, _)                                           => List(body)
    case Term.Delay(body, _)                                           => List(body)
    case Term.Constr(_, args, _)                                       => args
    case Term.Case(scrut, cases, _)                                    => scrut :: cases
    case _: Term.Var | _: Term.Const | _: Term.Builtin | _: Term.Error => Nil
