package dev.constructive.eo.demo.uplc

import dev.constructive.eo.optics.Iso
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassReader, ClassWriter}

/** A UPLC → JVM bytecode compiler as eo optics, all starting from the script as the ledger stores
  * it:
  *
  * {{{
  *   CborScript ≅ TypedProgram ≅ JvmClass ≅ JvmClassScript
  *              typed          lowering    writing
  * }}}
  *
  * Every stage keeps the script it came from — a typed program its source, a class the script
  * itself, embedded in its constants — so every arrow can be walked back, and each is a lawful iso
  * on what the compiler produces: going there and back is the identity, both ways. [[typed]],
  * [[bytecode]] and [[classFile]] are the views a caller asks for; `classFile.get` is the compiler,
  * and `classFile.reverseGet` hands back the script a class was compiled from.
  */
object UplcJvm:

  /** Script ≅ typed expression: decode and type; back, the script the program was read from. */
  val typed: Iso[CborScript, TypedProgram] =
    Iso(script => Typer.typeProgram(SourceProgram.decode(script)), _.script)

  /** Typed expression ≅ bytecode: lower; back, type the script the class embeds. */
  val lowering: Iso[TypedProgram, JvmClass] =
    Iso(Lower.lower, cls => typed.get(Lower.script(cls)))

  /** Bytecode ≅ class file: ASM writes the class, computing its stack map frames, and reads it
    * back. Every reference type generated code merges is the same on both paths or `Object`, so no
    * class is loaded to find a common one.
    */
  val writing: Iso[JvmClass, JvmClassScript] = Iso(write, read)

  val bytecode: Iso[CborScript, JvmClass] = typed.andThen(lowering)

  /** The compiler, and its inverse on the classes it writes. */
  val classFile: Iso[CborScript, JvmClassScript] = bytecode.andThen(writing)

  private def write(cls: JvmClass): JvmClassScript =
    val writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES):
      override def getCommonSuperClass(a: String, b: String): String = "java/lang/Object"
    cls.tree.accept(writer)
    JvmClassScript(writer.toByteArray)

  private def read(classFile: JvmClassScript): JvmClass =
    val tree = new ClassNode
    new ClassReader(classFile.bytes).accept(tree, ClassReader.SKIP_FRAMES)
    JvmClass(tree)
