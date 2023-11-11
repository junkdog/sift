package sift.core.asm.signature

import net.onedaybeard.collectionsby.filterNotBy
import sift.core.dsl.Type
import sift.core.dsl.type

data class BoundTypeParameter(
    val name: String,
    val boundType: Type,
    val extends: MutableList<TypeSignature> = mutableListOf(), // visitClassType of T
)

data class FormalTypeParameter(
    val name: String,
    val extends: MutableList<TypeSignature> = mutableListOf(), // visitClassType of T
) {
    internal fun specialize(typeParameters: Map<String, TypeParameter>): FormalTypeParameter {
        return copy(
            extends = extends.map { it.specialize(typeParameters) }.toMutableList()
        )
    }

    override fun hashCode(): Int = toString().hashCode()

    override fun toString(): String {
        val filtered = extends.filterNotBy(TypeSignature::argType, extendsAny)

        return name + when (filtered.size) {
            0    -> ""
            1    -> " : ${filtered.first()}"
            else -> " : $filtered"
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is FormalTypeParameter && other.name == name
    }
}

private val extendsAny = ArgType.Plain("java.lang.Object".type)
