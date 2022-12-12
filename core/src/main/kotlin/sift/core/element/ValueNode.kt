package sift.core.element

import sift.core.AsmNodeHashcoder.hash

class ValueNode private constructor(
    private val any: Any,
    internal val reference: Element
) : Element {
    override val simpleName: String
        get() = "$any: ${reference.simpleName}"

    override val annotations: List<AnnotationNode>
        get() = listOf() // fixme: remove

    override fun equals(other: Any?): Boolean {
        return other is ValueNode
            && reference == other.reference
            && any == other.any
    }

    override fun hashCode(): Int {
        return hash(any, reference)
    }

    companion object {
        fun from(value: Any, reference: Element): ValueNode {
            return ValueNode(value, reference)
        }
    }
}