package sift.core.asm.signature

import sift.core.dsl.Type

data class TypeSignature(
    val argType: ArgType,
    val arrayDepth: Int,
    val bound: MetaType,
    val args: MutableList<TypeSignature> = mutableListOf(),
) {
    internal fun reify(typeParameters: Map<String, TypeParameter>): TypeSignature {
        fun substitute(argType: ArgType): ArgType {
            return when (argType) {
                is ArgType.Plain -> argType
                is ArgType.Var   -> typeParameters[argType.type.name]?.let { ArgType.BoundVar(it.bound, argType.type) } ?: argType
                is ArgType.Array -> argType.copy(argType.wrapped?.let(::substitute))
                is ArgType.BoundVar   -> TODO("should not happen")
            }
        }

        return copy(
            argType = substitute(argType),
            args = args.map { arg -> arg.reify(typeParameters) }.toMutableList()
        )
    }

    override fun toString(): String {
        val array = "[]".repeat(arrayDepth)
        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""

        return "$argType$array$inner"
    }
}

sealed interface ArgType {
    data class Plain(val type: Type) : ArgType {
        override fun toString(): String = type.simpleName
    }

    data class Var(val type: FormalTypeParameter) : ArgType {
        override fun toString(): String = type.name
    }

    // resolved var from a class signature
    data class BoundVar(val type: Type, val ftp: FormalTypeParameter) : ArgType {
        override fun toString(): String = "$type: ${ftp.name}"
    }

    data class Array(var wrapped: ArgType?) : ArgType {
        override fun toString(): String = "$wrapped[]"
    }
}

enum class MetaType {
    GenericType, Class, Interface, Array, Undefined
}

internal data class TypeParameter(
    val name: String,
    val bound: Type,
    val constraint: List<Type>
)