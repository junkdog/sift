package sift.core.kotlin

import kotlinx.metadata.*
import kotlinx.metadata.jvm.signature
import sift.core.dsl.Type
import sift.core.element.AsmMethodNode

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

//    val visiblity: KmVisibility?
//        get() = kmFunction.visibility

    val isExtension: Boolean
        get() = receiver != null

    val receiver: Type? = kmFunction
        .receiverParameterType
        ?.let(Type::from)

    val name: String = listOfNotNull(
        receiver?.simpleName,
        receiver?.let { "." },
        kmFunction.name,
    ).joinToString("")

    val parameters: List<KotlinParameter> = kmFunction.valueParameters.map(KotlinParameter::from)

    fun matches(other: AsmMethodNode): Boolean {
        return kmFunction.signature?.name == other.name
            && kmFunction.signature?.desc == other.desc
    }

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



