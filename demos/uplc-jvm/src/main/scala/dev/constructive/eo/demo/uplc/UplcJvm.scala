package dev.constructive.eo.demo.uplc

import scala.util.Try

import dev.constructive.eo.optics.{BijectionIso, Iso}
import io.bullet.borer.Cbor

/** A UPLC → JVM bytecode compiler as ONE eo optic.
  *
  * {{{
  *   S = Array[Byte]   flat-encoded UPLC program
  *   A = TypedProgram  the program read back as a typed, Scala-like expression
  *   B = JvmProgram    JVM-shaped code: static methods, slots, explicit boxing
  *   T = Array[Byte]   a class file
  * }}}
  *
  * `get` decodes and types (S ⇒ A), `reverseGet` emits a class file (B ⇒ T), and the compiler is
  * `iso.modify(Lower.lower)`: the optic fixes the two ends, the lowering is the function you run
  * through it. Swap in another `A ⇒ B` — an optimiser before `Lower.lower`, an instrumenting
  * lowering — and the ends stay put.
  *
  * The Iso is polymorphic (`S`/`T` and `A`/`B` differ), so the round-trip laws of a monomorphic iso
  * do not apply. What must hold instead is the commuting square the test-suite checks for every
  * program: running the class file gives what the Scalus CEK machine gives for the flat program.
  */
object UplcJvm:

  val iso: BijectionIso[Array[Byte], Array[Byte], TypedProgram, JvmProgram] =
    Iso[Array[Byte], Array[Byte], TypedProgram, JvmProgram](
      flat => Typer.typeProgram(SourceProgram.decode(flat)),
      program => Emit.classFile(program),
    )

  /** Flat UPLC ⇒ class file. Defined on well-formed flat programs; use [[compile]] otherwise. */
  val translate: Array[Byte] => Array[Byte] = iso.modify(Lower.lower)

  /** Scripts as the ledger stores them: the flat bytes wrapped in a CBOR bytestring. Composed in
    * front of [[iso]] with `andThen`, it gives an optic from CBOR scripts straight to class files.
    */
  val cborScript: BijectionIso[Array[Byte], Array[Byte], Array[Byte], Array[Byte]] =
    Iso[Array[Byte], Array[Byte], Array[Byte], Array[Byte]](
      cbor => Cbor.decode(cbor).to[Array[Byte]].value,
      classFile => classFile,
    )

  val onChain: BijectionIso[Array[Byte], Array[Byte], TypedProgram, JvmProgram] =
    cborScript.andThen(iso)

  /** Checked entry point for bytes of unknown provenance. Scalus' flat decoder throws on malformed
    * input; this is the one place that is turned into a value.
    */
  def compile(flat: Array[Byte]): Either[String, Array[Byte]] =
    Try(SourceProgram.decode(flat)).toEither match
      case Left(e)  => Left(s"not a flat UPLC program: ${e.getMessage}")
      case Right(_) => Right(translate(flat))
