package dev.constructive.eo.demo.uplc

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

  /** The force/argument shape of a builtin, and the type of its saturated result. */
  def signature(fun: DefaultFun): Signature = signatures(fun.ordinal)

  private lazy val signatures: IArray[Signature] =
    IArray.from(DefaultFun.values.iterator.map(f => Signature.of(runtime(f).typeScheme)))

/** One step a builtin consumes before it runs: a `force` (type instantiation) or an argument. */
enum Step:
  case Force, Arg

final case class Signature(steps: List[Step], result: Ty):
  def arity: Int = steps.count(_ == Step.Arg)

object Signature:

  def of(scheme: TypeScheme): Signature = scheme match
    case TypeScheme.All(_, rest) =>
      val tail = of(rest)
      tail.copy(steps = Step.Force :: tail.steps)
    case TypeScheme.Arrow(_, rest) =>
      val tail = of(rest)
      tail.copy(steps = Step.Arg :: tail.steps)
    case TypeScheme.Type(uni) => Signature(Nil, Ty.ofUni(uni))
    case _                    => Signature(Nil, Ty.Dyn)

  /** True once a builtin with this remaining scheme has everything it needs and runs. */
  def saturated(scheme: TypeScheme): Boolean = scheme match
    case _: TypeScheme.All | _: TypeScheme.Arrow => false
    case _                                       => true
