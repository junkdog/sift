package sift.core.element

import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.readFieldAny
import sift.core.asm.type
import sift.core.combine
import sift.core.dsl.Type

class AnnotationNode private constructor(
    private val an: AsmAnnotationNode,
) : Element {
    override val simpleName: String
        get() = an.type.simpleName

    val type: Type
        get() = an.type

    operator fun get(field: String): Any? {
        return readFieldAny(field)(an)
    }

    override val annotations: List<AnnotationNode> = emptyList() // consider removal

    override fun hashCode(): Int {
        return idHash(an)
    }

    override fun equals(other: Any?): Boolean {
        return an === (other as? AnnotationNode)?.an
    }

    companion object {
        fun from(an: AsmAnnotationNode): AnnotationNode {
            return AnnotationNode(an)
        }

        fun from(
            visible: List<AsmAnnotationNode>?,
            invisible: List<AsmAnnotationNode>?
        ): List<AnnotationNode> {
            return combine(visible, invisible).map(::from)
        }
    }
}
