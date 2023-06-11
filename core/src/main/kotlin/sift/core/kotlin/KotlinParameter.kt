package sift.core.kotlin

import kotlinx.metadata.*
import kotlinx.metadata.jvm.signature
import sift.core.dsl.Type
import sift.core.element.AsmFieldNode

internal class KotlinParameter private constructor(
    private val kmParam: KmValueParameter?,
    receiverType: Type?
) {
    init {
        require(kmParam != null || receiverType != null) {
            "either kmParam or receiverType must be non-null"
        }
    }

    val name: String = kmParam?.name ?: receiverType!!.simpleName
    val type: Type = kmParam?.type?.let(Type::from) ?: receiverType!!
    val isExtensionReceiver: Boolean = receiverType != null

    override fun toString(): String = type.simpleName

    companion object {
        fun from(kvp: KmValueParameter): KotlinParameter = KotlinParameter(kvp, null)
        fun from(type: Type): KotlinParameter = KotlinParameter(null, type)
    }
}