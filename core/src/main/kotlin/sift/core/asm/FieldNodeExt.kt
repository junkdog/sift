package sift.core.asm

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import sift.core.combine
import sift.core.dsl.Type
import sift.core.element.AsmType

val FieldNode.type: AsmType
    get() = AsmType.getType(desc)

fun FieldNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun FieldNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

