package sift.core.dsl

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import sift.core.api.parseSignature
import sift.core.asm.internalName
import sift.core.asm.signature.ArgType
import sift.core.asm.signature.TypeSignature
import sift.core.element.AsmClassNode
import sift.core.element.AsmType
import sift.core.jackson.SiftTypeSerializer
import kotlin.reflect.KClass

// a 'descriptor' of types
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
sealed interface SiftType {
    val simpleName: String
    fun matches(rhs: Type): Boolean
}

@JsonSerialize(using = SiftTypeSerializer.Serializer::class)
@JsonDeserialize(using = SiftTypeSerializer.Deserializer::class)
class RegexType internal constructor(
    internal val match: Regex
): SiftType {
    override val simpleName: String = match.pattern
    override fun matches(rhs: Type): Boolean = match.containsMatchIn(rhs.name)
    override fun hashCode(): Int = match.hashCode()
    override fun equals(other: Any?): Boolean = (other as? RegexType)?.match == match
    override fun toString(): String = match.pattern
}

/**
 * Represents a reference to a class, including its generic type information if
 * applicable. The Type class provides methods and properties to work with both
 * the raw class and its generic type variant.
 */
@JsonSerialize(using = SiftTypeSerializer.Serializer::class)
@JsonDeserialize(using = SiftTypeSerializer.Deserializer::class)
class Type private constructor(
    internal val value: String,
): SiftType {
    internal val isPrimitive: Boolean = value.length == 1 && value in primitiveDescriptors

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

    override val simpleName: String
        get() = if (isPrimitive) name else value.substringAfterLast("/").replace('$', '.')

    val descriptor: String
        get() = if (isPrimitive) value else "L$internalName;"

    val rawType: Type
        get() = if (isGeneric) from(value.substringBefore("<")) else this

    override fun matches(rhs: Type): Boolean {
        // if this is not generic type, only compare raw types
        return equals(rhs.takeUnless { !isGeneric && rhs.isGeneric } ?: rhs.rawType)
    }

    override fun equals(other: Any?): Boolean = value == (other as? Type)?.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString() = name

    companion object {
        private const val primitiveDescriptors = "ZBCSIJFDV"

        internal fun primitiveType(descriptor: Char): Type {
            require(descriptor in primitiveDescriptors)
            return Type(descriptor.toString())
        }

        internal fun fromTypeDescriptor(descriptor: String): Type {
            return if (descriptor.startsWith('L') && descriptor.endsWith(';'))
                from(descriptor.substring(1, descriptor.length - 1))
            else
                primitiveType(descriptor[0])
        }

        internal fun from(s: String) = Type(s.replace('.', '/'))
        internal fun from(cls: KClass<*>) = from(cls.internalName)
        internal fun from(type: AsmType) = from(type.internalName)
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

inline fun <reified T> type() = type(T::class)
fun type(cls: KClass<*>) = Type.from(cls)
fun type(value: String): Type = Type.from(value)

fun regexType(value: String): RegexType = RegexType(value.toRegex())
fun regexType(value: Regex): RegexType = RegexType(value)

val String.type: Type
    get() = Type.from(this)

val String.regexType: RegexType
    get() = RegexType(this.toRegex())
val Regex.type: RegexType
    get() = RegexType(this)

operator fun List<Type>.contains(type: SiftType): Boolean = any(type::matches)