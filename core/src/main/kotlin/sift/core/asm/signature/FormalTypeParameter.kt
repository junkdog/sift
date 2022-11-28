package sift.core.asm.signature

import org.objectweb.asm.Type
import sift.core.asm.simpleName

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