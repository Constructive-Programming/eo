package dev.constructive.eo.demo.uplc.runtime

import dev.constructive.eo.demo.uplc.SourceProgram
import java.util.Base64
import scalus.uplc.{Constant, Term}

/** What a generated class knows about its source: the decoded program (to discharge closures) and
  * the constant tables its code indexes by node id.
  */
final class ProgramInfo(val source: SourceProgram):

  private def constants(f: Constant => AnyRef): Array[AnyRef | Null] =
    source
      .nodes
      .iterator
      .map[AnyRef | Null] {
        case Term.Const(c, _) => f(c)
        case _                => null
      }
      .toArray

  /** The constant at each `Const` node, in the uniform boxed representation. */
  def boxed(): Array[AnyRef | Null] = constants(c => c)

  /** The constant at each `Const` node, scalars unboxed (`BigInt`, `ByteString`, `String`). */
  def unboxed(): Array[AnyRef | Null] = constants {
    case Constant.Integer(v)    => v
    case Constant.ByteString(b) => b
    case Constant.String(s)     => s
    case other                  => other
  }

object ProgramInfo:

  /** Reassembles the flat bytes a class embedded as Base64 string chunks. */
  def fromChunks(chunks: Array[String]): ProgramInfo =
    new ProgramInfo(SourceProgram.decode(Base64.getDecoder.decode(chunks.mkString)))
