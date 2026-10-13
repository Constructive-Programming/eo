package dev.constructive.eo.demo.uplc.runtime

import dev.constructive.eo.demo.uplc.SourceProgram
import scalus.uplc.Constant

/** What a generated class knows about its source: the decoded program (to discharge closures) and
  * the constant tables its code indexes into.
  */
final class ProgramInfo(val source: SourceProgram):

  /** Constants in pre-order, in the uniform boxed representation. */
  def boxed(): Array[AnyRef] = source.constants.iterator.map(c => c: AnyRef).toArray

  /** Constants in pre-order, scalars unboxed (`BigInt`, `ByteString`, `String`). */
  def unboxed(): Array[AnyRef] =
    source
      .constants
      .iterator
      .map[AnyRef] {
        case Constant.Integer(v)    => v
        case Constant.ByteString(b) => b
        case Constant.String(s)     => s
        case other                  => other
      }
      .toArray

object ProgramInfo:

  /** Reassembles the flat bytes a class embedded as Latin-1 string chunks. */
  def fromChunks(chunks: Array[String]): ProgramInfo =
    val text = chunks.mkString
    new ProgramInfo(SourceProgram.decode(text.iterator.map(_.toByte).toArray))
