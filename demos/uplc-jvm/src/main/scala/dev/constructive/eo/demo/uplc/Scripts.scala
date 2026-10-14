package dev.constructive.eo.demo.uplc

import scala.util.Try

import java.util.Base64
import scalus.uplc.builtin.ByteString
import scalus.uplc.{DeBruijnedProgram, Program}

// The two byte encodings at the ends of the compiler. Both are Scalus' `ByteString` underneath —
// bytes with value equality, so the optics' round-trip laws can compare them with `==`.

/** A Plutus script as the ledger stores it: the flat-encoded program, wrapped in a CBOR bytestring.
  * Every value is well formed: one comes from a Scalus `Program`, or from bytes [[CborScript.parse]]
  * has checked.
  */
opaque type CborScript = ByteString

object CborScript:

  def of(program: Program): CborScript = ByteString.unsafeFromArray(program.cborEncoded)

  /** Bytes of unknown provenance. Scalus' decoder throws on malformed input; this is the one place
    * that becomes a value.
    */
  def parse(cbor: Array[Byte]): Either[String, CborScript] =
    Try(DeBruijnedProgram.fromCbor(cbor)).toEither match
      case Left(e)  => Left(s"not a CBOR-wrapped flat UPLC script: ${e.getMessage}")
      case Right(_) => Right(ByteString.fromArray(cbor))

  /** The script a compiled class embeds, from the Base64 chunks [[chunks]] cut it into. */
  private[uplc] def fromChunks(chunks: Iterable[String]): CborScript =
    ByteString.unsafeFromArray(Base64.getDecoder.decode(chunks.mkString))

  extension (script: CborScript)
    def bytes: Array[Byte] = script.bytes

    /** Base64, one constant-pool byte per char, in chunks a class-file string constant can hold. */
    private[uplc] def chunks: Seq[String] =
      Base64.getEncoder.encodeToString(script.bytes).grouped(65535).toSeq

/** A class file this compiler wrote for a script: its JVM code, and the script itself. Only the
  * compiler makes one ([[UplcJvm.classFile]]), and the script comes back out of it.
  */
opaque type JvmClassScript = ByteString

object JvmClassScript:

  private[uplc] def apply(classFile: Array[Byte]): JvmClassScript =
    ByteString.unsafeFromArray(classFile)

  extension (classFile: JvmClassScript) def bytes: Array[Byte] = classFile.bytes
