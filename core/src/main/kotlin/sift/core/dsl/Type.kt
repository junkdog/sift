package sift.core.dsl

import sift.core.api.parseSignature
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature
import sift.core.asm.type
import sift.core.element.AsmClassNode
import sift.core.element.AsmType
import kotlin.reflect.KClass

/**
 * Represents a reference to a class, including its generic type information if
 * applicable. The Type class provides methods and properties to work with both
 * the raw class and its generic type variant.
 */
class Type private constructor(
    private val value: String,
) {
    internal val internalName: String
        get() = value.substringBefore("<").replace('.', '/')

    internal val asmType: AsmType
        get() = AsmType.getType("L${internalName};")
    internal val signature: TypeSignature
        get() = parseSignature(value)
    val isGeneric: Boolean
        get() = signature.args.isNotEmpty()

    val simpleName: String
        get() = value.substringAfterLast(".")


    override fun equals(other: Any?): Boolean = value == (other as? Type)?.value
    override fun hashCode(): Int = value.hashCode()

    override fun toString() = value


    companion object {
        internal fun from(s: String) = Type(s)
        internal fun from(type: AsmType) = Type(type.className)
        internal fun from(cn: AsmClassNode) = from(cn.type)
        internal fun from(signature: TypeSignature): Type {
            fun fromType(argType: ArgType): String = when (argType) {
                is ArgType.Array -> fromType(argType.wrapped!!)
                is ArgType.Plain -> argType.type.className
                is ArgType.Var   -> argType.type.name
            }

            val inner = signature.args
                .takeIf(MutableList<TypeSignature>::isNotEmpty)
                ?.joinToString(prefix = "<", postfix = ">") { fromType(it.type) }
                ?: ""


            return from("${signature.type}$inner")
        }
    }
}

// FIXME: encode generics: name + generic args
inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.from(cls.java.name)
fun type(value: String): Type = Type.from(value)

val String.type: Type
    get() = Type.from(this)