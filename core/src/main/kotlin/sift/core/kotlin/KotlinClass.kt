package sift.core.kotlin

import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import net.onedaybeard.collectionsby.findBy
import sift.core.asm.annotations
import sift.core.asm.readAttributeAny
import sift.core.dsl.Type
import sift.core.element.AsmAnnotationNode
import sift.core.element.AsmClassNode



private fun allLocalFunctionsOf(kmClass: KmClass): List<KotlinCallable> {
    return kmClass.functions.map(::KotlinFunction) +
        kmClass.constructors.map(::KotlinConstructor)
}

internal class KotlinClass(
    private val kmClass: KmClass,
) {
    val type: Type = kmClass.name.let(Type::from)
    val functions: Map<String, KotlinCallable> = allLocalFunctionsOf(kmClass)
        .associateBy { it.jvmName + it.descriptor }

    val properties: Map<String, KotlinProperty> = kmClass.properties
        .map(::KotlinProperty)
        .associateBy(KotlinProperty::name)

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
        kind = readAttributeAny("k")(this) as Int,
        metadataVersion = (readAttributeAny("mv")(this) as List<Int>?)?.toIntArray(),
        data1 = (readAttributeAny("d1")(this) as List<String>?)?.toTypedArray(),
        data2 = (readAttributeAny("d2")(this) as List<String>?)?.toTypedArray(),
        extraString = readAttributeAny("xs")(this) as String?,
        packageName = readAttributeAny("pn")(this) as String?,
        extraInt = readAttributeAny("xi")(this) as Int?
    )
}
