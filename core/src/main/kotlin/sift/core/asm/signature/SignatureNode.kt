package sift.core.asm.signature

import org.objectweb.asm.Type
import sift.core.asm.simpleName

class SignatureNode(
    val type: ArgType,
    val parameters: List<FormalTypeParameter>,
    val args: List<TypeSignature>,
    val returnType: TypeSignature?,
    val extends: List<TypeSignature>
)

/** "usage" of generic type and constraints */
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

/** declaration of generic type and constraints */
data class FormalTypeParameter(
    val name: String,
    var extends: Type? = null, // visitClassType of T
    val args: MutableList<TypeSignature> = mutableListOf()
) {
    override fun toString(): String {
        val ext = extends?.let { " : ${it.simpleName}" }

        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""

        return "<$name$ext$inner>"
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

data class TypeVariable(
    val name: String,
    val type: Type
)