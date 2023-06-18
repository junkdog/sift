package sift.core.kotlin

import kotlinx.metadata.*
import kotlinx.metadata.jvm.signature
import sift.core.dsl.Type

internal sealed class KotlinCallable {
    abstract val isOperator: Boolean
    abstract val isInfix: Boolean
    abstract val isInline: Boolean
    abstract val isInternal: Boolean
    abstract val isExtension: Boolean
    abstract val receiver: Type?
    abstract val descriptor: String
    abstract val jvmName: String
    abstract val name: String
    abstract val parameters: List<KotlinParameter>
}

internal class KotlinConstructor(
    private val kmConstructor: KmConstructor,
) : KotlinCallable() {
    override val isOperator: Boolean
        get() = false
    override val isInfix: Boolean
        get() = false
    override val isInline: Boolean
        get() = false

    override val isInternal: Boolean
        get() = Flag.IS_INTERNAL(kmConstructor.flags)

    override val isExtension: Boolean
        get() = false

    override val receiver: Type? = null

    override val descriptor: String = kmConstructor.signature?.desc ?: "<unknown>"

    override val jvmName: String
        get() = "<init>"

    override val name: String = "<init>"

    override val parameters: List<KotlinParameter> = kmConstructor
        .valueParameters
        .map(KotlinParameter::from)

    override fun toString(): String {
        return "<init>(${kmConstructor.valueParameters.map { it.name }.joinToString()})"
    }
}

internal class KotlinFunction(
    private val kmFunction: KmFunction,
) : KotlinCallable() {
    override val isOperator: Boolean
        get() = Flag.Function.IS_OPERATOR(kmFunction.flags)
    override val isInfix: Boolean
        get() = Flag.Function.IS_INFIX(kmFunction.flags)
    override val isInline: Boolean
        get() = Flag.Function.IS_INLINE(kmFunction.flags)

    override val isInternal: Boolean
        get() = Flag.IS_INTERNAL(kmFunction.flags)

    override val isExtension: Boolean
        get() = receiver != null

    override val receiver: Type? = kmFunction
        .receiverParameterType
        ?.let(Type::from)

    override val descriptor: String = kmFunction.signature?.desc ?: "<unknown>"

    override val jvmName: String
        get() = kmFunction.signature?.name ?: "<unknown>"

    override val name: String = listOfNotNull(
        receiver?.simpleName,
        receiver?.let { "." },
        kmFunction.name,
    ).joinToString("")

    override val parameters: List<KotlinParameter> = kmFunction
        .valueParameters
        .map(KotlinParameter::from)

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



