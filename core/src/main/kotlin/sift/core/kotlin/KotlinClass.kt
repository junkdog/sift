package sift.core.kotlin

import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import net.onedaybeard.collectionsby.findBy
import sift.core.asm.annotations
import sift.core.asm.readFieldAny
import sift.core.dsl.Type
import sift.core.element.AsmAnnotationNode
import sift.core.element.AsmClassNode

internal class KotlinClass(
    private val kmClass: KmClass,
) {
    val type: Type = kmClass.name.let(Type::from)
    val functions: List<KotlinFunction> = kmClass.functions.map(::KotlinFunction)

    val isInternal: Boolean
        get() = Flag.IS_INTERNAL(kmClass.flags)

    override fun toString(): String = type.simpleName

    companion object {
        fun from(cn: AsmClassNode): KotlinClass? = cn
            .annotations()
            .findBy(AsmAnnotationNode::desc, "Lkotlin/Metadata;")
            ?.let(AsmAnnotationNode::toKotlinMetadata)
            ?.let { metadata -> KotlinClassMetadata.read(metadata) as? KotlinClassMetadata.Class }
            ?.toKmClass()
            ?.let(::KotlinClass)
    }
}

@Suppress("UNCHECKED_CAST")
private fun AsmAnnotationNode.toKotlinMetadata(): kotlin.Metadata {
    return kotlinx.metadata.jvm.Metadata(
        kind = readFieldAny("k")(this) as Int,
        metadataVersion = (readFieldAny("mv")(this) as List<Int>?)?.toIntArray(),
        data1 = (readFieldAny("d1")(this) as List<String>?)?.toTypedArray(),
        data2 = (readFieldAny("d2")(this) as List<String>?)?.toTypedArray(),
        extraString = readFieldAny("xs")(this) as String?,
        packageName = readFieldAny("pn")(this) as String?,
        extraInt = readFieldAny("xi")(this) as Int?
    )
}
