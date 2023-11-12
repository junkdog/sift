package sift.core.element

import net.onedaybeard.collectionsby.findBy
import org.objectweb.asm.tree.InnerClassNode
import sift.core.AsmNodeHashcoder.idHash
import sift.core.api.AccessFlags.acc_annotation
import sift.core.api.AccessFlags.acc_interface
import sift.core.asm.signature.ClassSignatureNode
import sift.core.asm.signature.signature
import sift.core.asm.superType
import sift.core.asm.toDebugString
import sift.core.dsl.Type
import sift.core.dsl.Visibility
import sift.core.kotlin.KotlinClass

class ClassNode private constructor(
    private val cn: AsmClassNode,
    override val annotations: List<AnnotationNode>
) : Element(), Trait.HasType {

    init {
        annotations.forEach { it.parent = this }
    }

    internal val asmClassNode: AsmClassNode
        get() = cn

    internal val signature: ClassSignatureNode? = cn.signature()

    internal val isInterface: Boolean
        get() = acc_interface.check(access)

    internal val isAnnotation: Boolean
        get() = acc_annotation.check(access)

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

    internal val fields: List<FieldNode> = cn.fields
        .map { fn -> FieldNode.from(this, fn, kotlinClass?.properties?.get(fn.name)) }

    // declared methods
    internal val methods: MutableList<MethodNode> = cn.methods
        .map { mn -> MethodNode.from(this, mn, kotlinClass?.functions?.get(mn.name + mn.desc)) }
        .toMutableList()

    internal var inheritedMethods: List<MethodNode>? = null
    internal var inheritedFields: List<FieldNode>? = null

    val isEnum: Boolean
        get() = cn.superType?.name == "java.lang.Enum"

    val extends: SignatureNode? = signature?.extends?.let(SignatureNode::from)

    // note: type-erased
    override val type: Type
        get() = Type.from(cn.name)

    val qualifiedName: String
        get() = type.name

    val superType: Type?
        get() = cn.superType

    override val simpleName: String
        get() = type.simpleName

    val access: Int
        get() = cn.access

    val visibility: Visibility
        get() = kotlinClass?.isInternal
            ?.takeIf { it }
            ?.let { Visibility.Internal }
            ?: Visibility.from(access)

    // TODO: TypeSignature
    val interfaces: List<Type>
        get() = cn.interfaces?.map { Type.from(it) } ?: emptyList()

    private val hash = idHash(cn)

    override fun toString() = simpleName
    internal fun toDebugString(): String = cn.toDebugString()

    override fun equals(other: Any?): Boolean {
        return cn === (other as? ClassNode)?.cn
    }

    override fun hashCode(): Int = hash

    companion object {
        fun from(cn: AsmClassNode): ClassNode = ClassNode(
            cn = cn,
            annotations = AnnotationNode.from(cn.visibleAnnotations, cn.invisibleAnnotations)
        )
    }
}

private val InnerClassNode.outerType: Type
    get() = Type.from(outerName)