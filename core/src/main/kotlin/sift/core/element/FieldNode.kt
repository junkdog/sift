package sift.core.element

import org.objectweb.asm.Opcodes.*
import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.signature.FieldSignatureNode
import sift.core.asm.signature.signature
import sift.core.dsl.Type

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

    val type: Type
        get() = Type.fromTypeDescriptor(fn.desc)

    val isStatic: Boolean
        get() = (fn.access and ACC_STATIC) == ACC_STATIC

    val isFinal: Boolean
        get() = (fn.access and ACC_FINAL) == ACC_FINAL

    val isEnum: Boolean
        get() = (fn.access and ACC_ENUM) == ACC_ENUM

    val access: Int
        get() = fn.access

    private val hash = hash(cn) * 31 + idHash(fn)

    val returns: SignatureNode?
        get() = fn.signature(cn.signature?.formalParameters ?: listOf())
            ?.let(FieldSignatureNode::extends)
            ?.let(SignatureNode::from)

    override fun equals(other: Any?): Boolean {
        return fn === (other as? FieldNode)?.fn
    }

    override fun hashCode(): Int = hash

    override fun toString(): String = "$cn.$name"

    companion object {
        internal fun from(cn: ClassNode, fn: AsmFieldNode): FieldNode {
            val ans = AnnotationNode.from(fn.visibleAnnotations, fn.invisibleAnnotations)
            return FieldNode(cn, fn, ans)
        }
    }
}