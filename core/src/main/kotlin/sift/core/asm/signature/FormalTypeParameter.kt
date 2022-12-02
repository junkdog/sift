package sift.core.asm.signature

import net.onedaybeard.collectionsby.filterNotBy
import org.objectweb.asm.Type

data class FormalTypeParameter(
    val name: String,
    val extends: MutableList<TypeSignature> = mutableListOf(), // visitClassType of T
) {
    override fun toString(): String {
        val extendsAny = ArgType.Plain(Type.getType(java.lang.Object::class.java))
        val filtered = extends.filterNotBy(TypeSignature::type, extendsAny)

        val ext = when (filtered.size) {
            1 -> filtered.first().toString()
            0 -> null
            else -> filtered.toString()
        }?.let { " : $it" } ?: ""

        return "$name$ext"
    }
}