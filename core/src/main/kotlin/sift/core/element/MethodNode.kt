package sift.core.element

import org.objectweb.asm.tree.AbstractInsnNode
import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.asSequence
import sift.core.asm.copy
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.MethodSignatureNode
import sift.core.asm.signature.signature
import sift.core.dsl.Type
import sift.core.dsl.Visibility
import sift.core.kotlin.KotlinCallable

class MethodNode private constructor(
    private val cn: ClassNode,
    private val mn: AsmMethodNode,
    override val annotations: List<AnnotationNode>,
    private val kfn: KotlinCallable?,
    private val originalCn: ClassNode? = null // when method is inherited
) : Element() {

    init {
        annotations.forEach { it.parent = this }
    }

    override val simpleName: String
        get() = kfn?.name ?: mn.name

    /** returns false kotlin property accessors, lambdas etc */
    internal val isKotlin: Boolean
        get() = kfn != null

    internal val isAbstract: Boolean
        get() = mn.instructions.size() == 0

    internal val owner: ClassNode
        get() = cn

    val name: String
        get() = kfn?.name ?: mn.name

    internal val desc: String
        get() = mn.desc
    internal val rawSignature: String
        get() = mn.signature ?: ""

    /** returns true if this function is a kotlin extension function */
    val isExtension: Boolean
        get() = kfn?.isExtension == true

    /** returns the type of the receiver if this function is a kotlin extension function */
    val receiver: Type?
        get() = kfn?.receiver

    internal val signature: MethodSignatureNode? =
        mn.signature((originalCn ?: cn).signature?.formalParameters ?: listOf())

    val formalTypeParameters: List<FormalTypeParameter>
        get() = signature?.formalParameters ?: listOf()

    val parameters: List<ParameterNode> = ParameterNode.from(cn, this, mn, kfn)

    val access: Int
        get() = mn.access

    val visibility: Visibility
        get() = kfn?.isInternal
            ?.takeIf { it }
            ?.let { Visibility.Internal }
            ?: Visibility.from(access)

    val returns: SignatureNode? by lazy { signature?.returns?.let(SignatureNode::from) }

    private val hash = hash(cn) * 31 + idHash(mn)

    fun toMethodRefString(): String = "$cn::$name"
    override fun toString(): String = toMethodRefString()
//    override fun toString(): String = "$cn.$name(${parameters.joinToString { "${it.name}: ${it.type.simpleName}" }})"

    fun instructions(): Sequence<AbstractInsnNode> = mn.asSequence()

    override fun equals(other: Any?): Boolean {
        return other is MethodNode
            && mn === other.mn
            && cn == other.cn
    }

    override fun hashCode() = hash

    internal fun copyWithOwner(cn: ClassNode): MethodNode {
        val anno = AnnotationNode.from(mn.visibleAnnotations, mn.invisibleAnnotations)
        return MethodNode(cn, mn.copy(), anno, kfn, originalCn ?: this.cn)
            .also { mn -> mn.id = -1 }
    }

    companion object {
        internal fun from(
            cn: ClassNode,
            mn: AsmMethodNode,
            kfn: KotlinCallable?
        ): MethodNode {
            val ans = AnnotationNode.from(mn.visibleAnnotations, mn.invisibleAnnotations)
            return MethodNode(cn, mn, ans, kfn)
        }
    }
}
