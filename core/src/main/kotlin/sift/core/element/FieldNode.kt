package sift.core.element

import org.objectweb.asm.Opcodes.*
import sift.core.AsmNodeHashcoder.hash
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.signature.FieldSignatureNode
import sift.core.asm.signature.TypeParameter
import sift.core.asm.signature.signature
import sift.core.dsl.Type
import sift.core.dsl.Visibility
import sift.core.kotlin.KotlinProperty

class FieldNode private constructor(
    private val cn: ClassNode,
    private val fn: AsmFieldNode,
    private val kprop: KotlinProperty?,
    override val annotations: List<AnnotationNode>,
    internal val signature: FieldSignatureNode? = fn.signature(cn.signature?.formalParameters ?: listOf())
) : Element(), Trait.HasType {

    init {
        annotations.forEach { it.parent = this }
    }

    override val simpleName: String
        get() = kprop?.name ?: fn.name

    val owner: ClassNode
        get() = cn

    val name: String
        get() = simpleName

    override val type: Type
        get() = Type.fromTypeDescriptor(fn.desc)

    val isStatic: Boolean
        get() = (fn.access and ACC_STATIC) == ACC_STATIC

    val isFinal: Boolean
        get() = (fn.access and ACC_FINAL) == ACC_FINAL

    val isEnum: Boolean
        get() = (fn.access and ACC_ENUM) == ACC_ENUM

    val access: Int
        get() = fn.access

    val visibility: Visibility
        get() = kprop?.visibility ?: Visibility.from(access)

    private val hash = hash(cn) * 31 + idHash(fn)

    val returns: SignatureNode? = signature
        ?.let(FieldSignatureNode::extends)
        ?.let(SignatureNode::from)

    override fun equals(other: Any?): Boolean {
        return fn === (other as? FieldNode)?.fn
    }

    override fun hashCode(): Int = hash

    override fun toString(): String = "$cn.$name"

    internal fun copyWithOwner(cn: ClassNode): FieldNode {
        return FieldNode(cn, fn, kprop, annotations, signature)
            .also { fn -> fn.id = -1 }
    }

    internal fun specialize(typeParameters: Map<String, TypeParameter>): FieldNode {
        return FieldNode(cn, fn, kprop, annotations, signature?.specialize(typeParameters))
            .also { fn -> fn.id = -1 }
    }

    companion object {
        internal fun from(cn: ClassNode, fn: AsmFieldNode, kp: KotlinProperty?): FieldNode {
            val ans = AnnotationNode.from(fn.visibleAnnotations, fn.invisibleAnnotations)
            return FieldNode(cn, fn, kp, ans)
        }
    }
}