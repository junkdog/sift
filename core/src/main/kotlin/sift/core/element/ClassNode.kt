package sift.core.element

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InnerClassNode
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.signature.ClassSignatureNode
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.TypeSignature
import sift.core.asm.signature.signature
import sift.core.asm.simpleName
import sift.core.asm.superType
import sift.core.asm.type

class ClassNode private constructor(
    private val cn: AsmClassNode,
    override val annotations: List<AnnotationNode>
) : Element {

    internal val signature: ClassSignatureNode? = cn.signature()

    val outerType: AsmType?
        get() = cn.innerClasses
            ?.findBy(InnerClassNode::name, cn.name)
            ?.outerType

    val innerName: String?
        get() = cn.innerClasses
            ?.findBy(InnerClassNode::name, cn.name)
            ?.innerName

    val fields: List<FieldNode> = cn.fields
        .map { fn -> FieldNode.from(this, fn) }

    val methods: MutableList<MethodNode> = cn.methods
        .map { mn -> MethodNode.from(this, mn) }
        .toMutableList()

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val isEnum: Boolean
        get() = cn.superType == type<java.lang.Enum<*>>()

    val extends: TypeSignature?
        get() = signature?.extends

    val type: AsmType
        get() = cn.type

    val formalTypeParameters: List<FormalTypeParameter>
        get() = signature?.formalParameters ?: listOf()

    val qualifiedName: String
        get() = type.className

    val superType: AsmType?
        get() = cn.superType

    override val simpleName: String
        get() = type.simpleName

    val access: Int
        get() = cn.access

    // TODO: TypeSignature
    val interfaces: List<AsmType>
        get() = cn.interfaces?.map { Type.getType("L${it};") } ?: emptyList()

    override fun toString() = simpleName

    override fun equals(other: Any?): Boolean {
        return cn === (other as? ClassNode)?.cn
    }

    override fun hashCode(): Int = idHash(cn)
    internal fun asAsmNode(): AsmClassNode = cn

    companion object {
        fun from(cn: AsmClassNode): ClassNode = ClassNode(
            cn = cn,
            annotations = AnnotationNode.from(cn.visibleAnnotations, cn.invisibleAnnotations)
        )
    }
}

private val InnerClassNode.outerType: Type
    get() = Type.getType("L${outerName};")