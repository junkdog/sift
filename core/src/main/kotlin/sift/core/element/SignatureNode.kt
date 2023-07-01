package sift.core.element

import sift.core.AsmNodeHashcoder.hash
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature
import sift.core.dsl.Type

class SignatureNode private constructor(
    val signature: TypeSignature,
) : Element, Trait.HasType {
    override val annotations: List<AnnotationNode> = emptyList() // consider removal

    internal val inner: List<SignatureNode> by lazy { signature.args.map(::from) }

    override val simpleName: String
        get() = signature.toString()

    override val type: Type
        get() = Type.from(signature)

    val argType: ArgType
        get() = signature.type

    override fun equals(other: Any?): Boolean {
        return other is SignatureNode
            && hash == other.hash
            && signature == other.signature
    }

    private val hash = hash(signature)

    override fun hashCode(): Int = hash

    override fun toString(): String = signature.toString()

    companion object {
        fun from(signature: TypeSignature): SignatureNode = SignatureNode(signature)
    }
}