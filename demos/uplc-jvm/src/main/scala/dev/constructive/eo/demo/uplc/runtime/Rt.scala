package dev.constructive.eo.demo.uplc.runtime

import scala.annotation.tailrec

import dev.constructive.eo.demo.uplc.Semantics
import scalus.uplc.builtin.{Builtins as B, ByteString, Data}
import scalus.uplc.eval.{Logger, NoLogger}
import scalus.uplc.{Constant, DefaultFun, DefaultUni}

/** The static entry points generated code calls (`INVOKESTATIC Rt.…`).
  *
  * Names are unique — the emitter resolves each one's descriptor by name off this class. Anything
  * that can fail as a matter of UPLC semantics throws [[UplcFailure]]; anything else that throws is
  * a bug in the compiler and is left to surface as one.
  */
object Rt:

  private val Empty: Array[AnyRef] = Array.empty
  private val True: AnyRef = Constant.Bool(true)
  private val False: AnyRef = Constant.Bool(false)

  /** The trace log of the evaluation running on the current thread. */
  val logs: ThreadLocal[Logger] = ThreadLocal.withInitial(() => NoLogger)

  def loadProgram(chunks: Array[String]): ProgramInfo = ProgramInfo.fromChunks(chunks)

  def fail(reason: String): RuntimeException = new UplcFailure(reason)

  def badDispatch(id: Int): RuntimeException =
    new IllegalStateException(s"generated code reached an impossible dispatch target ($id)")

  private[runtime] def describe(v: AnyRef): String = v match
    case c: Constant => s"constant of type ${c.tpe}"
    case _: Lam      => "a lambda"
    case _: Thunk    => "a delayed term"
    case p: Partial  => s"a partially applied ${p.fun}"
    case c: ConstrV  => s"constructor ${c.tag}"
    case other       => other.getClass.getName

  private def mismatch(expected: String, v: AnyRef): UplcFailure =
    new UplcFailure(s"expected $expected, got ${describe(v)}")

  // ---- representation changes -------------------------------------------------------------

  def boxInteger(v: BigInt): AnyRef = Constant.Integer(v)
  def boxBool(b: Boolean): AnyRef = if b then True else False
  def boxBytes(b: ByteString): AnyRef = Constant.ByteString(b)
  def boxString(s: String): AnyRef = Constant.String(s)

  def unInteger(v: AnyRef): BigInt = v match
    case Constant.Integer(i) => i
    case _                   => throw mismatch("an integer", v)

  def unBool(v: AnyRef): Boolean = v match
    case Constant.Bool(b) => b
    case _                => throw mismatch("a boolean", v)

  def unBytes(v: AnyRef): ByteString = v match
    case Constant.ByteString(b) => b
    case _                      => throw mismatch("a bytestring", v)

  def unString(v: AnyRef): String = v match
    case Constant.String(s) => s
    case _                  => throw mismatch("a string", v)

  // ---- typed fast paths: the very functions Scalus' CardanoBuiltins wraps -------------------

  private def nonZero(fun: String, d: BigInt): BigInt =
    if d == 0 then throw new UplcFailure(s"$fun: division by zero") else d

  def addInteger(a: BigInt, b: BigInt): BigInt = B.addInteger(a, b)
  def subtractInteger(a: BigInt, b: BigInt): BigInt = B.subtractInteger(a, b)
  def multiplyInteger(a: BigInt, b: BigInt): BigInt = B.multiplyInteger(a, b)
  def divideInteger(a: BigInt, b: BigInt): BigInt = B.divideInteger(a, nonZero("divideInteger", b))

  def quotientInteger(a: BigInt, b: BigInt): BigInt =
    B.quotientInteger(a, nonZero("quotientInteger", b))

  def remainderInteger(a: BigInt, b: BigInt): BigInt =
    B.remainderInteger(a, nonZero("remainderInteger", b))

  def modInteger(a: BigInt, b: BigInt): BigInt = B.modInteger(a, nonZero("modInteger", b))
  def equalsInteger(a: BigInt, b: BigInt): Boolean = B.equalsInteger(a, b)
  def lessThanInteger(a: BigInt, b: BigInt): Boolean = B.lessThanInteger(a, b)
  def lessThanEqualsInteger(a: BigInt, b: BigInt): Boolean = B.lessThanEqualsInteger(a, b)
  def appendByteString(a: ByteString, b: ByteString): ByteString = B.appendByteString(a, b)
  def equalsByteString(a: ByteString, b: ByteString): Boolean = B.equalsByteString(a, b)
  def lessThanByteString(a: ByteString, b: ByteString): Boolean = B.lessThanByteString(a, b)

  def lessThanEqualsByteString(a: ByteString, b: ByteString): Boolean =
    B.lessThanEqualsByteString(a, b)

  def lengthOfByteString(b: ByteString): BigInt = B.lengthOfByteString(b)

  def indexByteString(b: ByteString, i: BigInt): BigInt =
    if i < 0 || i >= b.size then
      throw new UplcFailure(s"indexByteString: index $i out of bounds for length ${b.size}")
    else B.indexByteString(b, i)

  def appendString(a: String, b: String): String = B.appendString(a, b)
  def equalsString(a: String, b: String): Boolean = B.equalsString(a, b)

  // ---- closures -----------------------------------------------------------------------------

  def noCaptures(): Array[AnyRef] = Empty

  def lambda(code: Dispatch, id: Int, origin: Int, arity: Int, env: Array[AnyRef]): AnyRef =
    new Lam(code, id, origin, arity, env, Empty)

  def thunk(code: Dispatch, id: Int, origin: Int, env: Array[AnyRef]): AnyRef =
    new Thunk(code, id, origin, env)

  /** The environment of a `let`-bound closure, for a direct call of its body method. */
  def envOf(closure: AnyRef): Array[AnyRef] = closure.asInstanceOf[Lam].env

  def apply(f: AnyRef, a: AnyRef): AnyRef = f match
    case l: Lam =>
      if l.applied.length + 1 < l.arity then
        new Lam(l.code, l.id, l.origin, l.arity, l.env, l.applied :+ a)
      else
        l.arity match
          case 1 => l.code.call1(l.id, l.env, a)
          case 2 => l.code.call2(l.id, l.env, l.applied(0), a)
          case _ => l.code.callN(l.id, l.env, l.applied :+ a)
    case p: Partial => BuiltinCalls.feed(p, Some(a))
    case other      => throw new UplcFailure(s"cannot apply ${describe(other)}")

  /** `f a1 … an`. A lambda chain takes as many arguments as it still needs in one call — partial
    * applications in between would have no observable effect — and the result takes the rest.
    */
  def applyAll(f: AnyRef, args: Array[AnyRef]): AnyRef =
    @tailrec
    def go(acc: AnyRef, from: Int): AnyRef =
      if from == args.length then acc
      else
        acc match
          case l: Lam =>
            val needed = l.arity - l.applied.length
            val available = args.length - from
            if available < needed then
              new Lam(
                l.code,
                l.id,
                l.origin,
                l.arity,
                l.env,
                l.applied ++ args.slice(from, args.length)
              )
            else
              val full =
                if l.applied.isEmpty && from == 0 && needed == args.length then args
                else l.applied ++ args.slice(from, from + needed)
              val result = l.arity match
                case 1 => l.code.call1(l.id, l.env, full(0))
                case 2 => l.code.call2(l.id, l.env, full(0), full(1))
                case _ => l.code.callN(l.id, l.env, full)
              go(result, from + needed)
          case other => go(apply(other, args(from)), from + 1)
    go(f, 0)

  /** `case (constr k a1 … an) [… f …]` with an unknown `f`: the fields were evaluated first. */
  def spread(args: Array[AnyRef], f: AnyRef): AnyRef = applyAll(f, args)

  def force(v: AnyRef): AnyRef = v match
    case t: Thunk   => t.code.call0(t.id, t.env)
    case p: Partial => BuiltinCalls.feed(p, None)
    case other      => throw new UplcFailure(s"cannot force ${describe(other)}")

  // ---- builtins -----------------------------------------------------------------------------

  def builtin(ordinal: Int): AnyRef = BuiltinCalls.start(DefaultFun.fromOrdinal(ordinal))

  def call(ordinal: Int, args: Array[AnyRef]): AnyRef =
    BuiltinCalls.run(DefaultFun.fromOrdinal(ordinal), args)

  /** Which delayed branch a selector builtin picks. */
  def chooseIndex(ordinal: Int, scrut: AnyRef): Int = DefaultFun.fromOrdinal(ordinal) match
    case DefaultFun.IfThenElse => if unBool(scrut) then 0 else 1
    case DefaultFun.ChooseList =>
      scrut match
        case Constant.List(_, xs) => if xs.isEmpty then 0 else 1
        case _                    => throw mismatch("a list", scrut)
    case DefaultFun.ChooseUnit =>
      scrut match
        case Constant.Unit => 0
        case _             => throw mismatch("unit", scrut)
    case DefaultFun.ChooseData =>
      scrut match
        case Constant.Data(d) => BuiltinCalls.dataIndex(d)
        case _                => throw mismatch("data", scrut)
    case other => throw new IllegalStateException(s"$other is not a selector")

  // ---- constructors and case ----------------------------------------------------------------

  def construct(tag: Long, fields: Array[AnyRef]): AnyRef = new ConstrV(tag, fields)

  private def noBranch(what: String, branches: Int): UplcFailure =
    new UplcFailure(s"case: no branch for $what among $branches")

  /** The branch a `case` takes — the CEK machine's `FrameCases` rules, including PV11 casing on
    * builtin values.
    */
  def caseIndex(scrut: AnyRef, branches: Int): Int = scrut match
    case c: ConstrV =>
      // An unsigned Word64 tag above Int.MaxValue can index no branch list.
      if java.lang.Long.compareUnsigned(c.tag, Int.MaxValue.toLong) > 0 || c.tag >= branches then
        throw noBranch(s"constructor ${java.lang.Long.toUnsignedString(c.tag)}", branches)
      else c.tag.toInt
    case c: Constant if Semantics.caseOnBuiltins => constantCaseIndex(c, branches)
    case other => throw new UplcFailure(s"case on ${describe(other)}")

  private def constantCaseIndex(c: Constant, n: Int): Int = c match
    case Constant.Integer(i) => integerCaseIndex(i, n)
    case Constant.Bool(b)    =>
      val index = if b then 1 else 0
      if n == 0 || n > 2 || index >= n then throw noBranch(s"boolean $b", n) else index
    case Constant.Unit =>
      if n == 1 then 0 else throw noBranch("unit", n)
    case Constant.List(_, xs) =>
      if n == 0 || n > 2 then throw noBranch("a list", n)
      else if xs.nonEmpty then 0
      else if n < 2 then throw noBranch("the empty list", n)
      else 1
    case Constant.Pair(_, _) =>
      if n == 1 then 0 else throw noBranch("a pair", n)
    case Constant.Data(d) if Semantics.caseOnData =>
      d match
        case Data.Constr(tag, _) if tag >= 0 && tag < n => tag.toInt
        case _                                          => throw noBranch("data", n)
    case other => throw new UplcFailure(s"case on constant of type ${other.tpe}")

  /** The values a `case` branch is applied to, in order. */
  def caseFields(scrut: AnyRef): Array[AnyRef] = scrut match
    case c: ConstrV                => c.fields
    case Constant.List(t, x :: xs) => Array(x, Constant.List(t, xs))
    case Constant.Pair(a, b)       => Array(a, b)
    case Constant.Data(Data.Constr(_, fields)) if Semantics.caseOnData =>
      Array(Constant.List(DefaultUni.Data, fields.toScalaList.map(Constant.Data(_))))
    case _ => Empty

  def integerCaseIndex(i: BigInt, n: Int): Int =
    if i >= 0 && i < n then i.toInt else throw noBranch(s"integer $i", n)
