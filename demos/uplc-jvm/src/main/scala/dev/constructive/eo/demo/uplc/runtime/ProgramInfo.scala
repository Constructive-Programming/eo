package dev.constructive.eo.demo.uplc.runtime

import dev.constructive.eo.demo.uplc.{CborScript, SourceProgram}
import scalus.uplc.Term

/** What a generated class knows about its source: the decoded program (to discharge closures) and
  * the constant at each `Const` node, by node id.
  */
final class ProgramInfo(val source: SourceProgram):

  def constants(): Array[AnyRef | Null] =
    Array.tabulate[AnyRef | Null](source.nodes.length)(i =>
      source.nodes(i) match { case Term.Const(c, _) => c; case _ => null }
    )

object ProgramInfo:

  /** Reassembles the script a class stores as string constants (`CborScript.asConstants`). */
  def fromConstants(constants: Array[AnyRef]): ProgramInfo =
    new ProgramInfo(SourceProgram.decode(CborScript.fromConstants(constants.map(_.toString))))
