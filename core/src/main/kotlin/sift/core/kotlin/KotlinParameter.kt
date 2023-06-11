package sift.core.kotlin

import kotlinx.metadata.*
import sift.core.dsl.Type

internal class KotlinParameter private constructor(
    private val kmParam: KmValueParameter?,
    receiverType: Type?
) {
    val name: String = kmParam?.name ?: receiverType!!.simpleName
    val type: Type = kmParam?.type?.let(Type::from) ?: receiverType!!
    val isExtensionReceiver: Boolean = receiverType != null

    override fun toString(): String = type.simpleName

    companion object {
        fun from(kvp: KmValueParameter): KotlinParameter = KotlinParameter(kvp, null)
        fun from(type: Type): KotlinParameter = KotlinParameter(null, type)
    }
}