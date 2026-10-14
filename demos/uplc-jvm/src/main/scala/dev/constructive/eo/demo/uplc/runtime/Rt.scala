package dev.constructive.eo.demo.uplc.runtime

import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq

import dev.constructive.eo.demo.uplc.Semantics
import java.util.Arrays
import scalus.cardano.onchain.plutus.prelude.List as PList
import scalus.uplc.builtin.{ByteString, Data}
import scalus.uplc.eval.{CekValue, Logger, MachineError, NoLogger}
import scalus.uplc.{Constant, DefaultFun, DefaultUni, Term, TypeScheme}

/** The static entry points generated code calls (`Rt.…`).
  *
  * An evaluation fails by throwing: a `MachineError` from here, whatever Scalus' builtins throw, a
  * `ClassCastException` where an ill-typed program meets a cast. It is the JVM's zero-cost way out
  * of every frame at once: a result that could be a failure would need a check after every call.
  * `Exec.run` is the one place it is caught and becomes a value — any exception escaping an
  * evaluation is its failure, as on the CEK machine. A compiler bug throws an `AssertionError`,
  * which is not.
  */
object Rt:

  /** The trace log of the evaluation running on the current thread. */
  val logs: ThreadLocal[Logger] = ThreadLocal.withInitial(() => NoLogger)

  private val Empty: Array[AnyRef] = Array.empty
  private val True: AnyRef = Constant.Bool(true)
  private val False: AnyRef = Constant.Bool(false)

  /** Stackless: the trace would cost more than evaluating a validator (~1.3 µs), and the failure is
    * an outcome of the program, not a fault of the JVM code.
    */
  def fail(reason: String): Throwable = new MachineError(reason):
    override def fillInStackTrace(): Throwable = this

  def noCaptures(): Array[AnyRef] = Empty

  // ---- representations: `box<Rep>` and `un<Rep>` for each unboxed `Rep` ------------------------

  def boxInt(v: BigInt): AnyRef = Constant.Integer(v)
  def boxBool(b: Boolean): AnyRef = if b then True else False
  def boxBytes(b: ByteString): AnyRef = Constant.ByteString(b)
  def boxStr(s: String): AnyRef = Constant.String(s)
  def boxData(d: Data): AnyRef = Constant.Data(d)
  def unInt(v: AnyRef): BigInt = v.asInstanceOf[Constant.Integer].value
  def unBool(v: AnyRef): Boolean = v.asInstanceOf[Constant.Bool].value
  def unBytes(v: AnyRef): ByteString = v.asInstanceOf[Constant.ByteString].value
  def unStr(v: AnyRef): String = v.asInstanceOf[Constant.String].value
  def unData(v: AnyRef): Data = v.asInstanceOf[Constant.Data].value

  // ---- list and pair builtins: the CEK meanings, unwrapped, and lazy over `Data` -----------------

  def headList(l: AnyRef): AnyRef = l match
    case DataList(PList.Cons(head, _)) => Constant.Data(head)
    case _                             => l.asInstanceOf[Constant.List].value.head

  def tailList(l: AnyRef): AnyRef = l match
    case DataList(PList.Cons(_, tail)) => DataList(tail)
    case c: Constant.List              => c.copy(value = c.value.tail)

  def nullList(l: AnyRef): Boolean = l match
    case DataList(elems) => elems.isEmpty
    case _               => l.asInstanceOf[Constant.List].value.isEmpty

  def dropList(n: BigInt, l: AnyRef): AnyRef = l match
    case DataList(elems)  => DataList(elems.drop(n))
    case c: Constant.List => c.copy(value = c.value.drop(n.max(0).min(Int.MaxValue).toInt))

  def fstPair(p: AnyRef): AnyRef = p match
    case ConstrPair(tag, _) => Constant.Integer(tag)
    case _                  => p.asInstanceOf[Constant.Pair].a

  def sndPair(p: AnyRef): AnyRef = p match
    case ConstrPair(_, fields) => DataList(fields)
    case _                     => p.asInstanceOf[Constant.Pair].b

  def unConstrData(d: Data): AnyRef =
    val c = d.asInstanceOf[Data.Constr]
    ConstrPair(c.constr, c.args)

  def unListData(d: Data): AnyRef = DataList(d.asInstanceOf[Data.List].values)

  /** A runtime value as the Scalus constant it stands for. */
  def constant(v: AnyRef): AnyRef = v match
    case DataList(elems) => Constant.List(DefaultUni.Data, elems.toScalaList.map(Constant.Data(_)))
    case ConstrPair(tag, args) =>
      Constant.Pair(Constant.Integer(tag), constant(DataList(args)).asInstanceOf[Constant])
    case other => other

  // ---- closures ----------------------------------------------------------------------------------

  /** A lambda chain of `arity` parameters, or a `delay` when there are none. */
  def closure(code: Dispatch, id: Int, origin: Int, arity: Int, env: Array[AnyRef]): AnyRef =
    new Lam(code, id, origin, arity, env, Empty)

  /** The environment of a `let`-bound closure, for a direct call of its body method. */
  def envOf(closure: AnyRef): Array[AnyRef] = closure.asInstanceOf[Lam].env

  def apply(f: AnyRef, a: AnyRef): AnyRef = f match
    case l: Lam if l.arity == 1 => l.code.call1(l.id, l.env, a)
    case _                      => applyAll(f, Array(a))

  private def enter(l: Lam, args: Array[AnyRef]): AnyRef = l.arity match
    case 1 => l.code.call1(l.id, l.env, args(0))
    case 2 => l.code.call2(l.id, l.env, args(0), args(1))
    case _ => l.code.callN(l.id, l.env, args)

  /** `f a1 … an`. A lambda chain takes as many arguments as it still needs in one call — partial
    * applications in between would have no observable effect — and the result takes the rest.
    */
  def applyAll(f: AnyRef, args: Array[AnyRef]): AnyRef = f match
    case l: Lam if l.applied.length == 0 && l.arity == args.length && l.arity > 0 => enter(l, args)
    case _ => applyAll(f, args, 0)

  @tailrec
  private def applyAll(f: AnyRef, args: Array[AnyRef], from: Int): AnyRef =
    if from == args.length then f
    else
      f match
        case l: Lam if l.arity > 0 =>
          val had = l.applied.length
          val take = math.min(l.arity - had, args.length - from)
          val all = Arrays.copyOf(l.applied, had + take)
          System.arraycopy(args, from, all, had, take)
          val next =
            if had + take < l.arity then new Lam(l.code, l.id, l.origin, l.arity, l.env, all)
            else enter(l, all)
          applyAll(next, args, from + take)
        case p: Partial => applyAll(feed(p, Some(args(from))), args, from + 1)
        case other      => throw fail(s"cannot apply ${other.getClass.getSimpleName}")

  def force(v: AnyRef): AnyRef = v match
    case t: Lam if t.arity == 0 => t.code.call0(t.id, t.env)
    case p: Partial             => feed(p, None)
    case other                  => throw fail(s"cannot force ${other.getClass.getSimpleName}")

  // ---- builtins as values, and builtins without a direct implementation --------------------------

  def builtin(ordinal: Int): AnyRef =
    val fun = DefaultFun.fromOrdinal(ordinal)
    new Partial(fun, Semantics.scheme(fun), Vector.empty, Vector.empty)

  /** One more force (`None`) or argument (`Some`) for a partial builtin; runs it once saturated. */
  private def feed(p: Partial, step: Option[AnyRef]): AnyRef =
    val next = (p.scheme, step) match
      case (TypeScheme.All(_, rest), None)      => rest
      case (TypeScheme.Arrow(_, rest), Some(_)) => rest
      case _ => throw fail(s"${p.fun}: a force and an argument in the wrong order")
    val args = step.fold(p.args)(p.args :+ _)
    if next.numTypeVars + next.arity > 0 then new Partial(p.fun, next, args, p.trail :+ step)
    else call(p.fun.ordinal, args.toArray)

  /** A builtin by Scalus' CEK meaning, on CEK values. A value that is not a constant goes in as a
    * stand-in of its own: a meaning rejects it as it would the closure it replaces, or — a selector
    * — returns it, and it is swapped back.
    */
  def call(ordinal: Int, args: Array[AnyRef]): AnyRef =
    val in = args.map[CekValue](constant(_) match
      case c: Constant => CekValue.VCon(c)
      case _           => CekValue.VDelay(Term.Error(), ArraySeq.empty))
    Semantics
      .runtime(DefaultFun.fromOrdinal(ordinal))
      .f(logs.get, ArraySeq.unsafeWrapArray(in)) match
      case CekValue.VCon(c) => c
      case other            => args(in.indexWhere(_ eq other))

  /** Which of its `n` delayed branches a selector builtin picks: its CEK meaning, on their indices.
    */
  def chooseIndex(ordinal: Int, scrut: AnyRef, n: Int): Int =
    unInt(call(ordinal, scrut +: Array.tabulate[AnyRef](n)(i => Constant.Integer(i)))).toInt

  // ---- constructors and case ---------------------------------------------------------------------

  def construct(tag: Long, fields: Array[AnyRef]): AnyRef = new ConstrV(tag, fields)

  private def noBranch(what: String, branches: Int): Throwable =
    fail(s"case: no branch for $what among $branches")

  /** The branch a `case` takes — the CEK machine's `FrameCases` rules, including PV11 casing on
    * builtin values.
    */
  def caseIndex(scrut: AnyRef, n: Int): Int = scrut match
    case c: ConstrV =>
      // An unsigned Word64 tag above Int.MaxValue can index no branch list.
      if java.lang.Long.compareUnsigned(c.tag, n.toLong) >= 0 then
        throw noBranch(s"constructor ${java.lang.Long.toUnsignedString(c.tag)}", n)
      else c.tag.toInt
    case _ if !Semantics.caseOnBuiltins       => throw fail("case on a builtin value before PV11")
    case Constant.Integer(i)                  => integerCaseIndex(i, n)
    case Constant.Bool(b) if n == 1 || n == 2 =>
      if b && n == 1 then throw noBranch("True", n) else if b then 1 else 0
    case Constant.Unit if n == 1                            => 0
    case _: Constant.Pair | _: ConstrPair if n == 1         => 0
    case _: Constant.List | _: DataList if n == 1 || n == 2 =>
      if !nullList(scrut) then 0 else if n == 2 then 1 else throw noBranch("the empty list", n)
    case Constant.Data(Data.Constr(tag, _)) if Semantics.caseOnData && tag >= 0 && tag < n =>
      tag.toInt
    case other => throw fail(s"case on ${other.getClass.getSimpleName} with $n branches")

  /** The values a `case` branch is applied to, in order. */
  def caseFields(scrut: AnyRef): Array[AnyRef] = scrut match
    case c: ConstrV                                         => c.fields
    case _: Constant.List | _: DataList if !nullList(scrut) =>
      Array(headList(scrut), tailList(scrut))
    case _: Constant.Pair | _: ConstrPair => Array(fstPair(scrut), sndPair(scrut))
    case Constant.Data(Data.Constr(_, fields)) if Semantics.caseOnData => Array(DataList(fields))
    case _                                                             => Empty

  def integerCaseIndex(i: BigInt, n: Int): Int =
    if i >= 0 && i < n then i.toInt else throw noBranch(s"integer $i", n)
