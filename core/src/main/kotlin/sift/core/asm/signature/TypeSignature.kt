package sift.core.asm.signature

import org.objectweb.asm.Type
import sift.core.asm.simpleName

data class TypeSignature(
    val type: ArgType,
    val bound: MetaType,
    val wildcard: Char,
    val args: MutableList<TypeSignature> = mutableListOf()
) {
    override fun toString(): String {
        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""

        return "$type$inner"
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
    GenericType, Class, Interface, Array
}