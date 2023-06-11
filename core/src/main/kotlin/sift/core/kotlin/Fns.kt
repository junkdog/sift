package sift.core.kotlin

import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import kotlinx.metadata.KmTypeProjection
import sift.core.dsl.Type

fun Type.Companion.from(kmType: KmType): Type {
    val rawType = when (val c = kmType.classifier) {
        is KmClassifier.Class         -> from(c.name)
        is KmClassifier.TypeAlias     -> from(c.name)
        is KmClassifier.TypeParameter -> error("type parameters are no yet supported")
    }

    val generics = kmType.arguments
        .takeIf(List<KmTypeProjection>::isNotEmpty)
        ?.map(::from)
        ?.let { "<${it.joinToString()}>" }
        ?: ""

    return from(rawType.internalName + generics)
}

fun Type.Companion.from(typeProjection: KmTypeProjection): Type {
    return from(typeProjection.type!!)
}
