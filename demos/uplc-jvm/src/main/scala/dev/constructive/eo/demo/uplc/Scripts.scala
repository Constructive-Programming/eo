package dev.constructive.eo.demo.uplc

import scala.util.Try

import java.util.Base64
import org.objectweb.asm.tree.ClassNode
import scalus.uplc.builtin.ByteString
import scalus.uplc.{DeBruijnedProgram, Program}

// What the compiler's optics focus on, besides the typed program. The two byte encodings at its ends
// are Scalus' `ByteString` underneath — bytes with value equality, so the optics' round-trip laws
// can compare them with `==`. None of the three hands out what it holds, only copies: a value
// cannot change after the compiler made or checked it, so the way back always works.

/** A Plutus script as the ledger stores it: the flat-encoded program, wrapped in a CBOR bytestring.
  * Every value is well formed: [[CborScript.of]] encoded it, or [[CborScript.parse]] checked it.
  */
opaque type CborScript = ByteString

object CborScript:

  /** A Scalus program, flat-encoded. Scalus' encoder throws on a program it cannot encode — one
    * with a free variable; this is the one place that becomes a value.
    */
  def of(program: Program): Either[String, CborScript] =
    Try(ByteString.fromArray(program.cborEncoded))
      .toEither
      .left
      .map(e => s"no flat encoding: ${e.getMessage}")

  /** Bytes of unknown provenance. Scalus' decoder throws on malformed input; this is the one place
    * that becomes a value.
    */
  def parse(cbor: Array[Byte]): Either[String, CborScript] =
    val script = ByteString.fromArray(cbor) // checked after copying: the caller keeps `cbor`
    Try(DeBruijnedProgram.fromCbor(script.bytes)).toEither match
      case Left(e)  => Left(s"not a CBOR-wrapped flat UPLC script: ${e.getMessage}")
      case Right(_) => Right(script)

  /** The script back from the string constants [[asConstants]] stored it in. */
  private[uplc] def fromConstants(constants: Iterable[String]): CborScript =
    ByteString.fromArray(Base64.getDecoder.decode(constants.mkString))

  extension (script: CborScript)
    def bytes: Array[Byte] = script.bytes.clone

    private[uplc] def program: DeBruijnedProgram = DeBruijnedProgram.fromCbor(script.bytes)

    /** The script as class-file string constants. A compiled class carries the script it was
      * compiled from — that is how `lowering` walks back, and what the runtime re-decodes to turn
      * results back into terms. A class file has no byte-array constants, so the script is stored
      * as text: Base64, whose characters take one byte each in a class file, cut into pieces of at
      * most 65535 characters, the most one string constant holds.
      */
    private[uplc] def asConstants: Seq[String] =
      Base64.getEncoder.encodeToString(script.bytes).grouped(65535).toSeq

/** A class file this compiler wrote for a script: its JVM code, and the script itself. Only the
  * compiler makes one ([[UplcJvm.toBytecode]]), and the script comes back out of it.
  */
opaque type JvmClassScript = ByteString

object JvmClassScript:

  private[uplc] def apply(classFile: Array[Byte]): JvmClassScript = ByteString.fromArray(classFile)

  extension (classFile: JvmClassScript) def bytes: Array[Byte] = classFile.bytes.clone

/** A script's JVM class in memory — ASM's class tree, as the compiler builds it or as ASM reads it
  * back from the class file ([[UplcJvm.decompile]]). Only those two make one, so it always embeds
  * its script.
  */
opaque type JvmClass = ClassNode

object JvmClass:

  private[uplc] def apply(tree: ClassNode): JvmClass = tree

  extension (cls: JvmClass)

    /** A copy of ASM's tree, to inspect or print the bytecode. */
    def tree: ClassNode =
      val copy = new ClassNode
      cls.accept(copy)
      copy

    private[uplc] def node: ClassNode = cls
