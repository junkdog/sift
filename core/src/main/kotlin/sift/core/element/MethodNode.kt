package sift.core.element

import org.objectweb.asm.tree.AbstractInsnNode
import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.asSequence
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.MethodSignatureNode
import sift.core.asm.signature.signature
import sift.core.dsl.Type
import sift.core.kotlin.KotlinFunction

class MethodNode private constructor(
    private val cn: ClassNode,
    private val mn: AsmMethodNode,
    override val annotations: List<AnnotationNode>,
    private val kfn: KotlinFunction?,
) : Element {

    override val simpleName: String
        get() = kfn?.name ?: mn.name

    val owner: ClassNode
        get() = cn

    val name: String
        get() = kfn?.name ?: mn.name

    val desc: String
        get() = mn.desc

    /** returns true if this function is a kotlin extension function */
    val isExtension: Boolean
        get() = kfn?.isExtension == true

    /** returns the type of the receiver if this function is a kotlin extension function */
    val receiver: Type?
        get() = kfn?.receiver

    val signature: MethodSignatureNode? by lazy {
        mn.signature(cn.signature?.formalParameters ?: listOf())
    }

    val formalTypeParameters: List<FormalTypeParameter>
        get() = signature?.formalParameters ?: listOf()

    val parameters: List<ParameterNode> = ParameterNode.from(cn, this, mn, kfn)

    val access: Int
        get() = mn.access

    fun returns(): SignatureNode? {
        return signature?.returns
            ?.let(SignatureNode::from)
    }

    private val hash = hash(cn) * 31 + idHash(mn)

    override fun toString(): String = "$cn::$name"

    fun instructions(): Sequence<AbstractInsnNode> = mn.asSequence()

    override fun equals(other: Any?): Boolean {
        return mn === (other as? MethodNode)?.mn
    }

    override fun hashCode() = hash

    companion object {
        internal fun from(
            cn: ClassNode,
            mn: AsmMethodNode,
            kfn: KotlinFunction?
        ): MethodNode {
            val ans = AnnotationNode.from(mn.visibleAnnotations, mn.invisibleAnnotations)
            return MethodNode(cn, mn, ans, kfn)
        }
    }
}
