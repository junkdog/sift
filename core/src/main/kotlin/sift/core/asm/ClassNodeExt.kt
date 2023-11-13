package sift.core.asm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.TraceClassVisitor
import sift.core.combine
import sift.core.dsl.Type
import sift.core.stringWriter
import java.io.PrintWriter

/** compatible with [Class.forName] */
val ClassNode.qualifiedName: String
    get() = type.internalName.replace('/', '.') // retaining '$' for inner classes

/** extended class type, returns `null` for `java.lang.Object` */
val ClassNode.superType: Type?
    get() = superName
        ?.takeUnless { "java/lang/Object" in it }
        ?.let(Type::from)

val ClassNode.shortName: String
    get() = qualifiedName.substringAfterLast(".")

fun ClassNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun ClassNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun ClassNode.copy() = ClassNode(Opcodes.ASM7).also(::accept)
fun ClassNode.copy(remapper: Remapper) = ClassNode(Opcodes.ASM7)
    .also { cn -> accept(ClassRemapper(cn, remapper)) }

fun ClassNode.toBytes(): ByteArray {
    val cw = ClassWriter(0)
    accept(cw)
    return cw.toByteArray()
}

fun ClassNode.toDebugString(): String = stringWriter {
    accept(TraceClassVisitor(PrintWriter(this)))
}