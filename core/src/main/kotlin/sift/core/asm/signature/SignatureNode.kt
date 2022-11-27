package sift.core.asm.signature

import org.objectweb.asm.Type
import sift.core.asm.simpleName

class SignatureNode(
    val type: ArgType,
    val parameters: List<FormalTypeParameter>,
    val args: List<TypeArgument>,
    val returnType: TypeSignature?,
    val extends: List<TypeSignature>
)

data class TypeSignature(
    val type: Type,
    val args: List<TypeArgument>
)

/** "usage" of generic type and constraints */
data class TypeArgument(
    val type: ArgType,
    val bound: MetaType,
    val wildcard: Char,
    val inner: MutableList<TypeArgument> = mutableListOf()
) {
    override fun toString(): String {
        return "TypeArgument($type)"
    }
}

/** declaration of generic type and constraints */
data class FormalTypeParameter(
    val name: String,
    var extends: Type? = null, // visitClassType of T
    val args: MutableList<TypeArgument> = mutableListOf()
) {
    override fun toString(): String {
        val ext = extends?.let { " : ${it.simpleName}" }
        return "<$name$ext>"
    }
}

sealed interface ArgType {
    data class Plain(val type: Type) : ArgType
    data class Var(val type: FormalTypeParameter) : ArgType
}

enum class MetaType {
    GenericType, Class, Interface, Array
}

data class TypeVariable(
    val name: String,
    val type: Type
)