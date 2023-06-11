package sift.core.kotlin

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.fieldSignature
import sift.core.dsl.Type
import sift.core.dsl.Visibility
import sift.core.element.AsmFieldNode

internal class KotlinProperty(
    private val kmProperty: KmProperty
) {
    val name: String = kmProperty.name
    val type: Type
        get() = kmProperty.returnType.let(Type::from)

    val isInternal: Boolean
        get() = visibility == Visibility.Internal
    val isExtensionReceiver: Boolean
        get() = kmProperty.receiverParameterType != null
    val receiverType: Type?
        get() = kmProperty.receiverParameterType?.let(Type::from)

    val visibility: Visibility = when {
        Flag.IS_PUBLIC(kmProperty.getterFlags)    -> Visibility.Public
        Flag.IS_PROTECTED(kmProperty.getterFlags) -> Visibility.Protected
        Flag.IS_INTERNAL(kmProperty.getterFlags)  -> Visibility.Internal
        Flag.IS_PRIVATE(kmProperty.getterFlags)   -> Visibility.Private
        else                                      -> Visibility.PackagePrivate
    }

    override fun toString(): String = name

    fun matches(other: AsmFieldNode): Boolean {
        return kmProperty.fieldSignature?.desc == other.desc
            && kmProperty.fieldSignature?.name == other.name
    }
}