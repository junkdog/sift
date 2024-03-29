package sift.core.tree

import org.objectweb.asm.Opcodes
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
    val properties: List<String>
) : Comparator<ElementNode>, Comparable<ElementNode> {
    constructor(element: Element, entity: Entity?, tracesToElement: Int) : this(
        element.toString(),
        element::class.simpleName!!,
        element.id,
        entity?.type,
        entity?.id,
        tracesToElement,
        element.properties()
    )

    override fun compareTo(other: ElementNode): Int {
        return compare(this, other)
    }

    override fun toString(): String = "$elementId: $label <<${type.replace("Node", "")}>>"
    override fun compare(o1: ElementNode, o2: ElementNode): Int = o1.elementId.compareTo(o2.elementId)
}

private fun Element.properties(): List<String> {
    return when (this) {
        is AnnotationNode -> listOfNotNull()
        is ClassNode -> listOfNotNull(
            "kotlin".takeIf { isKotlin },
            "iface".takeIf { isInterface },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is FieldNode -> listOfNotNull(
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
        )
        is MethodNode -> listOfNotNull(
            "abstract".takeIf { isAbstract },
            "static".takeIf { access and Opcodes.ACC_STATIC != 0 },
            "synth".takeIf { access and Opcodes.ACC_SYNTHETIC != 0 },
        )
        is ParameterNode -> listOfNotNull()
        is SignatureNode -> listOfNotNull()
        is ValueNode -> listOf()
    }
}
