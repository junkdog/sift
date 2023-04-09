package sift.core.asm.signature

import sift.core.asm.simpleName
import sift.core.element.AsmType

data class TypeSignature(
    val type: ArgType,
    val arrayDepth: Int,
    val bound: MetaType,
    val args: MutableList<TypeSignature> = mutableListOf(),
) {
    override fun toString(): String {
        val array = "[]".repeat(arrayDepth)
        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""

        return "$type$array$inner"
    }

    internal fun toTypeString(): String {
        fun fromType(argType: ArgType): String = when (argType) {
            is ArgType.Array -> fromType(argType.wrapped!!)
            is ArgType.Plain -> argType.type.className
            is ArgType.Var -> argType.type.name
        }

        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">") { fromType(it.type) }
            ?: ""

        return "$type$inner"
    }
}

sealed interface ArgType {
    data class Plain(val type: AsmType) : ArgType {
        override fun toString(): String = type.simpleName
    }

    data class Var(val type: FormalTypeParameter) : ArgType {
        override fun toString(): String = type.name
    }

    data class Array(var wrapped: ArgType?) : ArgType {
        override fun toString(): String = "$wrapped[]"
    }
}

enum class MetaType {
    GenericType, Class, Interface, Array, Undefined
}
