package sift.core.dsl

import sift.core.asm.internalName
import sift.core.element.AsmType
import kotlin.reflect.KClass

/**
 * Represents a reference to a class, including its generic type information if
 * applicable. The Type class provides methods and properties to work with both
 * the raw class and its generic type variant.
 */
class Type private constructor(
    private val value: String
) {
    val rawType: Type
        get() = Type(value.substringBefore("<"))

    internal val internalName: String
        get() = value.replace(".", "/")

    internal val asmType: AsmType
        get() = AsmType.getType("L${internalName};")

    val simpleName: String
        get() = value.substringAfterLast(".")

    override fun equals(other: Any?): Boolean = value == (other as? Type)?.value
    override fun hashCode(): Int = value.hashCode()

    companion object {
        internal fun from(s: String) = Type(s)
        internal fun from(type: AsmType) = Type(type.className)
    }
}

inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.from(cls.java.internalName)
fun type(value: String): Type = Type.from(value)

val String.type: Type
    get() = Type.from(this)