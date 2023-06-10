package sift.core.kotlin

import kotlinx.metadata.*
import sift.core.asm.signature.TypeSignature
import sift.core.dsl.Type

internal class KotlinFunction(
    private val kmFunction: KmFunction,
) {
    val isOperator: Boolean
        get() = Flag.Function.IS_OPERATOR(kmFunction.flags)
    val isInfix: Boolean
        get() = Flag.Function.IS_INFIX(kmFunction.flags)
    val isInline: Boolean
        get() = Flag.Function.IS_INLINE(kmFunction.flags)

    val isInternal: Boolean
        get() = Flag.IS_INTERNAL(kmFunction.flags)

    val receiver: Type? = kmFunction
        .receiverParameterType
        ?.let(Type::from)

    override fun toString(): String {
        return listOfNotNull(
            if (isInline) "inline " else null,
            if (isInfix) "infix " else null,
            if (isOperator) "operator " else null,
            receiver?.simpleName,
            receiver?.let { "." },
            kmFunction.name,
            "(${kmFunction.valueParameters.map { it.name }.joinToString()})"
        ).joinToString("")
    }
}

private fun Type.Companion.from(typeProjection: KmTypeProjection): Type {
    return from(typeProjection.type!!)
}


private fun Type.Companion.from(kmType: KmType): Type {
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

    return Type.from(rawType.internalName + generics)
}