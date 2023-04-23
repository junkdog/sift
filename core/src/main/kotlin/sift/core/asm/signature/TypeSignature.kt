package sift.core.asm.signature

import sift.core.dsl.Type

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
}

sealed interface ArgType {
    data class Plain(val type: Type) : ArgType {
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
