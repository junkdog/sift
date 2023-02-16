package sift.core.element

import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.signature.FieldSignatureNode
import sift.core.asm.signature.signature
import sift.core.asm.type

class FieldNode private constructor(
    private val cn: ClassNode,
    private val fn: AsmFieldNode,
    override val annotations: List<AnnotationNode>
) : Element {

    override val simpleName: String
        get() = fn.name

    val owner: ClassNode
        get() = cn

    val name: String
        get() = fn.name

    val type: AsmType
        get() = fn.type

    override fun equals(other: Any?): Boolean {
        return other is FieldNode
            && cn == other.cn
            && fn === other.fn
    }

    override fun hashCode(): Int = hash(cn) * 31 + idHash(fn)

    val returns: SignatureNode?
        get() = fn.signature(cn.signature?.formalParameters ?: listOf())
            ?.let(FieldSignatureNode::extends)
            ?.let { tsn -> SignatureNode.from(tsn, this) }

    override fun toString(): String = "$cn.$name"

    private val signature: FieldSignatureNode?
        get() = fn.signature(cn.signature?.formalParameters ?: listOf())

    companion object {
        fun from(cn: ClassNode, fn: AsmFieldNode): FieldNode {
            val ans = AnnotationNode.from(fn.visibleAnnotations, fn.invisibleAnnotations)
            return FieldNode(cn, fn, ans)
        }
    }
}