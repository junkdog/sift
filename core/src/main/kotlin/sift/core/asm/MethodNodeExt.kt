package sift.core.asm

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import sift.core.combine
import sift.core.dsl.Type
import sift.core.element.AsmType
import sift.core.stringWriter
import java.io.PrintWriter


fun MethodNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun MethodNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun InsnList.asSequence() = Iterable { iterator() }.asSequence()
fun MethodNode.asSequence() = instructions.asSequence()

fun MethodNode.argumentTypes(): Array<Type> = AsmType.getArgumentTypes(desc)
    .map { Type.from(it.internalName) }
    .toTypedArray()

fun MethodNode.copy(): MethodNode {
    return MethodNode(access, name, desc, signature, exceptions?.toTypedArray() ?: arrayOf())
        .also { mn -> accept(mn) }
}

fun MethodNode.toDebugString(): String = stringWriter {
    val printer = Textifier()
    accept(TraceMethodVisitor(printer))
    printer.print(PrintWriter(this))
}