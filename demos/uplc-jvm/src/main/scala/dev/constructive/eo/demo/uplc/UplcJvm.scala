package dev.constructive.eo.demo.uplc

import dev.constructive.eo.optics.{Iso, Prism}
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassReader, ClassTooLargeException, ClassWriter, MethodTooLargeException}

/** A UPLC → JVM bytecode compiler as eo optics, all starting from the script as the ledger stores
  * it:
  *
  * {{{
  *   CborScript ⇀ TypedProgram ≅ JvmClass ⇀ JvmClassScript
  *              toTyped        lowering   compiling
  * }}}
  *
  * Every stage keeps the script it came from — a typed program its source, a class the script
  * itself, embedded in its constants — so every arrow can be walked back. `lowering` is a lawful
  * iso: going there and back is the identity, both ways. The other two are prisms, total on the way
  * back. A script may declare a Plutus Core version these semantics do not know
  * ([[Semantics.versions]]): then it has no typed program. A class can be too large for a class
  * file: then it has none. So the compiler, [[toBytecode]], is a prism too: `toBytecode.getOption`
  * compiles, and `toBytecode.reverseGet` hands back the script a class file was compiled from.
  */
object UplcJvm:

  /** Script ⇀ typed expression: decode and type a script of a known Plutus Core version; back, the
    * script the program was read from.
    */
  val toTyped = Prism[CborScript, TypedProgram](
    script =>
      val source = SourceProgram.decode(script)
      Either.cond(Semantics.versions(source.version), Typer.typeProgram(source), script)
    ,
    _.script,
  )

  /** Typed expression ≅ JVM class: lower; back, type the script the class embeds — of a known
    * version, as only a typed program is lowered.
    */
  val lowering: Iso[TypedProgram, JvmClass] =
    Iso(Lower.lower, cls => Typer.typeProgram(SourceProgram.decode(Lower.script(cls))))

  /** JVM class ⇀ class file: [[compile]] when the class fits in one; back, [[decompile]]. */
  val compiling = Prism[JvmClass, JvmClassScript](cls => compile(cls).left.map(_ => cls), decompile)

  /** Script ⇀ JVM class. */
  val toJvmClass = toTyped.andThen(lowering)

  /** The compiler, and its inverse on the class files it writes. */
  val toBytecode = toJvmClass.andThen(compiling)

  /** ASM writes the class file, computing its stack map frames — or says why the class does not fit
    * in one: the JVM caps a method at 64 KB of bytecode, and a class at 65535 constants. Every
    * reference type generated code merges is the same on both paths or `Object`, so no class is
    * loaded to find a common one.
    */
  def compile(cls: JvmClass): Either[String, JvmClassScript] =
    val writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES):
      override def getCommonSuperClass(a: String, b: String): String = "java/lang/Object"
    // ASM throws when it meets a limit; this is the one place that becomes a value.
    try
      cls.node.accept(writer)
      Right(JvmClassScript(writer.toByteArray))
    catch case e: (MethodTooLargeException | ClassTooLargeException) => Left(e.getMessage)

  /** ASM reads a class file back into its tree; the frames [[compile]] computes are skipped. */
  def decompile(classFile: JvmClassScript): JvmClass =
    val tree = new ClassNode
    new ClassReader(classFile.bytes).accept(tree, ClassReader.SKIP_FRAMES)
    JvmClass(tree)
