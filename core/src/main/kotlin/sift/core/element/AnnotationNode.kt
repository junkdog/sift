package sift.core.element

import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.type
import sift.core.combine
import sift.core.dsl.Type

class AnnotationNode private constructor(
    private val an: AsmAnnotationNode,
) : Element, Trait.HasType {
    internal  lateinit var parent: Element

    internal val root: AnnotationNode
        get() = generateSequence(this) { it.parent as? AnnotationNode }.last()

    override val simpleName: String
        get() = an.type.simpleName

    override val type: Type
        get() = an.type

    private val values: Map<Any, Any?> by lazy {
        (an.values ?: emptyList())
            .chunked(2)
            .associate { (k, v) -> k to remapAnnotationValue(v) }
            .onEach {  }
    }

    operator fun get(field: String): Any? = values[field]

    override val annotations: List<AnnotationNode> = emptyList() // consider removal

    override fun toString() = "@$simpleName"

    override fun hashCode(): Int {
        return idHash(an)
    }

    override fun equals(other: Any?): Boolean {
        return an === (other as? AnnotationNode)?.an
    }

    private fun remapAnnotationValue(value: Any?): Any? = when (value) {
        is List<*>           -> value.map(::remapAnnotationValue)
        is Array<*>          -> value.map(::remapAnnotationValue)
        is AsmAnnotationNode -> from(value).also { it.parent = this }
        is AsmType           -> Type.from(value)
        else                 -> value
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
