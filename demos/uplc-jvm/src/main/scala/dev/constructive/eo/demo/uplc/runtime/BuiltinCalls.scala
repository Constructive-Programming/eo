package dev.constructive.eo.demo.uplc.runtime

import scala.collection.immutable.ArraySeq
import scala.util.control.NonFatal

import dev.constructive.eo.demo.uplc.Semantics
import scalus.uplc.builtin.Data
import scalus.uplc.eval.{CekValue, MachineError}
import scalus.uplc.{Constant, DefaultFun, Term, TypeScheme}

/** Saturated builtin calls: native for the five selectors, delegated to Scalus for the rest. */
private[runtime] object BuiltinCalls:

  /** Stands in for a non-constant argument handed to a delegated builtin. Only the selectors take
    * arbitrary values and those run natively, so every delegated builtin rejects this stand-in
    * exactly as it would reject the closure or constructor it replaces.
    */
  private val Opaque: CekValue = CekValue.VDelay(Term.Error(), ArraySeq.empty)

  def start(fun: DefaultFun): AnyRef =
    new Partial(fun, Semantics.runtime(fun).typeScheme, Vector.empty, Vector.empty)

  /** One more force (`None`) or argument (`Some`) for a partial builtin. */
  def feed(p: Partial, step: Option[AnyRef]): AnyRef =
    val next = (p.scheme, step) match
      case (TypeScheme.All(_, rest), None)      => rest
      case (TypeScheme.Arrow(_, rest), Some(_)) => rest
      case (_, None) => throw new MachineError(s"${p.fun}: forced where an argument was expected")
      case (_, Some(_)) => throw new MachineError(s"${p.fun}: argument where a force was expected")
    val args = step.fold(p.args)(p.args :+ _)
    if next.numTypeVars + next.arity == 0 then run(p.fun, args.toArray)
    else new Partial(p.fun, next, args, p.trail :+ step)

  def dataIndex(d: Data): Int = d match
    case Data.Constr(_, _) => 0
    case Data.Map(_)       => 1
    case Data.List(_)      => 2
    case Data.I(_)         => 3
    case Data.B(_)         => 4

  def run(fun: DefaultFun, args: Array[AnyRef]): AnyRef = fun match
    case DefaultFun.IfThenElse => if Rt.unBool(args(0)) then args(1) else args(2)
    case DefaultFun.ChooseUnit =>
      args(0) match
        case Constant.Unit => args(1)
        case other         => throw new MachineError(s"chooseUnit: got ${Rt.describe(other)}")
    case DefaultFun.Trace =>
      Rt.logs.get.log(Rt.unStr(args(0)))
      args(1)
    case DefaultFun.ChooseList =>
      args(0) match
        case Constant.List(_, xs) => if xs.isEmpty then args(1) else args(2)
        case other => throw new MachineError(s"chooseList: got ${Rt.describe(other)}")
    case DefaultFun.ChooseData =>
      args(0) match
        case Constant.Data(d) => args(1 + dataIndex(d))
        case other            => throw new MachineError(s"chooseData: got ${Rt.describe(other)}")
    case _ => delegate(fun, args)

  /** Scalus' builtin implementations are a throwing API: an argument of the wrong type, a malformed
    * encoding, a failed precondition all surface as exceptions. This is the single place they are
    * converted into the evaluation failure the CEK machine itself reports, a `MachineError`.
    */
  private def delegate(fun: DefaultFun, args: Array[AnyRef]): AnyRef =
    val in = ArraySeq.unsafeWrapArray(args.map[CekValue] {
      case c: Constant => CekValue.VCon(c)
      case _           => Opaque
    })
    val out =
      try Semantics.runtime(fun).f(Rt.logs.get, in)
      catch case NonFatal(e) => throw new MachineError(s"$fun: ${e.getMessage}")
    out match
      case CekValue.VCon(c) => c
      case other => throw new IllegalStateException(s"$fun returned a non-constant: $other")
