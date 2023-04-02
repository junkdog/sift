package sift.core.element

import org.objectweb.asm.tree.AbstractInsnNode
import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.asSequence
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.MethodSignatureNode
import sift.core.asm.signature.signature

class MethodNode(
    private val cn: ClassNode,
    private val mn: AsmMethodNode,
    override val annotations: List<AnnotationNode>
) : Element {
    override val simpleName: String
        get() = mn.name

    val owner: ClassNode
        get() = cn

    val name: String
        get() = mn.name

    val desc: String
        get() = mn.desc

    val signature: MethodSignatureNode? by lazy {
        mn.signature(cn.signature?.formalParameters ?: listOf())
    }

    val formalTypeParameters: List<FormalTypeParameter>
        get() = signature?.formalParameters ?: listOf()

    val parameters: List<ParameterNode> = ParameterNode.from(cn, this, mn)

    val access: Int
        get() = mn.access

    fun returns(): SignatureNode? {
        return signature?.returns
            ?.let(SignatureNode::from)
    }

    override fun toString(): String = "$cn::$name"

    fun instructions(): Sequence<AbstractInsnNode> = mn.asSequence()

    override fun equals(other: Any?): Boolean {
        return other is MethodNode
            && cn == other.cn
            && mn === other.mn
    }

    override fun hashCode(): Int {
        return hash(cn) * 31 + idHash(mn)
    }

    companion object {
        fun from(cn: ClassNode, mn: AsmMethodNode): MethodNode {
            val ans = AnnotationNode.from(mn.visibleAnnotations, mn.invisibleAnnotations)
            return MethodNode(cn, mn, ans)
        }
    }
}
