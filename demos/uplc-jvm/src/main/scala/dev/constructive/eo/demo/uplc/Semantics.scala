package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec

import java.lang.reflect.Method
import scalus.cardano.ledger.MajorProtocolVersion
import scalus.uplc.builtin.platform
import scalus.uplc.eval.PlutusVM
import scalus.uplc.{BuiltinRuntime, CardanoBuiltins, DefaultFun, DefaultUni, TypeScheme}
import scalus.utils.Utils

/** The single Plutus semantics this demo targets — Plutus V3 at protocol version 11 ("van Rossem"),
  * builtin semantics variant E, the same configuration Scalus 1.3.0 runs the Plutus conformance
  * suite under. The oracle VM and the generated code's runtime both read it from here, so they
  * cannot drift apart.
  */
object Semantics:

  val protocolVersion: MajorProtocolVersion = MajorProtocolVersion.vanRossemPV

  /** The Plutus Core versions a script may declare: those Plutus V3 has at [[protocolVersion]]
    * (`plcVersionsIntroducedIn` in plutus-ledger-api). A version only gates syntax — 1.1.0 adds
    * `constr` and `case`, which Scalus' codec refuses below it — but the ledger rejects a script of
    * any other version when it decodes it, and so does the compiler.
    */
  val versions: Set[(Int, Int, Int)] = Set((1, 0, 0), (1, 1, 0))

  /** The oracle: Scalus' CEK machine, configured for [[protocolVersion]]. */
  lazy val vm: PlutusVM = PlutusVM.makePlutusV3VM(protocolVersion)

  /** PV11 lets `case` scrutinise builtin values (integers, booleans, unit, lists, pairs). */
  val caseOnBuiltins: Boolean = protocolVersion >= MajorProtocolVersion.vanRossemPV

  /** Casing on `Data` arrives one hard fork later (PV12). */
  val caseOnData: Boolean = protocolVersion >= MajorProtocolVersion.dijkstraPV

  private lazy val cardanoBuiltins =
    new CardanoBuiltins(vm.machineParams.builtinCostModel, platform, vm.semanticVariant)

  private lazy val runtimes: IArray[BuiltinRuntime] =
    IArray.from(DefaultFun.values.iterator.map(cardanoBuiltins.getBuiltinRuntime))

  /** Scalus' implementation of a builtin — what generated code delegates to. */
  def runtime(fun: DefaultFun): BuiltinRuntime = runtimes(fun.ordinal)

  /** A builtin's type. Builtins are prenex-polymorphic: `numTypeVars` forces, then `arity`
    * arguments, and the builtin runs when the last of them arrives.
    */
  def scheme(fun: DefaultFun): TypeScheme = runtime(fun).typeScheme

  /** The type of a builtin's saturated result, given what is known of its arguments: matching them
    * against the scheme binds its type variables (`fstPair` of a `pair integer (list data)` is an
    * `integer`).
    */
  def result(fun: DefaultFun, args: List[Ty]): Ty =
    def bind(s: TypeScheme, t: DefaultUni, vars: Map[String, DefaultUni]): Map[String, DefaultUni] =
      (s, t) match
        case (TypeScheme.TVar(a), _)                        => vars.updated(a, t)
        case (TypeScheme.App(f, x), DefaultUni.Apply(g, y)) => bind(x, y, bind(f, g, vars))
        case _                                              => vars
    def subst(s: TypeScheme, vars: Map[String, DefaultUni]): Ty = s match
      case TypeScheme.Type(uni) => Some(uni)
      case TypeScheme.TVar(a)   => vars.get(a)
      case TypeScheme.App(f, x) => subst(f, vars).zip(subst(x, vars)).map(DefaultUni.Apply(_, _))
      case _                    => None
    @tailrec
    def go(s: TypeScheme, args: List[Ty], vars: Map[String, DefaultUni]): Ty = (s, args) match
      case (TypeScheme.All(_, rest), _)         => go(rest, args, vars)
      case (TypeScheme.Arrow(p, rest), a :: as) => go(rest, as, a.fold(vars)(bind(p, _, vars)))
      case _                                    => subst(s, vars)
    go(scheme(fun), args, Map.empty)

  /** Scalus' plain implementations of the builtins — the functions its CEK meanings wrap. */
  val Builtins: scalus.uplc.builtin.Builtins = scalus.uplc.builtin.Builtins

  /** A builtin's direct implementation, if it has one: a method of the builtin's name on the
    * runtime's `Rt` or else on [[Builtins]], taking one parameter per argument, each — like its
    * result — in a representation generated code has. Generated code calls it instead of the CEK
    * meaning, on unboxed values where the signature allows. `shiftByteString` and
    * `rotateByteString` keep their meanings, which add variant-E range checks to the functions.
    */
  def direct(fun: DefaultFun): Option[Method] =
    def on(c: Class[?]) = c
      .getMethods
      .find(m =>
        m.getName == name(fun) && m.getParameterCount == scheme(fun).arity &&
          (m.getReturnType +: m.getParameterTypes).forall(Rep.of(_).isDefined)
      )
    if fun == DefaultFun.ShiftByteString || fun == DefaultFun.RotateByteString then None
    else on(Class.forName("dev.constructive.eo.demo.uplc.runtime.Rt")).orElse(on(Builtins.getClass))

  /** The UPLC spelling of a builtin (`addInteger`); Scalus' enum cases are capitalised. */
  def name(fun: DefaultFun): String = Utils.lowerFirst(fun.toString)
