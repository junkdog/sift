package sift.core.element

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.tree.InnerClassNode
import sift.core.AsmNodeHashcoder.idHash
import sift.core.asm.signature.ClassSignatureNode
import sift.core.asm.signature.FormalTypeParameter
import sift.core.asm.signature.TypeSignature
import sift.core.asm.signature.signature
import sift.core.asm.superType
import sift.core.dsl.Type
import sift.core.kotlin.KotlinClass

class ClassNode private constructor(
    private val cn: AsmClassNode,
    override val annotations: List<AnnotationNode>
) : Element {

    internal val signature: ClassSignatureNode? = cn.signature()

    private val kotlinClass: KotlinClass? = KotlinClass.from(cn)

    internal val isKotlin: Boolean
        get() = kotlinClass != null

    val outerType: Type?
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
        .map { mn -> MethodNode.from(this, mn, kotlinClass?.functions?.find { it.matches(mn) }) }
        .toMutableList()

    val isEnum: Boolean
        get() = cn.superType?.rawType?.name == "java.lang.Enum"

    val extends: TypeSignature?
        get() = signature?.extends

    // note: type-erased
    val type: Type
        get() = Type.from(cn.name)

    val formalTypeParameters: List<FormalTypeParameter>
        get() = signature?.formalParameters ?: listOf()

    val qualifiedName: String
        get() = type.name

    val superType: Type?
        get() = cn.superType

    override val simpleName: String
        get() = type.simpleName

    val access: Int
        get() = cn.access

    // TODO: TypeSignature
    val interfaces: List<Type>
        get() = cn.interfaces?.map { Type.from(it) } ?: emptyList()

    private val hash = idHash(cn)

    override fun toString() = simpleName

    override fun equals(other: Any?): Boolean {
        return cn === (other as? ClassNode)?.cn
    }

    override fun hashCode(): Int = hash
    internal fun asAsmNode(): AsmClassNode = cn

    companion object {
        fun from(cn: AsmClassNode): ClassNode = ClassNode(
            cn = cn,
            annotations = AnnotationNode.from(cn.visibleAnnotations, cn.invisibleAnnotations)
        )
    }
}

private val InnerClassNode.outerType: Type
    get() = Type.from(outerName)