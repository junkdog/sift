package sift.core.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import sift.core.combine

val FieldNode.type: Type
    get() = Type.getType(desc)

fun FieldNode.annotations(): MutableIterable<AnnotationNode> =
    combine(invisibleAnnotations, visibleAnnotations)

fun FieldNode.hasAnnotation(type: Type) =
    type in visibleAnnotations || type in invisibleAnnotations

