package dev.constructive.eo.demo.uplc

import dev.constructive.eo.optics.Iso
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassReader, ClassWriter}

/** A UPLC → JVM bytecode compiler as eo optics, all starting from the script as the ledger stores
  * it:
  *
  * {{{
  *   CborScript ≅ TypedProgram ≅ JvmClass ≅ JvmClassScript
  *              toTyped        lowering   compiling
  * }}}
  *
  * Every stage keeps the script it came from — a typed program its source, a class the script
  * itself, embedded in its constants — so every arrow can be walked back, and each is a lawful iso
  * on what the compiler produces: going there and back is the identity, both ways. [[toTyped]],
  * [[toJvmClass]] and [[toBytecode]] are the views a caller asks for; `toBytecode.get` is the
  * compiler, and `toBytecode.reverseGet` hands back the script a class was compiled from.
  */
object UplcJvm:

  /** Script ≅ typed expression: decode and type; back, the script the program was read from. */
  val toTyped: Iso[CborScript, TypedProgram] =
    Iso(script => Typer.typeProgram(SourceProgram.decode(script)), _.script)

  /** Typed expression ≅ JVM class: lower; back, type the script the class embeds. */
  val lowering: Iso[TypedProgram, JvmClass] =
    Iso(Lower.lower, cls => toTyped.get(Lower.script(cls)))

  /** JVM class ≅ class file: [[compile]] one way, [[decompile]] the other. */
  val compiling: Iso[JvmClass, JvmClassScript] = Iso(compile, decompile)

  val toJvmClass: Iso[CborScript, JvmClass] = toTyped.andThen(lowering)

  /** The compiler, and its inverse on the classes it writes. */
  val toBytecode: Iso[CborScript, JvmClassScript] = toJvmClass.andThen(compiling)

  /** ASM writes the class file, computing its stack map frames. Every reference type generated code
    * merges is the same on both paths or `Object`, so no class is loaded to find a common one.
    */
  def compile(cls: JvmClass): JvmClassScript =
    val writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES):
      override def getCommonSuperClass(a: String, b: String): String = "java/lang/Object"
    cls.tree.accept(writer)
    JvmClassScript(writer.toByteArray)

  /** ASM reads a class file back into its tree; the frames [[compile]] computes are skipped. */
  def decompile(classFile: JvmClassScript): JvmClass =
    val tree = new ClassNode
    new ClassReader(classFile.bytes).accept(tree, ClassReader.SKIP_FRAMES)
    JvmClass(tree)
