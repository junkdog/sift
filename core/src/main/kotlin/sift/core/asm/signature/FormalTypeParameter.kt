package sift.core.asm.signature

import org.objectweb.asm.Type
import sift.core.asm.simpleName

data class FormalTypeParameter(
    val name: String,
    var extends: MutableList<Type> = mutableListOf(), // visitClassType of T
    var metaType: MetaType = MetaType.Undefined,
    val args: MutableList<TypeSignature> = mutableListOf()
) {
    override fun toString(): String {
        val ext = extends
            .filter { it.descriptor != "Ljava/lang/Object;" }
            .map(Type::simpleName)
            .takeIf(List<String>::isNotEmpty)
            ?.let { " : $it" }
            ?: ""

        val inner = args
            .takeIf(MutableList<TypeSignature>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">")
            ?: ""

        return "$name$ext$inner"
    }
}