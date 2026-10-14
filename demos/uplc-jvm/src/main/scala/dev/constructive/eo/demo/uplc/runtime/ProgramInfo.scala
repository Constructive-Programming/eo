package dev.constructive.eo.demo.uplc.runtime

import dev.constructive.eo.demo.uplc.SourceProgram
import java.util.Base64
import scalus.uplc.Term

/** What a generated class knows about its source: the decoded program (to discharge closures) and
  * the constant at each `Const` node, by node id.
  */
final class ProgramInfo(val source: SourceProgram):

  def constants(): Array[AnyRef | Null] = source
    .nodes
    .iterator
    .map[AnyRef | Null] {
      case Term.Const(c, _) => c
      case _                => null
    }
    .toArray

object ProgramInfo:

  /** Reassembles the flat bytes a class embedded as Base64 string chunks. */
  def fromChunks(chunks: Array[AnyRef]): ProgramInfo =
    new ProgramInfo(SourceProgram.decode(Base64.getDecoder.decode(chunks.mkString)))
