package dev.constructive.eo.demo.uplc.runtime

import scalus.cardano.onchain.plutus.prelude.List as PList
import scalus.uplc.builtin.Data
import scalus.uplc.{DefaultFun, TypeScheme}

/** Implemented by every generated class. Closures carry the instance plus a method id; the four
  * `call` arities are switches over the class's closure bodies.
  */
trait Dispatch:
  def run(): AnyRef
  def call0(id: Int, env: Array[AnyRef]): AnyRef
  def call1(id: Int, env: Array[AnyRef], a: AnyRef): AnyRef
  def call2(id: Int, env: Array[AnyRef], a: AnyRef, b: AnyRef): AnyRef
  def callN(id: Int, env: Array[AnyRef], args: Array[AnyRef]): AnyRef

// Runtime values. A UPLC value is one of: a Scalus `Constant` (boxed scalar or structured
// constant), or one of the classes below.

/** A lambda chain of `arity` parameters rooted at source node `origin`, with `applied` of them
  * already supplied (a partial application of a curried chain is still one object).
  */
final class Lam(
    val code: Dispatch,
    val id: Int,
    val origin: Int,
    val arity: Int,
    val env: Array[AnyRef],
    val applied: Array[AnyRef],
)

/** `delay body`, rooted at source node `origin`. */
final class Thunk(val code: Dispatch, val id: Int, val origin: Int, val env: Array[AnyRef])

/** A builtin that has not yet received all of its forces and arguments. `scheme` is what it still
  * expects; `trail` records what it got (innermost last) so it can be discharged.
  */
final class Partial(
    val fun: DefaultFun,
    val scheme: TypeScheme,
    val args: Vector[AnyRef],
    val trail: Vector[Option[AnyRef]],
)

/** `constr tag fields`. */
final class ConstrV(val tag: Long, val fields: Array[AnyRef])

/** A `list data` read straight off a `Data` value: an element becomes a constant only when read. */
final case class DataList(elems: PList[Data])

/** The `pair integer (list data)` `unConstrData` returns, before anyone takes it apart. */
final case class ConstrPair(tag: BigInt, fields: PList[Data])
