package sift.core.element

import sift.core.AsmNodeHashcoder.hash

class ValueNode private constructor(
    val data: Any,
    internal val reference: Element
) : Element {
    override val simpleName: String
        get() = "$data: ${reference.simpleName}"

    override val annotations: List<AnnotationNode>
        get() = listOf() // fixme: remove

    override fun equals(other: Any?): Boolean {
        return other is ValueNode
            && reference == other.reference
            && data == other.data
    }

    override fun hashCode(): Int {
        return hash(data, reference)
    }

    companion object {
        fun from(value: Any, reference: Element): ValueNode {
            return ValueNode(value, reference)
        }
    }
}