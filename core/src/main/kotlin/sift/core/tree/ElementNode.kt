package sift.core.tree

import org.objectweb.asm.Opcodes
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.FormalTypeParameter
import sift.core.element.*
import sift.core.entity.Entity
import java.util.*
import kotlin.Comparator

data class ElementNode(
    val label: String,
    val type: String,
    val elementId: Int,
    val entityType: Entity.Type?,
    val entityId: UUID?,
    val traces: Int,
    val properties: List<String>,
    val formalTypeParameters: List<String>,
) : Comparator<ElementNode>, Comparable<ElementNode> {
    constructor(element: Element, entity: Entity?, tracesToElement: Int) : this(
        element.toString(),
        element::class.simpleName!!,
        element.id,
        entity?.type,
        entity?.id,
        tracesToElement,
        element.properties(),
        element.formalTypeParameters()
    )

    override fun compareTo(other: ElementNode): Int {
        return compare(this, other)
    }

    override fun toString(): String = "$elementId: $label <<${type.replace("Node", "")}>>"
    override fun compare(o1: ElementNode, o2: ElementNode): Int = o1.elementId.compareTo(o2.elementId)
}

private fun Element.formalTypeParameters(): List<String> = when (this) {
    is AnnotationNode -> null
    is ClassNode      -> signature?.formalParameters
    is FieldNode      -> signature?.formalParameters
    is MethodNode     -> signature?.formalParameters
    is ParameterNode  -> null // todo: generic parameters
    is SignatureNode  -> (argType as? ArgType.Var)?.type?.let(::listOf)
    is ValueNode      -> null
}?.map(FormalTypeParameter::toString) ?: listOf()

private fun Element.properties(): List<String> {
    return when (this) {
        is AnnotationNode -> listOfNotNull()
        is ClassNode -> listOfNotNull(
            "kotlin".takeIf { isKotlin },
            "enum".takeIf { isEnum },
            "iface".takeIf { isInterface },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is FieldNode -> listOfNotNull(
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
            "inherited".takeIf { originalCn != null },
        )
        is MethodNode -> listOfNotNull(
            "abstract".takeIf { isAbstract },
            "inherited".takeIf { originalCn != null },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is ParameterNode -> listOfNotNull()
        is SignatureNode -> listOfNotNull()
        is ValueNode -> listOf()
    }
}
