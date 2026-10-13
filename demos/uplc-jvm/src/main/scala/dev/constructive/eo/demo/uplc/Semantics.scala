package dev.constructive.eo.demo.uplc

import scala.annotation.tailrec

import scalus.cardano.ledger.MajorProtocolVersion
import scalus.uplc.builtin.platform
import scalus.uplc.eval.PlutusVM
import scalus.uplc.{BuiltinRuntime, CardanoBuiltins, DefaultFun, TypeScheme}

/** The single Plutus semantics this demo targets — Plutus V3 at protocol version 11 ("van Rossem"),
  * builtin semantics variant E, the same configuration Scalus 1.3.0 runs the Plutus conformance
  * suite under. The oracle VM and the generated code's runtime both read it from here, so they
  * cannot drift apart.
  */
object Semantics:

  val protocolVersion: MajorProtocolVersion = MajorProtocolVersion.vanRossemPV

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

  /** The argument types and the result type of a builtin, past its forces. */
  def signature(fun: DefaultFun): (List[Ty], Ty) =
    @tailrec
    def go(t: TypeScheme, args: List[Ty]): (List[Ty], Ty) = t match
      case TypeScheme.All(_, rest)     => go(rest, args)
      case TypeScheme.Arrow(arg, rest) => go(rest, Ty.of(arg) :: args)
      case result                      => (args.reverse, Ty.of(result))
    go(scheme(fun), Nil)

  /** The UPLC spelling of a builtin (`addInteger`); Scalus' enum cases are capitalised. */
  def name(fun: DefaultFun): String =
    val n = fun.toString
    n.take(1).toLowerCase + n.drop(1)
