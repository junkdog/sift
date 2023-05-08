package sift.core.asm.signature

import net.onedaybeard.collectionsby.filterNotBy
import sift.core.dsl.type

data class FormalTypeParameter(
    val name: String,
    val extends: MutableList<TypeSignature> = mutableListOf(), // visitClassType of T
) {
    override fun toString(): String {
        val filtered = extends.filterNotBy(TypeSignature::type, extendsAny)

        return name + when (filtered.size) {
            0    -> ""
            1    -> " : ${filtered.first()}"
            else -> " : $filtered"
        }
    }
}

private val extendsAny = ArgType.Plain("java.lang.Object".type)
