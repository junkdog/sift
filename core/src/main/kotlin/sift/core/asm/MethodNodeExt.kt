package sift.core.asm

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import sift.core.combine
import sift.core.dsl.Type
import sift.core.element.AsmType


fun MethodNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun MethodNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

fun InsnList.asSequence() = Iterable { iterator() }.asSequence()
fun MethodNode.asSequence() = instructions.asSequence()

fun MethodNode.argumentTypes(): Array<Type> = AsmType.getArgumentTypes(desc)
    .map { Type.from(it.internalName) }
    .toTypedArray()
