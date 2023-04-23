package sift.core.dsl

import sift.core.api.parseSignature
import sift.core.asm.internalName
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature
import sift.core.element.AsmClassNode
import sift.core.element.AsmType
import kotlin.reflect.KClass

/**
 * Represents a reference to a class, including its generic type information if
 * applicable. The Type class provides methods and properties to work with both
 * the raw class and its generic type variant.
 */
class Type private constructor(
    internal val value: String,
    internal val isPrimitive: Boolean = false,
) {
    val name =  if (isPrimitive) when (value) {
        "Z" -> "boolean"
        "B" -> "byte"
        "C" -> "char"
        "S" -> "short"
        "I" -> "int"
        "J" -> "long"
        "F" -> "float"
        "D" -> "double"
        "V" -> "void"
        else -> error("Unknown primitive type: $value")
    } else value.replace('/', '.').replace('$', '.')

    internal val internalName: String
        get() = value.substringBefore("<")

    internal val signature: TypeSignature
        get() = parseSignature(name)
    val isGeneric: Boolean
        get() = signature.args.isNotEmpty()

    val simpleName: String
        get() = if (isPrimitive) name else value.substringAfterLast("/").replace('$', '.')

    val descriptor: String
        get() = if (isPrimitive) value else "L$internalName;"

    val rawType: Type
        get() = if (isGeneric) Type.from(value.substringBefore("<")) else this

    override fun equals(other: Any?): Boolean = value == (other as? Type)?.value
    override fun hashCode(): Int = value.hashCode()

    override fun toString() = name

    companion object {

        internal fun primitiveType(descriptor: Char): Type {
            require(descriptor in "ZBCSIJFDV")
            return Type(descriptor.toString(), true)
        }

        internal fun fromTypeDescriptor(descriptor: String): Type {
            return if (descriptor.startsWith('L') && descriptor.endsWith(';'))
                from(descriptor.substring(1, descriptor.length - 1))
            else
                primitiveType(descriptor[0])
        }

        internal fun from(s: String) = Type(s.replace('.', '/'))
        internal fun from(cls: KClass<*>) = from(cls.internalName)
        internal fun from(type: AsmType) = from(type.className)
        internal fun from(cn: AsmClassNode) = from(cn.name)
        internal fun from(signature: TypeSignature): Type {
            fun fromType(argType: ArgType): String = when (argType) {
                is ArgType.Array -> fromType(argType.wrapped!!)
                is ArgType.Plain -> argType.type.name
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
fun type(cls: KClass<*>) = Type.from(cls)
fun type(value: String): Type = Type.from(value)

val String.type: Type
    get() = Type.from(this)