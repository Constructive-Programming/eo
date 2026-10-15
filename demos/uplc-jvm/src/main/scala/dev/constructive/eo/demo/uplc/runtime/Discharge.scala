package dev.constructive.eo.demo.uplc.runtime

import scalus.cardano.ledger.Word64
import scalus.uplc.{Constant, Term}

/** Turns a runtime value back into a (de Bruijn) UPLC term — the CEK machine's `dischargeCekValue`,
  * so results of the two evaluators can be compared with `α_==`.
  *
  * A closure becomes its source node with every captured variable replaced by its discharged value.
  * The source node comes from the program the class embeds; the capture layout comes from
  * [[dev.constructive.eo.demo.uplc.SourceProgram.captures]], the same function the compiler used to
  * build the capture array.
  */
object Discharge:

  def apply(value: AnyRef, info: ProgramInfo): Term = value match
    case c: Constant                              => Term.Const(c)
    case lazyData @ (_: DataList | _: ConstrPair) => apply(Rt.constant(lazyData), info)
    case c: ConstrV => Term.Constr(Word64(c.tag), c.fields.iterator.map(apply(_, info)).toList)
    case l: Lam     =>
      // A partially applied chain is its inner lambda, the first `k` parameters bound to the
      // values applied so far (innermost = the last one applied); a `delay` is its own node.
      val k = l.applied.length
      val captures = info.source.captures(l.origin)
      substitute(
        info.source.nodes(l.origin + k),
        j =>
          if j <= k then Some(apply(l.applied(k - j), info))
          else Some(captures.indexOf(j - k)).filter(_ >= 0).map(i => apply(l.env(i), info)),
      )
    case p: Partial =>
      p.trail.foldLeft[Term](Term.Builtin(p.fun)) {
        case (acc, None)    => Term.Force(acc)
        case (acc, Some(a)) => Term.Apply(acc, apply(a, info))
      }
    case other => throw new IllegalStateException(s"not a UPLC value: $other")

  /** Replaces the variables free in `term` that `lookup` resolves; the rest stay (open terms). */
  private def substitute(term: Term, lookup: Int => Option[Term]): Term =
    def go(binders: Int, t: Term): Term = t match
      case Term.Var(name, _) if name.index > binders =>
        lookup(name.index - binders).getOrElse(t)
      case Term.LamAbs(n, body, a)   => Term.LamAbs(n, go(binders + 1, body), a)
      case Term.Apply(f, x, a)       => Term.Apply(go(binders, f), go(binders, x), a)
      case Term.Force(b, a)          => Term.Force(go(binders, b), a)
      case Term.Delay(b, a)          => Term.Delay(go(binders, b), a)
      case Term.Constr(tag, args, a) => Term.Constr(tag, args.map(go(binders, _)), a)
      case Term.Case(s, cases, a)    => Term.Case(go(binders, s), cases.map(go(binders, _)), a)
      case other                     => other
    go(0, term)
