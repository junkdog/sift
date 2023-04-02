package sift.core.element

import sift.core.AsmNodeHashcoder.hash
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature

class SignatureNode private constructor(
    private val signature: TypeSignature,
) : Element {
    override val annotations: List<AnnotationNode> = emptyList() // consider removal

    internal val inner: List<SignatureNode> by lazy { signature.args.map(::from) }

    override val simpleName: String
        get() = signature.toString()

    val argType: ArgType
        get() = signature.type

    override fun equals(other: Any?): Boolean {
        return other is SignatureNode
            && signature == other.signature
    }

    override fun hashCode(): Int = hash(signature)

    override fun toString(): String = signature.toString()

    companion object {
        fun from(signature: TypeSignature): SignatureNode = SignatureNode(signature)
    }
}