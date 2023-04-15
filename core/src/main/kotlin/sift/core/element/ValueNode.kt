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

    private val hash = hash(data, reference)

    override fun hashCode(): Int = hash

    override fun toString(): String = "<<$data>>"

    companion object {
        fun from(value: Any, reference: Element): ValueNode {
            return ValueNode(value, reference)
        }
    }
}